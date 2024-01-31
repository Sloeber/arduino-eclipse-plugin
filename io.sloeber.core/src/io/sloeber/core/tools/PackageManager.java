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
     
            return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Format_not_supported);
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
