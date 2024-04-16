package io.sloeber.autoBuild.buildTools.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.buildTools.api.ExtensionBuildToolsProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;

public class MinGW32ToolsProvider extends ExtensionBuildToolsProvider {


	private static String myMinGWHome = null;
    private static IPath myMinGWBinPath = null;
    final private static String MINGW_ID = "Mingw32"; //$NON-NLS-1$
    private static boolean myHoldsAllTools = false;
	private static Map<String,IBuildTools> myBuildTools=new HashMap<>();


    @Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		findTools();
	}


    private  void findTools() {
    	myBuildTools.clear();
        myMinGWHome = org.eclipse.cdt.internal.core.MinGW.getMinGWHome();
        if (myMinGWHome != null) {

            myMinGWBinPath = new Path(myMinGWHome).append(BIN_FOLDER);
            IBuildTools buildTools= new MinGWBuildTools(myMinGWBinPath, this,MINGW_ID);
            myBuildTools.put(buildTools.getSelectionID(), buildTools);
            myHoldsAllTools = buildTools.holdsAllTools();
        }

	}

	@Override
    public boolean holdsAllTools() {
        return myHoldsAllTools;
    }


	@Override
	public IBuildTools getBuildTools(String buildToolsID) {
		return myBuildTools.get(buildToolsID);
	}


	@Override
	public IBuildTools getAnyInstalledBuildTools() {
		for(IBuildTools curBuildTools:myBuildTools.values()) {
			return curBuildTools;
		}
		return null;
	}

	@Override
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>( myBuildTools.values());
	}


	@Override
	public void refreshToolchains() {
		findTools();
	}


}
