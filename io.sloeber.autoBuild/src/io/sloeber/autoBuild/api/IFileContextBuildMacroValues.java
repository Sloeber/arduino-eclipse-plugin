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
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IFileContextBuildMacroValues {
	public static final String PREFIX = "macro"; //$NON-NLS-1$
	public static final String SUFFIX = "Value"; //$NON-NLS-1$

	/**
	 *
	 * @return the array if strings representing the names of file context macros supported
	 * by the builder
	 */
	String[] getSupportedMacros();

	/**
	 *
	 * @return the file context macro value for the given macro name
	 */
	String getMacroValue(String macroName);

}
