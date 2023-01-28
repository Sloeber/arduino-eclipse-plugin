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
package io.sloeber.schema.api;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.content.IContentType;

import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;

/**
 * This interface represents an outputType instance in the managed build system.
 * It describes one category of output files created by a Tool. A tool can
 * have multiple outputType children.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOutputType extends ISchemaObject {
    public static final String OUTPUT_TYPE_ELEMENT_NAME = "outputType"; //$NON-NLS-1$

    public static final String OUTPUT_CONTENT_TYPE = "outputContentType"; //$NON-NLS-1$
    public static final String OPTION = "option"; //$NON-NLS-1$
    public static final String OUTPUT_PREFIX = "outputPrefix"; //$NON-NLS-1$
    public static final String OUTPUT_EXTENSION = "outputExtension"; //$NON-NLS-1$
    public static final String OUTPUT_NAME = "outputName"; //$NON-NLS-1$
    public static final String NAME_PATTERN = "namePattern"; //$NON-NLS-1$
    public static final String NAME_PROVIDER = "nameProvider"; //$NON-NLS-1$
    public static final String BUILD_VARIABLE = "buildVariable"; //$NON-NLS-1$

    /**
     * Returns the Eclipse <code>IContentType</code> that describes this
     * output type. If both the outputs attribute and the outputContentType
     * attribute are specified, the outputContentType will be used if it
     * is defined in Eclipse.
     *
     * @return IContentType
     */
    // public IContentType getOutputContentType();

    /**
     * Sets the Eclipse <code>IContentType</code> that describes this
     * output type.
     *
     * @param contentType
     *            The Eclipse content type
     */
    //  public void setOutputContentType(IContentType contentType);

    /**
     * Returns the list of valid output extensions from the
     * outputs attribute. Note that this value is not used
     * if output content type is specified and registered with Eclipse.
     * Also, the user will not be able to modify the set of file
     * extensions as they can when outputContentType is specified.
     *
     * @return <code>String[]</code> of extensions
     */
    //  public String[] getOutputExtensionsAttribute();

    /**
     * Sets all of the output extensions that the receiver can build.
     * NOTE: The value of this attribute will NOT be used if a
     * output content type is specified and is registered with
     * Eclipse.
     */
    // public void setOutputExtensionsAttribute(String extensions);

    /**
     * Returns the list of the output extensions that the receiver can build.
     * Note that the list will come from the outputContentType if it
     * is specified and registered with Eclipse. Otherwise the
     * outputs attribute will be used.
     *
     * @param tool
     *            the tool that contains the output-type
     * @return String[]
     */
    //  public String[] getOutputExtensions(ITool tool);

    /**
     * Answers <code>true</code> if the output type considers the file extension to
     * be
     * one associated with an output file.
     *
     * @param tool
     *            the tool that contains the output-type
     * @param ext
     *            file extension
     * @return boolean
     */
    public boolean isOutputExtension(ITool tool, String ext);

    /**
     * Returns the id of the option that is associated with this
     * output type on the command line. The default is to use the Tool
     * "outputFlag" attribute if primaryOutput is True. If option is not
     * specified, and primaryOutput is False, then the output file(s) of
     * this outputType are not added to the command line.
     * When specified, the namePattern, nameProvider and outputNames are ignored.
     *
     * @return String
     */
    public String getOptionId();

    /**
     * Sets the id of the option that is associated with this
     * output type on the command line.
     */
    // public void setOptionId(String optionId);

    /**
     * Returns <code>true</code> if this outputType creates multiple output
     * resources in one invocation of the tool, else <code>false</code>.
     *
     * @return boolean
     */
    // public boolean getMultipleOfType();

    /**
     * Sets whether this outputType can create multiple output resources in
     * one invocation of the tool.
     */
    //public void setMultipleOfType(boolean multiple);

    /**
     * Returns the input type that is used in determining the default
     * names of this output type.
     *
     * @return IInputType
     */
    // public IInputType getPrimaryInputType();

    /**
     * Sets the input type that is used in determining the default
     * names of this output type.
     */
    // public void setPrimaryInputType(IInputType contentType);

    /**
     * Returns <code>true</code> if this is considered the primary output
     * of the tool, else <code>false</code>.
     *
     * @return boolean
     */
    // public boolean getPrimaryOutput();

    /**
     * Sets whether this is the primary output of the tool.
     */
    // public void setPrimaryOutput(boolean primary);

    /**
     * Returns the prefix that the tool should prepend to the name of the build
     * artifact.
     * For example, a librarian usually prepends 'lib' to the target.a
     * 
     * @return String
     */
    // public String getOutputPrefix();

    /**
     * Sets the prefix that the tool should prepend to the name of the build
     * artifact.
     * For example, a librarian usually prepends 'lib' to the target.a
     */
    // public void setOutputPrefix(String prefix);

    /**
     * Returns the file names of the complete set of output files for this
     * outputType
     *
     * @return String[]
     */
    //public String[] getOutputNames();

    /**
     * Sets the complete set of output file names for this outputType
     */
    // public void setOutputNames(String names);

    /**
     * Returns the pattern, using the Gnu pattern rule syntax, for deriving the
     * output resource name from the input resource name. The default is to use
     * the input file base filename with the output extension.
     *
     * @return String
     */
    // public String getNamePattern();

    /**
     * Sets the pattern, using the Gnu pattern rule syntax, for deriving the
     * output resource name from the input resource name.
     *
     */
    //public void setNamePattern(String pattern);

    /**
     * Returns the IManagedOutputNameProvider interface as specified by the
     * nameProvider attribute.
     *
     * @return IManagedOutputNameProvider
     */
    // public IOutputNameProvider getNameProvider();

    /**
     * Returns the name of the build variable associated this this output type's
     * resources
     * The variable is used in the build file to represent the list of output files.
     * The same variable name can be used by an inputType to identify a set of
     * output
     * files that contribute to the tool's input (i.e., those using the same
     * buildVariable
     * name). The default name is chosen by MBS.
     *
     * @return String
     */
    public String getBuildVariable();

    /**
     * Sets the name of the build variable associated this this output type's
     * resources.
     *
     */
    //public void setBuildVariable(String variableName);

    /**
     * Returns <code>true</code> if this element has changes that need to
     * be saved in the project file, else <code>false</code>.
     *
     * @return boolean
     */
    //  public boolean isDirty();

    /**
     * Returns <code>true</code> if this OutputType was loaded from a manifest file,
     * and <code>false</code> if it was loaded from a project (.cdtbuild) file.
     *
     * @return boolean
     */
    // public boolean isExtensionElement();

    /**
     * Sets the element's "dirty" (have I been modified?) flag.
     */
    // public void setDirty(boolean isDirty);

    /**
     * Fiven a file, configurationdescription, and inputtype
     * provide the filename that will be created during the build.
     * <p>
     * Note that configurationdescription, a inputtype are provided as information
     * for advanced name provider functionality
     * Therefore these can be null for convenience reasons.
     * <p>
     * 
     * @param config
     *            The configuration this is asked for or null
     * @param inputType
     *            The input type that leads to this name provider or null
     * @param inputFile
     * @return The file that will be created by the build.
     */
    public IFile getOutputName(IFolder buildFolder, IFile inputFile, ICConfigurationDescription config,
            IInputType inputType);

}
