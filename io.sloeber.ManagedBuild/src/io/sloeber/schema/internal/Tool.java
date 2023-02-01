package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;

/**
 * Represents a tool that can be invoked during a build.
 * Note that this class implements IOptionCategory to represent the top
 * category.
 */
public class Tool extends SchemaObject implements ITool {

    private List<IEnvVarBuildPath> envVarBuildPathList;

    //  private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    private String[] modelIsAbstract;
    private String[] modelOutputFlag;
    private String[] modelNatureFilter;
    private String[] modelCommand;
    private String[] modelCommandLinePattern;
    private String[] modelCommandLineGenerator;
    private String[] modelErrorParsers;
    private String[] modelCustomBuildStep;
    private String[] modelAnnouncement;
    private String[] modelIcon;
    private String[] modelIsSystem;
    private String[] modelIsHidden;

    private boolean isHidden;
    private boolean customBuildStep;
    private boolean isAbstract;
    private boolean isSystem;
    private ToolChain parent;
    private String myAnnouncement;
    private int natureFilter;
    private Map<String, InputType> inputTypeMap = new HashMap<>();
    private Map<String, OutputType> outputTypeMap = new HashMap<>();

    private IManagedCommandLineGenerator commandLineGenerator;

    private Options myOptions = new Options();
    Expression myExpression;

    /**
     * Constructor to create a new tool for a tool-chain based on the information
     * defined in the plugin.xml manifest.
     *
     * @param parent
     *            The parent of this tool. This can be a ToolChain or a
     *            ResourceConfiguration.
     * @param element
     *            The element containing the information about the tool.
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public Tool(ToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;
        loadNameAndID(root, element);
        modelIsAbstract = getAttributes(IS_ABSTRACT);
        modelOutputFlag = getAttributes(OUTPUT_FLAG);
        modelNatureFilter = getAttributes(NATURE);
        modelCommand = getAttributes(COMMAND);
        modelCommandLinePattern = getAttributes(COMMAND_LINE_PATTERN);
        modelCommandLineGenerator = getAttributes(COMMAND_LINE_GENERATOR);
        modelErrorParsers = getAttributes(ERROR_PARSERS);
        modelCustomBuildStep = getAttributes(CUSTOM_BUILD_STEP);
        modelAnnouncement = getAttributes(ANNOUNCEMENT);
        modelIcon = getAttributes(ICON);
        modelIsSystem = getAttributes(IS_SYSTEM);
        modelIsHidden = getAttributes(IS_HIDDEN);

        isAbstract = Boolean.parseBoolean(modelIsAbstract[ORIGINAL]);
        customBuildStep = Boolean.parseBoolean(modelCustomBuildStep[SUPER]);
        isHidden = Boolean.parseBoolean(modelIsHidden[ORIGINAL]);
        isSystem = Boolean.parseBoolean(modelIsSystem[ORIGINAL]);

        switch (modelNatureFilter[SUPER]) {
        case "both": //$NON-NLS-1$
            natureFilter = FILTER_BOTH;
            break;
        case "cnature": //$NON-NLS-1$
            natureFilter = FILTER_C;
            break;
        case "ccnature": //$NON-NLS-1$
            natureFilter = FILTER_CC;
            break;
        default:
            natureFilter = FILTER_BOTH;
        }

        //        // icon - was saved as URL in string form
        //        if (!modelIcon[SUPER].isBlank()) {
        //            try {
        //                iconPathURL = new URL(modelIcon[SUPER]);
        //            } catch (@SuppressWarnings("unused") MalformedURLException e) {
        //                // Print a warning
        //                ManagedBuildManager.outputIconError(modelIcon[SUPER]);
        //                iconPathURL = null;
        //            }
        //        }

        if (modelAnnouncement[SUPER].isBlank()) {
            myAnnouncement = Tool_default_announcement + BLANK + getName(); // + "(" + getId() + ")";
        } else {
            myAnnouncement = modelAnnouncement[SUPER];
        }

        if (!modelCommandLineGenerator[SUPER].isBlank()) {
            commandLineGenerator = (IManagedCommandLineGenerator) createExecutableExtension(COMMAND_LINE_GENERATOR);
        }

        for (IConfigurationElement curChild : getAllChildren()) {
            switch (curChild.getName()) {
            case IOption.ELEMENT_NAME: {
                myOptions.add(new Option(this, root, curChild));
                break;
            }
            case IOptions.OPTION_CAT: {
                myOptions.add(new OptionCategory(this, root, curChild));
                break;
            }
            case IInputType.INPUT_TYPE_ELEMENT_NAME: {
                InputType child = new InputType(this, root, curChild);
                inputTypeMap.put(child.getId(), child);
                break;
            }
            case IOutputType.OUTPUT_TYPE_ELEMENT_NAME: {
                OutputType child = new OutputType(this, root, curChild);
                outputTypeMap.put(child.getId(), child);
                break;
            }
            }
            //            TOFIX JABA this enablement should be done with something like this
            //            It is to mutch work for now (need results in other areas) So I opt for hardcoding som enablemenst
            //            try {
            //                myExpression= ExpressionConverter.getDefault().perform(element);
            //                myExpression.evaluate(null;)
            //            } catch (CoreException e) {
            //                // TODO Auto-generated catch block
            //                e.printStackTrace();
            //            }
        }
    }

    @Override
    public IToolChain getParent() {
        return parent;
    }

    @Override
    public List<IInputType> getInputTypes() {
        List<IInputType> ret = new LinkedList<>();
        for (InputType cur : inputTypeMap.values()) {
            ret.add(cur);
        }
        return ret;
    }

    @Override
    public List<IOutputType> getOutputTypes() {
        List<IOutputType> out = new LinkedList<>();
        for (OutputType cur : outputTypeMap.values()) {
            //          if (cur.isEnabled(this)) {
            out.add(cur);
            //        }
        }
        return out;
    }

    @Override
    public IOutputType getOutputTypeById(String optputTypeID) {
        return getAllOutputTypeById(optputTypeID);
        //        OutputType type = (OutputType) getAllOutputTypeById(optputTypeID);
        //
        //        if (type == null || type.isEnabled(this))
        //            return type;
        //        return null;
    }

    public IOutputType getAllOutputTypeById(String optputTypeID) {
        return outputTypeMap.get(optputTypeID);
    }

    @Override
    public String getName() {
        return myName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public String getErrorParserIds() {
        return modelErrorParsers[SUPER];
    }

    /* (non-Javadoc)
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

    //    private Set<String> contributeErrorParsers(Set<String> set) {
    //        if (getErrorParserIds() != null) {
    //            if (set == null)
    //                set = new HashSet<>();
    //            String ids[] = getErrorParserList();
    //            if (ids.length != 0)
    //                set.addAll(Arrays.asList(ids));
    //        }
    //        return set;
    //    }

    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITool#getInputExtensions()
    //     * @deprecated
    //     */
    //    //@Override
    //    public List<String> getInputExtensions() {
    //        String[] exts = getPrimaryInputExtensions();
    //        List<String> extList = new ArrayList<>();
    //        for (String ext : exts) {
    //            extList.add(ext);
    //        }
    //        return extList;
    //    }

    //    private List<String> getInputExtensionsAttribute() {
    //        //        if ((inputExtensions == null) || (inputExtensions.size() == 0)) {
    //        //            // If I have a superClass, ask it
    //        //            if (getSuperClass() != null) {
    //        //                return ((Tool) getSuperClass()).getInputExtensionsAttribute();
    //        //            } else {
    //        //                inputExtensions = new ArrayList<>();
    //        //            }
    //        //        }
    //        return inputExtensions;
    //    }

    //    private List<String> getInputExtensionsList() {
    //        if (inputExtensions == null) {
    //            inputExtensions = new ArrayList<>();
    //        }
    //        return inputExtensions;
    //    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITool#getPrimaryInputExtensions()
    //     */
    @Override
    public List<String> getPrimaryInputExtensions() {
        List<String> ret = new LinkedList<>();
        for (InputType curInputType : inputTypeMap.values()) {
            ret.addAll(curInputType.getSourceExtensions(this));
        }
        return ret;
        //        IInputType type = getPrimaryInputType();
        //        if (type != null) {
        //            String[] exts = type.getSourceExtensions(this);
        //            // Use the first entry in the list
        //            if (exts.length > 0)
        //                return exts;
        //        }
        //        // If none, use the input extensions specified for the Tool (backwards compatibility)
        //        List<String> extsList = getInputExtensionsAttribute();
        //        // Use the first entry in the list
        //        if (extsList != null && extsList.size() > 0) {
        //            return extsList.toArray(new String[extsList.size()]);
        //        }
        //        return EMPTY_STRING_ARRAY;
    }

    //    public List<String> getInterfaceExtensions() {
    //        return getHeaderExtensionsAttribute();
    //    }

    //    private List<String> getHeaderExtensionsAttribute() {
    //        //        if (interfaceExtensions == null || interfaceExtensions.size() == 0) {
    //        //            // If I have a superClass, ask it
    //        //            if (getSuperClass() != null) {
    //        //                return ((Tool) getSuperClass()).getHeaderExtensionsAttribute();
    //        //            } else {
    //        //                if (interfaceExtensions == null) {
    //        //                    interfaceExtensions = new ArrayList<>();
    //        //                }
    //        //            }
    //        //        }
    //        return interfaceExtensions;
    //    }

    @Override
    public String getOutputFlag() {
        return modelOutputFlag[SUPER];
    }

    @Override
    public String getToolCommand() {
        return modelCommand[SUPER];
    }

    @Override
    public String getCommandLinePattern() {
        return modelCommandLinePattern[SUPER];
    }

    @Override
    public boolean getCustomBuildStep() {
        return customBuildStep;
    }

    @Override
    public String getAnnouncement() {
        return myAnnouncement;
    }

    @Override
    public IManagedCommandLineGenerator getCommandLineGenerator() {
        return commandLineGenerator;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getNatureFilter()
     */
    @Override
    public int getNatureFilter() {
        return natureFilter;
    }

    public List<InputType> getAllInputTypes() {
        return new LinkedList<>(inputTypeMap.values());

    }

    // 
    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getEnvVarBuildPaths()
     */
    @Override
    public IEnvVarBuildPath[] getEnvVarBuildPaths() {
        //        if (envVarBuildPathList != null) {
        return envVarBuildPathList.toArray(new IEnvVarBuildPath[envVarBuildPathList.size()]);
        //        } else if (getSuperClass() != null)
        //            return getSuperClass().getEnvVarBuildPaths();
        //        return null;
    }

    //    private void addEnvVarBuildPath(IEnvVarBuildPath path) {
    //        if (path == null)
    //            return;
    //        if (envVarBuildPathList == null)
    //            envVarBuildPathList = new ArrayList<>();
    //
    //        envVarBuildPathList.add(path);
    //    }

    //    public IProject getProject() {
    //        IBuildObject toolParent = getParent();
    //        if (toolParent != null) {
    //            if (toolParent instanceof IToolChain) {
    //                IConfiguration config = ((IToolChain) toolParent).getParent();
    //                if (config == null)
    //                    return null;
    //                return (IProject) config.getOwner();
    //            } else if (toolParent instanceof IResourceConfiguration) {
    //                return (IProject) ((IResourceConfiguration) toolParent).getOwner();
    //            }
    //        }
    //        return null;
    //    }

    public String[] getContentTypeFileSpecs(IContentType type) {
        return getContentTypeFileSpecs(type, null);// getProject());
    }

    public static String[] getContentTypeFileSpecs(IContentType type, IProject project) {
        String[] globalSpecs = type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
        IContentTypeSettings settings = null;
        //		IProject project = getProject();
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
        //JABA dead code
        return null;
        //        initDataMap();
        //        return typeToDataMap.get(type);
    }

    @Override
    public CLanguageData[] getCLanguageDatas() {
        //TOFIX JABA don't know what this does
        return null;
        //        initDataMap();
        //        return typeToDataMap.values().toArray(new BuildLanguageData[typeToDataMap.size()]);
    }

    public String getNameAndVersion() {
        String idVersion = ManagedBuildManager.getVersionFromIdAndVersion(getId());
        if (idVersion != null && idVersion.length() != 0) {
            return new StringBuilder().append(myName).append(" (").append(idVersion).append("").toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return myName;
    }

    //    public IConfigurationElement getConverterModificationElement(ITool toTool) {
    //        Map<String, IConfigurationElement> map = ManagedBuildManager.getConversionElements(this);
    //        IConfigurationElement element = null;
    //        if (!map.isEmpty()) {
    //            for (IConfigurationElement el : map.values()) {
    //                String toId = el.getAttribute("toId"); //$NON-NLS-1$
    //                ITool to = toTool;
    //                //                if (toId != null) {
    //                //                    for (; to != null; to = to.getSuperClass()) {
    //                //                        if (toId.equals(to.getId()))
    //                //                            break;
    //                //                    }
    //                //                }
    //
    //                if (to != null) {
    //                    element = el;
    //                    break;
    //                }
    //            }
    //        }
    //
    //        return element;
    //    }

    @Override
    public boolean isSystemObject() {
        return isSystem;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public String getUniqueRealName() {
        if (myName == null) {
            myName = getId();
        } else {
            String idVersion = ManagedBuildManager.getVersionFromIdAndVersion(getId());
            if (idVersion != null) {
                StringBuilder buf = new StringBuilder();
                buf.append(myName);
                buf.append(" (v").append(idVersion).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
                myName = buf.toString();
            }
        }
        return myName;
    }

    @Override
    public boolean buildsFileType(IFile file) {
        for (InputType inputType : inputTypeMap.values()) {
            if (inputType.isAssociatedWith(file)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IInputType getInputTypeByID(String id2) {
        return inputTypeMap.get(id2);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getCommandFlags()
     */
    //@Override
    public String[] getCommandFlags() throws BuildException {
        return getToolCommandFlags(null, null);
    }

    /**
     * This method used internally by the Tool to obtain the command flags with the
     * build macros resolved,
     * but could be also used by other MBS components to adjust the tool flags
     * resolution
     * behavior by passing the method some custom macro substitutor
     *
     * @return the command flags with the build macros resolved
     */
    //    private String[] getToolCommandFlags(IFile inputFile, IFile outputFile,
    //            SupplierBasedCdtVariableSubstitutor macroSubstitutor, IMacroContextInfoProvider provider) {
    //        List<IOption> opts = getOptions();
    //        ArrayList<String> flags = new ArrayList<>();
    //        StringBuilder sb = new StringBuilder();
    //        for (IOption option : opts) {
    //            if (option == null)
    //                continue;
    //            sb.setLength(0);
    //
    //            // check to see if the option has an applicability calculator
    //            IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();
    //            IOptionCategory cat = option.getCategory();
    //            IOptionCategoryApplicability catApplicabilityCalculator = cat.getApplicabilityCalculator();
    //
    //            IBuildObject config = null;
    //            //IBuildObject parent = getParent();
    //            if (parent instanceof IResourceConfiguration) {
    //                config = parent;
    //            } else if (parent instanceof IToolChain) {
    //                config = ((IToolChain) parent).getParent();
    //            }
    //
    //            if ((catApplicabilityCalculator == null
    //                    || catApplicabilityCalculator.isOptionCategoryVisible(config, this, cat))
    //                    && (applicabilityCalculator == null
    //                            || applicabilityCalculator.isOptionUsedInCommandLine(config, this, option))) {
    //
    //                // update option in case when its value changed.
    //                // This code is added to fix bug #219684 and
    //                // avoid using "getOptionToSet()"
    //                //                if (applicabilityCalculator != null
    //                //                        && !(applicabilityCalculator instanceof BooleanExpressionApplicabilityCalculator)) {
    //                //                    if (option.getSuperClass() != null)
    //                //                        option = getOptionBySuperClassId(option.getSuperClass().getId());
    //                //                    // bug #405904 - if the option is an extension element (first time we build),
    //                //                    // use the option id as a superclass id, otherwise we won't find the option we may have just
    //                //                    // set and will end up with the default setting
    //                //                    else if (option.isExtensionElement())
    //                //                        option = getOptionBySuperClassId(option.getId());
    //                //                    else
    //                //                        option = getOptionById(option.getId());
    //                //                }
    //
    //                try {
    //                    boolean generateDefaultCommand = true;
    //                    IOptionCommandGenerator commandGenerator = option.getCommandGenerator();
    //                    if (commandGenerator != null) {
    //                        switch (option.getValueType()) {
    //                        case IOption.BOOLEAN:
    //                        case IOption.ENUMERATED:
    //                        case IOption.TREE:
    //                        case IOption.STRING:
    //                        case IOption.STRING_LIST:
    //                        case IOption.INCLUDE_FILES:
    //                        case IOption.INCLUDE_PATH:
    //                        case IOption.LIBRARY_PATHS:
    //                        case IOption.LIBRARY_FILES:
    //                        case IOption.MACRO_FILES:
    //                        case IOption.UNDEF_INCLUDE_FILES:
    //                        case IOption.UNDEF_INCLUDE_PATH:
    //                        case IOption.UNDEF_LIBRARY_PATHS:
    //                        case IOption.UNDEF_LIBRARY_FILES:
    //                        case IOption.UNDEF_MACRO_FILES:
    //                        case IOption.PREPROCESSOR_SYMBOLS:
    //                        case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
    //                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
    //                                    new FileContextData(inputFile.getLocation(), outputFile.getLocation(), option,
    //                                            this));
    //                            if (info != null) {
    //                                macroSubstitutor.setMacroContextInfo(info);
    //                                String command = commandGenerator.generateCommand(option, macroSubstitutor);
    //                                if (command != null) {
    //                                    sb.append(command);
    //                                    generateDefaultCommand = false;
    //                                }
    //                            }
    //                            break;
    //                        default:
    //                            break;
    //                        }
    //                    }
    //                    if (generateDefaultCommand) {
    //                        switch (option.getValueType()) {
    //                        case IOption.BOOLEAN:
    //                            String boolCmd;
    //                            if (option.getBooleanValue()) {
    //                                boolCmd = option.getCommand();
    //                            } else {
    //                                // Note: getCommandFalse is new with CDT 2.0
    //                                boolCmd = option.getCommandFalse();
    //                            }
    //                            if (boolCmd != null && boolCmd.length() > 0) {
    //                                sb.append(boolCmd);
    //                            }
    //                            break;
    //
    //                        case IOption.ENUMERATED:
    //                            String enumVal = option.getEnumCommand(option.getSelectedEnum());
    //                            if (enumVal.length() > 0) {
    //                                sb.append(enumVal);
    //                            }
    //                            break;
    //
    //                        case IOption.TREE:
    //                            String treeVal = option.getCommand(option.getStringValue());
    //                            if (treeVal.length() > 0) {
    //                                sb.append(treeVal);
    //                            }
    //                            break;
    //
    //                        case IOption.STRING: {
    //                            String strCmd = option.getCommand();
    //                            String val = option.getStringValue();
    //                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
    //                                    new FileContextData(inputFile.getLocation(), outputFile.getLocation(), option,
    //                                            this));
    //                            if (info != null) {
    //                                macroSubstitutor.setMacroContextInfo(info);
    //                                if (val.length() > 0
    //                                        && (val = CdtVariableResolver.resolveToString(val, macroSubstitutor))
    //                                                .length() > 0) {
    //                                    sb.append(evaluateCommand(strCmd, val));
    //                                }
    //                            }
    //                        }
    //                            break;
    //
    //                        case IOption.STRING_LIST:
    //                        case IOption.INCLUDE_FILES:
    //                        case IOption.INCLUDE_PATH:
    //                        case IOption.LIBRARY_PATHS:
    //                        case IOption.LIBRARY_FILES:
    //                        case IOption.MACRO_FILES:
    //                        case IOption.UNDEF_INCLUDE_FILES:
    //                        case IOption.UNDEF_INCLUDE_PATH:
    //                        case IOption.UNDEF_LIBRARY_PATHS:
    //                        case IOption.UNDEF_LIBRARY_FILES:
    //                        case IOption.UNDEF_MACRO_FILES: {
    //                            String listCmd = option.getCommand();
    //                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
    //                                    new FileContextData(inputFile.getLocation(), outputFile.getLocation(), option,
    //                                            this));
    //                            if (info != null) {
    //                                macroSubstitutor.setMacroContextInfo(info);
    //                                String[] list = CdtVariableResolver.resolveStringListValues(
    //                                        option.getBasicStringListValue(), macroSubstitutor, true);
    //                                if (list != null) {
    //                                    for (String temp : list) {
    //                                        if (temp.length() > 0 && !temp.equals(EMPTY_QUOTED_STRING))
    //                                            sb.append(evaluateCommand(listCmd, temp)).append(BLANK);
    //                                    }
    //                                }
    //                            }
    //                        }
    //                            break;
    //
    //                        case IOption.PREPROCESSOR_SYMBOLS:
    //                        case IOption.UNDEF_PREPROCESSOR_SYMBOLS: {
    //                            String defCmd = option.getCommand();
    //                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
    //                                    new FileContextData(inputFile.getLocation(), outputFile.getLocation(), option,
    //                                            this));
    //                            if (info != null) {
    //                                macroSubstitutor.setMacroContextInfo(info);
    //                                String[] symbols = CdtVariableResolver.resolveStringListValues(
    //                                        option.getBasicStringListValue(), macroSubstitutor, true);
    //                                if (symbols != null) {
    //                                    for (String temp : symbols) {
    //                                        if (temp.length() > 0)
    //                                            sb.append(evaluateCommand(defCmd, temp) + BLANK);
    //                                    }
    //                                }
    //                            }
    //                        }
    //                            break;
    //
    //                        default:
    //                            break;
    //                        }
    //                    }
    //
    //                    if (sb.toString().trim().length() > 0)
    //                        flags.add(sb.toString().trim());
    //
    //                } catch (BuildException e) {
    //                    // Bug 315187 one broken option shouldn't cascade to all other options breaking the build...
    //                    Status s = new Status(IStatus.ERROR, Activator.getId(),
    //                            MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, option, option.getId()), e);
    //                    Activator.log(new CoreException(s));
    //                } catch (CdtVariableException e) {
    //                    Status s = new Status(IStatus.ERROR, Activator.getId(),
    //                            MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, option, option.getId()), e);
    //                    Activator.log(new CoreException(s));
    //                }
    //            }
    //        }
    //        String[] f = new String[flags.size()];
    //        return flags.toArray(f);
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getToolCommandFlags(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
     */
    @Override
    public String[] getToolCommandFlags(IFile inputFile, IFile outputFile) throws BuildException {
        return new String[0];
        //        SupplierBasedCdtVariableSubstitutor macroSubstitutor = new BuildfileMacroSubstitutor(null, EMPTY_STRING, BLANK);
        //        return getToolCommandFlags(inputFile, outputFile, macroSubstitutor, BuildMacroProvider.getDefault());
    }

    @Override
    public List<String> getExtraFlags(int optionType) {
        List<String> flags = new LinkedList<>();
        return flags;
        //        if (optionType != IOption.LIBRARIES && optionType != IOption.OBJECTS) {
        //            // Early exit to avoid performance penalty
        //            return flags;
        //        }
        //
        //        for (IOption option : getOptions()) {
        //            try {
        //                if (option.getValueType() != optionType) {
        //                    continue;
        //                }
        //
        //                // check to see if the option has an applicability calculator
        //                IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();
        //
        //                if (applicabilityCalculator == null
        //                        || applicabilityCalculator.isOptionUsedInCommandLine(this, this, option)) {
        //                    boolean generateDefaultCommand = true;
        //                    IOptionCommandGenerator commandGenerator = option.getCommandGenerator();
        //                    if (commandGenerator != null) {
        //                        SupplierBasedCdtVariableSubstitutor macroSubstitutor = new BuildfileMacroSubstitutor(null,
        //                                EMPTY_STRING, BLANK);
        //                        IMacroContextInfoProvider provider = BuildMacroProvider.getDefault();
        //                        IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_OPTION,
        //                                new OptionContextData(option, this));
        //                        if (info != null) {
        //                            macroSubstitutor.setMacroContextInfo(info);
        //                            String command = commandGenerator.generateCommand(option, macroSubstitutor);
        //                            if (command != null) {
        //                                flags.add(command);
        //                                generateDefaultCommand = false;
        //                            }
        //                        }
        //                    }
        //
        //                    if (generateDefaultCommand) {
        //                        switch (optionType) {
        //                        case IOption.LIBRARIES: {
        //                            String command = option.getCommand();
        //                            String[] libs = option.getLibraries();
        //                            for (String lib : libs) {
        //                                try {
        //                                    String resolved[] = ManagedBuildManager.getBuildMacroProvider()
        //                                            .resolveStringListValueToMakefileFormat(lib, " ", //$NON-NLS-1$
        //                                                    " ", //$NON-NLS-1$
        //                                                    IBuildMacroProvider.CONTEXT_OPTION,
        //                                                    new OptionContextData(option, this));
        //                                    if (resolved != null && resolved.length > 0) {
        //                                        for (String string : resolved) {
        //                                            if (!string.isEmpty()) {
        //                                                flags.add(command + string);
        //                                            }
        //                                        }
        //                                    }
        //                                } catch (BuildMacroException e) {
        //                                    // TODO: report error
        //                                    continue;
        //                                }
        //                            }
        //                            break;
        //                        }
        //                        case IOption.OBJECTS: {
        //                            String userObjs[] = option.getUserObjects();
        //                            if (userObjs != null && userObjs.length > 0) {
        //                                for (String userObj : userObjs) {
        //                                    try {
        //                                        String resolved[] = ManagedBuildManager.getBuildMacroProvider()
        //                                                .resolveStringListValueToMakefileFormat(userObj, "", //$NON-NLS-1$
        //                                                        " ", //$NON-NLS-1$
        //                                                        IBuildMacroProvider.CONTEXT_OPTION,
        //                                                        new OptionContextData(option, this));
        //                                        if (resolved != null && resolved.length > 0) {
        //                                            flags.addAll(Arrays.asList(resolved));
        //                                        }
        //                                    } catch (BuildMacroException e) {
        //                                        // TODO: report error
        //                                        continue;
        //                                    }
        //                                }
        //                            }
        //                            break;
        //                        }
        //                        default:
        //                            // Cannot happen
        //                            break;
        //                        }
        //                    }
        //                }
        //            } catch (BuildException | CdtVariableException e) {
        //                // TODO: report error
        //                continue;
        //            }
        //        }
        //        return flags;
    }

    @Override
    public IOptions getOptions() {
        return myOptions;
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + TOOL_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + IS_ABSTRACT + EQUAL + modelIsAbstract[ORIGINAL] + NEWLINE);
        ret.append(prepend + OUTPUT_FLAG + EQUAL + modelOutputFlag[SUPER] + NEWLINE);
        ret.append(prepend + NATURE + EQUAL + modelNatureFilter[SUPER] + NEWLINE);
        ret.append(prepend + COMMAND + EQUAL + modelCommand[SUPER] + NEWLINE);
        ret.append(prepend + COMMAND_LINE_PATTERN + EQUAL + modelCommandLinePattern[SUPER] + NEWLINE);

        ret.append(prepend + COMMAND_LINE_GENERATOR + EQUAL + modelCommandLineGenerator[SUPER] + NEWLINE);
        ret.append(prepend + ERROR_PARSERS + EQUAL + modelErrorParsers[SUPER] + NEWLINE);
        ret.append(prepend + CUSTOM_BUILD_STEP + EQUAL + modelCustomBuildStep[SUPER] + NEWLINE);
        ret.append(prepend + ANNOUNCEMENT + EQUAL + modelAnnouncement[SUPER] + NEWLINE);
        ret.append(prepend + ICON + EQUAL + modelIcon[SUPER] + NEWLINE);
        ret.append(prepend + IS_HIDDEN + EQUAL + modelIsHidden[SUPER] + NEWLINE);
        ret.append(prepend + IS_SYSTEM + EQUAL + modelIsSystem[SUPER] + NEWLINE);

        //        
        //        
        //
        //        ret.append(prepend + BEGIN_OF_CHILDREN + ITool.TOOL_ELEMENT_NAME + NEWLINE);
        //        ret.append(prepend + "Number of tools " + String.valueOf(myToolMap.size()));
        //        leadingChars++;
        //        for (Tool curTool : myToolMap.values()) {
        //            ret.append(curTool.dump(leadingChars));
        //        }
        //        ret.append(prepend + END_OF_CHILDREN + ITool.TOOL_ELEMENT_NAME + NEWLINE);

        return ret;
    }
}

//    public PathInfoCache setDiscoveredPathInfo(IInputType type, PathInfoCache info) {
//        return discoveredInfoMap.put(getTypeKey(type), info);
//    }
//
//    public PathInfoCache getDiscoveredPathInfo(IInputType type) {
//        return discoveredInfoMap.get(getTypeKey(type));
//    }
//
//    public PathInfoCache clearDiscoveredPathInfo(IInputType type) {
//        return discoveredInfoMap.remove(getTypeKey(type));
//    }
//
//    public void clearAllDiscoveredPathInfo() {
//        discoveredInfoMap.clear();
//    }

//    public void clearAllDiscoveredInfo() {
//        discoveredInfoMap.clear();
//    }

//    private String getTypeKey(IInputType type) {
//        if (type != null)
//            return type.getId();
//        return null;
//    }

//    @Override
//    public boolean matches(ITool tool) {
//        if (tool == this)
//            return true;
//
//        ITool rT = ManagedBuildManager.getRealTool(this);
//        if (rT == null)
//            return false;
//
//        return rT == ManagedBuildManager.getRealTool(tool);
//    }

/*  public SupportedProperties getSupportedProperties(){
        Map map = findSupportedProperties();
        if(map != null)
            return new HashMap(map);
        return null;
    }
*/

//    private List<String> getInterfaceExtensionsList() {
//        if (interfaceExtensions == null) {
//            interfaceExtensions = new ArrayList<>();
//        }
//        return interfaceExtensions;
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getAllInputExtensions()
//     */
//    @Override
//    public String[] getAllInputExtensions() {
//        return getAllInputExtensions(getProject());
//    }

//    public String[] getAllInputExtensions(IProject project) {
//        
//        IInputType[] types = inputTypeList;
//        if (inputTypeList.size() > 0) {
//            List<String> allExts = new ArrayList<>();
//            for (IInputType type : types) {
//                String[] exts = ((InputType) type).getSourceExtensions(this, project);
//                for (String ext : exts) {
//                    allExts.add(ext);
//                }
//            }
//            if (allExts.size() > 0) {
//                return allExts.toArray(new String[allExts.size()]);
//            }
//        }
//        // If none, use the input extensions specified for the Tool (backwards compatibility)
//        List<String> extsList = getInputExtensionsAttribute();
//        if (extsList != null && extsList.size() > 0) {
//            return extsList.toArray(new String[extsList.size()]);
//        }
//        return EMPTY_STRING_ARRAY;
//    }

//    @Override
//    public IInputType getPrimaryInputType() {
//        //TOFIX JABA 
//        //Primary input types no longer exists
//        return null;
//        //        IInputType type = null;
//        //        IInputType[] types = getInputTypes();
//        //        if (types != null && types.length > 0) {
//        //            for (int i = 0; i < types.length; i++) {
//        //                if (i == 0)
//        //                    type = types[0];
//        //                if (types[i].getPrimaryInput() == true) {
//        //                    type = types[i];
//        //                    break;
//        //                }
//        //            }
//        //        }
//        //        return type;
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getAdditionalDependencies()
//     */
//    @Override
//    public IPath[] getAdditionalDependencies() {
//        //TODO JABA dead code removal
//        return null;
//        //        List<IPath> allDeps = new ArrayList<>();
//        //        IInputType[] types = getInputTypes();
//        //        for (IInputType type : types) {
//        //            if (type != getPrimaryInputType()) {
//        //                if (type.getOptionId() != null) {
//        //                    IOption option = getOptionBySuperClassId(type.getOptionId());
//        //                    if (option != null) {
//        //                        try {
//        //                            List<IPath> inputs = new ArrayList<>();
//        //                            int optType = option.getValueType();
//        //                            if (optType == IOption.STRING) {
//        //                                inputs.add(Path.fromOSString(option.getStringValue()));
//        //                            } else if (optType == IOption.STRING_LIST || optType == IOption.LIBRARIES
//        //                                    || optType == IOption.OBJECTS || optType == IOption.INCLUDE_FILES
//        //                                    || optType == IOption.LIBRARY_PATHS || optType == IOption.LIBRARY_FILES
//        //                                    || optType == IOption.MACRO_FILES) {
//        //                                @SuppressWarnings("unchecked")
//        //                                List<String> inputNames = (List<String>) option.getValue();
//        //                                filterValues(optType, inputNames);
//        //                                for (String s : inputNames)
//        //                                    inputs.add(Path.fromOSString(s));
//        //                            }
//        //                            allDeps.addAll(inputs);
//        //                        } catch (BuildException ex) {
//        //                        }
//        //                    }
//        //                } else if (type.getBuildVariable() != null && type.getBuildVariable().length() > 0) {
//        //                    allDeps.add(Path.fromOSString("$(" + type.getBuildVariable() + ")")); //$NON-NLS-1$ //$NON-NLS-2$
//        //                }
//        //            }
//        //        }
//        //        return allDeps.toArray(new IPath[allDeps.size()]);
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getAllDependencyExtensions()
//     */
//    @Override
//    public String[] getAllDependencyExtensions() {
//        IInputType[] types = getInputTypes();
//        if (types != null && types.length > 0) {
//            List<String> allExts = new ArrayList<>();
//            for (IInputType t : types)
//                for (String s : t.getDependencyExtensions(this))
//                    allExts.add(s);
//
//            if (allExts.size() > 0)
//                return allExts.toArray(new String[allExts.size()]);
//        }
//        // If none, use the header extensions specified for the Tool (backwards compatibility)
//        List<String> extsList = getHeaderExtensionsAttribute();
//        if (extsList != null && extsList.size() > 0) {
//            return extsList.toArray(new String[extsList.size()]);
//        }
//        return EMPTY_STRING_ARRAY;
//    }

/* (non-Javadoc)
 * @see org.eclipse.cdt.managedbuilder.core.ITool#getInterfaceExtension()
 * @deprecated
 */

//    @Override
//    public IOptionPathConverter getOptionPathConverter() {
//        // Use existing converter
//        if (optionPathConverter != null) {
//            return optionPathConverter;
//        }
//        if (optionPathConverter == null) {
//            // If there is not yet a optionPathConverter try to construct from configuration element
//            IConfigurationElement element = getPathconverterElement();
//            if (element != null) {
//                try {
//                    if (element.getAttribute(ITool.OPTIONPATHCONVERTER) != null) {
//                        optionPathConverter = (IOptionPathConverter) element
//                                .createExecutableExtension(ITool.OPTIONPATHCONVERTER);
//                    }
//                } catch (CoreException e) {
//                }
//            }
////            if (optionPathConverter == null) {
////                // If there is still no optionPathConverter, ask superclass of this tool whether it has a converter
////                if (getSuperClass() != null) {
////                    ITool superTool = getSuperClass();
////                    optionPathConverter = superTool.getOptionPathConverter();
////                }
////            }
//            // If there is still no converter, ask the toolchain for a
//            // global converter
//            if ((optionPathConverter == null) && (getParent() instanceof IResourceConfiguration)) {
//                // The tool belongs to a resource configuration
//                IResourceConfiguration resourceConfiguration = (IResourceConfiguration) getParent();
//                IConfiguration configuration = resourceConfiguration.getParent();
//                if (null != configuration) {
//                    IToolChain toolchain = configuration.getToolChain();
//                    optionPathConverter = toolchain.getOptionPathConverter();
//                }
//            }
//            if ((optionPathConverter == null) && (getParent() instanceof IToolChain)) {
//                // The tool belongs to a toolchain
//                IToolChain toolchain = (IToolChain) getParent();
//                optionPathConverter = toolchain.getOptionPathConverter();
//            }
//        }
//
//        pathconverterElement = null; // discard now that we've created one
//        return optionPathConverter;
//    }

/*
 *  O B J E C T   S T A T E   M A I N T E N A N C E
 */
//
//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#isExtensionElement()
//     */
//    @Override
//    public boolean isExtensionElement() {
//        return isExtensionTool;
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#isHeaderFile(java.lang.String)
//     */
//    @Override
//    public boolean isHeaderFile(String ext) {
//        if (ext == null) {
//            return false;
//        }
//        String[] exts = getAllDependencyExtensions();
//        for (String dep : exts) {
//            if (ext.equals(dep))
//                return true;
//        }
//        return false;
//    }

/* (non-Javadoc)
 * @see org.eclipse.cdt.core.build.managed.ITool#producesFileType(java.lang.String)
 */
//    @Override
//    public boolean producesFileType(String extension) {
//        if (extension == null) {
//            return false;
//        }
//        //  Check the output-types first
//        if (getOutputType(extension) != null) {
//            return true;
//        }
//        //  If there are no OutputTypes, check the attribute
//        if (!hasOutputTypes()) {
//            String[] exts = getOutputsAttribute();
//            if (exts != null) {
//                for (String ext : exts) {
//                    if (ext.equals(extension))
//                        return true;
//                }
//            }
//        }
//        return false;
//    }

/**
 * @return the pathconverterElement
 */
//public IConfigurationElement getPathconverterElement() {
//    return pathconverterElement;
//}

//@Override
//public boolean getAdvancedInputCategory() {
//  //        if (advancedInputCategory == null) {
//  //            if (getSuperClass() != null) {
//  //                return getSuperClass().getAdvancedInputCategory();
//  //            } else {
//  //                return false; // default is false
//  //            }
//  //        }
//  return advancedInputCategory.booleanValue();
//}

//    public boolean hasCustomSettings(Tool tool) {
//        if (superClass == null)
//            return true;
//
//        ITool realTool = ManagedBuildManager.getRealTool(this);
//        ITool otherRealTool = ManagedBuildManager.getRealTool(tool);
//        if (realTool != otherRealTool)
//            return true;
//
//        if (hasCustomSettings())
//            return true;
//
//        if (outputTypeList != null && outputTypeList.size() != 0) {
//            for (OutputType outType : outputTypeList) {
//                if (outType.hasCustomSettings())
//                    return true;
//            }
//        }
//        Tool superTool = (Tool) superClass;
//
//        if (command != null && !command.equals(superTool.getToolCommand()))
//            return true;
//
//        if (modelErrorParsers[SUPER] != null && !errorParserIds.equals(superTool.getErrorParserIds()))
//            return true;
//
//        if (commandLinePattern != null && !commandLinePattern.equals(superTool.getCommandLinePattern()))
//            return true;
//
//        if (customBuildStep != null && customBuildStep.booleanValue() != superTool.getCustomBuildStep())
//            return true;
//
//        if (announcement != null && !announcement.equals(superTool.getAnnouncement()))
//            return true;
//
//        if (isHidden != null && isHidden.booleanValue() != superTool.isHidden())
//            return true;
//
//        //        if (discoveredInfoMap != null && discoveredInfoMap.size() != 0)
//        //            return true;
//
//        if (isAnyOptionModified(this, tool))
//            return true;
//
//        return false;
//    }

///* (non-Javadoc)
//* @see org.eclipse.cdt.managedbuilder.core.ITool#getCommandLineGeneratorElement()
//*/
//public IConfigurationElement getCommandLineGeneratorElement() {
// //        if (commandLineGeneratorElement == null) {
// //            if (getSuperClass() != null) {
// //                return ((Tool) getSuperClass()).getCommandLineGeneratorElement();
// //            }
// //        }
// return commandLineGeneratorElement;
//}

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputExtensions()
//     * @deprecated
//     */
//    //@Override
//    public String[] getOutputExtensions() {
//        return getOutputsAttribute();
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputsAttribute()
//     */
//    @Override
//    public String[] getOutputsAttribute() {
//        //        // TODO:  Why is this treated differently than inputExtensions?
//        //        if (outputExtensions == null) {
//        //            if (getSuperClass() != null) {
//        //                return getSuperClass().getOutputsAttribute();
//        //            } else {
//        //                return null;
//        //            }
//        //        }
//        return outputExtensions.split(DEFAULT_SEPARATOR);
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.core.build.managed.ITool#getOutputType(java.lang.String)
//     */
//    @Override
//    public IOutputType getOutputType(String outputExtension) {
//        IOutputType type = null;
//        IOutputType[] types = getOutputTypes();
//        if (types != null && types.length > 0) {
//            for (IOutputType t : types) {
//                if (t.isOutputExtension(this, outputExtension)) {
//                    type = t;
//                    break;
//                }
//            }
//        }
//        return type;
//    }

//public List<OutputType> getAllOutputTypes() {
//    return outputTypeList;
//return ourTypes.toArray(new OutputType[ourTypes.size()]);
//      
//        IOutputType[] types = null;
//        // Merge our output types with our superclass' output types.
//        if (getSuperClass() != null) {
//            types = ((Tool) getSuperClass()).getAllOutputTypes();
//        }
//        // Our options take precedence.
//        Vector<OutputType> ourTypes = getOutputTypeList();
//        if (types != null) {
//            for (int i = 0; i < ourTypes.size(); i++) {
//                IOutputType ourType = ourTypes.get(i);
//                int j;
//                for (j = 0; j < types.length; j++) {
//                    if (ourType.getSuperClass() != null && ourType.getSuperClass().getId().equals(types[j].getId())) {
//                        types[j] = ourType;
//                        break;
//                    }
//                }
//                //  No Match?  Add it.
//                if (j == types.length) {
//                    IOutputType[] newTypes = new IOutputType[types.length + 1];
//                    for (int k = 0; k < types.length; k++) {
//                        newTypes[k] = types[k];
//                    }
//                    newTypes[j] = ourType;
//                    types = newTypes;
//                }
//            }
//        } else {
//            types = ourTypes.toArray(new IOutputType[ourTypes.size()]);
//        }
//        return types;
//}
//    private boolean hasOutputTypes() {
//        return (outputTypeList.size() > 0);
//    }

/* (non-Javadoc)
 * @see org.eclipse.cdt.managedbuilder.core.ITool#getDependencyGeneratorElement()
 * @deprecated
 */
//    public IConfigurationElement getDependencyGeneratorElement() {
//        //  First try the primary InputType
//        IInputType type = getPrimaryInputType();
//        if (type != null) {
//            IConfigurationElement primary = ((InputType) type).getDependencyGeneratorElement();
//            if (primary != null)
//                return primary;
//        }
//
//        //  If not found, use the deprecated attribute
//        return getToolDependencyGeneratorElement();
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getDependencyGeneratorElementForExtension()
//     */
//    public IConfigurationElement getDependencyGeneratorElementForExtension(String sourceExt) {
//        IInputType[] types = getInputTypes();
//        if (types != null) {
//            for (IInputType type : types) {
//                if (type.isSourceExtension(this, sourceExt)) {
//                    return ((InputType) type).getDependencyGeneratorElement();
//                }
//            }
//        }
//
//        //  If not found, use the deprecated attribute
//        return getToolDependencyGeneratorElement();
//    }

//    private IConfigurationElement getToolDependencyGeneratorElement() {
//        if (dependencyGeneratorElement == null) {
//            if (getSuperClass() != null) {
//                return ((Tool) getSuperClass()).getToolDependencyGeneratorElement();
//            }
//        }
//        return dependencyGeneratorElement;
//    }

//  /* (non-Javadoc)
//   * @see org.eclipse.cdt.managedbuilder.core.ITool#setDependencyGeneratorElement(String)
//   * @deprecated
//   */
//  private void setDependencyGeneratorElement(IConfigurationElement element) {
//      dependencyGeneratorElement = element;
//      setDirty(true);
//  }

/* (non-Javadoc)
 * @see org.eclipse.cdt.managedbuilder.core.ITool#getDependencyGenerator()
 * @deprecated
 */
//    @Override
//    public IManagedDependencyGenerator getDependencyGenerator() {
//        if (dependencyGenerator != null) {
//            if (dependencyGenerator instanceof IManagedDependencyGenerator)
//                return (IManagedDependencyGenerator) dependencyGenerator;
//            else
//                return null;
//        }
//        IConfigurationElement element = getDependencyGeneratorElement();
//        if (element != null) {
//            try {
//                if (element.getAttribute(DEP_CALC_ID) != null) {
//                    dependencyGenerator = (IManagedDependencyGeneratorType) element
//                            .createExecutableExtension(DEP_CALC_ID);
//                    if (dependencyGenerator != null) {
//                        if (dependencyGenerator instanceof IManagedDependencyGenerator) {
//                            dependencyGeneratorElement = null; // no longer needed now that we've created one
//                            return (IManagedDependencyGenerator) dependencyGenerator;
//                        } else
//                            return null;
//                    }
//                }
//            } catch (CoreException e) {
//            }
//        }
//        return null;
//    }

//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getDependencyGeneratorForExtension()
//     */
//    @Override
//    public IManagedDependencyGeneratorType getDependencyGeneratorForExtension(String sourceExt) {
//        if (dependencyGenerator != null) {
//            return dependencyGenerator;
//        }
//        IConfigurationElement element = getDependencyGeneratorElementForExtension(sourceExt);
//        if (element != null) {
//            try {
//                if (element.getAttribute(DEP_CALC_ID) != null) {
//                    dependencyGenerator = (IManagedDependencyGeneratorType) element
//                            .createExecutableExtension(DEP_CALC_ID);
//                    return dependencyGenerator;
//                }
//            } catch (CoreException e) {
//            }
//        }
//        return null;
//    }
//public boolean hasScannerConfigSettings(IInputType type) {
//if (type == null) {
//  boolean has = hasScannerConfigSettings();
//  if (has)
//      return has;
//  //            ITool superClass = getSuperClass();
//  //            if (superClass != null && superClass instanceof Tool)
//  //                return ((Tool) superClass).hasScannerConfigSettings(type);
//  return false;
//}
//return ((InputType) type).hasScannerConfigSettings();
//}
//
//private boolean hasScannerConfigSettings() {
//
//if (getDiscoveryProfileIdAttribute() != null)
//  return true;
//
//return false;
//}
//public void addInputType(InputType type) {
//inputTypeList.add(type);
//getInputTypeMap().put(type.getId(), type);
//}

//
//    /* (non-Javadoc)
//     * @see org.eclipse.cdt.managedbuilder.core.ITool#getDefaultInputExtension()
//     */
//    @Override
//    public String getDefaultInputExtension() {
//        // Find the primary input type
//        IInputType type = getPrimaryInputType();
//        if (type != null) {
//            String[] exts = type.getSourceExtensions(this);
//            // Use the first entry in the list
//            if (exts.length > 0)
//                return exts[0];
//        }
//        // If none, use the input extensions specified for the Tool (backwards compatibility)
//        List<String> extsList = getInputExtensionsAttribute();
//        // Use the first entry in the list
//        if (extsList != null && extsList.size() > 0)
//            return extsList.get(0);
//        return EMPTY_STRING;
//    }

/**
 * //* Look for ${VALUE} in the command string
 * //
 */
//private static String evaluateCommand(String command, String values) {
//  final int DOLLAR_VALUE_LENGTH = 8;
//
//  if (command == null)
//      return values.trim();
//
//  String ret = command;
//  boolean found = false;
//  int start = 0;
//  int index;
//  int len;
//  while ((index = ret.indexOf("${", start)) >= 0 && //$NON-NLS-1$
//          (len = ret.length()) >= index + DOLLAR_VALUE_LENGTH) {
//      start = index;
//      index = index + 2;
//      int ch = ret.charAt(index);
//      if (ch == 'v' || ch == 'V') {
//          index++;
//          ch = ret.charAt(index);
//          if (ch == 'a' || ch == 'A') {
//              index++;
//              ch = ret.charAt(index);
//              if (ch == 'l' || ch == 'L') {
//                  index++;
//                  ch = ret.charAt(index);
//                  if (ch == 'u' || ch == 'U') {
//                      index++;
//                      ch = ret.charAt(index);
//                      if (ch == 'e' || ch == 'E') {
//                          index++;
//                          ch = ret.charAt(index);
//                          if (ch == '}') {
//                              String temp = ""; //$NON-NLS-1$
//                              index++;
//                              found = true;
//                              if (start > 0) {
//                                  temp = ret.substring(0, start);
//                              }
//                              temp = temp.concat(values.trim());
//                              if (len > index) {
//                                  start = temp.length();
//                                  ret = temp.concat(ret.substring(index));
//                                  index = start;
//                              } else {
//                                  ret = temp;
//                                  break;
//                              }
//                          }
//                      }
//                  }
//              }
//          }
//      }
//      start = index;
//  }
//  if (found)
//      return ret.trim();
//  return (command + values).trim();
//}

//private static final String EMPTY_QUOTED_STRING = "\"\""; //$NON-NLS-1$
//
//@Override
//public IOptionCategoryApplicability getApplicabilityCalculator() {
//  // Tool does not have any ApplicabilityCalculator.
//  return null;
//}
//@Override
//public IOptionCategory getTopOptionCategory() {
//  return this;
//}
//public boolean supportsLanguageSettings() {
//List<IOption> options = getOptions();
//boolean found = false;
//for (IOption option : options) {
//  try {
//      int type = option.getValueType();
//      if (ManagedBuildManager.optionTypeToEntryKind(type) != 0) {
//          found = true;
//          break;
//      }
//  } catch (BuildException e) {
//      Activator.log(e);
//  }
//}
//return found;
//}
//    public String getDiscoveryProfileIdAttribute() {
//        if (scannerConfigDiscoveryProfileId == null && superClass != null)
//            return ((Tool) superClass).getDiscoveryProfileIdAttribute();
//        return scannerConfigDiscoveryProfileId;
//    }

//    private IToolChain getToolChain() {
//        IBuildObject bo = getParent();
//        IToolChain tCh = null;
//        if (bo instanceof IToolChain) {
//            tCh = ((IToolChain) bo);
//        } else if (bo instanceof IFileInfo) {
//            tCh = ((ResourceConfiguration) bo).getBaseToolChain();
//        }
//        return tCh;
//    }

//    public String getDiscoveryProfileId() {
//        String id = getDiscoveryProfileIdAttribute();
//        if (id == null) {
//            IToolChain tc = getToolChain();
//            if (tc != null)
//                id = tc.getScannerConfigDiscoveryProfileId();
//        }
//        return id;
//    }

//public IOption[] getOptionsOfType(int type) {
//    List<IOption> list = new ArrayList<>();
//    for (IOption op : getOptions()) {
//        try {
//            if (op.getValueType() == type)
//                list.add(op);
//        } catch (BuildException e) {
//            Activator.log(e);
//        }
//    }
//    return list.toArray(new Option[list.size()]);
//}
//    @Override
//    public IInputType getInputTypeForCLanguageData(CLanguageData data) {
//        if (data instanceof BuildLanguageData)
//            return ((BuildLanguageData) data).getInputType();
//        return null;
//    }

//    private BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
//        if (booleanExpressionCalculator == null) {
//            if (superClass != null) {
//                return ((Tool) superClass).getBooleanExpressionCalculator();
//            }
//        }
//        return booleanExpressionCalculator;
//    }
//public boolean isEnabled(IResourceInfo rcInfo) {
//return true;
////        BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
////        if (calc == null)
////            return true;
////
////        return calc.isToolUsedInCommandLine(rcInfo, this);
//}

//    private SupportedProperties findSupportedProperties() {
//        if (supportedProperties == null) {
//            if (superClass != null) {
//                return ((Tool) superClass).findSupportedProperties();
//            }
//        }
//        return supportedProperties;
//    }