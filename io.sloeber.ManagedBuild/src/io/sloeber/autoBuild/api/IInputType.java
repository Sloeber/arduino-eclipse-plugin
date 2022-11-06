/*******************************************************************************
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
package io.sloeber.autoBuild.api;


import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.content.IContentType;

/**
 * This interface represents an inputType instance in the managed build system.
 * It describes one category of input files to a Tool.  A tool can have
 * multiple inputType children.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IInputType extends IBuildObject {
	public static final String INPUT_TYPE_ELEMENT_NAME = "inputType"; //$NON-NLS-1$
	public static final String SOURCE_CONTENT_TYPE = "sourceContentType"; //$NON-NLS-1$
	public static final String HEADER_CONTENT_TYPE = "headerContentType"; //$NON-NLS-1$
	public static final String SOURCES = "sources"; //$NON-NLS-1$
	public static final String HEADERS = "headers"; //$NON-NLS-1$
	public static final String DEPENDENCY_CONTENT_TYPE = "dependencyContentType"; //$NON-NLS-1$
	public static final String DEPENDENCY_EXTENSIONS = "dependencyExtensions"; //$NON-NLS-1$
	public static final String OPTION = "option"; //$NON-NLS-1$
	public static final String ASSIGN_TO_OPTION = "assignToOption"; //$NON-NLS-1$
	public static final String MULTIPLE_OF_TYPE = "multipleOfType"; //$NON-NLS-1$
	public static final String PRIMARY_INPUT = "primaryInput"; //$NON-NLS-1$
	public static final String BUILD_VARIABLE = "buildVariable"; //$NON-NLS-1$
	public static final String LANGUAGE_ID = "languageId"; //$NON-NLS-1$
	public static final String LANGUAGE_NAME = "languageName"; //$NON-NLS-1$
	public static final String LANGUAGE_INFO_CALCULATOR = "languageInfoCalculator"; //$NON-NLS-1$

	// The attribute name for the scanner info collector
	public static final String SCANNER_CONFIG_PROFILE_ID = "scannerConfigDiscoveryProfileId"; //$NON-NLS-1$

	/**
	 * Creates an inputOrder child for this InputType.
	 *
	 * @param path The path associated with the InputOrder element
	 * @return IInputOrder of the new element
	 */
	public IInputOrder createInputOrder(String path);

	/**
	 * Removes the InputOrder element with the path specified in the argument.
	 *
	 * @param path The path associated with the InputOrder element
	 */
	public void removeInputOrder(String path);

	/**
	 * Removes the InputOrder element specified in the argument.
	 *
	 * @param element The InputOrder element
	 */
	public void removeInputOrder(IInputOrder element);

	/**
	 * Returns all of the InputOrder children of this InputType
	 *
	 * @return IInputOrder[]
	 */
	public IInputOrder[] getInputOrders();

	/**
	 * Returns the InputOrder element with the path specified in the argument.
	 *
	 * @param path The path associated with the InputOrder element
	 * @return IInputOrder
	 */
	public IInputOrder getInputOrder(String path);

	/**
	 * Creates an additionalInput child for this InputType.
	 *
	 * @param path The path associated with the AdditionalInput element
	 * @return IAdditionalInput of the new element
	 */
	public IAdditionalInput createAdditionalInput(String path);

	/**
	 * Removes the AdditionalInput element with the path specified in the argument.
	 *
	 * @param path The path associated with the AdditionalInput element
	 */
	public void removeAdditionalInput(String path);

	/**
	 * Removes the AdditionalInput element specified in the argument.
	 *
	 * @param element The AdditionalInput element
	 */
	public void removeAdditionalInput(IAdditionalInput element);

	/**
	 * Returns all of the AdditionalInput children of this InputType
	 *
	 * @return IAdditionalInput[]
	 */
	public IAdditionalInput[] getAdditionalInputs();

	/**
	 * Returns the AdditionalInput element with the path specified in the argument.
	 *
	 * @param path The path associated with the AdditionalInput element
	 * @return IAdditionalInput
	 */
	public IAdditionalInput getAdditionalInput(String path);

	/**
	 * Returns all of the additional input resources of this InputType.
	 * Note: This does not include additional dependencies.
	 *
	 * @return IPath[]
	 */
	public IPath[] getAdditionalResources();

	/**
	 * Returns all of the additional dependency resources of this InputType.
	 * Note: This does not include additional inputs.
	 *
	 * @return IPath[]
	 */
	public IPath[] getAdditionalDependencies();

	/**
	 * Returns the tool parent of this InputType.
	 *
	 * @return ITool
	 */
	public ITool getParent();

	/**
	 * Returns the <code>IInputType</code> that is the superclass of this
	 * InputType, or <code>null</code> if the attribute was not specified.
	 *
	 * @return IInputType
	 */
	public IInputType getSuperClass();

	/**
	 * Returns the Eclipse <code>IContentType</code> that describes this
	 * input type.  If both the sources attribute and the sourceContentType
	 * attribute are specified, the sourceContentType will be used if it
	 * is registered in Eclipse.
	 *
	 * @return IContentType
	 */
	public IContentType getSourceContentType();

	public IContentType[] getSourceContentTypes();

	public IContentType[] getHeaderContentTypes();

	public String[] getSourceContentTypeIds();

	public String[] getHeaderContentTypeIds();

	public String[] getHeaderExtensions(ITool tool);

	public String[] getHeaderExtensionsAttribute();

	public void setSourceContentTypeIds(String[] ids);

	public void setHeaderContentTypeIds(String[] ids);

	public void setSourceExtensionsAttribute(String[] extensions);

	public void setHeaderExtensionsAttribute(String[] extensions);

	/**
	 * Sets the Eclipse <code>IContentType</code> that describes this
	 * input type.
	 *
	 * @param contentType  The Eclipse content type
	 */
	public void setSourceContentType(IContentType contentType);

	/**
	 * Returns the list of valid source extensions from the
	 * sourceExtensions attribute. Note that this value is not used
	 * if source content type is specified and registered with Eclipse.
	 * Also, the user will not be able to modify the set of file
	 * extensions as they can when sourceContentType is specified.
	 *
	 * @return String[]
	 */
	public String[] getSourceExtensionsAttribute();

	/**
	 * Sets the list of valid source extensions for this input type.
	 * NOTE: The value of this attribute will NOT be used if a
	 *       source content type is specified and is registered with
	 *       Eclipse.
	 *
	 * @param extensions  The comma-separated list of valid file extensions
	 *                    - not including the separator period.
	 */
	public void setSourceExtensionsAttribute(String extensions);

	/**
	 * Returns the list of valid source extensions for this input type.
	 * Note that the list will come from the sourceContentType if it
	 * is specified and registered with Eclipse.  Otherwise the
	 * sourceExtensions attribute will be used.
	 *
	 * @param tool  the tool that contains the input-type
	 * @return String[]
	 */
	public String[] getSourceExtensions(ITool tool);

	/**
	 * Answers <code>true</code> if the input type considers the file extension to be
	 * one associated with a source file.
	 *
	 * @param tool  the tool that contains the input-type
	 * @param ext  file extension of the source
	 * @return boolean
	 */
	public boolean isSourceExtension(ITool tool, String ext);

	/**
	 * Returns the Eclipse <code>IContentType</code> that describes the
	 * dependency files of this input type.  If both the dependencyExtensions
	 * attribute and the dependencyContentType attribute are specified,
	 * the dependencyContentType will be used if it is defined in Eclipse.
	 *
	 * @return IContentType
	 */
	public IContentType getDependencyContentType();

	/**
	 * Sets the Eclipse <code>IContentType</code> that describes the
	 * dependency files of this input type.
	 */
	public void setDependencyContentType(IContentType type);

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
	 * Sets the list of valid dependency extensions for this input type.
	 * NOTE: The value of this attribute will NOT be used if a
	 *       dependency content type is specified and is registered with
	 *       Eclipse.
	 *
	 * @param extensions  The comma-separated list of valid dependency extensions
	 *                    - not including the separator period.
	 */
	public void setDependencyExtensionsAttribute(String extensions);

	/**
	 * Returns the list of valid dependency extensions for this input type.
	 * Note that the list will come from the dependencyContentType if it
	 * is specified and registered with Eclipse.  Otherwise the
	 * dependencyExtensions attribute will be used.
	 *
	 * @param tool  the tool that contains the input-type
	 * @return String[]
	 */
	public String[] getDependencyExtensions(ITool tool);

	/**
	 * Answers <code>true</code> if the input type considers the file extension to be
	 * one associated with a dependency file.
	 *
	 * @param tool  the tool that contains the input-type
	 * @param ext  file extension of the source
	 * @return boolean
	 */
	public boolean isDependencyExtension(ITool tool, String ext);

	/**
	 * Returns the id of the option that is associated with this input
	 * type on the command line.  If specified, the name(s) of the input
	 * files for this input type are taken from the value specified
	 * for the option.
	 *
	 * @return String
	 */
	public String getOptionId();

	/**
	 * Sets the id of the option that is associated with this input type on
	 * the command line.  If specified, the name(s) of the input files for
	 * this input type are taken from the value specified for the option.
	 */
	public void setOptionId(String optionId);

	/**
	 * Returns the id of the option whose value is to be assigned to the
	 * file(s) calculated for this input type.  The default is not to
	 * assign the input file(s) to a command line option but to assign the
	 * files to the ${Inputs} part of the command line.  Note that the
	 * option value is only updated during build file generation and therefore
	 * could be out of sync with the project until build file generation
	 * occurs.
	 *
	 * @return String
	 */
	public String getAssignToOptionId();

	/**
	 * Sets the id of the option whose value is to be assigned to the
	 * file(s) calculated for this input type.  The default is not to
	 * assign the input file(s) to a command line option but to assign the
	 * files to the ${Inputs} part of the command line.  Note that the
	 * option value is only updated during build file generation and therefore
	 * could be out of sync with the project until build file generation
	 * occurs.
	 */
	public void setAssignToOptionId(String optionId);

	/**
	 * Returns <code>true</code> if this inputType can contain multiple input
	 * resources, else <code>false</code>.  The inputs can be project resources,
	 * or the outputs of other tools in the tool-chain.
	 *
	 * @return boolean
	 */
	public boolean getMultipleOfType();

	/**
	 * Sets whether this inputType can contain multiple input resources
	 */
	public void setMultipleOfType(boolean multiple);

	/**
	 * Returns <code>true</code> if this inputType is considered the primary input
	 * of the tool, else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean getPrimaryInput();

	/**
	 * Sets whether this inputType is considered the primary input of the tool
	 */
	public void setPrimaryInput(boolean primary);

	/**
	 * Returns a class instance that implements an interface to generate
	 * source-level dependencies for this input type.
	 * This method may return <code>null</code> in which case, the receiver
	 * should assume that the input type does not require dependency information
	 * when the project is built.
	 *
	 * @return IManagedDependencyGeneratorType
	 */
	//public IManagedDependencyGeneratorType getDependencyGenerator();

	/**
	 * Returns the name of the build variable associated this this input type's resources
	 * The build variable used in the build file to represent the list of input files when
	 * multipleOfType is True.  The same variable name can be used by an outputType to
	 * identify a set of output files that contribute to this tool's input
	 * (i.e., those using the same buildVariable name).  The default name is chosen by MBS.
	 *
	 * @return String
	 */
	public String getBuildVariable();

	/**
	 * Sets the name of the build variable associated this this input type's resources
	 */
	public void setBuildVariable(String variableName);

	/**
	 * Returns <code>true</code> if this element has changes that need to
	 * be saved in the project file, else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean isDirty();

	/**
	 * Returns <code>true</code> if this InputType was loaded from a manifest file,
	 * and <code>false</code> if it was loaded from a project (.cdtbuild) file.
	 *
	 * @return boolean
	 */
	public boolean isExtensionElement();

	/**
	 * Sets the element's "dirty" (have I been modified?) flag.
	 */
	public void setDirty(boolean isDirty);

	String getLanguageId(ITool tool);

	String getLanguageName(ITool tool);

	String getDiscoveryProfileId(ITool tool);

	void setLanguageIdAttribute(String id);

	void setLanguageNameAttribute(String name);

}
