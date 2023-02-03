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
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.Const.*;
import org.apache.commons.lang.StringUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;



public class OutputType extends SchemaObject implements IOutputType {

    private String[] modelOutputContentType;//Not yet implemented
    private String[] modelOption;
    private String[] modelOutputPrefix;
    private String[] modelOutputExtension;
    private String[] modelOutputName;
    private String[] modelNamePattern; //Not yet implemented
    private String[] modelNameProvider;
    private String[] modelBuildVariable;

    private IOutputNameProvider nameProvider = null;
    private String buildVariable;
    private IContentType outputContentType;

    //    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;
    //
    //    private List<OptionEnablementExpression> myOptionEnablementExpression = new ArrayList<>();;

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
    public OutputType(ITool parent, IExtensionPoint root, IConfigurationElement element) {

        loadNameAndID(root, element);

        modelOutputContentType = getAttributes(OUTPUT_CONTENT_TYPE);
        modelOption = getAttributes(OPTION);
        modelOutputPrefix = getAttributes(OUTPUT_PREFIX);
        modelOutputExtension = getAttributes(OUTPUT_EXTENSION);
        modelOutputName = getAttributes(OUTPUT_NAME);
        modelNamePattern = getAttributes(NAME_PATTERN);
        modelNameProvider = getAttributes(NAME_PROVIDER);
        modelBuildVariable = getAttributes(BUILD_VARIABLE);

        // buildVariable
        buildVariable = element.getAttribute(IOutputType.BUILD_VARIABLE);

        //        myOptionEnablementExpression.clear();
        //        IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        //        for (IConfigurationElement curEnablement : enablements) {
        //            myOptionEnablementExpression.add(new OptionEnablementExpression(curEnablement));
        //        }
        //        booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(myOptionEnablementExpression);

        if (modelBuildVariable[SUPER].isBlank()) {
            buildVariable = myName.toUpperCase().replaceAll("\\W", "_"); //$NON-NLS-1$  //$NON-NLS-2$
            buildVariable += "_OUTPUTS"; //$NON-NLS-1$
        } else {
            buildVariable = modelBuildVariable[SUPER];
        }

        if (!modelNameProvider[SUPER].isBlank()) {
            nameProvider = (IOutputNameProvider) createExecutableExtension(NAME_PROVIDER);
        }

        // Resolve content types
        IContentTypeManager manager = Platform.getContentTypeManager();
        if (!modelOutputContentType[SUPER].isBlank()) {
            outputContentType = manager.getContentType(modelOutputContentType[SUPER]);
        }

    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOutputType#getBuildVariable()
     */
    @Override
    public String getBuildVariable() {

        return buildVariable;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOuputType#getOptionId()
     */
    @Override
    public String getOptionId() {
        return modelOption[SUPER];
    }

    @Override
    public IContentType getOutputContentType() {
        return outputContentType;
    }

    //    public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
    //        return booleanExpressionCalculator;
    //    }
    //
    //    public boolean isEnabled(ITool tool) {
    //        BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
    //        if (calc == null)
    //            return true;
    //
    //        return calc.isOutputTypeEnabled(tool, this);
    //    }

    @Override
    public IFile getOutputName(IFolder buildFolder, IFile inputFile, ICConfigurationDescription config,
            IInputType inputType) {
        if (nameProvider != null) {
            String outputFile = nameProvider.getOutputFileName(inputFile, config, inputType, this);
            if (outputFile != null) {
                return getOutputFile(buildFolder, inputFile, outputFile);
            }
            return null;
        }

        if (!modelOutputName[SUPER].isBlank()) {
            return getOutputFile(buildFolder, inputFile, modelOutputName[SUPER]);
        }

        if (!modelOutputPrefix[SUPER].isBlank() || !modelOutputExtension[SUPER].isBlank()) {
            return getOutputFile(buildFolder, inputFile,
                    modelOutputPrefix[SUPER] + inputFile.getName() + DOT + modelOutputExtension[SUPER]);
        }
        return null;
    }

    private static IFile getOutputFile(IFolder buildFolder, IFile inputFile, String outputFile) {
        if (buildFolder.getProjectRelativePath().isPrefixOf(inputFile.getProjectRelativePath())) {
            return ((IFolder) inputFile.getParent()).getFile(outputFile);
        }
        IFolder fileBuldFolder = buildFolder.getFolder(inputFile.getParent().getProjectRelativePath());
        return fileBuldFolder.getFile(outputFile);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOutputType#isOutputExtension()
     */
    @Override
    public boolean isOutputExtension(ITool tool, String ext) {
        return ext.equals(modelOutputExtension[SUPER]);
    }

    @Override
    public String getOutputPrefix() {
        return modelOutputPrefix[SUPER];
    }

    @Override
    public String getOutputExtension() {
        return modelOutputExtension[SUPER];
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + OUTPUT_TYPE_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + OUTPUT_CONTENT_TYPE + EQUAL + modelOutputContentType[SUPER] + NEWLINE);
        ret.append(prepend + OPTION + EQUAL + modelOption[SUPER] + NEWLINE);
        ret.append(prepend + OUTPUT_PREFIX + EQUAL + modelOutputPrefix[SUPER] + NEWLINE);
        ret.append(prepend + OPTION + EQUAL + modelOption[SUPER] + NEWLINE);
        ret.append(prepend + OUTPUT_EXTENSION + EQUAL + modelOutputExtension[SUPER] + NEWLINE);

        ret.append(prepend + OUTPUT_NAME + EQUAL + modelOutputName[SUPER] + NEWLINE);
        ret.append(prepend + NAME_PATTERN + EQUAL + modelNamePattern[SUPER] + NEWLINE);
        ret.append(prepend + NAME_PROVIDER + EQUAL + modelNameProvider[SUPER] + NEWLINE);
        ret.append(prepend + BUILD_VARIABLE + EQUAL + modelBuildVariable[SUPER] + NEWLINE);
        return ret;
    }
}

//    /**
//     * This constructor is called to create an OutputType whose attributes will be
//     * set by separate calls.
//     *
//     * @param parent
//     *            The parent of the an OutputType
//     * @param superClass
//     *            The superClass, if any
//     * @param Id
//     *            The id for the new OutputType
//     * @param name
//     *            The name for the new OutputType
//     * @param isExtensionElement
//     *            Indicates whether this is an extension element or a managed
//     *            project element
//     */
//    public OutputType(Tool parent, IOutputType superClass, String Id, String name, boolean isExtensionElement) {
//        //        this.parent = parent;
//        //        this.superClass = superClass;
//        //        if (this.superClass != null) {
//        //            superClassId = this.superClass.getId();
//        //        }
//        //        setId(Id);
//        //        setName(name);
//        //        isExtensionOutputType = isExtensionElement;
//        //        if (isExtensionElement) {
//        //            // Hook me up to the Managed Build Manager
//        //            ManagedBuildManager.addExtensionOutputType(this);
//        //        } else {
//        //            //setDirty(true);
//        //            //setRebuildState(true);
//        //        }
//    }

//
//private boolean hasCustomSettings() {
//    //TODO:
//    return false;
//}

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

//public boolean needsRebuild() {
//return rebuildState;
//}
///**
//* Create an <code>OutputType</code> based upon an existing OutputType.
//*
//* @param parent
//*            The <code>ITool</code> the OutputType will be added to.
//* @param Id
//*            The identifier of the new OutputType
//* @param name
//*            The name of the new OutputType
//* @param outputType
//*            The existing OutputType to clone.
//*/
//public OutputType(ITool parent, String newID, String newName, OutputType outputType) {
// this.parent = parent;
// superClass = outputType.superClass;
// if (superClass != null) {
//     if (outputType.superClassId != null) {
//         superClassId = outputType.superClassId;
//     }
// }
// id = (newID);
// name = (newName);
// isExtensionOutputType = false;
// boolean copyIds = id.equals(outputType.id);
//
// //  Copy the remaining attributes
// if (outputType.optionId != null) {
//     optionId = outputType.optionId;
// }
// if (outputType.buildVariable != null) {
//     buildVariable = outputType.buildVariable;
// }
// if (outputType.outputPrefix != null) {
//     outputPrefix = outputType.outputPrefix;
// }
// if (outputType.outputName != null) {
//     outputName = outputType.outputName;
// }
// if (outputType.namePattern != null) {
//     namePattern = outputType.namePattern;
// }
//
// nameProviderElement = outputType.nameProviderElement;
// nameProvider = outputType.nameProvider;
//
// if (copyIds) {
//     isDirty = outputType.isDirty;
//     rebuildState = outputType.rebuildState;
// } else {
//     // setDirty(true);
//     // setRebuildState(true);
// }
//}
///**
//* Create an <code>OutputType</code> based on the specification stored in the
//* project file (.cdtbuild).
//*
//* @param parent
//*            The <code>ITool</code> the OutputType will be added to.
//* @param element
//*            The XML element that contains the OutputType settings.
//*/
//public OutputType(ITool parent, ICStorageElement root, ICStorageElement element) {
// this.parent = parent;
// isExtensionOutputType = false;
//
// // Initialize from the XML attributes
// loadNameAndID(element);
//
// // option
// if (element.getAttribute(IOutputType.OPTION) != null) {
//     optionId = element.getAttribute(IOutputType.OPTION);
// }
//
// // outputPrefix
// if (element.getAttribute(IOutputType.OUTPUT_PREFIX) != null) {
//     outputPrefix = element.getAttribute(IOutputType.OUTPUT_PREFIX);
// }
//
// // outputNames
// if (element.getAttribute(IOutputType.OUTPUT_NAME) != null) {
//     outputName = element.getAttribute(IOutputType.OUTPUT_NAME);
// }
//
// // namePattern
// if (element.getAttribute(IOutputType.NAME_PATTERN) != null) {
//     namePattern = element.getAttribute(IOutputType.NAME_PATTERN);
// }
//
// // buildVariable
// if (element.getAttribute(IOutputType.BUILD_VARIABLE) != null) {
//     buildVariable = element.getAttribute(IOutputType.BUILD_VARIABLE);
// }
//
// // Note: Name Provider cannot be specified in a project file because
// //       an IConfigurationElement is needed to load it!
// if (element.getAttribute(IOutputType.NAME_PROVIDER) != null) {
//     // TODO:  Issue warning?
// }
//}