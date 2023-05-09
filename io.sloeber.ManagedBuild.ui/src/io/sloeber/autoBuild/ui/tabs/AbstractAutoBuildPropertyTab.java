package io.sloeber.autoBuild.ui.tabs;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractAutoBuildPropertyTab extends AbstractCPropertyTab {

    //protected final static String ARGS_PREFIX = "io.sloeber.automake"; //$NON-NLS-1$
    Map<String, IAutoBuildConfigurationDescription> myAutoConfDescMap = new HashMap<>();
    IAutoBuildConfigurationDescription myAutoConfDesc = null;

    @Override
    public void updateData(ICResourceDescription cfgd) {

        for (ICConfigurationDescription curcfg : page.getCfgsEditable()) {
            if (myAutoConfDescMap.get(curcfg.getId()) == null) {
                myAutoConfDesc = AutoBuildProject.getAutoBuildConfig(curcfg);
                myAutoConfDescMap.put(curcfg.getId(), myAutoConfDesc);
            }
        }
        if (cfgd != null) {
            myAutoConfDesc = myAutoConfDescMap.get(cfgd.getConfiguration().getId());
        }
        updateButtons();
    }

    @Override
    public void performApply(ICResourceDescription src, ICResourceDescription dst) {
        //JABA this method is forced to override but is not used
    }

    @Override
    public boolean canSupportMultiCfg() {
        return false;
    }

    protected Shell getShell() {
        return usercomp.getShell();
    }
}
