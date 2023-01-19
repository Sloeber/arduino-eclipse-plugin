/*******************************************************************************
 * Copyright (c) 2004, 2020 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 * IBM Corporation
 * James Blackburn (Broadcom Corp.)
 *******************************************************************************/
package io.sloeber.schema.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CommandLauncherManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.settings.model.COutputEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICOutputEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.core.settings.model.util.LanguageSettingEntriesSerializer;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.Internal.BuildMacroProvider;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IFileContextBuildMacroValues;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.BuildRunner;
import io.sloeber.autoBuild.extensionPoint.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.extensionPoint.IOptionCategoryApplicability;
import io.sloeber.autoBuild.extensionPoint.IReservedMacroNameSupplier;
import io.sloeber.autoBuild.integration.BuildBuildData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.IToolChain;

public class Builder extends HoldsOptions implements IBuilder {
    public static final int UNLIMITED_JOBS = Integer.MAX_VALUE;
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    static final String VALUE_UNLIMITED = "unlimited"; //$NON-NLS-1$
    static final String PARALLEL_PATTERN_NUM = "*"; //$NON-NLS-1$
    static final String PARALLEL_PATTERN_NUM_START = "["; //$NON-NLS-1$
    static final String PARALLEL_PATTERN_NUM_END = "]"; //$NON-NLS-1$
    static final String VALUE_OPTIMAL = "optimal"; //$NON-NLS-1$
    static final String DEFAULT_TARGET_CLEAN = "clean"; //$NON-NLS-1$
    static final String DEFAULT_TARGET_INCREMENTAL = "all"; //$NON-NLS-1$
    static final String DEFAULT_TARGET_AUTO = "all"; //$NON-NLS-1$

    String[] modelsAbstract;
    String[] modelcommand;
    String[] modelarguments;
    String[] modelbuildfileGenerator;
    String[] modelvariableFormat;
    String[] modelreservedMacroNames;
    String[] modelreservedMacro;
    String[] modelsupportsManagedBuild;
    String[] modelautoBuildTarget;
    String[] modelincrementalBuildTarget;
    String[] modelcleanBuildTarget;
    String[] modelignoreErrCmd;
    String[] modelparallelBuildCmd;
    String[] modelparallelBuildOn;
    String[] modelparallelizationNumber;
    String[] modelisSystem;
    String[] modelcommandLauncher;
    String[] modelbuildRunner;

    //  Parent and children
    private IToolChain parent;
    //  Managed Build model attributes
    private String errorParserIds;
    private Boolean isAbstract;
    private String command;
    private String args;
    private String versionsSupported;
    private String convertToId;
    //    private FileContextBuildMacroValues fileContextBuildMacroValues;
    private String builderVariablePattern;
    private IReservedMacroNameSupplier reservedMacroNameSupplier;
    private IConfigurationElement reservedMacroNameSupplierElement;

    //custom builder settings
    private HashMap<String, String> customizedEnvironment;
    private IFolder myBuildFolder;
    private HashMap<String, String> customBuildProperties;
    // parallelization
    private Boolean isParallelBuildEnabled;
    private Integer parallelNumberAttribute; // negative number denotes "optimal" value, see getOptimalParallelJobNum()

    private boolean isTest;

    //  Miscellaneous

    private IConfigurationElement previousMbsVersionConversionElement = null;
    private IConfigurationElement currentMbsVersionConversionElement = null;

    private BuildBuildData fBuildData;

    private Boolean fSupportsCustomizedBuild;

    private List<Builder> identicalList;

    private ICOutputEntry[] outputEntries;

    private IConfigurationElement fCommandLauncherElement = null;

    private IBuildRunner fBuildRunner = null;
    private IConfigurationElement fBuildRunnerElement = null;
    private String[] reservedMacroNames;
    private IMakefileGenerator buildFileGeneratorElement;

    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create a builder defined by an extension point
     * in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The IToolChain parent of this builder, or <code>null</code> if
     *            defined at the top level
     * @param element
     *            The builder definition from the manifest file or a dynamic element
     *            provider
     * @param managedBuildRevision
     *            The fileVersion of Managed Buid System
     */
    public Builder(IToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;
        loadNameAndID(root, element);

        modelsAbstract = getAttributes(IS_ABSTRACT);
        modelcommand = getAttributes(COMMAND);
        modelarguments = getAttributes(ARGUMENTS);
        modelbuildfileGenerator = getAttributes(MAKEGEN_ID);
        modelvariableFormat = getAttributes(VARIABLE_FORMAT);
        modelreservedMacroNames = getAttributes(RESERVED_MACRO_NAMES);
        modelreservedMacro = getAttributes(RESERVED_MACRO_NAME_SUPPLIER);
        modelsupportsManagedBuild = getAttributes(ATTRIBUTE_SUPORTS_MANAGED_BUILD);
        modelautoBuildTarget = getAttributes(ATTRIBUTE_TARGET_AUTO);
        modelincrementalBuildTarget = getAttributes(ATTRIBUTE_TARGET_INCREMENTAL);
        modelcleanBuildTarget = getAttributes(ATTRIBUTE_TARGET_CLEAN);
        modelignoreErrCmd = getAttributes(ATTRIBUTE_IGNORE_ERR_CMD);
        modelparallelBuildCmd = getAttributes(ATTRIBUTE_PARALLEL_BUILD_CMD);
        modelparallelBuildOn = getAttributes(ATTRIBUTE_PARALLEL_BUILD_ON);
        modelparallelizationNumber = getAttributes(ATTRIBUTE_PARALLELIZATION_NUMBER);
        modelisSystem = getAttributes(IS_SYSTEM);
        modelcommandLauncher = getAttributes(ATTRIBUTE_COMMAND_LAUNCHER);
        modelbuildRunner = getAttributes(ATTRIBUTE_BUILD_RUNNER);

        IConfigurationElement[] optionElements = element.getChildren(IHoldsOptions.OPTION);
        for (IConfigurationElement optionElement : optionElements) {
            Option newOption = new Option(this, root, optionElement);
            myOptionMap.put(newOption.getName(), newOption);
        }

        buildFileGeneratorElement = (IMakefileGenerator) createExecutableExtension(MAKEGEN_ID);

        resolveFields();

    }

    /**
     * Create a <code>Builder</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>IToolChain</code> the Builder will be added to.
     * @param element
     *            The XML element that contains the Builder settings.
     * @param managedBuildRevision
     *            The fileVersion of Managed Buid System
     */
    public Builder(IToolChain parent, ICStorageElement element, String managedBuildRevision) {
        //        this.parent = parent;
        //        isExtensionBuilder = false;
        //
        //        fBuildData = new BuildBuildData(this);
        //
        //        // Set the managedBuildRevision
        //        setManagedBuildRevision(managedBuildRevision);
        //
        //        // Initialize from the XML attributes
        //        loadFromProject(element);
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> cloneMap(HashMap<String, String> map) {
        return (HashMap<String, String>) map.clone();
    }

    /**
     * Create a <code>Builder</code> based upon an existing builder.
     *
     * @param parent
     *            The <code>IToolChain</code> the builder will be added to.
     * @param builder
     *            The existing builder to clone.
     */
    public Builder(IToolChain parent, String Id, String name, Builder builder) {
        //        this.parent = parent;
        //
        //        superClass = builder.superClass;
        //        if (superClass != null && builder.superClassId != null) {
        //            superClassId = builder.superClassId;
        //        }
        //
        //        setId(Id);
        //        setName(name);
        //
        //        // Set the managedBuildRevision & the version
        //        setManagedBuildRevision(builder.getManagedBuildRevision());
        //        setVersion(getVersionFromId());
        //
        //        isExtensionBuilder = false;
        //
        //        //  Copy the remaining attributes
        //        if (builder.versionsSupported != null) {
        //            versionsSupported = builder.versionsSupported;
        //        }
        //        if (builder.convertToId != null) {
        //            convertToId = builder.convertToId;
        //        }
        //        if (builder.errorParserIds != null) {
        //            errorParserIds = builder.errorParserIds;
        //        }
        //        if (builder.isAbstract != null) {
        //            isAbstract = builder.isAbstract;
        //        }
        //        if (builder.command != null) {
        //            command = builder.command;
        //        }
        //        if (builder.args != null) {
        //            args = builder.args;
        //        }
        //        autoBuildTarget = builder.autoBuildTarget;
        //        autoBuildEnabled = builder.autoBuildEnabled;
        //        incrementalBuildTarget = builder.incrementalBuildTarget;
        //        incrementalBuildEnabled = builder.incrementalBuildEnabled;
        //        cleanBuildTarget = builder.cleanBuildTarget;
        //        cleanBuildEnabled = builder.cleanBuildEnabled;
        //        managedBuildOn = builder.managedBuildOn;
        //        keepEnvVarInBuildfile = builder.keepEnvVarInBuildfile;
        //        supportsManagedBuild = builder.supportsManagedBuild;
        //        if (builder.customizedErrorParserIds != null)
        //            customizedErrorParserIds = builder.customizedErrorParserIds.clone();
        //        if (builder.customizedEnvironment != null)
        //            customizedEnvironment = cloneMap(builder.customizedEnvironment);
        //        appendEnvironment = builder.appendEnvironment;
        //        myBuildFolder = builder.myBuildFolder;
        //        if (builder.customBuildProperties != null)
        //            customBuildProperties = cloneMap(builder.customBuildProperties);
        //
        //        buildFileGeneratorElement = builder.buildFileGeneratorElement;
        //
        //        //        if (builder.fileContextBuildMacroValues != null) {
        //        //            fileContextBuildMacroValues = (FileContextBuildMacroValues) builder.fileContextBuildMacroValues.clone();
        //        //            fileContextBuildMacroValues.setBuilder(this);
        //        //        }
        //
        //        builderVariablePattern = builder.builderVariablePattern;
        //
        //        if (builder.reservedMacroNames != null)
        //            reservedMacroNames = builder.reservedMacroNames.clone();
        //
        //        reservedMacroNameSupplierElement = builder.reservedMacroNameSupplierElement;
        //        reservedMacroNameSupplier = builder.reservedMacroNameSupplier;
        //
        //        fBuildData = new BuildBuildData(this);
        //
        //        stopOnErr = builder.stopOnErr;
        //        ignoreErrCmd = builder.ignoreErrCmd;
        //
        //        isParallelBuildEnabled = builder.isParallelBuildEnabled;
        //        parallelNumberAttribute = builder.parallelNumberAttribute;
        //        parallelBuildCmd = builder.parallelBuildCmd;
        //
        //        if (builder.outputEntries != null) {
        //            outputEntries = builder.outputEntries.clone();
        //        }
        //
        //        super.copyChildren(builder);
        //
        //        fCommandLauncherElement = builder.fCommandLauncherElement;
        //
        //        fBuildRunner = builder.fBuildRunner;
        //        fBuildRunnerElement = builder.fBuildRunnerElement;
    }

    public void copySettings(Builder builder, boolean allBuildSettings) {
        //        try {
        //            if (isAutoBuildEnable() != builder.isAutoBuildEnable())
        //                setAutoBuildEnable(builder.isAutoBuildEnable());
        //        } catch (CoreException e) {
        //        }
        //        try {
        //            if (isIncrementalBuildEnabled() != builder.isIncrementalBuildEnabled())
        //                setIncrementalBuildEnable(builder.isIncrementalBuildEnabled());
        //        } catch (CoreException e) {
        //        }
        //        try {
        //            if (isFullBuildEnabled() != builder.isFullBuildEnabled())
        //                setFullBuildEnable(builder.isFullBuildEnabled());
        //        } catch (CoreException e) {
        //        }
        //        try {
        //            if (isCleanBuildEnabled() != builder.isCleanBuildEnabled())
        //                setCleanBuildEnable(builder.isCleanBuildEnabled());
        //        } catch (CoreException e) {
        //        }
        //        if (isStopOnError() != builder.isStopOnError() && supportsStopOnError(builder.isStopOnError())) {
        //            try {
        //                setStopOnError(builder.isStopOnError());
        //            } catch (CoreException e) {
        //            }
        //        }
        //        if (isParallelBuildOn() != builder.isParallelBuildOn() && supportsParallelBuild()) {
        //            try {
        //                setParallelBuildOn(builder.isParallelBuildOn());
        //            } catch (CoreException e) {
        //            }
        //        }
        //        if (getParallelizationNumAttribute() != builder.getParallelizationNumAttribute() && supportsParallelBuild()) {
        //            try {
        //                setParallelizationNum(builder.getParallelizationNumAttribute());
        //            } catch (CoreException e) {
        //            }
        //        }
        //        if (builder.keepEnvironmentVariablesInBuildfile() && canKeepEnvironmentVariablesInBuildfile()) {
        //            setKeepEnvironmentVariablesInBuildfile(builder.keepEnvironmentVariablesInBuildfile());
        //        }
        //        if (isManagedBuildOn() != builder.isManagedBuildOn() && supportsBuild(builder.isManagedBuildOn())) {
        //            setManagedBuildOn(builder.isManagedBuildOn());
        //        }
        //
        //        if (builder.customizedErrorParserIds != null)
        //            customizedErrorParserIds = builder.customizedErrorParserIds.clone();
        //        if (builder.customizedEnvironment != null)
        //            customizedEnvironment = cloneMap(builder.customizedEnvironment);
        //        appendEnvironment = builder.appendEnvironment;
        //
        //        if (builder.customBuildProperties != null)
        //            customBuildProperties = cloneMap(builder.customBuildProperties);
        //
        //        if (allBuildSettings) {
        //            if (!getCommand().equals(builder.getCommand()))
        //                setCommand(builder.getCommand());
        //            if (!getArgumentsAttribute().equals(builder.getArgumentsAttribute()))
        //                setArgumentsAttribute(builder.getArgumentsAttribute());
        //            if (!Objects.equals(getAutoBuildTargetAttribute(), builder.getAutoBuildTargetAttribute())) {
        //                autoBuildTarget = builder.getAutoBuildTargetAttribute();
        //            }
        //            if (!Objects.equals(getIncrementalBuildTargetAttribute(), builder.getIncrementalBuildTargetAttribute())) {
        //                incrementalBuildTarget = builder.getIncrementalBuildTargetAttribute();
        //            }
        //            if (!Objects.equals(getCleanBuildTargetAttribute(), builder.getCleanBuildTargetAttribute())) {
        //                cleanBuildTarget = builder.getCleanBuildTargetAttribute();
        //            }
        //        }

    }

    /*	public Builder(IToolChain parent, String Id, String name, Builder builder, ICStorageElement el) {
    		this(parent, Id, name, builder);
    		loadFromProject(el);
    	}
    */

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /**
     * Loads the builder information from the ManagedConfigElement specified in the
     * argument.
     *
     * @param element
     *            Contains the Builder information
     */

    private String encodeParallelizationNumber(Integer jobsNumber) {
        if (jobsNumber <= 0)
            return VALUE_OPTIMAL;

        if (jobsNumber.equals(UNLIMITED_JOBS))
            return VALUE_UNLIMITED;

        return jobsNumber.toString();
    }

    private int decodeParallelizationNumber(String value) {
        int parallelNumber = -1;
        if (value == null || VALUE_OPTIMAL.equals(value)) {
            parallelNumber = -getOptimalParallelJobNum();
        } else if (VALUE_UNLIMITED.equals(value)) {
            parallelNumber = UNLIMITED_JOBS;
        } else {
            try {
                parallelNumber = Integer.decode(value);
                if (parallelNumber <= 0) {
                    // unlimited for External Builder
                    parallelNumber = UNLIMITED_JOBS;
                }
            } catch (NumberFormatException e) {
                Activator.log(e);
                // default to "optimal" if not recognized
                parallelNumber = -getOptimalParallelJobNum();
            }
        }
        return parallelNumber;
    }

    public void resolveFields() {

        reservedMacroNames = modelreservedMacroNames[SUPER].split(","); //$NON-NLS-1$
        isAbstract = Boolean.parseBoolean(modelsAbstract[SUPER]);

        isParallelBuildEnabled = Boolean.valueOf(modelparallelBuildOn[SUPER]);
        if (isParallelBuildEnabled) {
            setParallelizationNumAttribute(decodeParallelizationNumber(modelparallelizationNumber[SUPER]));
        }
        isTest = Boolean.parseBoolean(modelisSystem[SUPER]);

    }

    /**
     * Initialize the builder information from the XML element
     * specified in the argument
     *
     * @param element
     *            An XML element containing the builder information
     */
    protected void loadFromProject(ICStorageElement element) {
        //        Map<String, String> attributes = new HashMap<>();
        //        attributes.put(IBuildObject.ID, element.getAttribute(IBuildObject.ID));
        //        attributes.put(IBuildObject.NAME, element.getAttribute(IBuildObject.NAME));
        //        attributes.put(IProjectType.SUPERCLASS, element.getAttribute(IProjectType.SUPERCLASS));
        //        attributes.put(IS_ABSTRACT, element.getAttribute(IS_ABSTRACT));
        //        attributes.put(IToolChain.ERROR_PARSERS, element.getAttribute(IToolChain.ERROR_PARSERS));
        //        attributes.put(IBuilder.COMMAND, element.getAttribute(IBuilder.COMMAND));
        //        attributes.put(IBuilder.ARGUMENTS, element.getAttribute(IBuilder.ARGUMENTS));
        //        attributes.put(VERSIONS_SUPPORTED, element.getAttribute(VERSIONS_SUPPORTED));
        //        attributes.put(CONVERT_TO_ID, element.getAttribute(CONVERT_TO_ID));
        //        attributes.put(VARIABLE_FORMAT, element.getAttribute(VARIABLE_FORMAT));
        //        attributes.put(RESERVED_MACRO_NAMES, element.getAttribute(RESERVED_MACRO_NAMES));
        //        attributes.put(RESERVED_MACRO_NAME_SUPPLIER, element.getAttribute(RESERVED_MACRO_NAME_SUPPLIER));
        //        attributes.put(ATTRIBUTE_TARGET_AUTO, element.getAttribute(ATTRIBUTE_TARGET_AUTO));
        //        attributes.put(ATTRIBUTE_AUTO_ENABLED, element.getAttribute(ATTRIBUTE_AUTO_ENABLED));
        //        attributes.put(ATTRIBUTE_TARGET_INCREMENTAL, element.getAttribute(ATTRIBUTE_TARGET_INCREMENTAL));
        //        attributes.put(ATTRIBUTE_TARGET_CLEAN, element.getAttribute(ATTRIBUTE_TARGET_CLEAN));
        //        attributes.put(ATTRIBUTE_IGNORE_ERR_CMD, element.getAttribute(ATTRIBUTE_IGNORE_ERR_CMD));
        //
        //        attributes.put(ATTRIBUTE_BUILD_RUNNER, element.getAttribute(ATTRIBUTE_BUILD_RUNNER));
        //        attributes.put(ATTRIBUTE_COMMAND_LAUNCHER, element.getAttribute(ATTRIBUTE_COMMAND_LAUNCHER));
        //        attributes.put(IS_SYSTEM, element.getAttribute(IS_SYSTEM));
        //        attributes.put(ATTRIBUTE_PARALLELIZATION_NUMBER, element.getAttribute(ATTRIBUTE_PARALLELIZATION_NUMBER));
        //        attributes.put(ATTRIBUTE_PARALLEL_BUILD_ON, element.getAttribute(ATTRIBUTE_PARALLEL_BUILD_ON));
        //        attributes.put(ATTRIBUTE_PARALLEL_BUILD_CMD, element.getAttribute(ATTRIBUTE_PARALLEL_BUILD_CMD));
        //        attributes.put(ATTRIBUTE_STOP_ON_ERR, element.getAttribute(ATTRIBUTE_STOP_ON_ERR));
        //        attributes.put(ATTRIBUTE_CUSTOM_PROPS, element.getAttribute(ATTRIBUTE_CUSTOM_PROPS));
        //        attributes.put(ATTRIBUTE_APPEND_ENVIRONMENT, element.getAttribute(ATTRIBUTE_APPEND_ENVIRONMENT));
        //        attributes.put(ATTRIBUTE_ENVIRONMENT, element.getAttribute(ATTRIBUTE_ENVIRONMENT));
        //        attributes.put(ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS, element.getAttribute(ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS));
        //        attributes.put(ATTRIBUTE_SUPORTS_MANAGED_BUILD, element.getAttribute(ATTRIBUTE_SUPORTS_MANAGED_BUILD));
        //        attributes.put(ATTRIBUTE_KEEP_ENV, element.getAttribute(ATTRIBUTE_KEEP_ENV));
        //        attributes.put(ATTRIBUTE_MANAGED_BUILD_ON, element.getAttribute(ATTRIBUTE_MANAGED_BUILD_ON));
        //        attributes.put(ATTRIBUTE_CLEAN_ENABLED, element.getAttribute(ATTRIBUTE_CLEAN_ENABLED));
        //        attributes.put(ATTRIBUTE_AUTO_ENABLED, element.getAttribute(ATTRIBUTE_AUTO_ENABLED));
        //        attributes.put(BUILDFILEGEN_ID, element.getAttribute(BUILDFILEGEN_ID));
        //
        //        loadFromMap(attributes, null);
        //
        //        if (superClassId != null && superClassId.length() > 0) {
        //            superClass = ManagedBuildManager.getExtensionBuilder(superClassId);
        //            // Check for migration support
        //            checkForMigrationSupport();
        //        }
        //
        //        ICStorageElement[] children = element.getChildren();
        //        for (int i = 0; i < children.length; i++) {
        //            ICStorageElement child = children[i];
        //            if (loadChild(child)) {
        //                // nothing
        //            } else {
        //                String name = child.getName();
        //                if (OUTPUT_ENTRIES.equals(name)) {
        //                    ICSettingEntry entries[] = LanguageSettingEntriesSerializer.loadEntries(child);
        //                    if (entries.length == 0) {
        //                        outputEntries = new ICOutputEntry[0];
        //                    } else {
        //                        List<ICSettingEntry> list = new ArrayList<>(entries.length);
        //                        for (int k = 0; k < entries.length; k++) {
        //                            if (entries[k].getKind() == ICLanguageSettingEntry.OUTPUT_PATH)
        //                                list.add(entries[k]);
        //                        }
        //                        outputEntries = list.toArray(new ICOutputEntry[list.size()]);
        //                    }
        //                }
        //            }
        //        }

    }

    /**
     * Persist the builder to the project file.
     */
    public void serialize(ICStorageElement element, boolean resetDirtyState) {
        //        if (superClass != null)
        //            element.setAttribute(SUPERCLASS, superClass.getId());
        //
        //        element.setAttribute(IBuildObject.ID, id);
        //
        //        if (name != null) {
        //            element.setAttribute(IBuildObject.NAME, name);
        //        }
        //
        //        if (isAbstract != null) {
        //            element.setAttribute(IS_ABSTRACT, isAbstract.toString());
        //        }
        //
        //
        //        if (errorParserIds != null) {
        //            element.setAttribute(IToolChain.ERROR_PARSERS, errorParserIds);
        //        }
        //
        //        if (command != null) {
        //            element.setAttribute(IBuilder.COMMAND, command);
        //        }
        //
        //        if (args != null) {
        //            element.setAttribute(IBuilder.ARGUMENTS, args);
        //        }
        //
        //        if (autoBuildTarget != null)
        //            element.setAttribute(ATTRIBUTE_TARGET_AUTO, autoBuildTarget);
        //        if (autoBuildEnabled != null)
        //            element.setAttribute(ATTRIBUTE_AUTO_ENABLED, autoBuildEnabled.toString());
        //        if (incrementalBuildTarget != null)
        //            element.setAttribute(ATTRIBUTE_TARGET_INCREMENTAL, incrementalBuildTarget);
        //        if (incrementalBuildEnabled != null)
        //            element.setAttribute(ATTRIBUTE_INCREMENTAL_ENABLED, incrementalBuildEnabled.toString());
        //        if (cleanBuildTarget != null)
        //            element.setAttribute(ATTRIBUTE_TARGET_CLEAN, cleanBuildTarget);
        //        if (cleanBuildEnabled != null)
        //            element.setAttribute(ATTRIBUTE_CLEAN_ENABLED, cleanBuildEnabled.toString());
        //        if (managedBuildOn != null)
        //            element.setAttribute(ATTRIBUTE_MANAGED_BUILD_ON, managedBuildOn.toString());
        //        if (keepEnvVarInBuildfile != null)
        //            element.setAttribute(ATTRIBUTE_KEEP_ENV, keepEnvVarInBuildfile.toString());
        //        if (supportsManagedBuild != null)
        //            element.setAttribute(ATTRIBUTE_SUPORTS_MANAGED_BUILD, supportsManagedBuild.toString());
        //        if (customizedErrorParserIds != null)
        //            element.setAttribute(ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS,
        //                    CDataUtil.arrayToString(customizedErrorParserIds, ";")); //$NON-NLS-1$
        //        //        if (customizedEnvironment != null)
        //        //            element.setAttribute(ATTRIBUTE_ENVIRONMENT, MapStorageElement.encodeMap(customizedEnvironment));
        //        if (appendEnvironment != null)
        //            element.setAttribute(ATTRIBUTE_APPEND_ENVIRONMENT, appendEnvironment.toString());
        //
        //        if (ignoreErrCmd != null)
        //            element.setAttribute(ATTRIBUTE_IGNORE_ERR_CMD, ignoreErrCmd);
        //        if (stopOnErr != null)
        //            element.setAttribute(ATTRIBUTE_STOP_ON_ERR, stopOnErr.toString());
        //
        //        if (parallelBuildCmd != null)
        //            element.setAttribute(ATTRIBUTE_PARALLEL_BUILD_CMD, parallelBuildCmd);
        //
        //        if (isParallelBuildEnabled != null)
        //            element.setAttribute(ATTRIBUTE_PARALLEL_BUILD_ON, isParallelBuildEnabled.toString());
        //        if (isParallelBuildOn() && parallelNumberAttribute != null)
        //            element.setAttribute(ATTRIBUTE_PARALLELIZATION_NUMBER,
        //                    encodeParallelizationNumber(parallelNumberAttribute));
        //
        //        // Note: build file generator cannot be specified in a project file because
        //        //       an IConfigurationElement is needed to load it!
        //        if (buildFileGeneratorElement != null) {
        //            //  TODO:  issue warning?
        //        }
        //
        //        // options
        //        //        try {
        //        //            super.serialize(element);
        //        //        } catch (BuildException e) {
        //        //            Activator.log(e);
        //        //        }
        //
        //        if (outputEntries != null) {
        //            ICStorageElement outEl = element.createChild(OUTPUT_ENTRIES);
        //            LanguageSettingEntriesSerializer.serializeEntries(outputEntries, outEl);
        //        }

    }

    public void serializeRawData(ICStorageElement element) {
        //        if (superClass != null)
        //            element.setAttribute(IProjectType.SUPERCLASS, superClass.getId());
        //
        //        element.setAttribute(IBuildObject.ID, id);
        //
        //        if (getName() != null) {
        //            element.setAttribute(IBuildObject.NAME, getName());
        //        }
        //
        //        if (isAbstract != null) {
        //            element.setAttribute(IS_ABSTRACT, isAbstract.toString());
        //        }
        //
        //        // versionsSupported
        //        if (versionsSupported != null) {
        //            element.setAttribute(VERSIONS_SUPPORTED, versionsSupported);
        //        }
        //
        //        // convertToId
        //        if (convertToId != null) {
        //            element.setAttribute(CONVERT_TO_ID, convertToId);
        //        }
        //
        //        if (getErrorParserIds() != null) {
        //            element.setAttribute(IToolChain.ERROR_PARSERS, getErrorParserIds());
        //        }
        //
        //        if (getCommand() != null) {
        //            element.setAttribute(IBuilder.COMMAND, getCommand());
        //        }
        //
        //        if (getArgumentsAttribute() != null) {
        //            element.setAttribute(IBuilder.ARGUMENTS, getArguments/*Attribute*/());
        //        }
        //
        //        if (getAutoBuildTargetAttribute() != null)
        //            element.setAttribute(ATTRIBUTE_TARGET_AUTO, getAutoBuildTargetAttribute());
        //        element.setAttribute(ATTRIBUTE_AUTO_ENABLED, String.valueOf(isAutoBuildEnable()));
        //        if (getIncrementalBuildTargetAttribute() != null)
        //            element.setAttribute(ATTRIBUTE_TARGET_INCREMENTAL, getIncrementalBuildTargetAttribute());
        //        element.setAttribute(ATTRIBUTE_INCREMENTAL_ENABLED, String.valueOf(isIncrementalBuildEnabled()));
        //        if (getCleanBuildTargetAttribute() != null)
        //            element.setAttribute(ATTRIBUTE_TARGET_CLEAN, getCleanBuildTargetAttribute());
        //        element.setAttribute(ATTRIBUTE_CLEAN_ENABLED, String.valueOf(isCleanBuildEnabled()));
        //        element.setAttribute(ATTRIBUTE_MANAGED_BUILD_ON, String.valueOf(isManagedBuildOn()));
        //        element.setAttribute(ATTRIBUTE_KEEP_ENV, String.valueOf(keepEnvironmentVariablesInBuildfile()));
        //        element.setAttribute(ATTRIBUTE_SUPORTS_MANAGED_BUILD, String.valueOf(supportsBuild(true)));
        //        if (customizedErrorParserIds != null)
        //            element.setAttribute(ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS,
        //                    CDataUtil.arrayToString(customizedErrorParserIds, ";")); //$NON-NLS-1$
        //        //        if (customizedEnvironment != null)
        //        //            element.setAttribute(ATTRIBUTE_ENVIRONMENT, MapStorageElement.encodeMap(customizedEnvironment));
        //        element.setAttribute(ATTRIBUTE_APPEND_ENVIRONMENT, String.valueOf(appendEnvironment()));
        //        if (getIgnoreErrCmdAttribute() != null)
        //            element.setAttribute(ATTRIBUTE_IGNORE_ERR_CMD, getIgnoreErrCmdAttribute());
        //        element.setAttribute(ATTRIBUTE_STOP_ON_ERR, String.valueOf(isStopOnError()));
        //
        //        if (parallelBuildCmd != null)
        //            element.setAttribute(ATTRIBUTE_PARALLEL_BUILD_CMD, parallelBuildCmd);
        //
        //        if (isParallelBuildEnabled != null)
        //            element.setAttribute(ATTRIBUTE_PARALLEL_BUILD_ON, isParallelBuildEnabled.toString());
        //        if (isParallelBuildOn() && parallelNumberAttribute != null)
        //            element.setAttribute(ATTRIBUTE_PARALLELIZATION_NUMBER,
        //                    encodeParallelizationNumber(parallelNumberAttribute));
        //
        //        // Note: build file generator cannot be specified in a project file because
        //        //       an IConfigurationElement is needed to load it!
        //        if (buildFileGeneratorElement != null) {
        //            //  TODO:  issue warning?
        //        }
        //
        //        // options
        //        try {
        //            super.serialize(element);
        //        } catch (BuildException e) {
        //            Activator.log(e);
        //        }
        //
        //        if (outputEntries != null) {
        //            ICStorageElement outEl = element.createChild(OUTPUT_ENTRIES);
        //            LanguageSettingEntriesSerializer.serializeEntries(outputEntries, outEl);
        //        }
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    @Override
    public IToolChain getParent() {
        return parent;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    @Override
    public IBuilder getSuperClass() {
        return (IBuilder) superClass;
    }

    @Override
    public String getName() {
        return (name == null && superClass != null) ? superClass.getName() : name;
    }

    @Override
    public boolean isAbstract() {
        if (isAbstract != null) {
            return isAbstract.booleanValue();
        } else {
            return false; // Note: no inheritance from superClass
        }
    }

    @Override
    public String getCommand() {
        if (command == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return getSuperClass().getCommand();
            } else {
                return "make"; //$NON-NLS-1$
            }
        }
        return command;
    }

    @Override
    public String getArguments() {
        String args = getArgumentsAttribute();
        if (isDefaultBuildArgsOnly()) {
            String stopOnErrCmd = getStopOnErrCmd(isStopOnError());
            int parallelNum = getParallelizationNum();
            String parallelCmd = isParallelBuildOn() ? getParallelizationCmd(parallelNum) : EMPTY_STRING;

            String reversedStopOnErrCmd = getStopOnErrCmd(!isStopOnError());
            String reversedParallelBuildCmd = !isParallelBuildOn() ? getParallelizationCmd(parallelNum) : EMPTY_STRING;

            args = removeCmd(args, reversedStopOnErrCmd);
            args = removeCmd(args, reversedParallelBuildCmd);

            args = addCmd(args, stopOnErrCmd);
            args = addCmd(args, parallelCmd);
        }

        return args != null ? args.trim() : null;
    }

    private String addCmd(String args, String cmd) {
        // Don't modify the args parameter if the cmd to add is emtpy.
        // Bug 360846
        if (cmd.length() == 0)
            return args;

        if (getCmdIndex(args, cmd) == -1) {
            if (args.length() != 0) {
                args += ' ';
            }
            args += cmd;
        }
        return args;
    }

    private String removeCmd(String args, String cmd) {
        int index = getCmdIndex(args, cmd);
        if (index != -1) {
            String prefix = args.substring(0, index).trim();
            String suffix = args.substring(index + cmd.length(), args.length()).trim();
            if (prefix.length() == 0) {
                args = suffix;
            } else if (suffix.length() == 0) {
                args = prefix;
            } else {
                args = prefix + ' ' + suffix;
            }

            args = args.trim();
        }
        return args;
    }

    private int getCmdIndex(String args, String cmd) {
        if (cmd.length() == 0)
            return -1;

        String tmp = args;
        int index = -1;
        for (index = tmp.indexOf(cmd); index != -1; index = tmp.indexOf(cmd, index + 1)) {
            if (index != 0) {
                char c = tmp.charAt(index - 1);
                if (c != '\t' && c != ' ')
                    continue;
            }
            int end = index + cmd.length();
            if (end < tmp.length()) {
                char c = tmp.charAt(end);
                if (c != '\t' && c != ' ')
                    continue;
            }

            //found
            break;
        }
        return index;
    }

    public String getParallelizationCmd(int num) {
        String pattern = getParrallelBuildCmd();
        if (pattern.length() == 0 || num == 0) {
            return EMPTY_STRING;
        }
        // "unlimited" number of jobs results in not adding the number to parallelization cmd
        // that behavior corresponds that of "make" flag "-j".
        return processParallelPattern(pattern, num == UNLIMITED_JOBS, num);
    }

    /**
     * This method turns the supplied pattern to parallelization command
     *
     * It supports 2 kinds of pattern where "*" is replaced with number of jobs:
     * <li>Pattern 1 (supports "<b>-j*</b>"): "text*text" -> "text#text"</li>
     * <li>Pattern 2 (supports "<b>-[j*]</b>"): "text[text*text]text" ->
     * "texttext#texttext</li>
     * <br>
     * Where # is num or empty if {@code empty} is {@code true})
     */
    private String processParallelPattern(String pattern, boolean empty, int num) {
        Assert.isTrue(num > 0);

        int start = pattern.indexOf(PARALLEL_PATTERN_NUM_START);
        int end = -1;
        boolean hasStartChar = false;
        String result;
        if (start != -1) {
            end = pattern.indexOf(PARALLEL_PATTERN_NUM_END);
            if (end != -1) {
                hasStartChar = true;
            } else {
                start = -1;
            }
        }
        if (start == -1) {
            start = pattern.indexOf(PARALLEL_PATTERN_NUM);
            if (start != -1) {
                end = start + PARALLEL_PATTERN_NUM.length();
            }
        }
        if (start == -1) {
            result = pattern;
        } else {
            String prefix;
            String suffix;
            String numStr;
            prefix = pattern.substring(0, start);
            suffix = pattern.substring(end);
            numStr = pattern.substring(start, end);
            if (empty) {
                result = prefix + suffix;
            } else {
                String resolvedNum;
                if (hasStartChar) {
                    String numPrefix, numSuffix;
                    numStr = numStr.substring(0, PARALLEL_PATTERN_NUM_START.length());
                    numStr = numStr.substring(numStr.length() - PARALLEL_PATTERN_NUM_END.length());
                    int numStart = pattern.indexOf(PARALLEL_PATTERN_NUM);
                    if (numStart != -1) {
                        int numEnd = numStart + PARALLEL_PATTERN_NUM.length();
                        numPrefix = numStr.substring(0, numStart);
                        numSuffix = numStr.substring(numEnd);
                        resolvedNum = numPrefix + Integer.toString(num) + numSuffix;
                    } else {
                        resolvedNum = EMPTY_STRING;
                    }
                } else {
                    resolvedNum = Integer.toString(num);
                }
                result = prefix + resolvedNum + suffix;
            }
        }
        return result;
    }

    public String getArgumentsAttribute() {
        if (args == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return ((Builder) superClass).getArgumentsAttribute();
            }
            return EMPTY_STRING;
        }
        return args;
    }

    @Override
    public String getErrorParserIds() {
        String ids = errorParserIds;
        if (ids == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                ids = getSuperClass().getErrorParserIds();
            }
        }
        return ids;
    }

    @Override
    public String[] getErrorParserList() {
        String parserIDs = getErrorParserIds();
        String[] errorParsers = null;
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

    public void setArgumentsAttribute(String newArgs) {
        if (newArgs == null && args == null)
            return;
        if (args == null || newArgs == null || !newArgs.equals(args)) {
            args = newArgs;
        }
    }

    @Override
    public IMakefileGenerator getBuildFileGenerator() {
        return buildFileGeneratorElement;
        //return new GnuMakefileGenerator();
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    @Override
    public IFileContextBuildMacroValues getFileContextBuildMacroValues() {
        return null;
        //        if (fileContextBuildMacroValues == null && superClass != null)
        //            return getSuperClass().getFileContextBuildMacroValues();
        //        return fileContextBuildMacroValues;
    }

    @Override
    public String getBuilderVariablePattern() {
        if (builderVariablePattern == null && superClass != null)
            return getSuperClass().getBuilderVariablePattern();
        return builderVariablePattern;
    }

    @Override
    public String[] getReservedMacroNames() {
        if (reservedMacroNames == null && superClass != null)
            return getSuperClass().getReservedMacroNames();
        return reservedMacroNames;
    }

    @Override
    public IReservedMacroNameSupplier getReservedMacroNameSupplier() {
        if (reservedMacroNameSupplier == null && reservedMacroNameSupplierElement != null) {
            try {
                reservedMacroNameSupplier = (IReservedMacroNameSupplier) reservedMacroNameSupplierElement
                        .createExecutableExtension(RESERVED_MACRO_NAME_SUPPLIER);
            } catch (CoreException e) {
            }
        }
        if (reservedMacroNameSupplier == null && superClass != null)
            return getSuperClass().getReservedMacroNameSupplier();
        return reservedMacroNameSupplier;
    }

    /*
     * This function checks for migration support for the builder, while
     * loading. If migration support is needed, looks for the available
     * converters and stores them.
     */

    public void checkForMigrationSupport() {
        //
        //        //		String tmpId = null;
        //        boolean isExists = false;
        //
        //        if (getSuperClass() == null) {
        //            // If 'superClass' is null, then there is no builder available in
        //            // plugin manifest file with the same 'id' & version.
        //            // Look for the 'versionsSupported' attribute
        //            String high = ManagedBuildManager.getExtensionBuilderMap().lastKey();
        //
        //            SortedMap<String, ? extends IBuilder> subMap = null;
        //            if (superClassId.compareTo(high) <= 0) {
        //                subMap = ManagedBuildManager.getExtensionBuilderMap().subMap(superClassId, high + "\0"); //$NON-NLS-1$
        //            } else {
        //                // It means there are no entries in the map for the given id.
        //                // make the project is invalid
        //
        //                IToolChain parent = getParent();
        //                IConfiguration parentConfig = parent.getParent();
        //                IManagedProject managedProject = parentConfig.getManagedProject();
        //                if (managedProject != null) {
        //                    managedProject.setValid(false);
        //                }
        //                return;
        //            }
        //
        //            // for each element in the 'subMap',
        //            // check the 'versionsSupported' attribute whether the given
        //            // builder version is supported
        //
        //            String baseId = ManagedBuildManager.getIdFromIdAndVersion(superClassId);
        //            String version = ManagedBuildManager.getVersionFromIdAndVersion(superClassId);
        //
        //            Collection<? extends IBuilder> c = subMap.values();
        //            IBuilder[] builderElements = c.toArray(new IBuilder[c.size()]);
        //
        //            for (int i = 0; i < builderElements.length; i++) {
        //                IBuilder builderElement = builderElements[i];
        //
        //                if (ManagedBuildManager.getIdFromIdAndVersion(builderElement.getId()).compareTo(baseId) > 0)
        //                    break;
        //                // First check if both base ids are equal
        //                if (ManagedBuildManager.getIdFromIdAndVersion(builderElement.getId()).equals(baseId)) {
        //
        //                    // Check if 'versionsSupported' attribute is available'
        //                    String versionsSupported = builderElement.getVersionsSupported();
        //
        //                    if ((versionsSupported != null) && (!versionsSupported.isEmpty())) {
        //                        String[] tmpVersions = versionsSupported.split(","); //$NON-NLS-1$
        //
        //                        for (int j = 0; j < tmpVersions.length; j++) {
        //                            if (new Version(version).equals(new Version(tmpVersions[j]))) {
        //                                // version is supported.
        //                                // Do the automatic conversion without
        //                                // prompting the user.
        //                                // Get the supported version
        //                                String supportedVersion = ManagedBuildManager
        //                                        .getVersionFromIdAndVersion(builderElement.getId());
        //                                id = (ManagedBuildManager.getIdFromIdAndVersion(getId()) + "_" + supportedVersion); //$NON-NLS-1$
        //
        //                                // If control comes here means that 'superClass' is null
        //                                // So, set the superClass to this builder element
        //                                superClass = builderElement;
        //                                superClassId = superClass.getId();
        //                                isExists = true;
        //                                break;
        //                            }
        //                        }
        //                        if (isExists)
        //                            break; // break the outer for loop if 'isExists' is true
        //                    }
        //                }
        //            }
        //        }
        //        if (getSuperClass() != null) {
        //            // If 'getSuperClass()' is not null, look for 'convertToId' attribute in plugin
        //            // manifest file for this builder.
        //            String convertToId = getSuperClass().getConvertToId();
        //            if ((convertToId == null) || (convertToId.isEmpty())) {
        //                // It means there is no 'convertToId' attribute available and
        //                // the version is still actively
        //                // supported by the tool integrator. So do nothing, just return
        //                return;
        //            } else {
        //                // Incase the 'convertToId' attribute is available,
        //                // it means that Tool integrator currently does not support this
        //                // version of builder.
        //                // Look for the converters available for this builder version.
        //
        //                getConverter(convertToId);
        //            }
        //
        //        } else {
        //            // make the project is invalid
        //            //
        //            IToolChain parent = getParent();
        //            IConfiguration parentConfig = parent.getParent();
        //            IManagedProject managedProject = parentConfig.getManagedProject();
        //            if (managedProject != null) {
        //                managedProject.setValid(false);
        //            }
        //        }
        //        return;
    }

    private void getConverter(String convertToId) {

        String fromId = null;
        String toId = null;

        // Get the Converter Extension Point
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
                "org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
                "projectConverter"); //$NON-NLS-1$
        if (extensionPoint != null) {
            // Get the extensions
            IExtension[] extensions = extensionPoint.getExtensions();
            for (int i = 0; i < extensions.length; i++) {
                // Get the configuration elements of each extension
                IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
                for (int j = 0; j < configElements.length; j++) {

                    IConfigurationElement element = configElements[j];

                    if (element.getName().equals("converter")) { //$NON-NLS-1$

                        fromId = element.getAttribute("fromId"); //$NON-NLS-1$
                        toId = element.getAttribute("toId"); //$NON-NLS-1$
                        // Check whether the current converter can be used for
                        // the selected builder

                        if (fromId.equals(getSuperClass().getId()) && toId.equals(convertToId)) {
                            // If it matches
                            String mbsVersion = element.getAttribute("mbsVersion"); //$NON-NLS-1$
                            Version currentMbsVersion = ManagedBuildManager.getBuildInfoVersion();

                            // set the converter element based on the MbsVersion
                            if (currentMbsVersion.compareTo(new Version(mbsVersion)) > 0) {
                                previousMbsVersionConversionElement = element;
                            } else {
                                currentMbsVersionConversionElement = element;
                            }
                            return;
                        }
                    }
                }
            }
        }

        // If control comes here, it means 'Tool Integrator' specified
        // 'convertToId' attribute in toolchain definition file, but
        // has not provided any converter.
        // So, make the project is invalid

        IToolChain parent = getParent();
        IConfiguration parentConfig = parent.getParent();
        IManagedProject managedProject = parentConfig.getManagedProject();
        if (managedProject != null) {
            managedProject.setValid(false);
        }
    }

    public IConfigurationElement getPreviousMbsVersionConversionElement() {
        return previousMbsVersionConversionElement;
    }

    public IConfigurationElement getCurrentMbsVersionConversionElement() {
        return currentMbsVersionConversionElement;
    }

    @Override
    public CBuildData getBuildData() {
        return fBuildData;
    }

    //	public String[] getCustomizedErrorParserIds(){
    //		if(customizedErrorParserIds != null)
    //			return (String[])customizedErrorParserIds.clone();
    //		return null;
    //	}

    @Override
    public String[] getErrorParsers() {
        return null;
    }

    public String[] getCustomizedErrorParserIds() {
        return null;
    }

    private Object getMacroContextData() {
        return (Object) this;
    }

    //@Override
    public String getBuildArguments() {
        String args = getArguments();
        IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();

        try {
            args = provider.resolveValue(args, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, //$NON-NLS-1$//$NON-NLS-2$
                    getMacroContextData());
        } catch (BuildMacroException e) {
        }

        return args;
    }

    @Override
    public IPath getBuildCommand() {
        String command = getCommand();
        IBuildMacroProvider provider = BuildMacroProvider.getDefault();

        try {
            command = provider.resolveValue(command, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, //$NON-NLS-1$//$NON-NLS-2$
                    getMacroContextData());
        } catch (BuildMacroException e) {
            Activator.log(e);
        }

        return new Path(command);
    }

    @Override
    public boolean isDefaultBuildCmd() {
        return (command == null
                && args == null /*&& stopOnErr == null && parallelBuildOn == null && parallelNum == null */
                && superClass != null);
    }

    @Override
    public boolean isDefaultBuildCmdOnly() {
        return (command == null && superClass != null);
    }

    @Override
    public boolean isDefaultBuildArgsOnly() {
        return (args == null && superClass != null);
    }

    @Override
    public boolean isStopOnError() {
        return true;
    }

    @Override
    public void setBuildArguments(String args) throws CoreException {
        //deprecated
    }

    @Override
    public void setBuildCommand(IPath command) throws CoreException {
        //@Deprecated
    }

    @Override
    public void setStopOnError(boolean on) throws CoreException {
    }

    @Override
    public void setUseDefaultBuildCmd(boolean on) throws CoreException {
        //@Deprecated
    }

    @Override
    public void setUseDefaultBuildCmdOnly(boolean on) throws CoreException {
    }

    @Override
    public void setUseDefaultBuildArgsOnly(boolean on) throws CoreException {
        if (superClass != null) {
            if (on) {
                args = null;
            } else {
                args = EMPTY_STRING;
            }
        }
    }

    public String getAutoBuildTargetAttribute() {
        return modelautoBuildTarget[SUPER];
    }

    @Override
    public String getAutoBuildTarget() {
        String attr = getAutoBuildTargetAttribute();

        if (attr != null) {
            IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();

            try {
                attr = provider.resolveValue(attr, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, //$NON-NLS-1$//$NON-NLS-2$
                        getMacroContextData());
            } catch (BuildMacroException e) {
            }
        }
        if (attr == null) {
            attr = DEFAULT_TARGET_AUTO;
        }

        return attr;
    }

    public String getCleanBuildTargetAttribute() {
        return DEFAULT_TARGET_CLEAN;
    }

    @Override
    public String getCleanBuildTarget() {
        String attr = getCleanBuildTargetAttribute();

        if (attr != null) {
            IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();

            try {
                attr = provider.resolveValue(attr, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, //$NON-NLS-1$//$NON-NLS-2$
                        getMacroContextData());
            } catch (BuildMacroException e) {
            }
        }
        if (attr == null) {
            attr = DEFAULT_TARGET_CLEAN;
        }

        return attr;
    }

    @Override
    public String getFullBuildTarget() {
        return getIncrementalBuildTarget();
    }

    public String getIncrementalBuildTargetAttribute() {
        return null;
    }

    @Override
    public String getIncrementalBuildTarget() {
        String attr = getIncrementalBuildTargetAttribute();

        if (attr != null) {
            IBuildMacroProvider provider = ManagedBuildManager.getBuildMacroProvider();

            try {
                attr = provider.resolveValue(attr, "", " ", IBuildMacroProvider.CONTEXT_CONFIGURATION, //$NON-NLS-1$//$NON-NLS-2$
                        getMacroContextData());
            } catch (BuildMacroException e) {
            }
        }
        if (attr == null) {
            attr = DEFAULT_TARGET_INCREMENTAL;
        }

        return attr;
    }

    @Override
    public boolean isAutoBuildEnable() {
        return true;
    }

    @Override
    public boolean isCleanBuildEnabled() {
        return true;
    }

    @Override
    public boolean isFullBuildEnabled() {
        return isIncrementalBuildEnabled();
    }

    @Override
    public boolean isIncrementalBuildEnabled() {
        return true;
    }

    @Override
    public void setAutoBuildEnable(boolean enabled) throws CoreException {
    }

    @Override
    public void setAutoBuildTarget(String target) throws CoreException {
    }

    @Override
    public void setCleanBuildEnable(boolean enabled) throws CoreException {
    }

    @Override
    public void setCleanBuildTarget(String target) throws CoreException {
    }

    @Override
    public void setFullBuildEnable(boolean enabled) throws CoreException {
        setIncrementalBuildEnable(enabled);
    }

    //@Override TODO is this used?
    public String getBuildAttribute(String name, String defaultValue) {
        String result = null;
        if (BUILD_TARGET_INCREMENTAL.equals(name)) {
            result = getIncrementalBuildTargetAttribute();
        } else if (BUILD_TARGET_AUTO.equals(name)) {
            result = getAutoBuildTargetAttribute();
        } else if (BUILD_TARGET_CLEAN.equals(name)) {
            result = getCleanBuildTargetAttribute();
        } else if (BUILD_COMMAND.equals(name)) {
            result = getCommand();
        } else if (BUILD_ARGUMENTS.equals(name)) {
            result = getArguments();
            //        } else if (BuilderFactory.BUILD_COMMAND.equals(name)) {
            //            result = getCommand();
            //        } else if (BuilderFactory.BUILD_LOCATION.equals(name)) {
            //            result = getBuildPathAttribute();
            //        } else if (BuilderFactory.STOP_ON_ERROR.equals(name)) {
            //            result = String.valueOf(isStopOnError());
            //        } else if (BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)) {
            //            result = String.valueOf(isDefaultBuildCmd());
            //        } else if (BuilderFactory.BUILD_TARGET_AUTO.equals(name)) {
            //            result = getAutoBuildTargetAttribute();
            //        } else if (BuilderFactory.BUILD_TARGET_INCREMENTAL.equals(name)) {
            //            result = getIncrementalBuildTargetAttribute();
            //        } else if (BuilderFactory.BUILD_TARGET_FULL.equals(name)) {
            //            result = getIncrementalBuildTargetAttribute();
            //        } else if (BuilderFactory.BUILD_TARGET_CLEAN.equals(name)) {
            //            result = getCleanBuildTargetAttribute();
            //        } else if (BuilderFactory.BUILD_FULL_ENABLED.equals(name)) {
            //            result = String.valueOf(isFullBuildEnabled());
            //        } else if (BuilderFactory.BUILD_CLEAN_ENABLED.equals(name)) {
            //            result = String.valueOf(isCleanBuildEnabled());
            //        } else if (BuilderFactory.BUILD_INCREMENTAL_ENABLED.equals(name)) {
            //            result = String.valueOf(isIncrementalBuildEnabled());
            //        } else if (BuilderFactory.BUILD_AUTO_ENABLED.equals(name)) {
            //            result = String.valueOf(isAutoBuildEnable());
            //        } else if (BuilderFactory.BUILD_ARGUMENTS.equals(name)) {
            //            result = getArguments();
            //        } else if (BuilderFactory.ENVIRONMENT.equals(name)) {
            //            result = customizedEnvironment != null ? MapStorageElement.encodeMap(customizedEnvironment) : null;
            //        } else if (BuilderFactory.BUILD_APPEND_ENVIRONMENT.equals(name)) {
            //            result = String.valueOf(appendEnvironment());
        } else if (customBuildProperties != null) {
            result = customBuildProperties.get(name);
        }

        if (result == null)
            return defaultValue;
        return result;
    }

    public static String[] toBuildAttributes(String name) {

        //        if (ATTRIBUTE_TARGET_INCREMENTAL.equals(name)) {
        //            return new String[] { BUILD_TARGET_INCREMENTAL, BuilderFactory.BUILD_TARGET_INCREMENTAL, BUILD_TARGET_FULL,
        //                    BuilderFactory.BUILD_TARGET_FULL };
        //        } else if (ATTRIBUTE_TARGET_AUTO.equals(name)) {
        //            return new String[] { BUILD_TARGET_AUTO, BuilderFactory.BUILD_TARGET_AUTO };
        //        } else if (ATTRIBUTE_TARGET_CLEAN.equals(name)) {
        //            return new String[] { BUILD_TARGET_CLEAN, BuilderFactory.BUILD_TARGET_CLEAN };
        //        } else if (ATTRIBUTE_BUILD_PATH.equals(name)) {
        //            return new String[] { BUILD_LOCATION, BuilderFactory.BUILD_LOCATION };
        //        } else if (COMMAND.equals(name)) {
        //            return new String[] { BUILD_COMMAND, BuilderFactory.BUILD_COMMAND };
        //        } else if (ARGUMENTS.equals(name)) {
        //            return new String[] { BUILD_ARGUMENTS, BuilderFactory.BUILD_ARGUMENTS };
        //        } else if (ATTRIBUTE_STOP_ON_ERR.equals(name)) {
        //            return new String[] { BuilderFactory.STOP_ON_ERROR };
        //        } //TODO else if(BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)){
        //          //	return getCommand();
        //          //}
        //        else if (ATTRIBUTE_INCREMENTAL_ENABLED.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_INCREMENTAL_ENABLED, BuilderFactory.BUILD_FULL_ENABLED };
        //        } else if (ATTRIBUTE_CLEAN_ENABLED.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_CLEAN_ENABLED };
        //        } else if (ATTRIBUTE_AUTO_ENABLED.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_AUTO_ENABLED };
        //        } else if (ATTRIBUTE_ENVIRONMENT.equals(name)) {
        //            return new String[] { BuilderFactory.ENVIRONMENT };
        //        } else if (ATTRIBUTE_APPEND_ENVIRONMENT.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_APPEND_ENVIRONMENT };
        //        } else if (ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS.equals(name)) {
        //            return new String[] { ErrorParserManager.PREF_ERROR_PARSER };
        //        }

        return new String[0];
    }

    public static String toBuilderAttribute(String name) {

        //        if (BUILD_TARGET_INCREMENTAL.equals(name) || BuilderFactory.BUILD_TARGET_INCREMENTAL.equals(name)
        //                || BUILD_TARGET_FULL.equals(name) || BuilderFactory.BUILD_TARGET_FULL.equals(name)) {
        //            return ATTRIBUTE_TARGET_INCREMENTAL;
        //        } else if (BUILD_TARGET_AUTO.equals(name) || BuilderFactory.BUILD_TARGET_AUTO.equals(name)) {
        //            return ATTRIBUTE_TARGET_AUTO;
        //        } else if (BUILD_TARGET_CLEAN.equals(name) || BuilderFactory.BUILD_TARGET_CLEAN.equals(name)) {
        //            return ATTRIBUTE_TARGET_CLEAN;
        //        } else if (BUILD_LOCATION.equals(name) || BuilderFactory.BUILD_LOCATION.equals(name)) {
        //            return ATTRIBUTE_BUILD_PATH;
        //        } else if (BUILD_COMMAND.equals(name) || BuilderFactory.BUILD_COMMAND.equals(name)) {
        //            return COMMAND;
        //        } else if (BUILD_ARGUMENTS.equals(name) || BuilderFactory.BUILD_ARGUMENTS.equals(name)) {
        //            return ARGUMENTS;
        //        } else if (BuilderFactory.STOP_ON_ERROR.equals(name)) {
        //            return ATTRIBUTE_STOP_ON_ERR;
        //        } //TODO else if(BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)){
        //          //	return getCommand();
        //          //}
        //        else if (BuilderFactory.BUILD_INCREMENTAL_ENABLED.equals(name)
        //                || BuilderFactory.BUILD_FULL_ENABLED.equals(name)) {
        //            return ATTRIBUTE_INCREMENTAL_ENABLED;
        //        } else if (BuilderFactory.BUILD_CLEAN_ENABLED.equals(name)) {
        //            return ATTRIBUTE_CLEAN_ENABLED;
        //        } else if (BuilderFactory.BUILD_AUTO_ENABLED.equals(name)) {
        //            return ATTRIBUTE_AUTO_ENABLED;
        //        } else if (BuilderFactory.ENVIRONMENT.equals(name)) {
        //            return ATTRIBUTE_ENVIRONMENT;
        //        } else if (BuilderFactory.BUILD_APPEND_ENVIRONMENT.equals(name)) {
        //            return ATTRIBUTE_APPEND_ENVIRONMENT;
        //        } else if (ErrorParserManager.PREF_ERROR_PARSER.equals(name)) {
        //            return ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS;
        //        }
        return null;
    }

    @Override
    public Map<String, String> getEnvironment() {
        if (customizedEnvironment != null)
            return cloneMap(customizedEnvironment);
        return null;
    }

    @Override
    public Map<String, String> getExpandedEnvironment() throws CoreException {
        if (customizedEnvironment != null) {
            Map<String, String> expanded = cloneMap(customizedEnvironment);
            ICdtVariableManager mngr = CCorePlugin.getDefault().getCdtVariableManager();
            String separator = CCorePlugin.getDefault().getBuildEnvironmentManager().getDefaultDelimiter();
            ICConfigurationDescription cfgDes = ManagedBuildManager
                    .getDescriptionForConfiguration(getParent().getParent());
            Set<Entry<String, String>> entrySet = expanded.entrySet();
            for (Entry<String, String> entry : entrySet) {
                String value = entry.getValue();
                try {
                    value = mngr.resolveValue(value, "", separator, cfgDes); //$NON-NLS-1$
                    entry.setValue(value);
                } catch (CdtVariableException e) {
                }
            }

            return expanded;
        }
        return null;
    }

    @Override
    public void setBuildAttribute(String name, String value) throws CoreException {
        //        if (BUILD_TARGET_INCREMENTAL.equals(name)) {
        //            incrementalBuildTarget = value;
        //        } else if (BUILD_TARGET_AUTO.equals(name)) {
        //            autoBuildTarget = value;
        //        } else if (BUILD_TARGET_CLEAN.equals(name)) {
        //            cleanBuildTarget = value;
        //        } else if (BUILD_LOCATION.equals(name)) {
        //            buildPath = value;
        //        } else if (BUILD_COMMAND.equals(name)) {
        //            command = value;
        //        } else if (BUILD_ARGUMENTS.equals(name)) {
        //            args = value;
        //        } else if (BuilderFactory.BUILD_COMMAND.equals(name)) {
        //            command = value;
        //        } else if (BuilderFactory.BUILD_LOCATION.equals(name)) {
        //            buildPath = value;
        //        } else if (BuilderFactory.STOP_ON_ERROR.equals(name)) {
        //            stopOnErr = Boolean.valueOf(value);
        //        } else if (BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)) {
        //            if (value == null || Boolean.parseBoolean(value)) {
        //                if (superClass != null)
        //                    command = null;
        //            }
        //        } else if (BuilderFactory.BUILD_TARGET_AUTO.equals(name)) {
        //            autoBuildTarget = value;
        //        } else if (BuilderFactory.BUILD_TARGET_INCREMENTAL.equals(name)) {
        //            incrementalBuildTarget = value;
        //        } else if (BuilderFactory.BUILD_TARGET_FULL.equals(name)) {
        //            autoBuildTarget = value;
        //        } else if (BuilderFactory.BUILD_TARGET_CLEAN.equals(name)) {
        //            cleanBuildTarget = value;
        //        } else if (BuilderFactory.BUILD_FULL_ENABLED.equals(name)) {
        //            autoBuildEnabled = value != null ? Boolean.valueOf(value) : null;
        //        } else if (BuilderFactory.BUILD_CLEAN_ENABLED.equals(name)) {
        //            cleanBuildEnabled = value != null ? Boolean.valueOf(value) : null;
        //        } else if (BuilderFactory.BUILD_INCREMENTAL_ENABLED.equals(name)) {
        //            incrementalBuildEnabled = value != null ? Boolean.valueOf(value) : null;
        //        } else if (BuilderFactory.BUILD_AUTO_ENABLED.equals(name)) {
        //            autoBuildEnabled = value != null ? Boolean.valueOf(value) : null;
        //        } else if (BuilderFactory.BUILD_ARGUMENTS.equals(name)) {
        //            args = value;
        //        } else if (BuilderFactory.ENVIRONMENT.equals(name)) {
        //            if (value == null) {
        //                customizedEnvironment = null;
        //            } else {
        //                customizedEnvironment = MapStorageElement.decodeMap(value);
        //            }
        //        } else if (BuilderFactory.BUILD_APPEND_ENVIRONMENT.equals(name)) {
        //            appendEnvironment = value != null ? Boolean.valueOf(value) : null;
        //        } else {
        //            getCustomBuildPropertiesMap().put(name, value);
        //        }
    }

    private Map<String, String> getCustomBuildPropertiesMap() {
        if (customBuildProperties == null) {
            customBuildProperties = new HashMap<>();
        }
        return customBuildProperties;
    }

    @Override
    public void setEnvironment(Map<String, String> env) throws CoreException {
        customizedEnvironment = new HashMap<>(env);
    }

    @Override
    public boolean isCustomBuilder() {
        if (getParent().getBuilder() != this)
            return true;
        return false;
    }

    public IConfiguration getConfguration() {
        if (getParent() != null)
            return getParent().getParent();
        return null;
    }

    @Override
    public boolean isManagedBuildOn() {
        IConfiguration cfg = getConfguration();
        if (cfg != null) {
            if (!cfg.supportsBuild(true))
                return false;
            else if (!cfg.supportsBuild(false))
                return true;
        }

        Boolean attr = getManagedBuildOnAttribute();
        if (attr != null)
            return attr.booleanValue();
        return true;
    }

    public Boolean getManagedBuildOnAttribute() {
        return true;
    }

    @Override
    public void setManagedBuildOn(boolean on) {
    }

    @Override
    public boolean supportsCustomizedBuild() {
        if (fSupportsCustomizedBuild == null) {
            IMakefileGenerator makeGen = getBuildFileGenerator();
            if (makeGen instanceof IMakefileGenerator)
                fSupportsCustomizedBuild = true;
            else
                fSupportsCustomizedBuild = false;
        }
        return fSupportsCustomizedBuild.booleanValue();
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        return false;
    }

    public void setParent(IToolChain toolChain) {
        parent = toolChain;
    }

    @Override
    public boolean matches(IBuilder builder) {
        if (builder == this)
            return true;

        IBuilder rBld = ManagedBuildManager.getRealBuilder(this);
        if (rBld == null)
            return false;

        return rBld == ManagedBuildManager.getRealBuilder(builder);
    }

    public String getNameAndVersion() {
        String name = getName();
        String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
        if (version != null && version.length() != 0) {
            return new StringBuilder().append(name).append(" (").append(version).append("").toString(); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return name;
    }

    @Override
    public boolean isInternalBuilder() {
        return false;
    }

    /**
     * Returns the optimal number of parallel jobs.
     * The number is the number of available processors on the machine.
     *
     * The function never returns number smaller than 1.
     */
    public int getOptimalParallelJobNum() {
        // Bug 398426: On my Mac running parallel builds at full tilt hangs the desktop.
        // Need to pull it back one.
        int j = Runtime.getRuntime().availableProcessors();
        if (j > 1 && Platform.getOS().equals(Platform.OS_MACOSX))
            return j - 1;
        else
            return j;
    }

    /**
     * Returns the internal representation of maximum number of parallel jobs
     * to be used for a build.
     * Note that negative number represents "optimal" value.
     *
     * The value of the number is encoded as follows:
     * 
     * <pre>
     *  Status       Returns
     * No parallel      1
     * Optimal       -CPU# (negative number of processors)
     * Specific        >0  (positive number)
     * Unlimited    Builder.UNLIMITED_JOBS
     * </pre>
     */
    public int getParallelizationNumAttribute() {
        if (!isParallelBuildOn())
            return 1;

        if (parallelNumberAttribute == null) {
            if (superClass != null) {
                return ((Builder) superClass).getParallelizationNumAttribute();
            }
            return 1;
        }
        return parallelNumberAttribute.intValue();
    }

    private void setParallelizationNumAttribute(int parallelNumber) {
        isParallelBuildEnabled = (parallelNumber != 1);
        if (parallelNumber > 0) {
            parallelNumberAttribute = parallelNumber;
        } else {
            // "optimal"
            parallelNumberAttribute = -getOptimalParallelJobNum();
        }
    }

    @Override
    public int getParallelizationNum() {
        return Math.abs(getParallelizationNumAttribute());
    }

    /**
     * {@inheritDoc}
     *
     * @param jobs
     *            - maximum number of jobs. There are 2 special cases:
     *            <br>
     *            - any number <=0 is interpreted as setting "optimal" property,
     *            the value of the number itself is ignored in this case
     *            <br>
     *            - value 1 will turn parallel mode off.
     */
    @Override
    public void setParallelizationNum(int jobs) throws CoreException {
        if (!supportsParallelBuild())
            return;

        if (parallelNumberAttribute == null || parallelNumberAttribute != jobs) {
            String curCmd = getParallelizationCmd(getParallelizationNum());
            String args = getArgumentsAttribute();
            String updatedArgs = removeCmd(args, curCmd);
            if (!updatedArgs.equals(args)) {
                setArgumentsAttribute(updatedArgs);
            }

            setParallelizationNumAttribute(jobs);
        }
    }

    @Override
    public boolean supportsParallelBuild() {
        return getParrallelBuildCmd().length() != 0;
    }

    @Override
    public boolean supportsStopOnError(boolean on) {

        if (!on)
            return getIgnoreErrCmdAttribute().length() != 0;
        return true;
    }

    public String getStopOnErrCmd(boolean stop) {
        if (!stop)
            return getIgnoreErrCmdAttribute();
        return EMPTY_STRING;
    }

    public String getIgnoreErrCmdAttribute() {
        return modelignoreErrCmd[SUPER];
    }

    public String getParrallelBuildCmd() {
        return modelparallelBuildCmd[SUPER];
    }

    @Override
    public boolean isParallelBuildOn() {
        if (!supportsParallelBuild()) {
            return false;
        }
        if (isParallelBuildEnabled == null) {
            if (superClass != null) {
                return getSuperClass().isParallelBuildOn();
            }
            return false;
        }
        return isParallelBuildEnabled.booleanValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param on
     *            - the flag to enable or disable parallel mode.
     *            <br>
     *            {@code true} to enable, in this case the maximum number of jobs
     *            will be set to "optimal" number, see
     *            {@link #getOptimalParallelJobNum()}.
     *            <br>
     *            {@code false} to disable, the number of jobs will be set to 1.
     */
    @Override
    public void setParallelBuildOn(boolean on) throws CoreException {
        if (on) {
            // set "optimal" jobs by default when enabling parallel build
            setParallelizationNum(-1);
        } else {
            setParallelizationNum(1);
        }
    }

    public Set<String> contributeErrorParsers(Set<String> set) {
        if (getErrorParserIds() != null) {
            if (set == null)
                set = new HashSet<>();

            String ids[] = getErrorParserList();
            if (ids.length != 0)
                set.addAll(Arrays.asList(ids));
        }
        return set;
    }

    public void resetErrorParsers() {
        errorParserIds = null;
    }

    void removeErrorParsers(Set<String> set) {
        Set<String> oldSet = contributeErrorParsers(null);
        if (oldSet == null)
            oldSet = new HashSet<>();
        oldSet.removeAll(set);
        setErrorParserList(oldSet.toArray(new String[oldSet.size()]));
    }

    public void setErrorParserList(String[] ids) {
        if (ids == null) {
            errorParserIds = null;
        } else if (ids.length == 0) {
            errorParserIds = EMPTY_STRING;
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(ids[0]);
            for (int i = 1; i < ids.length; i++) {
                buf.append(";").append(ids[i]); //$NON-NLS-1$
            }
            errorParserIds = buf.toString();
        }
    }

    @Override
    public boolean isSystemObject() {
        if (isTest)
            return true;

        if (getParent() != null)
            return getParent().isSystemObject();
        return false;
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

    public ICOutputEntry[] getOutputEntries() {
        if (isManagedBuildOn()) {
            return getDefaultOutputSettings();
        }
        ICOutputEntry[] entries = getOutputEntrySettings();
        if (entries == null || entries.length == 0) {
            entries = getDefaultOutputSettings();
        }
        return entries;
    }

    private ICOutputEntry[] getDefaultOutputSettings() {
        Configuration cfg = (Configuration) getConfguration();
        if (cfg == null || cfg.isPreference()) {
            return new ICOutputEntry[] { new COutputEntry(Path.EMPTY, null,
                    ICLanguageSettingEntry.VALUE_WORKSPACE_PATH | ICLanguageSettingEntry.RESOLVED) };
        }

        IFolder BuildFolder = ManagedBuildManager.getBuildFolder(cfg, this);
        return new ICOutputEntry[] { new COutputEntry(BuildFolder, null,
                ICLanguageSettingEntry.VALUE_WORKSPACE_PATH | ICLanguageSettingEntry.RESOLVED) };
    }

    public ICOutputEntry[] getOutputEntrySettings() {
        if (outputEntries == null) {
            if (superClass != null) {
                return ((Builder) superClass).getOutputEntrySettings();
            }
            return null;

        }
        return outputEntries.clone();
    }

    public void setOutputEntries(ICOutputEntry[] entries) {
        if (entries != null)
            outputEntries = entries.clone();
        else
            outputEntries = null;
    }

    private int getSuperClassNum() {
        int num = 0;
        for (IBuilder superTool = getSuperClass(); superTool != null; superTool = superTool.getSuperClass()) {
            num++;
        }
        return num;
    }

    @Override
    public String toString() {
        return getUniqueRealName();
    }

    @Override
    public ICommandLauncher getCommandLauncher() {
        if (fCommandLauncherElement != null) {
            try {
                return (ICommandLauncher) fCommandLauncherElement.createExecutableExtension(ATTRIBUTE_COMMAND_LAUNCHER);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
        if (superClass != null)
            return getSuperClass().getCommandLauncher();

        // catch all for backwards compatibility
        return CommandLauncherManager.getInstance().getCommandLauncher();
    }

    @Override
    public IBuildRunner getBuildRunner() throws CoreException {
        // Already defined
        if (fBuildRunner != null)
            return fBuildRunner;

        // Instantiate from model
        if (fBuildRunnerElement != null) {
            fBuildRunner = (IBuildRunner) fBuildRunnerElement.createExecutableExtension(ATTRIBUTE_BUILD_RUNNER);
            return fBuildRunner;
        }

        // Check with superClass
        if (superClass != null)
            return getSuperClass().getBuildRunner();

        return new BuildRunner();
    }

    @Override
    protected IResourceInfo getParentResourceInfo() {
        // There are no resources associated with builders
        return null;
    }

    @Override
    public boolean appendEnvironment() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public IPath getBuildLocation() {
        return myBuildFolder.getLocation();
    }

    @Override
    public void setBuildLocation(IPath location) throws CoreException {
        // This method is Deprecated

    }

    @Override
    public void setIncrementalBuildEnable(boolean enabled) throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setIncrementalBuildTarget(String target) throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFullBuildTarget(String target) throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setAppendEnvironment(boolean append) throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setErrorParsers(String[] parsers) throws CoreException {
        // TODO Auto-generated method stub

    }

}
