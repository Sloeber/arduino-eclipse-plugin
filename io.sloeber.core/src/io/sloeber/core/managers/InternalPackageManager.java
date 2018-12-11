/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Jan Baeyens integrated in and extended for the arduino eclipse plugin named Sloeber
 *******************************************************************************/
package io.sloeber.core.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.MyMultiStatus;

public class InternalPackageManager extends PackageManager {

	private static final String  FILE = Messages.FILE;
	private static final String FOLDER = Messages.FOLDER;;
	private static boolean myIsReady = false;

	public static boolean isReady() {
		return myIsReady;
	}

	private InternalPackageManager() {
	}

	/**
	 * Loads all stuff needed and if this is the first time downloads the avr boards
	 * and needed tools
	 *
	 * @param monitor
	 */
	public static void startup_Pluging(IProgressMonitor monitor) {
		loadJsons(ConfigurationPreferences.getUpdateJasonFilesFlag());
		List<Board> allBoards = getInstalledBoards();
		if (!LibraryManager.libsAreInstalled()) {
			LibraryManager.InstallDefaultLibraries(monitor);
		}
		if (allBoards.isEmpty()) { // If boards are installed do nothing

			MyMultiStatus mstatus = new MyMultiStatus("Failed to configer Sloeber"); //$NON-NLS-1$

			// Download sample programs
			mstatus.addErrors(downloadAndInstall(Defaults.EXAMPLES_URL, Defaults.EXAMPLE_PACKAGE,
					ConfigurationPreferences.getInstallationPathExamples(), false, monitor));

			if (mstatus.isOK()) {
				// if successfully installed the examples: add the boards

				Package pkg = getPackageIndices().get(0).getPackages().get(0);
				if (pkg != null) {
					ArduinoPlatform platform = pkg.getLatestPlatform(Defaults.DEFAULT_INSTALL_PLATFORM_NAME, false);
					if (platform == null) {
						ArduinoPlatform[] platformList = new ArduinoPlatform[pkg.getLatestPlatforms().size()];
						pkg.getLatestPlatforms().toArray(platformList);
						platform = platformList[0];
					}
					if (platform != null) {
						mstatus.addErrors(downloadAndInstall(platform, false, monitor));
					}
				}
			}
			if (!mstatus.isOK()) {
				StatusManager stMan = StatusManager.getManager();
				stMan.handle(mstatus, StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK);
			}

		}
		myIsReady = true;

	}

	/**
	 * Given a platform description in a json file download and install all needed
	 * stuff. All stuff is including all tools and core files and hardware specific
	 * libraries. That is (on windows) inclusive the make.exe
	 *
	 * @param platform
	 * @param monitor
	 * @param object
	 * @return
	 */
	static public IStatus downloadAndInstall(ArduinoPlatform platform, boolean forceDownload,
			IProgressMonitor monitor) {

		MyMultiStatus mstatus = new MyMultiStatus("Failed to install " + platform.getName()); //$NON-NLS-1$
		mstatus.addErrors(downloadAndInstall(platform.getUrl(), platform.getArchiveFileName(),
				platform.getInstallPath(), forceDownload, monitor));
		if (!mstatus.isOK()) {
			// no use going on installing tools if the boards failed installing
			return mstatus;
		}

		if (platform.getToolsDependencies() != null) {
			for (ToolDependency tool : platform.getToolsDependencies()) {
				monitor.setTaskName(InstallProgress.getRandomMessage());
				mstatus.addErrors(tool.install(monitor));
			}
		}
		// On Windows install make
		if (SystemUtils.IS_OS_WINDOWS) {
			IPath localMakePath = ConfigurationPreferences.getMakePath();
			if (!ConfigurationPreferences.getMakePath().append("make.exe").toFile().exists()) { //$NON-NLS-1$
				mstatus.addErrors(
						downloadAndInstall("https://eclipse.baeyens.it/download/make.zip", "make.zip", localMakePath, //$NON-NLS-1$ //$NON-NLS-2$
								forceDownload, monitor));
			}
		}

		// always replace -DARDUINO_BOARD="{build.board}" with
		// -DARDUINO_BOARD="\"{build.board}\""
		FileModifiers.replaceInFile(platform.getPlatformFile(), false, "-DARDUINO_BOARD=\"{build.board}\"", //$NON-NLS-1$
				"-DARDUINO_BOARD=\"\\\"{build.board}\\\"\""); //$NON-NLS-1$

		return mstatus;

	}

	static public List<PackageIndex> getPackageIndices() {
		if (packageIndices == null) {
			loadJsons(false);
		}
		return packageIndices;
	}

	static public Board getBoard(String boardName, String platformName, String packageName, boolean mustBeInstalled) {
		for (PackageIndex index : getPackageIndices()) {
			Package pkg = index.getPackage(packageName);
			if (pkg != null) {
				ArduinoPlatform platform = pkg.getLatestPlatform(platformName, mustBeInstalled);
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

	static public List<Board> getBoards() {
		List<Board> boards = new ArrayList<>();
		for (PackageIndex index : getPackageIndices()) {
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
		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {
				platforms.addAll(pkg.getPlatforms());
			}
		}
		return platforms;
	}

	public static IPath getPlatformInstallPath(String vendor, String architecture) {

		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {
				for (ArduinoPlatform curPlatform : pkg.getLatestInstalledPlatforms()) {
					if (architecture.equalsIgnoreCase(curPlatform.getArchitecture())
							&& (vendor.equalsIgnoreCase(pkg.getName()))) {
						return new org.eclipse.core.runtime.Path(curPlatform.getInstallPath().toString());
					}
				}
			}
		}
		return null;
	}

	public static IPath getPlatformInstallPath(String refVendor, String refArchitecture, String refVersion) {
		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {
				if (refVendor.equalsIgnoreCase(pkg.getName())) {
					for (ArduinoPlatform curPlatform : pkg.getInstalledPlatforms()) {
						if (refArchitecture.equalsIgnoreCase(curPlatform.getArchitecture())
								&& refVersion.equalsIgnoreCase(curPlatform.getVersion())) {
							return new org.eclipse.core.runtime.Path(curPlatform.getInstallPath().toString());
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Given a platform.txt file find the platform in the platform manager
	 *
	 * @param platformTxt
	 * @return the found platform otherwise null
	 */
	public static ArduinoPlatform getPlatform(File platformTxt) {
		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {
				for (ArduinoPlatform curPlatform : pkg.getPlatforms()) {
					if (curPlatform.getPlatformFile().equals(platformTxt)) {
						return curPlatform;
					}
				}
			}
		}
		return null;
	}

	static public List<ArduinoPlatform> getLatestInstalledPlatforms() {
		List<ArduinoPlatform> platforms = new ArrayList<>();
		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {

				platforms.addAll(pkg.getLatestInstalledPlatforms());

			}
		}
		return platforms;
	}

	static public List<ArduinoPlatform> getInstalledPlatforms() {
		List<ArduinoPlatform> platforms = new ArrayList<>();
		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {

				platforms.addAll(pkg.getInstalledPlatforms());

			}
		}
		return platforms;
	}

	static public List<Board> getInstalledBoards() {
		List<Board> boards = new ArrayList<>();
		for (PackageIndex index : getPackageIndices()) {
			for (Package pkg : index.getPackages()) {
				for (ArduinoPlatform platform : pkg.getLatestInstalledPlatforms()) {
					boards.addAll(platform.getBoards());
				}
			}
		}
		return boards;
	}

	static public List<Package> getPackages() {
		List<Package> packages = new ArrayList<>();
		for (PackageIndex index : getPackageIndices()) {
			packages.addAll(index.getPackages());
		}
		return packages;
	}

	static public Package getPackage(String JasonName, String packageName) {
		for (PackageIndex index : getPackageIndices()) {
			if (index.getJsonFileName().equals(JasonName)) {
				return index.getPackage(packageName);
			}
		}
		return null;
	}

	static public Package getPackage(String packageName) {
		for (PackageIndex index : getPackageIndices()) {
			Package pkg = index.getPackage(packageName);
			if (pkg != null) {
				return pkg;
			}
		}
		return null;
	}

	static public Tool getTool(String packageName, String toolName, String version) {
		for (PackageIndex index : getPackageIndices()) {
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
	 * pInstallPath if pForceDownload is true the file will be downloaded even if
	 * the download file already exists if pForceDownload is false the file will
	 * only be downloaded if the download file does not exists The extraction is
	 * done with processArchive so only files types supported by this method will be
	 * properly extracted
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
	public static IStatus downloadAndInstall(String pURL, String pArchiveFileName, IPath pInstallPath,
			boolean pForceDownload, IProgressMonitor pMonitor) {
		IPath dlDir = ConfigurationPreferences.getInstallationPathDownload();
		IPath archivePath = dlDir.append(pArchiveFileName);
		try {
			URL dl = new URL(pURL);
			dlDir.toFile().mkdir();
			if (!archivePath.toFile().exists() || pForceDownload) {
				pMonitor.subTask("Downloading " + pArchiveFileName + " .."); //$NON-NLS-1$ //$NON-NLS-2$
				myCopy(dl, archivePath.toFile(), true);
			}
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Failed_to_download.replace(FILE, pURL), e);
		}
		return processArchive(pArchiveFileName, pInstallPath, pForceDownload, archivePath.toString(), pMonitor);
	}

	private static IStatus processArchive(String pArchiveFileName, IPath pInstallPath, boolean pForceDownload,
			String pArchiveFullFileName, IProgressMonitor pMonitor) {
		// Create an ArchiveInputStream with the correct archiving algorithm
		String faileToExtractMessage = Messages.Manager_Failed_to_extract.replace(FILE, pArchiveFullFileName);
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

		String pathPrefix = new String(); 

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
						throw new IOException(Messages.Manager_archiver_eror_single_root_folder_required);
					}
					slash++;
					localstripPath--;
				}
				pathPrefix = name.substring(0, slash);
			}

			// Strip the common path prefix when requested
			if (!name.startsWith(pathPrefix)) {
				throw new IOException(Messages.Manager_archive_error_root_folder_name_mismatch.replace(FILE, name).replace(FOLDER, pathPrefix));
				
			}
			name = name.substring(pathPrefix.length());
			if (name.isEmpty()) {
				continue;
			}
			File outputFile = new File(destFolder, name);

			File outputLinkedFile = null;
			if (isLink && linkName != null) {
				if (!linkName.startsWith(pathPrefix)) {
					throw new IOException(Messages.Manager_archive_error_root_folder_name_mismatch.replace(FILE, name).replace(FOLDER, pathPrefix));
				}
				linkName = linkName.substring(pathPrefix.length());
				outputLinkedFile = new File(destFolder, linkName);
			}
			if (isSymLink) {
				// Symbolic links are referenced with relative paths
				outputLinkedFile = new File(linkName);
				if (outputLinkedFile.isAbsolute()) {
					System.err.println(Messages.Manager_archive_error_symbolic_link_to_absolute_path.replace(FILE, outputFile.toString()).replace(FOLDER, outputLinkedFile.toString()));
					System.err.println();
				}
			}

			// Safety check
			if (isDirectory) {
				if (outputFile.isFile() && !overwrite) {
					throw new IOException(
							Messages.Manager_Cant_create_folder_exists.replace(FILE, outputFile.getPath()) );
				}
			} else {
				// - isLink
				// - isSymLink
				// - anything else
				if (outputFile.exists() && !overwrite) {
					throw new IOException(
							Messages.Manager_Cant_extract_file_exist.replace(FILE, outputFile.getPath()));
				}
			}

			// Extract the entry
			if (isDirectory) {
				if (!outputFile.exists() && !outputFile.mkdirs()) {
					throw new IOException(Messages.Manager_Cant_create_folder.replace(FILE, outputFile.getPath()));
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

	private static void symlink(String from, File to) throws IOException, InterruptedException {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			// needs special rights only one board seems to fail due to this
			// Process process = Runtime.getRuntime().exec(new String[] {
			// "mklink", from, to.getAbsolutePath() }, //$NON-NLS-1$
			// null, to.getParentFile());
			// process.waitFor();
		} else {
			Process process = Runtime.getRuntime().exec(new String[] { "ln", "-s", from, to.getAbsolutePath() }, //$NON-NLS-1$ //$NON-NLS-2$
					null, to.getParentFile());
			process.waitFor();
		}

	}

	/*
	 * create a link file at the level of the os
	 * using mklink /H on windows makes that no admin rights are needed
	 */
	@SuppressWarnings("nls")
	private static void link(File actualFile, File linkName) throws IOException, InterruptedException {
		String[] command = new String[] { "ln", actualFile.getAbsolutePath(), linkName.getAbsolutePath() }; 
		if (SystemUtils.IS_OS_WINDOWS) {
			command = new String[] { "cmd","/c","mklink", "/H",linkName.getAbsolutePath(), actualFile.getAbsolutePath() }; 
		}
		Process process = Runtime.getRuntime().exec(command, null, null);
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
					throw new IOException(Messages.Manager_Failed_to_extract.replace(FILE, outputFile.getAbsolutePath()));
				}
				fos.write(buffer, 0, length);
				leftToWrite -= length;
			}
		}
	}

	/**
	 * This method removes the json files from disk and removes memory references to
	 * these files or their content
	 *
	 * @param packageUrlsToRemove
	 */
	public static void removePackageURLs(Set<String> packageUrlsToRemove) {
		// remove the files from memory
		Set<String> activeUrls = new HashSet<>(Arrays.asList(ConfigurationPreferences.getJsonURLList()));

		activeUrls.removeAll(packageUrlsToRemove);

		ConfigurationPreferences.setJsonURLs(activeUrls.toArray((String[])null));

		// remove the files from disk
		for (String curJson : packageUrlsToRemove) {
			File localFile = getLocalFileName(curJson, true);
			if (localFile != null) {
				if (localFile.exists()) {
					localFile.delete();
				}
			}
		}

		// reload the indices (this will remove all potential remaining
		// references
		// existing files do not need to be refreshed as they have been
		// refreshed at startup
		loadJsons(false);

	}

	public static void setReady(boolean b) {
		myIsReady = b;

	}

	public static void onlyKeepLatestPlatforms() {
		List<Package> allPackages = getPackages();
		for (Package curPackage : allPackages) {
			curPackage.onlyKeepLatestPlatforms();
		}
	}

}
