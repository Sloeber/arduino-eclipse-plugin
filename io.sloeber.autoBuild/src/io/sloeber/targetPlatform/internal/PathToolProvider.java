package io.sloeber.targetPlatform.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.util.stream.Stream;

import io.sloeber.targetPlatform.api.ITargetTool;
import io.sloeber.targetPlatform.api.ITargetToolManager;
import io.sloeber.targetPlatform.api.ITargetToolProvider;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolType;

import static java.io.File.pathSeparator;
import static java.nio.file.Files.isExecutable;
import static java.lang.System.getenv;
import static java.util.regex.Pattern.quote;

public class PathToolProvider implements ITargetToolProvider {

    private static ToolFlavour myToolFlavour = null;
    private static Boolean myHoldAllTools = false;
    private static Map<String,ITargetTool> myTargetTools=new HashMap<>();
    private static String myID="PathToolProvider";

    static {
    	for (ToolFlavour curToolFlavour : ToolFlavour.values()) {
            ITargetTool curPathTargetTool = new PathTargetTool(curToolFlavour,myID);
            if (curPathTargetTool.holdsAllTools()) {
            	myTargetTools.put(curPathTargetTool.getSelectionID(), curPathTargetTool);
            	myToolFlavour=curPathTargetTool.getToolFlavour();
            	myHoldAllTools=true;
            	break;
            } 
        }
    }
    public PathToolProvider() {
    }
    
    @Override
    public String getID() {
        return myID ;
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
	public ITargetTool getAnyInstalledTargetTool() {
		for(ITargetTool curTargetTool:myTargetTools.values()) {
			return curTargetTool;
		}
		return null;
	}

    @Override
    public ToolFlavour getToolFlavour() {
        return myToolFlavour;
    }

    @Override
    public boolean holdsAllTools() {
        if (myHoldAllTools == null) {
            ITargetToolManager toolProviderManager = ITargetToolManager.getDefault();
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
        final var paths = getenv("PATH").split(quote(pathSeparator));
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
	public Set<ITargetTool> getAllInstalledTargetTools() {
			return new HashSet<>( myTargetTools.values());
		}




}
