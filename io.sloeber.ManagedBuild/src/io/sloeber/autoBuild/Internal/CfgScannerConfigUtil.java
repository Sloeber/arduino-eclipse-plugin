/*******************************************************************************
 * Copyright (c) 2007, 2020 Intel Corporation and others.
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
 * Sergei Kovalchuk (NXP)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Assert;

import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;

public class CfgScannerConfigUtil {


    public static String getFirstProfileId(List<ITool> tools) {
        String id = null;
        for (ITool tool: tools) {
            IInputType[] types = tool.getInputTypes();

            if (types.length != 0) {
                for (int k = 0; k < types.length; k++) {
                    id = types[k].getDiscoveryProfileId(tool);
                    if (id != null)
                        break;
                }
            } else {
                id = ((Tool) tool).getDiscoveryProfileId();
            }

            if (id != null)
                break;
        }

        return id;
    }

    /**
     * Search for toolchain's discovery profiles. Discovery profiles could be
     * specified on toolchain level, input types level or in their super-classes.
     *
     * @param toolchain
     *            - toolchain to search for scanner discovery profiles.
     * @return all available discovery profiles in given toolchain
     */
    public static Set<String> getAllScannerDiscoveryProfileIds(IToolChain toolchain) {
        Assert.isNotNull(toolchain);

        Set<String> profiles = new TreeSet<>();

        if (toolchain != null) {
            String toolchainProfileId = null;
            if (toolchain instanceof ToolChain) {
                // still allow a user a choice to select any legacy profiles
                toolchainProfileId = ((ToolChain) toolchain).getLegacyScannerConfigDiscoveryProfileId();
            } else {
                toolchainProfileId = toolchain.getScannerConfigDiscoveryProfileId();
            }
            if (toolchainProfileId != null && toolchainProfileId.length() > 0) {
                profiles.add(toolchainProfileId);
            }
            List<ITool> tools = toolchain.getTools();
            for (ITool tool : tools) {
                profiles.addAll(getAllScannerDiscoveryProfileIds(tool));
            }
//            IToolChain superClass = toolchain.getSuperClass();
//            if (superClass != null) {
//                profiles.addAll(getAllScannerDiscoveryProfileIds(superClass));
//            }
        }

        return profiles;
    }

    /**
     * Search for tool's discovery profiles. Discovery profiles could be retrieved
     * from tool/input type super-class. Input type could hold list of profiles
     * separated by pipe character '|'.
     *
     * @param tool
     *            - tool to search for scanner discovery profiles
     * @return all available discovery profiles in given configuration
     */
    public static Set<String> getAllScannerDiscoveryProfileIds(ITool tool) {
        Assert.isNotNull(tool);

        if (!(tool instanceof Tool)) {
            //TOFIX String msg = MessageFormat.format(ManagedMakeMessages.getString("CfgScannerConfigUtil_ErrorNotSupported"), //$NON-NLS-1$
            //        new Object[] { Tool.class.getName() });
            String msg = "CfgScannerConfigUtil_ErrorNotSupported"; //$NON-NLS-1$
            throw new UnsupportedOperationException(msg);
        }

        Set<String> profiles = new TreeSet<>();

        for (IInputType inputType : ((Tool) tool).getAllInputTypes()) {
            for (String profileId : getAllScannerDiscoveryProfileIds(inputType)) {
                profiles.add(profileId);
            }
        }

//        ITool superClass = tool.getSuperClass();
//        if (superClass != null) {
//            profiles.addAll(getAllScannerDiscoveryProfileIds(superClass));
//        }
        return profiles;
    }

    /**
     * Search for input type's discovery profiles. Discovery profiles could be
     * specified
     * on input type super-class. Input type could hold list of profiles
     * separated by pipe character '|'.
     *
     * @param inputType
     *            - input type to search for scanner discovery profiles
     * @return all available discovery profiles in given configuration
     */
    private static Set<String> getAllScannerDiscoveryProfileIds(IInputType inputType) {
        Assert.isNotNull(inputType);

        if (!(inputType instanceof InputType)) {
            //TOFIX String msg = MessageFormat.format(ManagedMakeMessages.getString("CfgScannerConfigUtil_ErrorNotSupported"), //$NON-NLS-1$
            //new Object[] { InputType.class.getName() });
            String msg = "CfgScannerConfigUtil_ErrorNotSupported"; //$NON-NLS-1$
            throw new UnsupportedOperationException(msg);
        }

        Set<String> profiles = new TreeSet<>();

        String attribute = ((InputType) inputType).getLegacyDiscoveryProfileIdAttribute();
        if (attribute != null) {
            // FIXME: temporary; we should add new method to IInputType instead of that
            for (String profileId : attribute.split("\\|")) { //$NON-NLS-1$
                profiles.add(profileId);
            }
        }
        IInputType superClass = inputType.getSuperClass();
        if (superClass != null) {
            profiles.addAll(getAllScannerDiscoveryProfileIds(superClass));
        }

        return profiles;
    }
}
