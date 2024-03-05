package io.sloeber.buildTool.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.core.Activator;
import io.sloeber.buildTool.api.ExtensionBuildToolProvider;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.schema.api.IProjectType;

public class UserDefinedBuildToolProvider extends ExtensionBuildToolProvider {
	private static IToolChainManager manager = Activator.getService(IToolChainManager.class);
	private static Map<String, CDTBuildTool> myCDTBuildTools = new HashMap<>();
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
				CDTBuildTool cdtBuildTools = new CDTBuildTool(curBuildTools);
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
	public IBuildTools getTargetTool(String targetToolID) {
		return myCDTBuildTools.get(targetToolID);
	}


	@Override
	public IBuildTools getAnyInstalledTargetTool() {
		if (myHoldsAllTool) {
			for (CDTBuildTool curBuildTools : myCDTBuildTools.values()) {
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
