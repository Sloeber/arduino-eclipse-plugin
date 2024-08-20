package io.sloeber.core.listeners;

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import io.sloeber.core.internal.SloeberConfiguration;

public class ConfigurationChangeListener implements ICProjectDescriptionListener {

    @Override
    public void handleEvent(CProjectDescriptionEvent event) {

        switch (event.getEventType()) {
        case CProjectDescriptionEvent.ABOUT_TO_APPLY: {
            	ICProjectDescription newProjDesc = event.getNewCProjectDescription();
            	for(ICConfigurationDescription curConfig:newProjDesc.getConfigurations()) {
            		SloeberConfiguration sloeberConfig=SloeberConfiguration.getConfig(curConfig);
            		if(sloeberConfig==null) {
            			return;
            		}
            		sloeberConfig.aboutToApplyConfigChange();
            	}
            	break;
        	}
        case CProjectDescriptionEvent.APPLIED: {
        	ICProjectDescription newProjDesc = event.getNewCProjectDescription();
        	for(ICConfigurationDescription curConfig:newProjDesc.getConfigurations()) {
        		SloeberConfiguration sloeberConfig=SloeberConfiguration.getConfig(curConfig);
        		if(sloeberConfig==null) {
        			return;
        		}
        		sloeberConfig.appliedConfigChange();
        	}
        	break;
    	}

        default: {
            // should not happen
        }
        }
    }


}
