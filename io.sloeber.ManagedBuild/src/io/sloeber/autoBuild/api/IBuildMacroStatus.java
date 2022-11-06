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

import org.eclipse.cdt.core.cdtvariables.ICdtVariableStatus;

/**
 * This interface represents the status of a build macro operation
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuildMacroStatus extends ICdtVariableStatus {

	/**
	 * returns the context type used in the operation
	 * @return int
	 */
	public int getContextType();

	/**
	 * returns the context data used in the operation
	 * @return Object
	 */
	public Object getContextData();

	public String getMacroName();

}
