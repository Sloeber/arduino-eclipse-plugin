package io.sloeber.autoBuild.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CFileData;
import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.Option;

public class AutoBuildConfigurationDescription {
    IConfiguration myAutoBuildConfiguration;
    IProject myProject;

    Map<IResource, List<Option>> myOptions = new HashMap<>();

    public AutoBuildConfigurationDescription(Configuration config, IProject project) {
        myAutoBuildConfiguration = config;
        myProject = project;
    }

    static public AutoBuildConfigurationDescription getAutoBuildConfigurationDescription(
            IConfiguration autoBuildConfiguration, IProject project) {
        return null;
    }

    public IConfiguration getAutoBuildConfiguration() {
        return myAutoBuildConfiguration;
    }

    public IProject getProject() {
        return myProject;
    }

    public List<Option> getAllOptions(IResource resource) {
        //TOFIX this will only return options for the resource but not for the parent IFolders
        //and also not for the project
        return myOptions.get(resource);
    }

    public List<Option> getOptions(IResource resource) {
        return myOptions.get(resource);
    }

}
