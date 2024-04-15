package io.sloeber.buildTool.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.ExtensionBuildToolProvider;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;

public class PathToolProvider extends ExtensionBuildToolProvider {
	private static boolean myHoldAllTools = false;
	private static Map<String, IBuildTools> myTargetTools = new HashMap<>();

	@Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		findTools();
	}

	private void findTools() {
		myTargetTools.clear();
		for (ToolFlavour curToolFlavour : ToolFlavour.values()) {
			IBuildTools curPathTargetTool = new PathBuildTools(curToolFlavour, this,false, null, null);
			if (curPathTargetTool.holdsAllTools()) {
				myTargetTools.put(curPathTargetTool.getSelectionID(), curPathTargetTool);
				myHoldAllTools = true;
				break;
			}
			curPathTargetTool = new PathBuildTools(curToolFlavour, this,true, "avr-", null); //$NON-NLS-1$
			if (curPathTargetTool.holdsAllTools()) {
				myTargetTools.put(curPathTargetTool.getSelectionID(), curPathTargetTool);
				myHoldAllTools = true;
				break;
			}
		}
	}

	public PathToolProvider() {

	}

	@Override
	public IBuildTools getTargetTool(String targetToolID) {
		return myTargetTools.get(targetToolID);
	}

	@Override
	public IBuildTools getAnyInstalledTargetTool() {
		for (IBuildTools curTargetTool : myTargetTools.values()) {
			return curTargetTool;
		}
		return null;
	}

	@Override
	public boolean holdsAllTools() {
		return myHoldAllTools;
	}

	@Override
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>(myTargetTools.values());
	}

	@Override
	public void refreshToolchains() {
		findTools();

	}

}
