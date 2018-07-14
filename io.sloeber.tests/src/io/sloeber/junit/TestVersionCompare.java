package io.sloeber.junit;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.sloeber.core.tools.Version;

@SuppressWarnings({"nls","static-method"})
public class TestVersionCompare {


	@Test
	public void NonStringVersion() {
		assertEquals("1",Version.compare("1.0.0","1"),1);
		assertEquals("2",Version.compare("1.1.0","1.1"),1);
		assertEquals("3",Version.compare("1.1","1.1.1"),-1);
		assertEquals("3",Version.compare("1","1.1"),-1);
		assertEquals("1",Version.compare("1.0.0","1.0.0"),0);
		assertEquals("2",Version.compare("1.1.0","1.1.0"),0);
		assertEquals("3",Version.compare("1.1.1","1.1.1"),0);
		assertEquals("4",Version.compare("1.0.0","1.1.0"),-1);
		assertEquals("5",Version.compare("1.0.0","1.0.1"),-1);
		assertEquals("6",Version.compare("1.0.0","1.1.1"),-1);
		assertEquals("7",Version.compare("1.1.0","1.0.0"),1);
		assertEquals("8",Version.compare("1.0.1","1.0.0"),1);
		assertEquals("9",Version.compare("1.1.1","1.0.0"),1);
	}
	@Test
	public void doubleDigit() {
		assertEquals("1",Version.compare("1.0.0","16"),-1);
		assertEquals("2",Version.compare("1.1.0","1.16"),-1);
		assertEquals("3",Version.compare("1.1","1.1.16"),-1);
		assertEquals("3",Version.compare("1","16.1"),-1);
		assertEquals("4",Version.compare("1.16.0","1.1.0"),1);
		assertEquals("5",Version.compare("16.0.0","1.0.1"),1);
		assertEquals("6",Version.compare("1.1.16","1.1.1"),1);

	}
	@Test
	public void StringVersions() {
		assertEquals("1",Version.compare("4.5.2r2","4.5.2"),1);
		assertEquals("1",Version.compare("4.5.2r2","4.5.2r3"),-1);
		assertEquals("1",Version.compare("4.5.2r2","4.5.20"),-1);
		assertEquals("1",Version.compare("4.5.20r2","4.5.20"),1);
	}
}
