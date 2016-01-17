/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArduinoPackage {

    private String name;
    private String maintainer;
    private String websiteURL;
    private String email;
    private List<ArduinoPlatform> platforms;
    private List<ArduinoTool> tools;

    // private transient ArduinoManager manager;

    void setOwner(ArduinoManager manager) {
	// this.manager = manager;
	for (ArduinoPlatform platform : this.platforms) {
	    platform.setOwner(this);
	}
	for (ArduinoTool tool : this.tools) {
	    tool.setOwner(this);
	}
    }

    // ArduinoManager getManager() {
    // return manager;
    // }

    public String getName() {
	return this.name;
    }

    public String getMaintainer() {
	return this.maintainer;
    }

    public String getWebsiteURL() {
	return this.websiteURL;
    }

    public String getEmail() {
	return this.email;
    }

    public List<ArduinoPlatform> getPlatforms() {
	return this.platforms;
    }

    /**
     * Only the latest versions of the platforms.
     * 
     * @return latest platforms
     */
    public Collection<ArduinoPlatform> getLatestPlatforms() {
	Map<String, ArduinoPlatform> platformMap = new HashMap<>();
	for (ArduinoPlatform platform : this.platforms) {
	    ArduinoPlatform p = platformMap.get(platform.getName());
	    if (p == null || ArduinoManager.compareVersions(platform.getVersion(), p.getVersion()) > 0) {
		platformMap.put(platform.getName(), platform);
	    }
	}
	return Collections.unmodifiableCollection(platformMap.values());
    }

    public Collection<ArduinoPlatform> getInstalledPlatforms() {
	Map<String, ArduinoPlatform> platformMap = new HashMap<>();
	for (ArduinoPlatform platform : this.platforms) {
	    if (platform.isInstalled()) {
		ArduinoPlatform p = platformMap.get(platform.getName());
		if (p == null || ArduinoManager.compareVersions(platform.getVersion(), p.getVersion()) > 0) {
		    platformMap.put(platform.getName(), platform);
		}
	    }
	}
	return Collections.unmodifiableCollection(platformMap.values());
    }

    public ArduinoPlatform getPlatform(String platformName) {
	ArduinoPlatform foundPlatform = null;
	for (ArduinoPlatform platform : this.platforms) {
	    if (platform.getName().equals(platformName)) {
		if (foundPlatform == null) {
		    foundPlatform = platform;
		} else {
		    if (platform.isInstalled() && ArduinoManager.compareVersions(platform.getVersion(), foundPlatform.getVersion()) > 0) {
			foundPlatform = platform;
		    }
		}
	    }
	}
	return foundPlatform;
    }

    public ArduinoPlatform getLatestPlatform(String platformName) {
	ArduinoPlatform foundPlatform = null;
	for (ArduinoPlatform platform : this.platforms) {
	    if (platform.getName().equals(platformName)) {
		if (foundPlatform == null) {
		    foundPlatform = platform;
		} else {
		    if (ArduinoManager.compareVersions(platform.getVersion(), foundPlatform.getVersion()) > 0) {
			foundPlatform = platform;
		    }
		}
	    }
	}
	return foundPlatform;
    }

    public ArduinoPlatform getPlatform(String platformName, String version) {

	for (ArduinoPlatform platform : this.platforms) {
	    if (platform.getName().equals(platformName)) {
		if (ArduinoManager.compareVersions(platform.getVersion(), version) == 0) {
		    return platform;
		}
	    }
	}
	return null;
    }

    public List<ArduinoTool> getTools() {
	return this.tools;
    }

    public ArduinoTool getTool(String toolName, String version) {
	for (ArduinoTool tool : this.tools) {
	    if (tool.getName().equals(toolName) && tool.getVersion().equals(version)) {
		return tool;
	    }
	}
	return null;
    }

    public ArduinoTool getLatestTool(String toolName) {
	ArduinoTool latestTool = null;
	for (ArduinoTool tool : this.tools) {
	    if (tool.getName().equals(toolName)) {
		if (latestTool == null || ArduinoManager.compareVersions(tool.getVersion(), latestTool.getVersion()) > 0) {
		    latestTool = tool;
		}
	    }
	}
	return latestTool;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj instanceof ArduinoPackage) {
	    return ((ArduinoPackage) obj).getName().equals(this.name);
	}
	return super.equals(obj);
    }

    @Override
    public int hashCode() {
	return this.name.hashCode();
    }

}
