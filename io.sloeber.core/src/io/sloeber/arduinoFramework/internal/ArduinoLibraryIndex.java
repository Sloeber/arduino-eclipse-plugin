package io.sloeber.arduinoFramework.internal;

import static io.sloeber.arduinoFramework.internal.GsonConverter.*;

import java.io.File;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import io.sloeber.arduinoFramework.api.IArduinoLibrary;
import io.sloeber.arduinoFramework.api.IArduinoLibraryIndex;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;

/**
 * This class represents a json file that references libraries
 *
 * @author jan
 *
 */
@JsonAdapter(ArduinoLibraryIndex.class)
public class ArduinoLibraryIndex extends Node
        implements  JsonDeserializer<ArduinoLibraryIndex>, IArduinoLibraryIndex {
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
    @Override
	public IArduinoLibrary getLibrary(String libraryName) {
        return libraries.get(libraryName);
    }

    /**
     * get all the latest versions of all the libraries provided that can be
     * installed but are not yet installed To do so I find all latest libraries and
     * I remove the once that are installed.
     *
     * @return
     */
    @Override
	public Map<String, IArduinoLibraryVersion> getLatestInstallableLibraries(Set<String> libNames) {
        Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
        if (libNames.isEmpty()) {
            return ret;
        }
        for (IArduinoLibrary curLibrary : libraries.values()) {
            if (libNames.contains(curLibrary.getNodeName())) {
                if (!curLibrary.isInstalled()) {
                    ret.put(curLibrary.getNodeName(), curLibrary.getNewestVersion());
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
    public String getNodeName() {
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
        return getNodeName();
    }

    @Override
    public int compareTo(IArduinoLibraryIndex o) {
        return getID().compareTo(o.getID());
    }

    @Override
	public Collection<IArduinoLibrary> getLibraries() {
        return new LinkedList<> (libraries.values());
    }

}
