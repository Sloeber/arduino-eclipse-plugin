/*******************************************************************************
 * Copyright (c) 2011 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Miwako Tokugawa (Intel Corporation) - initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

/**
 * This interface determines whether or not the option category is currently displayed.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 *
 * @since 8.0
 */
public interface IOptionCategoryApplicability {
	/**
	 * This method is queried whenever a new option category is displayed.
	 *
	 * @param configuration  build configuration of option
	 *                       (may be IConfiguration or IResourceConfiguration)
	 * @param optHolder		contains the holder of the option
	 * @param category         the option category itself
	 *
	 * @return true if this option should be visible in the build options page,
	 *         false otherwise
	 */
	public boolean isOptionCategoryVisible(IBuildObject configuration, IHoldsOptions optHolder,
			IOptionCategory category);
}
