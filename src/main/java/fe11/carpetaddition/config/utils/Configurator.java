package fe11.carpetaddition.config.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fe11.carpetaddition.Feca;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

public class Configurator<T> {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path CONFIG_PATH;
    private final Class<T> clazz;

    public Configurator(String configFilename, Class<T> clazz) {
        this.CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve(configFilename);
        this.clazz = clazz;
    }

    public Optional<T> load() {
        var file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file)) {
                return Optional.of(GSON.fromJson(reader, clazz));
            } catch (IOException e) {
                Feca.LOGGER.error("Failed to load config file: {}", file.getAbsolutePath(), e);
            }
        }
        return Optional.empty();
    }

    public T loadOr(T orValue) {
        var data = load();
        if (data.isEmpty()) {
            save(orValue);
            return orValue;
        }
        return data.get();
    }

    public void save(T config) {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            Feca.LOGGER.error("Failed to save config file: {}", CONFIG_PATH.toFile(), e);
        }
    }

}
