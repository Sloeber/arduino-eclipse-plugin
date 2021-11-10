package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import io.sloeber.core.api.VersionNumber;

/**
 * This class represents an entry in a library json file
 *
 * @author jan
 *
 */

public class ArduinoLibraryVersion extends Node implements Comparable<ArduinoLibraryVersion> {

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
    private ArduinoLibrary myParent;

    public static final String LIBRARY_SOURCE_FODER = "src"; //$NON-NLS-1$

    @SuppressWarnings("nls")
    public ArduinoLibraryVersion(JsonElement json, ArduinoLibrary arduinoLibrary) {
        JsonObject jsonObject = json.getAsJsonObject();
        try {
            myParent = arduinoLibrary;
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

    public boolean isInstalled() {
        return getInstallPath().toFile().exists();
    }

    @Override
    public int compareTo(ArduinoLibraryVersion other) {
        if (other == null) {
            return 1;
        }
        int ret = getLibrary().compareTo(other.getLibrary());
        if (ret != 0) {
            //seems we are comparing 2 different libraries.
            //No need to look at the versions
            return ret;
        }
        return getID().compareTo(other.getID());
    }

    public ArduinoLibrary getLibrary() {
        return myParent;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Node getParent() {
        return myParent;
    }

    @Override
    public Node[] getChildren() {
        return null;
    }

    @Override
    public String getID() {
        return version.toString();
    }

    @Override
    public IPath getInstallPath() {
        return getParent().getInstallPath().append(version.toString());
    }
}
