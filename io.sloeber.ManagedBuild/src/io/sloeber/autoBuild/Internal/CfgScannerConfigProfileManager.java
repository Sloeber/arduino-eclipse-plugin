/*******************************************************************************
 * Copyright (c) 2007, 2009 Intel Corporation and others.
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

import org.eclipse.cdt.make.core.scannerconfig.InfoContext;
import org.eclipse.core.resources.IProject;

import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IManagedBuildInfo;

public class CfgScannerConfigProfileManager {

    public static ICfgScannerConfigBuilderInfo2Set getCfgScannerConfigBuildInfo(IConfiguration cfg) {
        return CfgScannerConfigInfoFactory2.create(cfg);
    }

    public static boolean isPerFileProfile(String profileId) {
        //TOFIX		ScannerConfigProfile profile = ScannerConfigProfileManager.getInstance().getSCProfileConfiguration(profileId);
        //		ScannerConfigScope scope = profile.getProfileScope();
        //		return scope != null && scope.equals(ScannerConfigScope.FILE_SCOPE);
        return false;
    }

    public static InfoContext createDefaultContext(IProject project) {
        IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project);
        IConfiguration cfg = null;
        if (info != null && info.isValid()) {
            cfg = info.getDefaultConfiguration();
        }

        if (cfg != null)
            return new CfgInfoContext(cfg).toInfoContext();
        return new InfoContext(project);
    }
}
