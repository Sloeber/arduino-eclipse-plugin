package io.sloeber.core.api;

@SuppressWarnings("unused")
public class VersionNumber implements Comparable<Object> {
    private String[] parts;

    public VersionNumber(String version) {
        parts = version.split("\\."); //$NON-NLS-1$
    }

    @Override
    public int compareTo(Object other) {
        if (other instanceof String) {
            return compareParts(new VersionNumber((String) other).parts, 0);
        } else if (other instanceof VersionNumber) {
            return compareParts(((VersionNumber) other).parts, 0);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private int compareParts(String[] other, int level) {
        if (parts.length > level && other.length > level) {
            if (parts[level].compareTo(other[level]) == 0) {
                return compareParts(other, level + 1);
            }
            try {
                try {
                    int vi1 = Integer.parseInt(parts[level]);
                    int vi2 = Integer.parseInt(other[level]);
                    if (vi1 < vi2) {
                        return -1;
                    }

                    if (vi1 > vi2) {
                        return 1;
                    }
                } catch (NumberFormatException e) {
                    // not numbers try number string like 6r2
                    try {
                        int vi1 = Integer.parseInt(parts[level].replaceAll("\\D", " ").split(" ")[0]); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                        int vi2 = Integer.parseInt(other[level].replaceAll("\\D", " ").split(" ")[0]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        if (vi1 < vi2) {
                            return -1;
                        }

                        if (vi1 > vi2) {
                            return 1;
                        }
                    } catch (Exception e2) {
                        //
                    }
                    //do string compares
                    int c = parts[level].compareTo(other[level]);
                    if (c < 0) {
                        return -1;
                    }
                    if (c > 0) {
                        return 1;
                    }
                }
                return Integer.valueOf(this.parts[level]).compareTo(Integer.valueOf(Integer.parseInt(other[level])));
            } catch (Exception e) {
                return this.parts[level].compareTo(other[level]);
            }
        }
        if (parts.length == other.length) {
            return 0;
        }
        return parts.length > other.length ? 1 : -1;
    }

    @Override
    public String toString() {
        return String.join(".", parts); //$NON-NLS-1$
    }
}