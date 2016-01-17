/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.ui.Activator;

public class ArduinoTool {

    private String name;
    private String version;
    private List<ArduinoToolSystem> systems;

    private transient ArduinoPackage pkg;

    public void setOwner(ArduinoPackage pkg) {
	this.pkg = pkg;
	for (ArduinoToolSystem system : this.systems) {
	    system.setOwner(this);
	}
    }

    public ArduinoPackage getPackage() {
	return this.pkg;
    }

    public String getName() {
	return this.name;
    }

    public String getVersion() {
	return this.version;
    }

    public List<ArduinoToolSystem> getSystems() {
	return this.systems;
    }

    public Path getInstallPath() {
	return Paths.get(ConfigurationPreferences.getInstallationPath().append("tools").append(this.pkg.getName()).append(this.name) //$NON-NLS-1$
		.append(this.version).toString());
    }

    public boolean isInstalled() {
	return getInstallPath().toFile().exists();
    }

    public IStatus install(IProgressMonitor monitor) {
	if (isInstalled()) {
	    return Status.OK_STATUS;
	}

	for (ArduinoToolSystem system : this.systems) {
	    if (system.isApplicable()) {
		return system.install(monitor);
	    }
	}

	// No valid system
	return new Status(IStatus.ERROR, Activator.getId(), Messages.ArduinoTool_no_valid_system + this.name);
    }

    // public Properties getToolProperties() {
    // Properties properties = new Properties();
    // properties.put("runtime.tools." + name + ".path", ArduinoBuildConfiguration.pathString(getInstallPath())); // $NON-NLS-1$ //$NON-NLS-2$
    // return properties;
    // }

}
