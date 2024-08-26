package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.arduinoFramework.api.IArduinoLibrary;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.VersionNumber;

/**
 * This class represents an entry ina a library json file
 *
 * @author jan
 *
 */

public class ArduinoLibrary extends Node  implements IArduinoLibrary {

    private String name;
    private TreeMap<VersionNumber, ArduinoLibraryVersion> versions = new TreeMap<>(Collections.reverseOrder());
    private ArduinoLibraryIndex myParent;

    private static final String VERSION_KEY = "version"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    public ArduinoLibrary(JsonElement json, ArduinoLibraryIndex libraryIndexJson) {
        JsonObject jsonObject = json.getAsJsonObject();
        try {
            myParent = libraryIndexJson;
            name = getSafeString(jsonObject, "name");
            addVersion(json);
        } catch (Exception e) {
            throw new JsonParseException("failed to parse json  " + e.getMessage(),e);
        }

    }

    protected void addVersion(JsonElement json) {
        JsonObject jsonObject = json.getAsJsonObject();
        VersionNumber versionNumber = getSafeVersion(jsonObject, VERSION_KEY);
        versions.put(versionNumber, new ArduinoLibraryVersion(jsonObject, this));
    }

    @Override
	public Collection<IArduinoLibraryVersion> getVersions() {
        return new LinkedList<>(versions.values());
    }

    @Override
	public String getAuthor() {
        return getNewestVersion().getAuthor();
    }

    @Override
	public String getMaintainer() {
        return getNewestVersion().getMaintainer();
    }

    public String getSentence() {
        return getNewestVersion().getSentence();
    }

    public String getParagraph() {
        return getNewestVersion().getParagraph();
    }

    @Override
	public String getWebsite() {
        return getNewestVersion().getWebsite();
    }

    @Override
	public String getCategory() {
        return getNewestVersion().getCategory();
    }

    @Override
	public List<String> getArchitectures() {
        return getNewestVersion().getArchitectures();
    }

    @Override
	public List<String> getTypes() {
        return getNewestVersion().getTypes();
    }

    public String getUrl() {
        return getNewestVersion().getUrl();
    }

    /**
     * Get the newest version of this library
     *
     * @return the newest version of this library
     */
    @Override
	public ArduinoLibraryVersion getNewestVersion() {
        return versions.firstEntry().getValue();
    }

    /**
     * Get the version that is installed
     * If no version is installed return NULL
     *
     * @return
     */
    @Override
	public ArduinoLibraryVersion getInstalledVersion() {
        for (ArduinoLibraryVersion curVersion : versions.values()) {
            if (curVersion.isInstalled()) {
                return curVersion;
            }
        }
        return null;
    }

    /**
     * checks if a version of this library is installed.
     *
     * @return true if a version is installed. false in case no version is installed
     */
    @Override
	public boolean isInstalled() {
        return getInstalledVersion() != null;
    }

    @Override
    public int compareTo(IArduinoLibrary other) {
        return getID().compareTo(other.getID());
    }

    //Below are the Node overrides
    @Override
    public String getNodeName() {
        return name;
    }

    @Override
    public Node getParent() {
        return myParent;
    }

    @Override
    public Node[] getChildren() {
        return versions.values().toArray(new Node[versions.size()]);
    }

    @Override
    public String getID() {
        return name;
    }

    @Override
	public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathLibraries().append(this.name.replace(' ', '_'));
    }

    @Override
	public ArduinoLibraryVersion getVersion(VersionNumber versionNumber) {
        return versions.get(versionNumber);
    }

}
