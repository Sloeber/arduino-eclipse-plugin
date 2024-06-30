package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.api.IToolChain;
import io.sloeber.autoBuild.schema.internal.Tool;

/*
 * the mySelectedOptions works as follows: The Map<String, String> is the
 * optionID, valueID. In other words the selected value for a option
 * Map<IResource, Map<String, String>> adds the resource. The resource can not
 * be null. In most cases the resource will be a IProject (in other words valid
 * for all resources in the project) Map<ITool, Map<IResource, Map<String,
 * String>>> adds the tool. tool can be null. These are the options at the level
 * of the toolchain/configuration .... When the tool is null I would expect the
 * IResource to be a IProject When the tool is not null this option is only
 * valid when we deal with this tool
 *
 */
public class AutoBuildOptions {

    private Map<IResource, Map<IOption, String>> myDefaultOptions = new HashMap<>();
    private Map<IResource, Map<IOption, String>> mySelectedOptions = new HashMap<>();
    private Map<IResource, Map<IOption, String>> myCombinedOptions = new HashMap<>();


    public AutoBuildOptions() {
    }


    public AutoBuildOptions(AutoBuildOptions base) {
        options_copy(base.mySelectedOptions, mySelectedOptions);
        options_copy(base.myDefaultOptions, myDefaultOptions);
        options_copy(base.myCombinedOptions, myCombinedOptions);
    }

    public AutoBuildOptions(AutoBuildConfigurationDescription autoDesc, KeyValueTree keyValues) {
        IProject project =autoDesc.getProject();
        IProjectType projectType=autoDesc.getProjectType();
        KeyValueTree optionsKeyValue = keyValues.getChild(OPTION);
        for (KeyValueTree curResourceProp : optionsKeyValue.getChildren().values()) {
            IResource resource = curResourceProp.getResource(project);
            Map<IOption, String> resourceOptions = new HashMap<>();
            for (KeyValueTree curOption : curResourceProp.getChildren().values()) {
                String value = curOption.getValue(KEY_VALUE);
                IOption option = projectType.getOption(curOption.getValue(KEY));
                resourceOptions.put(option, value);
            }
            mySelectedOptions.put(resource, resourceOptions);
        }
    }


    public boolean equals(AutoBuildOptions other) {
    	return mySelectedOptions.equals(other.mySelectedOptions);
//        if(mySelectedOptions.size()!=other.mySelectedOptions.size()) {
//            return false;
//        }
//
//        for(Entry<IResource, Map<IOption, String>> curOption:other.mySelectedOptions.entrySet()) {
//        	IResource curResource=curOption.getKey();
//        	 Map<IOption, String> curOptions= curOption.getValue();
//            Map<IOption, String> localResourceOptions=mySelectedOptions.get(curResource);
//            if(localResourceOptions==null || (curOptions.size()!=localResourceOptions.size()) ) {
//                return false;
//            }
//        }
//        return true;
    }


    public TreeMap<IOption, String> getSelectedOptions(IResource file){
        Map<IOption, String> retProject = new HashMap<>();
        Map<Integer, Map<IOption, String>> retFolder = new HashMap<>();
        Map<IOption, String> retFile = new HashMap<>();
        for (Entry<IResource, Map<IOption, String>> curResourceOptions : myCombinedOptions.entrySet()) {
            IResource curResource = curResourceOptions.getKey();
            if (curResource == null || curResource instanceof IProject) {
                // null means project level and as sutch is valid for all resources
                retProject.putAll(curResourceOptions.getValue());
                continue;
            }
            if (curResource instanceof IFolder) {
                if (curResource.getProjectRelativePath().isPrefixOf(file.getProjectRelativePath())) {

                    retFolder.put(Integer.valueOf(curResource.getProjectRelativePath().segmentCount()),
                            curResourceOptions.getValue());

                }
                continue;
            }
            if ((curResource instanceof IFile) && (curResource.equals(file))) {
                retFile.putAll(curResourceOptions.getValue());
                continue;
            }
        }
        TreeMap<IOption, String> ret = getSortedOptionMap();
        ret.putAll(retProject);
        TreeSet<Integer> segments = new TreeSet<>(retFolder.keySet());
        for (Integer curSegment : segments) {
            ret.putAll(retFolder.get(curSegment));
        }
        ret.putAll(retFile);
        return ret;
    }


    private static TreeMap<IOption, String> getSortedOptionMap() {
        TreeMap<IOption, String> ret = new TreeMap<>(new java.util.Comparator<>() {

            @Override
            public int compare(IOption o1, IOption o2) {
                if (o1 == null || o2 == null) {
                    return 0;
                }
                return o1.getId().compareTo(o2.getId());
            }
        });
        return ret;
    }

    public TreeMap<IOption, String> getSelectedOptions(IResource file, ITool tool) {
        TreeMap<IOption, String> ret = getSelectedOptions(file);

        // remove all options not known to the tool
        List<IOption> toolOptions = tool.getOptions().getOptions();// TOFIX : this should be get Enabled Options
        for (Iterator<IOption> iterator = ret.keySet().iterator(); iterator.hasNext();) {
            IOption curOption = iterator.next();
            if (!toolOptions.contains(curOption)) {
                iterator.remove();
            }
        }
        return ret;
    }

    public TreeMap<IOption, String> getSelectedOptions(Set<? extends IResource> file, ITool tool) {
        TreeMap<IOption, String> ret = getSortedOptionMap();
        for (IResource curFile : file) {
            Map<IOption, String> fileOptions = getSelectedOptions(curFile, tool);
            for (Entry<IOption, String> curResourceOption : fileOptions.entrySet()) {
                IOption curKey = curResourceOption.getKey();
                String curValue = curResourceOption.getValue();
                if (ret.containsKey(curKey)) {
                    if (!ret.get(curKey).equals(curValue)) {
                        // TOFIX log error
                    }
                }
                ret.put(curKey, curValue);
            }
        }
        return ret;
    }

    public void setOptionValue(IResource resource, IOption option, String valueID) {
        Map<IOption, String> options = mySelectedOptions.get(resource);
        if (options == null) {
            if (valueID == null || valueID.isBlank()) {
                // as it does not exist and we want to erase do nothing
                return;
            }
            options = new HashMap<>();
            mySelectedOptions.put(resource, options);
        }
        if (valueID == null || valueID.isBlank()) {
            options.remove(option);
        } else {
            options.put(option, valueID);
        }
        options_combine();
    }

    public String getOptionValue(IResource resource, ITool tool, IOption option) {
        if (tool.getOption(option.getId()) == null) {
            // the tool does not know this option
            return EMPTY_STRING;
        }

        if (myCombinedOptions == null) {
            // there are no options selected by the user
            return EMPTY_STRING;
        }

        Map<IOption, String> resourceOptions = myCombinedOptions.get(resource);
        if (resourceOptions == null) {
            // there are no options selected by the user for this resource
            return EMPTY_STRING;
        }
        String ret = resourceOptions.get(option);
        if (ret == null) {
            // there are no options selected by the user for this resource/option
            return EMPTY_STRING;
        }
        return ret;
    }

    /*
     * take options and make a copy
     */
    private static void options_copy(Map<IResource, Map<IOption, String>> from,
            Map<IResource, Map<IOption, String>> to) {
        for (Entry<IResource, Map<IOption, String>> fromResourceSet : from.entrySet()) {
            IResource curResource = fromResourceSet.getKey();
            Map<IOption, String> curFromOptions = fromResourceSet.getValue();

            Map<IOption, String> curToOptions = to.get(curResource);
            if (curToOptions == null) {
                curToOptions = new HashMap<>();
                to.put(curResource, curToOptions);
            }
            curToOptions.putAll(curFromOptions);
        }
    }


    /**
     * get the default options and update the combined options
     */
    public void updateDefault(IConfiguration myAutoBuildConfiguration,AutoBuildConfigurationDescription parent) {
        IProject project=parent.getProject();
        myDefaultOptions.clear();
        Map<IOption, String> defaultOptions = myAutoBuildConfiguration.getDefaultOptions(project, parent);
        IToolChain toolchain = myAutoBuildConfiguration.getProjectType().getToolChain();
        defaultOptions.putAll(toolchain.getDefaultOptions(project, parent));

        for (ITool curITool : toolchain.getTools()) {
            Tool curTool = (Tool) curITool;
            if (!curTool.isEnabled(project, parent)) {
                continue;
            }
            // Map<IResource, Map<String, String>> resourceOptions = new HashMap<>();
            defaultOptions.putAll(curTool.getDefaultOptions(project, parent));

        }
        myDefaultOptions.put(null, defaultOptions);
        options_combine();
    }

    /*
     * take myDefaultOptions and mySelected options and combine them in
     * myCombinedOptions From now onwards the combined options are to be used as
     * they take the default and the user selected option into account in the
     * desired way
     */
    private void options_combine() {
        myCombinedOptions.clear();
        options_copy(myDefaultOptions, myCombinedOptions);
        options_copy(mySelectedOptions, myCombinedOptions);

    }

    public void serialize(KeyValueTree keyValuePairs) {
        int counter = counterStart;
        KeyValueTree optionsKeyValue = keyValuePairs.addChild(OPTION);
        for (Entry<IResource, Map<IOption, String>> curOption : mySelectedOptions.entrySet()) {
            IResource resource = curOption.getKey();
            KeyValueTree curResourceKeyValue=optionsKeyValue.addChild(String.valueOf(counter),resource);
            if(curResourceKeyValue==null) {
                continue;
            }
            counter++;
            int counter2 = counterStart;
            for (Entry<IOption, String> resourceOptions : curOption.getValue().entrySet()) {
                IOption key =resourceOptions.getKey();
                if(key==null) {
                    continue;
                }
                KeyValueTree curOptionKeyValue = curResourceKeyValue.addChild(String.valueOf(counter2));
                curOptionKeyValue.addValue(KEY, key.getId());
                curOptionKeyValue.addValue(KEY_VALUE, resourceOptions.getValue());
                counter2++;
            }
        }

    }
}
