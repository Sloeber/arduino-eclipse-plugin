package io.sloeber.buildTool.internal;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.nio.file.Paths;
import java.util.stream.Stream;

import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.buildTool.api.IBuildToolProvider;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;
import static java.io.File.pathSeparator;
import static java.nio.file.Files.isExecutable;
import static java.lang.System.getenv;
import static java.util.regex.Pattern.quote;

public class PathToolProvider implements IBuildToolProvider {

    private static boolean myHoldAllTools = false;
    private static Map<String,IBuildTools> myTargetTools=new HashMap<>();
    private static String myID="PathToolProvider";
    private static String NAME="Tool on the path";
    private static String DESCRIPTION="The Tools on the path";

    static {
    	findTools();
    }
    private static void findTools() {
    	myTargetTools.clear();
    	for (ToolFlavour curToolFlavour : ToolFlavour.values()) {
            IBuildTools curPathTargetTool = new PathBuildTool(curToolFlavour,myID);
            if (curPathTargetTool.holdsAllTools()) {
            	myTargetTools.put(curPathTargetTool.getSelectionID(), curPathTargetTool);
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
	public IBuildTools getTargetTool(String targetToolID) {
		return myTargetTools.get(targetToolID);
	}

	@Override
	public IBuildTools getAnyInstalledTargetTool() {
		for(IBuildTools curTargetTool:myTargetTools.values()) {
			return curTargetTool;
		}
		return null;
	}


    @Override
    public boolean holdsAllTools() {
        return myHoldAllTools;
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
	public Set<IBuildTools> getAllInstalledBuildTools() {
			return new HashSet<>( myTargetTools.values());
		}

	@Override
	public String getName() {
		return NAME;
	}
	@Override
	public void refreshToolchains() {
		findTools();
		
	}
	@Override
	public boolean supports(IProjectType projectType) {
		// We assume everything goes
		return true;
	}
	@Override
	public String getDescription() {
		return DESCRIPTION;
	}
	@Override
	public boolean isTest() {
		return false;
	}




}
