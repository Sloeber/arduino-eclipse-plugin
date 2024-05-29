package io.sloeber.autoBuild.buildTools.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.cdt.build.gcc.core.GCCToolChain;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolType;
import io.sloeber.autoBuild.schema.api.IProjectType;

public class CDTBuildTools implements IBuildTools {

    private String myProviderID=null;
    private IToolChain myCdtBuildTools=null;


    public CDTBuildTools(IToolChain cdtBuildTools) {
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
            IBuildToolsManager toolProviderManager = IBuildToolsManager.getDefault();
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

	@Override
	public boolean isProjectTypeSupported(IProjectType projectType) {
		return true;
	}

}
