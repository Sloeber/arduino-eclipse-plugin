package io.sloeber.core.tools;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.Library;
import io.sloeber.core.managers.Tool;
import io.sloeber.core.managers.ToolDependency;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 *
 * @author Jan Baeyens
 *
 */
public class Helpers extends Common {



    private static final String FILE = Messages.FILE;
	private static final String FOLDER = Messages.FOLDER;

	private static boolean myHasBeenLogged = false;


	/**
	 * This method is the internal working class that adds the provided include path
	 * to all configurations and languages.
	 *
	 * @param configurationDescription The configuration description of the project
	 *                                 to add it to
	 * @param IncludePath              The path to add to the include folders
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
    public static void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath IncludePath,
            boolean isWorkspacePath) {
        ICLanguageSetting[] languageSettings = configurationDescription.getRootFolderDescription()
                .getLanguageSettings();
		int pathSetting = ICSettingEntry.VALUE_WORKSPACE_PATH;
		if (!isWorkspacePath) {
			pathSetting = 0;
		}

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
					System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
					IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(IncludePath, pathSetting);
					lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
				}
			}
		}
	}

	/**
	 * Removes include folders that are not valid. This method does not save the
	 * configurationDescription description
	 *
	 * @param configurationDescription the configuration that is checked
	 * @return true is a include path has been removed. False if the include path
	 *         remains unchanged.
	 */
	public static boolean removeInvalidIncludeFolders(ICConfigurationDescription configurationDescription) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();
		boolean hasChange = false;
		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] OrgIncludeEntriesFull = lang
							.getResolvedSettingEntries(ICSettingEntry.INCLUDE_PATH);
					int copiedEntry = 0;
					for (int curEntry = 0; curEntry < OrgIncludeEntries.length; curEntry++) {
						IPath cusPath = ((CIncludePathEntry) OrgIncludeEntriesFull[curEntry]).getFullPath();
						if ((ResourcesPlugin.getWorkspace().getRoot().exists(cusPath))
								|| (((CIncludePathEntry) OrgIncludeEntries[curEntry]).isBuiltIn())) {
							OrgIncludeEntries[copiedEntry++] = OrgIncludeEntries[curEntry];
						} else {
							Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
									"Removed invalid include path" + cusPath, null)); //$NON-NLS-1$
						}
					}
					if (copiedEntry != OrgIncludeEntries.length) // do not save
					// if nothing
					// has changed
					{
						ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[copiedEntry];
						System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, copiedEntry);
						lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
						hasChange = true;

					}
				}
			}
		}
		return hasChange;
	}

	/**
	 * Creates a folder and links the folder to an existing folder Parent folders of
	 * the target folder are created if needed. In case this method fails an error
	 * is logged.
	 *
	 * @param project the project the newly created folder will belong to
	 * @param target  the folder name relative to the project
	 * @param source  the fully qualified name of the folder to link to
	 */
	public static void LinkFolderToFolder(IProject project, IPath source, IPath target) {

		// create target parent folder and grandparents
		IPath ParentFolders = new Path(target.toString()).removeLastSegments(1);
		for (int curfolder = ParentFolders.segmentCount() - 1; curfolder >= 0; curfolder--) {
			try {
				createNewFolder(project, ParentFolders.removeLastSegments(curfolder).toString(), null);
			} catch (@SuppressWarnings("unused") CoreException e) {// ignore this error as the parent
				// folders may have been created yet
			}
		}

		// create the actual link
		try {
			createNewFolder(project, target.toString(), source);
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_Create_folder_failed.replace(FOLDER, target.toString()), e));
		}
	}

	/**
	 * This method creates a link folder in the project and add the folder as a
	 * source path to the project it also adds the path to the include folder if the
	 * include path parameter points to a path that contains a subfolder named
	 * "utility" this subfolder will be added to the include path as well <br/>
	 * Forget about this. Arduino made this all so complicated I don't know anymore
	 * what needs to be added to what<br/>
	 * <br/>
	 *
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 *
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks like a
	 * bug to me.<br/>
	 *
	 * @param project
	 * @param Path
	 * @throws CoreException
	 *
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, IPath toLinkFolder, String LinkName,
			ICConfigurationDescription configurationDescription, boolean forceRoot) throws CoreException {
		IFolder link = project.getFolder(LinkName);		

		LinkFolderToFolder(project, toLinkFolder, new Path(LinkName));

		// Now the folder has been created we need to make sure the special
		// folders are added to the path

		String possibleIncludeFolder = "utility"; //$NON-NLS-1$
		File file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
            addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder), true);
		}

		if (forceRoot) {
            addIncludeFolder(configurationDescription, link.getFullPath(), true);
		} else {
			// add src or root give priority to src
			possibleIncludeFolder = Library.LIBRARY_SOURCE_FODER;
			file = toLinkFolder.append(possibleIncludeFolder).toFile();
			if (file.exists()) {
                addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder), true);
			} else {
                addIncludeFolder(configurationDescription, link.getFullPath(), true);
			}
		}
        // TOFIX removed this code as part of libraries not included in project after
        // creation,
        // Should run a lib test to see how this works without this code
        // if this is needed I should create a include with a environment var so I do
        // not need to get the boardDescriptor

        // possibleIncludeFolder = "arch"; //$NON-NLS-1$
        // file = toLinkFolder.append(possibleIncludeFolder).toFile();
        // if (file.exists()) {
        // InternalBoardDescriptor boardDescriptor = new
        // InternalBoardDescriptor(configurationDescription);
        // addIncludeFolder(rootFolderDescr,
        // link.getFullPath().append(possibleIncludeFolder).append(boardDescriptor.getArchitecture()));
        // }
	}

	public static void removeCodeFolder(IProject project, String LinkName) throws CoreException {
		IFolder link = project.getFolder(LinkName);
		if (link.exists()) {
			link.delete(true, null);
		}
	}

	/**
     * This method creates a link folder in the project descriptor and adds the
     * folder as a source path to the projectdescriptor it also adds the path to the
     * include folder if the include path parameter points to a path that contains a
     * subfolder named "utility" this subfolder will be added to the include path as
     * well <br/>
     * <br/>
     *
     * note Arduino has these subfolders in the libraries that need to be
     * include.<br/>
     * <br/>
     *
     * note that in the current eclipse version, there is no need to add the
     * subfolder as a code folder. This may change in the future as it looks like a
     * bug to me.<br/>
     *
     * @param project
     * @param Path
     * @throws CoreException
     *
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
     */
	public static void addCodeFolder(IProject project, Path Path, ICConfigurationDescription configurationDescription,
			boolean forceRoot) throws CoreException {
            String NiceName = Path.lastSegment();
		addCodeFolder(project, Path, NiceName, configurationDescription, forceRoot);
	}



	/**
	 * This method adds the content of a content stream to a file If the file
	 * already exist the file remains untouched
	 *
	 * @param container     used as a reference to the file
	 * @param path          The path to the file relative from the container
	 * @param contentStream The stream to put in the file
	 * @param monitor       A monitor to show progress
	 * @throws CoreException
	 */
	public static IFile addFileToProject(IContainer container, Path path, InputStream contentStream,
			IProgressMonitor monitor, boolean overwrite) throws CoreException {
		IFile file = container.getFile(path);
		file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		if (overwrite && file.exists()) {
			file.delete(true, null);
			file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}

		if (!file.exists() && (contentStream != null)) {
			file.create(contentStream, true, monitor);
		}
		return file;
	}

	public static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	/**
	 * This method adds the Arduino code in a subfolder named Arduino. 2 linked
	 * subfolders named core and variant link to the real Arduino code note
	 *
	 * @param project                  The project to add the arduino code to
	 * @param configurationDescription The configuration description that will
	 *                                 contain the change
	 * @throws CoreException
	 */
	public static void addArduinoCodeToProject(BoardDescription boardDescriptor, IProject project,
			ICConfigurationDescription configurationDescription) throws CoreException {

		IPath corePath = boardDescriptor.getActualCoreCodePath();
		if (corePath != null) {
			addCodeFolder(project, corePath, ARDUINO_CODE_FOLDER_PATH, configurationDescription, true);
			IPath variantPath = boardDescriptor.getActualVariantPath();
			if (variantPath == null) {
				// remove the existing link
				Helpers.removeCodeFolder(project, ARDUINO_VARIANT_FOLDER_PATH);
			} else {
				IPath redirectVariantPath = boardDescriptor.getActualVariantPath();
				Helpers.addCodeFolder(project, redirectVariantPath, ARDUINO_VARIANT_FOLDER_PATH,
						configurationDescription, false);
			}
		}

	}

	/**
	 * Creates a new folder resource as a link or local
	 *
	 * @param Project       the project the folder is added to
	 * @param newFolderName the new folder to create (can contain subfolders)
	 * @param linklocation  if null a local folder is created using newFolderName if
	 *                      not null a link folder is created with the name
	 *                      newFolderName and pointing to linklocation
	 *
	 * @return nothing
	 * @throws CoreException
	 */
	public static void createNewFolder(IProject Project, String newFolderName, IPath linklocation)
			throws CoreException {
		final IFolder newFolderHandle = Project.getFolder(newFolderName);
		if (linklocation != null) {
			URI relativeLinklocation = Project.getPathVariableManager().convertToRelative(URIUtil.toURI(linklocation),
					false, null);
			newFolderHandle.createLink(relativeLinklocation, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		} else {
			newFolderHandle.create(0, true, null);
		}

	}









	/**
	 * Set the project to force a rebuild. This method is called after the arduino
	 * settings have been updated. Note the only way I found I could get this to
	 * work is by deleting the build folder Still then the "indexer needs to recheck
	 * his includes from the language provider which still is not working
	 *
	 * @param project
	 */
	public static void setDirtyFlag(IProject project, ICConfigurationDescription cfgDescription) {
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null) {
			return; // Project is not a managed build project
		}

		IFolder buildFolder = project.getFolder(cfgDescription.getName());
		if (buildFolder.exists()) {
			try {
				buildFolder.delete(true, null);
			} catch (CoreException e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
						Messages.Helpers_delete_folder_failed.replace(FOLDER, cfgDescription.getName()), e));
			}
		}
	}

	/**
	 * Given a source file calculates the base of the output file. this method may
	 * not be needed if I can used the eclipse default behavior. However the eclipse
	 * default behavior is different from the arduino default behavior. So I keep it
	 * for now and we'll see how it goes The eclipse default behavior is (starting
	 * from the project folder [configuration]/Source The Arduino default behavior
	 * is all in 1 location (so no subfolders)
	 *
	 * @param Source The source file to find the
	 * @return The base file name for the ouput if Source is "file.cpp" the output
	 *         is "file.cpp"
	 */
	public static IPath GetOutputName(IPath Source) {
		return Source;
	}

	/**
	 * creates links to the root files and folders of the source location
	 *
	 * @param source the location where the files are that need to be linked to
	 * @param target the location where the links are to be created
	 */
	public static void linkDirectory(IProject project, IPath source, IPath target) {

		File[] a = source.toFile().listFiles();
		if (a == null) {
			if (!myHasBeenLogged) {
				Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
						Messages.Helpers_error_link_folder_is_empty.replace(FILE, source.toOSString()), null));
				myHasBeenLogged = true;
			}
			return;
		}
		for (File f : a) {
			if (f.isDirectory()) {
				LinkFolderToFolder(project, source.append(f.getName()), target.append(f.getName()));
			} else {
				final IFile newFileHandle = project.getFile(target.append(f.getName()));
				try {
					newFileHandle.createLink(source.append(f.getName()),
							IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

	}

    public static Map<String, String> getEnvVarPlatformFileTools(ArduinoPlatform platform, boolean reportToolNotFound) {
        HashMap<String, String> vars = new HashMap<>();
        if (platform == null) {
            return vars;
        }
        if (platform.getToolsDependencies() == null) {
            return vars;
        }
        Iterable<ToolDependency> tools = platform.getToolsDependencies();
        String RUNTIME_TOOLS = ERASE_START + RUNTIME + DOT + TOOLS + DOT;
        String DOT_PATH = DOT + PATH;
        for (ToolDependency tool : tools) {
            String keyString = RUNTIME_TOOLS + tool.getName() + DOT_PATH;
            Tool theTool = tool.getTool();
            if (theTool == null) {
                if (reportToolNotFound) {
                    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
                            "Error adding platformFileTools while processing tool " + tool.getName() + " version " //$NON-NLS-1$ //$NON-NLS-2$
                                    + tool.getVersion() + " Installpath is null")); //$NON-NLS-1$
                }
            } else {
                IPath installPath = theTool.getInstallPath();
                vars.put(keyString, installPath.toOSString());
                keyString = RUNTIME_TOOLS + tool.getName() + tool.getVersion() + DOT_PATH;
                vars.put(keyString, installPath.toOSString());
                keyString = RUNTIME_TOOLS + tool.getName() + '-' + tool.getVersion() + DOT_PATH;
                vars.put(keyString, installPath.toOSString());
            }
        }
        return vars;
    }
}
