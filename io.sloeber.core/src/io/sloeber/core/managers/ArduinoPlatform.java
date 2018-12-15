/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.managers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
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

	private Package pkg;
	private HierarchicalProperties boardsFile;
	private Properties platformProperties;
	private static final String PLATFORM_FILE_NAME = "platform.txt"; //$NON-NLS-1$

	void setOwner(Package pkg) {
		this.pkg = pkg;
		for (Board board : this.boards) {
			board.setOwners(this);
		}
		if (this.toolsDependencies != null) {
			for (ToolDependency toolDep : this.toolsDependencies) {
				toolDep.setOwner(this);
			}
		}
	}

	public Package getPackage() {
		return this.pkg;
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
		if (isInstalled() && this.boardsFile == null) {
			Properties boardProps = new Properties();
			try (Reader reader = new FileReader(getBoardsFile())) {
				boardProps.load(reader);
			} catch (IOException e) {
				Common.log(new Status(IStatus.ERROR, Activator.getId(), Messages.Platform_loading_boards, e));
				return this.boards;
			}

			this.boardsFile = new HierarchicalProperties(boardProps);

			// Replace the boards with a real ones
			this.boards = new ArrayList<>();
			for (Map.Entry<String, HierarchicalProperties> entry : this.boardsFile.getChildren().entrySet()) {
				if ((entry.getValue().getChild("name") != null) && (entry.getKey() != null)) { //$NON-NLS-1$
					// assume things with name and id are boards
					this.boards.add(new Board(entry.getKey(), entry.getValue()).setOwners(this));
				}
			}
		}
		return this.boards;
	}

	public Board getBoard(String boardName) {
		for (Board board : getBoards()) {
			if (boardName.equals(board.getName())) {
				return board;
			}
		}
		return null;
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

	public Properties getPlatformProperties() throws CoreException {
		if (this.platformProperties == null) {
			this.platformProperties = new Properties();
			try (BufferedReader reader = new BufferedReader(new FileReader(getPlatformFile()))) {
				// There are regex's here and need to preserve the \'s
				StringBuilder builder = new StringBuilder();
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					builder.append(line.replace("\\", "\\\\")); //$NON-NLS-1$ //$NON-NLS-2$
					builder.append('\n');
				}
				try (Reader reader1 = new StringReader(builder.toString())) {
					this.platformProperties.load(reader1);
				}
			} catch (IOException e) {
				throw new CoreException(
						new Status(IStatus.ERROR, Activator.getId(), Messages.Platform_loading_platform, e));
			}
		}
		return this.platformProperties;
	}

	public boolean isInstalled() {
		return getBoardsFile().exists();
	}

	public File getBoardsFile() {
		return getInstallPath().append(Const.BOARDS_FILE_NAME).toFile();
	}

	public File getPlatformFile() {
		return getInstallPath().append(PLATFORM_FILE_NAME).toFile();
	}

	public IPath getInstallPath() {
		IPath stPath = ConfigurationPreferences.getInstallationPathPackages().append(this.pkg.getName())
				.append(Const.ARDUINO_HARDWARE_FOLDER_NAME).append(this.architecture).append(this.version);
		return stPath;
	}

	public List<IPath> getIncludePath() {
		IPath installPath = getInstallPath();
		return Arrays.asList(installPath.append("cores/{build.core}"), //$NON-NLS-1$
				installPath.append(Const.VARIANTS_FOLDER_NAME + "/{build.variant}")); //$NON-NLS-1$
	}

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

	public IStatus install(IProgressMonitor monitor) {
		// Check if we're installed already
		if (isInstalled()) {
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
		result = prime * result + ((this.pkg == null) ? 0 : this.pkg.hashCode());
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
		if (this.pkg == null) {
			if (other.pkg != null)
				return false;
		} else if (!this.pkg.equals(other.pkg))
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

	public List<String> getBoardIDs() {
		List<String> ret = new ArrayList<>();
		for (Board curBoar : this.boards) {
			ret.add(curBoar.getId());
		}
		return ret;
	}

}
