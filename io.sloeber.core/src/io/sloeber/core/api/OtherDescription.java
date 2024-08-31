package io.sloeber.core.api;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.core.txt.TxtFile;

public class OtherDescription {
    private static final String KEY_SLOEBER_IS_VERSION_CONTROLLED = "IS_VERSION_CONTROLLED"; //$NON-NLS-1$

    private boolean myIsVersionControlled = false;

    OtherDescription(TxtFile configFile, String prefix) {

        KeyValueTree tree = configFile.getData();
        KeyValueTree section = tree.getChild(prefix);
        myIsVersionControlled = Boolean.parseBoolean(section.getValue(KEY_SLOEBER_IS_VERSION_CONTROLLED));
    }

    public OtherDescription() {
        // nothing needs to be done
    }

    public OtherDescription(OtherDescription srcObject) {
        myIsVersionControlled = srcObject.myIsVersionControlled;
    }

    @SuppressWarnings("static-method")
	public Map<String, String> getEnvVars() {
        Map<String, String> allVars = new TreeMap<>();
        // Nothing needs to be put in the environment variables
        return allVars;
    }

    public void serialize(KeyValueTree keyValueTree) {
    	keyValueTree.addChild(KEY_SLOEBER_IS_VERSION_CONTROLLED, Boolean.valueOf(myIsVersionControlled).toString());
    }

    /**
     * recreate the config based on the configuration environment variables
     *
     * @param envVars
     */
    public OtherDescription(KeyValueTree keyValues) {
    	myIsVersionControlled = Boolean.parseBoolean(keyValues.getValue(KEY_SLOEBER_IS_VERSION_CONTROLLED));
    }

    public Map<String, String> getEnvVarsVersion() {
    	Map<String, String> allVars = new TreeMap<>();
        allVars.put(KEY_SLOEBER_IS_VERSION_CONTROLLED, Boolean.valueOf(myIsVersionControlled).toString());
        return allVars;
    }

    public boolean IsVersionControlled() {
        return myIsVersionControlled;
    }

    public void setVersionControlled(boolean myIsVersionControlled) {
        this.myIsVersionControlled = myIsVersionControlled;
    }

	@SuppressWarnings({ "static-method", "unused" })
	public boolean needsRebuild(OtherDescription newOtherDesc) {
		return false;
	}

}
