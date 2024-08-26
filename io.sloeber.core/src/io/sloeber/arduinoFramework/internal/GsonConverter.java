package io.sloeber.arduinoFramework.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.sloeber.core.api.VersionNumber;

public class GsonConverter {
    public static String getSafeString(JsonObject jsonObject, String fieldName) {
        JsonElement field = jsonObject.get(fieldName);
        if (field == null) {
            return "no info found in file"; //$NON-NLS-1$
        }
        return field.getAsString();

    }

    public static VersionNumber getSafeVersion(JsonObject jsonObject, String fieldName) {
        JsonElement field = jsonObject.get(fieldName);
        if (field == null) {
            return new VersionNumber("no version number provided"); //$NON-NLS-1$
        }
        return new VersionNumber(field.getAsString());

    }

    public static String getSafeString(JsonObject jsonObject, String fieldName1, String fieldName2) {
        JsonElement field = jsonObject.get(fieldName1);
        if (field != null) {
            field = field.getAsJsonObject().get(fieldName2);
            if (field != null) {
                return field.getAsString();
            }
        }

        return "no info found in file"; //$NON-NLS-1$

    }

}
