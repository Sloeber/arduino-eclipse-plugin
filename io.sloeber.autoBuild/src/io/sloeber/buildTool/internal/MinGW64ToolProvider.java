package io.sloeber.buildTool.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolProvider;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;

public class MinGW64ToolProvider implements IBuildToolProvider {
	final private static String MINGW_ID = "Mingw64"; //$NON-NLS-1$
	final private static String MINGW_NAME = "Mingw 64 bit tools"; //$NON-NLS-1$
	private static boolean myHoldsAllTools = false;
	private static Map<String, IBuildTools> myTargetTools = new HashMap<>();
	private static String[] mingw64Locations = { "C:\\MinGW64", "C:\\Program Files\\mingw-w64" }; //$NON-NLS-1$ //$NON-NLS-2$
	static {
		if (isWindows) {
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
							IBuildTools newTargetTool=new MinGWTargetTool(curToolTargetPath, MINGW_ID, curChild);
							myTargetTools.put(curChild,newTargetTool );
							myHoldsAllTools=myHoldsAllTools||newTargetTool.holdsAllTools();
									
						}
					}
				}
			}
		}
	}

	public MinGW64ToolProvider() {
		// nothing to be done here
	}

	@Override
	public boolean holdsAllTools() {
		return myHoldsAllTools;
	}

	@Override
	public String getID() {
		return MINGW_ID;
	}

	@Override
	public Set<String> getTargetToolIDs() {
		return myTargetTools.keySet();
	}

	@Override
	public IBuildTools getTargetTool(String targetToolID) {
		return myTargetTools.get(targetToolID);
	}

	@Override
	public ToolFlavour getToolFlavour() {
		return ToolFlavour.MINGW;
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
	public String getName() {
		return MINGW_NAME;
	}

}
