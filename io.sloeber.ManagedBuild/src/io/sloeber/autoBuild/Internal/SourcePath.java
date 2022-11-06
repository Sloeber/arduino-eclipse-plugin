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

import org.eclipse.cdt.core.settings.model.ICStorageElement;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.IManagedConfigElement;

class SourcePath {
    static final String ELEMENT_NAME = "sourcePath"; //$NON-NLS-1$
    private static final String ATTRIBUTE_PATH = "path"; //$NON-NLS-1$
    private IPath path;

    SourcePath(IPath path) {
        this.path = path;
    }

    SourcePath(ICStorageElement el) {
        String pathStr = el.getAttribute(ATTRIBUTE_PATH);
        if (pathStr != null)
            path = new Path(pathStr);
    }

    SourcePath(IManagedConfigElement el) {
        String pathStr = el.getAttribute(ATTRIBUTE_PATH);
        if (pathStr != null)
            path = new Path(pathStr);
    }

    public IPath getPath() {
        return path;
    }

    void serialize(ICStorageElement el) {
        if (path != null) {
            String strPath = path.toString();
            el.setAttribute(ATTRIBUTE_PATH, strPath);
        }
    }
}
