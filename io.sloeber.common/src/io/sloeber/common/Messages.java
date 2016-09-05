package io.sloeber.common;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.common.messages"; //$NON-NLS-1$
    public static String buildBeforeUpload;
    public static String doYouWantToBuildBeforeUpload;
    public static String pleaseWaitForInstallerJob;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
