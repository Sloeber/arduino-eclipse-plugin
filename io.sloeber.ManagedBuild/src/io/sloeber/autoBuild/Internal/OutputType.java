/*******************************************************************************
 * Copyright (c) 2005, 2016 Intel Corporation and others.
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
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.core.resources.IFile;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.IProjectType;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.internal.enablement.OptionEnablementExpression;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;

public class OutputType extends BuildObject implements IOutputType {

    private static final String DEFAULT_SEPARATOR = ","; //$NON-NLS-1$
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    //  Superclass
    private IOutputType superClass;
    private String superClassId;
    //  Parent and children
    private ITool parent;
    //  Managed Build model attributes
    private String optionId;
    private String outputPrefix;
    private String outputExtension;
    private String outputName;
    private String namePattern;
    private IConfigurationElement nameProviderElement = null;
    private IOutputNameProvider nameProvider = null;
    private String buildVariable;

    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    //  Miscellaneous
    private boolean isExtensionOutputType = false;
    private boolean isDirty = false;
    private boolean resolved = true;
    private boolean rebuildState;

    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create an OutputType defined by an extension
     * point in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The ITool parent of this OutputType
     * @param element
     *            The OutputType definition from the manifest file or a dynamic
     *            element
     *            provider
     */
    public OutputType(ITool parent, IManagedConfigElement element) {
        this.parent = parent;
        isExtensionOutputType = true;

        // setup for resolving
        resolved = false;

        loadFromManifest(element);

        IManagedConfigElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        if (enablements.length > 0)
            booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(enablements);

        // Hook me up to the Managed Build Manager
        ManagedBuildManager.addExtensionOutputType(this);
    }

    /**
     * This constructor is called to create an OutputType whose attributes will be
     * set by separate calls.
     *
     * @param parent
     *            The parent of the an OutputType
     * @param superClass
     *            The superClass, if any
     * @param Id
     *            The id for the new OutputType
     * @param name
     *            The name for the new OutputType
     * @param isExtensionElement
     *            Indicates whether this is an extension element or a managed
     *            project element
     */
    public OutputType(Tool parent, IOutputType superClass, String Id, String name, boolean isExtensionElement) {
        this.parent = parent;
        this.superClass = superClass;
        if (this.superClass != null) {
            superClassId = this.superClass.getId();
        }
        setId(Id);
        setName(name);
        isExtensionOutputType = isExtensionElement;
        if (isExtensionElement) {
            // Hook me up to the Managed Build Manager
            ManagedBuildManager.addExtensionOutputType(this);
        } else {
            //setDirty(true);
            //setRebuildState(true);
        }
    }

    /**
     * Create an <code>OutputType</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>ITool</code> the OutputType will be added to.
     * @param element
     *            The XML element that contains the OutputType settings.
     */
    public OutputType(ITool parent, ICStorageElement element) {
        this.parent = parent;
        isExtensionOutputType = false;

        // Initialize from the XML attributes
        loadFromProject(element);
    }

    /**
     * Create an <code>OutputType</code> based upon an existing OutputType.
     *
     * @param parent
     *            The <code>ITool</code> the OutputType will be added to.
     * @param Id
     *            The identifier of the new OutputType
     * @param name
     *            The name of the new OutputType
     * @param outputType
     *            The existing OutputType to clone.
     */
    public OutputType(ITool parent, String Id, String name, OutputType outputType) {
        this.parent = parent;
        superClass = outputType.superClass;
        if (superClass != null) {
            if (outputType.superClassId != null) {
                superClassId = outputType.superClassId;
            }
        }
        setId(Id);
        setName(name);
        isExtensionOutputType = false;
        boolean copyIds = Id.equals(outputType.id);

        //  Copy the remaining attributes
        if (outputType.optionId != null) {
            optionId = outputType.optionId;
        }
        if (outputType.buildVariable != null) {
            buildVariable = outputType.buildVariable;
        }
        if (outputType.outputPrefix != null) {
            outputPrefix = outputType.outputPrefix;
        }
        if (outputType.outputName != null) {
            outputName = outputType.outputName;
        }
        if (outputType.namePattern != null) {
            namePattern = outputType.namePattern;
        }

        nameProviderElement = outputType.nameProviderElement;
        nameProvider = outputType.nameProvider;

        if (copyIds) {
            isDirty = outputType.isDirty;
            rebuildState = outputType.rebuildState;
        } else {
           // setDirty(true);
           // setRebuildState(true);
        }
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /* (non-Javadoc)
     * Loads the OutputType information from the ManagedConfigElement specified in the
     * argument.
     *
     * @param element Contains the OutputType information
     */
    protected void loadFromManifest(IManagedConfigElement element) {
        ManagedBuildManager.putConfigElement(this, element);

        // id
        setId(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.ID)));

        // Get the name
        setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));

        // option
        optionId = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.OPTION));


        // outputPrefix
        outputPrefix = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.OUTPUT_PREFIX));

        // outputNames
        outputName = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.OUTPUT_NAME));

        // namePattern
        namePattern = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.NAME_PATTERN));

        // buildVariable
        buildVariable = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.BUILD_VARIABLE));

        // Store the configuration element IFF there is a name provider defined
        String nameProvider = element.getAttribute(IOutputType.NAME_PROVIDER);
        if (nameProvider != null && element instanceof DefaultManagedConfigElement) {
            nameProviderElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
        }
    }

    /* (non-Javadoc)
     * Initialize the OutputType information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the OutputType information
     */
    protected void loadFromProject(ICStorageElement element) {

        // id (unique, do not intern)
        setId(element.getAttribute(IBuildObject.ID));

        // name
        if (element.getAttribute(IBuildObject.NAME) != null) {
            setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));
        }

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
        if (superClassId != null && superClassId.length() > 0) {
            superClass = ManagedBuildManager.getExtensionOutputType(superClassId);
            if (superClass == null) {
                // TODO:  Report error
            }
        }

        // option
        if (element.getAttribute(IOutputType.OPTION) != null) {
            optionId = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.OPTION));
        }


        // outputPrefix
        if (element.getAttribute(IOutputType.OUTPUT_PREFIX) != null) {
            outputPrefix = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.OUTPUT_PREFIX));
        }

        // outputNames
        if (element.getAttribute(IOutputType.OUTPUT_NAME) != null) {
            outputName = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.OUTPUT_NAME));
        }

        // namePattern
        if (element.getAttribute(IOutputType.NAME_PATTERN) != null) {
            namePattern = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.NAME_PATTERN));
        }

        // buildVariable
        if (element.getAttribute(IOutputType.BUILD_VARIABLE) != null) {
            buildVariable = SafeStringInterner.safeIntern(element.getAttribute(IOutputType.BUILD_VARIABLE));
        }

        // Note: Name Provider cannot be specified in a project file because
        //       an IConfigurationElement is needed to load it!
        if (element.getAttribute(IOutputType.NAME_PROVIDER) != null) {
            // TODO:  Issue warning?
        }
    }

    /**
     * Persist the OutputType to the project file.
     */
    public void serialize(ICStorageElement element) {
        if (superClass != null)
            element.setAttribute(IProjectType.SUPERCLASS, superClass.getId());

        element.setAttribute(IBuildObject.ID, id);

        if (name != null) {
            element.setAttribute(IBuildObject.NAME, name);
        }


        if (optionId != null) {
            element.setAttribute(IOutputType.OPTION, optionId);
        }


        if (outputPrefix != null) {
            element.setAttribute(IOutputType.OUTPUT_PREFIX, outputPrefix);
        }

        if (outputName != null) {
            element.setAttribute(IOutputType.OUTPUT_NAME, outputName);
        }

        if (namePattern != null) {
            element.setAttribute(IOutputType.NAME_PATTERN, namePattern);
        }

        if (buildVariable != null) {
            element.setAttribute(IOutputType.BUILD_VARIABLE, buildVariable);
        }

        // Note: dependency generator cannot be specified in a project file because
        //       an IConfigurationElement is needed to load it!
        if (nameProviderElement != null) {
            //  TODO:  issue warning?
        }

        // I am clean now
        isDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOutputType#getParent()
     */
    @Override
    public ITool getParent() {
        return parent;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IOutputType#getSuperClass()
     */
    @Override
    public IOutputType getSuperClass() {
        return superClass;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOutputType#getName()
     */
    @Override
    public String getName() {
        return (name == null && superClass != null) ? superClass.getName() : name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOutputType#getBuildVariable()
     */
    @Override
    public String getBuildVariable() {
        if (buildVariable == null) {
            // If I have a superClass, ask it
            if (superClass != null) {
                return superClass.getBuildVariable();
            } else {
                //  Use default name
                String name = getName();
                if (name == null || name.length() == 0) {
                    name = getId();
                }
                String defaultName = name.toUpperCase();
                defaultName = defaultName.replaceAll("\\W", "_"); //$NON-NLS-1$  //$NON-NLS-2$
                defaultName += "_OUTPUTS"; //$NON-NLS-1$
                return defaultName;
            }
        }
        return buildVariable;
    }





    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOuputType#getOptionId()
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




//    /* (non-Javadoc)
//     *  Resolve the element IDs to interface references
//     */
//    public void resolveReferences() {
//        if (!resolved) {
//            resolved = true;
//            // Resolve superClass
//            if (superClassId != null && superClassId.length() > 0) {
//                superClass = ManagedBuildManager.getExtensionOutputType(superClassId);
//                if (superClass == null) {
//                    // Report error
//                    ManagedBuildManager.outputResolveError("superClass", //$NON-NLS-1$
//                            superClassId, "outputType", //$NON-NLS-1$
//                            getId());
//                }
//            }
//
//            // Resolve content types
//            IContentTypeManager manager = Platform.getContentTypeManager();
//            if (outputContentTypeId != null && outputContentTypeId.length() > 0) {
//                outputContentType = manager.getContentType(outputContentTypeId);
//            }
//
//            // Resolve primary input type
//            if (primaryInputTypeId != null && primaryInputTypeId.length() > 0) {
//                primaryInputType = parent.getInputTypeById(primaryInputTypeId);
//            }
//        }
//    }

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
        return rebuildState;
    }


    public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
        if (booleanExpressionCalculator == null) {
            if (superClass != null) {
                return ((OutputType) superClass).getBooleanExpressionCalculator();
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

        return calc.isOutputTypeEnabled(tool, this);
    }

    public boolean hasCustomSettings() {
        //TODO:
        return false;
    }


    @Override
    public IFile getOutputName(IFile inputFile, ICConfigurationDescription config, IInputType inputType) {
		if (nameProvider != null) {
			String outputFile= nameProvider.getOutputFileName(inputFile, config,  inputType);
			if(outputFile==null) {
				return null;
			}
			return getOutputFile(inputFile,outputFile);
		}

		if (outputName != null && !outputName.isEmpty()) {
			return getOutputFile(inputFile,outputName);
		}

		if (outputExtension != null && !outputExtension.isEmpty()) {
			return getOutputFile(inputFile,inputFile.getName()+"."+outputExtension);
		}
		return null;
    }

	private IFile getOutputFile(IFile inputFile, String outputFile) {
		// TODO Auto-generated method stub
		return null;
	}

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOutputType#isOutputExtension()
     */
    @Override
    public boolean isOutputExtension(ITool tool, String ext) {
        if (outputExtension != null) {
                if (ext.equals(outputExtension))
                    return true;
        }
        return false;
    }

	
}
