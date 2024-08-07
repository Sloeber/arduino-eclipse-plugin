/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
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

import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.ITool;

public interface ILanguageInfoCalculator {
    String getLanguageName(IResource rcInfo, IAutoBuildConfigurationDescription confDesc, ITool tool, IInputType type);

    String getLanguageId(IResource rcInfo, IAutoBuildConfigurationDescription confDesc, ITool tool, IInputType type);
}
