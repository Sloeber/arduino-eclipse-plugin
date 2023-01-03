/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;

/**
 * This interface represents a utility of some sort that is used in the build process.
 * A tool will generally process one or more resources to produce output resources.
 * Most tools have a set of options that can be used to modify the behavior of the tool.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITool extends IHoldsOptions {
	// Schema element names
	public static final String COMMAND = "command"; //$NON-NLS-1$
	public static final String COMMAND_LINE_PATTERN = "commandLinePattern"; //$NON-NLS-1$
	public static final String COMMAND_LINE_GENERATOR = "commandLineGenerator"; //$NON-NLS-1$
	public static final String DEP_CALC_ID = "dependencyCalculator"; //$NON-NLS-1$
	public static final String INTERFACE_EXTS = "headerExtensions"; //$NON-NLS-1$
	public static final String NATURE = "natureFilter"; //$NON-NLS-1$
	public static final String OUTPUT_FLAG = "outputFlag"; //$NON-NLS-1$
	public static final String INPUT_TYPE = "inputType"; //$NON-NLS-1$
	public static final String OUTPUT_TYPE = "outputType"; //$NON-NLS-1$
	public static final String OUTPUT_PREFIX = "outputPrefix"; //$NON-NLS-1$
	public static final String OUTPUTS = "outputs"; //$NON-NLS-1$
	public static final String SOURCES = "sources"; //$NON-NLS-1$
	public static final String ADVANCED_INPUT_CATEGORY = "advancedInputCategory"; //$NON-NLS-1$
	public static final String CUSTOM_BUILD_STEP = "customBuildStep"; //$NON-NLS-1$
	public static final String ANNOUNCEMENT = "announcement"; //$NON-NLS-1$
	public static final String TOOL_ELEMENT_NAME = "tool"; //$NON-NLS-1$
	public static final String WHITE_SPACE = " "; //$NON-NLS-1$
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$
	public static final String IS_SYSTEM = "isSystem"; //$NON-NLS-1$
	/**
	 * @since 8.4
	 */
	public static final String IS_HIDDEN = "isHidden"; //$NON-NLS-1$

	public static final String VERSIONS_SUPPORTED = "versionsSupported"; //$NON-NLS-1$
	public static final String CONVERT_TO_ID = "convertToId"; //$NON-NLS-1$
	public static final String OPTIONPATHCONVERTER = "optionPathConverter"; //$NON-NLS-1$

	public static final String SUPPORTS_MANAGED_BUILD = "supportsManagedBuild"; //$NON-NLS-1$

	public static final int FILTER_C = 0;
	public static final int FILTER_CC = 1;
	public static final int FILTER_BOTH = 2;

	/**
	 * Returns the tool-chain or resource configuration that is the parent of this tool.
	 *
	 * @return IBuildObject
	 */
	public IBuildObject getParent();


	/**
	 * Removes an InputType from the tool's list.
	 *
	 * @since 3.0
	 */
	public void removeInputType(IInputType type);

	/**
	 * Returns the complete list of input types that are available for this tool.
	 * The list is a merging of the input types specified for this tool with the
	 * input types of its superclasses.  The lowest input type instance in the hierarchy
	 * takes precedence.
	 *
	 * @return IInputType[]
	 * @since 3.0
	 */
	public IInputType[] getInputTypes();



	/**
	 * Returns the primary <code>IInputType</code> in this tool
	 *
	 * <p>If the receiver has no InputTypes,
	 * the method returns <code>null</code>. It is the responsibility of the
	 * caller to verify the return value.
	 *
	 * @return <code>IInputType</code>
	 * @since 3.0
	 */
	public IInputType getPrimaryInputType();

	/**
	 * Returns all of the additional input resources of all InputType children.
	 * Note: This does not include the primary InputType and does not include
	 * additional dependencies.
	 *
	 * @return IPath[]
	 */
	public IPath[] getAdditionalResources();

	/**
	 * Returns all of the additional dependency resources of all InputType children.
	 * Note: This does not include the primary InputType and does not include
	 * additional inputs.
	 *
	 * @return IPath[]
	 */
	public IPath[] getAdditionalDependencies();

	/**
	 * Creates a child OutputType for this tool.
	 *
	 * @param superClass The superClass, if any
	 * @param Id The id for the new OutputType
	 * @param name The name for the new OutputType
	 * @param isExtensionElement Indicates whether this is an extension element or a managed project element
	 *
	 * @return IOutputType
	 * @since 3.0
	 */
	public IOutputType createOutputType(IOutputType superClass, String Id, String name, boolean isExtensionElement);

	/**
	 * Removes an OutputType from the tool's list.
	 *
	 * @since 3.0
	 */
	public void removeOutputType(IOutputType type);

	/**
	 * Returns the complete list of output types that are available for this tool.
	 * The list is a merging of the output types specified for this tool with the
	 * output types of its superclasses.  The lowest output type instance in the hierarchy
	 * takes precedence.
	 *
	 * @return IOutputType[]
	 * @since 3.0
	 */
	public IOutputType[] getOutputTypes();

	/**
	 * Get the <code>IOutputType</code> in the receiver with the specified
	 * ID. This is an efficient search in the receiver.
	 *
	 * <p>If the receiver does not have an OutputType with that ID, the method
	 * returns <code>null</code>. It is the responsibility of the caller to
	 * verify the return value.
	 *
	 * @param id unique identifier of the OutputType to search for
	 * @return <code>IOutputType</code>
	 * @since 3.0
	 */
	public IOutputType getOutputTypeById(String id);

	/**
	 * Returns the <code>IOutputType</code> in the tool that creates the
	 * specified extension.
	 *
	 * <p>If the receiver does not have an OutputType that creates the extension,
	 * the method returns <code>null</code>. It is the responsibility of the
	 * caller to verify the return value.
	 *
	 * @param outputExtension File extension
	 * @return <code>IOutputType</code>
	 * @since 3.0
	 */
	public IOutputType getOutputType(String outputExtension);


	/**
	 * Returns the <code>ITool</code> that is the superclass of this
	 * tool, or <code>null</code> if the attribute was not specified.
	 *
	 * @return ITool
	 */
	public ITool getSuperClass();

	/**
	 * Returns whether this element is abstract.  Returns <code>false</code>
	 * if the attribute was not specified.
	 * @return boolean
	 */
	public boolean isAbstract();

	/**
	 * Sets the isAbstract attribute of the tool-chain.
	 */
	public void setIsAbstract(boolean b);

	/**
	 * Returns a semi-colon delimited list of child Ids of the superclass'
	 * children that should not be automatically inherited by this element.
	 * Returns an empty string if the attribute was not specified.
	 * @return String
	 */
	public String getUnusedChildren();

	/**
	 * Returns the semicolon separated list of unique IDs of the error parsers associated
	 * with the tool.
	 *
	 * @return String
	 */
	public String getErrorParserIds();

	/**
	 * Returns the ordered list of unique IDs of the error parsers associated with the
	 * tool.
	 *
	 * @return String[]
	 */
	public String[] getErrorParserList();

	/**
	 * Sets the semicolon separated list of error parser ids
	 */
	public void setErrorParserIds(String ids);



	/**
	 * Returns the array of valid primary source extensions this tool knows how to build.
	 * The array may be empty but will never be <code>null</code>.
	 *
	 * @return String[]
	 */
	public String[] getPrimaryInputExtensions();

	/**
	 * Returns the array of all valid source extensions this tool knows how to build.
	 * The array may be empty but will never be <code>null</code>.
	 *
	 * @return String[]
	 */
	public String[] getAllInputExtensions();

	/**
	 * Returns the default input extension for the primary input of the tool
	 *
	 * @return String
	 */
	public String getDefaultInputExtension();

	/**
	 * Returns the array of all valid dependency extensions for this tool's inputs.
	 * The array may be empty but will never be <code>null</code>.
	 *
	 * @return String[]
	 */
	public String[] getAllDependencyExtensions();



	/**
	 * Answers a constant corresponding to the project nature the tool should be used
	 * for. Possible answers are:
	 *
	 * <dl>
	 * <dt>ITool.FILTER_C
	 * <dd>The tool should only be displayed for C projects. <i>Notes:</i> even
	 * though a C++ project has a C nature, this flag will mask the tool for C++
	 * projects.
	 * <dt>ITool.FILTER_CC
	 * <dd>The tool should only be displayed for C++ projects.
	 * <dt>ITool.FILTER_BOTH
	 * <dd>The tool should be displayed for projects with both natures.
	 * </dl>
	 *
	 * @return int
	 */
	public int getNatureFilter();





	/**
	 * Answers all of the output extensions that the receiver can build,
	 * from the value of the outputs attribute
	 *
	 * @return <code>String[]</code> of extensions
	 */
	public String[] getOutputsAttribute();



	/**
	 * Sets all of the output extensions that the receiver can build,
	 * into the outputs attribute.  Note that the outputs attribute is
	 * ignored when one or more outputTypes are specified.
	 */
	public void setOutputsAttribute(String extensions);

	/**
	 * Answers the argument that must be passed to a specific tool in order to
	 * control the name of the output artifact. For example, the GCC compile and
	 * linker use '-o', while the archiver does not.
	 *
	 * @return String
	 */
	public String getOutputFlag();





	/**
	 * Returns <code>true</code> if the Tool wants the MBS to display the Advanced
	 * Input category that allows the user to specify additional input resources and
	 * dependencies, else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean getAdvancedInputCategory();

	/**
	 * Sets whether the Tool wants the MBS to display the Advanced
	 * Input category that allows the user to specify additional input resources and
	 * dependencies.
	 */
	public void setAdvancedInputCategory(boolean display);

	/**
	 * Returns <code>true</code> if the Tool represents a user-define custom build
	 * step, else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean getCustomBuildStep();

	/**
	 * Sets whether the Tool represents a user-define custom build step.
	 */
	public void setCustomBuildStep(boolean customBuildStep);

	/**
	 * Returns the announcement string for this tool
	 * @return String
	 */
	public String getAnnouncement();

	/**
	 * Sets the announcement string for this tool
	 */
	public void setAnnouncement(String announcement);

	/**
	 * Answers the command-line invocation defined for the receiver.
	 *
	 * @return String
	 */
	public String getToolCommand();


	/**
	 * Returns command line pattern for this tool
	 * @return String
	 */
	public String getCommandLinePattern();

	/**
	 * Sets the command line pattern for this tool
	 */
	public void setCommandLinePattern(String pattern);

	/**
	 * Returns the command line generator specified for this tool
	 * @return IManagedCommandLineGenerator
	 */
	public IManagedCommandLineGenerator getCommandLineGenerator();



	/**
	 * Returns a class instance that implements an interface to generate
	 * source-level dependencies for the tool specified in the argument.
	 * This method may return <code>null</code> in which case, the receiver
	 * should assume that the tool does not require dependency information
	 * when the project is built.
	 *
	 * @param sourceExt  source file extension
	 * @return IManagedDependencyGeneratorType
	 */
	//public IManagedDependencyGeneratorType getDependencyGeneratorForExtension(String sourceExt);




	/**
	 * Returns an array of command line arguments that have been specified for
	 * the tool.
	 * The flags contain build macros resolved to the makefile format.
	 * That is if a user has chosen to expand all macros in the buildfile,
	 * the flags contain all macro references resolved, otherwise, if a user has
	 * chosen to keep the environment build macros unresolved, the flags contain
	 * the environment macro references converted to the buildfile variable format,
	 * all other macro references are resolved
	 */
	public String[] getToolCommandFlags(IPath inputFileLocation, IPath outputFileLocation) throws BuildException;

	/**
	 * Options are organized into categories for UI purposes.
	 * These categories are organized into a tree.  This is the root
	 * of that tree.
	 *
	 * @return IOptionCategory
	 */
	public IOptionCategory getTopOptionCategory();

	/**
	 * Return <code>true</code> if the receiver builds files with the
	 * specified extension, else <code>false</code>.
	 *
	 * @param extension file extension of the source
	 * @return boolean
	 */
	public boolean buildsFileType(IFile file);


	/**
	 * Answers <code>true</code> if the tool considers the file extension to be
	 * one associated with a header file.
	 *
	 * @param ext file extension of the source
	 * @return boolean
	 */
	public boolean isHeaderFile(String ext);

	/**
	 * Answers <code>true</code> if the receiver builds a file with the extension specified
	 * in the argument, else <code>false</code>.
	 *
	 * @param outputExtension extension of the file being produced by a tool
	 * @return boolean
	 */
	public boolean producesFileType(String outputExtension);





	/**
	 * Returns <code>true</code> if this tool was loaded from a manifest file,
	 * and <code>false</code> if it was loaded from a project (.cdtbuild) file.
	 *
	 * @return boolean
	 */
	public boolean isExtensionElement();

	/**
	 * Returns the 'versionsSupported' of this tool
	 *
	 * @return String
	 */
	public String getVersionsSupported();

	/**
	 * Returns the 'convertToId' of this tool
	 *
	 * @return String
	 */
	public String getConvertToId();

	/**
	 * Sets the 'versionsSupported' attribute of the tool.
	 */
	public void setVersionsSupported(String versionsSupported);

	/**
	 * Sets the 'convertToId' attribute of the tool.
	 */
	public void setConvertToId(String convertToId);

	/**
	 * Returns an array of the Environment Build Path variable descriptors
	 *
	 * @return IEnvVarBuildPath[]
	 */
	public IEnvVarBuildPath[] getEnvVarBuildPaths();

	/**
	 * Returns an IOptionPathConverter implementation for this tool
	 * or null, if no conversion is required
	 */
	public IOptionPathConverter getOptionPathConverter();

	CLanguageData getCLanguageData(IInputType type);

	CLanguageData[] getCLanguageDatas();

	IInputType getInputTypeForCLanguageData(CLanguageData data);

	IResourceInfo getParentResourceInfo();

	IInputType getEditableInputType(IInputType base);


	boolean isEnabled();

	boolean supportsBuild(boolean managed);

	boolean matches(ITool tool);

	boolean isSystemObject();

	/**
	 * @since 8.4
	 */
	boolean isHidden();

	/**
	 * @since 8.4
	 */
	void setHidden(boolean hidden);

	String getUniqueRealName();

	/**
	 * @since 9.4
	 */
	default public String[] getExtraFlags(int optionType) {
		return new String[0];
	}

	/**
	 * Find all matching inutTypes for this tool that matches the file or the macro
	 * <p>
	 * Note that there may be multiple input types that match in most cases that is not relevant
	 * as further processing is based on the output types. However advanced nae providers may filter.<p>
	 * Therefore all input types are needed.
	 * 
	 * @param file
	 * @param macroName
	 * @return
	 */
	public List<IInputType> getMatchingInputTypes(IFile file, String macroName);
}
