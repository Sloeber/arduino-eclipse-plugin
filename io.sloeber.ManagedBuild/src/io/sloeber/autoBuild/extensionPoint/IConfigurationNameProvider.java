/*******************************************************************************
 * Copyright (c) 2005 Intel Corporation and others.
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

package io.sloeber.autoBuild.extensionPoint;

import io.sloeber.autoBuild.api.IConfiguration;

public interface IConfigurationNameProvider {

	/*
	 * Returns the new  unique configuration name based on the 'configuration'
	 * object and the list of configuration names already in use in the project.
	 *
	 */

	String getNewConfigurationName(IConfiguration configuration, String[] usedConfigurationNames);
}
