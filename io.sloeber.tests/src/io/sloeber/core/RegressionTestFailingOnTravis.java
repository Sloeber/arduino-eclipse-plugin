package io.sloeber.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.BoardsManager;
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
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), false);
		if (!MySystem.getTeensyPlatform().isEmpty()) {
			BoardsManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());
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
        BoardsManager.installLatestPlatform("package_talk2.wisen.com_index.json", "Talk2", "avr");
		Map<String, String> options = new HashMap<>();
		options.put("mhz", "16MHz");
		BoardDescription boardid = BoardsManager.getBoardDescription("package_talk2.wisen.com_index.json", "Talk2",
                "avr", "whispernode", options);
		if (boardid == null) {
			fail("redirect Json ");
			return;
		}
        if (!Shared.BuildAndVerify("redirect_json", boardid, CodeDescription.createDefaultIno(),
                new CompileDescription())) {
            fail(Shared.getLastFailMessage() );
		}
	}
}
