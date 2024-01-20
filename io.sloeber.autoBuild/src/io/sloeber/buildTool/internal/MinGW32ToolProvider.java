package io.sloeber.buildTool.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolProvider;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

public class MinGW32ToolProvider implements IBuildToolProvider {
    private static String myMinGWHome = null;
    private static IPath myMinGWBinPath = null;
    final private static String MINGW_ID = "Mingw32"; //$NON-NLS-1$
    final private static String MINGW_Name = "Mingw 32 bit tools"; //$NON-NLS-1$
    private static boolean myHoldsAllTools = false;
	private static Map<String,IBuildTools> myTargetTools=new HashMap<>();
    static {
    	findTools();
    }

    public MinGW32ToolProvider() {
        //nothing to be done here
    }

    private static void findTools() {
    	myTargetTools.clear();
        myMinGWHome = org.eclipse.cdt.internal.core.MinGW.getMinGWHome();
        if (myMinGWHome != null) {
           
            myMinGWBinPath = new Path(myMinGWHome).append(BIN_FOLDER);
            IBuildTools targetTool= new MinGWTargetTool(myMinGWBinPath, MINGW_ID,MINGW_ID);
            myTargetTools.put(MINGW_ID, targetTool);
            myHoldsAllTools = targetTool.holdsAllTools();
        }
		
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
	public IBuildTools getTargetTool(String targetToolID) {
		return myTargetTools.get(targetToolID);
	}


	@Override
	public IBuildTools getAnyInstalledTargetTool() {
		for(IBuildTools curTargetTool:myTargetTools.values()) {
			return curTargetTool;
		}
		return null;
	}

	@Override
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>( myTargetTools.values());
	}

	@Override
	public String getName() {
		return MINGW_Name;
	}

	@Override
	public void refreshToolchains() {
		findTools();
	}
	
	
}
