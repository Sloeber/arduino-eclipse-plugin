/*******************************x************************************************
 * Copyright (c) 2005, 2010 Intel Corporation and others.
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
 *******************************************************************************/
package io.sloeber.schema.api;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.content.IContentType;

import io.sloeber.schema.internal.IBuildObject;

/**
 * This interface represents an inputType instance in the managed build system.
 * It describes one category of input files to a Tool. A tool can have
 * multiple inputType children.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IInputType extends IBuildObject {
    public static final String INPUT_TYPE_ELEMENT_NAME = "inputType"; //$NON-NLS-1$
    public static final String SOURCE_CONTENT_TYPE = "sourceContentType"; //$NON-NLS-1$
    public static final String EXTENSIONS = "extensions"; //$NON-NLS-1$
    public static final String OUTPUT_TYPE_ID = "outputTypeID"; //$NON-NLS-1$
    public static final String OPTION = "option"; //$NON-NLS-1$
    public static final String ASSIGN_TO_OPTION = "assignToOption"; //$NON-NLS-1$	
    public static final String DEPENDENCY_CONTENT_TYPE = "dependencyContentType"; //$NON-NLS-1$
    public static final String DEPENDENCY_EXTENSIONS = "dependencyExtensions"; //$NON-NLS-1$
    public static final String SCANNER_CONFIG_PROFILE_ID = "scannerConfigDiscoveryProfileId"; //$NON-NLS-1$
    public static final String LANGUAGE_ID = "languageId"; //$NON-NLS-1$
    public static final String LANGUAGE_INFO_CALCULATOR = "languageInfoCalculator"; //$NON-NLS-1$

    /**
     * Returns the tool parent of this InputType.
     *
     * @return ITool
     */
    public ITool getParent();

    /**
     * Returns the Eclipse <code>IContentType</code> that describes this
     * input type. If both the sources attribute and the sourceContentType
     * attribute are specified, the sourceContentType will be used if it
     * is registered in Eclipse.
     *
     * @return IContentType
     */
    public IContentType getSourceContentType();

    public List<IContentType> getSourceContentTypes();

    public String[] getSourceContentTypeIds();

    /**
     * Returns the list of valid source extensions from the
     * sourceExtensions attribute. Note that this value is not used
     * if source content type is specified and registered with Eclipse.
     * Also, the user will not be able to modify the set of file
     * extensions as they can when sourceContentType is specified.
     *
     * @return String[]
     */
    public List<String> getSourceExtensionsAttribute();

    /**
     * Returns the list of valid source extensions for this input type.
     * Note that the list will come from the sourceContentType if it
     * is specified and registered with Eclipse. Otherwise the
     * sourceExtensions attribute will be used.
     *
     * @param tool
     *            the tool that contains the input-type
     * @return String[]
     */
    public List<String> getSourceExtensions(ITool tool);

    /**
     * Returns the Eclipse <code>IContentType</code> that describes the
     * dependency files of this input type. If both the dependencyExtensions
     * attribute and the dependencyContentType attribute are specified,
     * the dependencyContentType will be used if it is defined in Eclipse.
     *
     * @return IContentType
     */
    public IContentType getDependencyContentType();

    /**
     * Returns the list of valid dependency extensions from the
     * dependencyExtensions attribute. Note that this value is not used
     * if dependency content type is specified and registered with Eclipse.
     * Also, the user will not be able to modify the set of file
     * extensions as they can when dependencyContentType is specified.
     *
     * @return String[]
     */
    public String[] getDependencyExtensionsAttribute();

    /**
     * Returns the list of valid dependency extensions for this input type.
     * Note that the list will come from the dependencyContentType if it
     * is specified and registered with Eclipse. Otherwise the
     * dependencyExtensions attribute will be used.
     *
     * @param tool
     *            the tool that contains the input-type
     * @return String[]
     */
    public String[] getDependencyExtensions(ITool tool);

    /**
     * Answers <code>true</code> if the input type considers the file extension to
     * be
     * one associated with a dependency file.
     *
     * @param tool
     *            the tool that contains the input-type
     * @param ext
     *            file extension of the source
     * @return boolean
     */
    public boolean isDependencyExtension(ITool tool, String ext);

    /**
     * Returns the id of the option whose value is to be assigned to the
     * file(s) calculated for this input type. The default is not to
     * assign the input file(s) to a command line option but to assign the
     * files to the ${Inputs} part of the command line. Note that the
     * option value is only updated during build file generation and therefore
     * could be out of sync with the project until build file generation
     * occurs.
     *
     * @return String
     */
    public String getAssignToOptionId();

    /**
     * Returns the name of the build variable associated this this input type's
     * resources
     * The build variable used in the build file to represent the list of input
     * files when
     * multipleOfType is True. The same variable name can be used by an outputType
     * to
     * identify a set of output files that contribute to this tool's input
     * (i.e., those using the same buildVariable name). The default name is chosen
     * by MBS.
     *
     * @return String
     */
    public String getBuildVariable();

    String getLanguageName(ITool tool);

    String getDiscoveryProfileId(ITool tool);

    /**
     * Does the given file belong to this input type
     * F.I.
     * If the file is named myFile.cpp and the input type is of
     * object files this method will return false.
     * 
     * @param file
     * @return true if the file matches the inputType
     */
    public boolean isAssociatedWith(IFile file);

    /**
     * Get the ID of the output tye this input type processes
     * @return an ID of a outputType or an empty string
     */
	public String getOutputTypeID();

	public boolean isAssociatedWith(IFile file, IOutputType outputType);

}
