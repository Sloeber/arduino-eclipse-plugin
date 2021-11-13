/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.common.ConfigurationPreferences;

public class ArduinoPlatformTool {

    private static final String TOOLS = "tools"; //$NON-NLS-1$
    private String name;
    private String version;
    private List<ArduinpPlatformToolSystem> systems = new ArrayList<>();

    private transient ArduinoPackage pkg;

    @SuppressWarnings("nls")
    public ArduinoPlatformTool(JsonElement json, ArduinoPackage pkg) {
        this.pkg = pkg;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            name = getSafeString(jsonObject, "name");
            version = getSafeString(jsonObject, "version");
            if (jsonObject.get("systems") != null) {
                for (JsonElement curElement : jsonObject.get("systems").getAsJsonArray()) {
                    systems.add(new ArduinpPlatformToolSystem(curElement, this));
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse Tool json  " + e.getMessage());
        }

    }

    public ArduinoPackage getPackage() {
        return this.pkg;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public List<ArduinpPlatformToolSystem> getSystems() {
        return this.systems;
    }

    public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathPackages().append(this.pkg.getName()).append(TOOLS)
                .append(this.name).append(this.version);

    }

    public boolean isInstalled() {
        return getInstallPath().toFile().exists();
    }

    /*
     * Get the installable for this tool on this system
     * May return null if none is found
     */
    public ArduinoInstallable getInstallable() {
        for (ArduinpPlatformToolSystem system : this.systems) {
            if (system.isApplicable()) {
                return system;
            }
        }
        return null;
    }

}
