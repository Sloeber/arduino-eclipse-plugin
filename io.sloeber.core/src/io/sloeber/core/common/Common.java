package io.sloeber.core.common;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.statushandlers.StatusManager;

import io.sloeber.core.Activator;

public class Common extends Const {

    private static String eclipseHomeValue = null;
    static {
        try {
            ICdtVariableManager manager = CCorePlugin.getDefault().getCdtVariableManager();
            ICdtVariable var = manager.getVariable(ECLIPSE_HOME, null);
            eclipseHomeValue = var.getStringValue();
        } catch (Exception e) {
            // nobody cares
        }
    }




    public static final boolean isWindows = Platform.getOS().equals(Platform.OS_WIN32);
    public static final boolean isLinux = Platform.getOS().equals(Platform.OS_LINUX);
    public static final boolean isMac = Platform.getOS().equals(Platform.OS_MACOSX);


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
=======
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
	 * @param name the string that needs to be checked
     * @return a name safe to create files or folders
     */
    public static String MakeNameCompileSafe(String name) {
        char[] badChars = { ' ', '/', '.', ':', '\\', '(', ')', '*', '?', '%', '|', '<', '>', ',', '-', '#' };
        String ret = name.trim();
        for (char curchar : badChars) {
            ret = ret.replace(curchar, '_');
        }
        return ret;
    }


    /**
     * Logs the status information
     *
	 * @param status the status information to log
     */
    public static void log(IStatus status) {
        switch (status.getSeverity()) {
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
     * Provides the build environment variable based on project and string This
     * method does not add any knowledge.(like adding A.)
     *
	 * @param project      the project that contains the environment variable
	 * @param configName   the project configuration to use
	 * @param envName      the key that describes the variable
	 * @param defaultvalue The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(IProject project, String configName, String envName,
            String defaultvalue) {
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
        return getBuildEnvironmentVariable(prjDesc.getConfigurationByName(configName), envName, defaultvalue);
    }

    /**
     *
     * Provides the build environment variable based on project and string This
     * method does not add any knowledge.(like adding A.)
     *
	 * @param project      the project that contains the environment variable
     *
	 * @param envName      the key that describes the variable
	 * @param defaultvalue The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(IProject project, String envName, String defaultvalue) {
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
        return getBuildEnvironmentVariable(prjDesc.getDefaultSettingConfiguration(), envName, defaultvalue);
    }

    /**
     *
     * Provides the build environment variable based on project and string This
     * method does not add any knowledge.(like adding A.)
     *
	 * @param project      the project that contains the environment variable
	 * @param envName      the key that describes the variable
	 * @param defaultvalue The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription,
            String envName, String defaultvalue) {

        return getBuildEnvironmentVariable(configurationDescription, envName, defaultvalue, true);
    }

    static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription,
            String envName, String defaultvalue, boolean expanded) {

        IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        try {
            return envManager.getVariable(envName, configurationDescription, expanded).getValue();
        } catch (Exception e) {// ignore all errors and return the default value
        }
        return defaultvalue;
    }

    public static IPath getWorkspaceRoot() {
        IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        return myWorkspaceRoot.getLocation();
    }

    static {

        try {
            ICdtVariableManager manager = CCorePlugin.getDefault().getCdtVariableManager();
            ICdtVariable var = manager.getVariable(ECLIPSE_HOME, null);
            eclipseHomeValue = var.getStringValue();
        } catch (Exception e) {
            // nobody cares
        }

    }

    /**
     * Check whether the string starts with the eclipse path If it does replace with
     * environment variable This keeps things more compatible over environments
     * 
	 * @param path string to check
     * @return modified string or the original
     */
    public static String makePathEnvironmentString(String path) {
        if (eclipseHomeValue == null) {
            return path;
        }
        return path.replace(eclipseHomeValue, makeEnvironmentVar(ECLIPSE_HOME));
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



}
