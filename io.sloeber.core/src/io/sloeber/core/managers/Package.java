/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Package implements Comparable<Package> {

	private String name;
	private String maintainer;
	private String websiteURL;
	private String email;
	private Help help;
	private List<ArduinoPlatform> platforms;
	private List<Tool> tools;

	void setOwner(Manager manager) {
		// it happened that the list contained a null so I remove null platforms
		this.platforms.remove(null);
		for (ArduinoPlatform platform : this.platforms) {
			platform.setOwner(this);
		}
		if (this.tools != null) {
			for (Tool tool : this.tools) {
				tool.setOwner(this);
			}
		}
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
	
	public Help getHelp() {
		return this.help;
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
			if (p == null || Manager.compareVersions(platform.getVersion(), p.getVersion()) > 0) {
				platformMap.put(platform.getName(), platform);
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
	public Collection<ArduinoPlatform> getInstalledPlatforms() {
		Map<String, ArduinoPlatform> platformMap = new HashMap<>();
		for (ArduinoPlatform platform : this.platforms) {
			if (platform.isInstalled()) {
				ArduinoPlatform p = platformMap.get(platform.getName());
				if (p == null || Manager.compareVersions(platform.getVersion(), p.getVersion()) > 0) {
					platformMap.put(platform.getName(), platform);
				}
			}
		}
		return Collections.unmodifiableCollection(platformMap.values());
	}

	public ArduinoPlatform getLatestPlatform(String platformName) {
		ArduinoPlatform foundPlatform = null;
		for (ArduinoPlatform platform : this.platforms) {
			if (platform.getName().equals(platformName)) {
				if (foundPlatform == null) {
					foundPlatform = platform;
				} else {
					if (Manager.compareVersions(platform.getVersion(), foundPlatform.getVersion()) > 0) {
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
				if (Manager.compareVersions(platform.getVersion(), version) == 0) {
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
				if (latestTool == null || Manager.compareVersions(tool.getVersion(), latestTool.getVersion()) > 0) {
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

}
