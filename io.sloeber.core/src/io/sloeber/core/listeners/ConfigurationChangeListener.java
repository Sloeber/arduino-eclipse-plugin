package io.sloeber.core.listeners;

/*** Message from jan baeyens
 * this listener makes sure that when you change from one configuration to another
 * the correct hardware libraries are attached to the project
 * for instance you can have a project with 2 configurations
 * one for teensy
 * one for arduino uno
 *
 *
 * when you use the spi library the library is a completely different library
 * this code takes care that you use the correct library when switching configuration
 *
 */

import java.io.File;

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;

@SuppressWarnings({ "unused" })
public class ConfigurationChangeListener implements ICProjectDescriptionListener {

    @Override
    public void handleEvent(CProjectDescriptionEvent event) {
        if (event.getEventType() != CProjectDescriptionEvent.ABOUT_TO_APPLY) {
            return;
        }

        IProject activeProject = event.getProject();
        // only handle arduino nature projects
        try {
            if (!activeProject.hasNature(Const.ARDUINO_NATURE_ID)) {
                return;
            }
        } catch (Exception e) {
            // don't care don't update
            return;
        }
        if (IndexerController.isPosponed(activeProject)) {
            Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                    "Ignoring configuration change during project creation " + activeProject.getName())); //$NON-NLS-1$
            return;
        }
        ICProjectDescription newProjDesc = event.getNewCProjectDescription();
        ICConfigurationDescription newConf = newProjDesc.getActiveConfiguration();
        IProject project = newConf.getProjectDescription().getProject();
        SloeberProject newSloebberProject = SloeberProject.getSloeberProject(project);
        BoardDescription newBoardDescriptor = newSloebberProject.getBoardDescription(newConf);

        ICProjectDescription oldprojDesc = event.getOldCProjectDescription();
        ICConfigurationDescription oldConf = oldprojDesc.getActiveConfiguration();
        IProject oldProject = oldConf.getProjectDescription().getProject();
        SloeberProject oldSloebberProject = SloeberProject.getSloeberProject(oldProject);
        BoardDescription oldBoardDescriptor = oldSloebberProject.getBoardDescription(oldConf);

        if (oldprojDesc.getName().equals(newProjDesc.getName())) {

            File newBoardsFile = newBoardDescriptor.getReferencingBoardsFile();
            File oldBoardsFile = oldBoardDescriptor.getReferencingBoardsFile();
            String newBoardsName = newBoardDescriptor.getBoardName();
            String oldBoardsName = oldBoardDescriptor.getBoardName();
            // only if the boardsFile or variant Changed we need to reattach libraries and
            // cores
            if (newBoardsFile.equals(oldBoardsFile) && newBoardsName.equals(oldBoardsName)) {
                return;
            }

            try {
                Helpers.setDirtyFlag(activeProject, newConf);
                Helpers.addArduinoCodeToProject(newBoardDescriptor, newConf);
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (!newBoardsFile.equals(oldBoardsFile)) {
                Libraries.reAttachLibrariesToProject(newConf);
            }
        } else {
            // we are doing a swap between configs
        }
    }

}
