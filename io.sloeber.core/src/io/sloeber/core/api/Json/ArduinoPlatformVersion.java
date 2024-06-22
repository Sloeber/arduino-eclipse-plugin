/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatformVersion extends ArduinoInstallable implements Comparable<ArduinoPlatformVersion> {

    private String architecture;
    private VersionNumber version;
    private String category;

    private List<String> boards = new ArrayList<>();
    private List<ArduinoPlatformTooldDependency> toolsDependencies = new ArrayList<>();;

    private ArduinoPlatform myParent;

    @SuppressWarnings("nls")
    public ArduinoPlatformVersion(JsonElement json, ArduinoPlatform parent) {
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
                    toolsDependencies.add(new ArduinoPlatformTooldDependency(curElement, this));
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse ArduinoPlatform json  " + e.getMessage(),e);
        }
    }

    public ArduinoPlatform getParent() {
        return myParent;
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

    public List<ArduinoPlatformTooldDependency> getToolsDependencies() {
        return toolsDependencies;
    }

    public boolean isInstalled() {
        return getBoardsFile().exists();
    }

    public File getBoardsFile() {
        return getInstallPath().append(BOARDS_FILE_NAME).toFile();
    }

    public File getPlatformFile() {
        return getInstallPath().append(PLATFORM_FILE_NAME).toFile();
    }

    @Override
    public IPath getInstallPath() {
        return getParent().getInstallPath().append(this.version.toString());
    }

    public List<IPath> getIncludePath() {
        IPath installPath = getInstallPath();
        return Arrays.asList(installPath.append("cores/{build.core}"), //$NON-NLS-1$
                installPath.append(ARDUINO_VARIANTS_FOLDER_NAME + "/{build.variant}")); //$NON-NLS-1$
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
        ArduinoPlatformVersion other = (ArduinoPlatformVersion) obj;
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
        return version.toString();
    }

    public String getConcattenatedBoardNames() {
        return String.join("\n", getBoardNames()); //$NON-NLS-1$
    }

    @Override
    public int compareTo(ArduinoPlatformVersion o) {
        return name.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return name + SPACE + architecture + '(' + version + ')';
    }

}
