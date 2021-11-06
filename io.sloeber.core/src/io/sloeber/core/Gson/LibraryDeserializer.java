package io.sloeber.core.Gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class LibraryDeserializer implements JsonDeserializer<LibraryJson> {

    @SuppressWarnings("nls")
    @Override
    public LibraryJson deserialize(JsonElement json, Type jsonType, JsonDeserializationContext arg2)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        LibraryJson lib = new LibraryJson();
        try {
            lib.name = getSafeString(jsonObject, "name");
            lib.version = getSafeString(jsonObject, "version");
            lib.author = getSafeString(jsonObject, "author");
            lib.maintainer = getSafeString(jsonObject, "maintainer");
            lib.sentence = getSafeString(jsonObject, "sentence");
            lib.paragraph = getSafeString(jsonObject, "paragraph");
            lib.website = getSafeString(jsonObject, "website");
            lib.category = getSafeString(jsonObject, "category");
            for (JsonElement curType : jsonObject.get("architectures").getAsJsonArray()) {
                lib.architectures.add(curType.getAsString());
            }
            for (JsonElement curType : jsonObject.get("types").getAsJsonArray()) {
                lib.types.add(curType.getAsString());
            }
            lib.url = getSafeString(jsonObject, "url");
            lib.archiveFileName = getSafeString(jsonObject, "archiveFileName");
            lib.size = jsonObject.get("size").getAsInt();
            lib.checksum = getSafeString(jsonObject, "checksum");
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage());
        }

        return lib;
    }

    private static String getSafeString(JsonObject jsonObject, String fieldName) {
        JsonElement field = jsonObject.get(fieldName);
        if (field == null) {
            return "no info found in file"; //$NON-NLS-1$
        }
        return field.getAsString();

    }

}
