package net.midpoint.mixin;

import net.midpoint.config.MidpointConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин "умного оптимизатора" мода Midpoint.
 *
 * Логика:
 *  - smartOptimizerEnabled = true  -> если FPS падает ниже 40, динамически снижаются
 *    лимиты частиц (ParticlesMode.MINIMAL) и упрощается рендеринг.
 *  - lowEndModeEnabled = true      -> принудительно отключаются облака, погодные эффекты
 *    и частицы урезаются до минимума, независимо от текущего FPS.
 *
 * Примечание: точные имена шадов/методов (currentFps, render и т.д.) соответствуют
 * стандартным Yarn-маппингам для линии 1.21.x. Если ваш build.gradle использует
 * другую версию маппингов, свяжите @Shadow/@At с актуальными именами.
 */
@Mixin(MinecraftClient.class)
public abstract class MidpointOptimizerMixin {

    @Shadow
    private int currentFps;

    @Shadow
    public GameOptions options;

    // Порог FPS, ниже которого включается динамическая оптимизация
    private static final int FPS_THRESHOLD = 40;

    // Запоминаем, применили ли мы уже "смягчённые" настройки, чтобы не спамить их каждый тик
    private boolean midpoint$softOptimizationsApplied = false;
    private boolean midpoint$lowEndApplied = false;

    /**
     * Внедряемся в конец игрового рендер-цикла клиента, чтобы каждый кадр
     * проверять текущий FPS и состояние конфига.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void midpoint$onRenderTail(boolean tick, CallbackInfo ci) {
        MidpointConfig config = MidpointConfig.getInstance();

        if (options == null) {
            return;
        }

        // ---------- Low-End режим: максимальное снижение нагрузки ----------
        if (config.lowEndModeEnabled) {
            if (!midpoint$lowEndApplied) {
                midpoint$applyLowEndSettings();
                midpoint$lowEndApplied = true;
            }
            return; // low-end режим приоритетнее умного оптимизатора
        } else if (midpoint$lowEndApplied) {
            // Low-end режим был выключен в конфиге во время игры - сбрасываем флаг,
            // настройки при этом не восстанавливаем автоматически (это делает игрок вручную).
            midpoint$lowEndApplied = false;
        }

        // ---------- Умный оптимизатор ----------
        if (config.smartOptimizerEnabled) {
            if (currentFps > 0 && currentFps < FPS_THRESHOLD) {
                if (!midpoint$softOptimizationsApplied) {
                    midpoint$applySoftOptimizations();
                    midpoint$softOptimizationsApplied = true;
                }
            } else if (currentFps >= FPS_THRESHOLD + 10) {
                // FPS восстановился с запасом - откатываем "мягкие" оптимизации
                if (midpoint$softOptimizationsApplied) {
                    midpoint$restoreDefaultSettings();
                    midpoint$softOptimizationsApplied = false;
                }
            }
        }
    }

    /**
     * Мягкое снижение нагрузки при просадке FPS ниже порога.
     */
    private void midpoint$applySoftOptimizations() {
        options.getParticles().setValue(ParticlesMode.MINIMAL);
        options.getCloudRenderMode().setValue(CloudRenderMode.FANCY.equals(options.getCloudRenderMode().getValue())
                ? CloudRenderMode.FAST
                : options.getCloudRenderMode().getValue());
    }

    /**
     * Возвращает настройки к более "полным" значениям после восстановления FPS.
     * (Не трогаем облака и частицы агрессивно, чтобы не дёргать визуал туда-сюда.)
     */
    private void midpoint$restoreDefaultSettings() {
        options.getParticles().setValue(ParticlesMode.DECREASED);
    }

    /**
     * Принудительный low-end режим: минимум частиц, отключенные облака.
     * Отключение погодных эффектов реализовано отдельным миксином ниже (WorldRendererWeatherMixin),
     * так как рендер погоды находится в WorldRenderer, а не в MinecraftClient.
     */
    private void midpoint$applyLowEndSettings() {
        options.getParticles().setValue(ParticlesMode.MINIMAL);
        options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
    }
}

/**
 * Вспомогательный миксин, отключающий отрисовку погодных эффектов (дождь/снег)
 * при включённом lowEndModeEnabled в конфиге Midpoint.
 *
 * Находится в этом же файле для удобства, так как логически относится к тому же
 * "оптимизатору", но физически внедряется в другой класс игры (WorldRenderer).
 */
@Mixin(WorldRenderer.class)
abstract class MidpointWeatherOptimizerMixin {

    @Inject(method = "renderWeather", at = @At("HEAD"), cancellable = true)
    private void midpoint$onRenderWeather(CallbackInfo ci) {
        MidpointConfig config = MidpointConfig.getInstance();
        if (config.lowEndModeEnabled) {
            ci.cancel();
        }
    }
}
