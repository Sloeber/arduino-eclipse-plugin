package io.sloeber.buildTool.internal;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;
import io.sloeber.buildTool.api.IBuildToolManager.ToolType;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;
import static java.io.File.pathSeparator;
import static java.nio.file.Files.isExecutable;
import static java.lang.System.getenv;
import static java.util.regex.Pattern.quote;

public class PathBuildTool implements IBuildTools {

	private String myBuildCommand=null;
    private ToolFlavour myToolFlavour = null;
    private Boolean myHoldAllTools = null;
    private String myProviderID=null;

    public PathBuildTool(ToolFlavour curToolFlavour,String providerID) {
        myToolFlavour = curToolFlavour;
        myProviderID=providerID;
    }

    @Override
    public String getSelectionID() {
        return myToolFlavour.name();
    }


	@Override
	public String getProviderID() {
		return myProviderID;
	}


    @Override
    public Map<String, String> getEnvironmentVariables() {
        // no vars
        return null;
    }

    @Override
    public Map<String, String> getToolVariables() {
        //no vars
        return null;
    }

    @Override
    public String getCommand(ToolType toolType) {
        if (holdsAllTools()) {
            IBuildToolManager toolProviderManager = IBuildToolManager.getDefault();
            return toolProviderManager.getDefaultCommand(getToolFlavour(), toolType);
        }
        return null;
    }

    @Override
    public IPath getToolLocation() {
        // As the tool is on the path null is fine
        return null;
    }

    @Override
    public ToolFlavour getToolFlavour() {
        return myToolFlavour;
    }

    @Override
    public boolean holdsAllTools() {
        if (myHoldAllTools == null) {
            IBuildToolManager toolProviderManager = IBuildToolManager.getDefault();
            Set<String> commands = new HashSet<>();
            //get all the commands removing duplicate
            for (ToolType curToolType : ToolType.values()) {
                switch (curToolType) {
                case A_TO_O:
                case CPP_TO_O:
                case C_TO_O:
                case O_TO_ARCHIVE:
                case O_TO_CPP_DYNAMIC_LIB:
                case O_TO_CPP_EXE:
                case O_TO_C_DYNAMIC_LIB:
                case O_TO_C_EXE:
                    commands.add(toolProviderManager.getDefaultCommand(getToolFlavour(), curToolType));
                    break;
                default:
                }
            }
            if (commands.remove(EMPTY_STRING)) {
                System.err.println(getClass().getName() + " found tool with empty command."); //$NON-NLS-1$
            }
            if (commands.remove(null)) {
                System.err.println(getClass().getName() + " found tool with null command."); //$NON-NLS-1$
            }
            for (String curCommand : commands) {
                if (!canExecute(curCommand)) {
                    myHoldAllTools = Boolean.FALSE;
                    return myHoldAllTools.booleanValue();
                }
            }
            myHoldAllTools = Boolean.TRUE;
        }
        return myHoldAllTools.booleanValue();
    }

    /**
     * copied from
     * https://stackoverflow.com/questions/934191/how-to-check-existence-of-a-program-in-the-path#23539220
     * and added EXTENSIONS
     *
     * @param exe
     * @return
     */
    @SuppressWarnings("nls")
    public static boolean canExecute(final String exe) {
        if (exe == null || exe.isBlank()) {
            new Throwable().printStackTrace();
            return false;
        }
        String knownExtensions[] = { "", ".exe", ".bat", ".com", ".cmd" };
        final var paths = getenv(ENV_VAR_PATH).split(quote(pathSeparator));
        return Stream.of(paths).map(Paths::get).anyMatch(path -> {
            final var p = path.resolve(exe);
            var found = false;

            for (final var extension : knownExtensions) {
                if (isExecutable(Path.of(p.toString() + extension))) {
                    found = true;
                    break;
                }
            }

            return found;
        });
    }

	@Override
	public String getBuildCommand() {
		return myBuildCommand;
	}

	@Override
	public String getPathExtension() {
		return null;
	}

	@Override
	public String getDiscoveryCommand(ToolType toolType) {
		return getToolLocation().append( getCommand(toolType)).toString() + DISCOVERY_PARAMETERS;
	}


}
