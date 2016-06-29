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
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.gson.Gson;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.common.Defaults;
import it.baeyens.arduino.ui.Activator;

public class Manager {

    static private List<PackageIndex> packageIndices;
    static private LibraryIndex libraryIndex;

    private Manager() {
    }

    private static void internalLoadIndices() {
	String[] boardUrls = ConfigurationPreferences.getBoardURLList();
	packageIndices = new ArrayList<>(boardUrls.length);
	for (String boardUrl : boardUrls) {
	    loadPackageIndex(boardUrl, false);
	}

	loadLibraryIndex(false);
    }

    /**
     * Loads all stuff needed and if this is the first time downloads the avr
     * boards and needed tools
     * 
     * @param monitor
     */
    public static void startup_Pluging(IProgressMonitor monitor) {
	loadIndices(true);
	try {
	    List<Board> allBoards = getInstalledBoards();
	    if (allBoards.isEmpty()) { // we test for boards
		// InformUserOfInstallationStart(monitor);
		// so first do the libraries

		InstallDefaultLibraries(monitor);

		// Downmload sample programs
		downloadAndInstall(Defaults.EXAMPLES_URL, Defaults.EXAMPLE_PACKAGE,
			Paths.get(ConfigurationPreferences.getInstallationPathExamples().toString()), false, monitor);

		// now add the boards
		Package pkg = packageIndices.get(0).getPackages().get(0);
		if (pkg != null) {
		    ArduinoPlatform platform = pkg.getLatestPlatform(Defaults.PLATFORM_NAME);
		    if (platform == null) {
			ArduinoPlatform[] platformList = new ArduinoPlatform[pkg.getLatestPlatforms().size()];
			pkg.getLatestPlatforms().toArray(platformList);
			platform = platformList[0];
		    }
		    if (platform != null) {
			downloadAndInstall(platform, false, monitor);
		    }
		}
	    }
	} catch (CoreException e) {
	    e.printStackTrace();
	}

    }

    private static void InstallDefaultLibraries(IProgressMonitor monitor) {
	LibraryIndex libindex = getLibraryIndex();

	for (String library : Defaults.INSTALLED_LIBRARIES) {
	    Library toInstalLib = libindex.getLatestLibrary(library);
	    if (toInstalLib != null) {
		toInstalLib.install(monitor);
	    }
	}
    }

    /**
     * Given a platform description in a json file download and install all
     * needed stuff. All stuff is including all tools and core files and
     * hardware specific libraries. That is (on windows) inclusive the make.exe
     * 
     * @param platform
     * @param monitor
     * @param object
     * @return
     */
    static public IStatus downloadAndInstall(ArduinoPlatform platform, boolean forceDownload,
	    IProgressMonitor monitor) {

	IStatus status = downloadAndInstall(platform.getUrl(), platform.getArchiveFileName(), platform.getInstallPath(),
		forceDownload, monitor);
	if (!status.isOK()) {
	    return status;
	}
	MultiStatus mstatus = new MultiStatus(status.getPlugin(), status.getCode(), status.getMessage(),
		status.getException());

	for (ToolDependency tool : platform.getToolsDependencies()) {
	    monitor.setTaskName(InstallProgress.getRandomMessage());
	    mstatus.add(tool.install(monitor));
	}
	// On Windows install make from equations.org
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    try {
		Path makePath = Paths
			.get(ConfigurationPreferences.getPathExtensionPath().append("make.exe").toString()); //$NON-NLS-1$
		if (!makePath.toFile().exists()) {
		    Files.createDirectories(makePath.getParent());
		    URL makeUrl = new URL("ftp://ftp.equation.com/make/32/make.exe"); //$NON-NLS-1$
		    Files.copy(makeUrl.openStream(), makePath);
		    makePath.toFile().setExecutable(true, false);
		}

	    } catch (IOException e) {
		mstatus.add(new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Downloading_make_exe, e));
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

    /**
     * convert a web url to a local file name. The local file name is the cache
     * of the web
     * 
     * @param url
     *            url of the file we want a local cache
     * @return the file that represents the file that is the local cache. the
     *         file itself may not exists. If the url is malformed return null;
     * @throws MalformedURLException
     */
    public static File getLocalFileName(String url) {
	URL packageUrl;
	try {
	    packageUrl = new URL(url.trim());
	} catch (MalformedURLException e) {
	    Common.log(new Status(IStatus.ERROR, Activator.getId(), "Malformed url " + url, e)); //$NON-NLS-1$
	    return null;
	}
	String localFileName = Paths.get(packageUrl.getPath()).getFileName().toString();
	Path packagePath = Paths.get(ConfigurationPreferences.getInstallationPath().append(localFileName).toString());
	return packagePath.toFile();
    }

    /**
     * This method takes a json boards file url and downloads it and parses it
     * for usage in the boards manager
     * 
     * @param url
     *            the url of the file to download and load
     * @param forceDownload
     *            set true if you want to download the file even if it is
     *            already available locally
     */
    static private void loadPackageIndex(String url, boolean forceDownload) {
	File packageFile = getLocalFileName(url);
	if (packageFile == null) {
	    return;
	}
	if (!packageFile.exists() || forceDownload) {
	    packageFile.getParentFile().mkdirs();
	    try {
		Files.copy(new URL(url.trim()).openStream(), packageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	    } catch (IOException e) {
		Common.log(new Status(IStatus.ERROR, Activator.getId(), "Unable to download " + url, e)); //$NON-NLS-1$
	    }
	}
	if (packageFile.exists()) {
	    try (Reader reader = new FileReader(packageFile)) {
		PackageIndex index = new Gson().fromJson(reader, PackageIndex.class);
		index.setOwners(null);
		packageIndices.add(index);
	    } catch (Exception e) {
		Common.log(new Status(IStatus.ERROR, Activator.getId(),
			"Unable to parse " + packageFile.getAbsolutePath(), e)); //$NON-NLS-1$
		packageFile.delete();// Delete the file so it stops damaging
	    }
	}
    }

    static public List<PackageIndex> getPackageIndices() {
	if (packageIndices == null) {
	    String[] boardUrls = ConfigurationPreferences.getBoardURLList();
	    packageIndices = new ArrayList<>(boardUrls.length);
	    for (String boardUrl : boardUrls) {
		loadPackageIndex(boardUrl, false);
	    }
	}
	return packageIndices;
    }

    private static void loadLibraryIndex(boolean download) {
	try {
	    URL librariesUrl = new URL(Defaults.LIBRARIES_URL);
	    String localFileName = Paths.get(librariesUrl.getPath()).getFileName().toString();
	    Path librariesPath = Paths
		    .get(ConfigurationPreferences.getInstallationPath().append(localFileName).toString());
	    File librariesFile = librariesPath.toFile();
	    if (!librariesFile.exists() || download) {
		librariesPath.getParent().toFile().mkdirs();
		Files.copy(librariesUrl.openStream(), librariesPath, StandardCopyOption.REPLACE_EXISTING);
	    }
	    if (librariesFile.exists()) {
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(librariesFile),
			Charset.forName("UTF8"))) { //$NON-NLS-1$
		    libraryIndex = new Gson().fromJson(reader, LibraryIndex.class);
		    libraryIndex.resolve();
		}
	    }
	} catch (IOException e) {
	    Common.log(new Status(IStatus.WARNING, Activator.getId(), "Failed to load library index", e)); //$NON-NLS-1$
	}

    }

    static public LibraryIndex getLibraryIndex() {
	if (libraryIndex == null) {
	    loadLibraryIndex(false);
	}
	return libraryIndex;
    }

    static public Board getBoard(String boardName, String platformName, String packageName) throws CoreException {
	for (PackageIndex index : packageIndices) {
	    Package pkg = index.getPackage(packageName);
	    if (pkg != null) {
		ArduinoPlatform platform = pkg.getLatestPlatform(platformName);
		if (platform != null) {
		    Board board = platform.getBoard(boardName);
		    if (board != null) {
			return board;
		    }
		}
	    }
	}
	return null;
    }

    static public List<Board> getBoards() throws CoreException {
	List<Board> boards = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    for (Package pkg : index.getPackages()) {
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
	    for (Package pkg : index.getPackages()) {
		platforms.addAll(pkg.getPlatforms());
	    }
	}
	return platforms;
    }

    public static IPath getPlatformFile(String vendor, String architecture) {

	for (PackageIndex index : packageIndices) {
	    for (Package pkg : index.getPackages()) {
		for (ArduinoPlatform curPlatform : pkg.getInstalledPlatforms()) {
		    if (architecture.equalsIgnoreCase(curPlatform.getArchitecture())
			    && (vendor.equalsIgnoreCase(pkg.getMaintainer()))) {
			ArduinoPlatform latestPlatform = pkg.getLatestPlatform(curPlatform.getName());
			return new org.eclipse.core.runtime.Path(latestPlatform.getPlatformFile().toString());
		    }
		}
	    }
	}
	return null;
    }

    public static ArduinoPlatform getPlatform(String platformTxt) {
	String searchString = new File(platformTxt).toString();
	for (PackageIndex index : packageIndices) {
	    for (Package pkg : index.getPackages()) {
		for (ArduinoPlatform curPlatform : pkg.getPlatforms()) {
		    String curFile = curPlatform.getPlatformFile().toString();
		    if (searchString.equals(curFile)) {
			return curPlatform;
		    }
		}
	    }
	}
	return null;
    }

    static public List<Board> getInstalledBoards() throws CoreException {
	List<Board> boards = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    for (Package pkg : index.getPackages()) {
		for (ArduinoPlatform platform : pkg.getInstalledPlatforms()) {
		    boards.addAll(platform.getBoards());
		}
	    }
	}
	return boards;
    }

    static public List<Package> getPackages() {
	List<Package> packages = new ArrayList<>();
	for (PackageIndex index : packageIndices) {
	    packages.addAll(index.getPackages());
	}
	return packages;
    }

    static public Package getPackage(String packageName) {
	for (PackageIndex index : packageIndices) {
	    Package pkg = index.getPackage(packageName);
	    if (pkg != null) {
		return pkg;
	    }
	}
	return null;
    }

    static public Tool getTool(String packageName, String toolName, String version) {
	for (PackageIndex index : packageIndices) {
	    Package pkg = index.getPackage(packageName);
	    if (pkg != null) {
		Tool tool = pkg.getTool(toolName, version);
		if (tool != null) {
		    return tool;
		}
	    }
	}
	return null;
    }

    /**
     * downloads an archive file from the internet and saves it in the download
     * folder under the name "pArchiveFileName" then extrats the file to
     * pInstallPath if pForceDownload is true the file will be downloaded even
     * if the download file already exists if pForceDownload is false the file
     * will only be downloaded if the download file does not exists The
     * extraction is done with processArchive so only files types supported by
     * this method will be properly extracted
     * 
     * @param pURL
     *            the url of the file to download
     * @param pArchiveFileName
     *            the name of the file in the download folder
     * @param pInstallPath
     * @param pForceDownload
     * @param pMonitor
     * @return
     */
    public static IStatus downloadAndInstall(String pURL, String pArchiveFileName, Path pInstallPath,
	    boolean pForceDownload, IProgressMonitor pMonitor) {
	IPath dlDir = ConfigurationPreferences.getInstallationPathDownload();
	IPath archivePath = dlDir.append(pArchiveFileName);
	String archiveFullFileName = archivePath.toString();
	try {
	    URL dl = new URL(pURL);
	    dlDir.toFile().mkdir();
	    if (!archivePath.toFile().exists() || pForceDownload) {
		pMonitor.subTask("Downloading " + pArchiveFileName + " .."); //$NON-NLS-1$ //$NON-NLS-2$
		Files.copy(dl.openStream(), Paths.get(archivePath.toString()), StandardCopyOption.REPLACE_EXISTING);
	    }
	} catch (IOException e) {
	    return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Failed_to_download + pURL, e);
	}
	return processArchive(pArchiveFileName, pInstallPath, pForceDownload, archiveFullFileName, pMonitor);
    }

    private static IStatus processArchive(String pArchiveFileName, Path pInstallPath, boolean pForceDownload,
	    String pArchiveFullFileName, IProgressMonitor pMonitor) {
	// Create an ArchiveInputStream with the correct archiving algorithm
	String faileToExtractMessage = Messages.Manager_Failed_to_extract + pArchiveFullFileName;
	if (pArchiveFileName.endsWith("tar.bz2")) { //$NON-NLS-1$
	    try (ArchiveInputStream inStream = new TarArchiveInputStream(
		    new BZip2CompressorInputStream(new FileInputStream(pArchiveFullFileName)))) {
		return extract(inStream, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
	    }
	} else if (pArchiveFileName.endsWith("zip")) { //$NON-NLS-1$
	    try (ArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(pArchiveFullFileName))) {
		return extract(in, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
	    }
	} else if (pArchiveFileName.endsWith("tar.gz")) { //$NON-NLS-1$
	    try (ArchiveInputStream in = new TarArchiveInputStream(
		    new GzipCompressorInputStream(new FileInputStream(pArchiveFullFileName)))) {
		return extract(in, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
	    }
	} else if (pArchiveFileName.endsWith("tar")) { //$NON-NLS-1$
	    try (ArchiveInputStream in = new TarArchiveInputStream(new FileInputStream(pArchiveFullFileName))) {
		return extract(in, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
	    } catch (IOException | InterruptedException e) {
		return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
	    }
	} else {
	    return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Format_not_supported);
	}
    }

    public static IStatus extract(ArchiveInputStream in, File destFolder, int stripPath, boolean overwrite,
	    IProgressMonitor pMonitor) throws IOException, InterruptedException {

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
	    Long modifiedTime = new Long(entry.getLastModifiedDate().getTime());

	    pMonitor.subTask("Processing " + name); //$NON-NLS-1$

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
		mode = new Integer(tarEntry.getMode());
		isLink = tarEntry.isLink();
		isSymLink = tarEntry.isSymbolicLink();
		linkName = tarEntry.getLinkName();
	    }

	    // On the first archive entry, if requested, detect the common path
	    // prefix to be stripped from filenames
	    int localstripPath = stripPath;
	    if (localstripPath > 0 && pathPrefix.isEmpty()) {
		int slash = 0;
		while (localstripPath > 0) {
		    slash = name.indexOf("/", slash); //$NON-NLS-1$
		    if (slash == -1) {
			throw new IOException(Messages.Manager_no_single_root_folder);
		    }
		    slash++;
		    localstripPath--;
		}
		pathPrefix = name.substring(0, slash);
	    }

	    // Strip the common path prefix when requested
	    if (!name.startsWith(pathPrefix)) {
		throw new IOException(Messages.Manager_no_single_root_folder_while_file + name
			+ Messages.Manager_is_outside + pathPrefix);
	    }
	    name = name.substring(pathPrefix.length());
	    if (name.isEmpty()) {
		continue;
	    }
	    File outputFile = new File(destFolder, name);

	    File outputLinkedFile = null;
	    if (isLink && linkName != null) {
		if (!linkName.startsWith(pathPrefix)) {
		    throw new IOException(Messages.Manager_no_single_root_folder_while_file + linkName
			    + Messages.Manager_is_outside + pathPrefix);
		}
		linkName = linkName.substring(pathPrefix.length());
		outputLinkedFile = new File(destFolder, linkName);
	    }
	    if (isSymLink) {
		// Symbolic links are referenced with relative paths
		outputLinkedFile = new File(linkName);
		if (outputLinkedFile.isAbsolute()) {
		    System.err.println(Messages.Manager_Warning_file + outputFile
			    + Messages.Manager_links_to_absolute_path + outputLinkedFile);
		    System.err.println();
		}
	    }

	    // Safety check
	    if (isDirectory) {
		if (outputFile.isFile() && !overwrite) {
		    throw new IOException(
			    Messages.Manager_Cant_create_folder + outputFile + Messages.Manager_File_exists);
		}
	    } else {
		// - isLink
		// - isSymLink
		// - anything else
		if (outputFile.exists() && !overwrite) {
		    throw new IOException(
			    Messages.Manager_Cant_extract_file + outputFile + Messages.Manager_File_already_exists);
		}
	    }

	    // Extract the entry
	    if (isDirectory) {
		if (!outputFile.exists() && !outputFile.mkdirs()) {
		    throw new IOException(Messages.Manager_Cant_create_folder + outputFile);
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
		outputFile.setLastModified(modifiedTime.longValue());
	    }

	    // Set file/folder permission
	    if (mode != null && !isSymLink && outputFile.exists()) {
		chmod(outputFile, mode.intValue());
	    }
	}

	for (Map.Entry<File, File> entry : hardLinks.entrySet()) {
	    if (entry.getKey().exists() && overwrite) {
		entry.getKey().delete();
	    }
	    link(entry.getValue(), entry.getKey());
	    Integer mode = hardLinksMode.get(entry.getKey());
	    if (mode != null) {
		chmod(entry.getKey(), mode.intValue());
	    }
	}

	for (Map.Entry<File, String> entry : symLinks.entrySet()) {
	    if (entry.getKey().exists() && overwrite) {
		entry.getKey().delete();
	    }
	    symlink(entry.getValue(), entry.getKey());
	    entry.getKey().setLastModified(symLinksModifiedTimes.get(entry.getKey()).longValue());
	}

	// Set folders timestamps
	for (Map.Entry<File, Long> entry : foldersTimestamps.entrySet()) {
	    entry.getKey().setLastModified(entry.getValue().longValue());
	}

	return Status.OK_STATUS;

    }

    private static void symlink(String something, File somewhere) throws IOException, InterruptedException {
	Process process = Runtime.getRuntime().exec(new String[] { "ln", "-s", something, somewhere.getAbsolutePath() }, //$NON-NLS-1$ //$NON-NLS-2$
		null, somewhere.getParentFile());
	process.waitFor();
    }

    private static void link(File something, File somewhere) throws IOException, InterruptedException {
	Process process = Runtime.getRuntime()
		.exec(new String[] { "ln", something.getAbsolutePath(), somewhere.getAbsolutePath() }, null, null); //$NON-NLS-1$
	process.waitFor();
    }

    private static void chmod(File file, int mode) throws IOException, InterruptedException {
	String octal = Integer.toOctalString(mode);
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    boolean ownerExecute = (((mode / (8 * 8)) & 1) == 1);
	    boolean ownerRead = (((mode / (8 * 8)) & 4) == 4);
	    boolean ownerWrite = (((mode / (8 * 8)) & 2) == 2);
	    boolean everyoneExecute = (((mode / 8) & 1) == 1);
	    boolean everyoneRead = (((mode / 8) & 4) == 4);
	    boolean everyoneWrite = (((mode / 8) & 2) == 2);
	    file.setWritable(true, false);
	    file.setExecutable(ownerExecute, !everyoneExecute);
	    file.setReadable(ownerRead, !everyoneRead);
	    file.setWritable(ownerWrite, !everyoneWrite);
	} else {
	    Process process = Runtime.getRuntime().exec(new String[] { "chmod", octal, file.getAbsolutePath() }, null, //$NON-NLS-1$
		    null);
	    process.waitFor();
	}
    }

    private static void copyStreamToFile(InputStream in, long size, File outputFile) throws IOException {
	try (FileOutputStream fos = new FileOutputStream(outputFile)) {

	    // if size is not available, copy until EOF...
	    if (size == -1) {
		byte[] buffer = new byte[4096];
		int length;
		while ((length = in.read(buffer)) != -1) {
		    fos.write(buffer, 0, length);
		}
		return;
	    }

	    // ...else copy just the needed amount of bytes
	    byte[] buffer = new byte[4096];
	    long leftToWrite = size;
	    while (leftToWrite > 0) {
		int length = in.read(buffer);
		if (length <= 0) {
		    throw new IOException(Messages.Manager_Failed_to_extract + outputFile.getAbsolutePath());
		}
		fos.write(buffer, 0, length);
		leftToWrite -= length;
	    }
	}
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
