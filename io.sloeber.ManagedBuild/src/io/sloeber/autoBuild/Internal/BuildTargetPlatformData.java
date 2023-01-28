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
package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import io.sloeber.schema.api.ITargetPlatform;
import io.sloeber.schema.internal.TargetPlatform;

public class BuildTargetPlatformData extends CTargetPlatformData {
    private TargetPlatform fTargetPlatform;
    private String[] myBinaryParserIds;
    private String myId;
    private String myName;
    private boolean myIsValid;

    public BuildTargetPlatformData(ITargetPlatform iTargetPlatform) {
        fTargetPlatform = (TargetPlatform) iTargetPlatform;
        if (fTargetPlatform != null) {
            myBinaryParserIds = fTargetPlatform.getBinaryParserList().toArray(new String[0]);
            myId = fTargetPlatform.getId();
            myName = fTargetPlatform.getName();
            myIsValid = true;
        } else {
            myBinaryParserIds = new String[0];
            myId = "my.stupid.id.because.i.fake.existance.but.in.matters.of.fatc.I.do.not.exist"; //$NON-NLS-1$
            myName = "to be or not to be; that is the questions"; //$NON-NLS-1$
            myIsValid = false;
        }
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
        return myIsValid;
    }

    public void setName(String name) {
        myName = name;
    }

}
