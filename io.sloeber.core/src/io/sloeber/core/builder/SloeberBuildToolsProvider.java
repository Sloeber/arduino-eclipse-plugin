package io.sloeber.core.builder;

import java.util.HashSet;
import java.util.Set;

import io.sloeber.buildTool.api.IBuildToolProvider;
import io.sloeber.buildTool.api.IBuildTools;

public class SloeberBuildToolsProvider implements IBuildToolProvider {
	public static String SLOEBER_BUILD_TOOL_PROVIDER_ID = "io.sloeber.core.arduino.ToolProvider"; //$NON-NLS-1$
private static Set<IBuildTools> myAllBuildTools=new HashSet<>();
private static IBuildTools myBuildTools;
{
	myBuildTools=new SloeberBuildTools();
	myAllBuildTools.add(myBuildTools);
}
	public SloeberBuildToolsProvider() {
	}

	@Override
	public boolean holdsAllTools() {
		return true;
	}

	@Override
	public IBuildTools getTargetTool(String targetToolID) {
		return myBuildTools;
	}

	@Override
	public String getID() {
		return SLOEBER_BUILD_TOOL_PROVIDER_ID;
	}

	@Override
	public String getName() {
		return "SloeberBuildToolsProvider";
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
		//nothing to do
	}

}
