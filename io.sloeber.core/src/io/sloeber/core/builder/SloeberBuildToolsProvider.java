package io.sloeber.core.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.buildTool.api.ExtensionBuildToolProvider;
import io.sloeber.buildTool.api.IBuildTools;

public class SloeberBuildToolsProvider extends ExtensionBuildToolProvider {
	private static Set<IBuildTools> myAllBuildTools = new HashSet<>();
	private static IBuildTools myBuildTools = null;

	@Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		if (myBuildTools == null) {
			myBuildTools = new SloeberBuildTools(getID());
			myAllBuildTools.add(myBuildTools);
		}
	}

	public SloeberBuildToolsProvider() {
	}

	@Override
	public boolean holdsAllTools() {
		return false;
	}

	@Override
	public IBuildTools getTargetTool(String targetToolID) {
		return myBuildTools;
	}

	@Override
	public IBuildTools getAnyInstalledTargetTool() {
		return myBuildTools;
	}

	@Override
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return myAllBuildTools;
	}

	@Override
	public void refreshToolchains() {
		// nothing to do
	}

}
