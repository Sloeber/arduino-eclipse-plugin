package it.baeyens.arduino.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.ui.Activator;

public class ArduinoLibrary implements Comparator<ArduinoLibrary> {

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

    public Path getInstallPath() {
	return Paths.get(ConfigurationPreferences.getInstallationPath().append(ArduinoConst.LIBRARY_PATH_SUFFIX).append(this.name.replace(' ', '_'))
		.append(this.version).toString());
    }

    public boolean isInstalled() {
	return getInstallPath().toFile().exists();
    }

    public IStatus install(IProgressMonitor monitor) {
	monitor.setTaskName("Downloading and installing " + getName() + " library."); //$NON-NLS-1$ //$NON-NLS-2$
	if (isInstalled()) {
	    return Status.OK_STATUS;
	}

	return ArduinoManager.downloadAndInstall(this.url, this.archiveFileName, getInstallPath(), false, monitor);
    }

    public Collection<Path> getIncludePath() {
	Path installPath = getInstallPath();
	Path srcPath = installPath.resolve("src"); //$NON-NLS-1$
	if (srcPath.toFile().isDirectory()) {
	    return Collections.singletonList(srcPath);
	}
	// TODO do I need the 'utility' directory?
	return Collections.singletonList(installPath);

    }

    private void getSources(IProject project, Collection<Path> sources, Path dir, boolean recurse) {
	for (File file : dir.toFile().listFiles()) {
	    if (file.isDirectory()) {
		if (recurse) {
		    getSources(project, sources, file.toPath(), recurse);
		}
	    } else {
		if (CoreModel.isValidSourceUnitName(project, file.getName())) {
		    sources.add(file.toPath());
		}
	    }
	}
    }

    public Collection<Path> getSources(IProject project) {
	List<Path> sources = new ArrayList<>();
	Path installPath = getInstallPath();
	Path srcPath = installPath.resolve("src"); //$NON-NLS-1$
	if (srcPath.toFile().isDirectory()) {
	    getSources(project, sources, srcPath, true);
	} else {
	    getSources(project, sources, installPath, false);
	    Path utilityPath = installPath.resolve("utility"); //$NON-NLS-1$
	    if (utilityPath.toFile().isDirectory()) {
		getSources(project, sources, utilityPath, false);
	    }
	}
	return sources;
    }

    @Override
    public int compare(ArduinoLibrary o1, ArduinoLibrary o2) {
	return o1.getName().compareTo(o2.getName());
    }

    public IStatus remove(IProgressMonitor monitor) {
	if (!isInstalled()) {
	    return Status.OK_STATUS;
	}

	try {
	    FileUtils.deleteDirectory(getInstallPath().toFile());
	} catch (IOException e) {
	    return new Status(IStatus.ERROR, Activator.getId(), "Failed to remove folder" + getInstallPath().toString(), e); //$NON-NLS-1$
	}

	return Status.OK_STATUS;
    }

}
