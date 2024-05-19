package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.VersionNumber;

/**
 * This class represents an entry ina a library json file
 *
 * @author jan
 *
 */

public class ArduinoLibrary extends Node implements Comparable<ArduinoLibrary> {

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
            throw new JsonParseException("failed to parse json  " + e.getMessage());
        }

    }

    protected void addVersion(JsonElement json) {
        JsonObject jsonObject = json.getAsJsonObject();
        VersionNumber versionNumber = getSafeVersion(jsonObject, VERSION_KEY);
        versions.put(versionNumber, new ArduinoLibraryVersion(jsonObject, this));
    }

    public Collection<ArduinoLibraryVersion> getVersions() {
        return versions.values();
    }

    public String getAuthor() {
        return getNewestVersion().getAuthor();
    }

    public String getMaintainer() {
        return getNewestVersion().getMaintainer();
    }

    public String getSentence() {
        return getNewestVersion().getSentence();
    }

    public String getParagraph() {
        return getNewestVersion().getParagraph();
    }

    public String getWebsite() {
        return getNewestVersion().getWebsite();
    }

    public String getCategory() {
        return getNewestVersion().getCategory();
    }

    public List<String> getArchitectures() {
        return getNewestVersion().getArchitectures();
    }

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
    public ArduinoLibraryVersion getNewestVersion() {
        return versions.firstEntry().getValue();
    }

    /**
     * Get the version that is installed
     * If no version is installed return NULL
     * 
     * @return
     */
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
    public boolean isInstalled() {
        return getInstalledVersion() != null;
    }

    @Override
    public int compareTo(ArduinoLibrary other) {
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

    public IPath getInstallPath() {
        return ConfigurationPreferences.getInstallationPathLibraries().append(this.name.replace(' ', '_'));
    }

    public ArduinoLibraryVersion getVersion(VersionNumber versionNumber) {
        return versions.get(versionNumber);
    }

}
