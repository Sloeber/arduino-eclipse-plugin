package io.sloeber.autoBuild.Internal;

import java.nio.file.Path;
import java.util.Map;

import io.sloeber.autoBuild.api.IToolProvider;
import io.sloeber.autoBuild.api.ITargetToolManager;
import io.sloeber.autoBuild.api.ITargetToolManager.ToolFlavour;
import io.sloeber.autoBuild.api.ITargetToolManager.ToolType;
import org.eclipse.cdt.internal.core.MinGW;

public class MinGWToolProvider implements IToolProvider {
    private static String myMinGWHome = null;
    private static Path myMinGPath = null;
    final private static String MINGW_ID = "system_Installed_Mingw"; //$NON-NLS-1$
    private static boolean holdsAllTools = false;

    static {
        myMinGWHome = org.eclipse.cdt.internal.core.MinGW.getMinGWHome();
        if (myMinGWHome != null) {
            holdsAllTools = true;
            myMinGPath = Path.of(myMinGWHome);
        }
    }

    public MinGWToolProvider() {
        //nothing to be done here
    }

    @Override
    public boolean holdsAllTools() {
        return holdsAllTools;
    }

    @Override
    public String getSelectionID() {
        return MINGW_ID;
    }

    @Override
    public Map<String, String> getEnvironmentVariables() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getToolVariables() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCommand(ToolType toolType) {
        return ITargetToolManager.getDefault().getDefaultCommand(getToolFlavour(), toolType);
    }

    @Override
    public Path getToolLocation(ToolType toolType) {
        return myMinGPath;
    }

    @Override
    public ToolFlavour getToolFlavour() {
        return ToolFlavour.MINGW;
    }

    @Override
    public String getID() {
        return MINGW_ID;
    }

}
