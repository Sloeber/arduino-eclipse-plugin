package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.io.File.pathSeparator;
import static java.nio.file.Files.isExecutable;
import static java.lang.System.getenv;
import static java.util.regex.Pattern.quote;

import io.sloeber.autoBuild.api.IToolProvider;
import io.sloeber.autoBuild.api.ITargetToolManager;
import io.sloeber.autoBuild.api.ITargetToolManager.ToolFlavour;
import io.sloeber.autoBuild.api.ITargetToolManager.ToolType;

public class PathToolProvider implements IToolProvider {

    private ToolFlavour myToolFlavour = null;
    private Boolean myHoldAllTools = null;

    public PathToolProvider(ToolFlavour curToolFlavour) {
        myToolFlavour = curToolFlavour;
    }

    @Override
    public String getSelectionID() {
        return myToolFlavour.name();
    }

    //    @Override
    //    public String setSelectionID(String ID) {
    //        // TODO Auto-generated method stub
    //        return null;
    //    }

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
            ITargetToolManager toolProviderManager = ITargetToolManager.getDefault();
            return toolProviderManager.getDefaultCommand(getToolFlavour(), toolType);
        }
        return null;
    }

    @Override
    public Path getToolLocation(ToolType toolType) {
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
    public String getID() {
        return getClass().getName() + SEMICOLON + myToolFlavour.name();
    }

}
