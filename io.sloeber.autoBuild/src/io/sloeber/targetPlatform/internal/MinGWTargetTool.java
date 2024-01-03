package io.sloeber.targetPlatform.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.targetPlatform.api.ITargetTool;
import io.sloeber.targetPlatform.api.ITargetToolManager;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolType;

public class MinGWTargetTool implements ITargetTool {
    private  IPath myMinGWBinPath = null;
    private String myId=null;
    private String myProviderID=null;
    private static boolean myHoldsAllTools = true;//TOFIX should at least check



    public MinGWTargetTool(IPath minGWBinPath,String providerID,String id) {
    	 myMinGWBinPath = minGWBinPath;
    	 myId=id;
    	 myProviderID=providerID;
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
		return myProviderID;
	}

    @Override
    public Map<String, String> getEnvironmentVariables() {
        Map<String ,  String>ret=new HashMap<>();
        if(isWindows) {
        	ret.put("path",myMinGWBinPath.toString()+File.pathSeparator+"%path%");
        }else {
        	ret.put("path",myMinGWBinPath.toString()+File.pathSeparator+"$path");
        }
        return ret;
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
    public IPath getToolLocation(ToolType toolType) {
        return myMinGWBinPath;
    }

    @Override
    public ToolFlavour getToolFlavour() {
        return ToolFlavour.MINGW;
    }




}
