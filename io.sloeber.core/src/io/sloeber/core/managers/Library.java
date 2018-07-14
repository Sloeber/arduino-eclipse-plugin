package io.sloeber.core.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.tools.FileModifiers;

/**
 * This class represents an entry ina a library json file
 *
 * @author jan
 *
 */
public class Library implements Comparable<Library> {

	private String name;
	private String version;
	private String author;
	private String maintainer;
	private String sentence;
	private String paragraph;
	private String website;
	private String category;
	private List<String> architectures;
	private List<String> types;
	private String url;
	private String archiveFileName;
	private int size;
	private String checksum;
	public static final String LIBRARY_SOURCE_FODER = "src"; //$NON-NLS-1$

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAuthor() {
		return this.author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getMaintainer() {
		return this.maintainer;
	}

	public void setMaintainer(String maintainer) {
		this.maintainer = maintainer;
	}

	public String getSentence() {
		return this.sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public String getParagraph() {
		return this.paragraph;
	}

	public void setParagraph(String paragraph) {
		this.paragraph = paragraph;
	}

	public String getWebsite() {
		return this.website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getCategory() {
		return this.category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public List<String> getArchitectures() {
		return this.architectures;
	}

	public void setArchitectures(List<String> architectures) {
		this.architectures = architectures;
	}

	public List<String> getTypes() {
		return this.types;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getArchiveFileName() {
		return this.archiveFileName;
	}

	public void setArchiveFileName(String archiveFileName) {
		this.archiveFileName = archiveFileName;
	}

	public int getSize() {
		return this.size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getChecksum() {
		return this.checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public IPath getInstallPath() {
		return ConfigurationPreferences.getInstallationPathLibraries().append(this.name.replace(' ', '_'))
				.append(this.version);
	}

	public boolean isInstalled() {
		return getInstallPath().toFile().exists();
	}

	/**
	 * checks if any version of this library is installed. Can popup a window if
	 * there is something wrong with the folder structure
	 *
	 * @return false if any version is installed. true in case of error and in case
	 *         no version is installed
	 */
	public boolean isAVersionInstalled() {
		File rootFolder=getInstallPath().toFile().getParentFile();
		if (!rootFolder.exists()) {
			return false;
		}
		if (rootFolder.isFile()) {
			// something is wrong here
			Common.log(new Status(IStatus.ERROR, Activator.getId(),
					rootFolder + " is a file but it should be a directory.")); //$NON-NLS-1$
			return false;
		}
		return rootFolder.list().length > 0;
	}

	public IStatus install(IProgressMonitor monitor) {
		monitor.setTaskName("Downloading and installing " + getName() + " library."); //$NON-NLS-1$ //$NON-NLS-2$
		if (isInstalled()) {
			return Status.OK_STATUS;
		}
		IStatus ret = InternalPackageManager.downloadAndInstall(this.url, this.archiveFileName, getInstallPath(), false, monitor);
		FileModifiers.addPragmaOnce(getInstallPath());
		return ret;
	}

	public Collection<IPath> getIncludePath() {
		IPath installPath = getInstallPath();
		IPath srcPath = installPath.append(LIBRARY_SOURCE_FODER);
		if (srcPath.toFile().isDirectory()) {
			return Collections.singletonList(srcPath);
		}
		return Collections.singletonList(installPath);

	}

	private void getSources(IProject project, Collection<IPath> sources, File dir, boolean recurse) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				if (recurse) {
					getSources(project, sources, file, recurse);
				}
			} else {
				if (CoreModel.isValidSourceUnitName(project, file.getName())) {
					sources.add(new Path(file.toString()));
				}
			}
		}
	}

	public Collection<IPath> getSources(IProject project) {
		List<IPath> sources = new ArrayList<>();
		IPath installPath = getInstallPath();
		File srcPath = installPath.append(LIBRARY_SOURCE_FODER).toFile();
		if (srcPath.isDirectory()) {
			getSources(project, sources, srcPath, true);
		} else {
			getSources(project, sources, installPath.toFile(), false);
			File utilityPath = installPath.append("utility").toFile(); //$NON-NLS-1$
			if (utilityPath.isDirectory()) {
				getSources(project, sources, utilityPath, false);
			}
		}
		return sources;
	}

	@Override
	public int compareTo(Library other) {
		return this.name.compareTo(other.name);
	}

	/**
	 * delete the library This will delete all installed versions of the library.
	 * Normally only 1 version can be installed so deleting all versions should be
	 * delete 1 version
	 *
	 * @param monitor
	 * @return Status.OK_STATUS if delete is successful otherwise IStatus.ERROR
	 */
	public IStatus remove(IProgressMonitor monitor) {
		if (!isInstalled()) {
			return Status.OK_STATUS;
		}

		try {
			FileUtils.deleteDirectory(getInstallPath().toFile().getParentFile());
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.getId(), "Failed to remove folder" + getInstallPath().toString(), //$NON-NLS-1$
					e);
		}

		return Status.OK_STATUS;
	}

}
