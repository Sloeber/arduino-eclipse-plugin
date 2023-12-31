package io.sloeber.autoBuild.Internal;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import io.sloeber.autoBuild.api.IToolProvider;
import io.sloeber.autoBuild.api.ITargetToolManager;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import static io.sloeber.autoBuild.api.ITargetToolManager.ToolFlavour.*;
import static io.sloeber.autoBuild.api.ITargetToolManager.ToolType.*;

@SuppressWarnings("nls")
public class TargetToolManager implements ITargetToolManager {
    private static TargetToolManager theToolProviderManager = new TargetToolManager();
    private static Map<String, IToolProvider> pathToolHoldingProvider = new HashMap<>();
    private static Map<String, IToolProvider> pathToolDeprivedProvider = new HashMap<>();
    private static Map<String, IToolProvider> toolHoldingProviders = new HashMap<>();
    private static Map<String, IToolProvider> toolDeprivedProviders = new HashMap<>();
    private static EnumMap<ToolFlavour, EnumMap<ToolType, String>> defaultCommands = new EnumMap<>(ToolFlavour.class);
    static {
        //Fill the map with the default tool commands as they will be used to
        //list the toolProviders in the ones that have tools and the ones that do not
        //and they may need this list to test

        EnumMap<ToolType, String> gnuCommmands = new EnumMap<>(ToolType.class);
        gnuCommmands.put(A_TO_O, "A_TO_O");
        gnuCommmands.put(CPP_TO_O, "CPP_TO_O");
        gnuCommmands.put(C_TO_O, "C_TO_O");
        gnuCommmands.put(O_TO_C_DYNAMIC_LIB, "O_TO_C_DYNAMIC_LIB");
        gnuCommmands.put(O_TO_CPP_DYNAMIC_LIB, "O_TO_CPP_DYNAMIC_LIB");
        gnuCommmands.put(O_TO_ARCHIVE, "O_TO_ARCHIVE");
        gnuCommmands.put(O_TO_C_EXE, "O_TO_C_EXE");
        gnuCommmands.put(O_TO_CPP_EXE, "O_TO_CPP_EXE");
        defaultCommands.put(GNU, gnuCommmands);
        EnumMap<ToolType, String> mingwCommands = new EnumMap<>(ToolType.class);
        mingwCommands.put(A_TO_O, "A_TO_O");
        mingwCommands.put(CPP_TO_O, "g++");
        mingwCommands.put(C_TO_O, "gcc");
        mingwCommands.put(O_TO_C_DYNAMIC_LIB, "O_TO_C_DYNAMIC_LIB");
        mingwCommands.put(O_TO_CPP_DYNAMIC_LIB, "O_TO_CPP_DYNAMIC_LIB");
        mingwCommands.put(O_TO_ARCHIVE, "ar");
        mingwCommands.put(O_TO_C_EXE, "gcc");
        mingwCommands.put(O_TO_CPP_EXE, "g++");
        defaultCommands.put(MINGW, mingwCommands);
        //TOFIX this table needs to be properly initialized
        //both below and above is not good
        defaultCommands.put(CYGWIN, gnuCommmands);
        defaultCommands.put(MVC, gnuCommmands);
        defaultCommands.put(MAC_OS, gnuCommmands);
        defaultCommands.put(GCC, gnuCommmands);
        defaultCommands.put(LLVM, gnuCommmands);

        //Get the plugin.xml provided tool providers
        loadToolProviders();

        if (toolHoldingProviders.size() == 0)
            for (ToolFlavour curToolFlavour : ToolFlavour.values()) {
                IToolProvider curPathToolProvider = new PathToolProvider(curToolFlavour);
                if (curPathToolProvider.holdsAllTools()) {
                    pathToolHoldingProvider.put(curPathToolProvider.getID(), curPathToolProvider);
                } else {
                    pathToolDeprivedProvider.put(curPathToolProvider.getID(), curPathToolProvider);
                }
            }
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
    public IToolProvider getToolProvider(String toolProviderID) {
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

    public static ITargetToolManager getDefault() {
        return theToolProviderManager;
    }

    @Override
    public IToolProvider getAnyInstalledToolProvider() {
        for (IToolProvider cur : pathToolHoldingProvider.values()) {
            return cur;
        }
        for (IToolProvider cur : toolHoldingProviders.values()) {
            return cur;
        }
        //        for (IToolProvider cur : toolDeprivedProviders.values()) {
        //            return cur;
        //        }
        //        for (IToolProvider cur : pathToolDeprivedProvider.values()) {
        //            return cur;
        //        }
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
                                IToolProvider toolprovider = (IToolProvider) curElement
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
}
