/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.managers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;

public class ToolDependency {

	private static final CharSequence NAME = Messages.NAME;
	private static final CharSequence VERSION =Messages.VERSION;
	private String packager;
	private String name;
	private String version;

	private transient ArduinoPlatform platform;

	public void setOwner(ArduinoPlatform platform) {
		this.platform = platform;
	}

	public String getPackager() {
		return this.packager;
	}

	public String getName() {
		return this.name;
	}

	public String getVersion() {
		return this.version;
	}

	public Tool getTool() {
		Package pkg = this.platform.getPackage();
		if (!pkg.getName().equals(this.packager)) {
			pkg = InternalPackageManager.getPackage(this.packager);
		}
		if(pkg==null) {
			return null;
		}
		return pkg.getTool(this.name, getVersion());
	}

	public IStatus install(IProgressMonitor monitor) {
		Tool tool = getTool();
		if (tool == null) {
			return new Status(IStatus.ERROR, Activator.getId(),
					Messages.ToolDependency_Tool_not_found.replace(NAME, this.name).replace(VERSION, this.version));
		}
		return tool.install(monitor);
	}

}
