package io.sloeber.core.builder;

import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;
import io.sloeber.buildTool.api.IBuildToolManager.ToolType;
import io.sloeber.buildTool.api.IBuildTools;

public class SloeberBuilTools implements IBuildTools {

	@Override
	public boolean holdsAllTools() {
		return true;
	}

	@Override
	public String getSelectionID() {
		return "SloeberBuilTools.id";
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
		return "SloeberBuilTools: no command";
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
