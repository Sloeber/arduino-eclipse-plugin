package io.sloeber.buildTool.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.ExtensionBuildToolProvider;

public class MinGW32ToolProvider extends ExtensionBuildToolProvider {


	private static String myMinGWHome = null;
    private static IPath myMinGWBinPath = null;
    final private static String MINGW_ID = "Mingw32"; //$NON-NLS-1$
    private static boolean myHoldsAllTools = false;
	private static Map<String,IBuildTools> myTargetTools=new HashMap<>();


    @Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		findTools();
	}


    private  void findTools() {
    	myTargetTools.clear();
        myMinGWHome = org.eclipse.cdt.internal.core.MinGW.getMinGWHome();
        if (myMinGWHome != null) {

            myMinGWBinPath = new Path(myMinGWHome).append(BIN_FOLDER);
            IBuildTools targetTool= new MinGWTargetTool(myMinGWBinPath, this,MINGW_ID);
            myTargetTools.put(targetTool.getSelectionID(), targetTool);
            myHoldsAllTools = targetTool.holdsAllTools();
        }

	}

	@Override
    public boolean holdsAllTools() {
        return myHoldsAllTools;
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
	public void refreshToolchains() {
		findTools();
	}


}
