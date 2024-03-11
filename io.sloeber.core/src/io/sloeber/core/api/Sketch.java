package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.core.common.IndexHelper;
import io.sloeber.core.tools.Helpers;

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
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(iProject, false);
    	for (ICConfigurationDescription curconfDesc : projectDescription.getConfigurations()) {
    		ISloeberConfiguration.getConfig(curconfDesc).reAttachLibraries();
    	}
    }

    public static boolean isSketch(IProject proj) {
        return IAutoBuildConfigurationDescription.getActiveConfig(proj,false)!=null;
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
         Helpers.LinkFolderToFolder(path, project.getFolder(path.lastSegment()));

    }

}
