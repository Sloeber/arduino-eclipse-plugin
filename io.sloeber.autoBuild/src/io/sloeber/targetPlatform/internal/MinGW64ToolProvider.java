package io.sloeber.targetPlatform.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.targetPlatform.api.ITargetTool;
import io.sloeber.targetPlatform.api.ITargetToolProvider;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour;

public class MinGW64ToolProvider implements ITargetToolProvider {
	final private static String MINGW_ID = "Mingw64"; //$NON-NLS-1$
	private static boolean myHoldsAllTools = false;
	private static Map<String, ITargetTool> myTargetTools = new HashMap<>();
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
							ITargetTool newTargetTool=new MinGWTargetTool(curToolTargetPath, MINGW_ID, curChild);
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
	public ITargetTool getTargetTool(String targetToolID) {
		return myTargetTools.get(targetToolID);
	}

	@Override
	public ToolFlavour getToolFlavour() {
		return ToolFlavour.MINGW;
	}

	@Override
	public ITargetTool getAnyInstalledTargetTool() {
		for (ITargetTool curTargetTool : myTargetTools.values()) {
			return curTargetTool;
		}
		return null;
	}

	@Override
	public Set<ITargetTool> getAllInstalledTargetTools() {
		return new HashSet<>( myTargetTools.values());
	}

}
