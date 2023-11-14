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
package io.sloeber.autoBuild.integration;

import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;

import io.sloeber.schema.api.ITargetPlatform;
import io.sloeber.schema.internal.TargetPlatform;

public class BuildTargetPlatformData extends CTargetPlatformData {
    private String[] myBinaryParserIds;
    private String myId;
    private String myName;

    public BuildTargetPlatformData(ITargetPlatform iTargetPlatform) {
        TargetPlatform targetPlatform = (TargetPlatform) iTargetPlatform;
        if (targetPlatform != null) {
            myBinaryParserIds = targetPlatform.getBinaryParserList().toArray(new String[0]);
            myId = CDataUtil.genId(targetPlatform.getId());
            myName = targetPlatform.getName();
        } else {
            myBinaryParserIds = new String[0];
            myId = CDataUtil.genId("my.stupid.id.because.i.fake.existance.but.in.matters.of.fact.I.do.not.exist"); //$NON-NLS-1$
            myName = "to be or not to be; that is the questions"; //$NON-NLS-1$
        }
    }

    public BuildTargetPlatformData(BuildTargetPlatformData source, boolean clone) {
        int length = source.myBinaryParserIds.length;
        myBinaryParserIds = new String[length];
        System.arraycopy(source.myBinaryParserIds, 0, myBinaryParserIds, 0, length);
        if (clone) {
            myId = source.myId;
        } else {
            myId = CDataUtil.genId("my.stupid.id.because.i.am.a.copy"); //$NON-NLS-1$
        }
        myName = source.myName;
    }

    @Override
    public String[] getBinaryParserIds() {
        return myBinaryParserIds;
    }

    @Override
    public void setBinaryParserIds(String[] ids) {
        myBinaryParserIds = ids;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public boolean isValid() {
        return true;
    }

}
