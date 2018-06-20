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
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.google.gson.Gson;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.core.DefaultInstallHandler;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.managers.Library;
import io.sloeber.core.managers.LibraryIndex;
import io.sloeber.core.tools.Version;

/**
 * This class is the main entry point for libraries. It handles private
 * libraries hardware libraries installed libraries
 *
 * @author jan
 *
 */
public class LibraryManager {
	private static final String INO = "ino"; //$NON-NLS-1$
	private static final String PDE = "pde";//$NON-NLS-1$
	private static final String CPP = "cpp";//$NON-NLS-1$
	private static final String C = "c";//$NON-NLS-1$
	private static final String LIBRARY_PATH_SUFFIX = "libraries"; //$NON-NLS-1$
	private static final String LIBRARY_DESCRIPTOR_PREFIX = "Library"; //$NON-NLS-1$
	private static final String EXAMPLE_DESCRIPTOR_PREFIX = "Example"; //$NON-NLS-1$
	private static final String FILE = Messages.FILE;

	static private List<LibraryIndex> libraryIndices;
	private static IInstallLibraryHandler myInstallLibraryHandler = new DefaultInstallHandler();

	static public List<LibraryIndex> getLibraryIndices() {
		if (libraryIndices == null) {
			InternalPackageManager.getPackageIndices();
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

	public static void setPrivateLibraryPaths(String[] libraryPaths) {
		InstancePreferences.setPrivateLibraryPaths(libraryPaths);

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

		for (String library : Defaults.DEFAULT_INSTALLED_LIBRARIES) {
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
					Messages.Manager_Failed_to_parse.replace(FILE, jsonFile.getAbsolutePath()), e)); 
			jsonFile.delete();// Delete the file so it stops damaging
		}
	}
	
	   /**
     * install 1 single library based on the library name
     * @param libName
     */
    public static void installLibrary(String libName) {
        Set<String> libNamesToInstall = new TreeSet<>();
        libNamesToInstall.add(libName);
        Map<String, LibraryDescriptor> libsToInstall = LibraryManager.getLatestInstallableLibraries(libNamesToInstall);
        if (!libsToInstall.isEmpty()) {
            for (Entry<String, LibraryDescriptor> curLib : libsToInstall.entrySet()) {
                curLib.getValue().toLibrary().install(new NullProgressMonitor());
            }
        }
    }

	/**
	 * Install the latest version of all the libraries belonging to this category If
	 * a earlier version is installed this version will be removed before
	 * installation of the newer version
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

	/**
	 * Searches for all libraries that can be installed but are not yet installed. A
	 * library is considered installed when 1 version of the library is installed.
	 *
	 * @return a map of all instalable libraries
	 */
	public static Map<String, LibraryDescriptor> getAllInstallableLibraries() {
		Map<String, LibraryDescriptor> ret = new HashMap<>();
		for (LibraryIndex libraryIndex : libraryIndices) {
			ret.putAll(libraryIndex.getLatestInstallableLibraries());
		}

		return ret;
	}

	public static Map<String, LibraryDescriptor> getLatestInstallableLibraries(Set<String> libnames) {
		Set<String> remainingLibNames = new TreeSet<>(libnames);
		Map<String, LibraryDescriptor> ret = new HashMap<>();
		for (LibraryIndex libraryIndex : libraryIndices) {
			ret.putAll(libraryIndex.getLatestInstallableLibraries(remainingLibNames));
			remainingLibNames.removeAll(ret.keySet());
		}

		return ret;
	}

	public static void registerInstallLibraryHandler(IInstallLibraryHandler installLibraryHandler) {
		myInstallLibraryHandler = installLibraryHandler;
	}

	public static IInstallLibraryHandler getInstallLibraryHandler() {
		return myInstallLibraryHandler;
	}

	/**
	 * Check wether a library is installed. The check looks at the library
	 * installation place at the disk.
	 *
	 * @return true if at least one library is installed
	 */
	public static boolean libsAreInstalled() {
		if (ConfigurationPreferences.getInstallationPathLibraries().toFile().exists()) {
			return ConfigurationPreferences.getInstallationPathLibraries().toFile().list().length != 0;
		}
		return false;
	}

	/**
	 * find all examples that are delivered with a library This does not include the
	 * libraries delivered with hardware
	 *
	 * @return
	 */
	public static TreeMap<String, IPath> getAllLibraryExamples() {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String libLocations[] = InstancePreferences.getPrivateLibraryPaths();

		File CommonLibLocation = ConfigurationPreferences.getInstallationPathLibraries().toFile();
		if (CommonLibLocation.exists()) {
			examples.putAll(getLibExampleFolders(CommonLibLocation));
		}

		// get the examples from the user provide library locations
		if (libLocations != null) {
			for (String curLibLocation : libLocations) {
				File curFile = new File(curLibLocation);
				if (curFile.exists()) {
					examples.putAll(getLibExampleFolders(curFile));
				}
			}
		}
		return examples;
	}

	/***
	 * finds all the example folders for both the version including and without
	 * version libraries
	 *
	 * @param location
	 *            The parent folder of the libraries
	 */
	private static TreeMap<String, IPath> getLibExampleFolders(File LibRoot) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		if (LibRoot == null) {
			return examples;
		}
		String[] Libs = LibRoot.list();
		if (Libs == null) {
			// Either dir does not exist or is not a directory
			return examples;
		}
		for (String curLib : Libs) {
			String libID = LIBRARY_DESCRIPTOR_PREFIX + '/' + curLib;
			File Lib_examples = LibRoot.toPath().resolve(curLib).resolve("examples").toFile();//$NON-NLS-1$
			File Lib_Examples = LibRoot.toPath().resolve(curLib).resolve("Examples").toFile();//$NON-NLS-1$
			if (Lib_examples.isDirectory()) {
				examples.putAll(getExamplesFromFolder(libID, Lib_examples));
			} else if (Lib_Examples.isDirectory()) {
				examples.putAll(getExamplesFromFolder(libID, Lib_Examples));
			} else // nothing found directly so maybe this is a version
					// based lib
			{
				String[] versions = LibRoot.toPath().resolve(curLib).toFile().list();
				if (versions != null) {
					if (versions.length == 1) {// There can only be 1
						// version of a lib
						Lib_examples = LibRoot.toPath().resolve(curLib).resolve(versions[0]).resolve("examples") //$NON-NLS-1$
								.toFile();
						Lib_Examples = LibRoot.toPath().resolve(curLib).resolve(versions[0]).resolve("Examples") //$NON-NLS-1$
								.toFile();
						if (Lib_examples.isDirectory()) {
							examples.putAll(getExamplesFromFolder(libID, Lib_examples));
						} else if (Lib_Examples.isDirectory()) {
							examples.putAll(getExamplesFromFolder(libID, Lib_Examples));
						}
					}
				}
			}
		}

		return examples;
	}

	/**
	 * This method adds a folder recursively examples. Leaves containing ino files
	 * are assumed to be examples
	 *
	 * @param File
	 */
	private static TreeMap<String, IPath> getExamplesFromFolder(String prefix, File location) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		File[] children = location.listFiles();
		if (children == null) {
			// Either dir does not exist or is not a directory
			return examples;
		}
		for (File exampleFolder : children) {
			String extension = FilenameUtils.getExtension(exampleFolder.toString());
			if (exampleFolder.isDirectory()) {
				examples.putAll(getExamplesFromFolder(prefix + '/' + exampleFolder.getName(), exampleFolder));
			} else if (INO.equalsIgnoreCase(extension) || PDE.equalsIgnoreCase(extension)
					|| CPP.equalsIgnoreCase(extension) || C.equalsIgnoreCase(extension)) {
				examples.put(prefix, new Path(location.toString()));
			}
		}
		return examples;
	}

	/*
	 * Get the examples of the libraries from the selected hardware These may be
	 * referenced libraries
	 */
	private static TreeMap<String, IPath> getAllHardwareLibraryExamples(BoardDescriptor boardDescriptor) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (boardDescriptor != null) {
			IPath platformPath = boardDescriptor.getreferencingPlatformPath();
			if (platformPath.toFile().exists()) {
				examples.putAll(getLibExampleFolders(platformPath.append(LIBRARY_PATH_SUFFIX).toFile()));
			}
		}
		return examples;
	}

	/**
	 * find all examples that are delivered with the Arduino IDE
	 *
	 * @return
	 */
	public static TreeMap<String, IPath> getAllArduinoIDEExamples() {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		File exampleLocation = ConfigurationPreferences.getInstallationPathExamples().toFile();

		if (exampleLocation.exists()) {
			examples.putAll(getExamplesFromFolder(EXAMPLE_DESCRIPTOR_PREFIX , exampleLocation));
		}
		return examples;
	}

	/**
	 * find all examples for this type of board. That is the examples provided by
	 * Arduino The examples provided by the common libraries The examples provided
	 * by the private libraries The examples provided by the platform the board
	 * belongs to
	 *
	 * If the boardID is null there will be no platform examples
	 *
	 * @param boardDescriptor
	 * @return
	 */
	public static TreeMap<String, IPath> getAllExamples(BoardDescriptor boardDescriptor) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		// Get the examples of the library manager installed libraries

		examples.putAll(getAllLibraryExamples());
		examples.putAll(getAllArduinoIDEExamples());
		// This one should be the last as hasmap overwrites doubles. This way
		// hardware libraries are preferred to others
		examples.putAll(getAllHardwareLibraryExamples(boardDescriptor));

		return examples;
	}


}