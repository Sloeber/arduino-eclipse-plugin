/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.arduinoFramework.api.IArduinoPackage;
import io.sloeber.arduinoFramework.api.IArduinoPlatform;
import io.sloeber.arduinoFramework.api.IArduinoPlatformVersion;
import io.sloeber.arduinoFramework.api.Node;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.VersionNumber;

public class ArduinoPackage extends Node implements  IArduinoPackage {

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
            throw new JsonParseException("failed to parse Package json  " + e.getMessage(),e);
        }
    }

    @Override
	public ArduinoPlatformPackageIndex getPackageIndex() {
        return myParent;
    }

    @Override
    public String getNodeName() {
        return name;
    }

    @Override
	public String getMaintainer() {
        return maintainer;
    }

    @Override
	public String getWebsiteURL() {
        return websiteURL;
    }

    @Override
	public String getEmail() {
        return email;
    }

    @Override
	public String getHelp() {
        return helpOnline;
    }

    @Override
	public Collection<IArduinoPlatform> getPlatforms() {
        return new LinkedList<>( platforms.values());
    }

    /**
     * This method looks up the installed platforms So if you have 2 arduino avr
     * platform versions installed you will get back 2.
     *
     * @return all the installed platforms
     */
    @Override
	public List<IArduinoPlatformVersion> getInstalledPlatforms() {
        List<IArduinoPlatformVersion> platformMap = new LinkedList<>();
        for (ArduinoPlatform platform : platforms.values()) {
            for (IArduinoPlatformVersion platformVersion : platform.getVersions()) {
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
    @Override
	public IArduinoPlatform getPlatform(String platformID) {
        return platforms.get(platformID);
    }

    @Override
	public Collection<ArduinoPlatformTool> getTools() {
        return tools.values();
    }

    @Override
	public ArduinoPlatformToolVersion getTool(String toolName, VersionNumber version) {
        ArduinoPlatformTool tool = tools.get(toolName);
        if (tool == null) {
            return null;
        }
        return tool.getVersion(version);
    }

    @Override
	public ArduinoPlatformTool getTool(String toolName) {
        return tools.get(toolName);
    }

    @Override
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
    public int compareTo(IArduinoPackage other) {
        return this.name.compareTo(other.getName());
    }

    @Override
	public void onlyKeepLatestPlatforms() {
        for (IArduinoPlatform curplatform : platforms.values()) {
            IArduinoPlatformVersion newestVersion = null;
            for (IArduinoPlatformVersion curVersion : curplatform.getVersions()) {
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

    @Override
	public boolean isInstalled() {
        for (IArduinoPlatform platform : platforms.values()) {
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
    @Override
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

    @Override
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

	@Override
	public String getName() {
		return name;
	}

}
