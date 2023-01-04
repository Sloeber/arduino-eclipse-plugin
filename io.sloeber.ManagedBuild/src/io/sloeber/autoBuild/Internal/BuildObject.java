/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;

public abstract class BuildObject implements IBuildObject {

    protected String id;
    protected String name;

    protected Version version = null;
    protected String managedBuildRevision = null;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString() {
        if (name != null) {
            return name;
        } else {
            return "id=" + id;
        }
    }

    /**
     * @return Returns the managedBuildRevision.
     */
    @Override
    public String getManagedBuildRevision() {
        return managedBuildRevision;
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        return version;
    }

    /**
     * @param version
     *            The version to set.
     */
    @Override
    public void setVersion(Version version) {
        this.version = version;
    }


    public Version getVersionFromId() {
        String versionNumber = ManagedBuildManager.getVersionFromIdAndVersion(getId());

        if (versionNumber == null) {
            // It means, Tool Integrator either not provided version information in 'id' or  provided in wrong format,
            // So get the default version based on 'managedBuildRevision' attribute.

            if (getManagedBuildRevision() != null) {
                Version tmpManagedBuildRevision = new Version(getManagedBuildRevision());
                if (tmpManagedBuildRevision.equals(new Version("1.2.0"))) //$NON-NLS-1$
                    versionNumber = "0.0.1"; //$NON-NLS-1$
                else if (tmpManagedBuildRevision.equals(new Version("2.0.0"))) //$NON-NLS-1$
                    versionNumber = "0.0.2"; //$NON-NLS-1$
                else if (tmpManagedBuildRevision.equals(new Version("2.1.0"))) //$NON-NLS-1$
                    versionNumber = "0.0.3"; //$NON-NLS-1$
                else
                    versionNumber = "0.0.4"; //$NON-NLS-1$
            } else {
                versionNumber = "0.0.0"; //$NON-NLS-1$
            }
        }
        return new Version(versionNumber);
    }

    public void setManagedBuildRevision(String managedBuildRevision) {
        this.managedBuildRevision = managedBuildRevision;
    }

    /**
     * updates revision for this build object and all its children
     */
    public void updateManagedBuildRevision(String revision) {
        setManagedBuildRevision(revision);
        setVersion(getVersionFromId());
    }
    

}
