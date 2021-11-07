/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json.packages;

import static io.sloeber.core.Gson.GsonConverter.*;

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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.Activator;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;

public class ArduinoPlatform implements Comparable<ArduinoPlatform> {

    private String name;
    private String architecture;
    private VersionNumber version;
    private String category;
    private String url;
    private String archiveFileName;
    private String checksum;
    private String size;
    private List<String> boards = new ArrayList<>();
    private List<ToolDependency> toolsDependencies = new ArrayList<>();;

    private ArduinoPackage myParent;

    private static final String ID_SEPERATOR = "-"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    public ArduinoPlatform(JsonElement json, ArduinoPackage parent) {
        myParent = parent;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            name = getSafeString(jsonObject, "name");
            architecture = getSafeString(jsonObject, "architecture");
            version = getSafeVersion(jsonObject, "version");
            category = getSafeString(jsonObject, "category");
            url = getSafeString(jsonObject, "url");
            archiveFileName = getSafeString(jsonObject, "archiveFileName");
            checksum = getSafeString(jsonObject, "checksum");
            size = getSafeString(jsonObject, "size");
            for (JsonElement curElement : jsonObject.get("boards").getAsJsonArray()) {
                boards.add(getSafeString(curElement.getAsJsonObject(), "name"));
            }
            if (jsonObject.get("toolsDependencies") != null) {
                for (JsonElement curElement : jsonObject.get("toolsDependencies").getAsJsonArray()) {
                    toolsDependencies.add(new ToolDependency(curElement, this));
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse ArduinoPlatform json  " + e.getMessage());
        }
    }

    public ArduinoPackage getParent() {
        return this.myParent;
    }

    public String getName() {
        return this.name;
    }

    public String getArchitecture() {
        return architecture;
    }

    public VersionNumber getVersion() {
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

    public List<ToolDependency> getToolsDependencies() {
        return toolsDependencies;
    }

    public boolean isInstalled() {
        return getBoardsFile().exists();
    }

    public boolean isAVersionOfThisPlatformInstalled() {
        return myParent.isAVersionOfThisPlatformInstalled(name);
    }

    public File getBoardsFile() {
        return getInstallPath().append(Const.BOARDS_FILE_NAME).toFile();
    }

    public File getPlatformFile() {
        return getInstallPath().append(Const.PLATFORM_FILE_NAME).toFile();
    }

    public IPath getInstallPath() {
        IPath stPath = ConfigurationPreferences.getInstallationPathPackages().append(this.myParent.getID())
                .append(Const.ARDUINO_HARDWARE_FOLDER_NAME).append(getID()).append(this.version.toString());
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

    //jaba added the @SuppressWarnings("nls") because I added some debugging stuff
    @SuppressWarnings("nls")
    public IStatus install(IProgressMonitor monitor) {
        // Check if we're installed already
        if (isInstalled()) {
            System.out.println("reusing platform " + name + " " + architecture + "(" + version + ")");
            return Status.OK_STATUS;
        }

        // Download platform archive
        System.out.println("start installing platform " + name + " " + architecture + "(" + version + ")");
        IStatus ret = PackageManager.downloadAndInstall(this, false, monitor);
        System.out.println("done installing platform " + name + " " + architecture + "(" + version + ")");
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
        return boards;
    }

    public String getID() {
        return architecture;
        //        String ID = new String();
        //
        //        if (myParent == null) {
        //            ID = getInstallPath().toString();
        //        } else {
        //            ID = myParent.getName();
        //        }
        //        ID = ID + ID_SEPERATOR + name + ID_SEPERATOR + architecture;
        //
        //        return ID;
    }

    public String getConcattenatedBoardNames() {
        return String.join("\n", getBoardNames()); //$NON-NLS-1$
    }

    public List<ArduinoPlatform> getPlatformVersions() {
        return myParent.getPlatformVersions(name);
    }

    @Override
    public int compareTo(ArduinoPlatform o) {
        return name.compareTo(o.getName());
    }

}