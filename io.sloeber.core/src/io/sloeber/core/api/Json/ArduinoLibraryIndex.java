package io.sloeber.core.api.Json;

import static io.sloeber.core.Gson.GsonConverter.*;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

/**
 * This class represents a json file that references libraries
 *
 * @author jan
 *
 */
@JsonAdapter(ArduinoLibraryIndex.class)
public class ArduinoLibraryIndex extends Node
        implements Comparable<ArduinoLibraryIndex>, JsonDeserializer<ArduinoLibraryIndex> {
    private TreeMap<String, ArduinoLibrary> libraries = new TreeMap<>();
    private transient File jsonFile;

    public void setJsonFile(File packageFile) {
        jsonFile = packageFile;
    }

    /**
     * given a library name provide the library
     * 
     * @param libraryName
     * @return the library or null if not found
     */
    public ArduinoLibrary getLibrary(String libraryName) {
        return libraries.get(libraryName);
    }

    /**
     * get all the latest versions of all the libraries provided that can be
     * installed but are not yet installed To do so I find all latest libraries and
     * I remove the once that are installed.
     *
     * @return
     */
    public Map<String, ArduinoLibraryVersion> getLatestInstallableLibraries(Set<String> libNames) {
        Map<String, ArduinoLibraryVersion> ret = new HashMap<>();
        if (libNames.isEmpty()) {
            return ret;
        }
        for (ArduinoLibrary curLibrary : libraries.values()) {
            if (libNames.contains(curLibrary.getName())) {
                if (!curLibrary.isInstalled()) {
                    ret.put(curLibrary.getName(), curLibrary.getNewestVersion());
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("nls")
    @Override
    public ArduinoLibraryIndex deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            for (JsonElement curElement : jsonObject.get("libraries").getAsJsonArray()) {
                JsonObject jsonObject2 = curElement.getAsJsonObject();
                String libName = getSafeString(jsonObject2, "name");
                ArduinoLibrary library = libraries.get(libName);
                if (library == null) {
                    libraries.put(libName, new ArduinoLibrary(curElement, this));
                } else {
                    library.addVersion(curElement);
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse LibraryIndexJson json  " + e.getMessage(), e);
        }

        return this;

    }

    @Override
    public String getName() {
        return jsonFile.getName();
    }

    @Override
    public Node[] getChildren() {
        return libraries.values().toArray(new Node[libraries.size()]);
    }

    @Override
    public Node getParent() {
        return null;
    }

    @Override
    public String getID() {
        return getName();
    }

    @Override
    public int compareTo(ArduinoLibraryIndex o) {
        return getID().compareTo(o.getID());
    }

    public Collection<ArduinoLibrary> getLibraries() {
        return libraries.values();
    }

}
