package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;

@SuppressWarnings("nls")
public class RegressionTestFailingOnTravis {

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */
	@BeforeClass
	public static void WaitForInstallerToFinish() {
		Shared.waitForAllJobsToFinish();
		Preferences.setUseBonjour(false);
		installAdditionalBoards();
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json" };
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), false);
		if (!MySystem.getTeensyPlatform().isEmpty()) {
			PackageManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());
		}
	}

	/**
	 * Test wether a platform json redirect is handled properly
	 * https://github.com/jantje/arduino-eclipse-plugin/issues/393
	 */
	@SuppressWarnings("static-method")
	@Test
	public void redirectedJson() {
		//this board references to arduino avr so install that one to
	    Arduino.installLatestAVRBoards();
		PackageManager.installLatestPlatform("package_talk2.wisen.com_index.json", "Talk2","Talk2 AVR Boards");
		Map<String, String> options = new HashMap<>();
		options.put("mhz", "16MHz");
		BoardDescriptor boardid = PackageManager.getBoardDescriptor("package_talk2.wisen.com_index.json", "Talk2",
				"Talk2 AVR Boards", "whispernode", options);
		if (boardid == null) {
			fail("redirect Json ");
			return;
		}
		if(!Shared.BuildAndVerify("redirect_json",boardid,CodeDescriptor.createDefaultIno(),new CompileOptions(null))) {
            fail(Shared.getLastFailMessage() );
		}
	}
}
