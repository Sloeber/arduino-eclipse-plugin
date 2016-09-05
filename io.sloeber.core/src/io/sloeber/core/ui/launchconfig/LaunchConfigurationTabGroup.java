package io.sloeber.core.ui.launchconfig;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;

public class LaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup{

	@Override
	/**
     * {@inheritDoc}
     */
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		AbstractLaunchConfigurationTab mainTab = new MainTab();
		AbstractLaunchConfigurationTab commonTab = new CommonTab();
        
		AbstractLaunchConfigurationTab[] tabs = {mainTab, commonTab};
        setTabs(tabs);
    }

}
