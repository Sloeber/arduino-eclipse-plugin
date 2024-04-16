package io.sloeber.autoBuild.schema.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.internal.AutoBuildCommon.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolType;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildMakeRule;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildMakeRules;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IOptionCategory;
import io.sloeber.autoBuild.schema.api.IOutputType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.api.IToolChain;
import io.sloeber.autoBuild.schema.internal.enablement.MBSEnablementExpression;

/**
 * Represents a tool that can be invoked during a build. Note that this class
 * implements IOptionCategory to represent the top category.
 */
public class Tool extends SchemaObject implements ITool {
    // @formatter:off
    /**
     * A tool is enabled if the enablement part of the xml model validates to true
     * and the modelNatureFilter matches the nature of the project The table below
     * shows the decisions
     *
     * tool cnature ccnature result
     * both yes yes true
     * both yes no true
     * both no yes true
     * both no no false
     *
     * cnature yes yes false
     * cnature yes no true
     * cnature no yes false
     * cnature no no false
     *
     * ccnature yes yes true
     * ccnature yes no false
     * ccnature no yes true
     * ccnature no no false
     *
     * note that the case no/no should not happen as a MBS project should have a
     * cNature and/or CCNature Therefore the code simplifies the decision table to
     *
     * tool cnature ccnature result
     * both yes yes true
     * both yes no true
     * both no yes true
     * both no no true
     *
     * cnature yes yes false
     * cnature yes no true
     * cnature no yes false
     * cnature no no true
     *
     * ccnature yes yes true
     * ccnature yes no false
     * ccnature no yes true
     * ccnature no no false
     *
     */

    private String[] myModelOutputFlag;
    private String[] myModelNatureFilter;
    private String[] myModelToolType;
    private String[] myModelCommand;
    private String[] myModelCommandLinePattern;
    private String[] myModelCommandLineGenerator;
    private String[] myModelErrorParsers;
    private String[] myModelCustomBuildStep;
    private String[] myModelAnnouncement;
    private String[] myModelIcon;
    private String[] myModelIsSystem;
    private String[] myModelIsHidden;
    private String[] myModelDependencyOutputPattern;
    private String[] myModelDependencyGenerationFlag;

    private boolean myIsHidden;
    private boolean myCustomBuildStep;
    private boolean myIsSystem;
    private ToolChain myToolchain;
    private String myAnnouncement;
    private Map<String, InputType> myInputTypeMap = new HashMap<>();
    private Map<String, OutputType> myOutputTypeMap = new HashMap<>();

    private IManagedCommandLineGenerator myCommandLineGenerator;

    private URL myIconPathURL;
    private ToolType myToolType;

    @Override
    public boolean isEnabled(IResource resource, IAutoBuildConfigurationDescription autoData) {
        if (!super.isEnabled(MBSEnablementExpression.ENABLEMENT_TYPE_CMD, resource, autoData)) {
            return false;
        }
        try {
            switch (myModelNatureFilter[SUPER]) {
            case "both": //$NON-NLS-1$
                return true;
            case "cnature": //$NON-NLS-1$
                return !resource.getProject().hasNature(CCProjectNature.CC_NATURE_ID);
            case "ccnature": //$NON-NLS-1$
                return resource.getProject().hasNature(CCProjectNature.CC_NATURE_ID);
			default:
				break;
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Constructor to create a new tool for a tool-chain based on the information
     * defined in the plugin.xml manifest.
     *
     * @param parent
     *            The parent of this tool. This can be a ToolChain
     *            or a ResourceConfiguration.
     * @param element
     *            The element containing the information about the
     *            tool.
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public Tool(ToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.myToolchain = parent;
        loadNameAndID(root, element);
        myModelOutputFlag = getAttributes(OUTPUT_FLAG);
        myModelNatureFilter = getAttributes(NATURE);
        myModelToolType = getAttributes(TOOL_TYPE);
        myModelCommand = getAttributes(COMMAND);
        myModelCommandLinePattern = getAttributes(COMMAND_LINE_PATTERN);
        myModelCommandLineGenerator = getAttributes(COMMAND_LINE_GENERATOR);
        myModelErrorParsers = getAttributes(ERROR_PARSERS);
        myModelCustomBuildStep = getAttributes(CUSTOM_BUILD_STEP);
        myModelAnnouncement = getAttributes(ANNOUNCEMENT);
        myModelIcon = getAttributes(ICON);
        myModelIsSystem = getAttributes(IS_SYSTEM);
        myModelIsHidden = getAttributes(IS_HIDDEN);
        myModelDependencyOutputPattern = getAttributes(DEPENDENCY_OUTPUT_PATTERN);
        myModelDependencyGenerationFlag = getAttributes(DEPENDENCY_GENERATION_FLAG);

        myCustomBuildStep = Boolean.parseBoolean(myModelCustomBuildStep[SUPER]);
        myIsHidden = Boolean.parseBoolean(myModelIsHidden[ORIGINAL]);
        myIsSystem = Boolean.parseBoolean(myModelIsSystem[ORIGINAL]);
        myToolType = ToolType.getToolType(myModelToolType[SUPER]);

        if (myModelCommandLinePattern[SUPER].isBlank()) {
            myModelCommandLinePattern[SUPER] = DEFAULT_PATTERN;
        }

        if (myModelAnnouncement[SUPER].isBlank()) {
            myAnnouncement = Tool_default_announcement + BLANK + getName();
        } else {
            myAnnouncement = myModelAnnouncement[SUPER];
        }

        if (!myModelCommandLineGenerator[SUPER].isBlank()) {
            myCommandLineGenerator = (IManagedCommandLineGenerator) createExecutableExtension(COMMAND_LINE_GENERATOR);
        }

        for (IConfigurationElement curChild : getFirstChildren(INPUT_TYPE_ELEMENT_NAME)) {
            InputType child = new InputType(this, root, curChild);
            myInputTypeMap.put(child.getId(), child);
        }
        for (IConfigurationElement curChild : getFirstChildren(OUTPUT_TYPE_ELEMENT_NAME)) {
            OutputType child = new OutputType(this, root, curChild);
            myOutputTypeMap.put(child.getId(), child);
        }

        if (!myModelIcon[SUPER].isBlank()) {
            try {
                myIconPathURL = new URL(myModelIcon[SUPER]);
            } catch (@SuppressWarnings("unused") MalformedURLException e) {
                AutoBuildManager.outputIconError(myModelIcon[SUPER]);
                myIconPathURL = null;
            }
        }
    }

    @Override
    public IToolChain getParent() {
        return myToolchain;
    }

    @Override
    public List<IInputType> getInputTypes() {
        List<IInputType> ret = new LinkedList<>();
        for (InputType cur : myInputTypeMap.values()) {
            ret.add(cur);
        }
        return ret;
    }

    @Override
    public List<IOutputType> getOutputTypes() {
        List<IOutputType> out = new LinkedList<>();
        for (OutputType cur : myOutputTypeMap.values()) {
            // if (cur.isEnabled(this)) {
            out.add(cur);
            // }
        }
        return out;
    }

    @Override
    public IOutputType getOutputTypeById(String optputTypeID) {
        return getAllOutputTypeById(optputTypeID);
        // OutputType type = (OutputType) getAllOutputTypeById(optputTypeID);
        //
        // if (type == null || type.isEnabled(this))
        // return type;
        // return null;
    }

    public IOutputType getAllOutputTypeById(String optputTypeID) {
        return myOutputTypeMap.get(optputTypeID);
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getErrorParserIds() {
        return myModelErrorParsers[SUPER];
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getErrorParserList()
     */
    @Override
    public String[] getErrorParserList() {
        String parserIDs = getErrorParserIds();
        String[] errorParsers;
        if (parserIDs != null) {
            // Check for an empty string
            if (parserIDs.length() == 0) {
                errorParsers = new String[0];
            } else {
                StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
                List<String> list = new ArrayList<>(tok.countTokens());
                while (tok.hasMoreElements()) {
                    list.add(tok.nextToken());
                }
                String[] strArr = { "" }; //$NON-NLS-1$
                errorParsers = list.toArray(strArr);
            }
        } else {
            errorParsers = new String[0];
        }
        return errorParsers;
    }

    @Override
    public String getOutputFlag() {
        return myModelOutputFlag[SUPER];
    }

    @Override
    public String getDefaultommandLineCommand() {
        return myModelCommand[SUPER];
    }

    @Override
    public String getDefaultCommandLinePattern() {
        return myModelCommandLinePattern[SUPER];
    }

    @Override
    public boolean getCustomBuildStep() {
        return myCustomBuildStep;
    }

    @Override
    public String getAnnouncement() {
        return myAnnouncement;
    }

    @Override
    public IManagedCommandLineGenerator getCommandLineGenerator() {
        return myCommandLineGenerator;
    }

    public List<InputType> getAllInputTypes() {
        return new LinkedList<>(myInputTypeMap.values());

    }

    public static String[] getContentTypeFileSpecs(IContentType type, IProject project) {
        String[] globalSpecs = type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
        IContentTypeSettings settings = null;
        // IProject project = getProject();
        if (project != null) {
            IScopeContext projectScope = new ProjectScope(project);
            try {
                settings = type.getSettings(projectScope);
            } catch (Exception e) {
                Activator.log(e);
            }
            if (settings != null) {
                String[] specs = settings.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
                if (specs.length > 0) {
                    int total = globalSpecs.length + specs.length;
                    String[] projSpecs = new String[total];
                    int i = 0;
                    for (String spec : specs) {
                        projSpecs[i] = spec;
                        i++;
                    }
                    for (String spec : globalSpecs) {
                        projSpecs[i] = spec;
                        i++;
                    }
                    return projSpecs;
                }
            }
        }
        return globalSpecs;
    }

    @Override
    public CLanguageData getCLanguageData(IInputType type) {
        // JABA dead code
        return null;
        // initDataMap();
        // return typeToDataMap.get(type);
    }

    @Override
    public boolean isSystemObject() {
        return myIsSystem;
    }

    @Override
    public boolean isHidden() {
        return myIsHidden;
    }

    @Override
    public IInputType getInputTypeByID(String id2) {
        return myInputTypeMap.get(id2);
    }

    @Override
    public Map<String, String> getToolCommandVars(IAutoBuildConfigurationDescription iAutoConfData,
            Map<IOption, String> selectedOptions) {

        Map<String, List<String>> allVars = new HashMap<>();

        for (Entry<IOption, String> curSelectrturOption : selectedOptions.entrySet()) {
            IOption curOption = curSelectrturOption.getKey();
            String optionValue = curSelectrturOption.getValue();
            if (curOption == null || optionValue == null) {
                //this should not happen but just to be safe
                continue;
            }
            Map<String, String> optionVars = curOption.getCommandVars(optionValue, iAutoConfData);
            for (Entry<String, String> curVar : optionVars.entrySet()) {
                String varName = curVar.getKey();
                String varValue = curVar.getValue();
                if (varValue.isBlank()) {
                    //blank items will add unnecessary spaces
                    continue;
                }
                List<String> alreadyFoundArguments = allVars.get(varName);
                if (alreadyFoundArguments == null) {
                    alreadyFoundArguments = new LinkedList<>();
                    allVars.put(varName, alreadyFoundArguments);
                }
                if (!alreadyFoundArguments.contains(varValue)) {
                    alreadyFoundArguments.add(varValue);
                }
            }
        }
        Map<String, String> ret = new HashMap<>();
        for (Entry<String, List<String>> curVar : allVars.entrySet()) {
            ret.put(curVar.getKey(), String.join(WHITESPACE, curVar.getValue()));
        }
        return ret;
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = DUMPLEAD.repeat(leadingChars);
        ret.append(prepend + TOOL_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + OUTPUT_FLAG + EQUAL + myModelOutputFlag[SUPER] + NEWLINE);
        ret.append(prepend + NATURE + EQUAL + myModelNatureFilter[SUPER] + NEWLINE);
        ret.append(prepend + COMMAND + EQUAL + myModelCommand[SUPER] + NEWLINE);
        ret.append(prepend + COMMAND_LINE_PATTERN + EQUAL + myModelCommandLinePattern[SUPER] + NEWLINE);

        ret.append(prepend + COMMAND_LINE_GENERATOR + EQUAL + myModelCommandLineGenerator[SUPER] + NEWLINE);
        ret.append(prepend + ERROR_PARSERS + EQUAL + myModelErrorParsers[SUPER] + NEWLINE);
        ret.append(prepend + CUSTOM_BUILD_STEP + EQUAL + myModelCustomBuildStep[SUPER] + NEWLINE);
        ret.append(prepend + ANNOUNCEMENT + EQUAL + myModelAnnouncement[SUPER] + NEWLINE);
        ret.append(prepend + ICON + EQUAL + myModelIcon[SUPER] + NEWLINE);
        ret.append(prepend + IS_HIDDEN + EQUAL + myModelIsHidden[SUPER] + NEWLINE);
        ret.append(prepend + IS_SYSTEM + EQUAL + myModelIsSystem[SUPER] + NEWLINE);
        if (myEnablement.isBlank()) {
            ret.append(prepend + "No enablement found" + NEWLINE); //$NON-NLS-1$
        } else {
            ret.append(prepend + "Enablement found" + NEWLINE); //$NON-NLS-1$
        }
        for (InputType curInputType : myInputTypeMap.values()) {
            ret.append(curInputType.dump(leadingChars + 1));
        }
        for (OutputType curOutputType : myOutputTypeMap.values()) {
            ret.append(curOutputType.dump(leadingChars + 1));
        }
        ret.append(myOptions.dump(leadingChars + 1));

        return ret;
    }

    static private final String IGNORED_BY = " ignored by: "; //$NON-NLS-1$
    static private final String DISABLED = " disabled: ";//$NON-NLS-1$
    static private final String ACCEPTED_BY = " accepted by: "; //$NON-NLS-1$

    @Override
    public AutoBuildMakeRules getMakeRules(IAutoBuildConfigurationDescription autoBuildConfData, IOutputType outputTypeIn,
            IFile inputFile, int makeRuleSequenceID, boolean VERBOSE) {
        AutoBuildMakeRules ret = new AutoBuildMakeRules();
        if (!isEnabled(inputFile, autoBuildConfData)) {
            if (VERBOSE) {
                System.out.println(myName + DISABLED);
            }
            return ret;
        }
        for (IInputType inputType : getInputTypes()) {
            if (outputTypeIn != null && myOutputTypeMap.get(outputTypeIn.getId()) != null) {
                //if an inputType is a outputType of this tool ignore it.
                //if not this will create a endless loop
                continue;
            }
            if (inputType.isAssociatedWith(inputFile, outputTypeIn)) {
                for (IOutputType outputType : getOutputTypes()) {
                    if (!outputType.isEnabled(MBSEnablementExpression.ENABLEMENT_TYPE_CMD, inputFile,
                            autoBuildConfData)) {
                        if (VERBOSE) {
                            System.out.println(inputFile + BLANK + myName + ACCEPTED_BY + inputType.getName() + DISABLED
                                    + outputType.getName());
                        }
                        continue;
                    }
                    IFile outputFile = outputType.getOutputFile(inputFile, autoBuildConfData, inputType);
                    if (outputFile == null) {
                        if (VERBOSE) {
                            System.out.println(inputFile + BLANK + myName + ACCEPTED_BY + inputType.getName()
                                    + IGNORED_BY + outputType.getName());
                        }
                        continue;
                    }
                    if (VERBOSE) {
                        System.out.println(inputFile + BLANK + myName + ACCEPTED_BY + inputType.getName() + ACCEPTED_BY
                                + outputType.getName());
                    }
                    AutoBuildMakeRule newMakeRule = new AutoBuildMakeRule(this, inputType, inputFile, outputType, outputFile,
                            makeRuleSequenceID);

                    ret.addRule(newMakeRule);
                }
            } else {
                if (VERBOSE) {
                    System.out.println(inputFile + BLANK + myName + IGNORED_BY + inputType.getName());
                }
            }
        }
        return ret;
    }

    @Override
    public String[] getRecipes(IAutoBuildConfigurationDescription autoBuildConfData, IFolder buildFolder,
            Set<IFile> inputFiles, Map<String, String> toolCommandVars, IFile targetFile) {
        AutoBuildConfigurationDescription autoBuildConf = (AutoBuildConfigurationDescription) autoBuildConfData;
        String cmd = myModelCommand[SUPER];
        String commandLinePattern = myModelCommandLinePattern[SUPER];
        if (inputFiles.size() == 1) {
            IFile selectedInputFile = inputFiles.iterator().next();
            cmd = autoBuildConf.getToolCommand(this, selectedInputFile);
            commandLinePattern = autoBuildConf.getToolPattern(this, selectedInputFile);
        } else {
            cmd = autoBuildConf.getToolCommand(this, autoBuildConf.getProject());
            commandLinePattern = autoBuildConf.getToolPattern(this, autoBuildConf.getProject());
        }
        // expand the command
        String resolvedCommand = resolve(cmd, EMPTY_STRING, WHITESPACE, autoBuildConf);
        if (!resolvedCommand.isBlank())
            cmd = resolvedCommand.trim();

        commandLinePattern = getVariableValue(commandLinePattern, commandLinePattern, false, autoBuildConf);

        String depFlag = myModelDependencyGenerationFlag[SUPER];
        if (!myModelDependencyGenerationFlag[SUPER].isBlank()) {
            IFile depFile = getDependencyFile(targetFile);
            depFlag = depFlag.replace(makeVariable(myModelDependencyOutputPattern[SUPER]),
                    GetNiceFileName(buildFolder, depFile));
            depFlag = depFlag.replace(OUT_MACRO, GetNiceFileName(buildFolder, targetFile));

            if (!depFlag.isBlank()) {
                String flagsValue = toolCommandVars.get(FLAGS_PRM_NAME);
                flagsValue = flagsValue.trim() + WHITESPACE + depFlag;
                toolCommandVars.put(FLAGS_PRM_NAME, flagsValue.trim());
            }
        }

        //add some more variables to the list
        toolCommandVars.put(CMD_LINE_PRM_NAME, cmd.trim());
        toolCommandVars.put(OUTPUT_FLAG_PRM_NAME, myModelOutputFlag[SUPER].trim());

        //add the tool provider stuff
        IBuildTools buildTools = autoBuildConfData.getBuildTools();
        if (buildTools != null) {
            String toolProviderCmd = buildTools.getCommand(myToolType);
            if (toolProviderCmd != null && !toolProviderCmd.isBlank()) {
                //replace the command with the one provided by the toolProvider
                toolCommandVars.put(CMD_LINE_PRM_NAME, toolProviderCmd.trim());
            }
            IPath toolPath = buildTools.getToolLocation();
            if (toolPath != null && !toolPath.toString().isBlank()) {
                //store the path
                toolCommandVars.put(CMD_LINE_TOOL_PATH, toolPath.toString().trim() + SLACH);
            }
            Map<String, String> toolVariables = buildTools.getToolVariables();
            if (toolVariables != null && toolVariables.size() > 0) {
                //replace the command with the one provided by the toolProvider
                for (Entry<String, String> curVar : toolVariables.entrySet()) {
                    String curVars = toolCommandVars.get(curVar.getKey());
                    if (curVars == null) {
                        toolCommandVars.put(curVar.getKey(), curVar.getValue());
                    } else {
                        toolCommandVars.put(curVar.getKey(), curVars + WHITESPACE + curVar.getValue());
                    }
                }

            }
        }

        //resolve the variables
        String command = commandLinePattern;
        String flagsVar = toolCommandVars.get(FLAGS_PRM_NAME);
        if (flagsVar == null || flagsVar.isBlank()) {
            //when FLAGS is empty you get 2 spaces which confuses other logic
            //TOFIX This should be a more generic solution
            command = command.replace(WHITESPACE + makeVariable(FLAGS_PRM_NAME), EMPTY_STRING);
        }
        for (Entry<String, String> curVar : toolCommandVars.entrySet()) {
            command = command.replace(makeVariable(curVar.getKey()), curVar.getValue().trim());
        }

        return command.split(LINE_BREAK_REGEX);
    }

    @Override
    public IFile getDependencyFile(IFile curTargetFile) {
        String depName = AutoBuildCommon.applyPattern(myModelDependencyOutputPattern[SUPER], curTargetFile);
        if (depName.isBlank()) {
            return null;
        }
        depName=makeNameMakeSafe(depName);

        IResource fileParent = curTargetFile.getParent();
        if (fileParent instanceof IFolder) {
            IFolder folder = (IFolder) fileParent;
            return folder.getFile(depName);
        }
        return null;
    }

    @Override
    public URL getIconPath() {
        return myIconPathURL;
    }

    @Override
    public Set<IOptionCategory> getCategories(IAutoBuildConfigurationDescription iAutoBuildConf, IResource resource) {
        Set<IOptionCategory> ret = new LinkedHashSet<>();
        AutoBuildConfigurationDescription autoBuildConf = (AutoBuildConfigurationDescription) iAutoBuildConf;
        if (!isEnabled(resource, autoBuildConf)) {
            //The tools is not enabled=>ignore
            return ret;
        }
        if (resource.isDerived()) {
            // the resource is derived=>ignore
            return ret;
        }
        if (resource instanceof IFile) {
            boolean isAssociated = false;
            for (IInputType inputType : getInputTypes()) {
                if (inputType.isAssociatedWith((IFile) resource, null)) {
                    isAssociated = true;
                    continue;
                }
            }
            if (!isAssociated) {
                //The resource is a file and this tool is not processing this file =>ignore
                return ret;
            }
        }

        return myOptions.getCategories(resource, autoBuildConf);
    }

    @Override
    public Set<IOption> getOptionsOfCategory(IOptionCategory cat, IResource resource,
            IAutoBuildConfigurationDescription iAutoBuildConf) {
        Set<IOption> ret = new LinkedHashSet<>();
        AutoBuildConfigurationDescription autoBuildConf = (AutoBuildConfigurationDescription) iAutoBuildConf;
        if (!isEnabled(resource, autoBuildConf)) {
            //The tools is not enabled=>ignore
            return ret;
        }
        if (resource.isDerived()) {
            // the resource is derived=>ignore
            return ret;
        }
        if (resource instanceof IFile) {
            boolean isAssociated = false;
            for (IInputType inputType : getInputTypes()) {
                if (inputType.isAssociatedWith((IFile) resource, null)) {
                    isAssociated = true;
                    continue;
                }
            }
            if (!isAssociated) {
                //The resource is a file and this tool is not processing this file =>ignore
                return ret;
            }
        }
        ret.addAll(myOptions.getOptionsOfCategory(cat, resource, autoBuildConf));
        return ret;
    }

    @Override
    public IOption getOption(String key) {
        return myOptions.getOptionById(key);
    }

	@Override
	public boolean isForLanguage(String languageId) {
		if(myToolType==null) {
			return false;
		}
		return myToolType.isForLanguage(languageId) ;
	}

	@Override
	public ToolType getToolType() {
		return myToolType;
	}

}
