package io.sloeber.core.tools;
@SuppressWarnings("unused") 
public class Version {
	/**
	 * compares 2 strings as if they are version numbers if version1<version2
	 * returns -1 if version1==version2(also if both are null) returns 0 else
	 * return 1 This method caters for the null case
	 *
	 * @param version1
	 * @param version2
	 * @return
	 */
	public static int compare(String version1, String version2) {
		if (version1 == null) {
			return version2 == null ? 0 : -1;
		}

		if (version2 == null) {
			return 1;
		}

		String[] v1 = version1.split("[\\.\\+-]"); //$NON-NLS-1$
		String[] v2 = version2.split("[\\.\\+-]"); //$NON-NLS-1$
		for (int i = 0; i < Math.max(v1.length, v2.length); ++i) {
			if (v1.length <= i) {
				return v2.length < i ? 0 : -1;
			}

			if (v2.length <= i) {
				return 1;
			}

			try {
				int vi1 = Integer.parseInt(v1[i]);
				int vi2 = Integer.parseInt(v2[i]);
				if (vi1 < vi2) {
					return -1;
				}

				if (vi1 > vi2) {
					return 1;
				}
			} catch (NumberFormatException e) {
				// not numbers try number string like 6r2
				try {
				int vi1 = Integer.parseInt(v1[i].replaceAll("\\D", " " ).split(" ")[0]);  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				int vi2 = Integer.parseInt(v2[i].replaceAll("\\D", " " ).split(" ")[0]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (vi1 < vi2) {
					return -1;
				}

				if (vi1 > vi2) {
					return 1;
				}}
				catch(Exception e2) {
					//
				}
				//do string compares
				int c = v1[i].compareTo(v2[i]);
				if (c < 0) {
					return -1;
				}
				if (c > 0) {
					return 1;
				}
			}
		}

		return 0;
	}

	/**
	 * Given a list of version strings returns the index of the highest version
	 * If the highest version is multiple times in the list the result will
	 * point to one of those but the result may be different for each call
	 *
	 * @param versions
	 *            a string list of version numbers
	 *
	 * @return the index to the highest version or 0 in case of an empty
	 *         versions
	 */
	public static int getHighestVersion(String[] versions) {
		int returnIndex = 0;
		for (int curVersion = 1; curVersion < versions.length; curVersion++) {
			if (compare(versions[returnIndex], versions[curVersion]) == -1) {
				returnIndex = curVersion;
			}

		}
		return returnIndex;
	}

}
