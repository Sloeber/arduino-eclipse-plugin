package io.sloeber.autobuild.cdt.ui;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;

public abstract class AbstractAutoBuildPropertyTab extends AbstractCPropertyTab {
    protected final static String ARGS_PREFIX = "io.sloeber.automake"; //$NON-NLS-1$
    Map<String, IAutoBuildConfigurationDescription> myAutoConfDescMap = new HashMap<>();
    IAutoBuildConfigurationDescription myAutoConfDesc = null;

    @Override
    public void updateData(ICResourceDescription cfgd) {
        if (cfgd == null)
            return;
        for (ICConfigurationDescription curcfg : page.getCfgsEditable()) {
            if (myAutoConfDescMap.get(curcfg.getId()) == null) {
                IAutoBuildConfigurationDescription autoCfg = AutoBuildProject.getAutoBuildConfig(curcfg);
                myAutoConfDescMap.put(curcfg.getId(), autoCfg);
            }
        }
        myAutoConfDesc = myAutoConfDescMap.get(cfgd.getConfiguration().getId());
        updateButtons();
    }

}
