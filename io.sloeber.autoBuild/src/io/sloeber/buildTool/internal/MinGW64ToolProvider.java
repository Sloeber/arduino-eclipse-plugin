package io.sloeber.buildTool.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.ExtensionBuildToolProvider;

public class MinGW64ToolProvider extends ExtensionBuildToolProvider {
	private static boolean myHoldsAllTools = false;
	private static Map<String, IBuildTools> myTargetTools = new HashMap<>();
	private static String[] mingw64Locations = { "C:\\MinGW64", "C:\\Program Files\\mingw-w64" }; //$NON-NLS-1$ //$NON-NLS-2$

	@Override
	public void initialize(IConfigurationElement element) {
		super.initialize(element);
		findTools();
	}

	private void findTools() {
		if (isWindows) {
			myTargetTools.clear();
			for (String curMinGWLocation : mingw64Locations) {
				Path curMinGWPath = new Path(curMinGWLocation);
				File curMinGWFile = curMinGWPath.toFile();
				if (curMinGWFile.exists() && curMinGWFile.isDirectory()) {
					// it looks like this location contains some targetTools
					for (String curChild : curMinGWFile.list()) {
						IPath curToolTargetPath = curMinGWPath.append(curChild).append(BIN_FOLDER);
						File curToolTarget = curToolTargetPath.toFile();
						if (curToolTarget.exists() && curToolTarget.isDirectory()) {
							// we have a winner
							IBuildTools newTargetTool=new MinGWTargetTool(curToolTargetPath, getID(), curChild);
							myTargetTools.put(curChild,newTargetTool );
							myHoldsAllTools=myHoldsAllTools||newTargetTool.holdsAllTools();
									
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
	public Set<IBuildTools> getAllInstalledBuildTools() {
		return new HashSet<>( myTargetTools.values());
	}


	@Override
	public void refreshToolchains() {
		findTools();
		
	}


}
