package io.sloeber.core.tools;

import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import io.sloeber.core.api.Json.ArduinoLibraryVersion;
import io.sloeber.core.api.Json.ArduinoPlatformTool;
import io.sloeber.core.api.Json.ArduinoPlatformTooldDependency;
import io.sloeber.core.api.Json.ArduinoPlatformVersion;

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
     * @param target
     *            the folder name relative to the project
     * @param source
     *            the fully qualified name of the folder to link to
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
            log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                    Messages.Helpers_Create_folder_failed.replace(FOLDER, target.toString()), e));
        }
    }

    /**
     * This method creates a link folder in the project and add the folder as a
     * source path to the project.
     * 
     * It returns a list of paths that need to be added to the link paths
     *
     * @param project
     * @param Path
     * @throws CoreException
     *
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
     */
    public static List<IPath> addCodeFolder(IProject project, IPath toLinkFolder, String LinkName, boolean forceRoot) {
        List<IPath> addToIncludePath = new ArrayList<>();
        IFolder link = project.getFolder(LinkName);

        LinkFolderToFolder(project, toLinkFolder, new Path(LinkName));

        // Now the folder has been created we need to make sure the special
        // folders are added to the path

        String possibleIncludeFolder = "utility"; //$NON-NLS-1$
        File file = toLinkFolder.append(possibleIncludeFolder).toFile();
        if (file.exists()) {
            addToIncludePath.add(link.getFullPath().append(possibleIncludeFolder));
        }

        if (forceRoot) {
            addToIncludePath.add(link.getFullPath());
        } else {
            // add src or root give priority to src
            possibleIncludeFolder = ArduinoLibraryVersion.LIBRARY_SOURCE_FODER;
            file = toLinkFolder.append(possibleIncludeFolder).toFile();
            if (file.exists()) {
                addToIncludePath.add(link.getFullPath().append(possibleIncludeFolder));
            } else {
                addToIncludePath.add(link.getFullPath());
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
        return addToIncludePath;
    }

    public static void removeCodeFolder(IProject project, String LinkName) {
        IFolder link = project.getFolder(LinkName);
        if (link.exists()) {
            try {
                link.delete(true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method adds the content of a content stream to a file If the file
     * already exist the file remains untouched
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
     * @param project
     *            The project to add the arduino code to
     * @param boardDescriptor
     *            The board that is used
     * 
     * @return returns a list of paths that need to be added to the link folders
     */
    public static List<IPath> addArduinoCodeToProject(IProject project, BoardDescription boardDescriptor) {
        List<IPath> addToIncludePath = new ArrayList<>();
        IPath corePath = boardDescriptor.getActualCoreCodePath();
        if (corePath != null) {
            addToIncludePath.addAll(addCodeFolder(project, corePath, ARDUINO_CODE_FOLDER_PATH, true));
            IPath variantPath = boardDescriptor.getActualVariantPath();
            if ((variantPath == null) || (!variantPath.toFile().exists())) {
                // remove the existing link
                Helpers.removeCodeFolder(project, ARDUINO_VARIANT_FOLDER_PATH);
            } else {
                IPath redirectVariantPath = boardDescriptor.getActualVariantPath();
                addToIncludePath
                        .addAll(addCodeFolder(project, redirectVariantPath, ARDUINO_VARIANT_FOLDER_PATH, false));
            }
        }
        return addToIncludePath;
    }

    /**
     * Creates a new folder resource as a link or local
     *
     * @param Project
     *            the project the folder is added to
     * @param newFolderName
     *            the new folder to create (can contain subfolders)
     * @param linklocation
     *            if null a local folder is created using newFolderName if not null
     *            a link folder is created with the name newFolderName and pointing
     *            to linklocation
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
     * work is by deleting the build folder
     *
     * @param project
     */
    public static void deleteBuildFolder(IProject project, String cfgName) {
        IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
        if (buildInfo == null) {
            return; // Project is not a managed build project
        }

        IFolder buildFolder = project.getFolder(cfgName);
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
     * Given a source file calculates the base of the output file. this method may
     * not be needed if I can used the eclipse default behavior. However the eclipse
     * default behavior is different from the arduino default behavior. So I keep it
     * for now and we'll see how it goes The eclipse default behavior is (starting
     * from the project folder [configuration]/Source The Arduino default behavior
     * is all in 1 location (so no subfolders)
     *
     * @param Source
     *            The source file to find the
     * @return The base file name for the ouput if Source is "file.cpp" the output
     *         is "file.cpp"
     */
    public static IPath GetOutputName(IPath Source) {
        return Source;
    }

    /**
     * creates links to the root files and folders of the source location
     *
     * @param source
     *            the location where the files are that need to be linked to
     * @param target
     *            the location where the links are to be created
     */
    public static void linkDirectory(IProject project, IPath source, IPath target) {

        File[] a = source.toFile().listFiles();
        if (a == null) {
            if (!myHasBeenLogged) {
                log(new Status(IStatus.INFO, CORE_PLUGIN_ID,
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

    public static Map<String, String> getEnvVarPlatformFileTools(ArduinoPlatformVersion platform,
            boolean reportToolNotFound) {
        HashMap<String, String> vars = new HashMap<>();
        if (platform == null) {
            return vars;
        }
        if (platform.getToolsDependencies() == null) {
            return vars;
        }
        Iterable<ArduinoPlatformTooldDependency> tools = platform.getToolsDependencies();
        String RUNTIME_TOOLS = RUNTIME + DOT + TOOLS + DOT;
        String DOT_PATH = DOT + PATH;
        for (ArduinoPlatformTooldDependency tool : tools) {
            String keyString = RUNTIME_TOOLS + tool.getName() + DOT_PATH;
            ArduinoPlatformTool theTool = tool.getTool();
            if (theTool == null) {
                if (reportToolNotFound) {
                    log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
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
