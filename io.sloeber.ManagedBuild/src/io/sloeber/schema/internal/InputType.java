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
package io.sloeber.schema.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

import io.sloeber.autoBuild.Internal.BooleanExpressionApplicabilityCalculator;
import io.sloeber.autoBuild.Internal.OptionEnablementExpression;
import io.sloeber.autoBuild.extensionPoint.ILanguageInfoCalculator;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;

public class InputType extends BuildObject implements IInputType {

    private static final String DEFAULT_SEPARATOR = ","; //$NON-NLS-1$
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    // Parent and children
    private ITool parent;

    // Superclass
    private IInputType superClass;

    // Managed Build model attributes
    private List<IContentType> mySourceContentTypes = new ArrayList<>();

    private List<IContentType> headerContentTypes = new ArrayList<>();
    private List<String> inputExtensions = new ArrayList<>();
    private IContentType dependencyContentType;
    private List<String> dependencyExtensions = new ArrayList<>();
    private ILanguageInfoCalculator languageInfoCalculator;
    private IConfigurationElement languageInfoCalculatorElement;

    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    // Miscellaneous
    private boolean isExtensionInputType = false;

    // read from model
    private String[] modelSourceContentType;
    private String[] modelExtensions ;
    private String[] modelOutputTypeID ;
    private String[] modelOption ;
    private String[] modelAssignToOption;
    private String[] modelDependencyContentType ;
    private String[] modelDependencyExtensions ;
    private String[] modelScannerConfigDiscoveryProfileID;
    private String[] modelLanguageID ;
    private String[] modelLanguageInfoCalculator ;
    private List<OptionEnablementExpression> myOptionEnablementExpression = new ArrayList<>();;

    /*
     * C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create an InputType defined by an extension
     * point in a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The ITool parent of this InputType
     * @param element
     *            The InputType definition from the manifest file or a dynamic
     *            element provider
     */
    public InputType(ITool parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;
        isExtensionInputType = true;

        loadNameAndID(root, element);
        modelSourceContentType = getAttributes(SOURCE_CONTENT_TYPE);
        modelExtensions = getAttributes(EXTENSIONS);
        modelOutputTypeID = getAttributes(OUTPUT_TYPE_ID);
        modelOption = getAttributes(OPTION);
        modelAssignToOption = getAttributes(ASSIGN_TO_OPTION);
        modelDependencyContentType = getAttributes(DEPENDENCY_CONTENT_TYPE);
        modelDependencyExtensions = getAttributes(DEPENDENCY_EXTENSIONS);
        modelScannerConfigDiscoveryProfileID = getAttributes(SCANNER_CONFIG_PROFILE_ID);
        modelLanguageID = getAttributes(LANGUAGE_ID);
        modelLanguageInfoCalculator = getAttributes(LANGUAGE_INFO_CALCULATOR);

        myOptionEnablementExpression.clear();
        IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        for (IConfigurationElement curEnablement : enablements) {
            myOptionEnablementExpression.add(new OptionEnablementExpression(curEnablement));
        }

        try {
            resolveFields();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // Hook me up to the Managed Build Manager
        //       ManagedBuildManager.addExtensionInputType(this);

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
        //        this.parent = parent;
        //        isExtensionInputType = false;
        //
        //        loadNameAndID( element);
        //        modelSourceContentType = getAttributes(SOURCE_CONTENT_TYPE);
        //        modelExtensions = getAttributes(EXTENSIONS);
        //        modelOutputTypeID = getAttributes(OUTPUT_TYPE_ID);
        //        modelOption = getAttributes(OPTION);
        //        modelAssignToOption = getAttributes(ASSIGN_TO_OPTION);
        //        modelDependencyContentType = getAttributes(DEPENDENCY_CONTENT_TYPE);
        //        modelDependencyExtensions = getAttributes(DEPENDENCY_EXTENSIONS);
        //        modelScannerConfigDiscoveryProfileID = getAttributes(SCANNER_CONFIG_PROFILE_ID);
        //        modelLanguageID = getAttributes(LANGUAGE_ID);
        //        modelLanguageInfoCalculator = getAttributes(LANGUAGE_INFO_CALCULATOR);
        //
        //        // sourceContentType
        //        IContentTypeManager manager = Platform.getContentTypeManager();
        //        if (element.getAttribute(IInputType.SOURCE_CONTENT_TYPE) != null) {
        //            String ids = element.getAttribute(IInputType.SOURCE_CONTENT_TYPE);
        //            if (ids != null) {
        //                StringTokenizer tokenizer = new StringTokenizer(ids, DEFAULT_SEPARATOR);
        //                while (tokenizer.hasMoreElements()) {
        //                    sourceContentTypeIds.add(tokenizer.nextToken());
        //                }
        //
        //                for (String sourceContentTypeId : sourceContentTypeIds) {
        //                    IContentType type = manager.getContentType(sourceContentTypeId);
        //                    if (type != null)
        //                        mySourceContentTypes.add(type);
        //                }
        //
        //            }
        //        }
        //
        //        // sources
        //        if (element.getAttribute(IInputType.SOURCES) != null) {
        //            String inputs = element.getAttribute(ITool.SOURCES);
        //            if (inputs != null) {
        //                StringTokenizer tokenizer = new StringTokenizer(inputs, DEFAULT_SEPARATOR);
        //                while (tokenizer.hasMoreElements()) {
        //                    inputExtensions.add(tokenizer.nextToken());
        //                }
        //
        //            }
        //        }
        //
        //        // dependencyContentType
        //        if (element.getAttribute(IInputType.DEPENDENCY_CONTENT_TYPE) != null) {
        //            dependencyContentTypeId = element.getAttribute(IInputType.DEPENDENCY_CONTENT_TYPE);
        //            if (dependencyContentTypeId != null && dependencyContentTypeId.length() > 0) {
        //                dependencyContentType = manager.getContentType(dependencyContentTypeId);
        //            }
        //        }
        //
        //        // dependencyExtensions
        //        // Get the dependency (header file) extensions
        //        if (element.getAttribute(IInputType.DEPENDENCY_EXTENSIONS) != null) {
        //            String headers = element.getAttribute(IInputType.DEPENDENCY_EXTENSIONS);
        //            if (headers != null) {
        //                StringTokenizer tokenizer = new StringTokenizer(headers, DEFAULT_SEPARATOR);
        //                while (tokenizer.hasMoreElements()) {
        //                    getDependencyExtensionsList().add(tokenizer.nextToken());
        //                }
        //            }
        //        }
        //
        //        // option
        //        if (element.getAttribute(IInputType.OPTION) != null) {
        //            optionId = element.getAttribute(IInputType.OPTION);
        //        }
        //
        //        // assignToOption
        //        if (element.getAttribute(IInputType.ASSIGN_TO_OPTION) != null) {
        //            assignToOptionId = element.getAttribute(IInputType.ASSIGN_TO_OPTION);
        //        }
        //
        //        // buildVariable
        //        if (element.getAttribute(IInputType.BUILD_VARIABLE) != null) {
        //            buildVariable = element.getAttribute(IInputType.BUILD_VARIABLE);
        //        }
        //
        //        languageId = element.getAttribute(LANGUAGE_ID);
        //
        //        // Note: dependency generator cannot be specified in a project file because
        //        // an IConfigurationElement is needed to load it!
        //        if (element.getAttribute(ITool.DEP_CALC_ID) != null) {
        //            // TODO: Issue warning?
        //        }

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
        //JABA I'm n,ot supporting modifications on the fly
        //        this.parent = parent;
        //        superClass = inputType.superClass;
        //        if (superClass != null && inputType.superClassId != null) {
        //            superClassId = inputType.superClassId;
        //        }
        //        setId(Id);
        //        setName(name);
        //
        //        isExtensionInputType = false;
        //        boolean copyIds = Id.equals(inputType.id);
        //
        //        mySourceContentTypes.clear();
        //        mySourceContentTypes.addAll(inputType.mySourceContentTypes);
        //
        //        inputExtensions.clear();
        //        inputExtensions.addAll(inputType.inputExtensions);
        //
        //        headerContentTypes.clear();
        //        headerContentTypes.addAll(inputType.headerContentTypes);
        //
        //        if (inputType.dependencyContentTypeId != null) {
        //            dependencyContentTypeId = inputType.dependencyContentTypeId;
        //        }
        //        dependencyContentType = inputType.dependencyContentType;
        //        if (inputType.dependencyExtensions != null) {
        //            dependencyExtensions = new ArrayList<>(inputType.dependencyExtensions);
        //        }
        //        if (inputType.optionId != null) {
        //            optionId = inputType.optionId;
        //        }
        //        if (inputType.assignToOptionId != null) {
        //            assignToOptionId = inputType.assignToOptionId;
        //        }
        //        if (inputType.buildVariable != null) {
        //            buildVariable = inputType.buildVariable;
        //        }
        //
        //        languageId = inputType.languageId;
        //        languageInfoCalculator = inputType.languageInfoCalculator;
        //        languageInfoCalculatorElement = inputType.languageInfoCalculatorElement;
        //        modelScannerConfigDiscoveryProfileID[SUPER] = inputType.modelScannerConfigDiscoveryProfileID[SUPER];
        //
        //        if (copyIds) {
        //            isDirty = inputType.isDirty;
        //            rebuildState = inputType.rebuildState;
        //        } else {
        //            setRebuildState(true);
        //        }
    }



    /*
     * P A R E N T A N D C H I L D H A N D L I N G
     */

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getParent()
     */
    @Override
    public ITool getParent() {
        return parent;
    }

    /*
     * M O D E L A T T R I B U T E A C C E S S O R S
     */

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.core.IInputType#getSuperClass()
     */
    @Override
    public IInputType getSuperClass() {
        return superClass;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getName()
     */
    @Override
    public String getName() {
        return (name == null && superClass != null) ? superClass.getName() : name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getBuildVariable()
     */
    @Override
    public String getBuildVariable() {
        return EMPTY_STRING;
    }

    /*
     * (non-Javadoc)
     * 
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#
     * getDependencyExtensionsAttribute()
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyExtensions()
     */
    @Override
    public String[] getDependencyExtensions(ITool tool) {
        // Use content type if specified and registered with Eclipse
        IContentType type = getDependencyContentType();
        if (type != null) {
            String[] exts = ((Tool) tool).getContentTypeFileSpecs(type);
            // TODO: This is a temporary hack until we decide how to specify the langauge (C
            // vs. C++)
            // of a .h file. If the content type is the CDT-defined C/C++ content type, then
            // add "h" to the list if it is not already there.
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

    /*
     * (non-Javadoc)
     * 
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyGenerator()
     */
    // @Override
    // public IManagedDependencyGeneratorType getDependencyGenerator() {
    // if (dependencyGenerator != null) {
    // return dependencyGenerator;
    // }
    // IConfigurationElement element = getDependencyGeneratorElement();
    // if (element != null) {
    // try {
    // if (element.getAttribute(ITool.DEP_CALC_ID) != null) {
    // dependencyGenerator = (IManagedDependencyGeneratorType) element
    // .createExecutableExtension(ITool.DEP_CALC_ID);
    // return dependencyGenerator;
    // }
    // } catch (CoreException e) {
    // }
    // }
    // return null;
    // }

    // /* (non-Javadoc)
    // * @see
    // org.eclipse.cdt.core.build.managed.IInputType#getDependencyGeneratorElement()
    // */
    // public IConfigurationElement getDependencyGeneratorElement() {
    // if (dependencyGeneratorElement == null) {
    // if (superClass != null) {
    // return ((InputType) superClass).getDependencyGeneratorElement();
    // }
    // }
    // return dependencyGeneratorElement;
    // }

    // /* (non-Javadoc)
    // * @see
    // org.eclipse.cdt.core.build.managed.IInputType#setDependencyGeneratorElement()
    // */
    // public void setDependencyGeneratorElement(IConfigurationElement element) {
    // dependencyGeneratorElement = element;
    // setDirty(true);
    // setRebuildState(true);
    // }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getAssignToOptionId()
     */
    @Override
    public String getAssignToOptionId() {
        return modelAssignToOption[SUPER];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getSourceContentType()
     */
    @Override
    public IContentType getSourceContentType() {
        List<IContentType> types = getSourceContentTypes();
        if (types.isEmpty()) {
            return null;
        }
        return types.get(0);
    }

    @Override
    public List<IContentType> getSourceContentTypes() {
        if (mySourceContentTypes == null) {
            if (superClass != null) {
                return superClass.getSourceContentTypes();
            }

            return new ArrayList<IContentType>();
        }
        return mySourceContentTypes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.cdt.core.build.managed.IInputType#getSourceExtensionsAttribute()
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
        return inputExtensions.toArray(new String[inputExtensions.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getSourceExtensions()
     */
    @Override
    public String[] getSourceExtensions(ITool tool) {
        return getSourceExtensions(tool, ((Tool) tool).getProject());
    }

    public String[] getSourceExtensions(ITool tool, IProject project) {
        // Use content type if specified and registered with Eclipse
        List<IContentType> types = getSourceContentTypes();
        if (!types.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (IContentType type : types) {
                list.addAll(Arrays.asList(((Tool) tool).getContentTypeFileSpecs(type, project)));
            }
            return list.toArray(new String[list.size()]);
        }
        return getSourceExtensionsAttribute();
    }

    /*
     * O B J E C T S T A T E M A I N T E N A N C E
     */

    //    /*
    //     * (non-Javadoc) Resolve the element IDs to interface references
    //     */
    //    public void resolveReferencesweg() {
    //        if (!resolved) {
    //            resolved = true;
    //            // Resolve superClass
    //            if (superClassId != null && superClassId.length() > 0) {
    //                superClass = ManagedBuildManager.getExtensionInputType(superClassId);
    //                if (superClass == null) {
    //                    // Report error
    //                    ManagedBuildManager.outputResolveError("superClass", //$NON-NLS-1$
    //                            superClassId, "inputType", //$NON-NLS-1$
    //                            getId());
    //                }
    //            }
    //
    //            // Resolve content types
    //            IContentTypeManager manager = Platform.getContentTypeManager();
    //            List<IContentType> list = new ArrayList<>();
    //            if (sourceContentTypeIds != null) {
    //                for (String sourceContentTypeId : sourceContentTypeIds) {
    //                    IContentType type = manager.getContentType(sourceContentTypeId);
    //                    if (type != null)
    //                        mySourceContentTypes.add(type);
    //                }
    //            }
    //
    //            if (dependencyContentTypeId != null && dependencyContentTypeId.length() > 0) {
    //                dependencyContentType = manager.getContentType(dependencyContentTypeId);
    //            }
    //
    //        }
    //    }

  
    @Override
    public String getLanguageName(ITool tool) {
//        IResourceInfo rcInfo = getRcInfo(tool);
        String langName = null;
//        if (langName == null || isExtensionInputType) {
//            ILanguageInfoCalculator calc = getLanguageInfoCalculator();
//            if (calc != null)
//                langName = calc.getLanguageName(rcInfo, tool, this);
//        }

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
        String profileId = modelScannerConfigDiscoveryProfileID[SUPER];
        if (profileId == null) {
            profileId = ScannerDiscoveryLegacySupport.getDeprecatedLegacyProfiles(id);
            if (profileId == null && superClass instanceof InputType) {
                profileId = ((InputType) superClass).getLegacyDiscoveryProfileIdAttribute();
            }
        }
        return profileId;
    }

    public String getDiscoveryProfileIdAttribute() {
        String discoveryProfileAttribute = modelScannerConfigDiscoveryProfileID[SUPER];
        if (discoveryProfileAttribute == null && useLegacyScannerDiscoveryProfiles()) {
            discoveryProfileAttribute = getLegacyDiscoveryProfileIdAttribute();
        }

        return discoveryProfileAttribute;
    }

    public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
        if (booleanExpressionCalculator == null) {
            if (superClass != null) {
                return ((InputType) superClass).getBooleanExpressionCalculator();
            }
        }
        return booleanExpressionCalculator;
    }

    public boolean hasScannerConfigSettings() {

        if (getDiscoveryProfileIdAttribute() != null)
            return true;

        if (superClass != null && superClass instanceof InputType)
            return ((InputType) superClass).hasScannerConfigSettings();

        return false;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public boolean isAssociatedWith(IFile file) {
        if (inputExtensions.contains(file.getFileExtension())) {
            return true;
        }
        for (IContentType curContentType : mySourceContentTypes) {
            if (curContentType.isAssociatedWith(file.getName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSourceContentTypeIds() {

        return null;
    }

    public void resolveFields() throws Exception {
        // sourceContentType
        IContentTypeManager manager = Platform.getContentTypeManager();
        if (modelSourceContentType[SUPER] != null) {
            StringTokenizer tokenizer = new StringTokenizer(modelSourceContentType[SUPER], DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                IContentType type = manager.getContentType(tokenizer.nextToken());
                if (type != null)
                    mySourceContentTypes.add(type);
            }
        }

        // Get the supported input file extensions
        if (modelExtensions[SUPER] != null) {
            StringTokenizer tokenizer = new StringTokenizer(modelExtensions[SUPER], DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                inputExtensions.add(tokenizer.nextToken());
            }
        }
        // Get the dependency (header file) extensions
        if (modelDependencyExtensions[SUPER] != null) {
            StringTokenizer tokenizer = new StringTokenizer(modelDependencyExtensions[SUPER], DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                dependencyExtensions.add(tokenizer.nextToken());
            }
        }

        booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(myOptionEnablementExpression);

    }

}
