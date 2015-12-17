/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.ui.Activator;

public class ToolDependency {

	private String packager;
	private String name;
	private String version;

	private transient ArduinoPlatform platform;

	public void setOwner(ArduinoPlatform platform) {
		this.platform = platform;
	}

	public String getPackager() {
		return packager;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public ArduinoTool getTool() throws CoreException {
		ArduinoPackage pkg = platform.getPackage();
		if (!pkg.getName().equals(packager)) {
			pkg = ArduinoManager.getPackage(packager);
		}

		return pkg.getTool(name, version);
	}

	public IStatus install(IProgressMonitor monitor) {
		try {
			ArduinoTool tool = getTool();
			if (tool == null) {
				return new Status(IStatus.ERROR, Activator.getId(),
						String.format("Tool not found %s %s", name, version));
			}
			return getTool().install(monitor);
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

}
