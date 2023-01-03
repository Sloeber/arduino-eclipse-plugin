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
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.core.Messages.*;

import org.eclipse.cdt.core.settings.model.MultiItemsHolder;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
//import org.eclipse.cdt.managedbuilder.core.BuildException;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.OptionStringValue;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.api.OptionStringValue;

/**
 * This class holds a number of IResourceInfo objects
 * belonging to different configurations while they are
 * edited simultaneously.
 */
public abstract class MultiResourceInfo extends MultiItemsHolder implements IResourceInfo {
    private static final int MODE_BOOL = 0;
    private static final int MODE_STR = 1;
    private static final int MODE_SAR = 2;
    private static final int MODE_OSV = 3;
    private static final int MODE_CMDLINE = 4;
    private static final int MODE_COMMAND = 5;

    protected IResourceInfo[] fRis = null;
    protected int curr = 0;
    IConfiguration parent = null;

    public MultiResourceInfo(IResourceInfo[] ris, IConfiguration _parent) {
        fRis = ris;
        parent = _parent;
        for (int i = 0; i < fRis.length; i++) {
            if (!(fRis[i].getParent() instanceof Configuration))
                continue;
            Configuration cfg = (Configuration) fRis[i].getParent();
            if (cfg.getConfigurationDescription().isActive()) {
                curr = i;
                break;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#canExclude(boolean)
     */
    @Override
    public boolean canExclude(boolean exclude) {
        for (int i = 0; i < fRis.length; i++)
            if (!fRis[i].canExclude(exclude))
                return false;
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#getCLanguageDatas()
     */
    @Override
    public CLanguageData[] getCLanguageDatas() {
        return fRis[curr].getCLanguageDatas();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#getKind()
     */
    @Override
    public int getKind() {
        return fRis[curr].getKind();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#getParent()
     */
    @Override
    public IConfiguration getParent() {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#getPath()
     */
    @Override
    public IPath getPath() {
        return fRis[curr].getPath();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#getResourceData()
     */
    @Override
    public CResourceData getResourceData() {
        if (DEBUG)
            System.out.println("Strange call: MultiResourceInfo.getResourceData()"); //$NON-NLS-1$
        return fRis[curr].getResourceData();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#getTools()
     */
    @Override
    public ITool[] getTools() {
        return fRis[curr].getTools();
    }


    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#isExcluded()
     */
    @Override
    public boolean isExcluded() {
        for (int i = 0; i < fRis.length; i++)
            if (fRis[i].isExcluded())
                return true;
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#isSupported()
     */
    @Override
    public boolean isSupported() {
        for (int i = 0; i < fRis.length; i++)
            if (fRis[i].isSupported())
                return true;
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#isValid()
     */
    @Override
    public boolean isValid() {
        for (int i = 0; i < fRis.length; i++)
            if (!fRis[i].isValid())
                return false;
        return true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#needsRebuild()
     */
    @Override
    public boolean needsRebuild() {
        for (int i = 0; i < fRis.length; i++)
            if (fRis[i].needsRebuild())
                return true;
        return false;
    }



    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setExclude(boolean)
     */
    @Override
    public void setExclude(boolean excluded) {
        for (int i = 0; i < fRis.length; i++)
            fRis[i].setExclude(excluded);
    }

    private String getSuperClassId(IOption op) {
        String s = null;
        while (op != null) {
            s = op.getId();
            op = op.getSuperClass();
        }
        return s;
    }







    private IOption setOption(IHoldsOptions parent, IOption option, Object value, int mode) throws BuildException {
        IOption op = null;
        String ext = parent instanceof ITool ? ((ITool) parent).getDefaultInputExtension() : null;

        String sid = getSuperClassId(option);
        for (int i = 0; i < fRis.length; i++) {
            IHoldsOptions[] hos;
            if (parent instanceof ITool)
                hos = fRis[i].getTools();
            else if (parent instanceof IToolChain)
                // If parent is an IToolChain then the resource infos must be at folder level
                hos = new IHoldsOptions[] { ((IFolderInfo) fRis[i]).getToolChain() };
            else // Shouldn't happen
                throw new BuildException(MultiResourceInfo_MultiResourceInfo_UnhandledIHoldsOptionsType);

            for (int j = 0; j < hos.length; j++) {
                if (ext != null && !ext.equals(((ITool) hos[j]).getDefaultInputExtension()))
                    continue;
                IOption op2 = hos[j].getOptionBySuperClassId(sid);
                if (op2 != null) {
                    switch (mode) {
                    case MODE_BOOL:
                        op = fRis[i].setOption(hos[j], op2, ((Boolean) value).booleanValue());
                        break;
                    case MODE_STR:
                        op = fRis[i].setOption(hos[j], op2, (String) value);
                        break;
                    case MODE_SAR:
                        op = fRis[i].setOption(hos[j], op2, (String[]) value);
                        break;
                    case MODE_OSV:
                        op = fRis[i].setOption(hos[j], op2, (OptionStringValue[]) value);
                        break;
                    }
                }
            }
        }
        return op;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setOption(org.eclipse.cdt.managedbuilder.core.IHoldsOptions, org.eclipse.cdt.managedbuilder.core.IOption, boolean)
     */
    @Override
    public IOption setOption(IHoldsOptions parent, IOption option, boolean value) throws BuildException {
        return setOption(parent, option, Boolean.valueOf(value), MODE_BOOL);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setOption(org.eclipse.cdt.managedbuilder.core.IHoldsOptions, org.eclipse.cdt.managedbuilder.core.IOption, java.lang.String)
     */
    @Override
    public IOption setOption(IHoldsOptions parent, IOption option, String value) throws BuildException {
        return setOption(parent, option, value, MODE_STR);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setOption(org.eclipse.cdt.managedbuilder.core.IHoldsOptions, org.eclipse.cdt.managedbuilder.core.IOption, java.lang.String[])
     */
    @Override
    public IOption setOption(IHoldsOptions parent, IOption option, String[] value) throws BuildException {
        return setOption(parent, option, value, MODE_SAR);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setOption(org.eclipse.cdt.managedbuilder.core.IHoldsOptions, org.eclipse.cdt.managedbuilder.core.IOption, org.eclipse.cdt.managedbuilder.core.OptionStringValue[])
     */
    @Override
    public IOption setOption(IHoldsOptions parent, IOption option, OptionStringValue[] value) throws BuildException {
        return setOption(parent, option, value, MODE_OSV);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setPath(org.eclipse.core.runtime.IPath)
     */
    @Override
    public void setPath(IPath path) {
        for (int i = 0; i < fRis.length; i++)
            fRis[i].setPath(path);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#setRebuildState(boolean)
     */
    @Override
    public void setRebuildState(boolean rebuild) {
        for (int i = 0; i < fRis.length; i++)
            fRis[i].setRebuildState(rebuild);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IResourceInfo#supportsBuild(boolean)
     */
    @Override
    public boolean supportsBuild(boolean managed) {
        return fRis[curr].supportsBuild(managed);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getBaseId()
     */
    @Override
    public String getBaseId() {
        return fRis[curr].getBaseId();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getId()
     */
    @Override
    public String getId() {
        return fRis[curr].getId();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getManagedBuildRevision()
     */
    @Override
    public String getManagedBuildRevision() {
        return fRis[curr].getManagedBuildRevision();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getName()
     */
    @Override
    public String getName() {
        return fRis[curr].getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getVersion()
     */
    @Override
    public Version getVersion() {
        return fRis[curr].getVersion();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#setVersion(org.eclipse.core.runtime.PluginVersionIdentifier)
     */
    @Override
    public void setVersion(Version version) {
        for (int i = 0; i < fRis.length; i++)
            fRis[i].setVersion(version);
    }

    @Override
    public Object[] getItems() {
        return fRis;
    }

    public boolean isRoot() {
        return ((ResourceInfo) fRis[curr]).isRoot();
    }

}
