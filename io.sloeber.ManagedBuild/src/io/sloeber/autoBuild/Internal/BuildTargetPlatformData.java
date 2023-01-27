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

import io.sloeber.schema.internal.TargetPlatform;

public class BuildTargetPlatformData extends CTargetPlatformData {
    private TargetPlatform fTargetPlatform;

    public BuildTargetPlatformData(TargetPlatform targetPlatform) {
        fTargetPlatform = targetPlatform;
    }

    @Override
    public String[] getBinaryParserIds() {
        return fTargetPlatform.getBinaryParserList();
    }

    @Override
    public void setBinaryParserIds(String[] ids) {
        fTargetPlatform.setBinaryParserList(ids);
    }

    @Override
    public String getId() {
        return fTargetPlatform.getId();
    }

    @Override
    public String getName() {
        return fTargetPlatform.getName();
    }

    @Override
    public boolean isValid() {
        //TODO:
        return true;
    }

    public void setName(String name) {
        fTargetPlatform.myName = (name);
    }

}
