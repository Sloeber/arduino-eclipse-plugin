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

        IProject activeProject = event.getProject();
        SloeberProject sloebberProject = SloeberProject.getSloeberProject(activeProject, true);
        if (sloebberProject == null) {
            // this is not a sloeber project so ignore
            return;
        }
        // don't do stuff during project creation
        if (IndexerController.isPosponed(activeProject)) {
            Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                    "Ignoring configuration change during project creation " + activeProject.getName())); //$NON-NLS-1$
            return;
        }

        switch (event.getEventType()) {
        case CProjectDescriptionEvent.ABOUT_TO_APPLY: {
            ICProjectDescription newProjDesc = event.getNewCProjectDescription();
            ICConfigurationDescription newConf = newProjDesc.getActiveConfiguration();
            BoardDescription newBoardDescriptor = sloebberProject.getBoardDescription(newConf);
            sloebberProject.setBoardDescription(newConf, newBoardDescriptor);
            break;
        }
        case CProjectDescriptionEvent.COPY_CREATED: {
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
            break;
        }
        default: {
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
        }
        }
    }

}
