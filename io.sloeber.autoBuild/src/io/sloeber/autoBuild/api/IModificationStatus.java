/*******************************************************************************
 * Copyright (c) 2007, 2011 Intel Corporation and others.
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

import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;

import io.sloeber.schema.api.ITool;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IModificationStatus extends IStatus {
	/**
	 * flags should be obtained via {@link IStatus#getCode()}
	 */

	public static final int TOOLS_CONFLICT = 1;
	public static final int PROPS_NOT_SUPPORTED = 1 << 1;
	public static final int REQUIRED_PROPS_NOT_SUPPORTED = 1 << 2;

	/**
	 * some properties used in the toolChain are not defined in the System
	 */
	public static final int PROPS_NOT_DEFINED = 1 << 3;

	/**
	 * some tools do not support Managed Build Mode
	 */
	public static final int TOOLS_DONT_SUPPORT_MANAGED_BUILD = 1 << 4;

	/**
	 *
	 * @return Map containing property Id to property Value associations.
	 * If value is not null then the given value is not supported
	 * If Value is not null then the fiven property is not supported
	 */
	Map<String, String> getUnsupportedProperties();

	/**
	 *
	 * @return Map containing property Id to property Value associations.
	 * If value is not null then the given value is not supported
	 * If Value is not null then the fiven property is not supported
	 */
	Map<String, String> getUnsupportedRequiredProperties();

	/**
	 *
	 * @return Set containing undefined property IDs
	 */
	Set<String> getUndefinedProperties();

	ITool[][] getToolsConflicts();

	ITool[] getNonManagedBuildTools();
}
