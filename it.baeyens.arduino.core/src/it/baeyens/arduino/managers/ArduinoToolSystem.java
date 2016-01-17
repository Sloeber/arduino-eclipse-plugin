/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

public class ArduinoToolSystem {

    private String host;
    private String archiveFileName;
    private String url;
    private String checksum;
    private String size;

    private transient ArduinoTool tool;

    public void setOwner(ArduinoTool tool) {
	this.tool = tool;
    }

    public String getHost() {
	return this.host;
    }

    public String getArchiveFileName() {
	return this.archiveFileName;
    }

    public String getUrl() {
	return this.url;
    }

    public String getChecksum() {
	return this.checksum;
    }

    public String getSize() {
	return this.size;
    }

    public boolean isApplicable() {
	switch (Platform.getOS()) {
	case Platform.OS_WIN32:
	    return "i686-mingw32".equals(this.host); //$NON-NLS-1$
	case Platform.OS_MACOSX:
	    switch (this.host) {
	    case "i386-apple-darwin11": //$NON-NLS-1$
	    case "x86_64-apple-darwin": //$NON-NLS-1$
		return true;
	    default:
		return false;
	    }
	case Platform.OS_LINUX:
	    switch (Platform.getOSArch()) {
	    case Platform.ARCH_X86_64:
		return "x86_64-linux-gnu".equals(this.host) || "x86_64-pc-linux-gnu".equals(this.host); //$NON-NLS-1$ //$NON-NLS-2$
	    case Platform.ARCH_X86:
		return "i686-linux-gnu".equals(this.host) || "i686-pc-linux-gnu".equals(this.host); //$NON-NLS-1$ //$NON-NLS-2$
	    default:
		return false;
	    }
	default:
	    return false;
	}
    }

    public IStatus install(IProgressMonitor monitor) {
	return ArduinoManager.downloadAndInstall(this.url, this.archiveFileName, this.tool.getInstallPath(), false, monitor);
    }

}
