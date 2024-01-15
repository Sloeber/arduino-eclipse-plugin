package io.sloeber.autoBuild.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.autoBuild.extensionPoint.providers.InternalBuildRunner;
import io.sloeber.autoBuild.integration.AutoBuildProjectGenerator;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolProvider;

public class AutoBuildProject {
	public static final String COMMON_BUILDER_ID="io.sloeber.autoBuild.AutoMakeBuilder"; //$NON-NLS-1$
    public static final String MAKE_BUILDER_ID = "io.sloeber.autoBuild.make.builder"; //$NON-NLS-1$
    public static final String INTERNAL_BUILDER_ID = "io.sloeber.autoBuild.internal.builder"; //$NON-NLS-1$
    public static final String ARGS_BUILDER_KEY = "The key to specify the value is a builder key"; //$NON-NLS-1$
    public static final String ARGS_TARGET_KEY = "The key to specify the value is the target to build"; //$NON-NLS-1$
    public static final String ARGS_CONFIGS_KEY = "The names of the configurations to build"; //$NON-NLS-1$

    public static IProject createProject(String projectName, String extensionPointID, String extensionID,
            String projectTypeID, String natureID, String codeRootFolder,ICodeProvider codeProvider, IBuildTools targetTool,
            boolean needsMoreWork, IProgressMonitor monitor) {
        return createProject(projectName, extensionPointID, extensionID, projectTypeID, null, natureID,codeRootFolder, codeProvider,
        		targetTool, needsMoreWork, monitor);
    }

    /**
     * 
     * @param projectName
     *            The name of the project
     * @param extensionPointID
     *            The ID of the extension point that describes the project to be
     *            created
     * @param extensionID
     *            The ID of the extension defined by the extensionpoindID
     * @param projectTypeID
     *            the projectTypeID of type extension ID
     * @param builderName
     *            The name of the builder to use (null is default)
     * @param natureID
     *            use CCProjectNature.CC_NATURE_ID for C++ project; all other values
     *            are currently ignored
     * @param codeProvider
     *            a provider that gives the code to add to the project
     * @param needsMoreWork
     *            if true the projectDescription will be marked as created
     *            if false you will need to call setCdtProjectCreated and
     *            setProjectDescription
     * @param monitor
     * 
     * @return the created project
     */
    public static IProject createProject(String projectName, String extensionPointID, String extensionID,
            String projectTypeID, String builderName, String natureID,String codeRootFolder, ICodeProvider codeProvider,
            IBuildTools targetTool, boolean needsMoreWork, IProgressMonitor monitor) {
        AutoBuildProjectGenerator theGenerator = new AutoBuildProjectGenerator();
        try {
            IProgressMonitor internalMonitor = monitor;
            if (internalMonitor == null) {
                internalMonitor = new NullProgressMonitor();
            }
            theGenerator.setCodeRootFolder(codeRootFolder);
            theGenerator.setTargetTool(targetTool);
            theGenerator.setExtentionPointID(extensionPointID);
            theGenerator.setExtentionID(extensionID);
            theGenerator.setProjectTypeID(projectTypeID);
            theGenerator.setProjectName(projectName);
            theGenerator.setCodeProvider(codeProvider);
            theGenerator.setBuilderName(builderName);
            theGenerator.setNatureID(natureID);
            theGenerator.setNeedsMoreWork(needsMoreWork);
            theGenerator.generate(internalMonitor);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return theGenerator.getProject();
    }

    public static HashMap<String, String> decodeMap(String value) {
        Set<String> list = decodeList(value);
        HashMap<String, String> map = new HashMap<>();
        char escapeChar = '\\';

        for (String curString : list) {
            StringBuilder line = new StringBuilder(curString);
            int lndx = 0;
            while (lndx < line.length()) {
                if (line.charAt(lndx) == '=') {
                    if (line.charAt(lndx - 1) == escapeChar) {
                        // escaped '=' - remove '\' and continue on.
                        line.deleteCharAt(lndx - 1);
                    } else {
                        break;
                    }
                }
                lndx++;
            }
            map.put(line.substring(0, lndx), line.substring(lndx + 1));
        }

        return map;

    }

    /**
     * Method used to decode a string to a set of strings
     * This method is used by AutoBuild to get the configurations from the args of
     * the build command in CommanBuild
     * 
     * @param value
     * @return
     */
    public static Set<String> decodeList(String value) {
        Set<String> ret = new HashSet<>();
        if (value != null) {
            StringBuilder envStr = new StringBuilder(value);
            String escapeChars = "|\\"; //$NON-NLS-1$
            char escapeChar = '\\';
            try {
                while (envStr.length() > 0) {
                    int ndx = 0;
                    while (ndx < envStr.length()) {
                        if (escapeChars.indexOf(envStr.charAt(ndx)) != -1) {
                            if (envStr.charAt(ndx - 1) == escapeChar) {
                                // escaped '|' - remove '\' and continue on.
                                envStr.deleteCharAt(ndx - 1);
                                if (ndx == envStr.length()) {
                                    break;
                                }
                            }
                            if (envStr.charAt(ndx) == '|')
                                break;
                        }
                        ndx++;
                    }
                    StringBuilder line = new StringBuilder(envStr.substring(0, ndx));
                    /*                  int lndx = 0;
                                        while (lndx < line.length()) {
                                            if (line.charAt(lndx) == '=') {
                                                if (line.charAt(lndx - 1) == escapeChar) {
                                                    // escaped '=' - remove '\' and continue on.
                                                    line.deleteCharAt(lndx - 1);
                                                } else {
                                                    break;
                                                }
                                            }
                                            lndx++;
                                        }
                    */
                    ret.add(line.toString());
                    envStr.delete(0, ndx + 1);
                }
            } catch (StringIndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    //    public static String encodeMap(Map<String, String> values) {
    //        Iterator<Entry<String, String>> entries = values.entrySet().iterator();
    //        StringBuilder str = new StringBuilder();
    //        while (entries.hasNext()) {
    //            Entry<String, String> entry = entries.next();
    //            str.append(escapeChars(entry.getKey(), "=|\\", '\\')); //$NON-NLS-1$
    //            str.append("="); //$NON-NLS-1$
    //            str.append(escapeChars(entry.getValue(), "|\\", '\\')); //$NON-NLS-1$
    //            str.append("|"); //$NON-NLS-1$
    //        }
    //        return str.toString();
    //    }

    public static String encode(Set<String> values) {
        StringBuilder str = new StringBuilder();
        Iterator<String> entries = values.iterator();
        while (entries.hasNext()) {
            String entry = entries.next();
            str.append(escapeChars(entry, "|\\", '\\')); //$NON-NLS-1$
            str.append("|"); //$NON-NLS-1$
        }
        return str.toString();
    }

    public static String escapeChars(String string, String escapeChars, char escapeChar) {
        StringBuilder str = new StringBuilder(string);
        for (int i = 0; i < str.length(); i++) {
            if (escapeChars.indexOf(str.charAt(i)) != -1) {
                str.insert(i, escapeChar);
                i++;
            }
        }
        return str.toString();
    }
}
