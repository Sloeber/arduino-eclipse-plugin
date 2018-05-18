package io.sloeber.core.common;

import java.io.File;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import io.sloeber.core.Activator;

@SuppressWarnings({"nls","unused"})
public class Common extends Const {
	private static final String ENV_PATTERN = "PATTERN";
	private static final String ENV_PROTOCOL = "PROTOCOL";
	protected static final String ENV_TOOL = "TOOL";

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
	 * @param name
	 *            the string that needs to be checked
	 * @return a name safe to create files or folders
	 */
	public static String MakeNameCompileSafe(String name) {
		char[] badChars = { ' ', '/', '.',':', '\\', '(', ')', '*', '?', '%', '|', '<', '>', ',', '-' };

		String ret = name.trim();
		for (char curchar : badChars) {
			ret = ret.replace(curchar, '_');
		}
		return ret;
	}

	/**
	 * Gets a persistent project property
	 *
	 * @param project
	 *            The project for which the property is needed
	 *
	 * @param tag
	 *            The tag identifying the property to read
	 * @return returns the property when found. When not found returns an empty
	 *         string
	 */
	public static String getPersistentProperty(IProject project, String tag) {
		try {
			String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, tag));
			if (sret == null) {
				sret = project.getPersistentProperty(new QualifiedName(new String(), tag)); // for
				// downwards
				// compatibility
				if (sret == null)
					sret = new String();
			}
			return sret;
		} catch (CoreException e) {
			log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to read persistent setting " + tag, e)); //$NON-NLS-1$
			return new String();
		}
	}

	public static int getPersistentPropertyInt(IProject project, String tag, int defaultValue) {
		try {
			String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, tag));
			if (sret == null) {
				return defaultValue;
			}
			return Integer.parseInt(sret);
		} catch (CoreException e) {
			log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to read persistent setting " + tag, e)); //$NON-NLS-1$
			return defaultValue;
		}
	}

	/**
	 * Sets a persistent project property
	 *
	 * @param project
	 *            The project for which the property needs to be set
	 *
	 * @param tag
	 *            The tag identifying the property to read
	 * @return returns the property when found. When not found returns an empty
	 *         string
	 */
	public static void setPersistentProperty(IProject project, String tag, String value) {
		try {
			project.setPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, tag), value);
			project.setPersistentProperty(new QualifiedName(new String(), tag), value); // for
			// downwards
			// compatibility
		} catch (CoreException e) {
			IStatus status = new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Failed to write arduino properties", e); //$NON-NLS-1$
			Common.log(status);

		}
	}

	public static void setPersistentProperty(IProject project, String tag, int value) {
		setPersistentProperty(project, tag, Integer.toString(value));
	}

	/**
	 * Logs the status information
	 *
	 * @param status
	 *            the status information to log
	 */
	public static void log(IStatus status) {
		if (status.getSeverity() == IStatus.ERROR) {
			int style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
			StatusManager stMan = StatusManager.getManager();
			stMan.handle(status, style);
		} else {
			Activator.getDefault().getLog().log(status);
		}
	}

	/**
	 * ToInt converts a string to a integer in a save way
	 *
	 * @param number
	 *            is a String that will be converted to an integer. Number can
	 *            be null or empty and can contain leading and trailing white
	 *            space
	 * @return The integer value represented in the string based on parseInt
	 * @see parseInt. After error checking and modifications parseInt is used
	 *      for the conversion
	 **/
	public static int ToInt(String number) {
		if (number == null)
			return 0;
		if (number.isEmpty())
			return 0;
		return Integer.parseInt(number.trim());
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getActivePage();
		}
		return null;
	}

	/**
	 *
	 * Provides the build environment variable based on project and string This
	 * method does not add any knowledge.(like adding A.)
	 *
	 * @param project
	 *            the project that contains the environment variable
	 * @param configName
	 *            the project configuration to use
	 * @param envName
	 *            the key that describes the variable
	 * @param defaultvalue
	 *            The return value if the variable is not found.
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
		return getBuildEnvironmentVariable(prjDesc.getDefaultSettingConfiguration(), envName, defaultvalue);
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

	public static File getWorkspaceRoot() {
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		return myWorkspaceRoot.getLocation().toFile();
	}

	public static String makePathEnvironmentString(String path) {
		String actualStoredValue = path;
		String searchString = ConfigurationPreferences.getEclipseHome().toString();
		if (path.startsWith(searchString)) {
			actualStoredValue = "${eclipse_home}" + path.substring(searchString.length()); //$NON-NLS-1$
		}
		return actualStoredValue;
	}

	public static void setBuildEnvironmentVariable(IContributedEnvironment contribEnv,
			ICConfigurationDescription confdesc, String key, String value) {

		IEnvironmentVariable var = new EnvironmentVariable(key, makePathEnvironmentString(value));
		contribEnv.addVariable(var, confdesc);
	}

	static public void setBuildEnvironmentVariable(IProject project, String envName, String value) {
		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
		setBuildEnvironmentVariable(prjDesc.getDefaultSettingConfiguration(), envName, value);
	}

	public static void setBuildEnvironmentVariable(ICConfigurationDescription confdesc, String key, String value) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		setBuildEnvironmentVariable(contribEnv, confdesc, key, value);
	}

	/**
	 * given a action return the environment key that matches it's protocol
	 *
	 * @param action
	 * @return the environment variable key to find the protocol
	 */
	public static String get_ENV_KEY_PROTOCOL(String action) {
		return ERASE_START + action.toUpperCase() + DOT + ENV_PROTOCOL;
	}

	/**
	 * given a action return the environment key that matches it's tool
	 *
	 * @param action
	 * @return the environment variable key to find the tool
	 */
	public static String get_ENV_KEY_TOOL(String action) {
		return ERASE_START + action.toUpperCase() + DOT + ENV_TOOL;
	}

	/**
	 * given a action return the environment key that matches it's recipe
	 *
	 * @param action
	 * @return he environment variable key to find the recipe
	 */
	public static String get_ENV_KEY_RECIPE(String action) {
		return ERASE_START + action.toUpperCase() + DOT + ENV_PATTERN;
	}

	public static String get_Jantje_KEY_PROTOCOL(String action) {
		return ENV_KEY_JANTJE_START + action.toUpperCase() + DOT + ENV_PROTOCOL;
	}

	public static String get_Jantje_KEY_RECIPE(String action) {
		return ENV_KEY_JANTJE_START + action.toUpperCase() + DOT + ENV_PATTERN;
	}

	/**
	 * given a action and a tool return the environment key that matches it's
	 * recipe
	 *
	 * @param action
	 * @return he environment variable key to find the recipe
	 */
	public static String get_ENV_KEY_RECIPE(String tool, String action) {
		return ERASE_START + "TOOLS" + DOT + tool.toUpperCase() + DOT + action.toUpperCase() + DOT + ENV_PATTERN; //$NON-NLS-1$
	}
}
