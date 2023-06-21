package io.sloeber.autoBuild.ui.tabs;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractAutoBuildPropertyTab extends AbstractCPropertyTab {

    //protected final static String ARGS_PREFIX = "io.sloeber.automake"; //$NON-NLS-1$
    Map<String, IAutoBuildConfigurationDescription> myAutoConfDescMap = new HashMap<>();
    IAutoBuildConfigurationDescription myAutoConfDesc = null;

    @Override
    public void updateData(ICResourceDescription cfgd) {
        if (cfgd != null) {
            myAutoConfDesc = AutoBuildConfigurationDescription.getFromConfig(cfgd.getConfiguration());
        }
        if (page.isMultiCfg()) {
            setAllVisible(false, null);
        } else {
            setAllVisible(true, null);
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
