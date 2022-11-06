package io.sloeber.autoBuild.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.managedBuild.ui.internal.messages"; //$NON-NLS-1$

    public static String NewAutoMakeProjectWizard_WindowTitle;
    public static String NewAutoMakeProjectWizard_Description;
    public static String NewAutoMakeProjectWizard_PageTitle;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
