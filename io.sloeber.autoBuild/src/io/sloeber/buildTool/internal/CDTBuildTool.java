package io.sloeber.buildTool.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.cdt.build.gcc.core.GCCToolChain;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.core.runtime.IPath;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;
import io.sloeber.buildTool.api.IBuildToolManager.ToolType;

public class CDTBuildTool implements IBuildTools {

    private String myProviderID=null;
    private IToolChain myCdtBuildTools=null;


    public CDTBuildTool(IToolChain cdtBuildTools) {
    	myCdtBuildTools=cdtBuildTools;
    	myProviderID=cdtBuildTools.getProvider().getId();
	}

	@Override
    public String getSelectionID() {
        return myCdtBuildTools.getId();
    }


	@Override
	public String getProviderID() {
		return myProviderID;
	}


    @Override
    public Map<String, String> getEnvironmentVariables() {
    	Map<String, String> ret=new HashMap<>();
    	for(IEnvironmentVariable curVariable :myCdtBuildTools.getVariables()) {
    		ret.put(curVariable.getName(), curVariable.getValue());
    	}
        return ret;
    }

    @Override
    public Map<String, String> getToolVariables() {
        return getEnvironmentVariables();
    }

    @Override
    public String getCommand(ToolType toolType) {
        if (holdsAllTools()) {
            IBuildToolManager toolProviderManager = IBuildToolManager.getDefault();
            return toolProviderManager.getDefaultCommand(getToolFlavour(), toolType);
        }
        return null;
    }

    @Override
    public IPath getToolLocation() {
    	if(myCdtBuildTools instanceof GCCToolChain) {
    		GCCToolChain toolChain=(GCCToolChain)myCdtBuildTools;
    		return IPath.fromOSString( toolChain.getPath().getParent().toString());
    	}
    	return IPath.fromOSString( myCdtBuildTools.getCommandPath( Path.of(EMPTY_STRING)).toString());
    }

    @Override
    public ToolFlavour getToolFlavour() {
    	//TOFIX find how to do this
    	return ToolFlavour.GNU;
//    	switch(myCdtBuildTools.getTypeId())
//        return myToolFlavour;
    }

    @Override
    public boolean holdsAllTools() {
        return true;
    }

	@Override
	public String getBuildCommand() {
		return null;
	}

	@Override
	public String getPathExtension() {
		return null;
	}

	@Override
	public String getDiscoveryCommand(ToolType toolType) {
		return getToolLocation().append( getCommand(toolType)).toString() + DISCOVERY_PARAMETERS;
	}


}
