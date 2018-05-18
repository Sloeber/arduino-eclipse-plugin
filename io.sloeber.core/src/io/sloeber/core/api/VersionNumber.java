package io.sloeber.core.api;
@SuppressWarnings("unused")
public class VersionNumber implements Comparable<Object> {
    private String[] parts;

    public VersionNumber(String version) {
	this.parts = version.split("\\."); //$NON-NLS-1$
    }

    @Override
    public int compareTo(Object other) {
	if (other instanceof String) {
	    return this.compareTo(new VersionNumber((String) other));
	} else if (other instanceof VersionNumber) {
	    return this.compareParts(((VersionNumber) other).parts, 0);
	} else {
	    throw new UnsupportedOperationException();
	}
    }

    private int compareParts(String[] other, int level) {
	if (this.parts.length > level && other.length > level) {
	    if (this.parts[level].compareTo(other[level]) == 0) {
		return this.compareParts(other, level + 1);
	    }
	    try {
		return new Integer(this.parts[level]).compareTo(new Integer(Integer.parseInt(other[level])));
	    } catch (Exception e) {
		return this.parts[level].compareTo(other[level]);
	    }
	}
	if (this.parts.length == other.length) {
	    return 0;
	}
	return this.parts.length > other.length ? 1 : -1;
    }

    @Override
    public String toString() {
	return String.join(".", this.parts); //$NON-NLS-1$
    }
}