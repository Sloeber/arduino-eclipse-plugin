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

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IProjectType;

public abstract class BuildObject implements IBuildObject {

    protected String id;
    public String name;
    protected String mySuperClassID;
    protected IConfigurationElement myConfigurationSuperClassElement;
    protected IConfigurationElement myConfigurationElement;
    protected ICStorageElement myStorageElement;
    protected ICStorageElement myStorageSuperClassElement;

    public static int ORIGINAL = 0;
    public static int SUPER = 1;

    protected Version version = null;
    protected String managedBuildRevision = null;

    protected void loadNameAndID(IExtensionPoint root, IConfigurationElement element) {
        myConfigurationElement = element;
        id = element.getAttribute(ID);
        name = element.getAttribute(NAME);
        mySuperClassID = element.getAttribute(SUPERCLASS);
        myConfigurationSuperClassElement = getSuperClassConfigurationElement();
        if (myConfigurationSuperClassElement == null) {
            ManagedBuildManager.outputResolveError("superClass", //$NON-NLS-1$
                    mySuperClassID, "option", //$NON-NLS-1$
                    getId());
        }
    }

    protected void loadNameAndID(ICStorageElement element) {
        myStorageElement = element;
        id = element.getAttribute(IBuildObject.ID);
        name = element.getAttribute(IBuildObject.NAME);
        // superClass
        mySuperClassID = element.getAttribute(SUPERCLASS);
        if (mySuperClassID != null && mySuperClassID.length() > 0) {
            myStorageSuperClassElement = getSuperClassSorageElement();
            if (myStorageSuperClassElement == null) {
                ManagedBuildManager.outputResolveError(SUPERCLASS, mySuperClassID, name, //$NON-NLS-1$
                        getId());
            }
        }
    }

    protected String[] getAttributes(String attributeName) {
        String[] ret = new String[2];
        ret[SUPER] = ret[ORIGINAL] = myConfigurationElement.getAttribute(attributeName);
        if (ret[SUPER] == null && myConfigurationSuperClassElement != null) {
            ret[SUPER] = myConfigurationSuperClassElement.getAttribute(attributeName);
        }
        if (ret[SUPER] == null) {
            ret[SUPER] = EMPTY;
        }
        if (ret[ORIGINAL] == null) {
            ret[ORIGINAL] = EMPTY;
        }
        return ret;
    }
    
    protected Object createExecutableExtension(String attributeName) {
    	try {
    		if(myConfigurationElement.getAttribute(attributeName)!=null) {
			return myConfigurationElement.createExecutableExtension(attributeName);
    		}
    		if(myConfigurationSuperClassElement!=null && myConfigurationSuperClassElement.getAttribute(attributeName)!=null) {
			return myConfigurationSuperClassElement.createExecutableExtension(attributeName);
    		}

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
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

    private IConfigurationElement getSuperClassConfigurationElement() {
        // TODO Auto-generated method stub
        return null;
    }

    private ICStorageElement getSuperClassSorageElement() {
        // TODO Auto-generated method stub
        return null;
    }
}
