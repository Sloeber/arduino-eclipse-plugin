/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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

import org.eclipse.cdt.core.envvar.IEnvironmentVariable;

/**
 * this interface represents the given environment variable
 * @since 3.0
 */
public interface IBuildEnvironmentVariable extends IEnvironmentVariable {
	public static final int ENVVAR_REPLACE = IEnvironmentVariable.ENVVAR_REPLACE;
	public static final int ENVVAR_REMOVE = IEnvironmentVariable.ENVVAR_REMOVE;
	public static final int ENVVAR_PREPEND = IEnvironmentVariable.ENVVAR_PREPEND;
	public static final int ENVVAR_APPEND = IEnvironmentVariable.ENVVAR_APPEND;

	/**
	 *
	 * @return the variable name
	 */
	@Override
	public String getName();

	/**
	 *
	 * @return the variable value
	 */
	@Override
	public String getValue();

	/**
	 * @return one of the IBuildEnvironmentVariable.ENVVAR_* operation types
	 */
	@Override
	public int getOperation();

	/**
	 * @return if the variable can hold the list of values this method returns the String representing
	 * the delimiter that is used to separate values. This information is used for the following:
	 *
	 * 1. in append and prepend operations:
	 * If the variable already exists and contains some value the new
	 * value will be calculated in the following way:
	 * For the "prepend" operation:
	 * 	<New value> = <the value from the getValue() method><delimiter><Old value>
	 * For the "append" operation:
	 * 	<New value> = <Old value><delimiter><the value from the getValue() method>
	 *
	 * The Environment Variable Provider will also remove the duplicates of "sub-values"
	 * in the resulting value.
	 * For example:
	 * If the current value is "string1:string2:string3", the getDelimiter() method returns ":"
	 * and getValue() method returns "string4:string2" the new value will contain:
	 * For the "prepend" operation: "string4:string2:string1:string3"
	 * For the "append" operation: "string1:string3:string4:string2"
	 *
	 * 2. Since the environment variables are also treated as build macros the delimiter is also used
	 * by the BuildMacroProvider to determine the type of the macro used to represent the
	 * given environment variable. If the variable has the delimiter it is treated as the Text-List macro
	 * otherwise it is treated as the Text macro. (See Build Macro design for more details)
	 *
	 * To specify that no delimiter should be used, the getDelimiter() method should
	 * return null or an empty string
	 */
	@Override
	public String getDelimiter();
}
