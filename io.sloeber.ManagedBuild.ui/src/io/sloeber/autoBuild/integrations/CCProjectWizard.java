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
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.integrations;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;

/**
 * The wizard to create new MBS C++ Project.
 */
public class CCProjectWizard extends CDTCommonProjectWizard {

    public CCProjectWizard() {
        super(Messages.NewModelProjectWizard_2, Messages.NewModelProjectWizard_3);
    }

    @Override
    public String[] getNatures() {
        return new String[] { CProjectNature.C_NATURE_ID, CCProjectNature.CC_NATURE_ID };
    }

    @Override
    protected IProject continueCreation(IProject prj) {
        SubMonitor subMonitor = SubMonitor.convert(continueCreationMonitor, Messages.CCProjectWizard_0, 2);
        try {
            CProjectNature.addCNature(prj, subMonitor.split(1));
            CCProjectNature.addCCNature(prj, subMonitor.split(1));
        } catch (CoreException e) {
        }
        return prj;
    }

    @Override
    public String[] getContentTypeIDs() {
        return new String[] { CCorePlugin.CONTENT_TYPE_CXXSOURCE, CCorePlugin.CONTENT_TYPE_CXXHEADER };
    }

}
