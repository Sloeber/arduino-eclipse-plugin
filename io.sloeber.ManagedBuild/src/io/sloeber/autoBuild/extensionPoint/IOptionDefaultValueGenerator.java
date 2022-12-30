/*******************************************************************************
 * Copyright (c) 2017 STMicroelectronics and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     cartu38 opendev (STMicroelectronics) - [514385] Custom defaultValue-generator support
 *******************************************************************************/

package io.sloeber.autoBuild.extensionPoint;

import io.sloeber.autoBuild.api.IOption;

/**
 * This interface can be implemented by clients to contribute custom defaultValue-generator for a
 * build-option.
 *
 * The custom defaultValue-generator class should be referenced in the <option>/defaultValueGenerator
 * attribute of the org.eclipse.cdt.managedbuilder.core.buildDefinitions extension-point.
 *
 * @since 8.5
 */
public interface IOptionDefaultValueGenerator {
	/**
	 * Generate the defaultValue for the given option.
	 *
	 * @param option
	 *            the underlying build-option
	 * @return the generated build-option defaultValue. May return {@code null} to fall back to the default
	 *         defaultValue generation logic.
	 */
	Object generateDefaultValue(IOption option);

}
