package net.midpoint.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.midpoint.config.MidpointConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayDeque;

/**
 * HUD-оверлей мода Midpoint.
 * Показывает: текущий FPS, точный Frame Time (мс), средний FPS и использование RAM.
 */
public class MidpointFpsHud implements HudRenderCallback {

    // Храним времена последних кадров (в наносекундах) для расчёта среднего FPS за последнюю секунду.
    private final ArrayDeque<Long> frameTimestamps = new ArrayDeque<>();

    private long lastFrameNanoTime = System.nanoTime();
    private double lastFrameTimeMs = 0.0;
    private int currentFps = 0;
    private int averageFps = 0;

    private static final long ONE_SECOND_NANOS = 1_000_000_000L;

    @Override
    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MidpointConfig config = MidpointConfig.getInstance();

        if (!config.enableHud) {
            return;
        }

        updateFrameStats();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null && client.world == null) {
            // На главном меню тоже можно показывать, но если хотите скрывать - раскомментируйте:
            // return;
        }

        TextRenderer textRenderer = client.textRenderer;

        // Собираем строки, которые будем выводить
        java.util.List<String> lines = new java.util.ArrayList<>();

        lines.add("Midpoint FPS: " + currentFps);

        if (config.showAverageFps) {
            lines.add("Avg FPS: " + averageFps);
        }

        if (config.showFrameTime) {
            lines.add(String.format("Frame Time: %.2f ms", lastFrameTimeMs));
        }

        if (config.showMemory) {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            long usedMb = usedMemory / (1024L * 1024L);
            long maxMb = maxMemory / (1024L * 1024L);

            lines.add("RAM: " + usedMb + " / " + maxMb + " MB");
        }

        renderLines(drawContext, textRenderer, lines, config);
    }

    /**
     * Обновляет счётчики FPS и frame time на основе реального времени между кадрами.
     */
    private void updateFrameStats() {
        long now = System.nanoTime();
        long delta = now - lastFrameNanoTime;
        lastFrameNanoTime = now;

        if (delta <= 0) {
            delta = 1;
        }

        lastFrameTimeMs = delta / 1_000_000.0;
        currentFps = (int) Math.round(1_000_000_000.0 / delta);

        frameTimestamps.addLast(now);

        // Убираем метки старше 1 секунды
        while (!frameTimestamps.isEmpty() && (now - frameTimestamps.peekFirst()) > ONE_SECOND_NANOS) {
            frameTimestamps.pollFirst();
        }

        averageFps = frameTimestamps.size();
    }

    /**
     * Отрисовывает HUD с учётом масштаба, прозрачности фона, тени текста и позиции из конфига.
     */
    private void renderLines(DrawContext drawContext, TextRenderer textRenderer, java.util.List<String> lines, MidpointConfig config) {
        if (lines.isEmpty()) {
            return;
        }

        float scale = config.hudScale <= 0 ? 1.0f : config.hudScale;

        int lineHeight = textRenderer.fontHeight + 2;
        int padding = 3;

        int maxWidth = 0;
        for (String line : lines) {
            int width = textRenderer.getWidth(line);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int boxWidth = maxWidth + padding * 2;
        int boxHeight = lines.size() * lineHeight + padding * 2;

        drawContext.getMatrices().push();
        drawContext.getMatrices().translate(config.hudX, config.hudY, 0);
        drawContext.getMatrices().scale(scale, scale, 1.0f);

        if (config.showBackground) {
            int alpha = (int) (Math.max(0.0f, Math.min(1.0f, config.hudTransparency)) * 255) << 24;
            int backgroundColor = alpha | 0x000000;
            drawContext.fill(0, 0, boxWidth, boxHeight, backgroundColor);
        }

        int textColor = 0xFF000000 | (config.textColor & 0x00FFFFFF);

        int y = padding;
        for (String line : lines) {
            if (config.showShadow) {
                drawContext.drawTextWithShadow(textRenderer, line, padding, y, textColor);
            } else {
                drawContext.drawText(textRenderer, line, padding, y, textColor, false);
            }
            y += lineHeight;
        }

        drawContext.getMatrices().pop();
    }
}
