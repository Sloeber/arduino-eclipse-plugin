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

/**
 * This interface represents an additionalInput instance in the managed build system.
 * This element is only present if the user or a tool integrator needs to define
 * additional inputs or dependencies to a tool.  An inputType element can have
 * multiple additionalInput children.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IAdditionalInput {

	// Schema attribute names for additionalInput elements
	public static final String ADDITIONAL_INPUT_ELEMENT_NAME = "additionalInput"; //$NON-NLS-1$
	public static final String PATHS = "paths"; //$NON-NLS-1$
	public static final String KIND = "kind"; //$NON-NLS-1$
	public static final String ADDITIONAL_DEPENDENCY = "additionaldependency"; //$NON-NLS-1$
	public static final int KIND_ADDITIONAL_DEPENDENCY = 1;
	public static final String ADDITIONAL_INPUT = "additionalinput"; //$NON-NLS-1$
	public static final int KIND_ADDITIONAL_INPUT = 2;
	public static final String ADDITIONAL_INPUT_DEPENDENCY = "additionalinputdependency"; //$NON-NLS-1$
	public static final int KIND_ADDITIONAL_INPUT_DEPENDENCY = 3;

	/**
	 * Returns the InputType parent of this AdditionalInput.
	 *
	 * @return IInputType
	 */
	public IInputType getParent();

	/**
	 * Returns an array of the relative or absolute paths of the resources
	 * to which this element applies.
	 * The resources must be a member of the project, the output from another tool in the
	 * tool-chain, or an external file.  The file name of the path can use GNU Make pattern
	 * rule syntax (in order to generate the name from the input file name).
	 *
	 * @return String[]
	 */
	public String[] getPaths();

	/**
	 * Sets semicolon separated list of the relative or absolute paths of the resources to
	 * which this element applies.
	 */
	public void setPaths(String paths);

	/**
	 * Returns the kind of additional input.  The valid values are:
	 *   KIND_ADDITIONAL_DEPENDENCY - added as a tool dependency, but not to the command line.
	 *   KIND_ADDITIONAL_INPUT - added as an additional input to the command line, but not as a dependency.
	 *   KIND_ADDITIONAL_INPUT_DEPENDENCY - added as both.
	 * The default is KIND_ADDITIONAL_INPUT_DEPENDENCY
	 */
	public int getKind();

	/**
	 * Sets the kind of additional input.
	 */
	public void setKind(int kind);

	/**
	 * Returns <code>true</code> if this element has changes that need to
	 * be saved in the project file, else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean isDirty();

	/**
	 * Sets the element's "dirty" (have I been modified?) flag.
	 */
	public void setDirty(boolean isDirty);

}
