/*******************************************************************************
 * Copyright (c) 2007, 2016 Intel Corporation and others.
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
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.schema.internal;

import java.util.List;
import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.ResourceInfoContainer;
import io.sloeber.autoBuild.Internal.ToolChainModificationHelper;
import io.sloeber.autoBuild.Internal.ToolListModificationInfo;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFileInfo;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IResourceConfiguration;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;

public abstract class ResourceInfo extends BuildObject implements IResourceInfo {
	private Configuration config;
	private IPath path;
	boolean isDirty;
	boolean needsRebuild;
	private ResourceInfoContainer rcInfo;
	private CResourceData resourceData;
	private boolean excluded = false;

	ResourceInfo(IConfiguration cfg, IConfigurationElement element, boolean hasBody) {
		config = (Configuration) cfg;
		if (hasBody)
			loadFromManifest(element);
	}

	ResourceInfo(IConfiguration cfg, ResourceInfo base, String id_in) {
		config = (Configuration) cfg;
		path = normalizePath(base.path);

		id = id_in;
		name = base.getName();

		if (id.equals(base.getId())) {
			isDirty = base.isDirty;
			needsRebuild = base.needsRebuild;
		} else {
			needsRebuild = true;
			isDirty = true;
		}
	}

	public boolean isRoot() {
		return path.segmentCount() == 0;
	}

	ResourceInfo(IConfiguration cfg, IPath path, String newID, String newName) {
		config = (Configuration) cfg;
		path = normalizePath(path);
		this.path = path;

		id = newID;
		name = newName;
	}

	ResourceInfo(IFileInfo base, IPath path, String newID, String newName) {
		config = (Configuration) base.getParent();

		id = newID;
		name = newName;
		path = normalizePath(path);

		this.path = path;
		needsRebuild = true;
		isDirty = true;
	}

	ResourceInfo(FolderInfo base, IPath path, String newID, String newName) {
		config =  base.getConfiguration();

		id = newID;
		name = newName;
		path = normalizePath(path);

		this.path = path;
		needsRebuild = true;
		isDirty = true;
	}

	ResourceInfo(IConfiguration cfg, ICStorageElement element, boolean hasBody) {
		config = (Configuration) cfg;
		if (hasBody)
			loadFromProject(element);
	}

	private void loadFromManifest(IConfigurationElement element) {

		// id
		id = (SafeStringInterner.safeIntern(element.getAttribute(ID)));

		// Get the name
		name = (SafeStringInterner.safeIntern(element.getAttribute(NAME)));

		// resourcePath
		String tmp = element.getAttribute(RESOURCE_PATH);
		if (tmp != null) {
			path = new Path(tmp);
			if (IResourceConfiguration.RESOURCE_CONFIGURATION_ELEMENT_NAME.equals(element.getName())) {
				path = path.removeFirstSegments(1);
			}
			path = normalizePath(path);
		} else {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					"ResourceInfo.loadFromManifest() : resourcePath=NULL", null); //$NON-NLS-1$
			Activator.log(status);
		}

		// exclude
//        String excludeStr = element.getAttribute(EXCLUDE);
//        if (excludeStr != null) {
//            config.setExcluded(getPath(), isFolderInfo(), (Boolean.parseBoolean(excludeStr)));
//        }
	}

	private void loadFromProject(ICStorageElement element) {

		// id (unique, do not intern)
		id = (element.getAttribute(ID));

		// name
		if (element.getAttribute(NAME) != null) {
			name = (SafeStringInterner.safeIntern(element.getAttribute(NAME)));
		}

		// resourcePath
		if (element.getAttribute(RESOURCE_PATH) != null) {
			String tmp = element.getAttribute(RESOURCE_PATH);
			if (tmp != null) {
				path = new Path(tmp);
				if (IResourceConfiguration.RESOURCE_CONFIGURATION_ELEMENT_NAME.equals(element.getName())) {
					path = path.removeFirstSegments(1);
				}
				path = normalizePath(path);
			} else {
				Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						"ResourceInfo.loadFromProject() : resourcePath=NULL", null); //$NON-NLS-1$
				Activator.log(status);
			}
		}

//        // exclude
//        if (element.getAttribute(EXCLUDE) != null) {
//            String excludeStr = element.getAttribute(EXCLUDE);
//            if (excludeStr != null) {
//                config.setExcluded(getPath(), isFolderInfo(), (Boolean.parseBoolean(excludeStr)));
//            }
//        }
	}

	@Override
	public IConfiguration getParent() {
		return config;
	}

	@Override
	public IPath getPath() {
		return normalizePath(path);
	}

	@Override
	public boolean isExcluded() {
		return false; // TOFIX jaba config.isExcluded(getPath());
	}

	@Override
	public boolean canExclude(boolean exclude) {
		return true; // tofix jaba config.canExclude(getPath(), isFolderInfo(), exclude);
	}

	public abstract boolean isFolderInfo();


	@Override
	public CResourceData getResourceData() {
		return resourceData;
	}

	protected void setResourceData(CResourceData data) {
		resourceData = data;
	}

	void removed() {
		config = null;
	}

	@Override
	public boolean isValid() {
		return config != null;
	}

	public ITool getToolById(String id) {
		List<ITool> tools = getTools();
		for (ITool tool : tools) {
			if (id.equals(tool.getId()))
				return tool;
		}
		return null;
	}

	public static IPath normalizePath(IPath path) {
		if (path == null)
			return null;
		return path.makeRelative();
	}



//	abstract void resolveProjectReferences(boolean onLoad);

	abstract public boolean hasCustomSettings();

	public ToolListModificationInfo getToolListModificationInfo(List<ITool> tools) {
		List<ITool> curTools = getTools();
		return ToolChainModificationHelper.getModificationInfo(this, curTools, tools);
	}

	static ITool[][] getRealPairs(ITool[] tools) {
		ITool[][] pairs = new ITool[tools.length][];
		for (int i = 0; i < tools.length; i++) {
			ITool[] pair = new ITool[2];
			pair[0] = ManagedBuildManager.getRealTool(tools[i]);
			if (pair[0] == null)
				pair[0] = tools[i];
			pair[1] = tools[i];
			pairs[i] = pair;
		}
		return pairs;
	}



}
