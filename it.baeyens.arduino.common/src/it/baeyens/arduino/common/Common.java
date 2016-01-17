package it.baeyens.arduino.common;

import java.io.File;
import java.util.HashSet;
import java.util.Vector;

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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

import it.baeyens.arduino.arduino.Serial;

public class Common extends ArduinoInstancePreferences {

    static ISerialUser OtherSerialUser = null; // If someone else uses the
					       // serial port he can register
					       // here so we can
					       // request him to disconnect
					       // when we need the serial port

    /**
     * This method is used to register a serial user. A serial user is alerted
     * when the serial port will be disconnected (for instance for a upload) The
     * serial user is requested to act appropriately Only 1 serial user can be
     * registered at a given time. No check is done.
     * 
     * @param SerialUser
     */
    public static void registerSerialUser(ISerialUser SerialUser) {
	OtherSerialUser = SerialUser;
    }

    /**
     * This method is to unregister a serial user.
     */
    public static void UnRegisterSerialUser() {
	OtherSerialUser = null;
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
     * @param Name
     *            the string that needs to be checked
     * @return a name safe to create files or folders
     */
    public static String MakeNameCompileSafe(String Name) {
	char badChars[] = { ' ', '/', '.', SLACH, ':', ' ', UNDERSCORE, BACK_SLACH, '(', ')', '*', '?', '%', '|', '<',
		'>', ',', '-' };

	String ret = Name.trim();
	for (char curchar : badChars) {
	    ret = ret.replace(curchar, UNDERSCORE);
	}
	return ret;
    }

    /**
     * Gets a persistent project property
     * 
     * @param project
     *            The project for which the property is needed
     * 
     * @param Tag
     *            The tag identifying the property to read
     * @return returns the property when found. When not found returns an empty
     *         string
     */
    public static String getPersistentProperty(IProject project, String Tag) {
	try {
	    String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, Tag));
	    if (sret == null) {
		sret = project.getPersistentProperty(new QualifiedName(EMPTY_STRING, Tag)); // for
		// downwards
		// compatibility
		if (sret == null)
		    sret = EMPTY_STRING;
	    }
	    return sret;
	} catch (CoreException e) {
	    log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + Tag, e)); //$NON-NLS-1$
	    // e.printStackTrace();
	    return EMPTY_STRING;
	}
    }

    public static int getPersistentPropertyInt(IProject project, String Tag, int defaultValue) {
	try {
	    String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, Tag));
	    if (sret == null) {
		return defaultValue;
	    }
	    return Integer.parseInt(sret);
	} catch (CoreException e) {
	    log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + Tag, e)); //$NON-NLS-1$
	    // e.printStackTrace();
	    return defaultValue;
	}
    }

    /**
     * Sets a persistent project property
     * 
     * @param project
     *            The project for which the property needs to be set
     * 
     * @param Tag
     *            The tag identifying the property to read
     * @return returns the property when found. When not found returns an empty
     *         string
     */
    public static void setPersistentProperty(IProject project, String Tag, String Value) {
	try {
	    project.setPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, Tag), Value);
	    project.setPersistentProperty(new QualifiedName(EMPTY_STRING, Tag), Value); // for
	    // downwards
	    // compatibility
	} catch (CoreException e) {
	    IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to write arduino properties", e); //$NON-NLS-1$
	    Common.log(status);

	}
    }

    public static void setPersistentProperty(IProject project, String Tag, int Value) {
	setPersistentProperty(project, Tag, Integer.toString(Value));
    }

    /**
     * Logs the status information
     * 
     * @param status
     *            the status information to log
     */
    public static void log(IStatus status) {
	int style = StatusManager.LOG;

	if (status.getSeverity() == IStatus.ERROR) {
	    style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
	    StatusManager stMan = StatusManager.getManager();
	    stMan.handle(status, style);
	} else {
	    Activator.getDefault().getLog().log(status);
	}

    }

    /**
     * This method returns the avrdude upload port prefix which is dependent on
     * the platform
     * 
     * @return avrdude upload port prefix
     */
    public static String UploadPortPrefix() {
	if (Platform.getOS().equals(Platform.OS_WIN32))
	    return UploadPortPrefix_WIN;
	if (Platform.getOS().equals(Platform.OS_LINUX))
	    return UploadPortPrefix_LINUX;
	if (Platform.getOS().equals(Platform.OS_MACOSX))
	    return UploadPortPrefix_MAC;
	Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null)); //$NON-NLS-1$
	return UploadPortPrefix_WIN;
    }

    /**
     * ToInt converts a string to a integer in a save way
     * 
     * @param Number
     *            is a String that will be converted to an integer. Number can
     *            be null or empty and can contain leading and trailing white
     *            space
     * @return The integer value represented in the string based on parseInt
     * @see parseInt. After error checking and modifications parseInt is used
     *      for the conversion
     **/
    public static int ToInt(String Number) {
	if (Number == null)
	    return 0;
	if (Number.isEmpty())
	    return 0;
	return Integer.parseInt(Number.trim());
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

    static HashSet<IProject> fProjects = new HashSet<>();

    public static boolean StopSerialMonitor(String mComPort) {
	if (OtherSerialUser != null) {
	    return OtherSerialUser.PauzePort(mComPort);
	}
	return false;
    }

    public static void StartSerialMonitor(String mComPort) {
	if (OtherSerialUser != null) {
	    OtherSerialUser.ResumePort(mComPort);
	}

    }

    public static String[] listComPorts() {
	Vector<String> SerialList = Serial.list();
	String outgoing[] = new String[SerialList.size()];
	SerialList.copyInto(outgoing);
	return outgoing;
    }

    public static String[] listBaudRates() {
	String outgoing[] = { "115200", "57600", "38400", "31250", "28800", "19200", "14400", "9600", "4800", "2400", //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$//$NON-NLS-5$//$NON-NLS-6$//$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
		"1200", "300" }; //$NON-NLS-1$ //$NON-NLS-2$
	return outgoing;
    }

    public static String[] listLineEndings() {
	String outgoing[] = { "none", "CR", "NL", "CR/NL" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	return outgoing;
    }

    public static String getLineEnding(int selectionIndex) {
	switch (selectionIndex) {
	default:
	case 0:
	    return EMPTY_STRING;
	case 1:
	    return "\r"; //$NON-NLS-1$
	case 2:
	    return "\n"; //$NON-NLS-1$
	case 3:
	    return "\r\n"; //$NON-NLS-1$
	}
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
     * @param EnvName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(IProject project, String configName, String EnvName,
	    String defaultvalue) {
	ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
	return getBuildEnvironmentVariable(prjDesc.getConfigurationByName(configName), EnvName, defaultvalue);
    }

    /**
     * 
     * Provides the build environment variable based on project and string This
     * method does not add any knowledge.(like adding A.)
     * 
     * @param project
     *            the project that contains the environment variable
     * 
     * @param EnvName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(IProject project, String EnvName, String defaultvalue) {
	ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
	return getBuildEnvironmentVariable(prjDesc.getDefaultSettingConfiguration(), EnvName, defaultvalue);
    }

    /**
     * 
     * Provides the build environment variable based on project and string This
     * method does not add any knowledge.(like adding A.)
     * 
     * @param project
     *            the project that contains the environment variable
     * @param EnvName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return The expanded build environment variable
     */
    static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription,
	    String EnvName, String defaultvalue) {

	return getBuildEnvironmentVariable(configurationDescription, EnvName, defaultvalue, true);
    }

    static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription,
	    String EnvName, String defaultvalue, boolean expanded) {

	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	try {
	    return envManager.getVariable(EnvName, configurationDescription, expanded).getValue();
	} catch (Exception e) {// ignore all errors and return the default value
	}
	return defaultvalue;
    }

    /**
     * Arduino has the default libraries in the user home directory in subfolder
     * Arduino/libraries. As the home directory is platform dependent getting
     * the value is resolved by this method
     * 
     * @return the folder where Arduino puts the libraries by default.
     */
    public static String getDefaultPrivateLibraryPath() {
	IPath homPath = new Path(System.getProperty("user.home")); //$NON-NLS-1$
	return homPath.append("Arduino").append(LIBRARY_PATH_SUFFIX).toString(); //$NON-NLS-1$
    }

    public static String getDefaultPrivateHardwarePath() {
	IPath homPath = new Path(System.getProperty("user.home")); //$NON-NLS-1$
	return homPath.append("Arduino").append(ARDUINO_HARDWARE_FOLDER_NAME).toString(); //$NON-NLS-1$ }
    }

    public static File getWorkspaceRoot() {
	IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	File ret = myWorkspaceRoot.getLocation().toFile();
	return ret;
    }

    public static void setBuildEnvironmentVariable(IContributedEnvironment contribEnv,
	    ICConfigurationDescription confdesc, String key, String value) {
	IEnvironmentVariable var = new EnvironmentVariable(key, value);
	contribEnv.addVariable(var, confdesc);

    }

}
