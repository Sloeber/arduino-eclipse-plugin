package io.sloeber.ui.core.launchconfig;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.ui.core.launchconfig.messages"; //$NON-NLS-1$
    public static String MainTab_browse;
    public static String MainTab_Main;
    public static String MainTab_Project;
    public static String MainTab_ProjectDoesNotExist;
    public static String MainTab_ProjectSelection;
    public static String MainTab_ProjectWrongType;
    public static String MainTab_SpecifyProject;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
