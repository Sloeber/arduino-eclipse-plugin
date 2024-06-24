package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.core.AutoBuildCommon;
import io.sloeber.core.Activator;

public class Common {

    public final static String sloeberHome = getSloeberHome();
    public final static IPath sloeberHomePath = new org.eclipse.core.runtime.Path(sloeberHome);
    public final static String sloeberHomePathToString = sloeberHomePath.toString();

    private static String getSloeberHome() {

        try {
            String sloeber_HomeValue = System.getenv(SLOEBER_HOME);
            if (sloeber_HomeValue != null) {
                if (!sloeber_HomeValue.isEmpty()) {
                    return sloeber_HomeValue;
                }
            }
            // no sloeber home provided
            // use eclipse home as sloeber home
            URL resolvedUrl = Platform.getInstallLocation().getURL();
            URI resolvedUri = new URI(resolvedUrl.getProtocol(), resolvedUrl.getPath(), null);
            return Paths.get(resolvedUri).toString();
        } catch (URISyntaxException e) {
            // this should not happen
            // but it seems a space in the path makes it happen
            Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                    "Eclipse fails to provide its own installation folder :-(. \nThis is known to happen when you have a space ! # or other wierd characters in your eclipse installation path", //$NON-NLS-1$
                    e));
        }
        return null;
    }


    /**
     * This method makes sure that a string can be used as a file or folder name
     * <br/>
     * To do this it replaces all unacceptable characters with underscores.<br/>
     * Currently it replaces (based on http://en.wikipedia.org/wiki/Filename ) /
     * slash used as a path name component separator in Unix-like, Windows, and
     * Amiga systems. (The MS-DOS command.com shell would consume it as a switch
     * character, but Windows itself always accepts it as a
     * separator.[6][vague]) \ backslash Also used as a path name component
     * separator in MS-DOS, OS/2 and Windows (where there are few differences
     * between slash and backslash); allowed in Unix filenames, see Note 1 ?
     * question mark used as a wildcard in Unix, Windows and AmigaOS; marks a
     * single character. Allowed in Unix filenames, see Note 1 % percent used as
     * a wildcard in RT-11; marks a single character. asterisk or star used as a
     * wildcard in Unix, MS-DOS, RT-11, VMS and Windows. Marks any sequence of
     * characters (Unix, Windows, later versions of MS-DOS) or any sequence of
     * characters in either the basename or extension (thus "*.*" in early
     * versions of MS-DOS means "all files". Allowed in Unix filenames, see note
     * 1 : colon used to determine the mount point / drive on Windows; used to
     * determine the virtual device or physical device such as a drive on
     * AmigaOS, RT-11 and VMS; used as a pathname separator in classic Mac OS.
     * Doubled after a name on VMS, indicates the DECnet nodename (equivalent to
     * a NetBIOS (Windows networking) hostname preceded by "\\".) | vertical bar
     * or pipe designates software pipelining in Unix and Windows; allowed in
     * Unix filenames, see Note 1 " quote used to mark beginning and end of
     * filenames containing spaces in Windows, see Note 1 < less than used to
     * redirect input, allowed in Unix filenames, see Note 1 > greater than used
     * to redirect output, allowed in Unix filenames, see Note 1 . period or dot
     *
     * # is excluded as it is seen as a special character by make
     * =======
     * character, but Windows itself always accepts it as a separator.[6][vague]) \
     * backslash Also used as a path name component separator in MS-DOS, OS/2 and
     * Windows (where there are few differences between slash and backslash);
     * allowed in Unix filenames, see Note 1 ? question mark used as a wildcard in
     * Unix, Windows and AmigaOS; marks a single character. Allowed in Unix
     * filenames, see Note 1 % percent used as a wildcard in RT-11; marks a single
     * character. asterisk or star used as a wildcard in Unix, MS-DOS, RT-11, VMS
     * and Windows. Marks any sequence of characters (Unix, Windows, later versions
     * of MS-DOS) or any sequence of characters in either the basename or extension
     * (thus "*.*" in early versions of MS-DOS means "all files". Allowed in Unix
     * filenames, see note 1 : colon used to determine the mount point / drive on
     * Windows; used to determine the virtual device or physical device such as a
     * drive on AmigaOS, RT-11 and VMS; used as a pathname separator in classic Mac
     * OS. Doubled after a name on VMS, indicates the DECnet nodename (equivalent to
     * a NetBIOS (Windows networking) hostname preceded by "\\".) | vertical bar or
     * pipe designates software pipelining in Unix and Windows; allowed in Unix
     * filenames, see Note 1 " quote used to mark beginning and end of filenames
     * containing spaces in Windows, see Note 1 < less than used to redirect input,
     * allowed in Unix filenames, see Note 1 > greater than used to redirect output,
     * allowed in Unix filenames, see Note 1 . period or dot
     *
     * @param name
     *            the string that needs to be checked
     * @return a name safe to create files or folders
     */
    public static String makeNameCompileSafe(String name) {
        char[] badChars = { ' ', '/', '.', ':', '\\', '(', ')', '*', '?', '%', '|', '<', '>', ',', '-', '#' };
        String ret = name.trim();
        for (char curchar : badChars) {
            ret = ret.replace(curchar, '_');
        }
        return ret;
    }

    /**
     * Logs the status information if status is OK then nothing happens
     *
     * @param status
     *            the status information to log
     */
    public static void log(IStatus status) {
        switch (status.getSeverity()) {
        case IStatus.OK: {
            break;
        }
        case IStatus.ERROR: {
            int style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
            StatusManager stMan = StatusManager.getManager();
            stMan.handle(status, style);
            break;
        }
        case SLOEBER_STATUS_DEBUG:
            // break;//remove break to add debugging
        default:
            Activator.getDefault().getLog().log(status);
        }
    }

    /**
     *
     * Provides the build environment variable based on project default
     * configuration and string This
     * method does not add any knowledge.(like adding A.)
     *
     * @param project
     *            the project that contains the environment variable
     *
     * @param envName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(IProject project, String envName, String defaultvalue) {
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
        ISloeberConfiguration SloeberConf = null;
        if (prjDesc != null) {
            ICConfigurationDescription cConf = prjDesc.getActiveConfiguration();
            if (cConf != null) {
                SloeberConf = ISloeberConfiguration.getConfig(cConf);

            }
        }
        return getBuildEnvironmentVariable(SloeberConf, envName, defaultvalue, true);
    }

    /**
     *
     * Provides the build environment variable based on project and string This
     * method does not add any knowledge.(like adding A.)
     *
     * @param project
     *            the project that contains the environment variable
     * @param envName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(ISloeberConfiguration sloeberConf, String envName,
            String defaultvalue) {
        return getBuildEnvironmentVariable(sloeberConf, envName, defaultvalue, true);
    }

    static public String getBuildEnvironmentVariable(ISloeberConfiguration sloeberConf, String envName,
            String defaultvalue, boolean expanded) {
        if (sloeberConf != null) {
        	IAutoBuildConfigurationDescription autoDesc= sloeberConf.getAutoBuildDesc();
        	return AutoBuildCommon.getVariableValue(envName, defaultvalue, expanded, autoDesc);
//            IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
//            try {
//                ICConfigurationDescription configurationDescription = sloeberConf.getAutoBuildDesc()
//                        .getCdtConfigurationDescription();
//                IEnvironmentVariable t= envManager.getVariable(envName, configurationDescription, expanded);
//                if(t!=null) {
//                	return envManager.getVariable(envName, configurationDescription, expanded).getValue();
//                }else {
//                	return envManager.getVariable(envName, configurationDescription, expanded).getValue();
//                }
//            } catch ( Exception e) {
//            	e.printStackTrace();
//            	// ignore all errors and return the default value
//            }
        }
        return defaultvalue;
    }

    public static IPath getWorkspaceRoot() {
        IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        return myWorkspaceRoot.getLocation();
    }

    private final static String SLOEBER_HOME_VAR = makeEnvironmentVar(SLOEBER_HOME);

    /**
     * Check whether the string starts with the SLOEBER_HOME path If it does replace
     * with environment variable This keeps things more compatible over environments
     *
     * @param file
     *            string to check
     * @return modified string or the original
     */
    public static String makePathVersionString(File file) {
    	if(sloeberHomePath.isPrefixOf(IPath.fromFile(file))) {
    		return SLOEBER_HOME_VAR+SLACH+IPath.fromFile(file) .makeRelativeTo(sloeberHomePath).toString();
    	}
        return file.toString();
    }

    /**
     * Support for the environment variable SLOBER_HOME
     *
     * @param file
     * @return
     */
    public static File resolvePathEnvironmentString(File file) {

        String retString = file.getPath();
        if (retString.startsWith(SLOEBER_HOME_VAR)) {
        	retString=retString.replace(SLOEBER_HOME_VAR, EMPTY_STRING);
        	return sloeberHomePath.append(retString).toFile();
        	//.replace(SLOEBER_HOME_VAR, sloeberHomePathToString);
        }
        return new File(retString);
    }

    /**
     * Converts a name to a tagged environment variable if variableName ="this" the
     * output is "${this}"
     *
     * @param variableName
     * @return
     */
    public static String makeEnvironmentVar(String variableName) {
        return "${" + variableName + '}'; //$NON-NLS-1$
    }

    public static void deleteDirectory(org.eclipse.core.runtime.IPath directory) throws IOException {
        deleteDirectory(Path.of(directory.toOSString()));
    }

    public static void deleteDirectory(Path directory) throws IOException {
        Files.walk(directory).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }

    /**
     * this is some code to work around issue
     * https://github.com/eclipse-cdt/cdt/issues/539
     *
     */
    public static String getFileExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');
        return (dotPosition == -1 || dotPosition == fileName.length() - 1) ? "" : fileName.substring(dotPosition + 1); //$NON-NLS-1$
    }

}
