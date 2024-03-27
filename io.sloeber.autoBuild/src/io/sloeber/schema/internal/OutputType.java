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

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;
import static io.sloeber.autoBuild.internal.AutoBuildCommon.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.enablement.MBSEnablementExpression;

public class OutputType extends SchemaObject implements IOutputType {

    private String[] modelOutputContentType;//Not yet implemented
    private String[] modelOption;
    private String[] modelOutputPrefix;
    private String[] modelOutputExtension;
    private String[] modelOutputName;
    private String[] modelNamePattern;
    private String[] modelNameProvider;
    private String[] modelBuildVariable;

    private IOutputNameProvider nameProvider = null;
    private String buildVariable;
    private IContentType outputContentType;
    private String myNamePattern;
    private ITool myTool;

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

        myTool = parent;
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
        buildVariable = element.getAttribute(BUILD_VARIABLE);

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

        //Make sure the name Patterns is valid
        myNamePattern = modelNamePattern[SUPER];
        if (myNamePattern.toString().isBlank()) {
            myNamePattern = PROCENT;
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

    @Override
    public String getOutputName() {
        return modelOutputName[SUPER];
    }

    @Override
    public String getOutputNameWithoutNameProvider(IFile inputFile) {
        if (!modelOutputName[SUPER].isBlank()) {
            return modelOutputName[SUPER];
        }

        if (!myNamePattern.isBlank()) {
            return AutoBuildCommon.applyPattern(myNamePattern, inputFile);
        }
        if (!modelOutputPrefix[SUPER].isBlank() || !modelOutputExtension[SUPER].isBlank()) {
            return modelOutputPrefix[SUPER] + inputFile.getName() + DOT + modelOutputExtension[SUPER];
        }

        return null;
    }

    @Override
    public IFile getOutputFile(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType) {
        if (!isEnabled(MBSEnablementExpression.ENABLEMENT_TYPE_CMD, inputFile, autoData)) {
            return null;
        }
        IFolder buildFolder = autoData.getBuildFolder();
        if (nameProvider != null) {
            String outputFilename = nameProvider.getOutputFileName(inputFile, autoData, inputType, this);
            if (outputFilename != null) {
                return getOutputFile(autoData, buildFolder, inputFile, outputFilename);
            }
            return null;
        }
        String outputFilename = getOutputNameWithoutNameProvider(inputFile);
        if (outputFilename != null) {
            return getOutputFile(autoData, buildFolder, inputFile, outputFilename);
        }
        return null;
    }

    /**
     * Given the buildFolder, the inputFile and the outputfileName
     * Provides the IFile basically the FQN
     * 
     * If the inputFile is in the build folder the output file is in the root of the
     * buildfolder
     * Else the outputfile is in in the buildfolder with a relative path that
     * is equal to the project relative path
     * 
     * @param autoBuildConfData
     * 
     * @param buildFolder
     * @param inputFile
     * @param outputFile
     * @return
     */
    private static IFile getOutputFile(IAutoBuildConfigurationDescription autoBuildConfData, IFolder buildFolder,
            IFile inputFile, String outputFile) {
        String resolvedFile = AutoBuildCommon.resolve(outputFile, autoBuildConfData);
        resolvedFile=makeNameMakeSafe(resolvedFile);
        if (buildFolder.getProjectRelativePath().isPrefixOf(inputFile.getProjectRelativePath())) {
            return buildFolder.getFile(resolvedFile);
        }
        IFolder fileBuldFolder = buildFolder.getFolder(inputFile.getParent().getProjectRelativePath());
        return fileBuldFolder.getFile(resolvedFile);
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

    @Override
    public ITool getTool() {
        return myTool;
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = DUMPLEAD.repeat(leadingChars);
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
