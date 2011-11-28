package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.tools.ArduinoPreferences;
import it.baeyens.avreclipse.AVRPlugin;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.IBuildObjectProperties;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoHelpers {

	/**
	 * ChangeProjectReference changes the reference from one project to another.
	 * This method is used when you change the MCU of a project. <br/>
	 * Then the reference has to change FI from arduino_core_atmega168 to
	 * arduino_atmega328p<br/>
	 * if project references OldLibraryProject and does not reference
	 * NewLibraryProject a reference to NewLibraryProject will be added<br/>
	 * if project references OldLibraryProject and references NewLibraryProject
	 * a status message will be logged together with a stack trace<br/>
	 * if project does not reference OldLibraryProject and does not reference
	 * NewLibraryProject a reference to NewLibraryProject will be added<br/>
	 * if project does not reference OldLibraryProject and references
	 * NewLibraryProject a a status message will be logged together with a stack
	 * trace<br/>
	 * Note that this action is enough when you change the MCU because all other
	 * references are based on environment parameters
	 * 
	 * @param project
	 *            The project containing a reference to OldLibraryProject an
	 *            needing a reference to NewLibraryProject
	 * @param OldLibraryProject
	 *            the project project referenced to (the reference has to
	 *            disappear)
	 * @param NewLibraryProject
	 *            the project project needs to reference to (the reference has
	 *            to be added)
	 */
	public static void ChangeProjectReference(IProject project,
			String OldLibraryProject, IProject NewLibraryProject) {
		try {
			IProjectDescription projectdescription = project.getDescription();
			IProject[] OrgReferencedProjects = projectdescription
					.getReferencedProjects();

			for (int curProject = 0; curProject < OrgReferencedProjects.length; curProject++) {
				if ((OrgReferencedProjects[curProject] == null)
						|| (OldLibraryProject
								.equalsIgnoreCase(OrgReferencedProjects[curProject]
										.getName()))) {
					OrgReferencedProjects[curProject] = NewLibraryProject;
					projectdescription
							.setReferencedProjects(OrgReferencedProjects);
					project.setDescription(projectdescription, 0, null);
					return;
				}
			}
			IProject[] NewReferencedProjects = new IProject[OrgReferencedProjects.length + 1];
			NewReferencedProjects[OrgReferencedProjects.length] = NewLibraryProject;
			projectdescription.setReferencedProjects(NewReferencedProjects);
			project.setDescription(projectdescription, 0, null);
		} catch (CoreException e) {
			IStatus status = new Status(IStatus.ERROR,ArduinoConst.CORE_PLUGIN_ID,"Failed to change project nature", e);
			AVRPlugin.getDefault().log(status);
			e.printStackTrace();
		}

	}

	/**
	 * ChangeProjectReference changes the reference from one project to another
	 * based on library names. This method gets the projects and if needed
	 * creates the NewLibraryProject Then it calls ChangeProjectReference with
	 * the projects
	 * 
	 * @param project
	 *            The project that needs different project references
	 * @param OldLibraryProjectName
	 *            the name of the project project referenced to (the reference
	 *            has to disappear)
	 * @param NewLibraryProjectname
	 *            the name of the project project needs to reference to (the
	 *            reference has to be added and maybe the project created)
	 * @param BoardName
	 *            The board to use when creating NewLibraryProject
	 * @see {@link #ChangeProjectReference(IProject , String , IProject )}
	 */
	public static void ChangeProjectReference(IProject project,
			String OldLibraryProjectName, String NewLibraryProjectname,
			String BoardName) {
		if (OldLibraryProjectName.equals(NewLibraryProjectname))
			return; // nothing to do
		IProject NewLibraryProject = findProjectByName(NewLibraryProjectname);
		if (NewLibraryProject == null) {
			IProject OldLibraryProject = findProjectByName(OldLibraryProjectName);
			if (OldLibraryProject == null) {
				return; // This should not happen
			}
			ArduinoProperties Properties = new ArduinoProperties();
			Properties.read(OldLibraryProject);
			Properties.setArduinoBoard(BoardName);
			try {
				NewLibraryProject = createArduino_coreProject(
						OldLibraryProject.getDescription(), null, Properties);
			} catch (CoreException e) {
				IStatus status = new Status(IStatus.ERROR,
						ArduinoConst.CORE_PLUGIN_ID,
						"Failed to create arduino core project "
								+ NewLibraryProjectname, e);
				AVRPlugin.getDefault().log(status);
				e.printStackTrace();
				return;
			}
		}
		ChangeProjectReference(project, OldLibraryProjectName,
				NewLibraryProject);
	}

	/**
	 * addLibraryDependency adds a dependency to a project. This includes
	 * following steps <br/>
	 * Creation of a link reference<br/>
	 * Creation of a include reference<br/>
	 * Creation of a code reference<br/>
	 * 
	 * @param project
	 *            the project to add the library dependence to
	 * @param libraryProject
	 *            The library that needs to be added
	 * @throws CoreException
	 */
	public static void addLibraryDependency(IProject project,
			IProject libraryProject) throws CoreException {
		ICProjectDescriptionManager mngr = CoreModel.getDefault()
				.getProjectDescriptionManager();
		ICProjectDescription projectDescription = mngr.getProjectDescription(
				project, true);
		ICConfigurationDescription configurationDescription = projectDescription
				.getDefaultSettingConfiguration();

		addIncludeFolder(configurationDescription, project.getFullPath());

		ChangeProjectReference(project, "", libraryProject);
		projectDescription.setActiveConfiguration(configurationDescription);
		projectDescription.setCdtProjectCreated();
		mngr.setProjectDescription(project, projectDescription, true, null);
	}

	/**
	 * This method adds the provided includepath to all configurations and
	 * languages.
	 * 
	 * @param configurationDescription
	 *            The configuration description of the project to add it to
	 * @param IncludePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	private static void addIncludeFolder(
			ICConfigurationDescription configurationDescription,
			IPath IncludePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription
				.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription
				.getLanguageSettings();

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String TheName=lang.getName();
			if (lang.getName().startsWith("GNU")) {
				ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
				ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
				System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0,OrgIncludeEntries.length);
				IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(IncludePath, ICSettingEntry.VALUE_WORKSPACE_PATH); // (location.toString());
				lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH,	IncludeEntries);
			}
		}

	}

	/**
	 * This method creates a link folder in the project and add the folder as a
	 * source path to the project it also adds the path to the include folder if
	 * the includepath parameter points to a path that contains a subfolder
	 * named "utility" this subfolder will be added to the include path as well <br/>
	 * <br/>
	 * 
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 * 
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks
	 * like a bug to me.<br/>
	 * 
	 * @param project
	 * @param Path
	 * @throws CoreException
	 * 
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, IPath Path)
			throws CoreException {

		ICProjectDescriptionManager mngr = CoreModel.getDefault()
				.getProjectDescriptionManager();
		ICProjectDescription projectDescription = mngr.getProjectDescription(
				project, true);
		ICConfigurationDescription configurationDescription = projectDescription
				.getDefaultSettingConfiguration();

		// create a link to the path
		String NiceName = Path.lastSegment();
		String PathName = project.getName() + NiceName;
		URI ShortPath = URIUtil.toURI(Path.removeTrailingSeparator()
				.removeLastSegments(1));

		IWorkspace workspace = project.getWorkspace();
		IPathVariableManager pathMan = workspace.getPathVariableManager();

		pathMan.setURIValue(PathName, ShortPath);

		IFolder link = project.getFolder(NiceName);
		IPath location = new Path(PathName).append(NiceName);
		link.createLink(location, IResource.NONE, null);
		// Link is now created

		// Use link to add the source
		ICSourceEntry[] OrgSourceEntries = configurationDescription
				.getSourceEntries();
		ICSourceEntry[] sourceEntries = new CSourceEntry[OrgSourceEntries.length + 1];
		System.arraycopy(OrgSourceEntries, 0, sourceEntries, 0,
				OrgSourceEntries.length);
		sourceEntries[OrgSourceEntries.length] = new CSourceEntry(
				link.getFullPath(), null, ICSettingEntry.RESOLVED);
		configurationDescription.setSourceEntries(sourceEntries);
		// source has been added

		addIncludeFolder(configurationDescription, link.getFullPath());
		File file = new File(Path.append("utility").toString());
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath()
					.append("utility"));
		}

		projectDescription.setActiveConfiguration(configurationDescription);
		projectDescription.setCdtProjectCreated();
		mngr.setProjectDescription(project, projectDescription, true, null);
	}

	/**
	 * addTheNatures replaces all existing natures by the natures needed for a
	 * arduino project
	 * 
	 * @param project
	 *            The project where the natures need to be added to
	 * @throws CoreException
	 */
	public static void addTheNatures(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();

		String[] newnatures = new String[5];
		newnatures[0] = ArduinoConst.Cnatureid;
		newnatures[1] = ArduinoConst.CCnatureid;
		newnatures[2] = ArduinoConst.Buildnatureid;
		newnatures[3] = ArduinoConst.Scannernatureid;
		newnatures[4] = ArduinoConst.AVRnatureid;
		description.setNatureIds(newnatures);
		project.setDescription(description, new NullProgressMonitor());
	}

	/**
	 * Converts a MCU Name to a Library project name If MCUName is atmega168
	 * this method returns arduino_atmega168
	 * 
	 * @param MCUName
	 * @return If MCUName is atmega168 this method returns arduino_atmega168
	 */
	public static String getMCUProjectName(String MCUName) {
		return ArduinoConst.CoreProjectNamePrefix + MCUName;
	}

	/**
	 * Given a project name give back the project itself
	 * 
	 * @param Projectname
	 *            The name of the project you want to find
	 * @return The project if found. Else null
	 */
	public static IProject findProjectByName(String Projectname) {
		IProject AllProjects[] = ResourcesPlugin.getWorkspace().getRoot()
				.getProjects();
		for (int CurProject = AllProjects.length - 1; CurProject >= 0; --CurProject) {
			if (AllProjects[CurProject].getName().equals(Projectname))
				return AllProjects[CurProject];
		}
		return null;
	}

	/**
	 * This method add the content of a content stream to a file
	 * 
	 * @param container
	 *            used as a reference to the file
	 * @param path
	 *            The path to the file relative from the container
	 * @param contentStream
	 *            The stream to put in the file
	 * @param monitor
	 *            A monitor to show progress
	 * @throws CoreException
	 */
	public static void addFileToProject(IContainer container, Path path,
			InputStream contentStream, IProgressMonitor monitor)
			throws CoreException {
		final IFile file = container.getFile(path);

		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}

	}

	/**
	 * This method creates a arduino core project
	 * 
	 * @param description
	 *            The project description to start from
	 * @param monitor
	 *            the monitor to show progress
	 * @param Properties
	 *            the properties to use when creating the arduino project
	 * @return A arduino core project
	 * @throws CoreException
	 */
	public static IProject createArduino_coreProject(
			IProjectDescription description, IProgressMonitor monitor,
			ArduinoProperties Properties) throws CoreException {
		// Validate if Arduino_core already exists
		String CoreProjectName = ArduinoHelpers.getMCUProjectName(Properties
				.getMCUName());
		IProject Arduino_Core_project = findProjectByName(CoreProjectName);
		if (Arduino_Core_project != null) {
			return Arduino_Core_project; // No need to create as it already
											// exists
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription desc = workspace
				.newProjectDescription(CoreProjectName);
		desc.setLocationURI(null);

		SubProgressMonitor SubMonitor1 = null;
		SubProgressMonitor SubMonitor2 = null;
		if (monitor != null) {
			SubMonitor1 = new SubProgressMonitor(monitor, 1000);
			SubMonitor2 = new SubProgressMonitor(monitor, 1000);
		}

		// Create the Arduino_Core project
		Arduino_Core_project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(CoreProjectName);
		Arduino_Core_project.create(description, SubMonitor1);
		if (monitor != null) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}

		// open the project
		Arduino_Core_project.open(IResource.BACKGROUND_REFRESH, SubMonitor2);

		/* Add the c configuration file */
		addFileToProject(Arduino_Core_project, new Path(".cproject"),
				Stream.openContentStream(CoreProjectName,
						"templates/cproject.static"), monitor);

		// Add the Arduino source code
		addTheNatures(Arduino_Core_project);

		// set the correct mcu and frequency
		Properties.save(Arduino_Core_project); // this sets to much but I assume
												// that is not a problem

		// Add the arduino code
		addCodeFolder(Arduino_Core_project,
				Properties.getArduinoSourceCodeLocation());

		return Arduino_Core_project;
	}

	/**
	 * Returns whether a project is a static library project or a application
	 * project Copied from AVR eclipse code
	 * 
	 * @param project
	 *            The project to see what it is
	 * @return true if it is a static lib. Otherwise false
	 */
	public static boolean IsStaticLib(IProject project) {

		IManagedProject p = getManagedProject(project);
		if (p != null) {
			IBuildObjectProperties props = p.getBuildProperties();
			IBuildProperty prop = props.getProperty(ArduinoConst.StaticLibTag);

			// may be null
			if (prop != null) {
				IBuildPropertyValue value = prop.getValue();
				if (value.getId().equals(ArduinoConst.StaticLibTag)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Not sure what it does. Is used by the IsStaticLib method
	 * 
	 * @param project
	 * @return
	 */
	private static IManagedProject getManagedProject(IProject project) {
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null) {
			// Project is not a managed build project
			return null;
		}

		IManagedProject p = buildInfo.getManagedProject();
		return p;
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
		if (Number.equals(""))
			return 0;
		return Integer.parseInt(Number.trim());
	}

	/**
	 * GetAvrDudeComPortPrefix is used to determine the prefix to be used with
	 * the com port.<br/>
	 * Avr dude delivered in WinAVR does not need a prefix.<br/>
	 * Avr dude delivered with Arduino needs a prefix <br/>
	 * This code checks the flag "use ide settings" that can be set in the
	 * preferences and when set this method will return a prefix.<br/>
	 * In all other cases a empty string will be returned.
	 * 
	 * @return The prefix needed for the com port for AvrDude
	 */
	public static String GetAvrDudeComPortPrefix() {
		if (ArduinoPreferences.getUseIDESettings())
			return "";
		return "";
	}

}
