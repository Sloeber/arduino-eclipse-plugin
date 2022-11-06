/*******************************************************************************
 * Copyright (c) 2004, 2011 TimeSys Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     TimeSys Corporation - initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

//import org.eclipse.cdt.managedbuilder.internal.core.OptionReference;

/**
 * @deprecated This class was deprecated in 2.1
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
@Deprecated
public interface IToolReference extends ITool {

    /**
     * Answers a reference to the option. If the reference does not exist,
     * a new reference is created.
     */
    //	public OptionReference createOptionReference(IOption option);

    /**
     * Answers the list of option references contained in the receiver.
     *
     * @return List
     */
    //	public List<OptionReference> getOptionReferenceList();

    /**
     * Answers the tool that the reference has been created for.
     */
    public ITool getTool();

    /**
     * Answers <code>true</code> if the receiver ahs been modified in any way.
     *
     * @return boolean
     */
    @Override
    public boolean isDirty();

    /**
     * Answers <code>true</code> if the reference is a reference to the
     * tool specified in the argument.
     *
     * @param tool
     *            the tool that should be tested
     * @return boolean
     */
    public boolean references(ITool tool);

    /**
     * @param isDirty
     *            The value to set the dirty flag to in the receiver
     */
    @Override
    public void setDirty(boolean isDirty);

    /**
     * Set the tool command in the receiver to be the argument.
     *
     * @return <code>true</code> if the command is changed, else <code>false</code>
     */
    @Override
    public boolean setToolCommand(String cmd);

    /*
     * The following methods are added to allow the converter from ToolReference -> Tool
     * to retrieve the actual value of attributes.  These routines do not go to the
     * referenced Tool for a value if the ToolReference does not have a value.
     */

    /**
     * Answers all of the output extensions that the receiver can build.
     *
     * @return String
     */
    public String getRawOutputExtensions();

    /**
     * Answers the argument that must be passed to a specific tool in order to
     * control the name of the output artifact. For example, the GCC compile and
     * linker use '-o', while the archiver does not.
     *
     * @return String
     */
    public String getRawOutputFlag();

    /**
     * Answers the prefix that the tool should prepend to the name of the build
     * artifact.
     * For example, a librarian usually prepends 'lib' to the target.a
     * 
     * @return String
     */
    public String getRawOutputPrefix();

    /**
     * Answers the command-line invocation defined for the receiver.
     *
     * @return String
     */
    public String getRawToolCommand();

}
