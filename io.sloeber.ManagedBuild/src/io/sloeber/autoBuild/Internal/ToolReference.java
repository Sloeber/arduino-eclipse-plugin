/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
//import org.eclipse.cdt.managedbuilder.core.BuildException;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IConfigurationV2;
//import org.eclipse.cdt.managedbuilder.core.IEnvVarBuildPath;
//import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineGenerator;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOptionApplicability;
//import org.eclipse.cdt.managedbuilder.core.IOptionCategory;
//import org.eclipse.cdt.managedbuilder.core.IOptionPathConverter;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.IToolReference;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGenerator;
//import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfigurationV2;
import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;
import io.sloeber.autoBuild.api.IOptionPathConverter;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.IResourceConfiguration;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITarget;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.api.IToolReference;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.IOptionApplicability;

public class ToolReference implements IToolReference {
    private static final String DEFAULT_SEPARATOR = ","; //$NON-NLS-1$

    private String command;
    private boolean isDirty = false;
    //   private List<OptionReference> optionReferences;
    private IBuildObject owner;
    private String outputExtensions;
    private String outputFlag;
    private String outputPrefix;
    protected ITool parent;
    private boolean resolved = true;
    private String versionsSupported;
    private String convertToId;

    /**
     * Create a new tool reference based on information contained in
     * a project file.
     *
     * @param owner
     *            The <code>IConfigurationV2</code> the receiver will be added to.
     * @param element
     *            The element defined in the project file containing build
     *            information
     *            for the receiver.
     */
    public ToolReference(BuildObject owner, Element element) {
        //        this.owner = owner;
        //
        //        if (owner instanceof IConfigurationV2) {
        //            if (parent == null) {
        //                ITarget parentTarget = (ITarget) ((IConfigurationV2) owner).getTarget();
        //                try {
        //                    parent = ((ITarget) parentTarget.getParent()).getTool(element.getAttribute(ID));
        //                } catch (NullPointerException e) {
        //                    parent = null;
        //                }
        //            }
        //            ((IConfigurationV2) owner).addToolReference(this);
        //        } else if (owner instanceof ITarget) {
        //            if (parent == null) {
        //                try {
        //                    parent = ((ITarget) ((ITarget) owner).getParent()).getTool(element.getAttribute(ID));
        //                } catch (NullPointerException e) {
        //                    parent = null;
        //                }
        //            }
        //            ((ITarget) owner).addToolReference(this);
        //        }
        //
        //        // Get the overridden tool command (if any)
        //        if (element.hasAttribute(ITool.COMMAND)) {
        //            command = element.getAttribute(ITool.COMMAND);
        //        }
        //
        //        // Get the overridden output prefix (if any)
        //        if (element.hasAttribute(ITool.OUTPUT_PREFIX)) {
        //            outputPrefix = element.getAttribute(ITool.OUTPUT_PREFIX);
        //        }
        //
        //        // Get the output extensions the reference produces
        //        if (element.hasAttribute(ITool.OUTPUTS)) {
        //            outputExtensions = element.getAttribute(ITool.OUTPUTS);
        //        }
        //        // Get the flag to control output
        //        if (element.hasAttribute(ITool.OUTPUT_FLAG))
        //            outputFlag = element.getAttribute(ITool.OUTPUT_FLAG);
        //
        //        NodeList configElements = element.getChildNodes();
        //        for (int i = 0; i < configElements.getLength(); ++i) {
        //            Node configElement = configElements.item(i);
        //            if (configElement.getNodeName().equals(ITool.OPTION_REF)) {
        //                new OptionReference(this, (Element) configElement);
        //            }
        //        }
    }

    /**
     * Created tool reference from an extension defined in a plugin manifest.
     *
     * @param owner
     *            The <code>BuildObject</code> the receiver will be added to.
     * @param element
     *            The element containing build information for the reference.
     */
    public ToolReference(BuildObject owner, IManagedConfigElement element) {
        // setup for resolving
        ManagedBuildManager.putConfigElement(this, element);
        resolved = false;

        this.owner = owner;

        //        // hook me up
        //        if (owner instanceof IConfigurationV2) {
        //            ((IConfigurationV2) owner).addToolReference(this);
        //        } else if (owner instanceof ITarget) {
        //            ((ITarget) owner).addToolReference(this);
        //        }

        // Get the overridden tool command (if any)
        command = element.getAttribute(ITool.COMMAND);

        // Get the overridden output prefix, if any
        outputPrefix = element.getAttribute(ITool.OUTPUT_PREFIX);

        // Get the overridden output extensions (if any)
        String output = element.getAttribute(ITool.OUTPUTS);
        if (output != null) {
            outputExtensions = output;
        }

        // Get the flag to control output
        outputFlag = element.getAttribute(ITool.OUTPUT_FLAG);

        //        IManagedConfigElement[] toolElements = element.getChildren();
        //        for (int m = 0; m < toolElements.length; ++m) {
        //            IManagedConfigElement toolElement = toolElements[m];
        //            if (toolElement.getName().equals(ITool.OPTION_REF)) {
        //                new OptionReference(this, toolElement);
        //            }
        //        }
    }

    /**
     * Created a tool reference on the fly based on an existing tool or tool
     * reference.
     *
     * @param owner
     *            The <code>BuildObject</code> the receiver will be added to.
     * @param tool
     *            The <code>ITool</code>tool the reference will be based on.
     */
    public ToolReference(BuildObject owner, ITool tool) {
        this.owner = owner;
        if (tool instanceof ToolReference) {
            parent = ((ToolReference) tool).getTool();
        } else {
            parent = tool;
        }
        command = tool.getToolCommand();
        outputFlag = tool.getOutputFlag();
        outputPrefix = tool.getOutputPrefix();
        String[] extensions = tool.getOutputsAttribute();
        outputExtensions = ""; //$NON-NLS-1$
        if (extensions != null) {
            for (int index = 0; index < extensions.length; ++index) {
                if (extensions[index] == null)
                    continue;
                outputExtensions += extensions[index];
                if (index < extensions.length - 1) {
                    outputExtensions += DEFAULT_SEPARATOR;
                }
            }
        }

        //        // Create a copy of the option references of the parent in the receiver
        //        if (tool instanceof ToolReference) {
        //            List<OptionReference> parentRefs = ((ToolReference) tool).getOptionReferenceList();
        //            for (OptionReference parent : parentRefs) {
        //                OptionReference clone = createOptionReference(parent);
        //                try {
        //                    switch (parent.getValueType()) {
        //                    case IOption.BOOLEAN:
        //                        clone.setValue(parent.getBooleanValue());
        //                        break;
        //                    case IOption.STRING:
        //                    case IOption.TREE:
        //                        clone.setValue(parent.getStringValue());
        //                        break;
        //                    case IOption.ENUMERATED:
        //                        clone.setValue(parent.getSelectedEnum());
        //                        break;
        //                    default:
        //                        clone.setValue(parent.getStringListValue());
        //                        break;
        //                    }
        //                } catch (BuildException e) {
        //                    // Likely a mismatch between the value and option type
        //                    continue;
        //                }
        //            }
        //        }

        //        if (owner instanceof IConfigurationV2) {
        //            ((IConfigurationV2) owner).addToolReference(this);
        //        } else if (owner instanceof ITarget) {
        //            ((ITarget) owner).addToolReference(this);
        //        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#references(org.eclipse.cdt.managedbuilder.core.ITool)
     */
    @Override
    public boolean references(ITool target) {
        if (equals(target)) {
            // we are the target
            return true;
        } else if (parent == null) {
            // basic sanity before proceeding
            return false;
        } else if (parent instanceof IToolReference) {
            // check the reference we are overriding
            return ((IToolReference) parent).references(target);
        } else if (target instanceof IToolReference) {
            return parent.equals(((IToolReference) target).getTool());
        } else {
            // the real reference
            return parent.equals(target);
        }
    }

    public void resolveReferences() {
        if (!resolved) {
            resolved = true;
            IManagedConfigElement element = ManagedBuildManager.getConfigElement(this);
            // resolve my parent
            if (owner instanceof IConfigurationV2) {
                ITarget target = (ITarget) ((IConfigurationV2) owner).getTarget();
                parent = target.getTool(element.getAttribute(ID));
            } else if (owner instanceof ITarget) {
                parent = ((ITarget) owner).getTool(element.getAttribute(ID));
            }
            // recursively resolve my parent
            if (parent instanceof Tool) {
                ((Tool) parent).resolveReferences();
            } else if (parent instanceof ToolReference) {
                ((ToolReference) parent).resolveReferences();
            }

            //            List<OptionReference> optionReferenceList = getOptionReferenceList();
            //            for (OptionReference optRef : optionReferenceList) {
            //                optRef.resolveReferences();
            //            }
        }
    }

    //    /**
    //     * Adds the option reference specified in the argument to the receiver.
    //     */
    //    public void addOptionReference(OptionReference optionRef) {
    //        getOptionReferenceList().add(optionRef);
    //        isDirty = true;
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#buildsFileType(java.lang.String)
     */
    @Override
    public boolean buildsFileType(String extension) {
        if (parent == null) {
            // bad reference
            return false;
        }
        return parent.buildsFileType(extension);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#buildsFileType(java.lang.String)
     */
    @Override
    public boolean isInputFileType(String extension) {
        if (parent == null) {
            // bad reference
            return false;
        }
        return parent.isInputFileType(extension);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#createOptionReference(org.eclipse.cdt.managedbuilder.core.IOption)
     */
    //    @Override
    //    public OptionReference createOptionReference(IOption option) {
    //        // Check if the option reference already exists
    //        OptionReference ref = getOptionReference(option);
    //        // It is possible that the search will return an option reference
    //        // that is supplied by another element of the build model, not the caller.
    //        // For example, if the search is starated by a configuration and the target
    //        // the caller  belongs to has an option reference for the option, it
    //        // will be returned. While this is correct behaviour for a search, the
    //        // caller will need to create a clone for itself, so make sure the tool
    //        // reference of the search result is owned by the caller
    //        if (ref == null || !ref.getToolReference().owner.equals(this.owner)) {
    //            ref = new OptionReference(this, option);
    //        }
    //        return ref;
    //    }

    /* (non-Javadoc)
     * @return
     */
    //    protected List<OptionReference> getAllOptionRefs() {
    //        // First get all the option references this tool reference contains
    //        if (owner instanceof IConfigurationV2) {
    //            return ((IConfigurationV2) owner).getOptionReferences(parent);
    //        } else if (owner instanceof ITarget) {
    //            return ((ITarget) owner).getOptionReferences(parent);
    //        } else {
    //            // this shouldn't happen
    //            return null;
    //        }
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getId()
     */
    @Override
    public String getId() {
        if (parent == null) {
            // bad reference
            return ""; //$NON-NLS-1$
        }
        return parent.getId();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getBaseId()
     */
    @Override
    public String getBaseId() {
        if (parent == null) {
            // bad reference
            return ""; //$NON-NLS-1$
        }
        return parent.getBaseId();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getInputExtensions()
     */
    //@Override
    public List<String> getInputExtensions() {
        String[] exts = getPrimaryInputExtensions();
        List<String> extList = new ArrayList<>();
        for (int i = 0; i < exts.length; i++) {
            extList.add(exts[i]);
        }
        return extList;
    }

    /* (non-Javadoc)
    * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getName()
    */
    @Override
    public String getName() {
        if (parent == null) {
            // bad reference
            return ""; //$NON-NLS-1$
        }
        return parent.getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getNatureFilter()
     */
    @Override
    public int getNatureFilter() {
        if (parent == null) {
            // bad reference
            return ITool.FILTER_BOTH;
        }
        return parent.getNatureFilter();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOption(java.lang.String)
     */
    //@Override
    public IOption getOption(String id) {
        return getOptionById(id);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getOption(java.lang.String)
     */
    @Override
    public IOption getOptionById(String id) {
        IOption[] options = getOptions();
        for (int i = 0; i < options.length; i++) {
            IOption current = options[i];
            if (current.getId().equals(id)) {
                return current;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#producesFileType(java.lang.String)
     */
    @Override
    public boolean producesFileType(String outputExtension) {
        // Check if the reference produces this type of file
        if (!getOutputsList().contains(outputExtension)) {
            return parent.producesFileType(outputExtension);
        } else {
            return true;
        }

    }

    /* (non-Javadoc)
     * @return
     */
    private List<String> getOutputsList() {
        ArrayList<String> answer = new ArrayList<>();
        if (outputExtensions != null) {
            String[] exts = outputExtensions.split(DEFAULT_SEPARATOR);
            answer.addAll(Arrays.asList(exts));
        }
        return answer;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#getTool()
     */
    @Override
    public ITool getTool() {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getToolCommand()
     */
    @Override
    public String getToolCommand() {
        if (command == null) {
            // see if the parent has one
            if (parent == null) {
                // bad reference
                return ""; //$NON-NLS-1$
            }
            return parent.getToolCommand();
        }
        return command;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getToolFlags()
     */
    //@Override
    public String getToolFlags() throws BuildException {
        // Get all of the options
        StringBuilder buf = new StringBuilder();
        IOption[] opts = getOptions();
        for (int index = 0; index < opts.length; index++) {
            IOption option = opts[index];

            // check to see if the option has an applicability calculator
            IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();

            boolean optionIsApplicable = true;
            if (applicabilityCalculator != null) {
                ITool tool = getTool();
                IBuildObject config;
                if (tool.getParent() instanceof IResourceConfiguration) {
                    config = tool.getParent();
                } else {
                    config = ((IToolChain) tool.getParent()).getParent();
                }
                optionIsApplicable = applicabilityCalculator.isOptionUsedInCommandLine(config, tool, option);
            }
            if (optionIsApplicable) {
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
                        buf.append(boolCmd).append(WHITE_SPACE);
                    }
                    break;

                case IOption.ENUMERATED:
                    String enumVal = option.getEnumCommand(option.getSelectedEnum());
                    if (enumVal.length() > 0) {
                        buf.append(enumVal).append(WHITE_SPACE);
                    }
                    break;

                case IOption.TREE:
                    String treeVal = option.getCommand(option.getStringValue());
                    if (treeVal.length() > 0) {
                        buf.append(treeVal).append(WHITE_SPACE);
                    }
                    break;

                case IOption.STRING:
                    String strCmd = option.getCommand();
                    String val = option.getStringValue();
                    if (val.length() > 0) {
                        if (strCmd != null)
                            buf.append(strCmd);
                        buf.append(val).append(WHITE_SPACE);
                    }
                    break;

                case IOption.STRING_LIST:
                    String cmd = option.getCommand();
                    String[] list = option.getStringListValue();
                    for (int j = 0; j < list.length; j++) {
                        String temp = list[j];
                        if (cmd != null)
                            buf.append(cmd);
                        buf.append(temp).append(WHITE_SPACE);
                    }
                    break;

                case IOption.INCLUDE_PATH:
                    String incCmd = option.getCommand();
                    String[] paths = option.getIncludePaths();
                    for (int j = 0; j < paths.length; j++) {
                        String temp = paths[j];
                        buf.append(incCmd).append(temp).append(WHITE_SPACE);
                    }
                    break;

                case IOption.PREPROCESSOR_SYMBOLS:
                    String defCmd = option.getCommand();
                    String[] symbols = option.getDefinedSymbols();
                    for (int j = 0; j < symbols.length; j++) {
                        String temp = symbols[j];
                        buf.append(defCmd).append(temp).append(WHITE_SPACE);
                    }
                    break;

                default:
                    break;
                }
            }
        }

        return buf.toString().trim();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getTopOptionCategory()
     */
    @Override
    public IOptionCategory getTopOptionCategory() {
        try {
            return parent.getTopOptionCategory();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /* (non-Javadoc)
     * Answers an option reference that overrides the option, or <code>null</code>
     *
     * @param option
     * @return OptionReference
     */
    //    private OptionReference getOptionReference(IOption option) {
    //        // Get all the option references for this option
    //        List<OptionReference> allOptionRefs = getAllOptionRefs();
    //        for (OptionReference optionRef : allOptionRefs) {
    //            if (optionRef.references(option))
    //                return optionRef;
    //        }
    //
    //        return null;
    //    }

    /* (non-Javadoc)
     *
     * @param id
     * @return
     */
    /*
    private OptionReference getOptionReference(String id) {
    	Iterator it = getOptionReferenceList().iterator();
    	while (it.hasNext()) {
    		OptionReference current = (OptionReference)it.next();
    		if (current.getId().equals(id)) {
    			return current;
    		}
    	}
    	return null;
    }
    */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#getOptionReferenceList()
     */
    //    @Override
    //    public List<OptionReference> getOptionReferenceList() {
    //        if (optionReferences == null) {
    //            optionReferences = new ArrayList<>();
    //        }
    //        return optionReferences;
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getOptions()
     */
    //    @Override
    //    public IOption[] getOptions() {
    //        IOption[] options = parent.getOptions();
    //
    //        // Replace with our references
    //        for (int i = 0; i < options.length; ++i) {
    //            OptionReference ref = getOptionReference(options[i]);
    //            if (ref != null)
    //                options[i] = ref;
    //        }
    //
    //        return options;
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputExtensions()
     */
    //@Override
    public String[] getOutputExtensions() {
        if (outputExtensions == null) {
            if (parent != null) {
                return parent.getOutputsAttribute();
            } else {
                return new String[0];
            }
        }
        return outputExtensions.split(DEFAULT_SEPARATOR);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputExtension(java.lang.String)
     */
    @Override
    public String getOutputExtension(String inputExtension) {
        if (parent == null) {
            // bad reference
            return ""; //$NON-NLS-1$
        }
        return parent.getOutputExtension(inputExtension);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputFlag()
     */
    @Override
    public String getOutputFlag() {
        if (outputFlag == null) {
            if (parent != null) {
                return parent.getOutputFlag();
            } else {
                // We never should be here
                return ""; //$NON-NLS-1$
            }
        }
        return outputFlag;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getOutputPrefix()
     */
    @Override
    public String getOutputPrefix() {
        if (outputPrefix == null) {
            if (parent != null) {
                return parent.getOutputPrefix();
            }
            return ""; // bad reference //$NON-NLS-1$
        }
        return outputPrefix;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#isDirty()
     */
    @Override
    public boolean isDirty() {
        return isDirty;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#isHeaderFile(java.lang.String)
     */
    @Override
    public boolean isHeaderFile(String ext) {
        if (parent == null) {
            // bad reference
            return false;
        }
        return parent.isHeaderFile(ext);
    }

    /**
     * Answers <code>true</code> if the owner of the receiver matches
     * the argument.
     */
    public boolean ownedByConfiguration(IConfigurationV2 config) {
        if (owner instanceof IConfigurationV2) {
            return ((IConfigurationV2) owner).equals(config);
        }
        return false;
    }

    /**
     * Persist receiver to project file.
     *
     * @param doc
     *            The persistent store for the reference information.
     * @param element
     *            The root element in the store the receiver must use
     *            to persist settings.
     */
    public void serialize(Document doc, Element element) {
        if (parent == null)
            return; // This is a bad reference
        element.setAttribute(ITool.ID, parent.getId());

        // Output the command
        if (command != null) {
            element.setAttribute(ITool.COMMAND, getToolCommand());
        }

        // Save output prefix
        if (outputPrefix != null) {
            element.setAttribute(ITool.OUTPUT_PREFIX, getOutputPrefix());
        }

        // Save the output flag
        if (outputFlag != null) {
            element.setAttribute(ITool.OUTPUT_FLAG, getOutputFlag());
        }

        // Save the outputs
        if (outputExtensions != null) {
            element.setAttribute(ITool.OUTPUTS, outputExtensions);
        }

        //        // Output the option references
        //        List<OptionReference> optionReferenceList = getOptionReferenceList();
        //        for (OptionReference optionRef : optionReferenceList) {
        //            Element optionRefElement = doc.createElement(ITool.OPTION_REF);
        //            element.appendChild(optionRefElement);
        //            optionRef.serialize(doc, optionRefElement);
        //        }

        // Set the reference to clean
        isDirty = false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
        // Override the local flag
        this.isDirty = isDirty;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#setToolCommand(java.lang.String)
     */
    @Override
    public boolean setToolCommand(String cmd) {
        if (cmd != null && !cmd.equals(command)) {
            command = cmd;
            isDirty = true;
            return true;
        } else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setOutputFlag(java.lang.String)
     */
    @Override
    public void setOutputFlag(String flag) {
        if (flag == null)
            return;
        if (outputFlag == null || !(flag.equals(outputFlag))) {
            outputFlag = flag;
            isDirty = true;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setOutputPrefix(java.lang.String)
     */
    @Override
    public void setOutputPrefix(String prefix) {
        if (prefix == null)
            return;
        if (outputPrefix == null || !(prefix.equals(outputPrefix))) {
            outputPrefix = prefix;
            isDirty = true;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setOutputExtensions(java.lang.String)
     */
    public void setOutputExtensions(String ext) {
        if (ext == null)
            return;
        if (outputExtensions == null || !(ext.equals(outputExtensions))) {
            outputExtensions = ext;
            isDirty = true;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setParent(IBuildObject)
     */
    public void setToolParent(IBuildObject newParent) {
        if (parent == null) {
            // bad reference
            return;
        }
        // Set the parent in the parent of this ToolRefernce, the tool
        ((Tool) parent).setToolParent(newParent);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getParent()
     */
    @Override
    public IBuildObject getParent() {
        return owner;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getCommandLinePattern()
     */
    @Override
    public String getCommandLinePattern() {
        if (parent == null)
            return ""; //$NON-NLS-1$
        return parent.getCommandLinePattern();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getCommandLineGenerator()
     */
    @Override
    public IManagedCommandLineGenerator getCommandLineGenerator() {
        if (parent == null)
            return null;
        return parent.getCommandLineGenerator();
    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITool#getDependencyGenerator()
    //     */
    //    @Override
    //    public IManagedDependencyGenerator getDependencyGenerator() {
    //        if (parent == null)
    //            return null;
    //        return parent.getDependencyGenerator();
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getCommandFlags()
     */
    //@Override
    public String[] getCommandFlags() throws BuildException {
        if (parent == null)
            return null;
        return parent.getToolCommandFlags(null, null);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getToolCommandFlags(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
     */
    @Override
    public String[] getToolCommandFlags(IPath inputFileLocation, IPath outputFileLocation) throws BuildException {
        return getCommandFlags();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getToolCommandFlagsString(org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IPath)
     */
    @Override
    public String getToolCommandFlagsString(IPath inputFileLocation, IPath outputFileLocation) throws BuildException {
        return getToolFlags();

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String answer = ""; //$NON-NLS-1$
        if (parent != null) {
            answer += "Reference to " + parent.getName(); //$NON-NLS-1$
        }

        if (answer.length() > 0) {
            return answer;
        } else {
            return super.toString();
        }
    }

    /*
     * The following methods are here in order to implement the new ITool methods.
     * They should never be called.
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getSuperClass()
     */
    @Override
    public ITool getSuperClass() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#isAbstract()
     */
    @Override
    public boolean isAbstract() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getUnusedChildren()
     */
    @Override
    public String getUnusedChildren() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getErrorParserList()
     */
    @Override
    public String[] getErrorParserList() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getErrorParserIds()
     */
    @Override
    public String getErrorParserIds() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setErrorParserIds()
     */
    @Override
    public void setErrorParserIds(String ids) {
    }

    //@Override
    public List<String> getInterfaceExtensions() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#isExtensionElement()
     */
    @Override
    public boolean isExtensionElement() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#createOption()
     */
    @Override
    public IOption createOption(IOption superClass, String Id, String name, boolean b) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#createOptions()
     */
    @Override
    public void createOptions(IHoldsOptions options) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#removeOption()
     */
    @Override
    public void removeOption(IOption o) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getChildCategories()
     */
    @Override
    public IOptionCategory[] getChildCategories() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setIsAbstract(boolean)
     */
    @Override
    public void setIsAbstract(boolean b) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getCommandLinePattern()
     */
    @Override
    public void setCommandLinePattern(String pattern) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#setCommandLineGenerator(IConfigurationElement)
     */
    public void setCommandLineGeneratorElement(IConfigurationElement element) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getCommandLineGenerator()
     */
    public IConfigurationElement getDependencyGeneratorElement() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#setCommandLineGenerator(IConfigurationElement)
     */
    public void setDependencyGeneratorElement(IConfigurationElement element) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getCommandLineGeneratorElement()
     */
    public IConfigurationElement getCommandLineGeneratorElement() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getAdvancedInputCategory()
     */
    @Override
    public boolean getAdvancedInputCategory() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#setAdvancedInputCategory()
     */
    @Override
    public void setAdvancedInputCategory(boolean display) {
    }

    @Override
    public IInputType createInputType(IInputType superClass, String Id, String name, boolean isExtensionElement) {
        return null;
    }

    @Override
    public IOutputType createOutputType(IOutputType superClass, String Id, String name, boolean isExtensionElement) {
        return null;
    }

    @Override
    public String getAnnouncement() {
        return null;
    }

    @Override
    public IInputType getInputTypeById(String id) {
        return null;
    }

    @Override
    public IInputType[] getInputTypes() {
        return null;
    }

    @Override
    public IOutputType getOutputTypeById(String id) {
        return null;
    }

    @Override
    public IOutputType[] getOutputTypes() {
        return null;
    }

    @Override
    public void removeInputType(IInputType type) {
    }

    @Override
    public void removeOutputType(IOutputType type) {
    }

    @Override
    public void setAnnouncement(String announcement) {
    }

    @Override
    public String getDefaultInputExtension() {
        return null;
    }

    @Override
    public String[] getAllInputExtensions() {
        return null;
    }

    @Override
    public String[] getPrimaryInputExtensions() {
        return null;
    }

    @Override
    public IInputType getInputType(String inputExtension) {
        return null;
    }

    @Override
    public String[] getOutputsAttribute() {
        return null;
    }

    @Override
    public IOutputType getOutputType(String outputExtension) {
        return null;
    }

    @Override
    public void setOutputsAttribute(String extensions) {
    }

    @Override
    public String[] getAllOutputExtensions() {
        return null;
    }

    @Override
    public String[] getAllDependencyExtensions() {
        return null;
    }

    @Override
    public IInputType getPrimaryInputType() {
        return null;
    }

    @Override
    public IOutputType getPrimaryOutputType() {
        return null;
    }

    @Override
    public IPath[] getAdditionalDependencies() {
        return null;
    }

    @Override
    public IPath[] getAdditionalResources() {
        return null;
    }

    public IConfigurationElement getDependencyGeneratorElementForExtension(String sourceExt) {
        return null;
    }

    //    @Override
    //    public IManagedDependencyGeneratorType getDependencyGeneratorForExtension(String sourceExt) {
    //        return null;
    //    }

    @Override
    public boolean getCustomBuildStep() {
        return false;
    }

    @Override
    public void setCustomBuildStep(boolean customBuildStep) {
    }

    @Override
    public IOption getOptionBySuperClassId(String id) {
        return null;
    }

    @Override
    public IOptionCategory getOptionCategory(String id) {
        // return null as class is deprecated
        return null;
    }

    @Override
    public void addOptionCategory(IOptionCategory category) {
    }

    @Override
    public void setHidden(boolean hidden) {
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    /*
     * The following methods are added to allow the converter from ToolReference -> Tool
     * to retrieve the actual value of attributes.  These routines do not go to the
     * referenced Tool for a value if the ToolReference does not have a value.
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#getRawOutputExtensions()
     */
    @Override
    public String getRawOutputExtensions() {
        return outputExtensions;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#getRawOutputFlag()
     */
    @Override
    public String getRawOutputFlag() {
        return outputFlag;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#getRawOutputPrefix()
     */
    @Override
    public String getRawOutputPrefix() {
        return outputPrefix;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolReference#getRawToolCommand()
     */
    @Override
    public String getRawToolCommand() {
        return command;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getConvertToId()
     */
    @Override
    public String getConvertToId() {
        if (convertToId == null) {
            // If I have a superClass, ask it
            if (parent != null) {
                return parent.getConvertToId();
            } else {
                return ""; //$NON-NLS-1$
            }
        }
        return convertToId;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setConvertToId(String)
     */
    @Override
    public void setConvertToId(String convertToId) {
        if (convertToId == null && this.convertToId == null)
            return;
        if (convertToId == null || this.convertToId == null || !convertToId.equals(this.convertToId)) {
            this.convertToId = convertToId;
            setDirty(true);
        }
        return;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getVersionsSupported()
     */
    @Override
    public String getVersionsSupported() {
        if (versionsSupported == null) {
            // If I have a superClass, ask it
            if (parent != null) {
                return parent.getVersionsSupported();
            } else {
                return ""; //$NON-NLS-1$
            }
        }
        return versionsSupported;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#setVersionsSupported(String)
     */
    @Override
    public void setVersionsSupported(String versionsSupported) {
        if (versionsSupported == null && this.versionsSupported == null)
            return;
        if (versionsSupported == null || this.versionsSupported == null
                || !versionsSupported.equals(this.versionsSupported)) {
            this.versionsSupported = versionsSupported;
            setDirty(true);
        }
        return;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITool#getEnvVarBuildPaths()
     */
    @Override
    public IEnvVarBuildPath[] getEnvVarBuildPaths() {
        return null;
    }

    @Override
    public Version getVersion() {
        return null;
    }

    @Override
    public void setVersion(Version version) {
        // TODO Auto-generated method stub
    }

    @Override
    public String getManagedBuildRevision() {
        return null;
    }

    @Override
    public IOption getOptionToSet(IOption option, boolean adjustExtension) {
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#needsRebuild()
     */
    @Override
    public boolean needsRebuild() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#setRebuildState(boolean)
     */
    @Override
    public void setRebuildState(boolean rebuild) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITool#getPathConverter()
     */
    @Override
    public IOptionPathConverter getOptionPathConverter() {
        if (parent != null) {
            return parent.getOptionPathConverter();
        }
        return null;
    }

    @Override
    public CLanguageData getCLanguageData(IInputType type) {
        return null;
    }

    @Override
    public CLanguageData[] getCLanguageDatas() {
        return new CLanguageData[0];
    }

    @Override
    public IInputType getInputTypeForCLanguageData(CLanguageData data) {
        return null;
    }

    @Override
    public IResourceInfo getParentResourceInfo() {
        return null;
    }

    @Override
    public IInputType getEditableInputType(IInputType base) {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public boolean isReal() {
        return false;
    }

    public boolean supportsManagedBuild() {
        return true;
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        return true;
    }

    @Override
    public boolean matches(ITool tool) {
        return false;
    }

    @Override
    public boolean isSystemObject() {
        return false;
    }

    @Override
    public String getUniqueRealName() {
        return getName();
    }

    @Override
    public IOutputType getEditableOutputType(IOutputType base) {
        return null;
    }

    @Override
    public void setOutputPrefixForPrimaryOutput(String prefix) {
        setOutputPrefix(prefix);
    }

    @Override
    public IOption[] getOptions() {
        // TODO Auto-generated method stub
        return null;
    }
}
