/*******************************************************************************
 * Copyright (c) 2005, 2012 Intel Corporation and others.
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
package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.internal.core.envvar.EnvironmentVariableManager;


/**
 * a trivial implementation of the IBuildEnvironmentVariable
 *
 * @since 3.0
 */
public class BuildEnvVar extends EnvironmentVariable implements IEnvironmentVariable {
	protected BuildEnvVar() {

	}

	public BuildEnvVar(String name) {
		super(name);
	}

	public BuildEnvVar(String name, String value) {
		super(name, value);
	}

	public BuildEnvVar(String name, String value, String delimiter) {
		super(name, value, delimiter);
	}

	public BuildEnvVar(String name, String value, int op) {
		super(name, value, op, EnvironmentVariableManager.getDefault().getDefaultDelimiter());
	}

	public BuildEnvVar(String name, String value, int op, String delimiter) {
		super(name, value, op, delimiter);
	}

	public BuildEnvVar(IEnvironmentVariable var) {
		super(var);
	}
}
