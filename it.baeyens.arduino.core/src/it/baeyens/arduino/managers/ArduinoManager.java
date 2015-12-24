/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Jan Baeyens integrated in and extended for the arduino eclipse plugin
 *******************************************************************************/
package it.baeyens.arduino.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.ui.Activator;

public class ArduinoManager {

    public static final String LIBRARIES_URL = "http://downloads.arduino.cc/libraries/library_index.json"; //$NON-NLS-1$
    static private List<PackageIndex> packageIndices;
    static private LibraryIndex libraryIndex;
    static private String stringSplitter = "\n";//$NON-NLS-1$

    private static void internalLoadIndices() {
	String[] boardUrls = ArduinoPreferences.getBoardUrls().split(stringSplitter);
	packageIndices = new ArrayList<>(boardUrls.length);
	for (String boardUrl : boardUrls) {
	    loadPackageIndex(boardUrl, false);
	}

	loadLibraryIndex(false);
    }

    /**
     * Loads all stuff needed and if this is the first time downloads the avr boards and needed tools
     */
    static public void startup_Pluging() {
	loadIndices(true);
	try {
	    List<ArduinoBoard> allBoards = getInstalledBoards();
	    if (allBoards.isEmpty()) {
		String platformName = "Arduino AVR Boards"; //$NON-NLS-1$
		ArduinoPackage pkg = packageIndices.get(0).getPackages().get(0);
		if (pkg != null) {
		    ArduinoPlatform platform = pkg.getLatestPlatform(platformName);
		    if (platform == null) {
			ArduinoPlatform platformList[] = new ArduinoPlatform[pkg.getLatestPlatforms().size()];
			pkg.getLatestPlatforms().toArray(platformList);
			platform = platformList[0];
		    }
		    if (platform != null) {
			downloadAndInstall(platform, false, null);
		    }
		}
	    }
	} catch (CoreException e) {
	    e.printStackTrace();
	}
	ArduinoInstancePreferences.setConfigured();

    }

    /**
     * Given a platform description in a json file download and install all needed stuff. All stuff is including all tools and core files and hardware
     * specific libraries. That is (on windows) inclusive the make.exe
     * 
     * @param platform
     * @param monitor
     * @return
     */
    @SuppressWarnings("resource")
    static public IStatus downloadAndInstall(ArduinoPlatform platform, boolean forceDownload, IProgressMonitor monitor) {

	IStatus status = downloadAndInstall(platform.getUrl(), platform.getArchiveFileName(), platform.getInstallPath(), forceDownload, monitor);
	if (!status.isOK()) {
	    return status;
	}
	MultiStatus mstatus = new MultiStatus(status.getPlugin(), status.getCode(), status.getMessage(), status.getException());

	List<ToolDependency> tools = platform.getToolsDependencies();
	// make a platform_plugin.txt file to store the tool paths
	File pluginFile = platform.getPluginFile();
	PrintWriter writer = null;

	try {
	    writer = new PrintWriter(pluginFile, "UTF-8");//$NON-NLS-1$
	    writer.println("#This is a automatically generated file by the Arduino eclipse plugin"); //$NON-NLS-1$
	    writer.println("#only edit if you know what you are doing"); //$NON-NLS-1$
	    writer.println("#Have fun"); //$NON-NLS-1$
	    writer.println("#Jantje"); //$NON-NLS-1$
	    writer.println();
	} catch (FileNotFoundException | UnsupportedEncodingException e) {
	    mstatus.add(new Status(IStatus.WARNING, Activator.getId(),
		    "Unable to create file :" + pluginFile + '\n' + platform.getName() + " will not work", e)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	for (ToolDependency tool : tools) {

	    status = tool.install(monitor);
	    if (!status.isOK()) {
		mstatus.add(status);
	    }
	    if (writer != null) {
		try {
		    writer.println("runtime.tools." + tool.getName() + ".path=" + tool.getTool().getInstallPath());//$NON-NLS-1$ //$NON-NLS-2$
		    writer.println("runtime.tools." + tool.getName() + tool.getVersion() + ".path=" + tool.getTool().getInstallPath());//$NON-NLS-1$ //$NON-NLS-2$
		} catch (CoreException e) {
		    mstatus.add(new Status(IStatus.WARNING, Activator.getId(),
			    "Unable to write tofile file :" + pluginFile + '\n' + tool.getName() + " will not work", e)); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    }
	}
	if (writer != null) {
	    writer.close();
	}

	// On Windows install make from equations.org
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    try {
		Path makePath = ArduinoPreferences.getArduinoHome().resolve("tools/make/make.exe"); //$NON-NLS-1$
		if (!makePath.toFile().exists()) {
		    Files.createDirectories(makePath.getParent());
		    URL makeUrl = new URL("ftp://ftp.equation.com/make/32/make.exe"); //$NON-NLS-1$
		    Files.copy(makeUrl.openStream(), makePath);
		    makePath.toFile().setExecutable(true, false);
		}

	    } catch (IOException e) {
		mstatus.add(new Status(IStatus.ERROR, Activator.getId(), "downloading make.exe", e));
	    }
	}

	return mstatus.getChildren().length == 0 ? Status.OK_STATUS : mstatus;

    }

    static public void loadIndices(boolean immediatly) {
	if (immediatly) {
	    internalLoadIndices();
	    return;
	}
	new Job("Fetching package index") { //$NON-NLS-1$
	    @SuppressWarnings("synthetic-access")
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		internalLoadIndices();
		return Status.OK_STATUS;
	    }
	}.schedule();
    }

    static private void loadPackageIndex(String url, boolean download) {
	try {
	    URL packageUrl = new URL(url.trim());
	    Path packagePath = ArduinoPreferences.getArduinoHome().resolve(Paths.get(packageUrl.getPath()).getFileName());
	    File packageFile = packagePath.toFile();
	    if (!packageFile.exists() || download) {
		packagePath.getParent().toFile().mkdirs();
		Files.copy(packageUrl.openStream(), packagePath, StandardCopyOption.REPLACE_EXISTING);
	    }
	    if (packageFile.exists()) {
		try (Reader reader = new FileReader(packageFile)) {
		    PackageIndex index = new Gson().fromJson(reader, PackageIndex.class);
		    index.setOwners(null);
		    packageIndices.add(index);
		}
	    }
	} catch (IOException e) {
	    Activator.log(e);
	}
    }

    static public List<PackageIndex> getPackageIndices() {
	if (packageIndices == null) {
	    String[] boardUrls = ArduinoPreferences.getBoardUrls().split(stringSplitter);
	    packageIndices = new ArrayList<>(boardUrls.length);
	    for (String boardUrl : boardUrls) {
		loadPackageIndex(boardUrl, false);
	    }
	}
	return packageIndices;
    }

    private static void loadLibraryIndex(boolean download) {
	try {
	    URL librariesUrl = new URL(LIBRARIES_URL);
	    Path librariesPath = ArduinoPreferences.getArduinoHome().resolve(Paths.get(librariesUrl.getPath()).getFileName());
	    File librariesFile = librariesPath.toFile();
	    if (!librariesFile.exists() || download) {
		Files.copy(librariesUrl.openStream(), librariesPath, StandardCopyOption.REPLACE_EXISTING);
	    }
	    if (librariesFile.exists()) {
		try (Reader reader = new FileReader(librariesFile)) {
		    libraryIndex = new Gson().fromJson(reader, LibraryIndex.class);
		    libraryIndex.resolve();
		}
	    }
	} catch (IOException e) {
	    Activator.log(e);
	}

    }

    static public LibraryIndex getLibraryIndex() {
	if (libraryIndex == null) {
	    loadLibraryIndex(false);
	}
	return libraryIndex;
    }

    static public ArduinoBoard getBoard(String boardName, String platformName, String packageName) throws CoreException {
	for (PackageIndex index : packageIndices) {
	    ArduinoPackage pkg = index.getPackage(packageName);
	    if (pkg != null) {
		ArduinoPlatform platform = pkg.getLatestPlatform(platformName);
		if (platform != null) {
		    ArduinoBoard board = platform.getBoard(boardName);
		    if (board != null) {
			return board;
		    }
		}
	    }
	}
	return null;
    }

    static public List<ArduinoBoard> getBoards() throws CoreException {
	List<ArduinoBoard> boards = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    for (ArduinoPackage pkg : index.getPackages()) {
		for (ArduinoPlatform platform : pkg.getLatestPlatforms()) {
		    boards.addAll(platform.getBoards());
		}
	    }
	}
	return boards;
    }

    public static List<ArduinoPlatform> getPlatforms() {
	List<ArduinoPlatform> platforms = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    for (ArduinoPackage pkg : index.getPackages()) {
		platforms.addAll(pkg.getPlatforms());
	    }
	}
	return platforms;
    }

    static public List<ArduinoBoard> getInstalledBoards() throws CoreException {
	List<ArduinoBoard> boards = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    for (ArduinoPackage pkg : index.getPackages()) {
		for (ArduinoPlatform platform : pkg.getInstalledPlatforms()) {
		    boards.addAll(platform.getBoards());
		}
	    }
	}
	return boards;
    }

    static public List<ArduinoPackage> getPackages() {
	List<ArduinoPackage> packages = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    packages.addAll(index.getPackages());
	}
	return packages;
    }

    static public ArduinoPackage getPackage(String packageName) {
	for (PackageIndex index : packageIndices) {
	    ArduinoPackage pkg = index.getPackage(packageName);
	    if (pkg != null) {
		return pkg;
	    }
	}
	return null;
    }

    static public ArduinoTool getTool(String packageName, String toolName, String version) {
	for (PackageIndex index : packageIndices) {
	    ArduinoPackage pkg = index.getPackage(packageName);
	    if (pkg != null) {
		ArduinoTool tool = pkg.getTool(toolName, version);
		if (tool != null) {
		    return tool;
		}
	    }
	}
	return null;
    }

    private static final String LIBRARIES = "libraries"; //$NON-NLS-1$

    private static IEclipsePreferences getSettings(IProject project) {
	return new ProjectScope(project).getNode(Activator.getId());
    }

    public static Collection<ArduinoLibrary> getLibraries(IProject project) {
	IEclipsePreferences settings = getSettings(project);
	String librarySetting = settings.get(LIBRARIES, "[]"); //$NON-NLS-1$
	Type stringSet = new TypeToken<Set<String>>() {
	    // don'taskme why this is empty
	}.getType();
	Set<String> libraryNames = new Gson().fromJson(librarySetting, stringSet);
	LibraryIndex index = getLibraryIndex();
	List<ArduinoLibrary> libraries = new ArrayList<>(libraryNames.size());
	for (String name : libraryNames) {
	    libraries.add(index.getLibrary(name));
	}
	return libraries;
    }

    public static void setLibraries(final IProject project, final Collection<ArduinoLibrary> libraries) {
	List<String> libraryNames = new ArrayList<>(libraries.size());
	for (ArduinoLibrary library : libraries) {
	    libraryNames.add(library.getName());
	}
	IEclipsePreferences settings = getSettings(project);
	settings.put(LIBRARIES, new Gson().toJson(libraryNames));
	try {
	    settings.flush();
	} catch (BackingStoreException e) {
	    Activator.log(e);
	}

	new Job("Install libraries") { //$NON-NLS-1$
	    @Override
	    protected IStatus run(IProgressMonitor monitor) {
		MultiStatus mstatus = new MultiStatus(Activator.getId(), 0, "Installing libraries", null); //$NON-NLS-1$
		for (ArduinoLibrary library : libraries) {
		    IStatus status = library.install(monitor);
		    if (!status.isOK()) {
			mstatus.add(status);
		    }
		}

		// Clear the scanner info caches to pick up new includes
		// TODO check if this clean is needed and if so make sure it is run
		// try {
		// for (IBuildConfiguration config : project.getBuildConfigs()) {
		// ArduinoBuildConfiguration arduinoConfig = config.getAdapter(ArduinoBuildConfiguration.class);
		// arduinoConfig.clearScannerInfo();
		// }
		// } catch (CoreException e) {
		// mstatus.add(e.getStatus());
		// }
		return mstatus;
	    }
	}.schedule();
    }

    public static IStatus downloadAndInstall(String url, String archiveFileName, Path installPath, boolean forceDownload, IProgressMonitor monitor) {
	Path dlDir = ArduinoPreferences.getArduinoHome().resolve("downloads"); //$NON-NLS-1$
	Path archivePath = dlDir.resolve(archiveFileName);
	String archiveFullFileName = archivePath.toString();
	try {
	    URL dl = new URL(url);
	    Files.createDirectories(dlDir);
	    if (!archivePath.toFile().exists() || forceDownload) {
		Files.copy(dl.openStream(), archivePath, StandardCopyOption.REPLACE_EXISTING);
	    }
	} catch (IOException e) {
	    return new Status(IStatus.ERROR, Activator.getId(), "Failed to download. " + url, e);//$NON-NLS-1$
	}

	// Create an ArchiveInputStream with the correct archiving algorithm
	if (archiveFileName.endsWith("tar.bz2")) { //$NON-NLS-1$
	    try (ArchiveInputStream inStream = new TarArchiveInputStream(new BZip2CompressorInputStream(new FileInputStream(archiveFullFileName)))) {
		return extract(inStream, installPath.toFile(), 1, forceDownload);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), "Failed to extract tar.bz2. " + archiveFullFileName, e);//$NON-NLS-1$
	    }
	} else if (archiveFileName.endsWith("zip")) { //$NON-NLS-1$
	    try (ArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(archiveFullFileName))) {
		return extract(in, installPath.toFile(), 1, forceDownload);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), "Failed to extract zip. " + archiveFullFileName, e);//$NON-NLS-1$
	    }
	} else if (archiveFileName.endsWith("tar.gz")) { //$NON-NLS-1$
	    try (ArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(archiveFullFileName)))) {
		return extract(in, installPath.toFile(), 1, forceDownload);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), "Failed to extract tar.gz. " + archiveFullFileName, e);//$NON-NLS-1$
	    }
	} else if (archiveFileName.endsWith("tar")) { //$NON-NLS-1$
	    try (ArchiveInputStream in = new TarArchiveInputStream(new FileInputStream(archiveFullFileName))) {
		return extract(in, installPath.toFile(), 1, forceDownload);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), "Failed to extract tar. " + archiveFullFileName, e);//$NON-NLS-1$
	    }
	}

	return new Status(IStatus.ERROR, Activator.getId(), "Archive format not supported.");//$NON-NLS-1$

    }

    public static IStatus extract(ArchiveInputStream in, File destFolder, int stripPath, boolean overwrite) throws IOException, InterruptedException {

	// Folders timestamps must be set at the end of archive extraction
	// (because creating a file in a folder alters the folder's timestamp)
	Map<File, Long> foldersTimestamps = new HashMap<>();

	String pathPrefix = ""; //$NON-NLS-1$

	Map<File, File> hardLinks = new HashMap<>();
	Map<File, Integer> hardLinksMode = new HashMap<>();
	Map<File, String> symLinks = new HashMap<>();
	Map<File, Long> symLinksModifiedTimes = new HashMap<>();

	// Cycle through all the archive entries
	while (true) {
	    ArchiveEntry entry = in.getNextEntry();
	    if (entry == null) {
		break;
	    }

	    // Extract entry info
	    long size = entry.getSize();
	    String name = entry.getName();
	    boolean isDirectory = entry.isDirectory();
	    boolean isLink = false;
	    boolean isSymLink = false;
	    String linkName = null;
	    Integer mode = null;
	    long modifiedTime = entry.getLastModifiedDate().getTime();

	    {
		// Skip MacOSX metadata
		// http://superuser.com/questions/61185/why-do-i-get-files-like-foo-in-my-tarball-on-os-x
		int slash = name.lastIndexOf('/');
		if (slash == -1) {
		    if (name.startsWith("._")) { //$NON-NLS-1$
			continue;
		    }
		} else {
		    if (name.substring(slash + 1).startsWith("._")) { //$NON-NLS-1$
			continue;
		    }
		}
	    }

	    // Skip git metadata
	    // http://www.unix.com/unix-for-dummies-questions-and-answers/124958-file-pax_global_header-means-what.html
	    if (name.contains("pax_global_header")) { //$NON-NLS-1$
		continue;
	    }

	    if (entry instanceof TarArchiveEntry) {
		TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
		mode = tarEntry.getMode();
		isLink = tarEntry.isLink();
		isSymLink = tarEntry.isSymbolicLink();
		linkName = tarEntry.getLinkName();
	    }

	    // On the first archive entry, if requested, detect the common path
	    // prefix to be stripped from filenames
	    if (stripPath > 0 && pathPrefix.isEmpty()) {
		int slash = 0;
		while (stripPath > 0) {
		    slash = name.indexOf("/", slash); //$NON-NLS-1$
		    if (slash == -1) {
			throw new IOException("Invalid archive: it must contain a single root folder");
		    }
		    slash++;
		    stripPath--;
		}
		pathPrefix = name.substring(0, slash);
	    }

	    // Strip the common path prefix when requested
	    if (!name.startsWith(pathPrefix)) {
		throw new IOException("Invalid archive: it must contain a single root folder while file " + name + " is outside " + pathPrefix);
	    }
	    name = name.substring(pathPrefix.length());
	    if (name.isEmpty()) {
		continue;
	    }
	    File outputFile = new File(destFolder, name);

	    File outputLinkedFile = null;
	    if (isLink) {
		if (!linkName.startsWith(pathPrefix)) {
		    throw new IOException(
			    "Invalid archive: it must contain a single root folder while file " + linkName + " is outside " + pathPrefix);
		}
		linkName = linkName.substring(pathPrefix.length());
		outputLinkedFile = new File(destFolder, linkName);
	    }
	    if (isSymLink) {
		// Symbolic links are referenced with relative paths
		outputLinkedFile = new File(linkName);
		if (outputLinkedFile.isAbsolute()) {
		    System.err.println("Warning: file " + outputFile + " links to an absolute path " + outputLinkedFile);
		    System.err.println();
		}
	    }

	    // Safety check
	    if (isDirectory) {
		if (outputFile.isFile() && !overwrite) {
		    throw new IOException("Can't create folder " + outputFile + ", a file with the same name exists!");
		}
	    } else {
		// - isLink
		// - isSymLink
		// - anything else
		if (outputFile.exists() && !overwrite) {
		    throw new IOException("Can't extract file " + outputFile + ", file already exists!");
		}
	    }

	    // Extract the entry
	    if (isDirectory) {
		if (!outputFile.exists() && !outputFile.mkdirs()) {
		    throw new IOException("Could not create folder: " + outputFile);
		}
		foldersTimestamps.put(outputFile, modifiedTime);
	    } else if (isLink) {
		hardLinks.put(outputFile, outputLinkedFile);
		hardLinksMode.put(outputFile, mode);
	    } else if (isSymLink) {
		symLinks.put(outputFile, linkName);
		symLinksModifiedTimes.put(outputFile, modifiedTime);
	    } else {
		// Create the containing folder if not exists
		if (!outputFile.getParentFile().isDirectory()) {
		    outputFile.getParentFile().mkdirs();
		}
		copyStreamToFile(in, size, outputFile);
		outputFile.setLastModified(modifiedTime);
	    }

	    // Set file/folder permission
	    if (mode != null && !isSymLink && outputFile.exists()) {
		chmod(outputFile, mode);
	    }
	}

	for (Map.Entry<File, File> entry : hardLinks.entrySet()) {
	    if (entry.getKey().exists() && overwrite) {
		entry.getKey().delete();
	    }
	    link(entry.getValue(), entry.getKey());
	    Integer mode = hardLinksMode.get(entry.getKey());
	    if (mode != null) {
		chmod(entry.getKey(), mode);
	    }
	}

	for (Map.Entry<File, String> entry : symLinks.entrySet()) {
	    if (entry.getKey().exists() && overwrite) {
		entry.getKey().delete();
	    }
	    symlink(entry.getValue(), entry.getKey());
	    entry.getKey().setLastModified(symLinksModifiedTimes.get(entry.getKey()));
	}

	// Set folders timestamps
	for (

	File folder : foldersTimestamps.keySet())

	{
	    folder.setLastModified(foldersTimestamps.get(folder));
	}
	return Status.OK_STATUS;

    }

    private static void symlink(String something, File somewhere) throws IOException, InterruptedException {
	Process process = Runtime.getRuntime().exec(new String[] { "ln", "-s", something, somewhere.getAbsolutePath() }, null, //$NON-NLS-1$ //$NON-NLS-2$
		somewhere.getParentFile());
	process.waitFor();
    }

    private static void link(File something, File somewhere) throws IOException, InterruptedException {
	Process process = Runtime.getRuntime().exec(new String[] { "ln", something.getAbsolutePath(), somewhere.getAbsolutePath() }, null, null); //$NON-NLS-1$
	process.waitFor();
    }

    private static void chmod(File file, int mode) throws IOException, InterruptedException {
	Process process = Runtime.getRuntime().exec(new String[] { "chmod", Integer.toOctalString(mode), file.getAbsolutePath() }, null, null); //$NON-NLS-1$
	process.waitFor();
    }

    private static void copyStreamToFile(InputStream in, long size, File outputFile) throws IOException {
	FileOutputStream fos = null;
	try {
	    fos = new FileOutputStream(outputFile);
	    // if size is not available, copy until EOF...
	    if (size == -1) {
		byte buffer[] = new byte[4096];
		int length;
		while ((length = in.read(buffer)) != -1) {
		    fos.write(buffer, 0, length);
		}
		return;
	    }

	    // ...else copy just the needed amount of bytes
	    byte buffer[] = new byte[4096];
	    while (size > 0) {
		int length = in.read(buffer);
		if (length <= 0) {
		    throw new IOException("Error while extracting file " + outputFile.getAbsolutePath());
		}
		fos.write(buffer, 0, length);
		size -= length;
	    }
	} finally {
	    IOUtils.closeQuietly(fos);
	}
    }

    private static Set<PosixFilePermission> toPerms(int mode) {
	Set<PosixFilePermission> perms = new HashSet<>();
	if ((mode & 0400) != 0) {
	    perms.add(PosixFilePermission.OWNER_READ);
	}
	if ((mode & 0200) != 0) {
	    perms.add(PosixFilePermission.OWNER_WRITE);
	}
	if ((mode & 0100) != 0) {
	    perms.add(PosixFilePermission.OWNER_EXECUTE);
	}
	if ((mode & 0040) != 0) {
	    perms.add(PosixFilePermission.GROUP_READ);
	}
	if ((mode & 0020) != 0) {
	    perms.add(PosixFilePermission.GROUP_WRITE);
	}
	if ((mode & 0010) != 0) {
	    perms.add(PosixFilePermission.GROUP_EXECUTE);
	}
	if ((mode & 0004) != 0) {
	    perms.add(PosixFilePermission.OTHERS_READ);
	}
	if ((mode & 0002) != 0) {
	    perms.add(PosixFilePermission.OTHERS_WRITE);
	}
	if ((mode & 0001) != 0) {
	    perms.add(PosixFilePermission.OTHERS_EXECUTE);
	}
	return perms;
    }

    public static int compareVersions(String version1, String version2) {
	if (version1 == null) {
	    return version2 == null ? 0 : -1;
	}

	if (version2 == null) {
	    return 1;
	}

	String[] v1 = version1.split("\\."); //$NON-NLS-1$
	String[] v2 = version2.split("\\."); //$NON-NLS-1$
	for (int i = 0; i < Math.max(v1.length, v2.length); ++i) {
	    if (v1.length <= i) {
		return v2.length < i ? 0 : -1;
	    }

	    if (v2.length <= i) {
		return 1;
	    }

	    try {
		int vi1 = Integer.parseInt(v1[i]);
		int vi2 = Integer.parseInt(v2[i]);
		if (vi1 < vi2) {
		    return -1;
		}

		if (vi1 > vi2) {
		    return 1;
		}
	    } catch (NumberFormatException e) {
		// not numbers, do string compares
		int c = v1[i].compareTo(v2[i]);
		if (c < 0) {
		    return -1;
		}
		if (c > 0) {
		    return 1;
		}
	    }
	}

	return 0;
    }

}
