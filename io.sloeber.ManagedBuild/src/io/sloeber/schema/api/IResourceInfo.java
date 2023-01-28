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
package io.sloeber.schema.api;

import java.util.List;

import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.core.runtime.IPath;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IResourceInfo extends ISchemaObject {
    //	public static final String PARENT_FOLDER_INFO_ID = "parentFolderInfoId";
    //	public static final String BASE_TOOLCHAIN_ID = "baseToolChainId";
    //	public static final String INHERIT_PARENT_INFO = "inheritParentInfo";					  //$NON-NLS-1$
    public static final String RESOURCE_PATH = "resourcePath"; //$NON-NLS-1$
    public static final String EXCLUDE = "exclude"; //$NON-NLS-1$

    IPath getPath();

    boolean isExcluded();

    IConfiguration getParent();

}
