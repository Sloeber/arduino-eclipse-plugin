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
package io.sloeber.schema.api;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRules;

/**
 * This interface represents a utility of some sort that is used in the build
 * process.
 * A tool will generally process one or more resources to produce output
 * resources.
 * Most tools have a set of options that can be used to modify the behavior of
 * the tool.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITool extends ISchemaObject {
    public static final String TOOL_ELEMENT_NAME = "tool"; //$NON-NLS-1$

    /**
     * Returns the tool-chain or resource configuration that is the parent of this
     * tool.
     *
     * @return IBuildObject
     */
    public IToolChain getParent();

    /**
     * Returns the complete list of input types that are available for this tool.
     * The list is a merging of the input types specified for this tool with the
     * input types of its superclasses. The lowest input type instance in the
     * hierarchy
     * takes precedence.
     *
     * @return IInputType[]
     * @since 3.0
     */
    public List<IInputType> getInputTypes();

    public IInputType getInputTypeByID(String id);

    /**
     * Returns the complete list of output types that are available for this tool.
     * The list is a merging of the output types specified for this tool with the
     * output types of its superclasses. The lowest output type instance in the
     * hierarchy
     * takes precedence.
     *
     * @return IOutputType[]
     * @since 3.0
     */
    public List<IOutputType> getOutputTypes();

    /**
     * Get the <code>IOutputType</code> in the receiver with the specified
     * ID. This is an efficient search in the receiver.
     *
     * <p>
     * If the receiver does not have an OutputType with that ID, the method
     * returns <code>null</code>. It is the responsibility of the caller to
     * verify the return value.
     *
     * @param id
     *            unique identifier of the OutputType to search for
     * @return <code>IOutputType</code>
     * @since 3.0
     */
    public IOutputType getOutputTypeById(String id);

    /**
     * Returns whether this element is abstract. Returns <code>false</code>
     * if the attribute was not specified.
     * 
     * @return boolean
     */
    public boolean isAbstract();

    /**
     * Returns the semicolon separated list of unique IDs of the error parsers
     * associated
     * with the tool.
     *
     * @return String
     */
    public String getErrorParserIds();

    /**
     * Returns the ordered list of unique IDs of the error parsers associated with
     * the
     * tool.
     *
     * @return String[]
     */
    public String[] getErrorParserList();

    /**
     * Answers the argument that must be passed to a specific tool in order to
     * control the name of the output artifact. For example, the GCC compile and
     * linker use '-o', while the archiver does not.
     *
     * @return String
     */
    public String getOutputFlag();

    /**
     * Returns <code>true</code> if the Tool represents a user-define custom build
     * step, else <code>false</code>.
     *
     * @return boolean
     */
    public boolean getCustomBuildStep();

    /**
     * Returns the announcement string for this tool
     * 
     * @return String
     */
    public String getAnnouncement();

    /**
     * Answers the command-line invocation defined for the receiver.
     *
     * @return String
     */
    public String getDefaultommandLineCommand();

    /**
     * Returns command line pattern for this tool
     * 
     * @return String
     */
    public String getDefaultCommandLinePattern();

    /**
     * Returns the command line generator specified for this tool
     * 
     * @return IManagedCommandLineGenerator
     */
    public IManagedCommandLineGenerator getCommandLineGenerator();

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
    public String[] getToolCommandFlags(IAutoBuildConfigurationDescription autoBuildConfData,
            IResource mySelectedResource);

    CLanguageData getCLanguageData(IInputType type);

    boolean isSystemObject();

    boolean isHidden();

    public MakeRules getMakeRules(IAutoBuildConfigurationDescription autoBuildConfData, IOutputType outputTypeIn,
            IFile inputFile, int makeRuleSequenceID, boolean VERBOSE);

    /**
     * Get the recipes for this tool
     * Note it is recipes (plural) because though in most cases there will be only
     * one recipe
     * there can be multiple
     * 
     * If there are multiple input files and a project level custom command is
     * provided the project level custom command will be used.
     * If there is only one input file (and a custom command is provided that
     * matches
     * the input file) that custom command will be used
     * 
     * @param autoBuildConfData
     * @param inputFiles
     *            the files used as input for the recipes
     * @param flags
     * @param outputName
     *            the target file name to be created
     * @param nicePreReqNameList
     * @return
     */
    public String[] getRecipes(IAutoBuildConfigurationDescription autoBuildConfData, Set<IFile> inputFiles,
            Set<String> flags, String outputName, Map<String, Set<String>> nicePreReqNameList);

    /**
     * Get the dependency file for this target
     * 
     * @param curTargetFile
     * @return the dependency file
     */
    public IFile getDependencyFile(IFile curTargetFile);

    public URL getIconPath();

    public Set<IOptionCategory> getCategories(IAutoBuildConfigurationDescription myAutoBuildConf, IResource resource);

    Set<IOption> getOptionsOfCategory(IOptionCategory cat, IResource resource,
            IAutoBuildConfigurationDescription autoBuildConf);

    boolean isEnabled(IResource resource, IAutoBuildConfigurationDescription autoData);

}
