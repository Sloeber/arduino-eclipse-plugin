package io.sloeber.targetPlatform.api;

import java.util.Set;

public interface ITargetToolManager {

    public enum ToolType {
        A_TO_O, CPP_TO_O, C_TO_O, O_TO_C_DYNAMIC_LIB, O_TO_CPP_DYNAMIC_LIB, O_TO_ARCHIVE, O_TO_C_EXE, O_TO_CPP_EXE;

        @SuppressWarnings("nls")
        public static ToolType getToolType(String toolTypeName) {
            try {
                if (valueOf(toolTypeName) != null) {
                    return valueOf(toolTypeName);
                }
            } catch (@SuppressWarnings("unused") Exception e) {
                //nothing to log here
            }
            switch (toolTypeName) {
            case "a->a.o":
                return A_TO_O;
            case "cpp->cpp.o":
                return CPP_TO_O;
            case "c->c.o":
                return C_TO_O;
            case "c.o->so/dll":
                return O_TO_C_DYNAMIC_LIB;
            case "cpp.o->so/dll":
                return O_TO_CPP_DYNAMIC_LIB;
            case "o->ar":
                return O_TO_ARCHIVE;
            case "c.o->exe":
                return O_TO_C_EXE;
            case "cpp.o->exe":
                return O_TO_CPP_EXE;
            }
            return null;
        }
    }

    public enum ToolFlavour {
        GNU, CYGWIN, MINGW, MVC, MAC_OS, GCC, LLVM;

    }

    public static ITargetToolManager getDefault() {
        return io.sloeber.targetPlatform.internal.TargetToolManager.getDefault();
    }

    public String getDefaultCommand(ToolFlavour toolFlavour, ToolType toolType);

    public ITargetToolProvider getToolProvider(String toolProviderID);
    public ITargetTool getTargetTool(String toolProviderID,String ID);

    /**
     * get all installed targetTools
     * Doesn't matter which tool provider
     * 
     * 
     * @return a tool
     */
    public Set<ITargetTool> getAllInstalledTargetTools();
    
    /**
     * get a tool provider
     * Doesn't matter which tool provider
     * 
     * 
     * @return a tool
     */
    public ITargetTool getAnyInstalledTargetTool();
}