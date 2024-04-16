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
package io.sloeber.autoBuild.schema.api;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.content.IContentType;

/**
 * This interface represents an inputType instance in the managed build system.
 * It describes one category of input files to a Tool. A tool can have
 * multiple inputType children.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IInputType extends ISchemaObject {

    /**
     * Returns the tool parent of this InputType.
     *
     * @return ITool
     */
    public ITool getTool();

    /**
     * Returns the Eclipse <code>IContentType</code> that describes this
     * input type. If both the sources attribute and the sourceContentType
     * attribute are specified, the sourceContentType will be used if it
     * is registered in Eclipse.
     *
     * @return IContentType
     */
    public List<IContentType> getSourceContentTypes();

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
     * Returns the Eclipse <code>IContentType</code> that describes the
     * dependency files of this input type. If both the dependencyExtensions
     * attribute and the dependencyContentType attribute are specified,
     * the dependencyContentType will be used if it is defined in Eclipse.
     *
     * @return IContentType
     */
    public IContentType getDependencyContentType();

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

    String getDiscoveryProfileId(ITool tool);

    /**
     * Does the given file belong to this input type
     * F.I.
     * If the file is named myFile.cpp and the input type is of
     * object files this method will return false.
     * 
     * @param file
     * @param outputType
     *            if null this is a source file. for generated files provide the
     *            outputType that generated the file
     * @return true if the file matches the inputType
     */
    public boolean isAssociatedWith(IFile file, IOutputType outputType);

    /**
     * Get the attribute assignToCommandVarriable
     * If no such attribute has been provided returns "INPUTS"
     * 
     * @return the string representation of the variable in the command to assign
     *         the input files to
     */
    public String getAssignToCmdVarriable();

    public String getLanguageID();

}
