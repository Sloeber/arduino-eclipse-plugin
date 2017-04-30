package io.sloeber.core.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.sloeber.core.api.Defaults;
import io.sloeber.core.tools.Version;

public class LibraryIndex {
	private String indexName;
	private List<Library> libraries;

	// category name to library name
	private Map<String, Set<String>> categories = new HashMap<>();

	// library name to latest version of library
	private Map<String, Library> latestLibs = new HashMap<>();

	public void resolve() {
		for (Library library : this.libraries) {
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

			Library current = this.latestLibs.get(name);
			if (current != null) {
				if (Version.compare(library.getVersion(), current.getVersion()) > 0) {
					this.latestLibs.put(name, library);
				}
			} else {
				this.latestLibs.put(name, library);
			}
		}
	}

	public Library getLatestLibrary(String name) {
		return this.latestLibs.get(name);
	}

	public Library getLibrary(String libName, String version) {
		for (Library library : this.libraries) {
			if (library.getName().equals(libName) && (library.getVersion().equals(version))) {
				return library;
			}
		}
		return null;
	}

	public Library getInstalledLibrary(String libName) {
		for (Library library : this.libraries) {
			if (library.getName().equals(libName) && library.isInstalled()) {
				return library;
			}
		}
		return null;
	}

	public Set<String> getCategories() {
		return this.categories.keySet();
	}

	public Collection<Library> getLatestLibraries(String category) {
		Set<String> categoryLibs = this.categories.get(category);
		if (categoryLibs == null) {
			return new ArrayList<>(0);
		}

		List<Library> libs = new ArrayList<>(categoryLibs.size());
		for (String name : categoryLibs) {
			libs.add(this.latestLibs.get(name));
		}
		return libs;
	}

	public Collection<Library> getLatestLibraries() {

		return this.latestLibs.values();
	}

	public Collection<Library> getLibraries(String category) {
		Set<String> categoryLibs = this.categories.get(category);
		if (categoryLibs == null) {
			return new ArrayList<>(0);
		}

		List<Library> libs = new ArrayList<>(categoryLibs.size());
		for (Library curLibrary : this.libraries) {
			if (categoryLibs.contains(curLibrary.getName())) {
				libs.add(curLibrary);
			}
		}
		return libs;
	}

	public void setJsonFile(File packageFile) {
		String fileName = packageFile.getName().toLowerCase();
		if (fileName.matches("(?i)library_index.json")) { //$NON-NLS-1$
			this.indexName = Defaults.DEFAULT;
		} else {
			this.indexName = fileName.replaceAll("(?i)" + Pattern.quote("library_"), "") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					.replaceAll("(?i)" + Pattern.quote("_index.json"), ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public String getName() {
		return this.indexName;
	}
}
