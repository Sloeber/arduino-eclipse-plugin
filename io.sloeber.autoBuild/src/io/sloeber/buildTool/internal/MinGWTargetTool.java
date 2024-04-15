package io.sloeber.buildTool.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;
import io.sloeber.buildTool.api.IBuildToolManager.ToolType;
import io.sloeber.buildTool.api.IBuildToolProvider;

public class MinGWTargetTool implements IBuildTools {
	final private static String MAKE_COMMAND ="mingw32-make.exe"; //$NON-NLS-1$
    private  IPath myMinGWBinPath = null;
    private String myId=null;
    private IBuildToolProvider myToolProvider=null;
    private String myBuildCommand=null;
    private static boolean myHoldsAllTools = true;//TOFIX should at least check



    public MinGWTargetTool(IPath minGWBinPath,IBuildToolProvider toolProvider,String id) {
    	 myMinGWBinPath = minGWBinPath;
    	 myId=id;
    	 myToolProvider=toolProvider;
    	 if(minGWBinPath.append(MAKE_COMMAND).toFile().exists()) {
    		 myBuildCommand=minGWBinPath.append(MAKE_COMMAND).toOSString();
    	 }
    }

    @Override
    public boolean holdsAllTools() {
        return myHoldsAllTools;
    }

    @Override
    public String getSelectionID() {
        return myId;
    }
	@Override
	public String getProviderID() {
		return myToolProvider.getID();
	}

    @Override
    public Map<String, String> getEnvironmentVariables() {
    	return null;
//        Map<String ,  String>ret=new HashMap<>();
//        if(isWindows) {
//        	//PATH must be capitals
//        	ret.put(ENV_VAR_PATH,myMinGWBinPath.toString()+File.pathSeparator+"%PATH%");
//        }else {
//        	ret.put(ENV_VAR_PATH,myMinGWBinPath.toString()+File.pathSeparator+"$PATH");
//        }
//        return ret;
    }

    @Override
    public Map<String, String> getToolVariables() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCommand(ToolType toolType) {
        return IBuildToolManager.getDefault().getDefaultCommand(getToolFlavour(), toolType);
    }

    @Override
    public IPath getToolLocation() {
        return myMinGWBinPath;
    }

    @Override
    public ToolFlavour getToolFlavour() {
        return ToolFlavour.MINGW;
    }

	@Override
	public String getBuildCommand() {
		return myBuildCommand;
	}

	@Override
	public String getPathExtension() {
		return myMinGWBinPath.toOSString();
	}

	@Override
	public String getDiscoveryCommand(ToolType toolType) {
		return getToolLocation().append( getCommand(toolType)).toString() +DISCOVERY_PARAMETERS;
	}

	@Override
	public boolean isProjectTypeSupported(IProjectType projectType) {
		return myToolProvider.supports(projectType);
	}

}
