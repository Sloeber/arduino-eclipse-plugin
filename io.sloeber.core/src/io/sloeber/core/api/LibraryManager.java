package io.sloeber.core.api;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;

import com.google.gson.Gson;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.Library;
import io.sloeber.core.managers.LibraryIndex;
import io.sloeber.core.managers.Manager;
import io.sloeber.core.managers.Messages;
import io.sloeber.core.tools.Version;

public class LibraryManager {
	static private List<LibraryIndex> libraryIndices;

	static public List<LibraryIndex> getLibraryIndices() {
		if (libraryIndices == null) {
			Manager.getPackageIndices();
		}
		return libraryIndices;
	}

	public static LibraryTree getLibraryTree() {
		return new LibraryTree();

	}

	public static class LibraryTree {

		private TreeMap<String, Category> categories = new TreeMap<>();

		public class Category implements Comparable<Category>, Node {
			private String name;
			protected TreeMap<String, Library> libraries = new TreeMap<>();

			public Category(String name) {
				this.name = name;
			}

			@Override
			public String getName() {
				return this.name;
			}

			public Collection<Library> getLibraries() {
				return this.libraries.values();
			}

			@Override
			public int compareTo(Category other) {
				return this.name.compareTo(other.name);
			}

			@Override
			public boolean hasChildren() {
				return !this.libraries.isEmpty();
			}

			@Override
			public Object[] getChildren() {
				return this.libraries.values().toArray();
			}

			@Override
			public Object getParent() {
				return LibraryTree.this;
			}
		}

		public class Library implements Comparable<Library>, Node {
			private String name;
			private String indexName;
			private Category category;
			protected TreeSet<VersionNumber> versions = new TreeSet<>();
			protected String version;
			private String tooltip;

			public Library(Category category, String name, String indexName, String tooltip) {
				this.category = category;
				this.name = name;
				this.tooltip = tooltip;
				this.indexName = indexName;
			}

			public Collection<VersionNumber> getVersions() {
				return this.versions;
			}

			@Override
			public String getName() {
				return this.name;
			}

			public String getTooltip() {
				return this.tooltip;
			}

			public String getLatest() {
				return this.versions.last().toString();
			}

			public String getVersion() {
				return this.version;
			}

			public String getIndexName() {
				return this.indexName;
			}

			public void setVersion(String version) {
				this.version = version;
			}

			@Override
			public int compareTo(Library other) {
				return this.name.compareTo(other.name);
			}

			@Override
			public boolean hasChildren() {
				return false;
			}

			@Override
			public Object[] getChildren() {
				return null;
			}

			@Override
			public Object getParent() {
				return this.category;
			}
		}

		public LibraryTree() {
			for (LibraryIndex libraryIndex : getLibraryIndices()) {
				for (String categoryName : libraryIndex.getCategories()) {
					Category category = this.categories.get(categoryName);
					if (category == null) {
						category = new Category(categoryName);
						this.categories.put(category.getName(), category);
					}
					for (io.sloeber.core.managers.Library library : libraryIndex.getLibraries(categoryName)) {
						Library lib = category.libraries.get(library.getName() + " (" + libraryIndex.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
						if (lib == null) {
							StringBuilder builder = new StringBuilder("Architectures:") //$NON-NLS-1$
									.append(library.getArchitectures().toString()).append("\n\n") //$NON-NLS-1$
									.append(library.getSentence());
							lib = new Library(category, library.getName(), libraryIndex.getName(), builder.toString());
							category.libraries.put(library.getName() + " (" + libraryIndex.getName() + ")", lib); //$NON-NLS-1$//$NON-NLS-2$
						}
						lib.versions.add(new VersionNumber(library.getVersion()));
						if (library.isInstalled()) {
							lib.version = library.getVersion();
						}
					}
				}
			}
		}

		public Collection<Category> getCategories() {
			return this.categories.values();
		}

		public Collection<Library> getAllLibraries() {
			Set<Library> all = new TreeSet<>();
			for (Category category : this.categories.values()) {
				all.addAll(category.getLibraries());
			}
			return all;
		}

		private static LibraryIndex findLibraryIndex(String name) {
			for (LibraryIndex libraryIndex : getLibraryIndices()) {
				if (libraryIndex.getName().equals(name))
					return libraryIndex;
			}
			return null;
		}

		public void reset() {
			for (Library library : this.getAllLibraries()) {
				LibraryIndex libraryIndex = findLibraryIndex(library.getIndexName());

				if (libraryIndex != null) {
					io.sloeber.core.managers.Library installed = libraryIndex.getInstalledLibrary(library.getName());
					library.setVersion(installed != null ? installed.getVersion() : null);
				}
			}
		}

	}

	public static IStatus setLibraryTree(LibraryTree libs, IProgressMonitor monitor, MultiStatus status) {
		for (LibraryTree.Library lib : libs.getAllLibraries()) {
			LibraryIndex libraryIndex = getLibraryIndex(lib.getIndexName());

			if (libraryIndex != null) {
				io.sloeber.core.managers.Library toRemove = libraryIndex.getInstalledLibrary(lib.getName());
				if (toRemove != null && !toRemove.getVersion().equals(lib.getVersion())) {
					status.add(toRemove.remove(monitor));
				}
				io.sloeber.core.managers.Library toInstall = libraryIndex.getLibrary(lib.getName(), lib.getVersion());
				if (toInstall != null && !toInstall.isInstalled()) {
					status.add(toInstall.install(monitor));
				}
			}

			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
		}
		return status;
	}

	public static String getPrivateLibraryPathsString() {
		return InstancePreferences.getPrivateLibraryPathsString();
	}

	/**
	 * get all the categories for all libraries (installed or not)
	 *
	 * @return a list of all the categories that exist in any library json file
	 *         known by sloeber
	 */
	public static Set<String> getAllCategories() {

		Set<String> ret = new TreeSet<>();

		for (LibraryIndex libraryIndex : getLibraryIndices()) {
			for (String categoryName : libraryIndex.getCategories()) {
				ret.add(categoryName);
			}
		}
		return ret;
	}

	public static void InstallDefaultLibraries(IProgressMonitor monitor) {
		LibraryIndex libindex = getLibraryIndex(Defaults.DEFAULT);
		if (libindex == null)
			return;

		for (String library : Defaults.INSTALLED_LIBRARIES) {
			Library toInstalLib = libindex.getLatestLibrary(library);
			if (toInstalLib != null) {
				toInstalLib.install(monitor);
			}
		}
	}

	static private LibraryIndex getLibraryIndex(String name) {
		for (LibraryIndex index : getLibraryIndices()) {
			if (index.getName().equals(name)) {
				return index;
			}
		}
		return null;
	}

	static public void loadJson(File jsonFile) {
		try (Reader reader = new FileReader(jsonFile)) {
			LibraryIndex index = new Gson().fromJson(reader, LibraryIndex.class);
			index.resolve();
			index.setJsonFile(jsonFile);
			libraryIndices.add(index);
		} catch (Exception e) {
			Common.log(new Status(IStatus.ERROR, Activator.getId(),
					Messages.Manager_Failed_to_parse.replace("${FILE}", jsonFile.getAbsolutePath()), e)); //$NON-NLS-1$
			jsonFile.delete();// Delete the file so it stops damaging
		}
	}

	/**
	 * Install the latest version of all the libraries belonging to this
	 * category If a earlier version is installed this version will be removed
	 * before installation of the newer version
	 *
	 * @param category
	 */
	public static void installAllLatestLibraries() {
		List<LibraryIndex> libraryIndices1 = getLibraryIndices();
		Map<String, Library> latestLibs = new HashMap<>();
		for (LibraryIndex libraryIndex : libraryIndices1) {
			Map<String, Library> libraries = libraryIndex.getLatestLibraries();
			for (Map.Entry<String, Library> entry : libraries.entrySet()) {
				String curLibName = entry.getKey();
				Library curLibrary = entry.getValue();
				Library current = latestLibs.get(curLibName);
				if (current != null) {
					if (Version.compare(curLibrary.getVersion(), current.getVersion()) > 0) {
						latestLibs.put(curLibName, curLibrary);
					}
				} else {
					latestLibs.put(curLibName, curLibrary);
				}
			}
		}
		// Base64 is a 1.0.0 version replaced with base64 So Don't install it
		latestLibs.remove("Base64"); //$NON-NLS-1$
		for (Map.Entry<String, Library> entry : latestLibs.entrySet()) {
			String curLibName = entry.getKey();
			Library curLibrary = entry.getValue();
			for (LibraryIndex libraryIndex : libraryIndices1) {

				Library previousVersion = libraryIndex.getInstalledLibrary(curLibName);
				if ((previousVersion != null) && (previousVersion != curLibrary)) {
					previousVersion.remove(new NullProgressMonitor());
				}
			}
			if (!curLibrary.isInstalled()) {
				curLibrary.install(new NullProgressMonitor());
			}
		}

	}

	public static void flushIndices() {
		libraryIndices = new ArrayList<>();
	}

	public static void removeAllLibs() {
		try {
			FileUtils.deleteDirectory(ConfigurationPreferences.getInstallationPathLibraries().toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}