package fe11.carpetaddition.config.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fe11.carpetaddition.Feca;
import fe11.carpetaddition.utils.MinecraftServerUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

public class Configurator<T> {
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path CONFIG_PATH;
    private final Class<T> clazz;

    private Configurator(Path configPath, Class<T> clazz) {
        this.CONFIG_PATH = configPath;
        this.clazz = clazz;
    }

    @Contract("_, _, _ -> new")
    public static <T> @NotNull Configurator<T> everySaves(@NotNull MinecraftServer server, String configFilename, Class<T> clazz) {
        return new Configurator<>(
                server.getWorldPath(LevelResource.ROOT).normalize().toAbsolutePath().resolve(configFilename),
                clazz
        );
    }

    @Contract("_, _ -> new")
    public static <T> @NotNull Configurator<T> global(String configFilename, Class<T> clazz) {
        return new Configurator<>(
                FabricLoader.getInstance().getConfigDir().resolve(configFilename),
                clazz
        );
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
