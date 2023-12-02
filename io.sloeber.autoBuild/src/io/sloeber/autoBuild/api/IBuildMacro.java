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

import org.eclipse.cdt.core.cdtvariables.ICdtVariable;

/**
 * This interface represents the given build macro
 * Clients may implement or extend this interface.
 *
 * @since 3.0
 */
public interface IBuildMacro extends ICdtVariable {
	int getMacroValueType();

	/**
	 * @throws BuildMacroException if macro holds StringList-type value
	 */
	@Override
	String getStringValue() throws BuildMacroException;

	/**
	 * @throws BuildMacroException if macro holds single String-type value
	 */
	@Override
	String[] getStringListValue() throws BuildMacroException;

}
