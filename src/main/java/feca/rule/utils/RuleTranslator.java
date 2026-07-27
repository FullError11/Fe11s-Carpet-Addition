package feca.rule.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

public class RuleTranslator {
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create();

    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final String LANG_FILE_PATH = "assets/fe11scarpetaddition/lang/%s.json";
    private static final Map<String, String> EMPTY_MAP = Collections.emptyMap();

    public static Map<String, String> getTranslationFromResourcePath(String lang) {
        if (lang == null || lang.isEmpty()) {
            return EMPTY_MAP;
        }

        String resourcePath = String.format(LANG_FILE_PATH, lang);

        try (InputStream langFile = RuleTranslator.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {

            if (langFile == null) {
                return EMPTY_MAP;
            }

            String jsonData = IOUtils.toString(langFile, StandardCharsets.UTF_8);
            Map<String, String> result = GSON.fromJson(jsonData, MAP_TYPE);
            return result != null ? result : EMPTY_MAP;

        } catch (IOException e) {
            return EMPTY_MAP;
        }
    }
}
