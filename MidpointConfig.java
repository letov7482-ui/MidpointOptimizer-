package net.midpoint.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Лёгкий JSON-конфиг мода Midpoint.
 * Хранится в файле config/midpoint.json
 */
public class MidpointConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("midpoint-config");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("midpoint.json");

    private static MidpointConfig INSTANCE;

    // ---------------------- HUD настройки ----------------------
    public boolean enableHud = true;
    public boolean showAverageFps = true;
    public boolean showFrameTime = true;
    public boolean showMemory = true;

    public int hudX = 4;
    public int hudY = 4;

    public float hudScale = 1.0f;
    public float hudTransparency = 0.4f; // 0.0f - полностью прозрачный фон, 1.0f - непрозрачный

    public int textColor = 0xFFFFFF; // HEX без альфы

    public boolean showShadow = true;
    public boolean showBackground = true;

    // ---------------------- Оптимизация ----------------------
    public boolean smartOptimizerEnabled = true;
    public boolean lowEndModeEnabled = false;

    private MidpointConfig() {
    }

    /**
     * Возвращает текущий загруженный экземпляр конфига.
     * Если конфиг ещё не загружен — загружает его с диска (или создаёт дефолтный).
     */
    public static MidpointConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    /**
     * Загружает конфиг из config/midpoint.json.
     * Если файл отсутствует или повреждён — создаёт новый с дефолтными значениями.
     */
    public static MidpointConfig load() {
        if (!Files.exists(CONFIG_PATH)) {
            MidpointConfig defaultConfig = new MidpointConfig();
            defaultConfig.save();
            return defaultConfig;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
            MidpointConfig loaded = GSON.fromJson(reader, MidpointConfig.class);
            if (loaded == null) {
                LOGGER.warn("[Midpoint] Конфиг пуст или повреждён, создаю новый по умолчанию.");
                loaded = new MidpointConfig();
                loaded.save();
            }
            return loaded;
        } catch (IOException e) {
            LOGGER.error("[Midpoint] Не удалось загрузить config/midpoint.json, использую значения по умолчанию.", e);
            MidpointConfig fallback = new MidpointConfig();
            fallback.save();
            return fallback;
        }
    }

    /**
     * Сохраняет текущее состояние конфига в config/midpoint.json.
     */
    public void save() {
        try {
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.error("[Midpoint] Не удалось сохранить config/midpoint.json", e);
        }
    }

    /**
     * Перезагружает конфиг с диска и заменяет текущий инстанс.
     */
    public static void reload() {
        INSTANCE = load();
    }
}
