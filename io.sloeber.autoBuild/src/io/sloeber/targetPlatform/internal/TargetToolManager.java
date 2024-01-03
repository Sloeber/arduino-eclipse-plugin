package io.sloeber.targetPlatform.internal;

import static io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour.*;
import static io.sloeber.targetPlatform.api.ITargetToolManager.ToolType.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.targetPlatform.api.ITargetTool;
import io.sloeber.targetPlatform.api.ITargetToolManager;
import io.sloeber.targetPlatform.api.ITargetToolProvider;

@SuppressWarnings("nls")
public class TargetToolManager implements ITargetToolManager {
	private static TargetToolManager theToolProviderManager = new TargetToolManager();
	private static Map<String, ITargetToolProvider> pathToolHoldingProvider = new HashMap<>();
	private static Map<String, ITargetToolProvider> pathToolDeprivedProvider = new HashMap<>();
	private static Map<String, ITargetToolProvider> toolHoldingProviders = new HashMap<>();
	private static Map<String, ITargetToolProvider> toolDeprivedProviders = new HashMap<>();
	private static EnumMap<ToolFlavour, EnumMap<ToolType, String>> defaultCommands = new EnumMap<>(ToolFlavour.class);
	static {
		// Fill the map with the default tool commands as they will be used to
		// list the toolProviders in the ones that have tools and the ones that do not
		// and they may need this list to test

		EnumMap<ToolType, String> gnuCommmands = new EnumMap<>(ToolType.class);
		gnuCommmands.put(A_TO_O, "as");
		gnuCommmands.put(CPP_TO_O, "g++");
		gnuCommmands.put(C_TO_O, "gcc");
		gnuCommmands.put(O_TO_C_DYNAMIC_LIB, "gcc");
		gnuCommmands.put(O_TO_CPP_DYNAMIC_LIB, "g++");
		gnuCommmands.put(O_TO_ARCHIVE, "ar");
		gnuCommmands.put(O_TO_C_EXE, "gcc");
		gnuCommmands.put(O_TO_CPP_EXE, "g++");
		defaultCommands.put(GNU, gnuCommmands);

		defaultCommands.put(MINGW, gnuCommmands);
		// TOFIX this table needs to be properly initialized
		// both below and above is not good
		defaultCommands.put(CYGWIN, gnuCommmands);
		defaultCommands.put(MVC, gnuCommmands);
		defaultCommands.put(MAC_OS, gnuCommmands);
		defaultCommands.put(GCC, gnuCommmands);
		defaultCommands.put(LLVM, gnuCommmands);

		// Get the plugin.xml provided tool providers
		loadToolProviders();

	}

	@Override
	public String getDefaultCommand(ToolFlavour toolFlavour, ToolType toolType) {

		EnumMap<ToolType, String> toolComands = defaultCommands.get(toolFlavour);
		if (toolComands == null) {
			return null;
		}
		return toolComands.get(toolType);
	}

	@Override
	public ITargetToolProvider getToolProvider(String toolProviderID) {
		if (pathToolHoldingProvider.get(toolProviderID) != null) {
			return pathToolHoldingProvider.get(toolProviderID);
		}
		if (toolHoldingProviders.get(toolProviderID) != null) {
			return toolHoldingProviders.get(toolProviderID);
		}
		if (toolDeprivedProviders.get(toolProviderID) != null) {
			return toolDeprivedProviders.get(toolProviderID);
		}
		if (pathToolDeprivedProvider.get(toolProviderID) != null) {
			return pathToolDeprivedProvider.get(toolProviderID);
		}
		return null;
	}

	@Override
	public ITargetTool getTargetTool(String toolProviderID, String ID) {
		ITargetToolProvider toolProvider = getToolProvider(toolProviderID);
		if (toolProvider == null) {
			return null;
		}
		return toolProvider.getTargetTool(ID);
	}

	public static ITargetToolManager getDefault() {
		return theToolProviderManager;
	}

	@Override
	public ITargetTool getAnyInstalledTargetTool() {
		for (ITargetToolProvider cur : pathToolHoldingProvider.values()) {
			return cur.getAnyInstalledTargetTool();
		}
		for (ITargetToolProvider cur : toolHoldingProviders.values()) {
			return cur.getAnyInstalledTargetTool();
		}
		return null;
	}

	private static void loadToolProviders() {
		try {
			for (String extensionPointID : AutoBuildManager.supportedExtensionPointIDs()) {
				// Get the extensions that use the current CDT managed build model
				IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
				if (extensionPoint == null) {
					continue;
				}
				for (IExtension extension : extensionPoint.getExtensions()) {
					for (IConfigurationElement curElement : extension.getConfigurationElements()) {
						try {
							if ("ToolProvider".equals(curElement.getName())) {
								ITargetToolProvider toolprovider = (ITargetToolProvider) curElement
										.createExecutableExtension("toolProvider");
								if (toolprovider.holdsAllTools()) {
									toolHoldingProviders.put(toolprovider.getID(), toolprovider);
								} else {
									toolDeprivedProviders.put(toolprovider.getID(), toolprovider);
								}
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}
			}
		} catch (Exception e) {
			Activator.log(e);
		}
	}

	@Override
	public Set<ITargetTool> getAllInstalledTargetTools() {
		 Set<ITargetTool> ret=new HashSet<>();
		for (ITargetToolProvider cur : pathToolHoldingProvider.values()) {
			ret.addAll( cur.getAllInstalledTargetTools());
		}
		for (ITargetToolProvider cur : toolHoldingProviders.values()) {
			ret.addAll( cur.getAllInstalledTargetTools());
		}
		return ret;
	}

}
