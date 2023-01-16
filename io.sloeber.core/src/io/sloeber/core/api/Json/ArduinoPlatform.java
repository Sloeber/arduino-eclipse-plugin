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

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.common.Const;

public class ArduinoPlatform implements Comparable<ArduinoPlatform> {

    private String name;
    private String architecture;
    private TreeMap<VersionNumber, ArduinoPlatformVersion> myVersions = new TreeMap<>(Collections.reverseOrder());

    private ArduinoPackage myParent;

    @SuppressWarnings("nls")
    public ArduinoPlatform(JsonElement json, ArduinoPackage parent) {
        myParent = parent;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            name = getSafeString(jsonObject, "name");
            architecture = getSafeString(jsonObject, "architecture");
            addVersion(json);
        } catch (Exception e) {
            throw new JsonParseException("failed to parse ArduinoPlatform json  " + e.getMessage());
        }
    }

    protected void addVersion(JsonElement json) {
        ArduinoPlatformVersion version = new ArduinoPlatformVersion(json, this);
        myVersions.put(version.getVersion(), version);
    }

    public ArduinoPackage getParent() {
        return this.myParent;
    }

    public String getName() {
        return this.name;
    }

    public String getArchitecture() {
        return architecture;
    }

    public boolean isInstalled() {
        for (ArduinoPlatformVersion curPlatformVersion : myVersions.values()) {
            if (curPlatformVersion.isInstalled()) {
                return true;
            }
        }
        return false;
    }

    public IPath getInstallPath() {
        return myParent.getInstallPath().append(Const.ARDUINO_HARDWARE_FOLDER_NAME).append(getID());
    }

    public String getID() {
        return architecture;
    }

    @Override
    public int compareTo(ArduinoPlatform o) {
        return name.compareTo(o.getName());
    }

    /**
     * Get the newest version of this platform
     * 
     * @return the newest version of this platform
     */
    public ArduinoPlatformVersion getNewestVersion() {
        return myVersions.firstEntry().getValue();
    }

    public Collection<ArduinoPlatformVersion> getVersions() {
        return myVersions.values();
    }

    public ArduinoPlatformVersion getVersion(VersionNumber refVersion) {
        return myVersions.get(refVersion);
    }

    /**
     * return the installed version with the newest version number
     * Null if no version is installed
     * 
     * @return
     */
    public ArduinoPlatformVersion getNewestInstalled() {
        for (ArduinoPlatformVersion curVersion : myVersions.values()) {
            if (curVersion.isInstalled()) {
                return curVersion;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name + SPACE + architecture;
    }
}
