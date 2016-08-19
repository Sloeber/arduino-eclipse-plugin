package io.sloeber.core.api;

import java.util.ArrayList;

/**
 * A "struct" for the configs - names and tool chain IDs.
 * 
 * @author Brody Kenrick Brought to its own class at API level by Jantje
 * 
 */
@SuppressWarnings("nls")
public class ConfigurationDescriptor {
    public final String Name;
    public final String ToolchainID;
    public final boolean DebugCompilerSettings;

    public ConfigurationDescriptor(String Name, String ToolchainID, boolean DebugCompilerSettings) {
	this.Name = Name;
	this.ToolchainID = ToolchainID;
	this.DebugCompilerSettings = DebugCompilerSettings;
    }

    static public ArrayList<ConfigurationDescriptor> getDefaultDescriptors() {
	ArrayList<ConfigurationDescriptor> alCfgs = new ArrayList<>();

	ConfigurationDescriptor cfgTCidPair = new ConfigurationDescriptor("Release", //$NON-NLS-1$
		"it.baeyens.arduino.core.toolChain.release", false); //$NON-NLS-1$
	alCfgs.add(cfgTCidPair);
	return alCfgs;
    }

    static public ArrayList<ConfigurationDescriptor> getReleaseAndDebug() {
	// TODO: Consider renaming Release to ArduinoIDEConfig
	// JABA:I don't think his is a good idea "standard" or "arduino" may be
	// better
	// Note that changing Release invalidates all existing workspaces. So if
	// we change this timing will be very important.
	ArrayList<ConfigurationDescriptor> alCfgs = new ArrayList<>();

	ConfigurationDescriptor cfgTCidPair = new ConfigurationDescriptor("Release",
		"it.baeyens.arduino.core.toolChain.release", false);
	alCfgs.add(cfgTCidPair); // Always have the release build here

	// Debug has same toolchain as release
	ConfigurationDescriptor cfgTCidPair2 = new ConfigurationDescriptor("Debug_AVaRICE",
		"it.baeyens.arduino.core.toolChain.release", true);
	alCfgs.add(cfgTCidPair2);

	return alCfgs;
    }
}
