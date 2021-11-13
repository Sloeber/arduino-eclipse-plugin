package io.sloeber.core.tools;

import static java.nio.file.StandardCopyOption.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Json.ArduinoInstallable;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;

public class PackageManager {
    private static final String FILE = Messages.FILE_TAG;
    private static final String FOLDER = Messages.FOLDER_TAG;
    private final static int MAX_HTTP_REDIRECTIONS = 5;

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
            return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Failed_to_download.replace(FILE, pURL),
                    e);
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

    private static IStatus extract(ArchiveInputStream in, File destFolder, int stripPath, boolean overwrite,
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
            Long modifiedTime = Long.valueOf(entry.getLastModifiedDate().getTime());

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
                mode = Integer.valueOf(tarEntry.getMode());
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
                throw new IOException(Messages.Manager_archive_error_root_folder_name_mismatch.replace(FILE, name)
                        .replace(FOLDER, pathPrefix));

            }
            name = name.substring(pathPrefix.length());
            if (name.isEmpty()) {
                continue;
            }
            File outputFile = new File(destFolder, name);

            File outputLinkedFile = null;
            if (isLink && linkName != null) {
                if (!linkName.startsWith(pathPrefix)) {
                    throw new IOException(Messages.Manager_archive_error_root_folder_name_mismatch.replace(FILE, name)
                            .replace(FOLDER, pathPrefix));
                }
                linkName = linkName.substring(pathPrefix.length());
                outputLinkedFile = new File(destFolder, linkName);
            }
            if (isSymLink) {
                // Symbolic links are referenced with relative paths
                outputLinkedFile = new File(linkName);
                if (outputLinkedFile.isAbsolute()) {
                    System.err.println(Messages.Manager_archive_error_symbolic_link_to_absolute_path
                            .replace(FILE, outputFile.toString()).replace(FOLDER, outputLinkedFile.toString()));
                    System.err.println();
                }
            }

            // Safety check
            if (isDirectory) {
                if (outputFile.isFile() && !overwrite) {
                    throw new IOException(
                            Messages.Manager_Cant_create_folder_exists.replace(FILE, outputFile.getPath()));
                }
            } else {
                // - isLink
                // - isSymLink
                // - anything else
                if (outputFile.exists() && !overwrite) {
                    throw new IOException(Messages.Manager_Cant_extract_file_exist.replace(FILE, outputFile.getPath()));
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
        if (Common.isWindows) {
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
     * create a link file at the level of the os using mklink /H on windows makes
     * that no admin rights are needed
     */
    @SuppressWarnings("nls")
    private static void link(File actualFile, File linkName) throws IOException, InterruptedException {
        String[] command = new String[] { "ln", actualFile.getAbsolutePath(), linkName.getAbsolutePath() };
        if (SystemUtils.IS_OS_WINDOWS) {
            command = new String[] { "cmd", "/c", "mklink", "/H", linkName.getAbsolutePath(),
                    actualFile.getAbsolutePath() };
        }
        Process process = Runtime.getRuntime().exec(command, null, null);
        process.waitFor();
    }

    private static void chmod(File file, int mode) throws IOException, InterruptedException {
        String octal = Integer.toOctalString(mode);
        if (Common.isWindows) {
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
                    throw new IOException(
                            Messages.Manager_Failed_to_extract.replace(FILE, outputFile.getAbsolutePath()));
                }
                fos.write(buffer, 0, length);
                leftToWrite -= length;
            }
        }
    }

    /**
     * copy a url locally taking into account redirections
     *
     * @param url
     * @param localFile
     * @throws IOException
     */
    protected static void myCopy(URL url, File localFile, boolean report_error) throws IOException {
        myCopy(url, localFile, report_error, 0);
    }

    @SuppressWarnings("nls")
    private static void myCopy(URL url, File localFile, boolean report_error, int redirectionCounter)
            throws IOException {
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
                try (InputStream stream = url.openStream()) {
                    Files.copy(stream, localFile.toPath(), REPLACE_EXISTING);
                }
                return;
            }

            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                if (redirectionCounter >= MAX_HTTP_REDIRECTIONS) {
                    throw new IOException("Too many redirections while downloading file.");
                }
                myCopy(new URL(conn.getHeaderField("Location")), localFile, report_error, redirectionCounter + 1);
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
    public static void mySafeCopy(URL url, File localFile, boolean report_error) throws IOException {
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

    /**
     * Given a platform description in a json file download and install all needed
     * stuff. All stuff is including all tools and core files and hardware specific
     * libraries. That is (on windows) inclusive the make.exe
     *
     * @param installable
     * @param monitor
     * @param object
     * @return
     */
    static public synchronized IStatus downloadAndInstall(ArduinoInstallable installable, boolean forceDownload,
            IProgressMonitor monitor) {

        return downloadAndInstall(installable.getUrl(), installable.getArchiveFileName(), installable.getInstallPath(),
                forceDownload, monitor);

    }

}
