package io.sloeber.core.api.Json.library;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;

import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.LibraryDescriptor;
import io.sloeber.core.api.VersionNumber;

/**
 * This class represents a json file that references libraries
 *
 * @author jan
 *
 */
@JsonAdapter(LibraryIndexJson.class)
public class LibraryIndexJson implements Comparable<LibraryJson>, JsonDeserializer<LibraryIndexJson> {
    private List<LibraryJson> libraries = new ArrayList<>();
    private transient String jsonFileName;
    // category name to library name
    private transient Map<String, Set<String>> categories = new HashMap<>();
    // library name to latest version of library
    private transient Map<String, LibraryJson> latestLibs = new HashMap<>();

    public void resolve(File packageFile) {
        String fileName = packageFile.getName().toLowerCase();
        if (fileName.matches("(?i)library_index.json")) { //$NON-NLS-1$
            this.jsonFileName = Defaults.DEFAULT;
        } else {
            this.jsonFileName = fileName.replaceAll("(?i)" + Pattern.quote("library_"), "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    .replaceAll("(?i)" + Pattern.quote("_index.json"), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        for (LibraryJson library : this.libraries) {
            String name = library.getName();

            String category = library.getCategory();
            if (category == null) {
                category = "Uncategorized"; //$NON-NLS-1$
            }

            Set<String> categoryLibs = this.categories.get(category);
            if (categoryLibs == null) {
                categoryLibs = new HashSet<>();
                this.categories.put(category, categoryLibs);
            }
            categoryLibs.add(name);

            LibraryJson current = this.latestLibs.get(name);
            if (current != null) {
                if (library.getVersion().compareTo(current.getVersion()) > 0) {
                    this.latestLibs.put(name, library);
                }
            } else {
                this.latestLibs.put(name, library);
            }
        }
    }

    public LibraryJson getLatestLibrary(String name) {
        return this.latestLibs.get(name);
    }

    public LibraryJson getLibrary(String libName, VersionNumber version) {
        for (LibraryJson library : this.libraries) {
            if (library.getName().equals(libName) && (library.getVersion().equals(version))) {
                return library;
            }
        }
        return null;
    }

    public LibraryJson getInstalledLibrary(String libName) {
        for (LibraryJson library : this.libraries) {
            if (library.getName().equals(libName) && library.isInstalled()) {
                return library;
            }
        }
        return null;
    }

    public Set<String> getCategories() {
        return this.categories.keySet();
    }

    public Collection<LibraryJson> getLatestLibraries(String category) {
        Set<String> categoryLibs = this.categories.get(category);
        if (categoryLibs == null) {
            return new ArrayList<>(0);
        }

        List<LibraryJson> libs = new ArrayList<>(categoryLibs.size());
        for (String name : categoryLibs) {
            libs.add(this.latestLibs.get(name));
        }
        return libs;
    }

    public Map<String, LibraryJson> getLatestLibraries() {
        return this.latestLibs;
    }

    /**
     * get all the latest versions of alll libraries that can be installed but are
     * not yet installed To do so I find all latest libraries and I remove the once
     * that are installed.
     *
     * @return
     */
    public Map<String, LibraryDescriptor> getLatestInstallableLibraries() {
        Map<String, LibraryDescriptor> ret = new HashMap<>();
        for (Entry<String, LibraryJson> curLibrary : this.latestLibs.entrySet()) {
            if (!curLibrary.getValue().isAVersionInstalled()) {
                ret.put(curLibrary.getKey(), new LibraryDescriptor(curLibrary.getValue()));
            }
        }
        return ret;
    }

    public Collection<LibraryJson> getLibraries(String category) {
        Set<String> categoryLibs = this.categories.get(category);
        if (categoryLibs == null) {
            return new ArrayList<>(0);
        }

        List<LibraryJson> libs = new ArrayList<>(categoryLibs.size());
        for (LibraryJson curLibrary : this.libraries) {
            if (categoryLibs.contains(curLibrary.getName())) {
                libs.add(curLibrary);
            }
        }
        return libs;
    }

    public String getName() {
        return this.jsonFileName;
    }

    /**
     * get all the latest versions of alll the libraries provided that can be
     * installed but are not yet installed To do so I find all latest libraries and
     * I remove the once that are installed.
     *
     * @return
     */
    public Map<String, LibraryDescriptor> getLatestInstallableLibraries(Set<String> libNames) {
        Map<String, LibraryDescriptor> ret = new HashMap<>();
        if (libNames.isEmpty()) {
            return ret;
        }
        for (Entry<String, LibraryJson> curLibrary : this.latestLibs.entrySet()) {
            if (libNames.contains(curLibrary.getKey())) {
                if (!curLibrary.getValue().isAVersionInstalled()) {
                    ret.put(curLibrary.getKey(), new LibraryDescriptor(curLibrary.getValue()));
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("nls")
    @Override
    public LibraryIndexJson deserialize(JsonElement json, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        try {
            for (JsonElement curElement : jsonObject.get("libraries").getAsJsonArray()) {
                libraries.add(new LibraryJson(curElement, this));
            }
        } catch (Exception e) {
            throw new JsonParseException("failed to parse LibraryIndexJson json  " + e.getMessage());
        }

        return this;

    }

    @Override
    public int compareTo(LibraryJson o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
