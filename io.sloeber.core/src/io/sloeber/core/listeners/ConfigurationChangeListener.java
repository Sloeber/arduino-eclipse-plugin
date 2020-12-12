package io.sloeber.core.listeners;

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICDescriptionDelta;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

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
        ICDescriptionDelta projectDelta = event.getProjectDelta();
        if (projectDelta != null) {
            int projectChangeFlags = projectDelta.getChangeFlags();
            int projectDeltaKind = projectDelta.getDeltaKind();
        }
        ICDescriptionDelta cfgDelta = event.getActiveCfgDelta();
        if (cfgDelta != null) {
            int cfgChangeFlags = cfgDelta.getChangeFlags();
            int cfgDeltaKind = cfgDelta.getDeltaKind();
        }
        ICDescriptionDelta defaultDelta = event.getDefaultSettingCfgDelta();
        if (defaultDelta != null) {
            int changeFlags = defaultDelta.getChangeFlags();
            int deltaKind = defaultDelta.getDeltaKind();
        }
        SloeberProject sloebberProject = SloeberProject.getSloeberProject(activeProject);

        ICProjectDescription newProjDesc = event.getNewCProjectDescription();
        ICConfigurationDescription newConf = newProjDesc.getActiveConfiguration();
        BoardDescription newBoardDescriptor = sloebberProject.getBoardDescription(newConf);
        sloebberProject.setBoardDescription(newConf, newBoardDescriptor);
    }

}
