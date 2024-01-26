package io.sloeber.core.listeners;

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.Const;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.SloeberProject;

public class ConfigurationChangeListener implements ICProjectDescriptionListener {

    @Override
    public void handleEvent(CProjectDescriptionEvent event) {
        //
        //        IProject activeProject = event.getProject();
        //        ISloeberConfiguration sloeberProject = ISloeberConfiguration.getActiveConfig(activeProject);
        //        if (sloeberProject == null) {
        //            // this is not a sloeber project so ignore
        //            return;
        //        }
        //        // don't do stuff during project creation
        //        if (IndexerController.isPosponed(activeProject)) {
        //            Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
        //                    "Ignoring configuration change during project creation " + activeProject.getName())); //$NON-NLS-1$
        //            return;
        //        }
        //
        //        switch (event.getEventType()) {
        //        case CProjectDescriptionEvent.ABOUT_TO_APPLY: {
        //            ICProjectDescription newProjDesc = event.getNewCProjectDescription();
        //            ICProjectDescription oldProjDesc = event.getOldCProjectDescription();
        //            //TOFIX do we need to do something       sloeberProject.configChangeAboutToApply(newProjDesc, oldProjDesc);
        //            break;
        //        }
        //
        //        default: {
        //            // should not happen
        //        }
        //        }
    }

}
