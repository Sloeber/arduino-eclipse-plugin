package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.BoardsManager;

public class ArduinoPlatformTooldDependency {

    private String packager;
    private String name;
    private String version;

    private transient ArduinoPlatformVersion platform;

    @SuppressWarnings("nls")
    public ArduinoPlatformTooldDependency(JsonElement json, ArduinoPlatformVersion arduinoPlatformVersion) {
        platform = arduinoPlatformVersion;
        JsonObject jsonObject = json.getAsJsonObject();
        try {

            packager = getSafeString(jsonObject, "packager");
            name = getSafeString(jsonObject, "name");
            version = getSafeString(jsonObject, "version");
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage());
        }
    }

    public String getPackager() {
        return this.packager;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public ArduinoPlatformTool getTool() {
        ArduinoPackage pkg = this.platform.getParent().getParent();
        if (!pkg.getName().equals(this.packager)) {
            pkg = BoardsManager.getPackage(this.packager);
        }
        if (pkg == null) {
            return null;
        }
        return pkg.getTool(this.name, getVersion());
    }

}
