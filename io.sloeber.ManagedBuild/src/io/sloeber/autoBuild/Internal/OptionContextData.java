/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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
 * Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.List;

import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFileInfo;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.IBuildObject;

/**
 * This is a trivial implementation of the IOptionContextData used internally by
 * the MBS
 *
 * @since 3.0
 */
public class OptionContextData implements IOptionContextData {
    private IOption fOption;
    private IOptionCategory fCategory;
    private IBuildObject fParent;

    public OptionContextData(IOption option, IBuildObject parent) {
        fOption = option;
        fParent = parent;
    }

    /*
     * @since 8.0
     */
    public OptionContextData(IOptionCategory category, IBuildObject parent) {
        fCategory = category;
        fParent = parent;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IOptionContextData#getOption()
     */
    @Override
    public IOption getOption() {
        return fOption;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IOptionContextData#getOptionCategory()
     */
    @Override
    public IOptionCategory getOptionCategory() {
        return fCategory;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IOptionContextData#getParent()
     */
    @Override
    public IBuildObject getParent() {
        return fParent;
    }

    public static IHoldsOptions getHolder(IOptionContextData data) {
        IBuildObject buildObj = data.getParent();
        IToolChain tCh = null;
        IHoldsOptions ho = null;
        IResourceInfo rcInfo = null;
        IFileInfo fileInfo = null;
        IFolderInfo folderInfo = null;
        if (buildObj instanceof ITool)
            ho = (ITool) buildObj;
        else if (buildObj instanceof IToolChain)
            tCh = (IToolChain) buildObj;
        else if (buildObj instanceof IFileInfo) {
            fileInfo = (IFileInfo) buildObj;
            rcInfo = fileInfo;
        } else if (buildObj instanceof IConfiguration)
            tCh = ((IConfiguration) buildObj).getToolChain();
        else if (buildObj instanceof IFolderInfo) {
            folderInfo = (IFolderInfo) buildObj;
           // rcInfo = folderInfo;
            tCh = folderInfo.getToolChain();
        }

        if (ho == null) {
            IOption option = data.getOption();
            if (option == null)
                return null;

            IHoldsOptions tmp = option.getOptionHolder();

            List<ITool> tools = null;
//            if (tCh != null) {
//                for (IToolChain cur = tCh; cur != null; cur = cur.getSuperClass()) {
//                    if (cur == tmp)
//                        return tCh;
//                }
//                tools = tCh.getTools();
//            } else 
            	if (rcInfo != null) {
                tools = rcInfo.getTools();
            }

//            if (tools != null) {
//                for (int i = 0; i < tools.length; i++) {
//                    for (ITool cur = tools[i]; cur != null; cur = cur.getSuperClass()) {
//                        if (cur == tmp) {
//                            ITool tool = tools[i];
//                            if (!tool.isExtensionElement() && tool.getParent() != null) {
//                                ho = tools[i];
//                                break;
//                            }
//                        }
//                    }
//                }
//            }

            if (ho == null && tmp != null) {
                if (tmp instanceof ITool) {
                    ITool tool = (ITool) tmp;
                    if (!tool.isExtensionElement() && tool.getParent() != null) {
                        ho = tmp;
                    }
                } else if (tmp instanceof IToolChain) {
                    IToolChain tChain = (IToolChain) tmp;
                    if ( tChain.getParent() != null) {
                        ho = tmp;
                    }
                }
            }
        }
        return ho;
    }
}
