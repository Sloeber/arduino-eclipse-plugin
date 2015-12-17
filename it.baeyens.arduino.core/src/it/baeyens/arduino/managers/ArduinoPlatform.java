/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package it.baeyens.arduino.managers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.ui.Activator;

public class ArduinoPlatform {

	private String name;
	private String architecture;
	private String version;
	private String category;
	private String url;
	private String archiveFileName;
	private String checksum;
	private String size;
	private List<ArduinoBoard> boards;
	private List<ToolDependency> toolsDependencies;

	private ArduinoPackage pkg;
	private HierarchicalProperties boardsFile;
	private Properties platformProperties;

	void setOwner(ArduinoPackage pkg) {
		this.pkg = pkg;
		for (ArduinoBoard board : boards) {
			board.setOwners(this);
		}
		for (ToolDependency toolDep : toolsDependencies) {
			toolDep.setOwner(this);
		}
	}

	public ArduinoPackage getPackage() {
		return pkg;
	}

	public String getName() {
		return name;
	}

	public String getArchitecture() {
		return architecture;
	}

	public String getVersion() {
		return version;
	}

	public String getCategory() {
		return category;
	}

	public String getUrl() {
		return url;
	}

	public String getArchiveFileName() {
		return archiveFileName;
	}

	public String getChecksum() {
		return checksum;
	}

	public String getSize() {
		return size;
	}

	public List<ArduinoBoard> getBoards() throws CoreException {
		if (isInstalled() && boardsFile == null) {
			Properties boardProps = new Properties();
			try (Reader reader = new FileReader(getInstallPath().resolve("boards.txt").toFile())) { //$NON-NLS-1$
				boardProps.load(reader);
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), "Loading boards.txt", e));
			}

			boardsFile = new HierarchicalProperties(boardProps);

			// Replace the boards with a real ones
			boards = new ArrayList<>();
			for (Map.Entry<String, HierarchicalProperties> entry : boardsFile.getChildren().entrySet()) {
				if (entry.getValue().getChild("name") != null) { //$NON-NLS-1$
					// assume things with names are boards
					boards.add(new ArduinoBoard(entry.getKey(), entry.getValue()).setOwners(this));
				}
			}
		}
		return boards;
	}

	public ArduinoBoard getBoard(String name) throws CoreException {
		for (ArduinoBoard board : getBoards()) {
			if (name.equals(board.getName())) {
				return board;
			}
		}
		return null;
	}

	public List<ToolDependency> getToolsDependencies() {
		return toolsDependencies;
	}

	public ArduinoTool getTool(String name) throws CoreException {
		for (ToolDependency toolDep : toolsDependencies) {
			if (toolDep.getName().equals(name)) {
				return toolDep.getTool();
			}
		}
		return null;
	}

	public Properties getPlatformProperties() throws CoreException {
		if (platformProperties == null) {
			platformProperties = new Properties();
			try (BufferedReader reader = new BufferedReader(
					new FileReader(getInstallPath().resolve("platform.txt").toFile()))) { //$NON-NLS-1$
				// There are regex's here and need to preserve the \'s
				StringBuffer buffer = new StringBuffer();
				for (String line = reader.readLine(); line != null; line = reader.readLine()) {
					buffer.append(line.replace("\\", "\\\\")); //$NON-NLS-1$ //$NON-NLS-2$
					buffer.append('\n');
				}
				try (Reader reader1 = new StringReader(buffer.toString())) {
					platformProperties.load(reader1);
				}
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), "Loading platform.txt", e));
			}
		}
		return platformProperties;
	}

	public boolean isInstalled() {
		return getInstallPath().resolve("boards.txt").toFile().exists(); //$NON-NLS-1$
	}

	public Path getInstallPath() {
		return ArduinoPreferences.getArduinoHome().resolve("hardware").resolve(pkg.getName()).resolve(architecture) //$NON-NLS-1$
				.resolve(version);
	}

	public List<Path> getIncludePath() {
		Path installPath = getInstallPath();
		return Arrays.asList(installPath.resolve("cores/{build.core}"), //$NON-NLS-1$
				installPath.resolve("variants/{build.variant}")); //$NON-NLS-1$
	}

	public IStatus install(IProgressMonitor monitor) {
		// Check if we're installed already
		if (isInstalled()) {
			return Status.OK_STATUS;
		}

		// Download platform archive
		IStatus status = ArduinoManager.downloadAndInstall(url, archiveFileName, getInstallPath(), monitor);
		if (!status.isOK()) {
			return status;
		}

		// Install the tools
		MultiStatus mstatus = null;
		for (ToolDependency toolDep : toolsDependencies) {
			status = toolDep.install(monitor);
			if (!status.isOK()) {
				if (mstatus == null) {
					mstatus = new MultiStatus(status.getPlugin(), status.getCode(), status.getMessage(),
							status.getException());
				} else {
					mstatus.add(status);
				}
			}
		}

		// On Windows install make from equations.org
		try {
			Path makePath = ArduinoPreferences.getArduinoHome().resolve("tools/make/make.exe");
			if (!makePath.toFile().exists()) {
				Files.createDirectories(makePath.getParent());
				URL makeUrl = new URL("ftp://ftp.equation.com/make/32/make.exe");
				Files.copy(makeUrl.openStream(), makePath);
				makePath.toFile().setExecutable(true, false);
			}

		} catch (IOException e) {
			mstatus.add(new Status(IStatus.ERROR, Activator.getId(), "downloading make.exe", e));
		}

		return mstatus != null ? mstatus : Status.OK_STATUS;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((pkg == null) ? 0 : pkg.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (pkg == null) {
			if (other.pkg != null)
				return false;
		} else if (!pkg.equals(other.pkg))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

}
