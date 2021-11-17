package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatformTooldDependency {

    private String myName;
    private VersionNumber myVersion;

    private transient ArduinoPlatformVersion myParentPlatform;

    @SuppressWarnings("nls")
    public ArduinoPlatformTooldDependency(JsonElement json, ArduinoPlatformVersion arduinoPlatformVersion) {
        myParentPlatform = arduinoPlatformVersion;
        JsonObject jsonObject = json.getAsJsonObject();
        try {

            myName = getSafeString(jsonObject, "name");
            myVersion = getSafeVersion(jsonObject, "version");
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage());
        }
    }

    public String getName() {
        return this.myName;
    }

    public VersionNumber getVersion() {
        return myVersion;
    }

}
