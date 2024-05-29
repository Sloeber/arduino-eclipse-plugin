package io.sloeber.autoBuild.buildTools.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolType;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.autoBuild.schema.api.IProjectType;

public class PathBuildTools implements IBuildTools {

	private String myBuildCommand = null;
	private ToolFlavour myToolFlavour = null;
	private Boolean myHoldAllTools = null;
	private IBuildToolsProvider myToolProvider=null;
	private String myPrefix = null;
	private String mySuffix = null;
	private boolean myIsEmbedded=false;

	public PathBuildTools(ToolFlavour curToolFlavour, IBuildToolsProvider toolProvider,boolean isEmbedded, String prefix, String suffix) {
		myToolFlavour = curToolFlavour;
		myToolProvider = toolProvider;
		myIsEmbedded=isEmbedded;
		myPrefix=prefix==null?EMPTY_STRING:prefix;
		mySuffix=suffix==null?EMPTY_STRING:suffix;
	}

	@Override
	public String getSelectionID() {
		return myToolFlavour.name();
	}

	@Override
	public String getProviderID() {
		return myToolProvider.getID();
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		if (myPrefix == null & mySuffix == null) {
			return null;
		}
		Map<String, String> ret = new HashMap<>();
		if (myPrefix != null) {
			ret.put(TOOL_PREFIX, myPrefix);
		}
		if (mySuffix != null) {
			ret.put(TOOL_SUFFIX, mySuffix);
		}
		return ret;
	}

	@Override
	public Map<String, String> getToolVariables() {
		// no vars
		return null;
	}

	@Override
	public String getCommand(ToolType toolType) {
		IBuildToolsManager toolProviderManager = IBuildToolsManager.getDefault();
		return myPrefix + toolProviderManager.getDefaultCommand(getToolFlavour(), toolType) + mySuffix;
	}

	@Override
	public IPath getToolLocation() {
		// As the tool is on the path null is fine
		return null;
	}

	@Override
	public ToolFlavour getToolFlavour() {
		return myToolFlavour;
	}

	@Override
	public boolean holdsAllTools( ) {
		if (myHoldAllTools == null) {
			Set<String> commands = new HashSet<>();
			// get all the commands removing duplicate
			for (ToolType curToolType : ToolType.values()) {
				switch (curToolType) {
				case A_TO_O:
				case CPP_TO_O:
				case C_TO_O:
				case O_TO_ARCHIVE:
				case O_TO_CPP_DYNAMIC_LIB:
				case O_TO_CPP_EXE:
				case O_TO_C_DYNAMIC_LIB:
				case O_TO_C_EXE:
					commands.add(getCommand( curToolType));
					break;
				default:
				}
			}
			if (commands.remove(EMPTY_STRING)) {
				System.err.println(getClass().getName() + " found tool with empty command."); //$NON-NLS-1$
			}
			if (commands.remove(null)) {
				System.err.println(getClass().getName() + " found tool with null command."); //$NON-NLS-1$
			}

			for (String curCommand : commands) {
				if (!AutoBuildCommon.canExecute(curCommand)) {
					myHoldAllTools = Boolean.FALSE;
					return myHoldAllTools.booleanValue();
				}
			}
			myHoldAllTools = Boolean.TRUE;
		}
		return myHoldAllTools.booleanValue();
	}

	@Override
	public String getBuildCommand() {
		return myBuildCommand;
	}

	@Override
	public String getPathExtension() {
		return null;
	}

	@Override
	public String getDiscoveryCommand(ToolType toolType) {
		return getCommand(toolType).toString() + DISCOVERY_PARAMETERS;
	}

	@Override
	public boolean isProjectTypeSupported(IProjectType projectType) {
		if (!myIsEmbedded) {
			return myToolProvider.supports(projectType);
		}
		switch (projectType.getId()) {
		case PROJECT_TYPE_ID_DYNAMIC_LIB:
		case PROJECT_TYPE_ID_COMPOUND_EXE:
			return false;
		default:
			return true;
		}
	}

}
