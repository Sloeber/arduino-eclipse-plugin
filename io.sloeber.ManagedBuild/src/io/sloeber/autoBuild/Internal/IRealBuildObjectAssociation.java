/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
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

import io.sloeber.autoBuild.api.IBuildObject;

//import org.eclipse.cdt.managedbuilder.core.IBuildObject;

public interface IRealBuildObjectAssociation extends IBuildObject {
    int OBJECT_TOOLCHAIN = 1;
    int OBJECT_TOOL = 1 << 1;
    int OBJECT_BUILDER = 1 << 2;
    int OBJECT_FILE_INFO = 1 << 3;
    int OBJECT_FOLDER_INFO = 1 << 4;
    int OBJECT_CONFIGURATION = 1 << 5;

    int getType();

    IRealBuildObjectAssociation getRealBuildObject();

    IRealBuildObjectAssociation getExtensionObject();

    boolean isRealBuildObject();

    boolean isExtensionBuildObject();

    IRealBuildObjectAssociation[] getIdenticBuildObjects();

    IRealBuildObjectAssociation getSuperClassObject();

    String getUniqueRealName();
}
