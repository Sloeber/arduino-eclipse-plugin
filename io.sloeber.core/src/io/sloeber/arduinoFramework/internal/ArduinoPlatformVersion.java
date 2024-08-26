/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.arduinoFramework.api.IArduinoPlatformVersion;
import io.sloeber.core.api.VersionNumber;

public class ArduinoPlatformVersion  extends IArduinoPlatformVersion {

    private String myArchitecture;
    private VersionNumber myVersion;
    private String myCategory;

    private List<String> myBoards = new ArrayList<>();
    private List<ArduinoPlatformTooldDependency> myToolDependencies = new ArrayList<>();

    private ArduinoPlatform myParent;

    @SuppressWarnings("nls")
    public ArduinoPlatformVersion(JsonElement json, ArduinoPlatform parent) {
        myParent = parent;
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            myName = getSafeString(jsonObject, "name");
            myArchitecture = getSafeString(jsonObject, "architecture");
            myVersion = getSafeVersion(jsonObject, "version");
            myCategory = getSafeString(jsonObject, "category");
            myURL = getSafeString(jsonObject, "url");
            myArchiveFileName = getSafeString(jsonObject, "archiveFileName");
            myChecksum = getSafeString(jsonObject, "checksum");
            mySize = getSafeString(jsonObject, "size");
            for (JsonElement curElement : jsonObject.get("boards").getAsJsonArray()) {
                myBoards.add(getSafeString(curElement.getAsJsonObject(), "name"));
            }
            if (jsonObject.get("toolsDependencies") != null) {
                for (JsonElement curElement : jsonObject.get("toolsDependencies").getAsJsonArray()) {
                    myToolDependencies.add(new ArduinoPlatformTooldDependency(curElement, this));
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse ArduinoPlatform json  " + e.getMessage(),e);
        }
    }

    @Override
	public ArduinoPlatform getParent() {
        return myParent;
    }

    @Override
	public String getArchitecture() {
        return myArchitecture;
    }

    @Override
	public VersionNumber getVersion() {
        return myVersion;
    }

    @Override
	public String getCategory() {
        return myCategory;
    }

    @Override
	public List<ArduinoPlatformTooldDependency> getToolsDependencies() {
        return myToolDependencies;
    }

    @Override
	public boolean isInstalled() {
        return getBoardsFile().exists();
    }

    @Override
	public File getBoardsFile() {
        return getInstallPath().append(BOARDS_FILE_NAME).toFile();
    }

    @Override
	public File getPlatformFile() {
        return getInstallPath().append(PLATFORM_FILE_NAME).toFile();
    }

    @Override
    public IPath getInstallPath() {
        return getParent().getInstallPath().append(this.myVersion.toString());
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.myName == null) ? 0 : this.myName.hashCode());
        result = prime * result + ((this.myParent == null) ? 0 : this.myParent.hashCode());
        result = prime * result + ((this.myVersion == null) ? 0 : this.myVersion.hashCode());
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
        if (this.myName == null) {
            if (other.myName != null)
                return false;
        } else if (!this.myName.equals(other.myName))
            return false;
        if (this.myParent == null) {
            if (other.myParent != null)
                return false;
        } else if (!this.myParent.equals(other.myParent))
            return false;
        if (this.myVersion == null) {
            if (other.myVersion != null)
                return false;
        } else if (!this.myVersion.equals(other.myVersion))
            return false;
        return true;
    }

    @Override
	public List<String> getBoardNames() {
        return myBoards;
    }

    @Override
	public String getID() {
        return myVersion.toString();
    }

    @Override
	public String getConcattenatedBoardNames() {
        return String.join("\n", getBoardNames()); //$NON-NLS-1$
    }

    @Override
    public int compareTo(IArduinoPlatformVersion o) {
        return myName.compareTo(o.getName());
    }

    @Override
    public String toString() {
        return myName + SPACE + myArchitecture + '(' + myVersion + ')';
    }

}
