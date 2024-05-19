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
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.VersionNumber;

public class ArduinoPackage extends Node implements Comparable<ArduinoPackage> {

    private String name;
    private String maintainer;
    private String websiteURL;
    private String email;
    private String helpOnline;
    private TreeMap<String, ArduinoPlatform> platforms = new TreeMap<>();
    private TreeMap<String, ArduinoPlatformTool> tools = new TreeMap<>();
    private transient ArduinoPlatformPackageIndex myParent = null;

    @SuppressWarnings("nls")
    public ArduinoPackage(JsonElement json, ArduinoPlatformPackageIndex packageIndex) {
        myParent = packageIndex;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            name = getSafeString(jsonObject, "name");
            maintainer = getSafeString(jsonObject, "maintainer");
            websiteURL = getSafeString(jsonObject, "websiteURL");
            email = getSafeString(jsonObject, "email");
            helpOnline = getSafeString(jsonObject, "help", "online");
            for (JsonElement curElement : jsonObject.get("platforms").getAsJsonArray()) {
                JsonObject jsonObject2 = curElement.getAsJsonObject();
                // architecture is the id for a platform
                String architecture = getSafeString(jsonObject2, "architecture");
                ArduinoPlatform platform = platforms.get(architecture);
                if (platform == null) {
                    ArduinoPlatform newPlatform = new ArduinoPlatform(curElement, this);
                    platforms.put(newPlatform.getID(), newPlatform);
                } else {
                    platform.addVersion(curElement);
                }
            }
            for (JsonElement curElement : jsonObject.get("tools").getAsJsonArray()) {
                JsonObject jsonObject2 = curElement.getAsJsonObject();
                // architecture is the id for a platform
                String toolName = getSafeString(jsonObject2, "name");
                ArduinoPlatformTool tool = tools.get(toolName);
                if (tool == null) {
                    ArduinoPlatformTool newTool = new ArduinoPlatformTool(curElement, this);
                    tools.put(newTool.getID(), newTool);
                } else {
                    tool.addVersion(curElement);
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse Package json  " + e.getMessage());
        }
    }

    public ArduinoPlatformPackageIndex getPackageIndex() {
        return myParent;
    }

    @Override
    public String getNodeName() {
        return name;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public String getWebsiteURL() {
        return websiteURL;
    }

    public String getEmail() {
        return email;
    }

    public String getHelp() {
        return helpOnline;
    }

    public Collection<ArduinoPlatform> getPlatforms() {
        return platforms.values();
    }

    /**
     * This method looks up the installed platforms So if you have 2 arduino avr
     * platform versions installed you will get back 2.
     *
     * @return all the installed platforms
     */
    public List<ArduinoPlatformVersion> getInstalledPlatforms() {
        List<ArduinoPlatformVersion> platformMap = new LinkedList<>();
        for (ArduinoPlatform platform : platforms.values()) {
            for (ArduinoPlatformVersion platformVersion : platform.getVersions()) {
                if (platformVersion.isInstalled()) {
                    platformMap.add(platformVersion);
                }
            }
        }
        return platformMap;
    }

    /**
     * get tyhe platform based on the platform ID
     * The platform ID is the architecture
     * 
     * @param platformID
     * @return return the platfiorm or null if not found
     */
    public ArduinoPlatform getPlatform(String platformID) {
        return platforms.get(platformID);
    }

    public Collection<ArduinoPlatformTool> getTools() {
        return tools.values();
    }

    public ArduinoPlatformToolVersion getTool(String toolName, VersionNumber version) {
        ArduinoPlatformTool tool = tools.get(toolName);
        if (tool == null) {
            return null;
        }
        return tool.getVersion(version);
    }

    public ArduinoPlatformTool getTool(String toolName) {
        return tools.get(toolName);
    }

    public ArduinoPlatformToolVersion getNewestInstalled(String toolName) {
        ArduinoPlatformTool tool = tools.get(toolName);
        if (tool == null) {
            return null;
        }
        return tool.getNewestInstalled();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArduinoPackage) {
            return ((ArduinoPackage) obj).getNodeName().equals(this.name);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public int compareTo(ArduinoPackage other) {
        return this.name.compareTo(other.name);
    }

    public void onlyKeepLatestPlatforms() {
        for (ArduinoPlatform curplatform : platforms.values()) {
            ArduinoPlatformVersion newestVersion = null;
            for (ArduinoPlatformVersion curVersion : curplatform.getVersions()) {
                if (curVersion.isInstalled()) {
                    if (newestVersion == null) {
                        newestVersion = curVersion;
                    } else {
                        if (newestVersion.getVersion().compareTo(curVersion.getVersion()) > 0) {
                            BoardsManager.uninstall(curVersion, null);
                        } else {
                            BoardsManager.uninstall(newestVersion, null);
                        }
                    }
                }
            }
        }
    }

    public boolean isInstalled() {
        for (ArduinoPlatform platform : platforms.values()) {
            if (platform.isInstalled()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Is any version of the platform installed
     * 
     * @param platformName
     * @return if a platform with this name is installed
     */
    public boolean isAVersionOfThisPlatformInstalled(String platformName) {
        for (ArduinoPlatform platform : this.platforms.values()) {
            if (platform.getName().equals(platformName)) {
                if (platform.isInstalled()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getID() {
        return name;
    }

    public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathPackages().append(getID());
    }

    @Override
    public Node[] getChildren() {
        return platforms.values().toArray(new Node[platforms.size()]);
    }

    @Override
    public Node getParent() {
        return myParent;
    }

}
