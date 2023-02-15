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

import static io.sloeber.autoBuild.integration.AutoBuildConstants.DUMPLEAD;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.EQUAL;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.NEWLINE;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class InputType extends SchemaObject implements IInputType {

    private static final String DEFAULT_SEPARATOR = ","; //$NON-NLS-1$
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    // Parent and children
    private ITool parent;

    // Superclass
    private IInputType superClass;

    // Managed Build model attributes
    private List<IContentType> mySourceContentTypes = new ArrayList<>();

    private List<String> inputExtensions = new ArrayList<>();
    private IContentType dependencyContentType;
    private List<String> dependencyExtensions = new ArrayList<>();

    // read from model
    private String[] modelSourceContentType;
    private String[] modelExtensions;
    private String[] modelOutputTypeID;
    private String[] modelOption;
    private String[] modelAssignToOption;
    private String[] modelDependencyContentType;
    private String[] modelDependencyExtensions;
    private String[] modelScannerConfigDiscoveryProfileID;
    private String[] modelLanguageID;
    private String[] modelLanguageInfoCalculator;

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

        try {
            resolveFields();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

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
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getName()
     */
    @Override
    public String getName() {
        return myName;
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

    @Override
    public String getOutputTypeID() {
        return modelOutputTypeID[SUPER];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IInputType#getDependencyContentType()
     */
    @Override
    public IContentType getDependencyContentType() {
        return dependencyContentType;
    }

    @Override
    public String[] getDependencyExtensionsAttribute() {
        return dependencyExtensions.toArray(new String[dependencyExtensions.size()]);
    }

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
        return mySourceContentTypes;
    }

    @Override
    public List<String> getSourceExtensionsAttribute() {
        return inputExtensions;
    }

    //  

    @Override
    public String getDiscoveryProfileId(ITool tool) {
        String discoveryid = getDiscoveryProfileIdAttribute();
        //        if (id == null) {
        //            id = ((Tool) tool).getDiscoveryProfileId();
        //        }
        // if there is more than one ('|'-separated), return the first one
        // TODO: expand interface with String[] getDiscoveryProfileIds(ITool tool)
        if (null != discoveryid) {
            int nPos = discoveryid.indexOf('|');
            if (nPos > 0)
                discoveryid = discoveryid.substring(0, nPos);
        }
        return discoveryid;
    }

    public String getDiscoveryProfileIdAttribute() {
        return modelScannerConfigDiscoveryProfileID[SUPER];
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
    public boolean isAssociatedWith(IFile file, IOutputType outputType) {
        if (outputType != null) {
            if (modelOutputTypeID[SUPER].equals(outputType.getId())
                    || inputExtensions.contains(file.getFileExtension())) {
                return true;
            }
        }

        for (IContentType curContentType : mySourceContentTypes) {
            if (curContentType.isAssociatedWith(file.getName())) {
                return true;
            }
        }
        if (mySourceContentTypes.size() > 0) {
            return false;
        }
        if (inputExtensions.contains(file.getFileExtension())) {
            return true;
        }

        return false;
    }

    private void resolveFields() throws Exception {
        if (modelSourceContentType[SUPER] != null) {
            IContentTypeManager manager = Platform.getContentTypeManager();
            StringTokenizer tokenizer = new StringTokenizer(modelSourceContentType[SUPER], DEFAULT_SEPARATOR);
            while (tokenizer.hasMoreElements()) {
                String curToken = tokenizer.nextToken();
                IContentType type = manager.getContentType(curToken);
                if (type != null) {
                    mySourceContentTypes.add(type);
                } else {
                    System.err.println("failed to load source content type :" + curToken); //$NON-NLS-1$
                }
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
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + INPUT_TYPE_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + SOURCE_CONTENT_TYPE + EQUAL + modelSourceContentType[SUPER] + NEWLINE);
        ret.append(prepend + EXTENSIONS + EQUAL + modelExtensions[SUPER] + NEWLINE);
        ret.append(prepend + OUTPUT_TYPE_ID + EQUAL + modelOutputTypeID[SUPER] + NEWLINE);
        ret.append(prepend + OPTION + EQUAL + modelOption[SUPER] + NEWLINE);
        ret.append(prepend + ASSIGN_TO_OPTION + EQUAL + modelAssignToOption[SUPER] + NEWLINE);

        ret.append(prepend + DEPENDENCY_CONTENT_TYPE + EQUAL + modelDependencyContentType[SUPER] + NEWLINE);
        ret.append(prepend + DEPENDENCY_EXTENSIONS + EQUAL + modelDependencyExtensions[SUPER] + NEWLINE);
        ret.append(prepend + SCANNER_CONFIG_PROFILE_ID + EQUAL + modelScannerConfigDiscoveryProfileID[SUPER] + NEWLINE);
        ret.append(prepend + LANGUAGE_ID + EQUAL + modelLanguageID[SUPER] + NEWLINE);
        ret.append(prepend + LANGUAGE_INFO_CALCULATOR + EQUAL + modelLanguageInfoCalculator[SUPER] + NEWLINE);

        return ret;
    }

}
