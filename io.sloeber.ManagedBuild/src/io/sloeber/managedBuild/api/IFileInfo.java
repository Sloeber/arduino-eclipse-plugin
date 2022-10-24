/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
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
package io.sloeber.managedBuild.api;

import org.eclipse.cdt.core.settings.model.extension.CFileData;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IFileInfo extends IResourceConfiguration {
	public static final String FILE_INFO_ELEMENT_NAME = "fileInfo"; //$NON-NLS-1$

	CFileData getFileData();

}
