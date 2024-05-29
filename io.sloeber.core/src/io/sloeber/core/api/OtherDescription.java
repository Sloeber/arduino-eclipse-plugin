package io.sloeber.core.api;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.core.txt.TxtFile;

public class OtherDescription {
    private static final String KEY_SLOEBER_IS_VERSION_CONTROLLED = "IS_VERSION_CONTROLLED"; //$NON-NLS-1$

    private boolean myIsVersionControlled = false;

    OtherDescription(TxtFile configFile, String prefix) {

        KeyValueTree tree = configFile.getData();
        KeyValueTree section = tree.getChild(prefix);
        myIsVersionControlled = TRUE.equalsIgnoreCase(section.getValue(KEY_SLOEBER_IS_VERSION_CONTROLLED));
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

    public Map<String, String> getEnvVarsConfig() {
        Map<String, String> allVars = new TreeMap<>();

        allVars.put(KEY_SLOEBER_IS_VERSION_CONTROLLED, Boolean.valueOf(myIsVersionControlled).toString());

        return allVars;
    }

    /**
     * recreate the config based on the configuration environment variables
     *
     * @param envVars
     */
    public OtherDescription(Map<String, String> envVars) {
        for (Entry<String, String> curEnvVar : envVars.entrySet()) {
            String key = curEnvVar.getKey();
            String value = curEnvVar.getValue();
            switch (key) {
            case KEY_SLOEBER_IS_VERSION_CONTROLLED:
                myIsVersionControlled = Boolean.parseBoolean(value);
                break;
			default:
				break;
            }
        }
    }

    public Map<String, String> getEnvVarsVersion() {
        return getEnvVarsConfig();
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
