package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.make.core.IMakeTarget;
import org.eclipse.cdt.make.core.IMakeTargetManager;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.make.internal.core.MakeTarget;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;

public class Sketch {

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
        return getConfigLocalFile(proj).exists();
    }

    public static IFile getConfigVersionFile(IProject proj) {
        return proj.getFile(SLOEBER_CFG);
    }

    /*
     * Get the sloeber configuration file
     */
    public static IFile getConfigLocalFile(IProject proj) {
        return proj.getFile(SLOEBER_PROJECT);
    }

    public static boolean removeLibrariesFromProject(IProject project, ICProjectDescription projDesc,
            Set<String> libraries) {
        return Libraries.removeLibrariesFromProject(project, projDesc, libraries);

    }

    public static Map<String, IPath> getAllAvailableLibraries(ISloeberConfiguration confDesc) {
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
        CoreModel coreModel = CoreModel.getDefault();

        Helpers.addCodeFolder(path, project.getFolder(path.lastSegment()), false);

    }

}
