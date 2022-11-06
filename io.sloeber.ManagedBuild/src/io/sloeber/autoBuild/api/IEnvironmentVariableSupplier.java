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

import org.eclipse.cdt.core.envvar.IEnvironmentVariable;

/**
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IEnvironmentVariableSupplier {

	/**
	 *
	 * @param name the variable name
	 * @param context the context
	 * @return the reference to the IBuildEnvironmentVariable interface representing
	 * the variable of a given name
	 */
	IEnvironmentVariable getVariable(String name, Object context);

	/**
	 *
	 * @param context the context
	 * @return the array of IBuildEnvironmentVariable that represents the environment variables
	 */
	IEnvironmentVariable[] getVariables(Object context);
}
