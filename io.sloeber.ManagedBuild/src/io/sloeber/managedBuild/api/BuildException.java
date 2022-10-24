/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package io.sloeber.managedBuild.api;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
public class BuildException extends Exception {

	public static final int BUILD_FAILED = -1;

	public BuildException(String msg) {
		super(msg);
	}

}
