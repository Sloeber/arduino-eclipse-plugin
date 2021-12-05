package io.sloeber.junit;

import static org.junit.Assert.*;

import org.junit.Test;

import io.sloeber.core.api.VersionNumber;

@SuppressWarnings({ "nls", "static-method" })
public class TestVersionCompare {

    @Test
    public void NonStringVersion() {
        assertEquals("1", new VersionNumber("1.0.0").compareTo("1"), 1);
        assertEquals("2", new VersionNumber("1.1.0").compareTo("1.1"), 1);
        assertEquals("3", new VersionNumber("1.1").compareTo("1.1.1"), -1);
        assertEquals("3", new VersionNumber("1").compareTo("1.1"), -1);
        assertEquals("1", new VersionNumber("1.0.0").compareTo("1.0.0"), 0);
        assertEquals("2", new VersionNumber("1.1.0").compareTo("1.1.0"), 0);
        assertEquals("3", new VersionNumber("1.1.1").compareTo("1.1.1"), 0);
        assertEquals("4", new VersionNumber("1.0.0").compareTo("1.1.0"), -1);
        assertEquals("5", new VersionNumber("1.0.0").compareTo("1.0.1"), -1);
        assertEquals("6", new VersionNumber("1.0.0").compareTo("1.1.1"), -1);
        assertEquals("7", new VersionNumber("1.1.0").compareTo("1.0.0"), 1);
        assertEquals("8", new VersionNumber("1.0.1").compareTo("1.0.0"), 1);
        assertEquals("9", new VersionNumber("1.1.1").compareTo("1.0.0"), 1);
    }

    @Test
    public void doubleDigit() {
        assertEquals("1", new VersionNumber("1.0.0").compareTo("16"), -1);
        assertEquals("2", new VersionNumber("1.1.0").compareTo("1.16"), -1);
        assertEquals("3", new VersionNumber("1.1").compareTo("1.1.16"), -1);
        assertEquals("3", new VersionNumber("1").compareTo("16.1"), -1);
        assertEquals("4", new VersionNumber("1.16.0").compareTo("1.1.0"), 1);
        assertEquals("5", new VersionNumber("16.0.0").compareTo("1.0.1"), 1);
        assertEquals("6", new VersionNumber("1.1.16").compareTo("1.1.1"), 1);

    }

    @Test
    public void StringVersions() {
        assertEquals("1", new VersionNumber("4.5.2r2").compareTo("4.5.2"), 1);
        assertEquals("1", new VersionNumber("4.5.2r2").compareTo("4.5.2r3"), -1);
        assertEquals("1", new VersionNumber("4.5.2r2").compareTo("4.5.20"), -1);
        assertEquals("1", new VersionNumber("4.5.20r2").compareTo("4.5.20"), 1);
    }
}
