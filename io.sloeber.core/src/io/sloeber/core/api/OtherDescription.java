package io.sloeber.core.api;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.core.internal.SloeberConfiguration;

public class OtherDescription {
	/*
	 * Currently otherdescription only contains the flag to save a "team" (something to check into version control)
	 * version of the configuration.
	 * However autobuild has the same flag so this functionality boils down to reading and setting
	 * the flag in autoBuild
	 * If the autoBuild is null we use a local variable (not saved or read from disk)
	 * otherwise we route through to autobuild to handle
	 * That is why this class is basically empty
	 */
    //private static final String KEY_SLOEBER_IS_VERSION_CONTROLLED = "IS_VERSION_CONTROLLED"; //$NON-NLS-1$

    private boolean myDummyIsVersionControlled = false;
    private SloeberConfiguration mySloeberConfiguration=null;

    public OtherDescription( SloeberConfiguration sloeberConfiguration, OtherDescription newOtherDesc) {
		mySloeberConfiguration=sloeberConfiguration;
		if(newOtherDesc.mySloeberConfiguration!=null) {
			setVersionControlled( newOtherDesc.IsVersionControlled());
		}

//        KeyValueTree tree = configFile.getData();
//        KeyValueTree section = tree.getChild(prefix);
//        myIsVersionControlled = Boolean.parseBoolean(section.getValue(KEY_SLOEBER_IS_VERSION_CONTROLLED));
    }

    public OtherDescription() {
        // nothing needs to be done
    }

//	public OtherDescription(OtherDescription srcObject) {
//    	myDummyIsVersionControlled = srcObject.myDummyIsVersionControlled;
//    }

    @SuppressWarnings("static-method")
	public Map<String, String> getEnvVars() {
        Map<String, String> allVars = new TreeMap<>();
        // Nothing needs to be put in the environment variables
        return allVars;
    }

    public void serialize(@SuppressWarnings("unused") KeyValueTree keyValueTree) {
    	//keyValueTree.addChild(KEY_SLOEBER_IS_VERSION_CONTROLLED, Boolean.valueOf(myIsVersionControlled).toString());
    }

    /**
     * recreate the config based on the configuration environment variables
     *
     * @param envVars
     */
    public OtherDescription(@SuppressWarnings("unused") KeyValueTree keyValues) {
    	//myIsVersionControlled = Boolean.parseBoolean(keyValues.getValue(KEY_SLOEBER_IS_VERSION_CONTROLLED));
    }

//    public Map<String, String> getEnvVarsVersion() {
//    	Map<String, String> allVars = new TreeMap<>();
//        //allVars.put(KEY_SLOEBER_IS_VERSION_CONTROLLED, Boolean.valueOf(myIsVersionControlled).toString());
//        return allVars;
//    }

	public boolean IsVersionControlled() {
		if (mySloeberConfiguration == null) {
			return myDummyIsVersionControlled;
		}
		return mySloeberConfiguration.getAutoBuildDesc().isTeamShared();
	}

	public void setVersionControlled(boolean newIsVersionControlled) {
		myDummyIsVersionControlled = newIsVersionControlled;
		if (mySloeberConfiguration != null) {
			mySloeberConfiguration.getAutoBuildDesc().setTeamShared(newIsVersionControlled);
		}
	}

	@SuppressWarnings({ "static-method", "unused" })
	public boolean needsRebuild(OtherDescription newOtherDesc) {
		return false;
	}

}
