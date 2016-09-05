package io.sloeber.common.test;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class ArduinoIDEVersionNameParsing {

    private Map<String, String> versionList;

    @Test
    public void testAllArduinoVersions() {
	this.versionList = new LinkedHashMap<>();
	this.versionList.put("1.5.1", "151"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.2", "152"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.3", "153"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.4", "154"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.5", "155"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.6", "156"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.6-r2", "156"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.7", "157"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.5.8", "158"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.0", "10600"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.1", "10601"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.2", "10602"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.3", "10603"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.4", "10604"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.5", "10605"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.5-r2", "10605"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.5-r3", "10605"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.5-r4", "10605"); //$NON-NLS-1$ //$NON-NLS-2$
	this.versionList.put("1.6.5-r5", "10605"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
