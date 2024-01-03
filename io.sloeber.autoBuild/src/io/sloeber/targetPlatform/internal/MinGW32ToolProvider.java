package io.sloeber.targetPlatform.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.targetPlatform.api.ITargetTool;
import io.sloeber.targetPlatform.api.ITargetToolProvider;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

public class MinGW32ToolProvider implements ITargetToolProvider {
    private static String myMinGWHome = null;
    private static IPath myMinGWBinPath = null;
    final private static String MINGW_ID = "Mingw32"; //$NON-NLS-1$
    private static boolean myHoldsAllTools = false;
	private static Map<String,ITargetTool> myTargetTools=new HashMap<>();
    static {
        myMinGWHome = org.eclipse.cdt.internal.core.MinGW.getMinGWHome();
        if (myMinGWHome != null) {
           
            myMinGWBinPath = new Path(myMinGWHome).append(BIN_FOLDER);
            ITargetTool targetTool= new MinGWTargetTool(myMinGWBinPath, MINGW_ID,MINGW_ID);
            myTargetTools.put(MINGW_ID, targetTool);
            myHoldsAllTools = targetTool.holdsAllTools();
        }
    }

    public MinGW32ToolProvider() {
        //nothing to be done here
    }

    @Override
    public boolean holdsAllTools() {
        return myHoldsAllTools;
    }


    @Override
    public String getID() {
        return MINGW_ID;
    }

	@Override
	public Set<String> getTargetToolIDs() {
		 return myTargetTools.keySet();
	}

	@Override
	public ITargetTool getTargetTool(String targetToolID) {
		return myTargetTools.get(targetToolID);
	}

	@Override
	public ToolFlavour getToolFlavour() {
		return ToolFlavour.MINGW;
	}

	@Override
	public ITargetTool getAnyInstalledTargetTool() {
		for(ITargetTool curTargetTool:myTargetTools.values()) {
			return curTargetTool;
		}
		return null;
	}

	@Override
	public Set<ITargetTool> getAllInstalledTargetTools() {
		return new HashSet<>( myTargetTools.values());
	}
	
	
}
