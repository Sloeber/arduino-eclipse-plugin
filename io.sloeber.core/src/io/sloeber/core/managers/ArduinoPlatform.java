/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;

public class ArduinoPlatform {

	private String name;
	private String architecture;
	private String version;
	private String category;
	private String url;
	private String archiveFileName;
	private String checksum;
	private String size;
	private List<Board> boards;
	private List<ToolDependency> toolsDependencies;

	private Package myParent;

	private static final String ID_SEPERATOR = "-"; //$NON-NLS-1$

	void setParent(Package parent) {
		myParent = parent;
		for (Board board : boards) {
			board.setOwners(this);
		}
		if (this.toolsDependencies != null) {
			for (ToolDependency toolDep : toolsDependencies) {
				toolDep.setOwner(this);
			}
		}
	}

	public Package getParent() {
		return this.myParent;
	}

	public String getName() {
		return this.name;
	}

	public String getArchitecture() {
		return this.architecture;
	}

	public String getVersion() {
		return this.version;
	}

	public String getCategory() {
		return this.category;
	}

	public String getUrl() {
		return this.url;
	}

	public String getArchiveFileName() {
		return this.archiveFileName;
	}

	public String getChecksum() {
		return this.checksum;
	}

	public String getSize() {
		return this.size;
	}

	public List<Board> getBoards() {
		return this.boards;
	}


	public List<ToolDependency> getToolsDependencies() {
		return this.toolsDependencies;
	}

	public Tool getTool(String toolName) {
		for (ToolDependency toolDep : this.toolsDependencies) {
			if (toolDep.getName().equals(toolName)) {
				return toolDep.getTool();
			}
		}
		return null;
	}

	public boolean isInstalled() {
		return getBoardsFile().exists();
	}

	public File getBoardsFile() {
		return getInstallPath().append(Const.BOARDS_FILE_NAME).toFile();
	}

	public File getPlatformFile() {
		return getInstallPath().append(Const.PLATFORM_FILE_NAME).toFile();
	}

	public IPath getInstallPath() {
		IPath stPath = ConfigurationPreferences.getInstallationPathPackages().append(this.myParent.getName())
				.append(Const.ARDUINO_HARDWARE_FOLDER_NAME).append(this.architecture).append(this.version);
		return stPath;
	}

	public List<IPath> getIncludePath() {
		IPath installPath = getInstallPath();
		return Arrays.asList(installPath.append("cores/{build.core}"), //$NON-NLS-1$
				installPath.append(Const.VARIANTS_FOLDER_NAME + "/{build.variant}")); //$NON-NLS-1$
	}

	@SuppressWarnings("unused")
	public IStatus remove(IProgressMonitor monitor) {
		// Check if we're installed
		if (!isInstalled()) {
			return Status.OK_STATUS;
		}

		try {
			FileUtils.deleteDirectory(getInstallPath().toFile());
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.getId(), "Failed to remove folder" + getInstallPath().toString(), //$NON-NLS-1$
					e);
		}

		return Status.OK_STATUS;
	}

	//jaba added the @SuppressWarnings("nls") because I added some debugging stuff
	@SuppressWarnings("nls")
	public IStatus install(IProgressMonitor monitor) {
		// Check if we're installed already
		if (isInstalled()) {
			System.out.println("reusing platform "+name + " "+architecture +"("+version+")");
			return Status.OK_STATUS;
		}

		// Download platform archive
		System.out.println("start installing platform "+name + " "+architecture +"("+version+")");
		IStatus ret= InternalPackageManager.downloadAndInstall(this, false, monitor);
		System.out.println("done installing platform "+name + " "+architecture +"("+version+")");
		return ret;

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
		result = prime * result + ((this.myParent == null) ? 0 : this.myParent.hashCode());
		result = prime * result + ((this.version == null) ? 0 : this.version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArduinoPlatform other = (ArduinoPlatform) obj;
		if (this.name == null) {
			if (other.name != null)
				return false;
		} else if (!this.name.equals(other.name))
			return false;
		if (this.myParent == null) {
			if (other.myParent != null)
				return false;
		} else if (!this.myParent.equals(other.myParent))
			return false;
		if (this.version == null) {
			if (other.version != null)
				return false;
		} else if (!this.version.equals(other.version))
			return false;
		return true;
	}

	public List<String> getBoardNames() {
		List<String> ret = new ArrayList<>();
		for (Board curBoar : this.boards) {
				ret.add(curBoar.getName());
			}
		return ret;
	}


	public String getID() {
		String ID=new String();
	
		if (myParent==null) {
			ID=getInstallPath().toOSString();
		}
		else {
			ID=myParent.getName();
		}
		ID=ID+ID_SEPERATOR+name+ID_SEPERATOR+architecture;
				
		return ID;
	}

}
