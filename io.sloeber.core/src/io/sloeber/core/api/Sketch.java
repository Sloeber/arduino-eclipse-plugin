package io.sloeber.core.api;

import static io.sloeber.core.common.Const.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Messages;
import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;

public class Sketch {

    public static IStatus isUploadableProject(IProject project) {
        try {
            if (project == null || !project.hasNature(ARDUINO_NATURE_ID)) {
                return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_no_arduino_sketch, null);
            }
        } catch (CoreException e) {
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_Project_nature_unaccesible, e);
        }
        return Status.OK_STATUS;
    }



    /**
     * Synchronous upload of the sketch returning the status.
     *
     * @param project
     * @return the status of the upload. Status.OK means upload is OK
     */
    public static IStatus syncUpload(IProject project) {

        IStatus ret = isUploadableProject(project);
        if (!ret.isOK()) {
            return ret;
        }
        SloeberProject sProject = SloeberProject.getSloeberProject(project, true);
        return sProject.upload();
    }

    /**
     * given a project look in the source code for the line of code that sets the
     * baud rate on the board Serial.begin([baudRate]);
     *
     *
     *
     * return the integer value of [baudrate] or in case of error a negative value
     *
     * @param iProject
     * @return
     */
    public static int getCodeBaudRate(IProject iProject) {
        String parentFunc = "setup"; //$NON-NLS-1$
        String childFunc = "Serial.begin"; //$NON-NLS-1$
        String baudRate = IndexHelper.findParameterInFunction(iProject, parentFunc, childFunc, null);
        if (baudRate == null) {
            return -1;
        }
        return Integer.parseInt(baudRate);

    }

    public static void reAttachLibrariesToProject(IProject iProject) {
        Libraries.reAttachLibrariesToProject(iProject);
    }

    public static boolean isSketch(IProject proj) {
        try {
            return proj.hasNature(ARDUINO_NATURE_ID);
        } catch (CoreException e) {
            // ignore
            e.printStackTrace();
        }
        return false;
    }

    public static boolean removeLibrariesFromProject(IProject project, ICProjectDescription projDesc,
            Set<String> libraries) {
        return Libraries.removeLibrariesFromProject(project, projDesc, libraries);

    }

    public static boolean addLibrariesToProject(IProject project, ICConfigurationDescription confDesc,
            Set<String> libraries) {
        Map<String, List<IPath>> foldersToChange = Libraries.addLibrariesToProject(project, confDesc, libraries);
        return Libraries.adjustProjectDescription(confDesc, foldersToChange);
    }

    public static Map<String, IPath> getAllAvailableLibraries(ICConfigurationDescription confDesc) {
        return Libraries.getAllInstalledLibraries(confDesc);
    }

    public static Set<String> getAllImportedLibraries(IProject project) {
        return Libraries.getAllLibrariesFromProject(project);
    }

    /**
     * Adds a folder to the project and adds the folder to the linked folders if
     * needed Stores the projectDescription if it has changed
     * 
     * @param project
     *            the project to add the folder to
     * @param path
     *            the path that needs adding to the project
     * 
     * @throws CoreException
     */
    public static void addCodeFolder(IProject project, Path path) throws CoreException {
        boolean projDescNeedsSaving = false;
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(project);

        List<IPath> includeFolders = Helpers.addCodeFolder(project, path, path.lastSegment(), false);
        for (ICConfigurationDescription curConfig : projectDescription.getConfigurations()) {
            if (Helpers.addIncludeFolder(curConfig, includeFolders, true)) {
                projDescNeedsSaving = true;
            }
        }
        if (projDescNeedsSaving) {
            coreModel.getProjectDescriptionManager().setProjectDescription(project, projectDescription, true, null);
        }
    }

}
