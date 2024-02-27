package io.sloeber.core.tools;

import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.core.Messages;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 *
 * @author Jan Baeyens
 *
 */
public class Helpers {

    private static final String FILE = Messages.FILE_TAG;
    private static final String FOLDER = Messages.FOLDER_TAG;

    private static boolean myHasBeenLogged = false;

    /**
     * This method is the internal working class that adds the provided include
     * paths to the configuration for all languages.
     *
     * @param configurationDescription
     *            The configuration description of the project to add it to
     * @param IncludePath
     *            The path to add to the include folders
     * @param isWorkspacePath
     *            is this path in the workspace
     * 
     * @return true if the configuration description has changed
     *         (setprojectdescription is needed to make the changes effective)
     */
    public static boolean addIncludeFolder(ICConfigurationDescription configurationDescription,
            List<IPath> IncludePaths, boolean isWorkspacePath) {

        boolean confDesckMustBeSet = false;
        if (IncludePaths == null) {
            return false;
        }
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
                    HashSet<IPath> toAddPaths = new HashSet<>(IncludePaths);
                    for (ICLanguageSettingEntry curLangSetting : OrgIncludeEntries) {
                        CIncludePathEntry curIncludePathEntry = (CIncludePathEntry) curLangSetting;
                        toAddPaths.remove(curIncludePathEntry.getFullPath());
                    }
                    if (!toAddPaths.isEmpty()) {
                        confDesckMustBeSet = true;
                        ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length
                                + toAddPaths.size()];
                        System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
                        int startPointer = OrgIncludeEntries.length;
                        for (IPath curPath : toAddPaths) {
                            IncludeEntries[startPointer++] = new CIncludePathEntry(curPath, pathSetting);
                        }
                        lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
                    }
                }
            }
        }
        return confDesckMustBeSet;
    }

    /**
     * Removes include folders that are not valid. This method does not save the
     * configurationDescription description
     *
     * @param configurationDescription
     *            a writable project description the configuration that is checked
     * @return true is a include path has been removed. False if the include path
     *         remains unchanged. If true the projectdescription must be set
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
                            log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "Removed invalid include path" + cusPath, //$NON-NLS-1$
                                    null));
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
     * @param project
     *            the project the newly created folder will belong to
     * @param projectFolder
     *            the folder name relative to the project
     * @param source
     *            the fully qualified name of the folder to link to
     */
    public static void LinkFolderToFolder(IPath source, IFolder projectFolder) {
        IProject project = projectFolder.getProject();

        // create target parent folder and grandparents
        IPath ParentFolders = projectFolder.getProjectRelativePath().removeLastSegments(1);
        for (int curfolder = ParentFolders.segmentCount() - 1; curfolder >= 0; curfolder--) {
            try {
                createNewFolder(project.getFolder(ParentFolders.removeLastSegments(curfolder)), null);
            } catch (@SuppressWarnings("unused") CoreException e) {// ignore this error as the parent
                // folders may have been created yet
            }
        }

        // create the actual link
        try {
            createNewFolder(projectFolder, source);
        } catch (CoreException e) {
            log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                    Messages.Helpers_Create_folder_failed.replace(FOLDER, projectFolder.toString()), e));
        }
    }


    public static void removeCodeFolder(IFolder deleteFolder) {
        if (deleteFolder.exists()) {
            try {
                deleteFolder.delete(true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method adds the content of a content stream to a file If the file
     * already exist the file remains untouched
     *
     * @param file
     *            The file to create
     * @param contentStream
     *            The stream to put in the file
     * @param monitor
     *            A monitor to show progress
     * @throws CoreException
     */
	public static IFile addFileToProject(IFile file, InputStream contentStream, IProgressMonitor monitor,
			boolean overwrite) throws CoreException {
		file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		if (overwrite && file.exists()) {
			file.delete(true, null);
			// TODO is this needed? I don't think so
			file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}

		if (!file.exists() && (contentStream != null)) {
			IPath filePath = file.getProjectRelativePath();
			IFolder curFolder = file.getProject().getFolder(filePath.segment(0));
			int curSegment = 1;
			do {

				if (!curFolder.exists()) {
					curFolder.create(true, false, monitor);
				}
				curFolder = curFolder.getFolder(filePath.segment(curSegment));
				curSegment++;
			} while (curSegment < filePath.segmentCount() );
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
     * Creates a new folder resource as a link or local
     *
     * @param newFolder
     *            the new folder to create (can contain subfolders)
     * @param linklocation
     *            if null a local folder is created using newFolderName if not null
     *            a link folder is created with the name newFolderName and pointing
     *            to linklocation
     *
     * @return nothing
     * @throws CoreException
     */
    public static void createNewFolder(IFolder newFolder, IPath linklocation) throws CoreException {
        if (linklocation != null) {
            URI relativeLinklocation = newFolder.getProject().getPathVariableManager()
                    .convertToRelative(URIUtil.toURI(linklocation), false, null);
            newFolder.createLink(relativeLinklocation, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
        } else {
            newFolder.create(0, true, null);
        }

    }

    /**
     * Set the project to force a rebuild. This method is called after the arduino
     * settings have been updated. Note the only way I found I could get this to
     * work is by deleting the build folder
     *
     * @param project
     */
    public static void deleteBuildFolder(IProject project, String cfgName) {
        ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(project, false);
        ICConfigurationDescription cdtConfigurationDescription = cdtProjectDescription.getConfigurationByName(cfgName);
        IAutoBuildConfigurationDescription autoData = IAutoBuildConfigurationDescription
                .getConfig(cdtConfigurationDescription);

        IFolder buildFolder = autoData.getBuildFolder();
        if (buildFolder.exists()) {
            try {
                buildFolder.delete(true, null);
            } catch (CoreException e) {
                log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                        Messages.Helpers_delete_folder_failed.replace(FOLDER, cfgName), e));
            }
        }
    }

    /**
     * creates links to the root files and folders of the source location
     *
     * @param source
     *            the location where the files are that need to be linked to
     * @param target
     *            the location where the links are to be created
     */
    public static void linkDirectory(IPath source, IFolder target) {

        File[] sourceFiles = source.toFile().listFiles();
        if (sourceFiles == null) {
            if (!myHasBeenLogged) {
                log(new Status(IStatus.INFO, CORE_PLUGIN_ID,
                        Messages.Helpers_error_link_folder_is_empty.replace(FILE, source.toOSString()), null));
                myHasBeenLogged = true;
            }
            return;
        }
        for (File curFile : sourceFiles) {
            if (curFile.isDirectory()) {
                LinkFolderToFolder(source.append(curFile.getName()), target.getFolder(curFile.getName()));
            } else {
                try {
                    target.getFile(curFile.getName()).createLink(source.append(curFile.getName()),
                            IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
                } catch (CoreException e) {
                    e.printStackTrace();
                }
            }
        }

    }

}
