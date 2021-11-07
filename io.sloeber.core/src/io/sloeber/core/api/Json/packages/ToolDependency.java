package io.sloeber.core.api.Json.packages;

import static io.sloeber.core.Gson.GsonConverter.*;
import static io.sloeber.core.Messages.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.Activator;
import io.sloeber.core.api.PackageManager;

public class ToolDependency {

    private String packager;
    private String name;
    private String version;

    private transient ArduinoPlatform platform;

    @SuppressWarnings("nls")
    public ToolDependency(JsonElement json, ArduinoPlatform arduinoPlatform) {
        platform = arduinoPlatform;
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

    //TODO remove this code
    public Tool getTool() {
        ArduinoPackage pkg = this.platform.getParent();
        if (!pkg.getName().equals(this.packager)) {
            pkg = PackageManager.getPackage(this.packager);
        }
        if (pkg == null) {
            return null;
        }
        return pkg.getTool(this.name, getVersion());
    }

    //TODO remove this code
    public IStatus install(IProgressMonitor monitor) {
        Tool tool = getTool();
        if (tool == null) {
            return new Status(IStatus.ERROR, Activator.getId(),
                    ToolDependency_Tool_not_found.replace(NAME_TAG, this.name).replace(VERSION_TAG, this.version));
        }
        return tool.install(monitor);
    }

}
