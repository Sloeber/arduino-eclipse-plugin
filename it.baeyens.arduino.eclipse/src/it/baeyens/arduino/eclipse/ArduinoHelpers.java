package it.baeyens.arduino.eclipse;

import it.baeyens.arduino.globals.*;
import it.baeyens.avreclipse.AVRPlugin;
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
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;


public class ArduinoHelpers {

	public static void SetGlobalValue(String key, String Value) {

		IEclipsePreferences myScope =ConfigurationScope.INSTANCE.getNode("it.baeyens.arduino");
		myScope.put(key, Value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String GetGlobal(String key) {
		IEclipsePreferences myScope =ConfigurationScope.INSTANCE.getNode("it.baeyens.arduino");
		return myScope.get(key,null);
	}
	
	

	public static void ChangeProjectReference(IProject project, String OldLibraryProject, IProject NewLibraryProject) {
		try {
			IProjectDescription projectdescription = project.getDescription();
			IProject[] OrgReferencedProjects = projectdescription.getReferencedProjects();

			for (int curProject = 0; curProject < OrgReferencedProjects.length; curProject++) {
				if ((OrgReferencedProjects[curProject] == null) || (OldLibraryProject.equalsIgnoreCase(OrgReferencedProjects[curProject].getName()))) {
					OrgReferencedProjects[curProject] = NewLibraryProject;
					projectdescription.setReferencedProjects(OrgReferencedProjects);
					project.setDescription(projectdescription, 0, null);
					return;
				}
			}
			IProject[] NewReferencedProjects = new IProject[OrgReferencedProjects.length + 1];
			NewReferencedProjects[OrgReferencedProjects.length] = NewLibraryProject;
			projectdescription.setReferencedProjects(NewReferencedProjects);
			project.setDescription(projectdescription, 0, null);
		} catch (CoreException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to change project nature", e);
			AVRPlugin.getDefault().log(status);
			e.printStackTrace();
		}

	}

	public static void AddLibraryDependency(IProject project, IProject libraryProject) throws CoreException {
		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();

		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];

			ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
			ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
			System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
			IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(project.getFullPath(), CIncludePathEntry.VALUE_WORKSPACE_PATH); // (location.toString());
			lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);

		}

		ChangeProjectReference(project, "", libraryProject);
		// IProjectDescription projectdescription =project.getDescription();
		// IProject[] OrgReferencedProjects=
		// projectdescription.getReferencedProjects();
		// IProject[] NewReferencedProjects = new
		// IProject[OrgReferencedProjects.length+1];
		// NewReferencedProjects[OrgReferencedProjects.length]=LiraryProject;
		// projectdescription.setReferencedProjects(NewReferencedProjects);
		// project.setDescription(projectdescription, 0, null);

		projectDescription.setActiveConfiguration(configurationDescription);
		projectDescription.setCdtProjectCreated();
		mngr.setProjectDescription(project, projectDescription, true, null);
	}

	public static void addCodeFolder(IProject project, IPath Path) throws CoreException {

		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();

		// create a link to the path
		String NiceName = Path.lastSegment();
		String PathName = project.getName() + NiceName;
		URI ShortPath = URIUtil.toURI(Path.removeTrailingSeparator().removeLastSegments(1));

		IWorkspace workspace = project.getWorkspace();
		IPathVariableManager pathMan = workspace.getPathVariableManager();

		pathMan.setURIValue(PathName, ShortPath);

		IFolder link = project.getFolder(NiceName);
		IPath location = new Path(PathName + "/" + NiceName);
		link.createLink(location, IResource.NONE, null);
		// Link is now created

		// Use link to add the source
		ICSourceEntry[] OrgSourceEntries = configurationDescription.getSourceEntries();
		ICSourceEntry[] sourceEntries = new CSourceEntry[OrgSourceEntries.length + 1];
		System.arraycopy(OrgSourceEntries, 0, sourceEntries, 0, OrgSourceEntries.length);
		sourceEntries[OrgSourceEntries.length] = new CSourceEntry(link.getFullPath(), null, ICSettingEntry.RESOLVED);
		configurationDescription.setSourceEntries(sourceEntries);
		// source has been added

		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];

			ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
			ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
			System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
			IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(link.getFullPath(), CIncludePathEntry.VALUE_WORKSPACE_PATH); // (location.toString());
			lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
		}

		projectDescription.setActiveConfiguration(configurationDescription);
		projectDescription.setCdtProjectCreated();
		mngr.setProjectDescription(project, projectDescription, true, null);
	}

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

	public static String getMCUProjectName(String MCUName) {
		return ArduinoConst.CoreProjectNamePrefix + MCUName;
	}

	public static void ChangeProjectReference(IProject project, String OldLibraryProjectName, String NewLibraryProjectname, String BoardName) {
		if (OldLibraryProjectName.equals(NewLibraryProjectname))
			return; // nothing to do
		IProject NewLibraryProject = FindProjectByName(NewLibraryProjectname);
		if (NewLibraryProject == null) {
			IProject OldLibraryProject = FindProjectByName(OldLibraryProjectName);
			if (OldLibraryProject == null) {
				return; // This should not happen
			}
			ArduinoProperties Properties = new ArduinoProperties();
			Properties.read(OldLibraryProject);
			Properties.setArduinoBoard(BoardName);
			try {
				NewLibraryProject = createArduino_coreProject(OldLibraryProject.getDescription(), null, Properties);
			} catch (CoreException e) {
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to create arduino core project "+ NewLibraryProjectname, e);
				AVRPlugin.getDefault().log(status);
				e.printStackTrace();
				return;
			}
		}
		ChangeProjectReference(project, OldLibraryProjectName, NewLibraryProject);
	}

	public static IProject FindProjectByName(String Projectname) {
		IProject AllProjects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int CurProject = AllProjects.length - 1; CurProject >= 0; --CurProject) {
			if (AllProjects[CurProject].getName().equals(Projectname))
				return AllProjects[CurProject];
		}
		return null;
	}

	public static void addFileToProject(IContainer container, Path path, InputStream contentStream, IProgressMonitor monitor) throws CoreException {
		final IFile file = container.getFile(path);

		if (file.exists()) {
			file.setContents(contentStream, true, true, monitor);
		} else {
			file.create(contentStream, true, monitor);
		}

	}

	public static IProject createArduino_coreProject(IProjectDescription description, IProgressMonitor monitor, ArduinoProperties Properties)
			throws CoreException {
		// Validate if Arduino_core already exists
		String CoreProjectName = ArduinoHelpers.getMCUProjectName(Properties.getMCUName());
		IProject Arduino_Core_project = FindProjectByName(CoreProjectName);
		if (Arduino_Core_project != null) {
			return Arduino_Core_project; // No need to create as it already
											// exists
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProjectDescription desc = workspace.newProjectDescription(CoreProjectName);
		desc.setLocationURI(null);

		SubProgressMonitor SubMonitor1 = null;
		SubProgressMonitor SubMonitor2 = null;
		if (monitor != null) {
			SubMonitor1 = new SubProgressMonitor(monitor, 1000);
			SubMonitor2 = new SubProgressMonitor(monitor, 1000);
		}

		// Create the Arduino_Core project
		Arduino_Core_project = ResourcesPlugin.getWorkspace().getRoot().getProject(CoreProjectName);
		Arduino_Core_project.create(description, SubMonitor1);
		if (monitor != null) {
			if (monitor.isCanceled()) {
				throw new OperationCanceledException();
			}
		}

		// open the project
		Arduino_Core_project.open(IResource.BACKGROUND_REFRESH, SubMonitor2);

		/* Add the c configuration file */
		addFileToProject((IContainer) Arduino_Core_project, new Path(".cproject"), Stream.openContentStream(CoreProjectName, "templates/cproject.static"),
				monitor);

		// Add the Arduino source code
		addTheNatures(Arduino_Core_project);

		// set the correct mcu and frequency
		Properties.save(Arduino_Core_project); // this sets to much but I assume
												// that is not a problem

		// Add the arduino code
		addCodeFolder(Arduino_Core_project, Properties.getArduinoSourceCodeLocation());

		return Arduino_Core_project;
	}

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

	private static IManagedProject getManagedProject(IProject project) {

		// Get the managed Project
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null) {
			// Project is not a managed build project
			return null;
		}

		IManagedProject p = buildInfo.getManagedProject();
		return p;
	}
	
	public static int ToInt(String Number)
	{
		if (Number== null)	return 0;
		if (Number == "") return 0;
		return Integer.parseInt(Number.trim());
	}
}
