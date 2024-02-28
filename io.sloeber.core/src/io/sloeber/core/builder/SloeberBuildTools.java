package io.sloeber.core.builder;

import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;
import io.sloeber.buildTool.api.IBuildToolManager.ToolType;
import io.sloeber.buildTool.api.IBuildTools;

public class SloeberBuildTools implements IBuildTools {

	@Override
	public boolean holdsAllTools() {
		return false;
	}

	@Override
	public String getSelectionID() {
		return "SloeberBuildTools.id";
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return null;
	}

	@Override
	public Map<String, String> getToolVariables() {
		return null;
	}

	@Override
	public String getCommand(ToolType toolType) {
		return "SloeberBuildTools: no command";
	}

	@Override
	public IPath getToolLocation() {
		return null;
	}

	@Override
	public ToolFlavour getToolFlavour() {
		return ToolFlavour.GCC;
	}

	@Override
	public String getProviderID() {
		return SloeberBuildToolsProvider.SLOEBER_BUILD_TOOL_PROVIDER_ID;
	}

	@Override
	public String getBuildCommand() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPathExtension() {
		// TODO Auto-generated method stub
		return null;
	}

}
