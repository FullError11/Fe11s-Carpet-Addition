package feca.config.utils;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;

public class IdentifierDeserializer implements JsonDeserializer<Identifier> {
    @Override
    public @NonNull Identifier deserialize(@NonNull JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String keyString = json.getAsString();
        return Identifier.parse(keyString);
    }
}
