/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;
import static io.sloeber.core.api.Const.*;

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
    private transient IPath myInstallPath = null;

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
            throw new JsonParseException("failed to parse Tool json  " + e.getMessage(),e);
        }

    }

    public ArduinoPlatformTool getTool() {
        return myParentTool;
    }

    @Override
    public String getNodeName() {
        return myParentTool.getNodeName();
    }

    public VersionNumber getVersion() {
        return myVersion;
    }

    public List<ArduinpPlatformToolSystem> getSystems() {
        return mySystems;
    }

    /**
     * This method is fucking wierd ...
     * but I can't help it...
     * The problem is that the tool depency references a packager (which is part of
     * the install path)
     * but the tool itself doe not
     * So to know where to install there are 2 options
     * 1) install in the local platform (resulting in tool duplication)
     * 2) search the dependency tree for the tooldepency and use the installpath
     * from there
     *
     * @return
     */
    public IPath getInstallPath() {
        if (myInstallPath != null) {
            return myInstallPath;
        }
        ArduinoPackage pkg = myParentTool.getPackage();
        for (ArduinoPlatform curPlatform : pkg.getPlatforms()) {
            for (ArduinoPlatformVersion curplatformVersion : curPlatform.getVersions()) {
                for (ArduinoPlatformTooldDependency curTooldependency : curplatformVersion.getToolsDependencies()) {
                    if (curTooldependency.getName().equals(myParentTool.getNodeName())
                            && curTooldependency.getVersion().compareTo(myVersion) == 0) {
                        myInstallPath = curTooldependency.getInstallPath();
                        return myInstallPath;
                    }
                }
            }
        }
        myInstallPath = myParentTool.getInstallPath().append(getID());
        return myInstallPath;

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
        String keyString = ENV_KEY_RUNTIME_TOOLS + getNodeName() + getVersion() + DOT_PATH;
        vars.put(keyString, installPath);
        keyString = ENV_KEY_RUNTIME_TOOLS + getNodeName() + '-' + getVersion() + DOT_PATH;
        vars.put(keyString, installPath);
        if (skipDefault) {
            return vars;
        }
        keyString = ENV_KEY_RUNTIME_TOOLS + getNodeName() + DOT_PATH;
        vars.put(keyString, installPath);
        return vars;
    }

}
