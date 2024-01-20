package io.sloeber.buildTool.internal;

import static io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour.*;
import static io.sloeber.buildTool.api.IBuildToolManager.ToolType.*;

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
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildToolProvider;

@SuppressWarnings("nls")
public class BuildToolManager implements IBuildToolManager {
	private static BuildToolManager theToolProviderManager = new BuildToolManager();
	private static Map<String, IBuildToolProvider> toolHoldingProviders = new HashMap<>();
	private static Map<String, IBuildToolProvider> toolDeprivedProviders = new HashMap<>();
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
		loadToolProviders(false);

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
	public IBuildToolProvider getToolProvider(String toolProviderID) {
		if (toolHoldingProviders.get(toolProviderID) != null) {
			return toolHoldingProviders.get(toolProviderID);
		}
		if (toolDeprivedProviders.get(toolProviderID) != null) {
			return toolDeprivedProviders.get(toolProviderID);
		}
		return null;
	}
	
	@Override
	public IBuildToolProvider GetToolProviderByName(String toolProviderName) {
		for (IBuildToolProvider curValue:toolHoldingProviders.values()) {
			if( toolProviderName.equals(curValue.getName())) {
				return curValue;
			}
		}
		for (IBuildToolProvider curValue:toolDeprivedProviders.values()) {
			if( toolProviderName.equals(curValue.getName())) {
				return curValue;
			}
		}
		return null;
	}
	
	@Override
	public Set<IBuildToolProvider> GetToolProviders(boolean onlyHoldsTools) {
		Set<IBuildToolProvider>ret=new HashSet<>();
		ret.addAll(toolHoldingProviders.values());
		if(onlyHoldsTools) {
			return ret;
		}
		ret.addAll(toolDeprivedProviders.values());
		
		return ret;
	}

	@Override
	public IBuildTools getBuildTools(String toolProviderID, String ID) {
		IBuildToolProvider toolProvider = getToolProvider(toolProviderID);
		if (toolProvider == null) {
			return null;
		}
		return toolProvider.getTargetTool(ID);
	}

	public static IBuildToolManager getDefault() {
		return theToolProviderManager;
	}

	@Override
	public IBuildTools getAnyInstalledTargetTool() {
		for (IBuildToolProvider cur : toolHoldingProviders.values()) {
			return cur.getAnyInstalledTargetTool();
		}
		return null;
	}

	private static void loadToolProviders(boolean refresh) {
		try {
			toolHoldingProviders.clear();
			toolDeprivedProviders.clear();
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
								IBuildToolProvider toolprovider = (IBuildToolProvider) curElement
										.createExecutableExtension("toolProvider");
								if(refresh) {
									toolprovider.refreshToolchains();
								}
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
	public Set<IBuildTools> getAllInstalledTargetTools() {
		 Set<IBuildTools> ret=new HashSet<>();
		for (IBuildToolProvider cur : toolHoldingProviders.values()) {
			ret.addAll( cur.getAllInstalledBuildTools());
		}
		return ret;
	}

	@Override
	public void refreshToolchains() {
		loadToolProviders(true);
	}





}
