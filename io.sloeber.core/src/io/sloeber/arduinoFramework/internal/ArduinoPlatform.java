/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;
import static io.sloeber.core.api.Const.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.arduinoFramework.api.IArduinoPackage;
import io.sloeber.arduinoFramework.api.IArduinoPlatform;
import io.sloeber.arduinoFramework.api.IArduinoPlatformVersion;
import io.sloeber.core.api.Const;
import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatform implements IArduinoPlatform {

    private String name;
    private String architecture;
    private TreeMap<VersionNumber, IArduinoPlatformVersion> myVersions = new TreeMap<>(Collections.reverseOrder());

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
            throw new JsonParseException("failed to parse ArduinoPlatform json  " + e.getMessage(),e);
        }
    }

    protected void addVersion(JsonElement json) {
        ArduinoPlatformVersion version = new ArduinoPlatformVersion(json, this);
        myVersions.put(version.getVersion(), version);
    }

    @Override
	public IArduinoPackage getParent() {
        return this.myParent;
    }

    @Override
	public String getName() {
        return this.name;
    }

    @Override
	public String getArchitecture() {
        return architecture;
    }

    @Override
	public boolean isInstalled() {
        for (IArduinoPlatformVersion curPlatformVersion : myVersions.values()) {
            if (curPlatformVersion.isInstalled()) {
                return true;
            }
        }
        return false;
    }

    @Override
	public IPath getInstallPath() {
        return myParent.getInstallPath().append(Const.ARDUINO_HARDWARE_FOLDER_NAME).append(getID());
    }

    @Override
	public String getID() {
        return architecture;
    }

    @Override
    public int compareTo(IArduinoPlatform o) {
        return name.compareTo(o.getName());
    }

    /**
     * Get the newest version of this platform
     *
     * @return the newest version of this platform
     */
    @Override
	public IArduinoPlatformVersion getNewestVersion() {
        return myVersions.firstEntry().getValue();
    }

    @Override
	public Collection<IArduinoPlatformVersion> getVersions() {
        return new LinkedList<>(myVersions.values());
    }

    @Override
	public IArduinoPlatformVersion getVersion(VersionNumber refVersion) {
        return myVersions.get(refVersion);
    }

    /**
     * return the installed version with the newest version number
     * Null if no version is installed
     *
     * @return
     */
    @Override
	public IArduinoPlatformVersion getNewestInstalled() {
        for (IArduinoPlatformVersion curVersion : myVersions.values()) {
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
