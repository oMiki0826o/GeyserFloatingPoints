package oxy.geyser.fp.util.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.geysermc.geyser.api.extension.Extension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private ConfigLoader() { }
    public static <T> T load(Extension ext, Class<?> extensionClass, Class<T> type) {
        Path dir = ext.dataFolder();
        Path file = dir.resolve("config.yml");
        try {
            Files.createDirectories(dir);
            if (Files.notExists(file)) {
                try (InputStream in = extensionClass.getClassLoader().getResourceAsStream("config.yml")) {
                    if (in == null) throw new IOException("Embedded config.yml missing");
                    Files.copy(in, file);
                }
            }
            return new ObjectMapper(new YAMLFactory())
                    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
                    .disable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
                    .readValue(file.toFile(), type);
        } catch (IOException e) {
            ext.logger().error("Failed to load configuration", e);
            return null;
        }
    }
}
