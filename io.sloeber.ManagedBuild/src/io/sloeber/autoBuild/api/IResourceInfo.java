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

import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.core.runtime.IPath;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IResourceInfo extends IBuildObject {
	//	public static final String PARENT_FOLDER_INFO_ID = "parentFolderInfoId";
	//	public static final String BASE_TOOLCHAIN_ID = "baseToolChainId";
	//	public static final String INHERIT_PARENT_INFO = "inheritParentInfo";					  //$NON-NLS-1$
	public static final String RESOURCE_PATH = "resourcePath"; //$NON-NLS-1$
	public static final String EXCLUDE = "exclude"; //$NON-NLS-1$

	IPath getPath();

	void setPath(IPath path);

	boolean isExcluded();

	boolean isExtensionElement();

	void setExclude(boolean excluded);

	boolean canExclude(boolean exclude);

	boolean isDirty();

	boolean needsRebuild();

	void setDirty(boolean dirty);

	void setRebuildState(boolean rebuild);

	int getKind();

	IConfiguration getParent();

	//	IFolderInfo getParentFolderInfo();

	//	IToolChain getBaseToolChain();

	CResourceData getResourceData();

	boolean isValid();

	CLanguageData[] getCLanguageDatas();

	ITool[] getTools();

	//	boolean isParentInfoInherited();

	boolean supportsBuild(boolean managed);

	/**
	 * Sets the value of a boolean option for this resource configuration.
	 *
	 * @param parent The holder/parent of the option.
	 * @param option The option to change.
	 * @param value The value to apply to the option.
	 *
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 */
	public IOption setOption(IHoldsOptions parent, IOption option, boolean value) throws BuildException;

	/**
	 * Sets the value of a string option for this resource configuration.
	 *
	 * @param parent The holder/parent of the option.
	 * @param option The option that will be effected by change.
	 * @param value The value to apply to the option.
	 *
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 */
	public IOption setOption(IHoldsOptions parent, IOption option, String value) throws BuildException;

	/**
	 * Sets the value of a list option for this resource configuration.
	 *
	 * @param parent The holder/parent of the option.
	 * @param option The option to change.
	 * @param value The values to apply to the option.
	 *
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 */
	public IOption setOption(IHoldsOptions parent, IOption option, String[] value) throws BuildException;

	public IOption setOption(IHoldsOptions parent, IOption option, OptionStringValue[] value) throws BuildException;

	boolean isSupported();
}
