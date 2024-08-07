package io.sloeber.autoBuild.buildTools.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.buildTools.api.ExtensionBuildToolsProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;

public class PathToolsProvider extends ExtensionBuildToolsProvider {
	private static boolean myHoldAllTools = false;
	private static Map<String, IBuildTools> myBuildTools = new HashMap<>();

	@Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		findTools();
	}

	private void findTools() {
		myBuildTools.clear();
		for (ToolFlavour curToolFlavour : ToolFlavour.values()) {
			IBuildTools curPathBuildTools = new PathBuildTools(curToolFlavour, this,false, null, null);
			if (curPathBuildTools.holdsAllTools()) {
				myBuildTools.put(curPathBuildTools.getSelectionID(), curPathBuildTools);
				myHoldAllTools = true;
				break;
			}
			curPathBuildTools = new PathBuildTools(curToolFlavour, this,true, "avr-", null); //$NON-NLS-1$
			if (curPathBuildTools.holdsAllTools()) {
				myBuildTools.put(curPathBuildTools.getSelectionID(), curPathBuildTools);
				myHoldAllTools = true;
				break;
			}
		}
	}

	public PathToolsProvider() {

	}

	@Override
	public IBuildTools getBuildTools(String buildToolID) {
		return myBuildTools.get(buildToolID);
	}

	@Override
	public IBuildTools getAnyInstalledBuildTools() {
		for (IBuildTools curBuildTool : myBuildTools.values()) {
			return curBuildTool;
		}
		return null;
	}

	@Override
	public boolean holdsAllTools() {
		return myHoldAllTools;
	}

	@Override
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>(myBuildTools.values());
	}

	@Override
	public void refreshToolchains() {
		findTools();

	}

}
