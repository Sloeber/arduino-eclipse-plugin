package io.sloeber.autoBuild.buildTools.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.buildTools.api.ExtensionBuildToolsProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;

public class MinGW64ToolsProvider extends ExtensionBuildToolsProvider {
	private static boolean myHoldsAllTools = false;
	private static Map<String, IBuildTools> myBuildTools = new HashMap<>();
	private static String[] mingw64Locations = { "C:\\MinGW64", "C:\\Program Files\\mingw-w64" }; //$NON-NLS-1$ //$NON-NLS-2$

	@Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		findTools();
	}

	private void findTools() {
		if (isWindows) {
			myBuildTools.clear();
			for (String curMinGWLocation : mingw64Locations) {
				Path curMinGWPath = new Path(curMinGWLocation);
				File curMinGWFile = curMinGWPath.toFile();
				if (curMinGWFile.exists() && curMinGWFile.isDirectory()) {
					// it looks like this location contains a buildTools
					for (String curChild : curMinGWFile.list()) {
						IPath curToolTargetPath = curMinGWPath.append(curChild).append(BIN_FOLDER);
						File curToolTarget = curToolTargetPath.toFile();
						if (curToolTarget.exists() && curToolTarget.isDirectory()) {
							// we have a winner
							IBuildTools newBuildTool=new MinGWBuildTools(curToolTargetPath, this, curChild);
							myBuildTools.put(curChild,newBuildTool );
							myHoldsAllTools=myHoldsAllTools||newBuildTool.holdsAllTools();

						}
					}
				}
			}
		}

	}

	@Override
	public boolean holdsAllTools() {
		return myHoldsAllTools;
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
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>( myBuildTools.values());
	}


	@Override
	public void refreshToolchains() {
		findTools();

	}


}
