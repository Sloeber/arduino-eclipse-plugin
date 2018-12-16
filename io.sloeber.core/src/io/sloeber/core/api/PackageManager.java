package io.sloeber.core.api;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
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
import io.sloeber.core.api.PackageManager.PlatformTree.IndexFile;
import io.sloeber.core.api.PackageManager.PlatformTree.InstallableVersion;
import io.sloeber.core.api.PackageManager.PlatformTree.Platform;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.Board;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.managers.Package;
import io.sloeber.core.managers.PackageIndex;
import io.sloeber.core.tools.TxtFile;

/**
 * This class groups both boards installed by the hardware manager and boards
 * installed locally.
 *
 * @author jantje
 *
 */
@SuppressWarnings("unused")
public class PackageManager {

	private static final String FILE = Messages.FILE;
	private static final String FOLDER = Messages.FOLDER;
	protected static List<PackageIndex> packageIndices;
	private static boolean myHasbeenLogged=false;
	/**
	 * Gets the board descriptor based on the information provided. If
	 * jsonFileName="local" the board is assumed not to be installed by the
	 * boards manager. Otherwise the boardsmanager is queried to find the board
	 * descriptor. In this case the latest installed board will be returned
	 *
	 * @param jsonFileName
	 *            equals to "local" or the name of the json file used by the
	 *            boards manager to install the boards
	 * @param packageName
	 *            if jsonFileName equals "local" the filename of the boards.txt
	 *            containing the boards. otherwise the name of the package
	 *            containing the board
	 * @param platformName
	 *            ignored if jsonFileName equals "local" otherwise the name of
	 *            the platform containing the board
	 * @param boardID
	 *            the id of the board in the boards.txt file
	 * @param options
	 *            the options to specify the board (the menu named on the
	 *            boards.txt file)
	 *            or null for defaults
	 * @return The class BoardDescriptor or null
	 */
	static public BoardDescriptor getBoardDescriptor(String jsonFileName, String packageName, String platformName,
			String boardID, Map<String, String> options) {
		if (jsonFileName.equals("local")) { //$NON-NLS-1$
			return BoardDescriptor.makeBoardDescriptor(new File(packageName), boardID, options);
		}
		return getNewestBoardIDFromBoardsManager(jsonFileName, packageName, platformName, boardID, options);
	}

	static private BoardDescriptor getNewestBoardIDFromBoardsManager(String jsonFileName, String packageName,
			String platformName, String boardID, Map<String, String> options) {

		Package thePackage = InternalPackageManager.getPackage(jsonFileName, packageName);
		if (thePackage == null) {
			// fail("failed to find package:" + this.mPackageName);
			return null;
		}
		ArduinoPlatform platform = thePackage.getLatestPlatform(platformName, true);
		if (platform == null) {
			// fail("failed to find platform " + this.mPlatform + " in
			// package:" + this.mPackageName);
			return null;
		}
		List<Board> boards = platform.getBoards();
		if (boards == null) {
			// fail("No boards found");
			return null;
		}
		for (Board curBoard : boards) {
			if (curBoard.getId().equals(boardID)) {
				java.io.File boardsFile = curBoard.getPlatform().getBoardsFile();
				BoardDescriptor boardid = BoardDescriptor.makeBoardDescriptor(boardsFile, curBoard.getId(), options);

				return boardid;
			}

		}
		return null;
	}

	public static void addPackageURLs(HashSet<String> packageUrlsToAdd, boolean forceDownload) {
		HashSet<String> originalJsonUrls = new HashSet<>(Arrays.asList(ConfigurationPreferences.getJsonURLList()));
		packageUrlsToAdd.addAll(originalJsonUrls);

		ConfigurationPreferences.setJsonURLs(packageUrlsToAdd);
		loadJsons(forceDownload);
	}
	public static void setPackageURLs(HashSet<String> packageUrls, boolean forceDownload) {
		ConfigurationPreferences.setJsonURLs(packageUrls);
		loadJsons(forceDownload);
	}

	public static void removePackageURLs(Set<String> packageUrlsToRemove) {
		InternalPackageManager.removePackageURLs(packageUrlsToRemove);

	}

	public static void installAllLatestPlatforms() {
		NullProgressMonitor monitor = new NullProgressMonitor();
		List<Package> allPackages = InternalPackageManager.getPackages();
		for (Package curPackage : allPackages) {
			Collection<ArduinoPlatform> latestPlatforms = curPackage.getLatestPlatforms();
			for (ArduinoPlatform curPlatform : latestPlatforms) {
				curPlatform.install(monitor);
			}
		}
	}

	public static void installLatestPlatform(String JasonName, String packageName, String platformName) {

		Package curPackage = InternalPackageManager.getPackage(JasonName, packageName);
		if (curPackage != null) {
			ArduinoPlatform curPlatform = curPackage.getLatestPlatform(platformName, false);
			if (curPlatform != null) {
				NullProgressMonitor monitor = new NullProgressMonitor();
				curPlatform.install(monitor);
				return;
			}
		}
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
				"failed to find " + JasonName + " " + packageName + " " + platformName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void addPrivateHardwarePath(String newHardwarePath) {
		if(newHardwarePath==null) {
			return;
		}
		String currentPaths[] = InstancePreferences.getPrivateHardwarePaths();
		String newPaths[] = new String[currentPaths.length + 1];
		for (int i = 0; i < currentPaths.length; i++) {
			if (currentPaths[i].equals(newHardwarePath)) {
				return;
			}
			newPaths[i] = currentPaths[i];
		}
		newPaths[currentPaths.length] = newHardwarePath;
		InstancePreferences.setPrivateHardwarePaths(newPaths);
	}

	public static boolean isReady() {
		return InternalPackageManager.isReady();
	}




	public static String[] getBoardNames(String boardFile) {
		TxtFile theBoardsFile = new TxtFile(new File(boardFile));
		return theBoardsFile.getAllNames();
	}

	/**
	 * Searches for all boards.txt files from the hardware folders and the
	 * boards manager
	 *
	 * @return all the boards.txt files with full path and in a case insensitive
	 *         order
	 */
	public static String[] getAllBoardsFiles() {
		String hardwareFolders[] = getHardwarePaths();

		TreeSet<String> boardFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (String CurFolder : hardwareFolders) {
			searchFiles(new File(CurFolder), boardFiles, Const.BOARDS_FILE_NAME, 6);
		}
		if (boardFiles.size() == 0) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_No_boards_txt_found.replace(FILE, String.join("\n", hardwareFolders)), null)); //$NON-NLS-1$
			return null;
		}
		return boardFiles.toArray(new String[boardFiles.size()]);
	}

	private static void searchFiles(File folder, TreeSet<String> Hardwarelists, String Filename, int depth) {
		if (depth > 0) {
			File[] a = folder.listFiles();
			if (a == null) {
				if(!myHasbeenLogged) {
				Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
						Messages.Helpers_Error_The_folder_is_empty.replace(FOLDER, folder.toString()) , null));
				myHasbeenLogged=true;
				}
				return;
			}
			for (File f : a) {
				if (f.isDirectory()) {
					searchFiles(f, Hardwarelists, Filename, depth - 1);
				} else if (f.getName().equals(Filename)) {
					Hardwarelists.add(new Path(f.toString()).toString());
				}
			}
		}
	}

	/**
	 * Gets all the folders that can contain hardware
	 *
	 * @return a list of all the folder locations that can contain hardware
	 */
	private static String[] getHardwarePaths() {
		return (InstancePreferences.getPrivateHardwarePathsString() + File.pathSeparator
				+ ConfigurationPreferences.getInstallationPathPackages()).split(File.pathSeparator);
	}



	public static class PlatformTree {
		private TreeMap<String, IndexFile> IndexFiles = new TreeMap<>();

		public class InstallableVersion implements Comparable<InstallableVersion> {
			private Platform platform;
			private ArduinoPlatform myInternalPlatformm;
			private boolean isInstalled;

			public InstallableVersion(ArduinoPlatform internalPlatformm, Platform platform) {
				this.platform = platform;
				this.myInternalPlatformm = internalPlatformm;
				this.isInstalled = this.myInternalPlatformm.isInstalled();
			}

			public VersionNumber getVersion() {
				return new VersionNumber(this.myInternalPlatformm.getVersion());
			}

			public boolean isInstalled() {
				return this.isInstalled;
			}

			public void setInstalled(boolean isInstalled) {
				this.isInstalled = isInstalled;
			}

			@Override
			public int compareTo(InstallableVersion o) {
				return getVersion().compareTo(o.getVersion());
			}

			public Platform getPlatform() {
				return this.platform;
			}

			public ArduinoPlatform getInternalPlatform() {
				return this.myInternalPlatformm;
			}

		}

		public class Platform implements Comparable<Platform> {

			private String name;
			private String architecture;
			protected TreeSet<InstallableVersion> versions = new TreeSet<>();
			protected String boards;
			private Package pac;

			public String getArchitecture() {
				return this.architecture;
			}

			public String getBoards() {
				return this.boards;
			}

			public Platform(ArduinoPlatform internalPlatformm, Package pac) {
				this.name = internalPlatformm.getName();
				this.architecture = internalPlatformm.getArchitecture();
				this.versions.add(new InstallableVersion(internalPlatformm, this));
				this.boards = String.join("\n", internalPlatformm.getBoardNames()); //$NON-NLS-1$
				this.pac = pac;
			}

			public void addVersion(ArduinoPlatform internalPlatformm) {
				this.versions.add(new InstallableVersion(internalPlatformm, this));

			}

			public Collection<InstallableVersion> getVersions() {
				return this.versions;
			}

			public String getName() {
				return this.name;
			}

			public String getLatest() {
				return this.versions.last().toString();
			}

			@Override
			public int compareTo(Platform other) {
				return this.name.compareToIgnoreCase(other.name);
			}

			public InstallableVersion getVersion(VersionNumber versionNumber) {
				for (InstallableVersion curVersion : this.versions) {
					if (curVersion.getVersion().compareTo(versionNumber) == 0) {
						return curVersion;
					}
				}
				return null;
			}

			public boolean isInstalled() {
				for (InstallableVersion curVersion : this.versions) {
					if (curVersion.isInstalled()) {
						return true;
					}
				}
				return false;
			}

			public Package getPackage() {
				return this.pac;
			}
		}

		/**
		 * This class represents the json file on disk
		 */
		public class IndexFile implements Comparable<IndexFile> {
			protected File jsonFile;
			protected TreeMap<String, Package> packages = new TreeMap<>();

			public IndexFile(File jsonFile) {
				this.jsonFile = jsonFile;
			}

			@Override
			public int compareTo(IndexFile other) {
				return this.jsonFile.compareTo(other.jsonFile);
			}

			public Collection<Package> getAllPackages() {
				return this.packages.values();
			}

			public String getNiceName() {
				return this.jsonFile.getName();
			}

			public String getFullName() {
				return this.jsonFile.getPath();
			}

			/**
			 * is one ore more packages of this index file installed
			 *
			 * @return returns true if at least one version of one child
			 *         platform of a child packageis installed
			 */

			public boolean isInstalled() {
				for (Package curpackage : this.packages.values()) {
					if (curpackage.isInstalled()) {
						return true;

					}
				}
				return false;
			}
		}

		public class Package implements Comparable<Package> {
			public String getMaintainer() {
				return this.maintainer;
			}

			public URL getWebsiteURL() {
				return this.websiteURL;
			}

			public String getEmail() {
				return this.email;
			}

			private String name;
			private String maintainer;
			private URL websiteURL;
			private String email;

			protected TreeMap<String, Platform> platforms = new TreeMap<>();
			private IndexFile indexFile;

			public Package(String name) {
				this.name = name;
			}

			public Package(io.sloeber.core.managers.Package pack, IndexFile indexFile) {
				this.name = pack.getName();
				this.maintainer = pack.getMaintainer();
				try {
					this.websiteURL = new URL(pack.getWebsiteURL());
				} catch (MalformedURLException e) {
					// ignore this error
				}
				this.email = pack.getEmail();
				this.indexFile = indexFile;
			}

			public String getName() {
				return this.name;
			}

			public Collection<Platform> getPlatforms() {
				return this.platforms.values();
			}

			@Override
			public int compareTo(Package other) {
				return this.name.compareToIgnoreCase(other.name);
			}

			/**
			 * is one ore more platforms of this package installed
			 *
			 * @return returns true if at least one version of one child
			 *         platform is installed
			 */
			public boolean isInstalled() {
				for (Platform curplatform : this.platforms.values()) {
					if (curplatform.isInstalled()) {
						return true;

					}
				}
				return false;
			}

			public IndexFile getIndexFile() {
				return this.indexFile;
			}

		}

		public PlatformTree() {
			List<PackageIndex> packageIndexes = InternalPackageManager.getPackageIndices();
			for (PackageIndex curPackageIndex : packageIndexes) {
				IndexFile curIndexFile = new IndexFile(curPackageIndex.getJsonFile());
				this.IndexFiles.put(curPackageIndex.getJsonFileName(), curIndexFile);
				for (io.sloeber.core.managers.Package curInternalPackage : curPackageIndex.getPackages()) {
					Package curPackage = new Package(curInternalPackage, curIndexFile);
					curIndexFile.packages.put(curPackage.getName(), curPackage);
					for (ArduinoPlatform curInternalPlatform : curInternalPackage.getPlatforms()) {
						Platform curPlatform = new Platform(curInternalPlatform, curPackage);
						Platform foundPlatform = curPackage.platforms.get(curPlatform.getName());
						if (foundPlatform == null) {
							curPackage.platforms.put(curPlatform.getName(), curPlatform);
						} else {
							foundPlatform.addVersion(curInternalPlatform);
						}
					}
				}
			}
		}

		public Collection<IndexFile> getAllIndexFiles() {
			return this.IndexFiles.values();
		}

		public Set<Package> getAllPackages() {
			Set<Package> all = new TreeSet<>();
			for (IndexFile indexFile : this.IndexFiles.values()) {
				all.addAll(indexFile.getAllPackages());
			}
			return all;
		}

		public Collection<Platform> getAllPlatforms() {
			Set<Platform> all = new TreeSet<>();
			for (IndexFile curIndexFile : this.IndexFiles.values()) {
				for (Package curPackage : curIndexFile.getAllPackages()) {
					all.addAll(curPackage.getPlatforms());
				}
			}
			return all;
		}

		public IndexFile getIndexFile(PackageIndex packageIndex) {
			return this.IndexFiles.get(packageIndex.getJsonFileName());
		}

		@SuppressWarnings("static-method")
		public Package getPackage(IndexFile indexFile, io.sloeber.core.managers.Package curInternalPackage) {
			return indexFile.packages.get(curInternalPackage.getName());
		}

		@SuppressWarnings("static-method")
		public Platform getPlatform(Package curPackage, ArduinoPlatform curInternalPlatform) {
			return curPackage.platforms.get(curInternalPlatform.getName());
		}

	}

	public static IStatus setPlatformTree(PlatformTree platformTree, IProgressMonitor monitor, MultiStatus status) {
		if (!InternalPackageManager.isReady()) {
			status.add(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "BoardsManager is still busy", null)); //$NON-NLS-1$
			return status;
		}
		if(!ConfigurationPreferences.getUpdateJasonFilesFlag()) {
		   loadJsons(true);
		}
		try {
			InternalPackageManager.setReady(false);

			for (IndexFile curIndexFile : platformTree.getAllIndexFiles()) {
				for (io.sloeber.core.api.PackageManager.PlatformTree.Package curPackage : curIndexFile
						.getAllPackages()) {
					for (Platform curPlatform : curPackage.getPlatforms()) {
						for (InstallableVersion curVersion : curPlatform.getVersions()) {
							if (curVersion.isInstalled()) {
								status.add(curVersion.getInternalPlatform().install(monitor));
							} else {

								status.add(curVersion.getInternalPlatform().remove(monitor));
							}
						}
					}
				}

			}
		} catch (Exception e) {
			// do nothing
		}
		InternalPackageManager.setReady(true);
		return status;
	}





	/**
	 * returns all the menu names for all installed platforms. The return is
	 * sorted and unique
	 *
	 * @return
	 */
	public static Set<String> getAllMenuNames() {
		Set<String> ret = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		String[] boardFiles = getAllBoardsFiles();
		for (String curBoardFile : boardFiles) {
			TxtFile txtFile = new TxtFile(new File(curBoardFile));
			ret.addAll(txtFile.getMenuNames());
		}
		return ret;
	}

	public static TreeMap<String, String> getAllmenus() {
		TreeMap<String, String> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String[] boardFiles = getAllBoardsFiles();
		for (String curBoardFile : boardFiles) {
			TxtFile txtFile = new TxtFile(new File(curBoardFile));
			ret.putAll(txtFile.getMenus());
		}
		return ret;
	}

	/**
	 * Remove all packages that have a more recent version
	 */
	public static void onlyKeepLatestPlatforms() {
		InternalPackageManager.onlyKeepLatestPlatforms();

	}

	public static void setPrivateHardwarePaths(String[] hardWarePaths) {
		InstancePreferences.setPrivateHardwarePaths(hardWarePaths);
	}
	public static String getPrivateHardwarePathsString() {
		return InstancePreferences.getPrivateHardwarePathsString();
	}

	protected static void loadJsons(boolean forceDownload) {
		packageIndices = new ArrayList<>();
		LibraryManager.flushIndices();

		String[] jsonUrls = ConfigurationPreferences.getJsonURLList();
		for (String jsonUrl : jsonUrls) {
			loadJson(jsonUrl, forceDownload);
		}
	}
	/**
	 * This method takes a json boards file url and downloads it and parses it for
	 * usage in the boards manager
	 *
	 * @param url
	 *            the url of the file to download and load
	 * @param forceDownload
	 *            set true if you want to download the file even if it is already
	 *            available locally
	 */
	static private void loadJson(String url, boolean forceDownload) {
		File jsonFile = getLocalFileName(url, true);
		if (jsonFile == null) {
			return;
		}
		if (!jsonFile.exists() || forceDownload) {
			jsonFile.getParentFile().mkdirs();
			try {
				mySafeCopy(new URL(url.trim()), jsonFile, false);
			} catch (IOException e) {
				Common.log(new Status(IStatus.ERROR, Activator.getId(), "Unable to download " + url, e)); //$NON-NLS-1$
			}
		}
		if (jsonFile.exists()) {
			if (jsonFile.getName().toLowerCase().startsWith("package_")) { //$NON-NLS-1$
				loadPackage(jsonFile);
			} else if (jsonFile.getName().toLowerCase().startsWith("library_")) { //$NON-NLS-1$
				LibraryManager.loadJson(jsonFile);
			} else {
				Common.log(new Status(IStatus.ERROR, Activator.getId(),
						"json files should start with \"package_\" or \"library_\" " + url + " is ignored")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	static private void loadPackage(File jsonFile) {
		try (Reader reader = new FileReader(jsonFile)) {
			PackageIndex index = new Gson().fromJson(reader, PackageIndex.class);
			index.setOwners(null);
			index.setJsonFile(jsonFile);
			packageIndices.add(index);
		} catch (Exception e) {
			Common.log(new Status(IStatus.ERROR, Activator.getId(),
					Messages.Manager_Failed_to_parse.replace(FILE, jsonFile.getAbsolutePath()), e)); 
			jsonFile.delete();// Delete the file so it stops damaging
		}
	}


	/**
	 * convert a web url to a local file name. The local file name is the cache of
	 * the web
	 *
	 * @param url
	 *            url of the file we want a local cache
	 * @return the file that represents the file that is the local cache. the file
	 *         itself may not exists. If the url is malformed return null;
	 * @throws MalformedURLException
	 */
	protected static File getLocalFileName(String url, boolean show_error) {
		URL packageUrl;
		try {
			packageUrl = new URL(url.trim());
		} catch (MalformedURLException e) {
			if (show_error) {
				Common.log(new Status(IStatus.ERROR, Activator.getId(), "Malformed url " + url, e)); //$NON-NLS-1$
			}
			return null;
		}
		if ("file".equals(packageUrl.getProtocol())) { //$NON-NLS-1$
			String tst = packageUrl.getFile();
			File file = new File(tst);
			String localFileName = file.getName();
			java.nio.file.Path packagePath = Paths
					.get(ConfigurationPreferences.getInstallationPath().append(localFileName).toString());
			return packagePath.toFile();
		}
		String localFileName = Paths.get(packageUrl.getPath()).getFileName().toString();
		java.nio.file.Path packagePath = Paths.get(ConfigurationPreferences.getInstallationPath().append(localFileName).toString());
		return packagePath.toFile();
	}


	/**
	 * copy a url locally taking into account redirections
	 *
	 * @param url
	 * @param localFile
	 * @throws IOException
	 */
	@SuppressWarnings("nls")
	protected
	static void myCopy(URL url, File localFile, boolean report_error) throws IOException {
		if ("file".equals(url.getProtocol())) {
			FileUtils.copyFile(new File(url.getFile()), localFile);
			return;
		}
		try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(30000);
			conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
			conn.addRequestProperty("User-Agent", "Mozilla");
			conn.addRequestProperty("Referer", "google.com");

			// normally, 3xx is redirect
			int status = conn.getResponseCode();

			if (status == HttpURLConnection.HTTP_OK) {
				Files.copy(url.openStream(), localFile.toPath(), REPLACE_EXISTING);
				return;
			}

			if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER) {
				Files.copy(new URL(conn.getHeaderField("Location")).openStream(), localFile.toPath(), REPLACE_EXISTING);
				return;
			}
			if (report_error) {
				Common.log(new Status(IStatus.WARNING, Activator.getId(),
						"Failed to download url " + url + " error code is: " + status, null));
			}
			throw new IOException("Failed to download url " + url + " error code is: " + status);

		} catch (Exception e) {
			if (report_error) {
				Common.log(new Status(IStatus.WARNING, Activator.getId(), "Failed to download url " + url, e));
			}
			throw e;

		}
	}

	
	
	/**
	 * copy a url locally taking into account redirections in such a way that if
	 * there is already a file it does not get lost if the download fails
	 *
	 * @param url
	 * @param localFile
	 * @throws IOException
	 */
	protected static void mySafeCopy(URL url, File localFile, boolean report_error) throws IOException {
		File savedFile = null;
		if (localFile.exists()) {
			savedFile = File.createTempFile(localFile.getName(), "Sloeber"); //$NON-NLS-1$
			Files.move(localFile.toPath(), savedFile.toPath(), REPLACE_EXISTING);
		}
		try {
			myCopy(url, localFile, report_error);
		} catch (Exception e) {
			if (null != savedFile) {
				Files.move(savedFile.toPath(), localFile.toPath(), REPLACE_EXISTING);
			}
			throw e;
		}
	}
	
	public static String[] getJsonURLList() {
		return ConfigurationPreferences.getJsonURLList();
	}

	/**
	 * Completely replace the list with jsons with a new list
	 *
	 * @param newJsonUrls
	 */
	public static void setJsonURLs(String[] newJsonUrls) {

		String curJsons[] = getJsonURLList();
		HashSet<String> origJsons = new HashSet<>(Arrays.asList(curJsons));
		HashSet<String> currentSelectedJsons = new HashSet<>(Arrays.asList(newJsonUrls));
		origJsons.removeAll(currentSelectedJsons);
		// remove the files from disk which were in the old lst but not in the
		// new one
		for (String curJson : origJsons) {
			try {
				File localFile = getLocalFileName(curJson, false);
				if (localFile.exists()) {
					localFile.delete();
				}
			} catch ( Exception e) {
				// ignore
			}
		}
		// save to configurationsettings before calling LoadIndices
		ConfigurationPreferences.setJsonURLs(newJsonUrls);
		// reload the indices (this will remove all potential remaining
		// references
		// existing files do not need to be refreshed as they have been
		// refreshed at startup
		// new files will be added
		loadJsons(false);
	}

	public static String getDefaultURLs() {
		return ConfigurationPreferences.getDefaultJsonURLs();
	}

	public static void removeAllInstalledPlatforms() {
		try {
			FileUtils.deleteDirectory(ConfigurationPreferences.getInstallationPathPackages().toFile());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static IPath getInstallationPath() {
	    return ConfigurationPreferences.getInstallationPath();
	}
}
