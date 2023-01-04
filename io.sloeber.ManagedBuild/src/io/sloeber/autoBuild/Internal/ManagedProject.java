/*******************************************************************************
 * Copyright (c) 2004, 2013 Intel Corporation and others.
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
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.internal.core.cdtvariables.StorableCdtVariables;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.core.resources.IResource;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;

public class ManagedProject extends BuildObject
        implements IManagedProject {

    //  Parent and children
    private IProjectType projectType;
    private String projectTypeId;
    private IResource owner;
    //	private List configList;	//  Configurations of this project type
    private Map<String, Configuration> configMap = Collections
            .synchronizedMap(new LinkedHashMap<String, Configuration>());
    //  Miscellaneous
    private boolean isDirty = false;
    private boolean isValid = true;
    private boolean resolved = true;


    /*
     *  C O N S T R U C T O R S
     */

    /* (non-Javadoc)
     * Sets the Eclipse project that owns the Managed Project
     *
     * @param owner
     */
    protected ManagedProject(IResource owner) {
        this.owner = owner;
    }

    /**
     * Create a project instance from the project-type specified in the argument,
     * that is owned by the specified Eclipse project.
     *
     * @param owner
     *            the Eclipse project that owns the Managed Project
     */
    public ManagedProject(IResource owner, IProjectType projectType) {
        // Make the owner of the ProjectType the project resource
        this(owner);

        // Copy the parent's identity
        this.projectType = projectType;
        int id = ManagedBuildManager.getRandomNumber();
        setId(owner.getName() + "." + projectType.getId() + "." + id); //$NON-NLS-1$ //$NON-NLS-2$
        setName(projectType.getName());

        setManagedBuildRevision(projectType.getManagedBuildRevision());

        // Hook me up
        IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(owner);
        buildInfo.setManagedProject(this);
    }

    public ManagedProject(ICProjectDescription des) {
        // Make the owner of the ProjectType the project resource
        this(des.getProject());

        // Copy the parent's identity
        //		this.projectType = projectType;
        int id = ManagedBuildManager.getRandomNumber();
        setId(owner.getName() + "." + des.getId() + "." + id); //$NON-NLS-1$ //$NON-NLS-2$
        setName(des.getName());

        //		setManagedBuildRevision(projectType.getManagedBuildRevision());

        // Hook me up
        //		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(owner);
        //		buildInfo.setManagedProject(this);
        //		setDirty(true);
    }

    /**
     * Create the project instance from project file.
     *
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public ManagedProject(ManagedBuildInfo buildInfo, ICStorageElement element, boolean loadConfigs,
            String managedBuildRevision) {
        this(buildInfo.getOwner());

        setManagedBuildRevision(managedBuildRevision);

        // Initialize from the XML attributes
        if (loadFromProject(element)) {

            // check for migration support.
            boolean isSupportAvailable = projectType != null ? projectType.checkForMigrationSupport() : true;
            if (isSupportAvailable == false) {
                setValid(false);
            }

            if (loadConfigs) {
                // Load children
                StorableCdtVariables vars = null;
                ICStorageElement configElements[] = element.getChildren();
                for (ICStorageElement configElement : configElements) {
                    if (configElement.getName().equals(IConfiguration.CONFIGURATION_ELEMENT_NAME)) {
                        Configuration config = new Configuration(this, configElement, managedBuildRevision, false);
                    } else if (configElement.getName().equals("macros")) { //$NON-NLS-1$
                        vars = new StorableCdtVariables(configElement, false);
                    }

                }

                if (vars != null) {
                    for (Configuration cfg : getConfigurationCollection()) {
                        ((ToolChain) cfg.getToolChain()).addProjectVariables(vars);
                    }
                }
            }
        } else {
            setValid(false);
        }

        // hook me up
        buildInfo.setManagedProject(this);
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /* (non-Javadoc)
     * Initialize the project information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the project information
     */
    protected boolean loadFromProject(ICStorageElement element) {
        // note: id and name are unique, so don't intern them
        // id
        setId(element.getAttribute(IBuildObject.ID));

        // name
        if (element.getAttribute(IBuildObject.NAME) != null) {
            setName(element.getAttribute(IBuildObject.NAME));
        }

        // projectType
        projectTypeId = element.getAttribute(PROJECTTYPE);
        if (projectTypeId != null && projectTypeId.length() > 0) {
            projectType = ManagedBuildManager.getExtensionProjectType(projectTypeId);
            if (projectType == null) {
                return false;
            }
        }

        return true;
    }

    public void serializeProjectInfo(ICStorageElement element) {
        element.setAttribute(IBuildObject.ID, id);

        if (name != null) {
            element.setAttribute(IBuildObject.NAME, name);
        }

        if (projectType != null) {
            element.setAttribute(PROJECTTYPE, projectType.getId());
        }

        // I am clean now
        isDirty = false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#serialize()
     */
    public void serialize(ICStorageElement element, boolean saveChildren) {
        serializeProjectInfo(element);

        if (saveChildren) {
            for (Configuration cfg : getConfigurationCollection()) {
                ICStorageElement configElement = element.createChild(IConfiguration.CONFIGURATION_ELEMENT_NAME);
                cfg.serialize(configElement);
            }
        }
        // Serialize my children

        //		//serialize user-defined macros
        //		if(userDefinedMacros != null){
        //			Element macrosElement = doc.createElement(StorableMacros.MACROS_ELEMENT_NAME);
        //			element.appendChild(macrosElement);
        //			userDefinedMacros.serialize(doc,macrosElement);
        //		}
        //
        //		if(userDefinedEnvironment != null){
        //			EnvironmentVariableProvider.fUserSupplier.storeEnvironment(this,true);
        //		}

        // I am clean now
        isDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#getOwner()
     */
    @Override
    public IResource getOwner() {
        return owner;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#updateOwner(org.eclipse.core.resources.IResource)
     */
    @Override
    public void updateOwner(IResource resource) {
        if (!resource.equals(owner)) {
            // Set the owner correctly
            owner = resource;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#getProjectType()
     */
    //@Override
    public IProjectType getProjectType() {
        return projectType;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedProject#createConfiguration(org.eclipse.cdt.core.build.managed.IConfiguration)
     */
    @Override
    public IConfiguration createConfiguration(IConfiguration parent, String id) {
        Configuration config = new Configuration(this, (Configuration) parent, id, false, false, false);
        ManagedBuildManager.performValueHandlerEvent(config, IManagedOptionValueHandler.EVENT_OPEN);
        return config;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedProject#createConfigurationClone(org.eclipse.cdt.core.build.managed.IConfiguration)
     */
    @Override
    public IConfiguration createConfigurationClone(IConfiguration parent, String id) {
        Configuration config = new Configuration(this, (Configuration) parent, id, true, false, false);
        // Inform all options in the configuration and all its resource configurations
        ManagedBuildManager.performValueHandlerEvent(config, IManagedOptionValueHandler.EVENT_OPEN);
        return config;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedProject#getConfiguration()
     */
    @Override
    public IConfiguration getConfiguration(String id) {
        return configMap.get(id);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#getConfigurations()
     */
    @Override
    public IConfiguration[] getConfigurations() {
        synchronized (configMap) {
            return configMap.values().toArray(new IConfiguration[configMap.size()]);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#removeConfiguration(java.lang.String)
     */
    @Override
    public void removeConfiguration(String id) {
        final String removeId = id;

        //handle the case of temporary configuration
        if (!configMap.containsKey(id))
            return;

        configMap.remove(removeId);
        //
        //		IWorkspaceRunnable remover = new IWorkspaceRunnable() {
        //			public void run(IProgressMonitor monitor) throws CoreException {
        //				// Remove the specified configuration from the list and map
        //				Iterator iter = getConfigurationCollection().iterator();
        //				while (iter.hasNext()) {
        //					 IConfiguration config = (IConfiguration)iter.next();
        //					 if (config.getId().equals(removeId)) {
        //						// TODO:  For now we clean the entire project.  This may be overkill, but
        //						//        it avoids a problem with leaving the configuration output directory
        //					 	//        around and having the outputs try to be used by the makefile generator code.
        //					 	IResource proj = config.getOwner();
        //						IManagedBuildInfo info = null;
        //					 	if (proj instanceof IProject) {
        //							info = ManagedBuildManager.getBuildInfo(proj);
        //					 	}
        //						IConfiguration currentConfig = null;
        //						boolean isCurrent = true;
        //			 			if (info != null) {
        //			 				currentConfig = info.getDefaultConfiguration();
        //			 				if (!currentConfig.getId().equals(removeId)) {
        //			 					info.setDefaultConfiguration(config);
        //			 					isCurrent = false;
        //			 				}
        //			 			}
        //			 			((IProject)proj).build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
        //
        //			 			ManagedBuildManager.performValueHandlerEvent(config,
        //			 					IManagedOptionValueHandler.EVENT_CLOSE);
        //						PropertyManager.getInstance().clearProperties(config);
        ////					 	getConfigurationList().remove(config);
        //						getConfigurationMap().remove(removeId);
        //
        //						if (info != null) {
        //							if (!isCurrent) {
        //			 					info.setDefaultConfiguration(currentConfig);
        //							} else {
        //								// If the current default config is the one being removed, reset the default config
        //								String[] configs = info.getConfigurationNames();
        //								if (configs.length > 0) {
        //									info.setDefaultConfiguration(configs[0]);
        //								}
        //							}
        //			 			}
        //						break;
        //					}
        //				}
        //			}
        //		};
        //		try {
        //			ResourcesPlugin.getWorkspace().run( remover, null );
        //		}
        //		catch( CoreException e ) {}
    }

    /* (non-Javadoc)
     * Adds the Configuration to the Configuration list and map
     *
     * @param Tool
     */
    public void addConfiguration(Configuration configuration) {
        if (!configuration.isTemporary())
            configMap.put(configuration.getId(), configuration);
    }

    /**
     * (non-Javadoc)
     * Safe accessor for the list of configurations.
     *
     * @return List containing the configurations
     */
    private Collection<Configuration> getConfigurationCollection() {
        synchronized (configMap) {
            return new ArrayList<>(configMap.values());
        }
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#(getDefaultArtifactName)
     */
    @Override
    public String getDefaultArtifactName() {
        return CdtVariableResolver.createVariableReference(CdtVariableResolver.VAR_PROJ_NAME);
    }

    /* (non-Javadoc)
     *  Resolve the element IDs to interface references
     */
    public boolean resolveReferences() {
        if (!resolved) {
            resolved = true;
            // Resolve project-type
            if (projectTypeId != null && projectTypeId.length() > 0) {
                projectType = ManagedBuildManager.getExtensionProjectType(projectTypeId);
                if (projectType == null) {
                    return false;
                }
            }

            // call resolve references on any children
            for (Configuration cfg : getConfigurationCollection())
                cfg.resolveReferences();
        }
        return true;
    }

  
    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#isValid()
     */
    @Override
    public boolean isValid() {
        //  TODO:  In the future, children could also have a "valid" state that should be checked
        return isValid;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedProject#setValid(boolean)
     */
    @Override
    public void setValid(boolean isValid) {
        //  TODO:  In the future, children could also have a "valid" state...
        this.isValid = isValid;
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        if (version == null) {
            if (getProjectType() != null) {
                return getProjectType().getVersion();
            }
        }
        return version;
    }

    @Override
    public void setVersion(Version version) {
        // Do nothing
    }

    /*
     * this method is called by the UserDefinedMacroSupplier to obtain user-defined
     * macros available for this managed project
     */
    /*	public StorableMacros getUserDefinedMacros(){
    		if(userDefinedMacros == null)
    			userDefinedMacros = new StorableMacros();
    		return userDefinedMacros;
    	}
    */
    //	public StorableEnvironment getUserDefinedEnvironmet(){
    //		return userDefinedEnvironment;
    //	}
    //
    //	public void setUserDefinedEnvironmet(StorableEnvironment env){
    //		userDefinedEnvironment = env;
    //	}

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.internal.core.BuildObject#updateManagedBuildRevision(java.lang.String)
     */
    @Override
    public void updateManagedBuildRevision(String revision) {
        super.updateManagedBuildRevision(revision);
        for (Configuration cfg : getConfigurationCollection()) {
            cfg.updateManagedBuildRevision(revision);
        }
    }

    public void setProjectType(IProjectType projectType) {
        if (this.projectType != projectType) {
            this.projectType = projectType;
            if (this.projectType == null) {
                projectTypeId = null;
            } else {
                projectTypeId = this.projectType.getId();
            }
        }
    }

    public void applyConfiguration(Configuration cfg) {
        cfg.applyToManagedProject(this);
    }


	@Override
	public void resolveFields() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resolveSuperClass() throws Exception {
		// TODO Auto-generated method stub
		
	}
}
