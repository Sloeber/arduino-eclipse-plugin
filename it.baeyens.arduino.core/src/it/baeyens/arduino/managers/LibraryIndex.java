package it.baeyens.arduino.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryIndex {

	private List<ArduinoLibrary> libraries;

	// category name to library name
	private Map<String, Set<String>> categories = new HashMap<>();
	// library name to latest version of library
	private Map<String, ArduinoLibrary> latestLibs = new HashMap<>();

	public void resolve() {
		for (ArduinoLibrary library : libraries) {
			String name = library.getName();

			String category = library.getCategory();
			if (category == null) {
				category = "Uncategorized"; //$NON-NLS-1$
			}

			Set<String> categoryLibs = categories.get(category);
			if (categoryLibs == null) {
				categoryLibs = new HashSet<>();
				categories.put(category, categoryLibs);
			}
			categoryLibs.add(name);

			ArduinoLibrary current = latestLibs.get(name);
			if (current != null) {
				if (ArduinoManager.compareVersions(library.getVersion(), current.getVersion()) > 0) {
					latestLibs.put(name, library);
				}
			} else {
				latestLibs.put(name, library);
			}
		}
	}

	public ArduinoLibrary getLibrary(String name) {
		return latestLibs.get(name);
	}

	public Collection<String> getCategories() {
		return Collections.unmodifiableCollection(categories.keySet());
	}

	public Collection<ArduinoLibrary> getLibraries(String category) {
		Set<String> categoryLibs = categories.get(category);
		if (categoryLibs == null) {
			return new ArrayList<>(0);
		}

		List<ArduinoLibrary> libs = new ArrayList<>(categoryLibs.size());
		for (String name : categoryLibs) {
			libs.add(latestLibs.get(name));
		}
		return libs;
	}

}
