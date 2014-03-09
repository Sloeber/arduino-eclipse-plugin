package it.baeyens.arduino.common;

import it.baeyens.arduino.arduino.Serial;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICElement;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.statushandlers.StatusManager;

public class Common extends ArduinoInstancePreferences {

    private static boolean RXTXLibraryLoaded = false;
    static ISerialUser OtherSerialUser = null; // If someone else uses the
					       // serial port he can register
					       // here so we can
					       // request him to disconnect
					       // when we need the serial port

    /**
     * This method is used to register a serial user. A serial user is alerted when the serial port will be disconnected (for instance for a upload)
     * The serial user is requested to act appropriately Only 1 serial user can be registered at a given time. No check is done.
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
     * This method makes sure that a string can be used as a file or folder name<br/>
     * To do this it replaces all unacceptable characters with underscores.<br/>
     * Currently it replaces (based on http://en.wikipedia.org/wiki/Filename ) / slash used as a path name component separator in Unix-like, Windows,
     * and Amiga systems. (The MS-DOS command.com shell would consume it as a switch character, but Windows itself always accepts it as a
     * separator.[6][vague]) \ backslash Also used as a path name component separator in MS-DOS, OS/2 and Windows (where there are few differences
     * between slash and backslash); allowed in Unix filenames, see Note 1 ? question mark used as a wildcard in Unix, Windows and AmigaOS; marks a
     * single character. Allowed in Unix filenames, see Note 1 % percent used as a wildcard in RT-11; marks a single character. asterisk or star used
     * as a wildcard in Unix, MS-DOS, RT-11, VMS and Windows. Marks any sequence of characters (Unix, Windows, later versions of MS-DOS) or any
     * sequence of characters in either the basename or extension (thus "*.*" in early versions of MS-DOS means "all files". Allowed in Unix
     * filenames, see note 1 : colon used to determine the mount point / drive on Windows; used to determine the virtual device or physical device
     * such as a drive on AmigaOS, RT-11 and VMS; used as a pathname separator in classic Mac OS. Doubled after a name on VMS, indicates the DECnet
     * nodename (equivalent to a NetBIOS (Windows networking) hostname preceded by "\\".) | vertical bar or pipe designates software pipelining in
     * Unix and Windows; allowed in Unix filenames, see Note 1 " quote used to mark beginning and end of filenames containing spaces in Windows, see
     * Note 1 < less than used to redirect input, allowed in Unix filenames, see Note 1 > greater than used to redirect output, allowed in Unix
     * filenames, see Note 1 . period or dot
     * 
     * @param Name
     *            the string that needs to be checked
     * @return a name safe to create files or folders
     */
    public static String MakeNameCompileSafe(String Name) {
	return Name.trim().replace(" ", "_").replace("/", "_").replace("\\", "_").replace("(", "_").replace(")", "_").replace("*", "_")
		.replace("?", "_").replace("%", "_").replace(".", "_").replace(":", "_").replace("|", "_").replace("<", "_").replace(">", "_")
		.replace(",", "_").replace("\"", "_").replace("-", "_");
    }

    /**
     * Gets a persistent project property
     * 
     * @param project
     *            The project for which the property is needed
     * 
     * @param Tag
     *            The tag identifying the property to read
     * @return returns the property when found. When not found returns an empty string
     */
    public static String getPersistentProperty(IProject project, String Tag) {
	try {
	    String sret = project.getPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, Tag));
	    if (sret == null) {
		sret = project.getPersistentProperty(new QualifiedName("", Tag)); // for
										  // downwards
										  // compatibility
		if (sret == null)
		    sret = "";
	    }
	    return sret;
	} catch (CoreException e) {
	    log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + Tag, e));
	    // e.printStackTrace();
	    return "";
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
	    log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to read persistent setting " + Tag, e));
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
     * @return returns the property when found. When not found returns an empty string
     */
    public static void setPersistentProperty(IProject project, String Tag, String Value) {
	try {
	    project.setPersistentProperty(new QualifiedName(CORE_PLUGIN_ID, Tag), Value);
	    project.setPersistentProperty(new QualifiedName("", Tag), Value); // for
									      // downwards
									      // compatibility
	} catch (CoreException e) {
	    IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to write arduino properties", e);
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
     * This method returns the avrdude upload port prefix which is dependent on the platform
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
	Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
	return UploadPortPrefix_WIN;
    }

    /**
     * ToInt converts a string to a integer in a save way
     * 
     * @param Number
     *            is a String that will be converted to an integer. Number can be null or empty and can contain leading and trailing white space
     * @return The integer value represented in the string based on parseInt
     * @see parseInt. After error checking and modifications parseInt is used for the conversion
     **/
    public static int ToInt(String Number) {
	if (Number == null)
	    return 0;
	if (Number.equals(""))
	    return 0;
	return Integer.parseInt(Number.trim());
    }

    // /**
    // * Converts a object to a project if it is related to a project
    // *
    // * @param item
    // * an object that one way or anther refers to a IProject
    // * @return the referred project or an iproject
    // */
    // public static IProject getProject(Object item) {
    // // See if the given is an IProject (directly or via IAdaptable)
    // if (item instanceof IProject) {
    // return (IProject) item;
    // } else if (item instanceof IResource) {
    // return ((IResource) item).getProject();
    // } else if (item instanceof IAdaptable) {
    // IAdaptable adaptable = (IAdaptable) item;
    // IProject project = (IProject) adaptable.getAdapter(IProject.class);
    // if (project != null) {
    // return project;
    // }
    // // Try ICProject -> IProject
    // ICProject cproject = (ICProject) adaptable.getAdapter(ICProject.class);
    // if (cproject == null) {
    // // Try ICElement -> ICProject -> IProject
    // ICElement celement = (ICElement) adaptable.getAdapter(ICElement.class);
    // if (celement != null) {
    // cproject = celement.getCProject();
    // }
    // }
    // if (cproject != null) {
    // return cproject.getProject();
    // }
    // }
    // return null;
    // }

    private static ICConfigurationDescription[] getCfgs(IProject prj) {
	ICProjectDescription prjd = CoreModel.getDefault().getProjectDescription(prj, false);
	if (prjd != null) {
	    ICConfigurationDescription[] cfgs = prjd.getConfigurations();
	    if (cfgs != null) {
		return cfgs;
	    }
	}

	return new ICConfigurationDescription[0];
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
     * Class used to efficiently special case the scenario where there's only a single project in the workspace. See bug 375760
     */
    private static class ImaginarySelection implements ISelection {
	private IProject fProject;

	ImaginarySelection(IProject project) {
	    fProject = project;
	}

	@Override
	public boolean isEmpty() {
	    return fProject == null;
	}

	IProject getProject() {
	    return fProject;
	}
    }

    static HashSet<IProject> fProjects = new HashSet<IProject>();

    static public IProject[] getSelectedProjects() {
	fProjects.clear();
	getSelectedProjects(getActiveWorkbenchWindow().getSelectionService().getSelection());
	return fProjects.toArray(new IProject[fProjects.size()]);
    }

    static private void getSelectedProjects(ISelection selection) {

	boolean badObject = false;

	if (selection != null) {
	    if (selection instanceof IStructuredSelection) {
		if (selection.isEmpty()) {
		    // could be a form editor or something. try to get the
		    // project from the active part
		    IWorkbenchPage page = getActivePage();
		    if (page != null) {
			IWorkbenchPart part = page.getActivePart();
			if (part != null) {
			    Object o = part.getAdapter(IResource.class);
			    if (o != null && o instanceof IResource) {
				fProjects.add(((IResource) o).getProject());
			    }
			}
		    }
		}
		Iterator<?> iter = ((IStructuredSelection) selection).iterator();
		while (iter.hasNext()) {
		    Object selItem = iter.next();
		    IProject project = null;
		    if (selItem instanceof ICElement) {
			ICProject cproject = ((ICElement) selItem).getCProject();
			if (cproject != null)
			    project = cproject.getProject();
		    } else if (selItem instanceof IResource) {
			project = ((IResource) selItem).getProject();
			// } else if (selItem instanceof IncludeRefContainer) {
			// ICProject fCProject =
			// ((IncludeRefContainer)selItem).getCProject();
			// if (fCProject != null)
			// project = fCProject.getProject();
			// } else if (selItem instanceof IncludeReferenceProxy)
			// {
			// IncludeRefContainer irc =
			// ((IncludeReferenceProxy)selItem).getIncludeRefContainer();
			// if (irc != null) {
			// ICProject fCProject = irc.getCProject();
			// if (fCProject != null)
			// project = fCProject.getProject();
			// }
		    } else if (selItem instanceof IAdaptable) {
			Object adapter = ((IAdaptable) selItem).getAdapter(IProject.class);
			if (adapter != null && adapter instanceof IProject) {
			    project = (IProject) adapter;
			}
		    }
		    // Check whether the project is CDT project
		    if (project != null) {
			if (!CoreModel.getDefault().isNewStyleProject(project))
			    project = null;
			else {
			    ICConfigurationDescription[] tmp = getCfgs(project);
			    if (tmp.length == 0)
				project = null;
			}
		    }
		    if (project != null) {
			fProjects.add(project);
		    } else {
			badObject = true;
			break;
		    }
		}
	    } else if (selection instanceof ITextSelection) {
		// If a text selection check the selected part to see if we can
		// find
		// an editor part that we can adapt to a resource and then
		// back to a project.
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
		    IWorkbenchPage page = window.getActivePage();
		    if (page != null) {
			IWorkbenchPart part = page.getActivePart();
			if (part instanceof IEditorPart) {
			    IEditorPart epart = (IEditorPart) part;
			    IResource resource = (IResource) epart.getEditorInput().getAdapter(IResource.class);
			    if (resource != null) {
				IProject project = resource.getProject();
				badObject = !(project != null && CoreModel.getDefault().isNewStyleProject(project));

				if (!badObject) {
				    fProjects.add(project);
				}
			    }
			}
			// if (part instanceof IConsoleView) {
			// IConsoleView epart = (IConsoleView) part;
			// IProject project = epart.
			//
			// }
		    }
		}

	    } else if (selection instanceof ImaginarySelection) {
		fProjects.add(((ImaginarySelection) selection).getProject());
	    }
	}

	if (!badObject && !fProjects.isEmpty()) {
	    Iterator<IProject> iter = fProjects.iterator();
	    ICConfigurationDescription[] firstConfigs = getCfgs(iter.next());
	    if (firstConfigs != null) {
		for (ICConfigurationDescription firstConfig : firstConfigs) {
		    boolean common = true;
		    Iterator<IProject> iter2 = fProjects.iterator();
		    while (iter2.hasNext()) {
			ICConfigurationDescription[] currentConfigs = getCfgs(iter2.next());
			int j = 0;
			for (; j < currentConfigs.length; j++) {
			    if (firstConfig.getName().equals(currentConfigs[j].getName()))
				break;
			}
			if (j == currentConfigs.length) {
			    common = false;
			    break;
			}
		    }
		    if (common) {
			break;
		    }
		}
	    }
	}
	// action.setEnabled(enable);

	// Bug 375760
	// If focus is on a view that doesn't provide a resource/project
	// context. Use the selection in a
	// project/resource view. We support three views. If more than one is
	// open, nevermind. If there's only
	// one project in the workspace and it's a CDT one, use it
	// unconditionally.
	//
	// Note that whatever project we get here is just a candidate; it's
	// tested for suitability when we
	// call ourselves recursively
	//
	if (badObject || fProjects.isEmpty()) {
	    // Check for lone CDT project in workspace
	    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	    if (projects != null && projects.length == 1) {
		IProject project = projects[0];
		if (CoreModel.getDefault().isNewStyleProject(project) && (getCfgs(project).length > 0)) {
		    getSelectedProjects(new ImaginarySelection(project));
		    return;
		}
	    }

	    // Check the three supported views
	    IWorkbenchPage page = getActivePage();
	    int viewCount = 0;
	    if (page != null) {
		IViewReference theViewRef = null;
		IViewReference viewRef = null;

		theViewRef = page.findViewReference("org.eclipse.cdt.ui.CView"); //$NON-NLS-1$
		viewCount += (theViewRef != null) ? 1 : 0;

		viewRef = page.findViewReference("org.eclipse.ui.navigator.ProjectExplorer"); //$NON-NLS-1$
		viewCount += (viewRef != null) ? 1 : 0;
		theViewRef = (theViewRef == null) ? viewRef : theViewRef;

		viewRef = page.findViewReference("org.eclipse.ui.views.ResourceNavigator"); //$NON-NLS-1$
		viewCount += (viewRef != null) ? 1 : 0;
		theViewRef = (theViewRef == null) ? viewRef : theViewRef;

		if (theViewRef != null && viewCount >= 1) {
		    IViewPart view = theViewRef.getView(false);
		    if (view != null) {
			ISelection cdtSelection = view.getSite().getSelectionProvider().getSelection();
			if (cdtSelection != null) {
			    if (!cdtSelection.isEmpty()) {
				if (!cdtSelection.equals(selection)) { // avoids
								       // infinite
								       // recursion
				    getSelectedProjects(cdtSelection);
				    return;
				}
			    }
			}
		    }
		}
	    }
	}
    }

    public static boolean RXTXDisabled() {
	return getGlobalBoolean(KEY_RXTXDISABLED);
    }

    public static boolean StopSerialMonitor(String mComPort) {
	if (RXTXDisabled())
	    return false;
	if (OtherSerialUser != null) {
	    return OtherSerialUser.PauzePort(mComPort);
	}
	return false;
    }

    public static void StartSerialMonitor(String mComPort) {
	if (RXTXDisabled())
	    return;
	if (OtherSerialUser != null) {
	    OtherSerialUser.ResumePort(mComPort);
	}

    }

    public static String[] listComPorts() {
	if (RXTXDisabled())
	    return new String[0];
	Vector<String> SerialList = Serial.list();
	String outgoing[] = new String[SerialList.size()];
	SerialList.copyInto(outgoing);
	return outgoing;
    }

    public static boolean LoadRXTX() {
	if (RXTXLibraryLoaded)
	    return true;
	return RXTXLibraryLoaded = LoadRXTX(null);
    }

    public static boolean LoadRXTX(Shell parent) {
	String OsInfo = "\nOs =" + Platform.getOS() + " Os Architecture =" + Platform.getOSArch();
	try {
	    // try to load the arduino DLL
	    System.load(GetSerialFullDllName());
	} catch (Error e1) {
	    try {
		// Log the Stack trace
		e1.printStackTrace();
		// Try to load the dll delivered with the plugin
		System.loadLibrary(GetSerialDllName());
	    } catch (Error e2) {
		String FailMessage = "Failed to load Arduino IDE delivered rxtx library (" + GetSerialFullDllName() + ") " + e1.getMessage();
		FailMessage += "\nFailed to load Eclipse plugin delivered rxtx library (" + GetSerialDllName() + ") " + e2.getMessage();
		FailMessage += OsInfo;
		FailMessage += "\nSee Error view for more info.";
		int FailErrorCode = IStatus.WARNING;
		if (parent != null) {
		    MessageBox Failed = new MessageBox(parent, SWT.ICON_ERROR | SWT.OK);
		    Failed.setText("Failed to load rxtx dll!");
		    Failed.setMessage(FailMessage);
		    Failed.open();
		} else {
		    if (!RXTXDisabled()) {
			FailErrorCode = IStatus.ERROR;
		    }
		}
		Common.log(new Status(FailErrorCode, ArduinoConst.CORE_PLUGIN_ID, FailMessage, e2));
		e2.printStackTrace();
		return false;
	    }
	    if (parent != null) {
		MessageBox SuccessBox = new MessageBox(parent, SWT.ICON_WORKING | SWT.OK);
		SuccessBox.setText("Succesfully loaded rxtx dll!");
		SuccessBox.setMessage("Eclipse plugin delivered rxtx library (" + GetSerialDllName() + ") has been loaded successfully" + OsInfo);
		SuccessBox.open();
	    }
	    return true; // Succeeded in loading the eclipse delivered dll
	}
	if (parent != null) {
	    MessageBox SuccessBox = new MessageBox(parent, SWT.ICON_WORKING | SWT.OK);
	    SuccessBox.setText("Succesfully loaded rxtx dll!");
	    SuccessBox.setMessage("Arduino IDE delivered rxtx library (" + GetSerialFullDllName() + ") has been loaded successfully" + OsInfo);
	    SuccessBox.open();
	}
	return true; // Succeeded in loading the Arduino IDE delivered dll
    }

    /**
     * This method reads the full dll name of the rxtxSerial.dll delivered with the Arduino IDE
     * 
     * @return the arduino path
     * @author Jan Baeyens
     */
    private static String GetSerialFullDllName() {
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    return getArduinoPath() + "/rxtxSerial.dll";
	}
	if (Platform.getOS().equals(Platform.OS_LINUX)) {
	    if (Platform.getOSArch().equals(Platform.ARCH_IA64) || Platform.getOSArch().equals(Platform.ARCH_X86_64))
		return getArduinoPath() + "/lib/librxtxSerial64.so";
	    return getArduinoPath() + "/lib/librxtxSerial.so";
	}
	if (Platform.getOS().equals(Platform.OS_MACOSX))
	    return getArduinoPath() + "/contents/Resources/java/LibrtxSerial.jnilib";
	Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system for serial functionality", null));
	return getArduinoPath() + "/rxtxSerial.dll";
    }

    private static String GetSerialDllName() {
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    if (Platform.getOSArch().equals(Platform.ARCH_X86_64)) {
		return "rxtxSerial64";
	    }
	    return "rxtxSerial";
	}
	if (Platform.getOS().equals(Platform.OS_LINUX)) {
	    if (Platform.getOSArch().equals(Platform.ARCH_IA64))
		return "rxtxSerial";
	    if (Platform.getOSArch().equals(Platform.ARCH_X86))
		return "rxtxSerial";
	    if (Platform.getOSArch().equals(Platform.ARCH_X86_64))
		return "rxtxSerialx86_64";
	}
	if (Platform.getOS().equals(Platform.OS_MACOSX))
	    return "rxtxSerial";
	Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system for serial functionality", null));
	return "rxtxSerial";
    }

    public static String[] listBaudRates() {
	String outgoing[] = { "115200", "57600", "38400", "31250", "28800", "19200", "14400", "9600", "4800", "2400", "1200", "300" };
	return outgoing;
    }

    public static Object listLineEndings() {
	String outgoing[] = { "none", "CR", "NL", "NL/CR" };
	return outgoing;
    }

    public static String getLineEnding(int selectionIndex) {
	switch (selectionIndex) {
	default:
	case 0:
	    return "";
	case 1:
	    return "\r";
	case 2:
	    return "\n";
	case 3:
	    return "\r\n";
	}
    }

    /**
     * 
     * Provides the build environment variable based on project and string This method does not add any knowledge.(like adding A.)
     * 
     * @param project
     *            the project that contains the environment variable
     * @param EnvName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return
     */
    static public String getBuildEnvironmentVariable(IProject project, String configName, String EnvName, String defaultvalue) {
	ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project);
	return getBuildEnvironmentVariable(prjDesc.getConfigurationByName(configName), EnvName, defaultvalue);
    }

    /**
     * 
     * Provides the build environment variable based on project and string This method does not add any knowledge.(like adding A.)
     * 
     * @param project
     *            the project that contains the environment variable
     * @param EnvName
     *            the key that describes the variable
     * @param defaultvalue
     *            The return value if the variable is not found.
     * @return
     */
    static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription, String EnvName, String defaultvalue) {

	return getBuildEnvironmentVariable(configurationDescription, EnvName, defaultvalue, true);
    }

    static public String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription, String EnvName, String defaultvalue,
	    boolean expanded) {

	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	try {
	    return envManager.getVariable(EnvName, configurationDescription, expanded).getValue();
	} catch (Exception e) {// ignore all errors and return the default value
	}
	return defaultvalue;
    }

    static public String getArduinoIdeSuffix() {
	if (Platform.getOS().equals(Platform.OS_WIN32))
	    return ArduinoIdeSuffix_WIN;
	if (Platform.getOS().equals(Platform.OS_LINUX))
	    return ArduinoIdeSuffix_LINUX;
	if (Platform.getOS().equals(Platform.OS_MACOSX))
	    return ArduinoIdeSuffix_MAC;
	Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unsupported operating system", null));
	return ArduinoIdeSuffix_WIN;
    }

    /**
     * Arduino has the default libraries in the user home directory in subfolder Arduino/libraries. As the home directory is platform dependent
     * getting the value is resolved by this method
     * 
     * @return the folder where Arduino puts the libraries by default.
     */
    public static String getDefaultPrivateLibraryPath() {
	IPath homPath = new Path(System.getProperty("user.home"));
	return homPath.append("Arduino").append("libraries").toString();
    }

    /**
     * same as getDefaultLibPath but for the hardware folder
     * 
     * @return
     */
    public static String getDefaultPrivateHardwarePath() {
	IPath homPath = new Path(System.getProperty("user.home"));
	return homPath.append("Arduino").append("hardware").toString();
    }

}
