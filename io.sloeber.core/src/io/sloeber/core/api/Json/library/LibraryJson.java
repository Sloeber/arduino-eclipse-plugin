package io.sloeber.core.api.Json.library;

import static io.sloeber.core.Gson.GsonConverter.*;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.Activator;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.tools.FileModifiers;

/**
 * This class represents an entry ina a library json file
 *
 * @author jan
 *
 */

public class LibraryJson implements Comparable<LibraryJson> {

    private String name;
    private VersionNumber version;
    private String author;
    private String maintainer;
    private String sentence;
    private String paragraph;
    private String website;
    private String category;
    private List<String> architectures = new ArrayList<>();
    private List<String> types = new ArrayList<>();
    private String url;
    private String archiveFileName;
    private int size;
    private String checksum;
    public static final String LIBRARY_SOURCE_FODER = "src"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    public LibraryJson(JsonElement json, LibraryIndexJson libraryIndexJson) {
        JsonObject jsonObject = json.getAsJsonObject();
        try {
            name = getSafeString(jsonObject, "name");
            version = getSafeVersion(jsonObject, "version");
            author = getSafeString(jsonObject, "author");
            maintainer = getSafeString(jsonObject, "maintainer");
            sentence = getSafeString(jsonObject, "sentence");
            paragraph = getSafeString(jsonObject, "paragraph");
            website = getSafeString(jsonObject, "website");
            category = getSafeString(jsonObject, "category");
            for (JsonElement curType : jsonObject.get("architectures").getAsJsonArray()) {
                architectures.add(curType.getAsString());
            }
            for (JsonElement curType : jsonObject.get("types").getAsJsonArray()) {
                types.add(curType.getAsString());
            }
            url = getSafeString(jsonObject, "url");
            archiveFileName = getSafeString(jsonObject, "archiveFileName");
            size = jsonObject.get("size").getAsInt();
            checksum = getSafeString(jsonObject, "checksum");
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage());
        }

    }

    public String getName() {
        return name;
    }

    public VersionNumber getVersion() {
        return version;
    }

    public String getAuthor() {
        return author;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public String getSentence() {
        return sentence;
    }

    public String getParagraph() {
        return paragraph;
    }

    public String getWebsite() {
        return website;
    }

    public String getCategory() {
        return category;
    }

    public List<String> getArchitectures() {
        return architectures;
    }

    public List<String> getTypes() {
        return types;
    }

    public String getUrl() {
        return url;
    }

    public String getArchiveFileName() {
        return archiveFileName;
    }

    public int getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathLibraries().append(this.name.replace(' ', '_'))
                .append(version.toString());
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
        File rootFolder = getInstallPath().toFile().getParentFile();
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
        IStatus ret = PackageManager.downloadAndInstall(this.url, this.archiveFileName, getInstallPath(), false,
                monitor);
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
    public int compareTo(LibraryJson other) {
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
