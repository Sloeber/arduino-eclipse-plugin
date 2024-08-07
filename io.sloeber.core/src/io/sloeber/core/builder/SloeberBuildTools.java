package io.sloeber.core.builder;

import static  io.sloeber.core.api.Const.*;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolType;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.core.api.SloeberProject;

@SuppressWarnings("nls")
public class SloeberBuildTools implements IBuildTools {
	private String myProviderID = null;

	public SloeberBuildTools(String providerID) {
		myProviderID = providerID;
	}

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
		return myProviderID;
	}

	@Override
	public String getBuildCommand() {
		return "${MAKE_HOME}make"; //$NON-NLS-1$
	}

	@Override
	public String getPathExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDiscoveryCommand(ToolType toolType) {
		switch (toolType) {
		case CPP_TO_O:
			return RECIPE_CPP_O_CODAN;
		case C_TO_O:
			return RECIPE_C_O_CODAN;
		default:
		}
		return null;
	}

	@Override
	public boolean isProjectTypeSupported(IProjectType projectType) {
		return SloeberProject.PROJECT_ID.equals(projectType.getId());
	}

}
