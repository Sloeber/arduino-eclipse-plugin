package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;
import static io.sloeber.core.api.Const.*;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.arduinoFramework.api.IArduinoPlatformVersion;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatformTooldDependency {

    private String myName;
    private String myPackager;
    private VersionNumber myVersion;

    private transient IArduinoPlatformVersion myParentPlatform;

    @SuppressWarnings("nls")
    public ArduinoPlatformTooldDependency(JsonElement json, IArduinoPlatformVersion arduinoPlatformVersion) {
        myParentPlatform = arduinoPlatformVersion;
        JsonObject jsonObject = json.getAsJsonObject();
        try {

            myName = getSafeString(jsonObject, "name");
            myPackager = getSafeString(jsonObject, "packager");
            myVersion = getSafeVersion(jsonObject, "version");
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage(),e);
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
