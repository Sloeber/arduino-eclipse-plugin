/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatformTool extends Node {

    private static final String TOOLS = "tools"; //$NON-NLS-1$
    private String myName;
    private TreeMap<VersionNumber, ArduinoPlatformToolVersion> myVersions = new TreeMap<>(Collections.reverseOrder());

    private transient ArduinoPackage myParentPackage;

    @SuppressWarnings("nls")
    public ArduinoPlatformTool(JsonElement json, ArduinoPackage pkg) {
        myParentPackage = pkg;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            myName = getSafeString(jsonObject, "name");
            addVersion(jsonObject);
        } catch (Exception e) {
            throw new JsonParseException("failed to parse Tool json  " + e.getMessage());
        }
    }

    protected void addVersion(JsonElement json) {
        ArduinoPlatformToolVersion version = new ArduinoPlatformToolVersion(json, this);
        myVersions.put(version.getVersion(), version);
    }

    public ArduinoPackage getPackage() {
        return myParentPackage;
    }

    @Override
    public String getName() {
        return myName;
    }

    public ArduinoPlatformToolVersion getVersion(VersionNumber version) {
        return myVersions.get(version);
    }

    public IPath getInstallPath() {
        return myParentPackage.getInstallPath().append(TOOLS).append(getID());

    }

    @Override
    public Node[] getChildren() {
        return myVersions.values().toArray(new Node[myVersions.size()]);
    }

    @Override
    public Node getParent() {
        return myParentPackage;
    }

    @Override
    public String getID() {
        return getName();
    }

    /**
     * Get the newest version of this tool
     * 
     * @return the newest version of this tool
     */
    public ArduinoPlatformToolVersion getNewest() {
        return myVersions.firstEntry().getValue();
    }

    /**
     * return the installed version with the newest version number
     * Null if no version is installed
     * 
     * @return
     */
    public ArduinoPlatformToolVersion getNewestInstalled() {
        for (ArduinoPlatformToolVersion curVersion : myVersions.values()) {
            if (curVersion.isInstalled()) {
                return curVersion;
            }
        }
        return null;
    }

    public Collection<ArduinoPlatformToolVersion> getVersions() {
        return myVersions.values();
    }

}
