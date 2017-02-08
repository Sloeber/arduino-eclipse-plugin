/*******************************************************************************
 * Copyright (c) 2017 Remain Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package io.sloeber.ui.preferences;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Utilities class for the preferences.
 *
 * @author Wim Jongman (wim.jongman@remainsoftware.com)
 * @since 4.1
 *
 */
public class PreferenceUtils {

    /**
     * ID for the main preference page.
     */
    public static final String SLOEBER_MAIN = "io.sloeber.eclipse.ArduinoPreferencePage";

    /**
     * ID for the open preference page command parameter
     */
    public static final String PREFERENCE_PARAMETER1 = "io.sloeber.ui.actions.openPreferences.pageId";

    private static String[] fSloeberPreferencePageIds;

    /**
     * Finds all preference pages contributed by plugins in the passed name
     * space.
     *
     * @return an array of preference pages.
     * @since 4.1
     */
    public static String[] getPreferencePages(String nameSpace) {
	if (fSloeberPreferencePageIds != null) {
	    return fSloeberPreferencePageIds;
	}
	fSloeberPreferencePageIds = loadRemainPreferencePages(nameSpace);
	return fSloeberPreferencePageIds;
    }

    private static String[] loadRemainPreferencePages(String nameSpace) {
	ArrayList<String> result = new ArrayList<>();
	IConfigurationElement[] elements = Platform.getExtensionRegistry()
		.getConfigurationElementsFor("org.eclipse.ui.preferencePages");
	for (IConfigurationElement element : elements) {
	    String contributor = element.getContributor().getName();
	    if (contributor.startsWith(nameSpace)) {
		String pageId = element.getAttribute("id");
		result.add(pageId);
	    }
	}
	return result.toArray(new String[0]);
    }
}
