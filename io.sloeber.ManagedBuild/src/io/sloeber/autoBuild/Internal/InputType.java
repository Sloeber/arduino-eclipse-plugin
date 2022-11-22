/*******************************************************************************
 * Copyright (c) 2005, 2019 Intel Corporation and others.
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
 * EclipseSource
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.internal.core.SafeStringInterner;
//import org.eclipse.cdt.managedbuilder.core.IAdditionalInput;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.core.IInputOrder;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.ILanguageInfoCalculator;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IProjectType;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.internal.enablement.OptionEnablementExpression;
//import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IAdditionalInput;
import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IInputOrder;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.ILanguageInfoCalculator;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;

public class InputType extends BuildObject implements IInputType {

    private static final String DEFAULT_SEPARATOR = ","; //$NON-NLS-1$
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    //  Superclass
    private IInputType superClass;
    private String superClassId;
    //  Parent and children
    private ITool parent;
    private Vector<InputOrder> inputOrderList;
    private Vector<AdditionalInput> additionalInputList;
    //  Managed Build model attributes
    private String[] sourceContentTypeIds;
    private IContentType[] sourceContentTypes;
    private String[] headerContentTypeIds;
    private IContentType[] headerContentTypes;
    private String[] inputExtensions;
    private String[] headerExtensions;
    private String dependencyContentTypeId;
    private IContentType dependencyContentType;
    private List<String> dependencyExtensions;
    private String optionId;
    private String assignToOptionId;
    private String buildVariable;
    private Boolean multipleOfType;
    private Boolean primaryInput;
    //private IConfigurationElement dependencyGeneratorElement = null;
    //    private IManagedDependencyGeneratorType dependencyGenerator = null;
    private String languageId;
    private String languageName;
    private IConfigurationElement languageInfoCalculatorElement;
    private ILanguageInfoCalculator languageInfoCalculator;
    private String buildInfoDicsoveryProfileId;

    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    //  Miscellaneous
    private boolean isExtensionInputType = false;
    private boolean isDirty = false;
    private boolean resolved = true;
    private boolean rebuildState;

    //	private class DefaultLanguageInfoCalculator implements ILanguageInfoCalculator {
    //
    //		public String getLanguageId(IResourceInfo rcInfo, ITool tool, IInputType type) {
    //			if(languageId == null && superClass != null)
    //				return ((InputType)superClass).getLanguageInfoCalculator().getLanguageId(rcInfo, tool, type);
    //			return languageId;
    //		}
    //
    //		public String getLanguageName(IResourceInfo rcInfo, ITool tool, IInputType type) {
    //			if(languageName == null && superClass != null)
    //				return ((InputType)superClass).getLanguageInfoCalculator().getLanguageName(rcInfo, tool, type);
    //			return languageName;
    //		}
    //	}
    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create an InputType defined by an extension
     * point in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The ITool parent of this InputType
     * @param element
     *            The InputType definition from the manifest file or a dynamic
     *            element
     *            provider
     */
    public InputType(ITool parent, IManagedConfigElement element) {
        this.parent = parent;
        isExtensionInputType = true;

        // setup for resolving
        resolved = false;

        loadFromManifest(element);

        IManagedConfigElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        if (enablements.length > 0)
            booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(enablements);

        // Hook me up to the Managed Build Manager
        ManagedBuildManager.addExtensionInputType(this);

        // Load Children
        IManagedConfigElement[] iElements = element.getChildren();
        for (IManagedConfigElement elem : iElements) {
            if (elem.getName().equals(IInputOrder.INPUT_ORDER_ELEMENT_NAME)) {
                InputOrder inputOrder = new InputOrder(this, elem);
                getInputOrderList().add(inputOrder);
            } else if (elem.getName().equals(IAdditionalInput.ADDITIONAL_INPUT_ELEMENT_NAME)) {
                AdditionalInput addlInput = new AdditionalInput(this, elem);
                getAdditionalInputList().add(addlInput);
            }
        }
    }

    /**
     * This constructor is called to create an InputType whose attributes and
     * children will be
     * added by separate calls.
     *
     * @param parent
     *            - The parent of the an InputType
     * @param superClass
     *            - The superClass, if any
     * @param Id
     *            - The id for the new InputType
     * @param name
     *            - The name for the new InputType
     * @param isExtensionElement
     *            - Indicates whether this is an extension element or a managed
     *            project element
     */
    public InputType(Tool parent, IInputType superClass, String Id, String name, boolean isExtensionElement) {
        this.parent = parent;
        this.superClass = superClass;
        if (this.superClass != null) {
            superClassId = this.superClass.getId();
        }
        setId(Id);
        setName(name);

        isExtensionInputType = isExtensionElement;
        if (isExtensionElement) {
            // Hook me up to the Managed Build Manager
            ManagedBuildManager.addExtensionInputType(this);
        } else {
            setDirty(true);
            setRebuildState(true);
        }
    }

    /**
     * Create an <code>InputType</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>ITool</code> the InputType will be added to.
     * @param element
     *            The XML element that contains the InputType settings.
     *
     */
    public InputType(ITool parent, ICStorageElement element) {
        this.parent = parent;
        isExtensionInputType = false;

        // Initialize from the XML attributes
        loadFromProject(element);

        // Load children
        ICStorageElement configElements[] = element.getChildren();
        for (ICStorageElement configElement : configElements) {
            if (configElement.getName().equals(IInputOrder.INPUT_ORDER_ELEMENT_NAME)) {
                InputOrder inputOrder = new InputOrder(this, configElement);
                getInputOrderList().add(inputOrder);
            } else if (configElement.getName().equals(IAdditionalInput.ADDITIONAL_INPUT_ELEMENT_NAME)) {
                AdditionalInput addlInput = new AdditionalInput(this, configElement);
                getAdditionalInputList().add(addlInput);
            }
        }
    }

    /**
     * Create an <code>InputType</code> based upon an existing InputType.
     *
     * @param parent
     *            The <code>ITool</code> the InputType will be added to.
     * @param Id
     *            The identifier of the new InputType
     * @param name
     *            The name of the new InputType
     * @param inputType
     *            The existing InputType to clone.
     */
    public InputType(ITool parent, String Id, String name, InputType inputType) {
        this.parent = parent;
        superClass = inputType.superClass;
        if (superClass != null && inputType.superClassId != null) {
            superClassId = inputType.superClassId;
        }
        setId(Id);
        setName(name);

        isExtensionInputType = false;
        boolean copyIds = Id.equals(inputType.id);

        //  Copy the remaining attributes

        if (inputType.sourceContentTypeIds != null) {
            sourceContentTypeIds = inputType.sourceContentTypeIds.clone();
        }
        if (inputType.sourceContentTypes != null) {
            sourceContentTypes = inputType.sourceContentTypes.clone();
        }
        if (inputType.inputExtensions != null) {
            inputExtensions = inputType.inputExtensions.clone();
        }
        if (inputType.headerContentTypeIds != null) {
            headerContentTypeIds = inputType.headerContentTypeIds.clone();
        }
        if (inputType.headerContentTypes != null) {
            headerContentTypes = inputType.headerContentTypes.clone();
        }
        if (inputType.headerExtensions != null) {
            headerExtensions = inputType.headerExtensions.clone();
        }

        if (inputType.dependencyContentTypeId != null) {
            dependencyContentTypeId = inputType.dependencyContentTypeId;
        }
        dependencyContentType = inputType.dependencyContentType;
        if (inputType.dependencyExtensions != null) {
            dependencyExtensions = new ArrayList<>(inputType.dependencyExtensions);
        }
        if (inputType.optionId != null) {
            optionId = inputType.optionId;
        }
        if (inputType.assignToOptionId != null) {
            assignToOptionId = inputType.assignToOptionId;
        }
        if (inputType.buildVariable != null) {
            buildVariable = inputType.buildVariable;
        }
        if (inputType.multipleOfType != null) {
            multipleOfType = inputType.multipleOfType;
        }
        if (inputType.primaryInput != null) {
            primaryInput = inputType.primaryInput;
        }
        //dependencyGeneratorElement = inputType.dependencyGeneratorElement;
        //dependencyGenerator = inputType.dependencyGenerator;

        languageId = inputType.languageId;
        languageName = inputType.languageName;
        languageInfoCalculatorElement = inputType.languageInfoCalculatorElement;
        languageInfoCalculator = inputType.languageInfoCalculator;
        buildInfoDicsoveryProfileId = inputType.buildInfoDicsoveryProfileId;

        //  Clone the children
        if (inputType.inputOrderList != null) {
            for (InputOrder inputOrder : inputType.getInputOrderList()) {
                InputOrder newInputOrder = new InputOrder(this, inputOrder, copyIds);
                getInputOrderList().add(newInputOrder);
            }
        }
        if (inputType.additionalInputList != null) {
            for (AdditionalInput additionalInput : inputType.getAdditionalInputList()) {
                AdditionalInput newAdditionalInput = new AdditionalInput(this, additionalInput, copyIds);
                getAdditionalInputList().add(newAdditionalInput);
            }
        }

        if (copyIds) {
            isDirty = inputType.isDirty;
            rebuildState = inputType.rebuildState;
        } else {
            setDirty(true);
            setRebuildState(true);
        }
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /* (non-Javadoc)
     * Loads the InputType information from the ManagedConfigElement specified in the
     * argument.
     *
     * @param element Contains the InputType information
     */
    protected void loadFromManifest(IManagedConfigElement element) {
        ManagedBuildManager.putConfigElement(this, element);

        // id
        setId(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.ID)));

        // Get the name
        setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));

        // sourceContentType
        List<String> list = new ArrayList<>();
        String ids = element.getAttribute(IInputType.SOURCE_CONTENT_TYPE);
        if (ids != null) {
            StringTokenizer tokenizer = new StringTokenizer(ids, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
            }
            if (list.size() != 0) {
                sourceContentTypeIds = list.toArray(new String[list.size()]);
                list.clear();
            }
        }

        // Get the supported input file extensions
        String inputs = element.getAttribute(ITool.SOURCES);
        if (inputs != null) {
            StringTokenizer tokenizer = new StringTokenizer(inputs, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
            }

            if (list.size() != 0) {
                inputExtensions = list.toArray(new String[list.size()]);
                list.clear();
            }
        }

        // headerContentType
        ids = element.getAttribute(IInputType.HEADER_CONTENT_TYPE);
        if (ids != null) {
            StringTokenizer tokenizer = new StringTokenizer(ids, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
            }
            if (list.size() != 0) {
                headerContentTypeIds = list.toArray(new String[list.size()]);
                list.clear();
            }
        }

        // Get the supported header file extensions
        String hs = element.getAttribute(HEADERS);
        if (hs != null) {
            StringTokenizer tokenizer = new StringTokenizer(hs, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
            }

            if (list.size() != 0) {
                headerExtensions = list.toArray(new String[list.size()]);
                list.clear();
            }
        }

        // dependencyContentType
        dependencyContentTypeId = element.getAttribute(IInputType.DEPENDENCY_CONTENT_TYPE);

        // Get the dependency (header file) extensions
        String headers = element.getAttribute(IInputType.DEPENDENCY_EXTENSIONS);
        if (headers != null) {
            StringTokenizer tokenizer = new StringTokenizer(headers, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                getDependencyExtensionsList().add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
            }
        }

        // option
        optionId = SafeStringInterner.safeIntern(element.getAttribute(IInputType.OPTION));

        // assignToOption
        assignToOptionId = SafeStringInterner.safeIntern(element.getAttribute(IInputType.ASSIGN_TO_OPTION));

        // multipleOfType
        String isMOT = element.getAttribute(IInputType.MULTIPLE_OF_TYPE);
        if (isMOT != null) {
            multipleOfType = Boolean.parseBoolean(isMOT);
        }

        // primaryInput
        String isPI = element.getAttribute(IInputType.PRIMARY_INPUT);
        if (isPI != null) {
            primaryInput = Boolean.parseBoolean(isPI);
        }

        // buildVariable
        buildVariable = SafeStringInterner.safeIntern(element.getAttribute(IInputType.BUILD_VARIABLE));

        languageId = SafeStringInterner.safeIntern(element.getAttribute(LANGUAGE_ID));
        languageName = SafeStringInterner.safeIntern(element.getAttribute(LANGUAGE_NAME));
        if (element.getAttribute(LANGUAGE_INFO_CALCULATOR) != null && element instanceof DefaultManagedConfigElement) {
            languageInfoCalculatorElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
        }
        //		else {
        //			languageInfoCalculator = new DefaultLanguageInfoCalculator();
        //		}
        buildInfoDicsoveryProfileId = SafeStringInterner.safeIntern(element.getAttribute(SCANNER_CONFIG_PROFILE_ID));

        //        // Store the configuration element IFF there is a dependency generator defined
        //        String depGenerator = element.getAttribute(ITool.DEP_CALC_ID);
        //        if (depGenerator != null && element instanceof DefaultManagedConfigElement) {
        //            dependencyGeneratorElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
        //        }
    }

    /* (non-Javadoc)
     * Initialize the InputType information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the InputType information
     */
    protected boolean loadFromProject(ICStorageElement element) {
        // id
        // note: IDs are unique so no benefit to intern them
        setId(element.getAttribute(IBuildObject.ID));

        // name
        if (element.getAttribute(IBuildObject.NAME) != null) {
            setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));
        }

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
        if (superClassId != null && superClassId.length() > 0) {
            superClass = ManagedBuildManager.getExtensionInputType(superClassId);
            if (superClass == null) {
                // TODO:  Report error
            }
        }

        // sourceContentType
        IContentTypeManager manager = Platform.getContentTypeManager();
        List<String> list = new ArrayList<>();
        if (element.getAttribute(IInputType.SOURCE_CONTENT_TYPE) != null) {
            String ids = element.getAttribute(IInputType.SOURCE_CONTENT_TYPE);
            if (ids != null) {
                StringTokenizer tokenizer = new StringTokenizer(ids, DEFAULT_SEPARATOR);
                while (tokenizer.hasMoreElements()) {
                    list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
                }

                if (list.size() != 0) {
                    sourceContentTypeIds = list.toArray(new String[list.size()]);
                    list.clear();
                }

                if (sourceContentTypeIds != null) {
                    List<IContentType> types = new ArrayList<>();
                    for (String sourceContentTypeId : sourceContentTypeIds) {
                        IContentType type = manager.getContentType(sourceContentTypeId);
                        if (type != null)
                            types.add(type);
                    }

                    if (types.size() != 0) {
                        sourceContentTypes = types.toArray(new IContentType[types.size()]);
                    }
                }
            }
        }

        // sources
        if (element.getAttribute(IInputType.SOURCES) != null) {
            String inputs = element.getAttribute(ITool.SOURCES);
            if (inputs != null) {
                StringTokenizer tokenizer = new StringTokenizer(inputs, DEFAULT_SEPARATOR);
                while (tokenizer.hasMoreElements()) {
                    list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
                }

                if (list.size() != 0) {
                    inputExtensions = list.toArray(new String[list.size()]);
                    list.clear();
                }
            }
        }

        //header content types
        if (element.getAttribute(IInputType.HEADER_CONTENT_TYPE) != null) {
            String ids = element.getAttribute(IInputType.HEADER_CONTENT_TYPE);
            if (ids != null) {
                StringTokenizer tokenizer = new StringTokenizer(ids, DEFAULT_SEPARATOR);
                while (tokenizer.hasMoreElements()) {
                    list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
                }

                if (list.size() != 0) {
                    headerContentTypeIds = list.toArray(new String[list.size()]);
                    list.clear();
                }

                if (headerContentTypeIds != null) {
                    List<IContentType> types = new ArrayList<>();
                    for (String headerContentTypeId : headerContentTypeIds) {
                        IContentType type = manager.getContentType(headerContentTypeId);
                        if (type != null)
                            types.add(type);
                    }

                    if (types.size() != 0) {
                        headerContentTypes = types.toArray(new IContentType[types.size()]);
                    }
                }
            }
        }

        // headers
        if (element.getAttribute(IInputType.HEADERS) != null) {
            String inputs = element.getAttribute(HEADERS);
            if (inputs != null) {
                StringTokenizer tokenizer = new StringTokenizer(inputs, DEFAULT_SEPARATOR);
                while (tokenizer.hasMoreElements()) {
                    list.add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
                }

                if (list.size() != 0) {
                    headerExtensions = list.toArray(new String[list.size()]);
                    list.clear();
                }
            }
        }

        // dependencyContentType
        if (element.getAttribute(IInputType.DEPENDENCY_CONTENT_TYPE) != null) {
            dependencyContentTypeId = element.getAttribute(IInputType.DEPENDENCY_CONTENT_TYPE);
            if (dependencyContentTypeId != null && dependencyContentTypeId.length() > 0) {
                dependencyContentType = manager.getContentType(dependencyContentTypeId);
            }
        }

        // dependencyExtensions
        // Get the dependency (header file) extensions
        if (element.getAttribute(IInputType.DEPENDENCY_EXTENSIONS) != null) {
            String headers = element.getAttribute(IInputType.DEPENDENCY_EXTENSIONS);
            if (headers != null) {
                StringTokenizer tokenizer = new StringTokenizer(headers, DEFAULT_SEPARATOR);
                while (tokenizer.hasMoreElements()) {
                    getDependencyExtensionsList().add(SafeStringInterner.safeIntern(tokenizer.nextToken()));
                }
            }
        }

        // option
        if (element.getAttribute(IInputType.OPTION) != null) {
            optionId = SafeStringInterner.safeIntern(element.getAttribute(IInputType.OPTION));
        }

        // assignToOption
        if (element.getAttribute(IInputType.ASSIGN_TO_OPTION) != null) {
            assignToOptionId = SafeStringInterner.safeIntern(element.getAttribute(IInputType.ASSIGN_TO_OPTION));
        }

        // multipleOfType
        if (element.getAttribute(IInputType.MULTIPLE_OF_TYPE) != null) {
            String isMOT = element.getAttribute(IInputType.MULTIPLE_OF_TYPE);
            if (isMOT != null) {
                multipleOfType = Boolean.parseBoolean(isMOT);
            }
        }

        // primaryInput
        if (element.getAttribute(IInputType.PRIMARY_INPUT) != null) {
            String isPI = element.getAttribute(IInputType.PRIMARY_INPUT);
            if (isPI != null) {
                primaryInput = Boolean.parseBoolean(isPI);
            }
        }

        // buildVariable
        if (element.getAttribute(IInputType.BUILD_VARIABLE) != null) {
            buildVariable = SafeStringInterner.safeIntern(element.getAttribute(IInputType.BUILD_VARIABLE));
        }

        languageId = SafeStringInterner.safeIntern(element.getAttribute(LANGUAGE_ID));
        languageName = SafeStringInterner.safeIntern(element.getAttribute(LANGUAGE_NAME));
        buildInfoDicsoveryProfileId = SafeStringInterner.safeIntern(element.getAttribute(SCANNER_CONFIG_PROFILE_ID));

        // Note: dependency generator cannot be specified in a project file because
        //       an IConfigurationElement is needed to load it!
        if (element.getAttribute(ITool.DEP_CALC_ID) != null) {
            // TODO:  Issue warning?
        }

        return true;
    }

    private String composeString(String array[], String separator) {
        if (array == null)
            return null;
        if (array.length == 0)
            return ""; //$NON-NLS-1$

        StringBuilder buf = new StringBuilder();
        buf.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            buf.append(separator).append(array[i]);
        }

        return buf.toString();
    }

    /**
     * Persist the InputType to the project file.
     */
    public void serialize(ICStorageElement element) {
        if (superClass != null)
            element.setAttribute(IProjectType.SUPERCLASS, superClass.getId());

        element.setAttribute(IBuildObject.ID, id);

        if (name != null) {
            element.setAttribute(IBuildObject.NAME, name);
        }

        // sourceContentType
        if (sourceContentTypeIds != null) {
            String s = composeString(sourceContentTypeIds, DEFAULT_SEPARATOR);
            element.setAttribute(IInputType.SOURCE_CONTENT_TYPE, s);
        }

        // input file extensions
        if (inputExtensions != null) {
            String inputs = composeString(inputExtensions, DEFAULT_SEPARATOR);
            element.setAttribute(IInputType.SOURCES, inputs);
        }

        // headerContentType
        if (headerContentTypeIds != null) {
            String s = composeString(headerContentTypeIds, DEFAULT_SEPARATOR);
            element.setAttribute(IInputType.HEADER_CONTENT_TYPE, s);
        }

        // input file extensions
        if (headerExtensions != null) {
            String inputs = composeString(headerExtensions, DEFAULT_SEPARATOR);
            element.setAttribute(IInputType.HEADERS, inputs);
        }

        // dependencyContentType
        if (dependencyContentTypeId != null) {
            element.setAttribute(IInputType.DEPENDENCY_CONTENT_TYPE, dependencyContentTypeId);
        }

        // dependency (header file) extensions
        if (getDependencyExtensionsList().size() > 0) {
            String headers = ""; //$NON-NLS-1$
            for (String header : getDependencyExtensionsList()) {
                if (headers.length() > 0)
                    headers += DEFAULT_SEPARATOR;
                headers += header;
            }
            element.setAttribute(IInputType.DEPENDENCY_EXTENSIONS, headers);
        }

        if (optionId != null) {
            element.setAttribute(IInputType.OPTION, optionId);
        }

        if (assignToOptionId != null) {
            element.setAttribute(IInputType.ASSIGN_TO_OPTION, assignToOptionId);
        }

        if (multipleOfType != null) {
            element.setAttribute(IInputType.MULTIPLE_OF_TYPE, multipleOfType.toString());
        }

        if (primaryInput != null) {
            element.setAttribute(IInputType.PRIMARY_INPUT, primaryInput.toString());
        }

        if (buildVariable != null) {
            element.setAttribute(IInputType.BUILD_VARIABLE, buildVariable);
        }

        if (languageId != null)
            element.setAttribute(LANGUAGE_ID, languageId);

        if (languageName != null)
            element.setAttribute(LANGUAGE_NAME, languageName);

        if (buildInfoDicsoveryProfileId != null)
            element.setAttribute(SCANNER_CONFIG_PROFILE_ID, buildInfoDicsoveryProfileId);

        // Note: dependency generator cannot be specified in a project file because
        //       an IConfigurationElement is needed to load it!
        //        if (dependencyGeneratorElement != null) {
        //            //  TODO:  issue warning?
        //        }

        // Serialize my children
        for (InputOrder io : getInputOrderList()) {
            ICStorageElement ioElement = element.createChild(IInputOrder.INPUT_ORDER_ELEMENT_NAME);
            io.serialize(ioElement);
        }
        for (AdditionalInput ai : getAdditionalInputList()) {
            ICStorageElement aiElement = element.createChild(IAdditionalInput.ADDITIONAL_INPUT_ELEMENT_NAME);
            ai.serialize(aiElement);
        }

        // I am clean now
        isDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getParent()
     */
    @Override
    public ITool getParent() {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#createInputOrder()
     */
    @Override
    public IInputOrder createInputOrder(String path) {
        InputOrder inputOrder = new InputOrder(this, false);
        inputOrder.setPath(path);
        getInputOrderList().add(inputOrder);
        setDirty(true);
        return inputOrder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getInputOrders()
     */
    @Override
    public IInputOrder[] getInputOrders() {
        return getInputOrderList().toArray(new IInputOrder[0]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getInputOrder()
     */
    @Override
    public IInputOrder getInputOrder(String path) {
        // TODO Convert both paths to absolute?
        for (InputOrder io : getInputOrderList()) {
            if (path.compareToIgnoreCase(io.getPath()) == 0) {
                return io;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#removeInputOrder()
     */
    @Override
    public void removeInputOrder(String path) {
        IInputOrder order = getInputOrder(path);
        if (order != null)
            removeInputOrder(order);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#removeInputOrder()
     */
    @Override
    public void removeInputOrder(IInputOrder element) {
        getInputOrderList().remove(element);
        setDirty(true);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#createAdditionalInput()
     */
    @Override
    public IAdditionalInput createAdditionalInput(String paths) {
        AdditionalInput addlInput = new AdditionalInput(this, false);
        addlInput.setPaths(paths);
        getAdditionalInputList().add(addlInput);
        setDirty(true);
        return addlInput;
    }

    IAdditionalInput createAdditionalInput(IAdditionalInput base) {
        AdditionalInput newAdditionalInput = new AdditionalInput(this, (AdditionalInput) base);
        getAdditionalInputList().add(newAdditionalInput);
        setDirty(true);
        return newAdditionalInput;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getAdditionalInputs()
     */
    @Override
    public IAdditionalInput[] getAdditionalInputs() {
        return getAdditionalInputList().toArray(new IAdditionalInput[0]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getAdditionalInput()
     */
    @Override
    public IAdditionalInput getAdditionalInput(String paths) {
        // TODO Convert both paths to absolute?
        // Must match all strings
        String[] inputTokens = paths.split(";"); //$NON-NLS-1$
        for (AdditionalInput ai : getAdditionalInputList()) {
            boolean match = false;
            String[] tokens = ai.getPaths();
            if (tokens.length == inputTokens.length) {
                match = true;
                for (int i = 0; i < tokens.length; i++) {
                    if (tokens[i].compareToIgnoreCase(inputTokens[i]) != 0) {
                        match = false;
                        break;
                    }
                }
            }
            if (match)
                return ai;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#removeAdditionalInput()
     */
    @Override
    public void removeAdditionalInput(String path) {
        IAdditionalInput input = getAdditionalInput(path);
        if (input != null)
            removeAdditionalInput(input);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#removeAdditionalInput()
     */
    @Override
    public void removeAdditionalInput(IAdditionalInput element) {
        getAdditionalInputList().remove(element);
        setDirty(true);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getAdditionalDependencies()
     */
    @Override
    public IPath[] getAdditionalDependencies() {
        List<IPath> deps = new ArrayList<>();
        for (AdditionalInput additionalInput : getAdditionalInputList()) {
            int kind = additionalInput.getKind();
            if (kind == IAdditionalInput.KIND_ADDITIONAL_DEPENDENCY
                    || kind == IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY) {
                String[] paths = additionalInput.getPaths();
                if (paths != null) {
                    for (String path : paths) {
                        if (path.length() > 0) {
                            deps.add(Path.fromOSString(path));
                        }
                    }
                }
            }
        }
        return deps.toArray(new IPath[deps.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getAdditionalResources()
     */
    @Override
    public IPath[] getAdditionalResources() {
        List<IPath> ins = new ArrayList<>();
        for (AdditionalInput additionalInput : getAdditionalInputList()) {
            int kind = additionalInput.getKind();
            if (kind == IAdditionalInput.KIND_ADDITIONAL_INPUT
                    || kind == IAdditionalInput.KIND_ADDITIONAL_INPUT_DEPENDENCY) {
                String[] paths = additionalInput.getPaths();
                if (paths != null) {
                    for (String path : paths) {
                        if (path.length() > 0) {
                            ins.add(Path.fromOSString(path));
                        }
                    }
                }
            }
        }
        return ins.toArray(new IPath[ins.size()]);
    }

    /* (non-Javadoc)
     * Memory-safe way to access the list of input orders
     */
    private Vector<InputOrder> getInputOrderList() {
        if (inputOrderList == null) {
            inputOrderList = new Vector<>();
        }
        return inputOrderList;
    }

    /* (non-Javadoc)
     * Memory-safe way to access the list of input orders
     */
    private Vector<AdditionalInput> getAdditionalInputList() {
        if (additionalInputList == null) {
            additionalInputList = new Vector<>();
        }
        return additionalInputList;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputType#getSuperClass()
     */
    @Override
    public IInputType getSuperClass() {
        return superClass;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getName()
     */
    @Override
    public String getName() {
        return (name == null && superClass != null) ? superClass.getName() : name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getBuildVariable()
     */
    @Override
    public String getBuildVariable() {
        if (buildVariable == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return superClass.getBuildVariable();
            } else {
                return EMPTY_STRING;
            }
        }
        return buildVariable;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setBuildVariable()
     */
    @Override
    public void setBuildVariable(String variableName) {
        if (variableName == null && buildVariable == null)
            return;
        if (buildVariable == null || variableName == null || !(variableName.equals(buildVariable))) {
            buildVariable = variableName;
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyContentType()
     */
    @Override
    public IContentType getDependencyContentType() {
        if (dependencyContentType == null) {
            if (superClass != null) {
                return superClass.getDependencyContentType();
            } else {
                return null;
            }
        }
        return dependencyContentType;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setDependencyContentType()
     */
    @Override
    public void setDependencyContentType(IContentType type) {
        if (dependencyContentType != type) {
            dependencyContentType = type;
            if (dependencyContentType != null) {
                dependencyContentTypeId = dependencyContentType.getId();
            } else {
                dependencyContentTypeId = null;
            }
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyExtensionsAttribute()
     */
    @Override
    public String[] getDependencyExtensionsAttribute() {
        if (dependencyExtensions == null || dependencyExtensions.size() == 0) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return superClass.getDependencyExtensionsAttribute();
            } else {
                if (dependencyExtensions == null) {
                    dependencyExtensions = new ArrayList<>();
                }
            }
        }
        return dependencyExtensions.toArray(new String[dependencyExtensions.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setDependencyExtensionsAttribute()
     */
    @Override
    public void setDependencyExtensionsAttribute(String extensions) {
        getDependencyExtensionsList().clear();
        if (extensions != null) {
            StringTokenizer tokenizer = new StringTokenizer(extensions, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                getDependencyExtensionsList().add(tokenizer.nextToken());
            }
        }
        setDirty(true);
        setRebuildState(true);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyExtensions()
     */
    @Override
    public String[] getDependencyExtensions(ITool tool) {
        //  Use content type if specified and registered with Eclipse
        IContentType type = getDependencyContentType();
        if (type != null) {
            String[] exts = ((Tool) tool).getContentTypeFileSpecs(type);
            //  TODO: This is a temporary hack until we decide how to specify the langauge (C vs. C++)
            //  of a .h file.  If the content type is the CDT-defined C/C++ content type, then
            //  add "h" to the list if it is not already there.
            if (type.getId().compareTo("org.eclipse.cdt.core.cxxHeader") == 0) { //$NON-NLS-1$
                boolean h_found = false;
                for (String ext : exts) {
                    if (ext.compareTo("h") == 0) { //$NON-NLS-1$
                        h_found = true;
                        break;
                    }
                }
                if (!h_found) {
                    String[] cppexts = new String[exts.length + 1];
                    int i = 0;
                    for (; i < exts.length; i++) {
                        cppexts[i] = exts[i];
                    }
                    cppexts[i] = "h"; //$NON-NLS-1$
                    return cppexts;
                }
            }
            return exts;
        }
        return getDependencyExtensionsAttribute();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#isDependencyExtension()
     */
    @Override
    public boolean isDependencyExtension(ITool tool, String ext) {
        String[] exts = getDependencyExtensions(tool);
        for (String depExt : exts) {
            if (ext.equals(depExt))
                return true;
        }
        return false;
    }

    private List<String> getDependencyExtensionsList() {
        if (dependencyExtensions == null) {
            dependencyExtensions = new ArrayList<>();
        }
        return dependencyExtensions;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyGenerator()
     */
    //    @Override
    //    public IManagedDependencyGeneratorType getDependencyGenerator() {
    //        if (dependencyGenerator != null) {
    //            return dependencyGenerator;
    //        }
    //        IConfigurationElement element = getDependencyGeneratorElement();
    //        if (element != null) {
    //            try {
    //                if (element.getAttribute(ITool.DEP_CALC_ID) != null) {
    //                    dependencyGenerator = (IManagedDependencyGeneratorType) element
    //                            .createExecutableExtension(ITool.DEP_CALC_ID);
    //                    return dependencyGenerator;
    //                }
    //            } catch (CoreException e) {
    //            }
    //        }
    //        return null;
    //    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyGeneratorElement()
    //     */
    //    public IConfigurationElement getDependencyGeneratorElement() {
    //        if (dependencyGeneratorElement == null) {
    //            if (superClass != null) {
    //                return ((InputType) superClass).getDependencyGeneratorElement();
    //            }
    //        }
    //        return dependencyGeneratorElement;
    //    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.IInputType#setDependencyGeneratorElement()
    //     */
    //    public void setDependencyGeneratorElement(IConfigurationElement element) {
    //        dependencyGeneratorElement = element;
    //        setDirty(true);
    //        setRebuildState(true);
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getMultipleOfType()
     */
    @Override
    public boolean getMultipleOfType() {
        if (multipleOfType == null) {
            if (superClass != null) {
                return superClass.getMultipleOfType();
            } else {
                return false; // default is false
            }
        }
        return multipleOfType.booleanValue();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setMultipleOfType()
     */
    @Override
    public void setMultipleOfType(boolean b) {
        if (multipleOfType == null || !(b == multipleOfType.booleanValue())) {
            multipleOfType = b;
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getPrimaryInput()
     */
    @Override
    public boolean getPrimaryInput() {
        if (primaryInput == null) {
            if (superClass != null) {
                return superClass.getPrimaryInput();
            } else {
                return false; // default is false
            }
        }
        return primaryInput.booleanValue();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setMultipleOfType()
     */
    @Override
    public void setPrimaryInput(boolean b) {
        if (primaryInput == null || !(b == primaryInput.booleanValue())) {
            primaryInput = b;
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getOptionId()
     */
    @Override
    public String getOptionId() {
        if (optionId == null) {
            if (superClass != null) {
                return superClass.getOptionId();
            } else {
                return null;
            }
        }
        return optionId;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setOptionId()
     */
    @Override
    public void setOptionId(String id) {
        if (id == null && optionId == null)
            return;
        if (id == null || optionId == null || !(optionId.equals(id))) {
            optionId = id;
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getAssignToOptionId()
     */
    @Override
    public String getAssignToOptionId() {
        if (assignToOptionId == null) {
            if (superClass != null) {
                return superClass.getAssignToOptionId();
            } else {
                return null;
            }
        }
        return assignToOptionId;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setAssignToOptionId()
     */
    @Override
    public void setAssignToOptionId(String id) {
        if (id == null && assignToOptionId == null)
            return;
        if (id == null || assignToOptionId == null || !(assignToOptionId.equals(id))) {
            assignToOptionId = id;
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getSourceContentType()
     */
    @Override
    public IContentType getSourceContentType() {
        IContentType[] types = getSourceContentTypes();

        if (types != null && types.length != 0) {
            return types[0];
        }
        return null;
    }

    @Override
    public IContentType[] getSourceContentTypes() {
        if (sourceContentTypes == null) {
            if (superClass != null) {
                return superClass.getSourceContentTypes();
            }

            return new IContentType[0];
        }
        return sourceContentTypes.clone();
    }

    @Override
    public IContentType[] getHeaderContentTypes() {
        if (headerContentTypes == null) {
            if (superClass != null) {
                return superClass.getHeaderContentTypes();
            }

            return new IContentType[0];
        }
        return headerContentTypes.clone();
    }

    @Override
    public String[] getHeaderExtensionsAttribute() {
        if (headerExtensions == null) {
            if (superClass != null) {
                return superClass.getHeaderExtensionsAttribute();
            }

            return new String[0];
        }
        return headerExtensions.clone();
    }

    @Override
    public String[] getHeaderContentTypeIds() {
        if (headerContentTypeIds == null) {
            if (superClass != null) {
                return superClass.getHeaderContentTypeIds();
            }

            return new String[0];
        }
        return headerContentTypeIds.clone();
    }

    @Override
    public String[] getSourceContentTypeIds() {
        if (sourceContentTypeIds == null) {
            if (superClass != null) {
                return superClass.getSourceContentTypeIds();
            }

            return new String[0];
        }
        return sourceContentTypeIds.clone();
    }

    @Override
    public void setHeaderContentTypeIds(String[] ids) {
        if (!Arrays.equals(headerContentTypeIds, ids)) {
            headerContentTypeIds = ids != null ? (String[]) ids.clone() : null;

            setDirty(true);
            setRebuildState(true);
        }
    }

    @Override
    public void setHeaderExtensionsAttribute(String[] extensions) {
        if (!Arrays.equals(headerExtensions, extensions)) {
            headerExtensions = extensions != null ? (String[]) extensions.clone() : null;

            setDirty(true);
            setRebuildState(true);
        }
    }

    @Override
    public void setSourceContentTypeIds(String[] ids) {
        if (!Arrays.equals(sourceContentTypeIds, ids)) {
            sourceContentTypeIds = ids != null ? (String[]) ids.clone() : null;

            setDirty(true);
            setRebuildState(true);
        }
    }

    @Override
    public void setSourceExtensionsAttribute(String[] extensions) {
        if (!Arrays.equals(inputExtensions, extensions)) {
            inputExtensions = extensions != null ? (String[]) extensions.clone() : null;

            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setSourceContentType()
     */
    @Override
    public void setSourceContentType(IContentType type) {
        if (type == null) {
            if (sourceContentTypes != null) {
                sourceContentTypes = null;
                sourceContentTypeIds = null;
                setDirty(true);
                setRebuildState(true);
            }
        } else {
            if (sourceContentTypes == null || sourceContentTypes.length != 1 || sourceContentTypes[0] != type) {
                sourceContentTypes = new IContentType[1];
                sourceContentTypes[0] = type;
                sourceContentTypeIds = new String[1];
                sourceContentTypeIds[0] = type.getId();
                setDirty(true);
                setRebuildState(true);
            }
            return;
        }
    }

    public void setSourceContentTypes(IContentType types[]) {
        if (types == null) {
            if (sourceContentTypes != null) {
                sourceContentTypes = null;
                sourceContentTypeIds = null;
                setDirty(true);
                setRebuildState(true);
            }
        } else {
            sourceContentTypes = types.clone();
            sourceContentTypeIds = new String[types.length];
            for (int i = 0; i < types.length; i++) {
                sourceContentTypeIds[i] = types[i].getId();
            }
            setDirty(true);
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getSourceExtensionsAttribute()
     */
    @Override
    public String[] getSourceExtensionsAttribute() {
        if (inputExtensions == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return superClass.getSourceExtensionsAttribute();
            }

            return new String[0];
        }
        return inputExtensions.clone();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#setSourceExtensionsAttribute()
     */
    @Override
    public void setSourceExtensionsAttribute(String extensions) {
        if (extensions == null) {
            if (inputExtensions != null) {
                inputExtensions = null;
                setDirty(true);
                setRebuildState(true);
            }
        } else {
            List<String> list = new ArrayList<>();
            StringTokenizer tokenizer = new StringTokenizer(extensions, DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                list.add(tokenizer.nextToken());
            }

            String[] newExts = list.toArray(new String[list.size()]);
            if (!Arrays.equals(newExts, inputExtensions)) {
                inputExtensions = newExts;
                setDirty(true);
                setRebuildState(true);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#getSourceExtensions()
     */
    @Override
    public String[] getSourceExtensions(ITool tool) {
        return getSourceExtensions(tool, ((Tool) tool).getProject());
        //		//  Use content type if specified and registered with Eclipse
        //		IContentType types[] = getSourceContentTypes();
        //		if (types.length != 0) {
        //			IContentType type;
        //			List list = new ArrayList();
        //			for(int i = 0; i < types.length; i++){
        //				type = types[i];
        //				list.addAll(Arrays.asList(((Tool)tool).getContentTypeFileSpecs(type)));
        //			}
        //			return (String[])list.toArray(new String[list.size()]);
        //		}
        //		return getSourceExtensionsAttribute();
    }

    public String[] getSourceExtensions(ITool tool, IProject project) {
        //  Use content type if specified and registered with Eclipse
        IContentType types[] = getSourceContentTypes();
        if (types.length != 0) {
            List<String> list = new ArrayList<>();
            for (IContentType type : types) {
                list.addAll(Arrays.asList(((Tool) tool).getContentTypeFileSpecs(type, project)));
            }
            return list.toArray(new String[list.size()]);
        }
        return getSourceExtensionsAttribute();
    }

    @Override
    public String[] getHeaderExtensions(ITool tool) {
        IContentType types[] = getHeaderContentTypes();
        if (types.length != 0) {
            List<String> list = new ArrayList<>();
            for (IContentType type : types) {
                list.addAll(Arrays.asList(((Tool) tool).getContentTypeFileSpecs(type)));
            }
            return list.toArray(new String[list.size()]);
        }
        return getHeaderExtensionsAttribute();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputType#isSourceExtension()
     */
    @Override
    public boolean isSourceExtension(ITool tool, String ext) {
        return isSourceExtension(tool, ext, ((Tool) tool).getProject());
    }

    public boolean isSourceExtension(ITool tool, String ext, IProject project) {
        String[] exts = getSourceExtensions(tool, project);
        for (String srcExt : exts) {
            if (ext.equals(srcExt))
                return true;
        }
        return false;
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputType#isExtensionElement()
     */
    @Override
    public boolean isExtensionElement() {
        return isExtensionInputType;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputType#isDirty()
     */
    @Override
    public boolean isDirty() {
        // This shouldn't be called for an extension InputType
        if (isExtensionInputType)
            return false;

        // Check my children
        for (InputOrder inputOrder : getInputOrderList()) {
            if (inputOrder.isDirty())
                return true;
        }
        for (AdditionalInput additionalInput : getAdditionalInputList()) {
            if (additionalInput.isDirty())
                return true;
        }

        return isDirty;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputType#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
        // Propagate "false" to the children
        if (!isDirty) {
            for (InputOrder inputOrder : getInputOrderList()) {
                inputOrder.setDirty(false);
            }
            for (AdditionalInput additionalInput : getAdditionalInputList()) {
                additionalInput.setDirty(false);
            }
        }
    }

    /* (non-Javadoc)
     *  Resolve the element IDs to interface references
     */
    public void resolveReferences() {
        if (!resolved) {
            resolved = true;
            // Resolve superClass
            if (superClassId != null && superClassId.length() > 0) {
                superClass = ManagedBuildManager.getExtensionInputType(superClassId);
                if (superClass == null) {
                    // Report error
                    ManagedBuildManager.outputResolveError("superClass", //$NON-NLS-1$
                            superClassId, "inputType", //$NON-NLS-1$
                            getId());
                }
            }

            // Resolve content types
            IContentTypeManager manager = Platform.getContentTypeManager();
            List<IContentType> list = new ArrayList<>();
            if (sourceContentTypeIds != null) {
                for (String sourceContentTypeId : sourceContentTypeIds) {
                    IContentType type = manager.getContentType(sourceContentTypeId);
                    if (type != null)
                        list.add(type);
                }
                if (list.size() != 0) {
                    sourceContentTypes = list.toArray(new IContentType[list.size()]);
                    list.clear();
                } else {
                    sourceContentTypes = new IContentType[0];
                }
            }

            if (headerContentTypeIds != null) {
                for (String headerContentTypeId : headerContentTypeIds) {
                    IContentType type = manager.getContentType(headerContentTypeId);
                    if (type != null)
                        list.add(type);
                }
                if (list.size() != 0) {
                    headerContentTypes = list.toArray(new IContentType[list.size()]);
                    list.clear();
                } else {
                    headerContentTypes = new IContentType[0];
                }
            }

            if (dependencyContentTypeId != null && dependencyContentTypeId.length() > 0) {
                dependencyContentType = manager.getContentType(dependencyContentTypeId);
            }

            //  Call resolveReferences on our children
            for (InputOrder inputOrder : getInputOrderList()) {
                inputOrder.resolveReferences();
            }
            for (AdditionalInput additionalInput : getAdditionalInputList()) {
                additionalInput.resolveReferences();
            }
        }
    }

    /**
     * @return Returns the managedBuildRevision.
     */
    @Override
    public String getManagedBuildRevision() {
        if (managedBuildRevision == null) {
            if (getParent() != null) {
                return getParent().getManagedBuildRevision();
            }
        }
        return managedBuildRevision;
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        if (version == null) {
            if (getParent() != null) {
                return getParent().getVersion();
            }
        }
        return version;
    }

    @Override
    public void setVersion(Version version) {
        // Do nothing
    }

    public boolean needsRebuild() {
        if (rebuildState)
            return true;

        for (InputOrder inputOrder : getInputOrderList()) {
            if (inputOrder.needsRebuild())
                return true;
        }
        for (AdditionalInput additionalInput : getAdditionalInputList()) {
            if (additionalInput.needsRebuild())
                return true;
        }

        return rebuildState;
    }

    public void setRebuildState(boolean rebuild) {
        if (isExtensionElement() && rebuild)
            return;

        rebuildState = rebuild;

        // Propagate "false" to the children
        if (!rebuild) {
            for (InputOrder inputOrder : getInputOrderList()) {
                inputOrder.setRebuildState(false);
            }
            for (AdditionalInput additionalInput : getAdditionalInputList()) {
                additionalInput.setRebuildState(false);
            }
        }
    }

    public IResourceInfo getRcInfo(ITool tool) {
        IBuildObject parent = tool.getParent();
        if (parent instanceof IFileInfo)
            return (IFileInfo) parent;
        else if (parent instanceof IToolChain)
            return ((IToolChain) parent).getParentFolderInfo();
        return null;
    }

    private ILanguageInfoCalculator getLanguageInfoCalculator() {
        if (languageInfoCalculator == null) {
            if (languageInfoCalculatorElement != null) {
                try {
                    Object ex = languageInfoCalculatorElement.createExecutableExtension(LANGUAGE_INFO_CALCULATOR);
                    if (ex instanceof ILanguageInfoCalculator)
                        languageInfoCalculator = (ILanguageInfoCalculator) ex;
                } catch (CoreException e) {
                }
            }

            //			if(languageInfoCalculator == null)
            //				languageInfoCalculator = new DefaultLanguageInfoCalculator();
        }
        return languageInfoCalculator;
    }

    public String getLanguageIdAttribute() {
        if (languageId == null) {
            if (superClass != null) {
                return ((InputType) superClass).getLanguageIdAttribute();
            }
            return null;
        }
        return languageId;
    }

    public String getLanguageNameAttribute() {
        if (languageName == null) {
            if (superClass != null) {
                return ((InputType) superClass).getLanguageNameAttribute();
            }
            return null;
        }
        return languageName;
    }

    @Override
    public String getLanguageId(ITool tool) {
        IResourceInfo rcInfo = getRcInfo(tool);
        String langId = this.languageId;
        if (langId == null || isExtensionInputType) {
            ILanguageInfoCalculator calc = getLanguageInfoCalculator();
            if (calc != null)
                langId = calc.getLanguageId(rcInfo, tool, this);
        }

        if (langId == null) {
            langId = getLanguageIdAttribute();
        }

        if (langId == null) {
            IContentType contentType = getSourceContentType();
            if (contentType != null) {
                ILanguage language = LanguageManager.getInstance().getLanguage(contentType);
                if (language != null)
                    langId = language.getId();
            }
        }

        return langId;
    }

    @Override
    public String getLanguageName(ITool tool) {
        IResourceInfo rcInfo = getRcInfo(tool);
        String langName = this.languageName;
        if (langName == null || isExtensionInputType) {
            ILanguageInfoCalculator calc = getLanguageInfoCalculator();
            if (calc != null)
                langName = calc.getLanguageName(rcInfo, tool, this);
        }

        if (langName == null) {
            langName = getLanguageNameAttribute();
            if (langName == null) {
                IContentType types[] = getSourceContentTypes();
                for (IContentType type : types) {
                    String name = type.getName();
                    if (name != null && name.length() != 0) {
                        langName = name;
                        break;
                    }
                }

                if (langName == null) {
                    types = getHeaderContentTypes();
                    for (IContentType type : types) {
                        String name = type.getName();
                        if (name != null && name.length() != 0) {
                            langName = name;
                            break;
                        }
                    }
                }
            }
        }

        if (langName == null) {
            String[] exts = getSourceExtensions(tool);
            if (exts.length != 0) {
                langName = CDataUtil.arrayToString(exts, ","); //$NON-NLS-1$
            } else {
                exts = getHeaderExtensions(tool);
                if (exts.length != 0)
                    langName = CDataUtil.arrayToString(exts, ","); //$NON-NLS-1$
            }
        }

        if (langName == null) {
            langName = getName();
            if (langName == null) {
                langName = tool.getName();
                if (langName == null) {
                    langName = getId();
                }
            }
        }

        return langName;
    }

    @Override
    public String getDiscoveryProfileId(ITool tool) {
        String id = getDiscoveryProfileIdAttribute();
        if (id == null) {
            id = ((Tool) tool).getDiscoveryProfileId();
        }
        // if there is more than one ('|'-separated), return the first one
        // TODO: expand interface with String[] getDiscoveryProfileIds(ITool tool)
        if (null != id) {
            int nPos = id.indexOf('|');
            if (nPos > 0)
                id = id.substring(0, nPos);
        }
        return id;
    }

    /**
     * Check if legacy scanner discovery profiles should be used.
     */
    private boolean useLegacyScannerDiscoveryProfiles() {
        boolean useLegacy = true;
        ITool tool = getParent();
        if (tool != null) {
            IBuildObject toolchain = tool.getParent();
            if (toolchain instanceof IToolChain
                    && ((IToolChain) toolchain).getDefaultLanguageSettingsProviderIds() != null) {
                IConfiguration cfg = ((IToolChain) toolchain).getParent();
                if (cfg != null && cfg.getDefaultLanguageSettingsProviderIds() != null) {
                    IResource rc = cfg.getOwner();
                    if (rc != null) {
                        IProject project = rc.getProject();
                        useLegacy = !ScannerDiscoveryLegacySupport
                                .isLanguageSettingsProvidersFunctionalityEnabled(project);
                    }
                }
            }
        }
        return useLegacy;
    }

    /**
     * Temporary method to support compatibility during SD transition.
     * 
     * @noreference This method is not intended to be referenced by clients.
     */
    public String getLegacyDiscoveryProfileIdAttribute() {
        String profileId = buildInfoDicsoveryProfileId;
        if (profileId == null) {
            profileId = ScannerDiscoveryLegacySupport.getDeprecatedLegacyProfiles(id);
            if (profileId == null && superClass instanceof InputType) {
                profileId = ((InputType) superClass).getLegacyDiscoveryProfileIdAttribute();
            }
        }
        return profileId;
    }

    public String getDiscoveryProfileIdAttribute() {
        String discoveryProfileAttribute = getDiscoveryProfileIdAttributeInternal();
        if (discoveryProfileAttribute == null && useLegacyScannerDiscoveryProfiles()) {
            discoveryProfileAttribute = getLegacyDiscoveryProfileIdAttribute();
        }

        return discoveryProfileAttribute;
    }

    /**
     * Do not inline! This method needs to call itself recursively.
     */
    private String getDiscoveryProfileIdAttributeInternal() {
        if (buildInfoDicsoveryProfileId == null && superClass instanceof InputType) {
            return ((InputType) superClass).getDiscoveryProfileIdAttributeInternal();
        }
        return buildInfoDicsoveryProfileId;
    }

    @Override
    public void setLanguageIdAttribute(String id) {
        languageId = id;
    }

    @Override
    public void setLanguageNameAttribute(String name) {
        languageName = name;
    }

    public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
        if (booleanExpressionCalculator == null) {
            if (superClass != null) {
                return ((InputType) superClass).getBooleanExpressionCalculator();
            }
        }
        return booleanExpressionCalculator;
    }

    public boolean isEnabled(ITool tool) {
        if (tool.isExtensionElement())
            return true;

        BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
        if (calc == null)
            return true;

        return calc.isInputTypeEnabled(tool, this);
    }

    public boolean hasScannerConfigSettings() {

        if (getDiscoveryProfileIdAttribute() != null)
            return true;

        if (superClass != null && superClass instanceof InputType)
            return ((InputType) superClass).hasScannerConfigSettings();

        return false;
    }

    public boolean hasCustomSettings() {
        //TODO:
        return false;
    }


    @Override
    public String toString() {
        return getId();
    }
}
