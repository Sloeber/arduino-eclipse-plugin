package io.sloeber.core.api;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.core.common.Const;
import io.sloeber.core.txt.KeyValueTree;
import io.sloeber.core.txt.TxtFile;

public class OtherDescription {
    private final String KEY_SLOEBER_IS_VERSION_CONTROLLED = "IS_VERSION_CONTROLLED"; //$NON-NLS-1$

    private boolean myIsVersionControlled = false;

    OtherDescription(TxtFile configFile, String prefix) {

        KeyValueTree tree = configFile.getData();
        KeyValueTree section = tree.getChild(prefix);
        myIsVersionControlled = Const.TRUE.equalsIgnoreCase(section.getValue(KEY_SLOEBER_IS_VERSION_CONTROLLED));
    }

    public OtherDescription() {
        // nothing needs to be done
    }

    public OtherDescription(OtherDescription srcObject) {
        myIsVersionControlled = srcObject.myIsVersionControlled;
    }

    public Map<String, String> getEnvVars() {
        Map<String, String> allVars = new TreeMap<>();
        // Nothing needs to be put in the environment variables
        return allVars;
    }

    public Map<String, String> getEnvVarsConfig(String prefix) {
        Map<String, String> allVars = new TreeMap<>();

        allVars.put(prefix + KEY_SLOEBER_IS_VERSION_CONTROLLED, Boolean.valueOf(myIsVersionControlled).toString());

        return allVars;
    }

    public Map<String, String> getEnvVarsVersion(String prefix) {
        return getEnvVarsConfig(prefix);
    }

    public static OtherDescription getFromCDT(ICConfigurationDescription confDesc) {
        OtherDescription ret = new OtherDescription();
        ret.myIsVersionControlled = false;
        return ret;
    }

    public boolean IsVersionControlled() {
        return myIsVersionControlled;
    }

    public void setVersionControlled(boolean myIsVersionControlled) {
        this.myIsVersionControlled = myIsVersionControlled;
    }
}
