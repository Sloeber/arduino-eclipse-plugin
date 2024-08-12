package io.sloeber.junit;



import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.sloeber.core.api.VersionNumber;

@SuppressWarnings({ "nls", "static-method" })
public class TestVersionCompare {

    @Test
    public void NonStringVersion() {
        assertEquals( new VersionNumber("1.0.0").compareTo("1"), 1);
        assertEquals( new VersionNumber("1.1.0").compareTo("1.1"), 1);
        assertEquals( new VersionNumber("1.1").compareTo("1.1.1"), -1);
        assertEquals( new VersionNumber("1").compareTo("1.1"), -1);
        assertEquals( new VersionNumber("1.0.0").compareTo("1.0.0"), 0);
        assertEquals( new VersionNumber("1.1.0").compareTo("1.1.0"), 0);
        assertEquals( new VersionNumber("1.1.1").compareTo("1.1.1"), 0);
        assertEquals( new VersionNumber("1.0.0").compareTo("1.1.0"), -1);
        assertEquals( new VersionNumber("1.0.0").compareTo("1.0.1"), -1);
        assertEquals( new VersionNumber("1.0.0").compareTo("1.1.1"), -1);
        assertEquals( new VersionNumber("1.1.0").compareTo("1.0.0"), 1);
        assertEquals( new VersionNumber("1.0.1").compareTo("1.0.0"), 1);
        assertEquals( new VersionNumber("1.1.1").compareTo("1.0.0"), 1);
    }

    @Test
    public void doubleDigit() {
        assertEquals( new VersionNumber("1.0.0").compareTo("16"), -1);
        assertEquals( new VersionNumber("1.1.0").compareTo("1.16"), -1);
        assertEquals( new VersionNumber("1.1").compareTo("1.1.16"), -1);
        assertEquals( new VersionNumber("1").compareTo("16.1"), -1);
        assertEquals( new VersionNumber("1.16.0").compareTo("1.1.0"), 1);
        assertEquals( new VersionNumber("16.0.0").compareTo("1.0.1"), 1);
        assertEquals( new VersionNumber("1.1.16").compareTo("1.1.1"), 1);

    }

    @Test
    public void StringVersions() {
        assertEquals( new VersionNumber("4.5.2r2").compareTo("4.5.2"), 1);
        assertEquals( new VersionNumber("4.5.2r2").compareTo("4.5.2r3"), -1);
        assertEquals( new VersionNumber("4.5.2r2").compareTo("4.5.20"), -1);
        assertEquals( new VersionNumber("4.5.20r2").compareTo("4.5.20"), 1);
    }
}
