package io.sloeber.autoBuild.api;

import java.util.Set;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;

/**
 * this abstract class is the basis to extend AutoBuild with your own project
 * data/methods
 *
 * Note you need to create a constructor of type
 *
 * public YourDerivedClass(IAutoBuildConfigurationDescription
 * autoCfgDescription,
 * String curConfigsText, String lineStart, String lineEnd)
 *
 * Create a configuration based on persisted content.
 * The persisted content can contain multiple configurations and as sutch a
 * filtering is needed
 * The lineStart and lineEnd are used to filter content only applicable to this
 * configuration
 *
 * @param cfgDescription
 *            the CDT configuration this object will belong to
 * @param curConfigsText
 *            the persistent content
 * @param lineStart
 *            only consider lines that start with this string
 * @param lineEnd
 *            only consider lines that end with this string
 *
 *
 *
 */
@SuppressWarnings("static-method")
public abstract class AutoBuildConfigurationExtensionDescription {
    private IAutoBuildConfigurationDescription myAutoBuildConfigurationDescription = null;

    public AutoBuildConfigurationExtensionDescription() {
    }

    public IAutoBuildConfigurationDescription getAutoBuildDescription() {
        return myAutoBuildConfigurationDescription;
    }

    public void setAutoBuildDescription(IAutoBuildConfigurationDescription newDesc) {
        myAutoBuildConfigurationDescription = newDesc;
    }

    /**
     * Method to copy the provided class to this. All fields should be copied except
     * myAutoBuildConfigurationDescription
     * to.myAutoBuildConfigurationDescription is set before this call is made
     * to.myAutoBuildConfigurationDescription will unlikely equal to
     * from.myAutoBuildConfigurationDescription
     *
     * @param from
     *            the class to copy from
     */
    public abstract void copyData(AutoBuildConfigurationExtensionDescription from);

    /**
     * convert the object to a string that can be stored (or store it yourself
     * somehow)
     * Note that there is no deserialize as deserialisation is happening in a
     * constructor
     *
     * @param linePrefix
     * @param lineEnd
     * @return
     */
    public abstract void serialize(KeyValueTree keyValuePairs);

    /**
     * A simple copy constructor you should likely leave as is.
     * Implement the copy in copyData
     *
     * @param AutoBuildConfigurationDescription
     *            for the copy
     * @param descriptionBase
     *            the base to copy
     */
    public AutoBuildConfigurationExtensionDescription(
            IAutoBuildConfigurationDescription AutoBuildConfigurationDescription,
            AutoBuildConfigurationExtensionDescription descriptionBase) {
        myAutoBuildConfigurationDescription = AutoBuildConfigurationDescription;
        copyData(descriptionBase);
    }

    /**
     * Get the OSGI bundel name.
     * The OSGI bundel name is used to load your class when reading from disk
     * therefore the bundel needs to be known
     *
     * @return the bundel name your implementation of this class belongs to
     */
    public abstract String getBundelName();

    /**
     * Get the command to use to discover the compiler
     * Only override this method if you want to override
     * AutoBuilds default command
     *
     * @param languageId
     * @return the overriding command or null if no override is needed
     */
	public String getDiscoveryCommand(String languageId) {
    	return null;
    }

	public abstract Set<String> getTeamDefaultExclusionKeys(String name);

	public abstract boolean equals(AutoBuildConfigurationExtensionDescription base);

}
