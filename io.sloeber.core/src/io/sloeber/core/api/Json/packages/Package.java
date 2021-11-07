/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json.packages;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class Package implements Comparable<Package> {

    private String name;
    private String maintainer;
    private String websiteURL;
    private String email;
    private String helpOnline;
    private List<ArduinoPlatform> platforms = new ArrayList<>();
    private List<Tool> tools = new ArrayList<>();
    private transient PackageIndex myParent = null;

    @SuppressWarnings("nls")
    public Package(JsonElement json, PackageIndex packageIndex) {
        myParent = packageIndex;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            name = getSafeString(jsonObject, "name");
            maintainer = getSafeString(jsonObject, "maintainer");
            websiteURL = getSafeString(jsonObject, "websiteURL");
            email = getSafeString(jsonObject, "email");
            helpOnline = getSafeString(jsonObject, "help", "online");
            for (JsonElement curElement : jsonObject.get("platforms").getAsJsonArray()) {
                platforms.add(new ArduinoPlatform(curElement, this));
            }
            for (JsonElement curElement : jsonObject.get("tools").getAsJsonArray()) {
                tools.add(new Tool(curElement, this));
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse Package json  " + e.getMessage());
        }
    }

    public PackageIndex getParent() {
        return myParent;
    }

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

    public String getHelp() {
        return this.helpOnline;
    }

    public List<ArduinoPlatform> getPlatforms() {
        return this.platforms;
    }

    /**
     * Only the latest versions of the platforms.
     * Arduino IDE does not do this based on the name.
     * I hope it is based on the architecture because this code is now
     *
     * @return latest platforms
     */
    public Collection<ArduinoPlatform> getLatestPlatforms() {
        Map<String, ArduinoPlatform> platformMap = new HashMap<>();
        for (ArduinoPlatform platform : this.platforms) {
            ArduinoPlatform p = platformMap.get(platform.getArchitecture());
            if (p == null || platform.getVersion().compareTo(p.getVersion()) > 0) {
                platformMap.put(platform.getArchitecture(), platform);
            }
        }
        return Collections.unmodifiableCollection(platformMap.values());
    }

    /**
     * This method looks up the installed platforms with the highest version
     * number So if you have 2 arduino avr platform versions installed you will
     * only get back 1.
     *
     * @return the installed platforms but only one for each platform (the one
     *         with the highest version number)
     */
    public Collection<ArduinoPlatform> getLatestInstalledPlatforms() {
        Map<String, ArduinoPlatform> platformMap = new HashMap<>();
        for (ArduinoPlatform platform : this.platforms) {
            if (platform.isInstalled()) {
                ArduinoPlatform p = platformMap.get(platform.getID());
                if (p == null || platform.getVersion().compareTo(p.getVersion()) > 0) {
                    platformMap.put(platform.getID(), platform);
                }
            }
        }
        return Collections.unmodifiableCollection(platformMap.values());
    }

    /**
     * This method looks up the installed platforms So if you have 2 arduino avr
     * platform versions installed you will get back 2.
     *
     * @return all the installed platforms
     */
    public List<ArduinoPlatform> getInstalledPlatforms() {
        List<ArduinoPlatform> platformMap = new LinkedList<>();
        for (ArduinoPlatform platform : this.platforms) {
            if (platform.isInstalled()) {
                platformMap.add(platform);
            }
        }
        return platformMap;
    }

    public ArduinoPlatform getLatestPlatform(String architectureName, boolean mustBeInstalled) {
        ArduinoPlatform foundPlatform = null;
        for (ArduinoPlatform platform : this.platforms) {
            if (!mustBeInstalled || platform.isInstalled()) {
                if (architectureName.equals(platform.getArchitecture())) {
                    if (foundPlatform == null) {
                        foundPlatform = platform;
                    } else {
                        if (platform.getVersion().compareTo(foundPlatform.getVersion()) > 0) {
                            foundPlatform = platform;
                        }
                    }
                }
            }
        }
        return foundPlatform;
    }

    public ArduinoPlatform getPlatform(String platformName, String version) {

        for (ArduinoPlatform platform : this.platforms) {
            if (platform.getName().equals(platformName)) {
                if (platform.getVersion().compareTo(version) == 0) {
                    return platform;
                }
            }
        }
        return null;
    }

    public List<Tool> getTools() {
        return this.tools;
    }

    public Tool getTool(String toolName, String version) {
        for (Tool tool : this.tools) {
            if (tool.getName().trim().equals(toolName) && tool.getVersion().equals(version)) {
                return tool;
            }
        }
        return null;
    }

    public Tool getLatestTool(String toolName) {
        Tool latestTool = null;
        for (Tool tool : this.tools) {
            if (tool.getName().equals(toolName)) {
                if (latestTool == null || tool.getVersion().compareTo(latestTool.getVersion()) > 0) {
                    latestTool = tool;
                }
            }
        }
        return latestTool;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Package) {
            return ((Package) obj).getName().equals(this.name);
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public int compareTo(Package other) {
        return this.name.compareTo(other.name);
    }

    public void onlyKeepLatestPlatforms() {
        Collection<ArduinoPlatform> latestPlatforms = getLatestPlatforms();
        for (ArduinoPlatform curplatform : this.platforms) {
            if (!latestPlatforms.contains(curplatform)) {
                curplatform.remove(null);
            }
        }
    }

    public boolean hasInstalledBoards() {
        for (ArduinoPlatform platform : this.platforms) {
            if (platform.isInstalled()) {
                return true;
            }
        }
        return false;
    }

}
