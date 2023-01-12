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
package io.sloeber.autoBuild.api;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.Internal.Configuration;
import io.sloeber.autoBuild.Internal.ToolChain;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IFolderInfo extends IBuildObject {
    public final static String FOLDER_INFO_ELEMENT_NAME = "folderInfo"; //$NON-NLS-1$
    public final static String RESOURCEPATH = "resourcePath"; //$NON-NLS-1$
    public final static String EXCLUDE = "exclude"; //$NON-NLS-1$

    boolean isExcluded(IFile file);

	public Configuration getConfiguration() ;

	public ToolChain getToolChain() ;
	public IPath getPath();
}
