package io.sloeber.buildTool.api;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.util.Set;

import io.sloeber.schema.api.IProjectType;

public interface IBuildToolManager {

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
			default:
				break;
            }
            return null;
        }

		public boolean isForLanguage(String languageId) {
			if(LANGUAGEID_ASSEMBLY.equals(languageId)) {
				switch(this) {
				case A_TO_O:
					return true;
					default:
						return false;
				}
			}
			if(LANGUAGEID_C.equals(languageId)) {
				switch(this) {
				case C_TO_O:case O_TO_C_DYNAMIC_LIB:case O_TO_C_EXE:
					return true;
					default:
						return false;
				}
			}
			if(LANGUAGEID_CPP.equals(languageId)) {
				switch(this) {
				case CPP_TO_O:case O_TO_CPP_DYNAMIC_LIB:case O_TO_CPP_EXE:case O_TO_ARCHIVE:
					return true;
					default:
						return false;
				}
			}
			return false;
		}
    }

    public enum ToolFlavour {
        GNU, CYGWIN, MINGW, MVC, MAC_OS, GCC, LLVM;

    }

    /**
     * There should only be one BuildToolManger.
     * Use this static method to get the BuildToolManager
     *
     * @return
     */
    public static IBuildToolManager getDefault() {
        return io.sloeber.buildTool.internal.BuildToolManager.getDefault();
    }

    public String getDefaultCommand(ToolFlavour toolFlavour, ToolType toolType);

    /**
     * Get the tool provider associated with the provided tool provider ID
     * @param toolProviderID
     * @return The tool Provider associated with the tool provider ID or null if no such provider is found
     */
    public IBuildToolProvider getToolProvider(String toolProviderID);
    /**
     * Given the tool provider and the build tool ID return the build tools
     * @param toolProviderID
     * @param buildToolsID
     * @return the buildTools or null if not found
     */
    public IBuildTools getBuildTools(String toolProviderID,String buildToolsID);

    /**
     * get all installed targetTools
     * Doesn't matter which tool provider
     *
     *
     * @return a tool
     */
    public Set<IBuildTools> getAllInstalledTargetTools();

    /**
     * get a build tools that is compatible with the given
     * projectType
     * Doesn't matter which tool provider
     *
     *
     * @return a build tools
     */
    public IBuildTools getAnyInstalledBuildTools(IProjectType projectType);

    /**
     * Get all the build tool providers known to the
     * Build tool Manager
     *
     * @param onlyHoldsTools if true only return the providers that have actually
     * 						installed tools
     * 						if false return all known tool providers
     *
     * @return a set of IBuildToolProvider
     */
	public Set<IBuildToolProvider> GetToolProviders(boolean onlyHoldsTools);

	/**
	 * refresh the toolchains known.
	 * The manager caches the toolchains.
	 * When this method is called the cache needs to be refreshed.
	 *
	 */
	public void refreshToolchains();


	public IBuildToolProvider GetToolProviderByName(String toolProviderName);
}
