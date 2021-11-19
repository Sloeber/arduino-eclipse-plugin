/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;
import static io.sloeber.core.common.Const.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatformToolVersion extends Node {

    private VersionNumber myVersion;
    private List<ArduinpPlatformToolSystem> mySystems = new ArrayList<>();

    private transient ArduinoPlatformTool myParentTool;

    @SuppressWarnings("nls")
    public ArduinoPlatformToolVersion(JsonElement json, ArduinoPlatformTool tool) {
        myParentTool = tool;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            myVersion = getSafeVersion(jsonObject, "version");
            if (jsonObject.get("systems") != null) {
                for (JsonElement curElement : jsonObject.get("systems").getAsJsonArray()) {
                    mySystems.add(new ArduinpPlatformToolSystem(curElement, this));
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse Tool json  " + e.getMessage());
        }

    }

    public ArduinoPlatformTool getTool() {
        return myParentTool;
    }

    @Override
    public String getName() {
        return myParentTool.getName();
    }

    public VersionNumber getVersion() {
        return myVersion;
    }

    public List<ArduinpPlatformToolSystem> getSystems() {
        return mySystems;
    }

    public IPath getInstallPath() {
        return myParentTool.getInstallPath().append(getID());

    }

    public boolean isInstalled() {
        return getInstallPath().toFile().exists();
    }

    /*
     * Get the installable for this tool on this system
     * May return null if none is found
     */
    public ArduinoInstallable getInstallable() {
        for (ArduinpPlatformToolSystem system : this.mySystems) {
            if (system.isApplicable()) {
                return system;
            }
        }
        return null;
    }

    @Override
    public Node[] getChildren() {
        return null;
    }

    @Override
    public Node getParent() {
        return myParentTool;
    }

    @Override
    public String getID() {
        return myVersion.toString();
    }

    public HashMap<String, String> getEnvVars(boolean skipDefault) {

        HashMap<String, String> vars = new HashMap<>();
        String installPath = getInstallPath().toOSString();
        String keyString = RUNTIME_TOOLS + getName() + getVersion() + DOT_PATH;
        vars.put(keyString, installPath);
        keyString = RUNTIME_TOOLS + getName() + '-' + getVersion() + DOT_PATH;
        vars.put(keyString, installPath);
        if (skipDefault) {
            return vars;
        }
        keyString = RUNTIME_TOOLS + getName() + DOT_PATH;
        vars.put(keyString, installPath);
        return vars;
    }

}
