package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRule;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRules;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.enablement.MBSEnablementExpression;

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
    // @formatter:on

    private List<IEnvVarBuildPath> myEnvVarBuildPathList;

    // private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    private String[] myModelIsAbstract;
    private String[] myModelOutputFlag;
    private String[] myModelNatureFilter;
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
    private boolean myIsAbstract;
    private boolean myIsSystem;
    private ToolChain myToolchain;
    private String myAnnouncement;
    private Map<String, InputType> myInputTypeMap = new HashMap<>();
    private Map<String, OutputType> myOutputTypeMap = new HashMap<>();

    private IManagedCommandLineGenerator myCommandLineGenerator;

    private URL myIconPathURL;

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
        myModelIsAbstract = getAttributes(IS_ABSTRACT);
        myModelOutputFlag = getAttributes(OUTPUT_FLAG);
        myModelNatureFilter = getAttributes(NATURE);
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

        myIsAbstract = Boolean.parseBoolean(myModelIsAbstract[ORIGINAL]);
        myCustomBuildStep = Boolean.parseBoolean(myModelCustomBuildStep[SUPER]);
        myIsHidden = Boolean.parseBoolean(myModelIsHidden[ORIGINAL]);
        myIsSystem = Boolean.parseBoolean(myModelIsSystem[ORIGINAL]);

        if (myModelCommandLinePattern[SUPER].isBlank()) {
            myModelCommandLinePattern[SUPER] = DEFAULT_PATTERN;
        }

        if (myModelAnnouncement[SUPER].isBlank()) {
            myAnnouncement = Tool_default_announcement + BLANK + getName(); // + "(" + getId() + ")";
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.ITool#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return myIsAbstract;
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

    //
    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getEnvVarBuildPaths()
     */
    @Override
    public IEnvVarBuildPath[] getEnvVarBuildPaths() {
        // if (envVarBuildPathList != null) {
        return myEnvVarBuildPathList.toArray(new IEnvVarBuildPath[myEnvVarBuildPathList.size()]);
        // } else if (getSuperClass() != null)
        // return getSuperClass().getEnvVarBuildPaths();
        // return null;
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

    /**
     * This method used internally by the Tool to obtain the command flags with the
     * build macros resolved, but could be also used by other MBS components to
     * adjust the tool flags resolution behavior by passing the method some custom
     * macro substitutor
     *
     * @return the command flags with the build macros resolved
     */
    private String[] getToolCommandFlagsInternal(AutoBuildConfigurationDescription autoBuildConfData,
            IResource resource) {
        // List<IOption> opts = getOptions();
        Map<String, String> selectedOptions = autoBuildConfData.getSelectedOptions(resource, this);

        List<String> flags = new LinkedList<>();

        for (IOption curOption : getOptions().getOptions()) {
            String optionValue = selectedOptions.get(curOption.getId());
            if (optionValue == null) {
                // no value set for this option
                // as options with a default are set during project creation time it is
                // safe to ignore this option
                // or this option has no default setting nor a user setting
                // At least that is my thinking JABA
                continue;
            }

            String[] cmdContrib = curOption.getCommandLineContribution(resource, optionValue, autoBuildConfData);
            java.util.Collections.addAll(flags, cmdContrib);
            //
            // } catch (BuildException e) {
            // // Bug 315187 one broken option shouldn't cascade to all other options
            // //breaking the build...
            // Status s = new Status(IStatus.ERROR, Activator.getId(),
            // MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, curOption,
            // curOption.getId()),
            // e);
            // Activator.log(new CoreException(s));
            // } catch (CdtVariableException e) {
            // Status s = new Status(IStatus.ERROR, Activator.getId(),
            // MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, curOption,
            // curOption.getId()),
            // e);
            // Activator.log(new CoreException(s));
            // }
        }
        for (int curFlag = flags.size() - 1; curFlag >= 0; curFlag--) {
            if (flags.get(curFlag).isBlank()) {
                flags.remove(curFlag);
            }
        }
        return flags.toArray(new String[flags.size()]);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.cdt.managedbuilder.core.ITool#getToolCommandFlags(org.eclipse.
     * core.runtime.IPath, org.eclipse.core.runtime.IPath)
     */
    @Override
    public String[] getToolCommandFlags(IAutoBuildConfigurationDescription autoConfData, IResource resource)
            throws BuildException {
        return getToolCommandFlagsInternal((AutoBuildConfigurationDescription) autoConfData, resource);
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = DUMPLEAD.repeat(leadingChars);
        ret.append(prepend + TOOL_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + IS_ABSTRACT + EQUAL + myModelIsAbstract[ORIGINAL] + NEWLINE);
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
    public MakeRules getMakeRules(IAutoBuildConfigurationDescription autoBuildConfData, IOutputType outputTypeIn,
            IFile inputFile, int makeRuleSequenceID, boolean VERBOSE) {
        MakeRules ret = new MakeRules();
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
                    MakeRule newMakeRule = new MakeRule(this, inputType, inputFile, outputType, outputFile,
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
    public String[] getRecipes(IAutoBuildConfigurationDescription autoBuildConfIn, Set<IFile> inputFiles,
            Set<String> flags, String outputName, Map<String, Set<String>> nicePreReqNameList) {
        AutoBuildConfigurationDescription autoBuildConf = (AutoBuildConfigurationDescription) autoBuildConfIn;
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

        String quotedOutputName = outputName;
        // if the output name isn't a variable then quote it
        if (!quotedOutputName.isBlank() && !quotedOutputName.contains("$(")) { //$NON-NLS-1$
            quotedOutputName = DOUBLE_QUOTE + quotedOutputName + DOUBLE_QUOTE;
        }

        Map<String, String> preReqFiles = new HashMap<>();
        for (String curCmdVariable : nicePreReqNameList.keySet()) {
            String inputsStr = EMPTY_STRING;
            for (String inp : nicePreReqNameList.get(curCmdVariable)) {
                if (inp != null && !inp.isEmpty()) {
                    // if the input resource isn't a variable then quote it
                    if (inp.indexOf("$(") != 0) { //$NON-NLS-1$
                        inp = DOUBLE_QUOTE + inp + DOUBLE_QUOTE;
                    }
                    inputsStr = inputsStr + inp + WHITESPACE;
                }
            }
            preReqFiles.put(curCmdVariable, inputsStr.trim());
        }

        if (!myModelDependencyGenerationFlag[SUPER].isBlank()) {
            IProject project = autoBuildConf.getProject();
            IFile depFile = getDependencyFile(project.getFile(outputName));
            String depFlag = myModelDependencyGenerationFlag[SUPER];
            depFlag = depFlag.replace(makeVariable(myModelDependencyOutputPattern[SUPER]),
                    depFile.getProjectRelativePath().toString());
            flags.add(depFlag);
        }
        String flagsStr = String.join(WHITESPACE, flags);

        String command = commandLinePattern.replace(makeVariable(CMD_LINE_PRM_NAME), cmd);

        command = command.replace(makeVariable(FLAGS_PRM_NAME), flagsStr);
        command = command.replace(makeVariable(OUTPUT_FLAG_PRM_NAME), myModelOutputFlag[SUPER]);
        command = command.replace(makeVariable(OUTPUT_PRM_NAME), quotedOutputName);
        for (Entry<String, String> curCmdVariable : preReqFiles.entrySet()) {
            command = command.replace(makeVariable(curCmdVariable.getKey()), curCmdVariable.getValue());

        }
        IToolChain toolchain = autoBuildConf.getConfiguration().getToolChain();
        ArrayList<String> toolRecipes = toolchain.getPreToolRecipes(this);
        //make sure all environment variables are resolved
        toolRecipes.addAll(Arrays.asList(command.split("\\r?\\n"))); //$NON-NLS-1$

        toolRecipes.addAll(toolchain.getPostToolRecipes(this));

        return toolRecipes.toArray(new String[toolRecipes.size()]);
    }

    @Override
    public IFile getDependencyFile(IFile curTargetFile) {
        String depName = AutoBuildCommon.applyPattern(myModelDependencyOutputPattern[SUPER], curTargetFile);
        if (depName.isBlank()) {
            return null;
        }
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

}
