/*******************************************************************************
 * Copyright (c) 2004, 2010 Intel Corporation and others.
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
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IManagedCommandLineInfo {
	/**
	 * provide fully qualified command line string for tool invocation
	 * @return command line
	 */
	public String getCommandLine();

	/**
	 * give command line pattern
	 */
	public String getCommandLinePattern();

	/**
	 * provide tool name
	 */
	public String getCommandName();

	/**
	 * give command flags
	 */
	public String getFlags();



	/**
	 * return output file name
	 */
	public String getOutput();

	/**
	 * give command flag to generate output
	 */
	public String getOutputFlag();

	/**
	 * return output prefix
	 */
	public String getOutputPrefix();
}
