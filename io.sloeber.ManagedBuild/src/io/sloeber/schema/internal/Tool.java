package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.integration.Const.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.InputMap;

import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.Internal.BooleanExpressionApplicabilityCalculator;
import io.sloeber.autoBuild.Internal.BuildLanguageData;
import io.sloeber.autoBuild.Internal.BuildMacroProvider;
import io.sloeber.autoBuild.Internal.BuildfileMacroSubstitutor;
import io.sloeber.autoBuild.Internal.FileContextData;
import io.sloeber.autoBuild.Internal.IMacroContextInfo;
import io.sloeber.autoBuild.Internal.IMacroContextInfoProvider;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.OptionContextData;
import io.sloeber.autoBuild.Internal.OptionEnablementExpression;
import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.api.IOptionPathConverter;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.IOptionApplicability;
import io.sloeber.autoBuild.extensionPoint.IOptionCategoryApplicability;
import io.sloeber.autoBuild.extensionPoint.IOptionCommandGenerator;
import io.sloeber.buildProperties.PropertyManager;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFileInfo;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.IResourceConfiguration;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.HoldsOptions;
import io.sloeber.schema.internal.IBuildObject;
import io.sloeber.schema.internal.InputType;
import io.sloeber.schema.internal.Option;
import io.sloeber.schema.internal.OutputType;
import io.sloeber.schema.internal.ToolChain;

/**
 * Represents a tool that can be invoked during a build.
 * Note that this class implements IOptionCategory to represent the top
 * category.
 */
public class Tool extends HoldsOptions implements ITool, IOptionCategory {

    private static final String EMPTY_QUOTED_STRING = "\"\""; //$NON-NLS-1$
    private static final String WHITESPACE = " "; //$NON-NLS-1$

    private List<IEnvVarBuildPath> envVarBuildPathList;

    private List<String> inputExtensions;
    private List<String> interfaceExtensions;

    private SupportedProperties supportedProperties;
    //  Miscellaneous
    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;
    private HashMap<IInputType, CLanguageData> typeToDataMap = new HashMap<>(2);
    private List<OptionEnablementExpression> myOptionEnablementExpression = new ArrayList<>();

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
    private URL iconPathURL;
    private IBuildObject parent;
    private String myAnnouncement;
    private int natureFilter;
    private Map<String, InputType> inputTypeMap = new HashMap<>();
    private Map<String, OutputType> outputTypeMap = new HashMap<>();

    private IManagedCommandLineGenerator commandLineGenerator;

    /*
     *  C O N S T R U C T O R S
     */

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
    public Tool(IBuildObject parent, IExtensionPoint root, IConfigurationElement element) {
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

        // icon - was saved as URL in string form
        if (!modelIcon[SUPER].isBlank()) {
            try {
                iconPathURL = new URL(modelIcon[SUPER]);
            } catch (MalformedURLException e) {
                // Print a warning
                ManagedBuildManager.outputIconError(modelIcon[SUPER]);
                iconPathURL = null;
            }
        }

        if (modelAnnouncement[SUPER].isBlank()) {
            myAnnouncement = Tool_default_announcement + WHITESPACE + getName(); // + "(" + getId() + ")";
        } else {
            myAnnouncement = modelAnnouncement[SUPER];
        }

        if (!modelCommandLineGenerator[SUPER].isBlank()) {
            commandLineGenerator = (IManagedCommandLineGenerator) createExecutableExtension(COMMAND_LINE_GENERATOR);
        }

        IConfigurationElement[] children = element.getChildren();
        for (IConfigurationElement curChild : children) {
            switch (curChild.getName()) {
            case IOption.ELEMENT_NAME: {
                Option child = new Option(this, root, curChild);
                myOptionMap.put(child.getId(), child);
                break;
            }
            case IHoldsOptions.OPTION_CAT: {
                OptionCategory child = new OptionCategory(this, root, curChild);
                categoryMap.put(child.getId(), child);
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
        }
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    @Override
    public IBuildObject getParent() {
        return parent;
    }

    @Override
    public IOptionCategory getTopOptionCategory() {
        return this;
    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITool#createInputType(IInputType, String, String, boolean)
    //     */
    //    public IInputType createInputType(IInputType superClass, String Id, String name, boolean isExtensionElement) {
    //        //        InputType type = superClass == null || superClass.isExtensionElement()
    //        //                ? new InputType(this, superClass, Id, name, isExtensionElement)
    //        //                : new InputType(this, Id, name, (InputType) superClass);
    //        InputType type = new InputType(this, Id, name, (InputType) superClass);
    //        if (superClass != null) {
    //            BuildLanguageData data = (BuildLanguageData) typeToDataMap.remove(superClass);
    //            if (data != null) {
    //                data.updateInputType(type);
    //                typeToDataMap.put(type, data);
    //            }
    //        }
    //        addInputType(type);
    //
    //        return type;
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getInputTypes()
     */
    @Override
    public List<IInputType> getInputTypes() {
        List<IInputType> ret = new LinkedList<>();
        for (InputType cur : inputTypeMap.values()) {
            ret.add(cur);
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputTypes()
     */
    @Override
    public List<IOutputType> getOutputTypes() {
        List<IOutputType> out = new LinkedList<>();
        for (OutputType cur : outputTypeMap.values()) {
            if (cur.isEnabled(this)) {
                out.add(cur);
            }
        }
        return out;
    }

    @Override
    public IOutputType getOutputTypeById(String id) {
        OutputType type = (OutputType) getAllOutputTypeById(id);

        if (type == null || type.isEnabled(this))
            return type;
        return null;
    }

    public IOutputType getAllOutputTypeById(String id) {
        IOutputType type = outputTypeMap.get(id);
        //        if (type == null) {
        //            if (getSuperClass() != null) {
        //                return ((Tool) getSuperClass()).getAllOutputTypeById(id);
        //            }
        //        }
        return type;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOwner()
     */
    @Override
    public IOptionCategory getOwner() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getIconPath()
     */
    @Override
    public URL getIconPath() {
        //        if (iconPathURL == null && getSuperClass() != null) {
        //            return getSuperClass().getTopOptionCategory().getIconPath();
        //        }
        return iconPathURL;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptions()
     */
    //@Override
    public Object[][] getOptions(IConfiguration configuration) {
        // Find the child of the configuration that represents the same tool.
        // It could be the tool itself, or a "sub-class" of the tool.
        if (configuration != null) {
            List<ITool> tools = configuration.getTools();
            return getOptions(tools);
        } else {
            return getAllOptions(this);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptions()
     */
    //@Override
    public Object[][] getOptions(IResourceConfiguration resConfig) {
        List<ITool> tools = resConfig.getTools();
        return getOptions(tools);
    }

    public Object[][] getOptions(IResourceInfo resConfig) {
        List<ITool> tools = resConfig.getTools();
        return getOptions(tools);
    }

    private Object[][] getOptions(List<ITool> tools) {
        return getAllOptions(this);
        //        ITool catTool = this;
        //        ITool tool = null;
        //        for (ITool curTool : tools) {
        //            ITool superTool = curTool;
        //            do {
        //                if (catTool == superTool) {
        //                    tool = curTool;
        //                    break;
        //                }
        //            } while ((superTool = superTool.getSuperClass()) != null);
        //            if (tool != null)
        //                break;
        //        }
        //        // Get all of the tool's options and see which ones are part of
        //        // this category.
        //        if (tool == null)
        //            return null;
        //
        //        return getAllOptions(tool);
    }

    private Object[][] getAllOptions(ITool tool) {
        List<IOption> allOptions = tool.getOptions();
        Object[][] retOptions = new Object[allOptions.size()][2];
        int index = 0;
        for (IOption option : allOptions) {
            IOptionCategory optCat = option.getCategory();
            if (optCat instanceof ITool) {
                //  Determine if the category is this tool or a superclass
                if (optCat == this) {
                    retOptions[index] = new Object[2];
                    retOptions[index][0] = tool;
                    retOptions[index][1] = option;
                    index++;
                }
            }
        }

        return retOptions;
        //       List< IOption> allOptions = tool.getOptions();
        //        Object[][] retOptions = new Object[allOptions.size()][2];
        //        int index = 0;
        //        for (IOption option : allOptions) {
        //            IOptionCategory optCat = option.getCategory();
        //            if (optCat instanceof ITool) {
        //                //  Determine if the category is this tool or a superclass
        //                ITool current = this;
        //                boolean match = false;
        //                do {
        //                    if (optCat == current) {
        //                        match = true;
        //                        break;
        //                    }
        //                } while ((current = current.getSuperClass()) != null);
        //                if (match) {
        //                    retOptions[index] = new Object[2];
        //                    retOptions[index][0] = tool;
        //                    retOptions[index][1] = option;
        //                    index++;
        //                }
        //            }
        //        }
        //
        //        return retOptions;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getTool()
     */
    //@Override
    public ITool getTool() {
        return this;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getName()
     */
    @Override
    public String getName() {
        return name;
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

    private List<String> getInputExtensionsAttribute() {
        //        if ((inputExtensions == null) || (inputExtensions.size() == 0)) {
        //            // If I have a superClass, ask it
        //            if (getSuperClass() != null) {
        //                return ((Tool) getSuperClass()).getInputExtensionsAttribute();
        //            } else {
        //                inputExtensions = new ArrayList<>();
        //            }
        //        }
        return inputExtensions;
    }

    private List<String> getInputExtensionsList() {
        if (inputExtensions == null) {
            inputExtensions = new ArrayList<>();
        }
        return inputExtensions;
    }

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

    public List<String> getInterfaceExtensions() {
        return getHeaderExtensionsAttribute();
    }

    private List<String> getHeaderExtensionsAttribute() {
        //        if (interfaceExtensions == null || interfaceExtensions.size() == 0) {
        //            // If I have a superClass, ask it
        //            if (getSuperClass() != null) {
        //                return ((Tool) getSuperClass()).getHeaderExtensionsAttribute();
        //            } else {
        //                if (interfaceExtensions == null) {
        //                    interfaceExtensions = new ArrayList<>();
        //                }
        //            }
        //        }
        return interfaceExtensions;
    }

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
    public String[] getToolCommandFlags(IPath inputFileLocation, IPath outputFileLocation,
            SupplierBasedCdtVariableSubstitutor macroSubstitutor, IMacroContextInfoProvider provider) {
        List<IOption> opts = getOptions();
        ArrayList<String> flags = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (IOption option : opts) {
            if (option == null)
                continue;
            sb.setLength(0);

            // check to see if the option has an applicability calculator
            IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();
            IOptionCategory cat = option.getCategory();
            IOptionCategoryApplicability catApplicabilityCalculator = cat.getApplicabilityCalculator();

            IBuildObject config = null;
            IBuildObject parent = getParent();
            if (parent instanceof IResourceConfiguration) {
                config = parent;
            } else if (parent instanceof IToolChain) {
                config = ((IToolChain) parent).getParent();
            }

            if ((catApplicabilityCalculator == null
                    || catApplicabilityCalculator.isOptionCategoryVisible(config, this, cat))
                    && (applicabilityCalculator == null
                            || applicabilityCalculator.isOptionUsedInCommandLine(config, this, option))) {

                // update option in case when its value changed.
                // This code is added to fix bug #219684 and
                // avoid using "getOptionToSet()"
                //                if (applicabilityCalculator != null
                //                        && !(applicabilityCalculator instanceof BooleanExpressionApplicabilityCalculator)) {
                //                    if (option.getSuperClass() != null)
                //                        option = getOptionBySuperClassId(option.getSuperClass().getId());
                //                    // bug #405904 - if the option is an extension element (first time we build),
                //                    // use the option id as a superclass id, otherwise we won't find the option we may have just
                //                    // set and will end up with the default setting
                //                    else if (option.isExtensionElement())
                //                        option = getOptionBySuperClassId(option.getId());
                //                    else
                //                        option = getOptionById(option.getId());
                //                }

                try {
                    boolean generateDefaultCommand = true;
                    IOptionCommandGenerator commandGenerator = option.getCommandGenerator();
                    if (commandGenerator != null) {
                        switch (option.getValueType()) {
                        case IOption.BOOLEAN:
                        case IOption.ENUMERATED:
                        case IOption.TREE:
                        case IOption.STRING:
                        case IOption.STRING_LIST:
                        case IOption.INCLUDE_FILES:
                        case IOption.INCLUDE_PATH:
                        case IOption.LIBRARY_PATHS:
                        case IOption.LIBRARY_FILES:
                        case IOption.MACRO_FILES:
                        case IOption.UNDEF_INCLUDE_FILES:
                        case IOption.UNDEF_INCLUDE_PATH:
                        case IOption.UNDEF_LIBRARY_PATHS:
                        case IOption.UNDEF_LIBRARY_FILES:
                        case IOption.UNDEF_MACRO_FILES:
                        case IOption.PREPROCESSOR_SYMBOLS:
                        case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
                            IMacroContextInfo info = provider.getMacroContextInfo(BuildMacroProvider.CONTEXT_FILE,
                                    new FileContextData(inputFileLocation, outputFileLocation, option, this));
                            if (info != null) {
                                macroSubstitutor.setMacroContextInfo(info);
                                String command = commandGenerator.generateCommand(option, macroSubstitutor);
                                if (command != null) {
                                    sb.append(command);
                                    generateDefaultCommand = false;
                                }
                            }
                            break;
                        default:
                            break;
                        }
                    }
                    if (generateDefaultCommand) {
                        switch (option.getValueType()) {
                        case IOption.BOOLEAN:
                            String boolCmd;
                            if (option.getBooleanValue()) {
                                boolCmd = option.getCommand();
                            } else {
                                // Note: getCommandFalse is new with CDT 2.0
                                boolCmd = option.getCommandFalse();
                            }
                            if (boolCmd != null && boolCmd.length() > 0) {
                                sb.append(boolCmd);
                            }
                            break;

                        case IOption.ENUMERATED:
                            String enumVal = option.getEnumCommand(option.getSelectedEnum());
                            if (enumVal.length() > 0) {
                                sb.append(enumVal);
                            }
                            break;

                        case IOption.TREE:
                            String treeVal = option.getCommand(option.getStringValue());
                            if (treeVal.length() > 0) {
                                sb.append(treeVal);
                            }
                            break;

                        case IOption.STRING: {
                            String strCmd = option.getCommand();
                            String val = option.getStringValue();
                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
                                    new FileContextData(inputFileLocation, outputFileLocation, option, this));
                            if (info != null) {
                                macroSubstitutor.setMacroContextInfo(info);
                                if (val.length() > 0
                                        && (val = CdtVariableResolver.resolveToString(val, macroSubstitutor))
                                                .length() > 0) {
                                    sb.append(evaluateCommand(strCmd, val));
                                }
                            }
                        }
                            break;

                        case IOption.STRING_LIST:
                        case IOption.INCLUDE_FILES:
                        case IOption.INCLUDE_PATH:
                        case IOption.LIBRARY_PATHS:
                        case IOption.LIBRARY_FILES:
                        case IOption.MACRO_FILES:
                        case IOption.UNDEF_INCLUDE_FILES:
                        case IOption.UNDEF_INCLUDE_PATH:
                        case IOption.UNDEF_LIBRARY_PATHS:
                        case IOption.UNDEF_LIBRARY_FILES:
                        case IOption.UNDEF_MACRO_FILES: {
                            String listCmd = option.getCommand();
                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
                                    new FileContextData(inputFileLocation, outputFileLocation, option, this));
                            if (info != null) {
                                macroSubstitutor.setMacroContextInfo(info);
                                String[] list = CdtVariableResolver.resolveStringListValues(
                                        option.getBasicStringListValue(), macroSubstitutor, true);
                                if (list != null) {
                                    for (String temp : list) {
                                        if (temp.length() > 0 && !temp.equals(EMPTY_QUOTED_STRING))
                                            sb.append(evaluateCommand(listCmd, temp)).append(BLANK);
                                    }
                                }
                            }
                        }
                            break;

                        case IOption.PREPROCESSOR_SYMBOLS:
                        case IOption.UNDEF_PREPROCESSOR_SYMBOLS: {
                            String defCmd = option.getCommand();
                            IMacroContextInfo info = provider.getMacroContextInfo(IBuildMacroProvider.CONTEXT_FILE,
                                    new FileContextData(inputFileLocation, outputFileLocation, option, this));
                            if (info != null) {
                                macroSubstitutor.setMacroContextInfo(info);
                                String[] symbols = CdtVariableResolver.resolveStringListValues(
                                        option.getBasicStringListValue(), macroSubstitutor, true);
                                if (symbols != null) {
                                    for (String temp : symbols) {
                                        if (temp.length() > 0)
                                            sb.append(evaluateCommand(defCmd, temp) + BLANK);
                                    }
                                }
                            }
                        }
                            break;

                        default:
                            break;
                        }
                    }

                    if (sb.toString().trim().length() > 0)
                        flags.add(sb.toString().trim());

                } catch (BuildException e) {
                    // Bug 315187 one broken option shouldn't cascade to all other options breaking the build...
                    Status s = new Status(IStatus.ERROR, Activator.getId(),
                            MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, option, option.getId()), e);
                    Activator.log(new CoreException(s));
                } catch (CdtVariableException e) {
                    Status s = new Status(IStatus.ERROR, Activator.getId(),
                            MessageFormat.format(Tool_Problem_Discovering_Args_For_Option, option, option.getId()), e);
                    Activator.log(new CoreException(s));
                }
            }
        }
        String[] f = new String[flags.size()];
        return flags.toArray(f);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getToolCommandFlags(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
     */
    @Override
    public String[] getToolCommandFlags(IPath inputFileLocation, IPath outputFileLocation) throws BuildException {
        SupplierBasedCdtVariableSubstitutor macroSubstitutor = new BuildfileMacroSubstitutor(null, EMPTY, BLANK);
        return getToolCommandFlags(inputFileLocation, outputFileLocation, macroSubstitutor,
                BuildMacroProvider.getDefault());
    }

    /**
     * Look for ${VALUE} in the command string
     */
    public String evaluateCommand(String command, String values) {
        final int DOLLAR_VALUE_LENGTH = 8;

        if (command == null)
            return values.trim();

        String ret = command;
        boolean found = false;
        int start = 0;
        int index;
        int len;
        while ((index = ret.indexOf("${", start)) >= 0 && //$NON-NLS-1$
                (len = ret.length()) >= index + DOLLAR_VALUE_LENGTH) {
            start = index;
            index = index + 2;
            int ch = ret.charAt(index);
            if (ch == 'v' || ch == 'V') {
                index++;
                ch = ret.charAt(index);
                if (ch == 'a' || ch == 'A') {
                    index++;
                    ch = ret.charAt(index);
                    if (ch == 'l' || ch == 'L') {
                        index++;
                        ch = ret.charAt(index);
                        if (ch == 'u' || ch == 'U') {
                            index++;
                            ch = ret.charAt(index);
                            if (ch == 'e' || ch == 'E') {
                                index++;
                                ch = ret.charAt(index);
                                if (ch == '}') {
                                    String temp = ""; //$NON-NLS-1$
                                    index++;
                                    found = true;
                                    if (start > 0) {
                                        temp = ret.substring(0, start);
                                    }
                                    temp = temp.concat(values.trim());
                                    if (len > index) {
                                        start = temp.length();
                                        ret = temp.concat(ret.substring(index));
                                        index = start;
                                    } else {
                                        ret = temp;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            start = index;
        }
        if (found)
            return ret.trim();
        return (command + values).trim();
    }

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

    private void addEnvVarBuildPath(IEnvVarBuildPath path) {
        if (path == null)
            return;
        if (envVarBuildPathList == null)
            envVarBuildPathList = new ArrayList<>();

        envVarBuildPathList.add(path);
    }

    public IProject getProject() {
        IBuildObject toolParent = getParent();
        if (toolParent != null) {
            if (toolParent instanceof IToolChain) {
                IConfiguration config = ((IToolChain) toolParent).getParent();
                if (config == null)
                    return null;
                return (IProject) config.getOwner();
            } else if (toolParent instanceof IResourceConfiguration) {
                return (IProject) ((IResourceConfiguration) toolParent).getOwner();
            }
        }
        return null;
    }

    public String[] getContentTypeFileSpecs(IContentType type) {
        return getContentTypeFileSpecs(type, getProject());
    }

    public String[] getContentTypeFileSpecs(IContentType type, IProject project) {
        String[] globalSpecs = type.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
        IContentTypeSettings settings = null;
        //		IProject project = getProject();
        if (project != null) {
            IScopeContext projectScope = new ProjectScope(project);
            try {
                settings = type.getSettings(projectScope);
            } catch (Exception e) {
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

    public boolean supportsLanguageSettings() {
        List<IOption> options = getOptions();
        boolean found = false;
        for (IOption option : options) {
            try {
                int type = option.getValueType();
                if (ManagedBuildManager.optionTypeToEntryKind(type) != 0) {
                    found = true;
                    break;
                }
            } catch (BuildException e) {
            }
        }
        return found;
    }

    @Override
    public CLanguageData[] getCLanguageDatas() {
        //TOFIX JABA don't know what this does
        return null;
        //        initDataMap();
        //        return typeToDataMap.values().toArray(new BuildLanguageData[typeToDataMap.size()]);
    }

    @Override
    public IInputType getInputTypeForCLanguageData(CLanguageData data) {
        if (data instanceof BuildLanguageData)
            return ((BuildLanguageData) data).getInputType();
        return null;
    }

    @Override
    public IResourceInfo getParentResourceInfo() {
        if (parent instanceof IFileInfo)
            return (IResourceInfo) parent;
        //        else if (parent instanceof IToolChain)
        //            return ((IToolChain) parent).getParentFolderInfo();
        return null;
    }

    private BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
        if (booleanExpressionCalculator == null) {
            if (superClass != null) {
                return ((Tool) superClass).getBooleanExpressionCalculator();
            }
        }
        return booleanExpressionCalculator;
    }

    //    @Override
    //    public boolean isEnabled() {
    //        return isEnabled(getParentResourceInfo());
    //    }

    public boolean isEnabled(IResourceInfo rcInfo) {

        BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
        if (calc == null)
            return true;

        return calc.isToolUsedInCommandLine(rcInfo, this);
    }

    private SupportedProperties findSupportedProperties() {
        if (supportedProperties == null) {
            if (superClass != null) {
                return ((Tool) superClass).findSupportedProperties();
            }
        }
        return supportedProperties;
    }

    public String getNameAndVersion() {
        String name = getName();
        String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
        if (version != null && version.length() != 0) {
            return new StringBuilder().append(name).append(" (").append(version).append("").toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return name;
    }

    public IConfigurationElement getConverterModificationElement(ITool toTool) {
        Map<String, IConfigurationElement> map = ManagedBuildManager.getConversionElements(this);
        IConfigurationElement element = null;
        if (!map.isEmpty()) {
            for (IConfigurationElement el : map.values()) {
                String toId = el.getAttribute("toId"); //$NON-NLS-1$
                ITool to = toTool;
                //                if (toId != null) {
                //                    for (; to != null; to = to.getSuperClass()) {
                //                        if (toId.equals(to.getId()))
                //                            break;
                //                    }
                //                }

                if (to != null) {
                    element = el;
                    break;
                }
            }
        }

        return element;
    }

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
        String name = getName();
        if (name == null) {
            name = getId();
        } else {
            String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
            if (version != null) {
                StringBuilder buf = new StringBuilder();
                buf.append(name);
                buf.append(" (v").append(version).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
                name = buf.toString();
            }
        }
        return name;
    }

    //    public String getDiscoveryProfileIdAttribute() {
    //        if (scannerConfigDiscoveryProfileId == null && superClass != null)
    //            return ((Tool) superClass).getDiscoveryProfileIdAttribute();
    //        return scannerConfigDiscoveryProfileId;
    //    }

    private IToolChain getToolChain() {
        IBuildObject bo = getParent();
        IToolChain tCh = null;
        if (bo instanceof IToolChain) {
            tCh = ((IToolChain) bo);
        } else if (bo instanceof IFileInfo) {
            tCh = ((ResourceConfiguration) bo).getBaseToolChain();
        }
        return tCh;
    }

    //    public String getDiscoveryProfileId() {
    //        String id = getDiscoveryProfileIdAttribute();
    //        if (id == null) {
    //            IToolChain tc = getToolChain();
    //            if (tc != null)
    //                id = tc.getScannerConfigDiscoveryProfileId();
    //        }
    //        return id;
    //    }

    private boolean isAnyOptionModified(ITool t1, ITool t2) {
        for (IOption op1 : t1.getOptions()) {
            for (IOption op2 : t2.getOptions()) {
                // find matching option
                try {
                    if (op1.getValueType() == op2.getValueType() && op1.getName() != null
                            && op1.getName().equals(op2.getName())) {
                        Object ob1 = op1.getValue();
                        Object ob2 = op2.getValue();
                        if (ob1 == null && ob2 == null)
                            break;
                        // values are different ?
                        if ((ob1 == null || ob2 == null) || !(ob1.equals(ob2)))
                            return true;
                        else
                            break;
                    }
                } catch (BuildException e) {
                    return true; // unprobable
                }
            }
        }
        return false;
    }

    public IOption[] getOptionsOfType(int type) {
        List<IOption> list = new ArrayList<>();
        for (IOption op : getOptions()) {
            try {
                if (op.getValueType() == type)
                    list.add(op);
            } catch (BuildException e) {
                Activator.log(e);
            }
        }
        return list.toArray(new Option[list.size()]);
    }

    @Override
    public IOptionCategoryApplicability getApplicabilityCalculator() {
        // Tool does not have any ApplicabilityCalculator.
        return null;
    }

    @Override
    public List<String> getExtraFlags(int optionType) {
        List<String> flags = new LinkedList<>();
        if (optionType != IOption.LIBRARIES && optionType != IOption.OBJECTS) {
            // Early exit to avoid performance penalty
            return flags;
        }

        for (IOption option : getOptions()) {
            try {
                if (option.getValueType() != optionType) {
                    continue;
                }

                // check to see if the option has an applicability calculator
                IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();

                if (applicabilityCalculator == null
                        || applicabilityCalculator.isOptionUsedInCommandLine(this, this, option)) {
                    boolean generateDefaultCommand = true;
                    IOptionCommandGenerator commandGenerator = option.getCommandGenerator();
                    if (commandGenerator != null) {
                        SupplierBasedCdtVariableSubstitutor macroSubstitutor = new BuildfileMacroSubstitutor(null,
                                EMPTY, BLANK);
                        IMacroContextInfoProvider provider = BuildMacroProvider.getDefault();
                        IMacroContextInfo info = provider.getMacroContextInfo(BuildMacroProvider.CONTEXT_OPTION,
                                new OptionContextData(option, this));
                        if (info != null) {
                            macroSubstitutor.setMacroContextInfo(info);
                            String command = commandGenerator.generateCommand(option, macroSubstitutor);
                            if (command != null) {
                                flags.add(command);
                                generateDefaultCommand = false;
                            }
                        }
                    }

                    if (generateDefaultCommand) {
                        switch (optionType) {
                        case IOption.LIBRARIES: {
                            String command = option.getCommand();
                            String[] libs = option.getLibraries();
                            for (String lib : libs) {
                                try {
                                    String resolved[] = ManagedBuildManager.getBuildMacroProvider()
                                            .resolveStringListValueToMakefileFormat(lib, " ", //$NON-NLS-1$
                                                    " ", //$NON-NLS-1$
                                                    IBuildMacroProvider.CONTEXT_OPTION,
                                                    new OptionContextData(option, this));
                                    if (resolved != null && resolved.length > 0) {
                                        for (String string : resolved) {
                                            if (!string.isEmpty()) {
                                                flags.add(command + string);
                                            }
                                        }
                                    }
                                } catch (BuildMacroException e) {
                                    // TODO: report error
                                    continue;
                                }
                            }
                            break;
                        }
                        case IOption.OBJECTS: {
                            String userObjs[] = option.getUserObjects();
                            if (userObjs != null && userObjs.length > 0) {
                                for (String userObj : userObjs) {
                                    try {
                                        String resolved[] = ManagedBuildManager.getBuildMacroProvider()
                                                .resolveStringListValueToMakefileFormat(userObj, "", //$NON-NLS-1$
                                                        " ", //$NON-NLS-1$
                                                        IBuildMacroProvider.CONTEXT_OPTION,
                                                        new OptionContextData(option, this));
                                        if (resolved != null && resolved.length > 0) {
                                            flags.addAll(Arrays.asList(resolved));
                                        }
                                    } catch (BuildMacroException e) {
                                        // TODO: report error
                                        continue;
                                    }
                                }
                            }
                            break;
                        }
                        default:
                            // Cannot happen
                            break;
                        }
                    }
                }
            } catch (BuildException | CdtVariableException e) {
                // TODO: report error
                continue;
            }
        }
        return flags;
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
    public List<IInputType> getMatchingInputTypes(IFile file, String macroName) {
        String safeMacroName = macroName;
        if (macroName == null) {
            safeMacroName = new String();
        }
        List<IInputType> ret = new LinkedList<>();
        for (InputType inputType : inputTypeMap.values()) {
            if (inputType.isAssociatedWith(file)) {
                ret.add(inputType);
            } else {
                if (safeMacroName.equals(inputType.getAssignToOptionId())) {
                    ret.add(inputType);
                }
            }
        }
        return ret;
    }

    @Override
    public IInputType getInputTypeByID(String id) {
        return inputTypeMap.get(id);
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