package io.sloeber.core.builder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.buildTools.api.ExtensionBuildToolsProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;

public class SloeberBuildToolsProvider extends ExtensionBuildToolsProvider {
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
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return myAllBuildTools;
	}

	@Override
	public void refreshToolchains() {
		// nothing to do
	}

	@Override
	public IBuildTools getBuildTools(String buildToolsID) {
		return myBuildTools;
	}

	@Override
	public IBuildTools getAnyInstalledBuildTools() {
		return myBuildTools;
	}

}
