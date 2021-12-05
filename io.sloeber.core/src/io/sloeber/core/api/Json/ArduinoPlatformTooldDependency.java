package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;
import static io.sloeber.core.common.Const.*;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.common.ConfigurationPreferences;

public class ArduinoPlatformTooldDependency {

    private String myName;
    private String myPackager;
    private VersionNumber myVersion;

    private transient ArduinoPlatformVersion myParentPlatform;

    @SuppressWarnings("nls")
    public ArduinoPlatformTooldDependency(JsonElement json, ArduinoPlatformVersion arduinoPlatformVersion) {
        myParentPlatform = arduinoPlatformVersion;
        JsonObject jsonObject = json.getAsJsonObject();
        try {

            myName = getSafeString(jsonObject, "name");
            myPackager = getSafeString(jsonObject, "packager");
            myVersion = getSafeVersion(jsonObject, "version");
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage());
        }
    }

    public String getName() {
        return myName;
    }

    public VersionNumber getVersion() {
        return myVersion;
    }

    public String getPackager() {
        return myPackager;
    }

    public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathPackages().append(myPackager).append(TOOLS).append(myName)
                .append(myVersion.toString());

    }
}
