package io.sloeber.autoBuild.buildTools.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.buildTools.api.ExtensionBuildToolsProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.schema.api.IProjectType;

public class UserDefinedBuildToolsProvider extends ExtensionBuildToolsProvider {
	private static IToolChainManager manager = Activator.getService(IToolChainManager.class);
	private static Map<String, CDTBuildTools> myCDTBuildTools = new HashMap<>();
	private static boolean myHoldsAllTool = false;
	{
		getToolchains();
	}

	private static void getToolchains() {
		try {
			myCDTBuildTools.clear();
			Collection<IToolChain> allToolchains = manager.getAllToolChains();
			myHoldsAllTool = allToolchains.size() > 0;
			for (IToolChain curBuildTools : allToolchains) {
				CDTBuildTools cdtBuildTools = new CDTBuildTools(curBuildTools);
				myCDTBuildTools.put(curBuildTools.getId(), cdtBuildTools);
			}

		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean holdsAllTools() {
		return myHoldsAllTool;
	}

	@Override
	public IBuildTools getBuildTools(String buildToolsID) {
		return myCDTBuildTools.get(buildToolsID);
	}


	@Override
	public IBuildTools getAnyInstalledBuildTools() {
		if (myHoldsAllTool) {
			for (CDTBuildTools curBuildTools : myCDTBuildTools.values()) {
				return curBuildTools;
			}
		}
		return null;
	}

	@Override
	public boolean supports(IProjectType projectType) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>(myCDTBuildTools.values());
	}

	@Override
	public void refreshToolchains() {
		getToolchains();
	}
}
