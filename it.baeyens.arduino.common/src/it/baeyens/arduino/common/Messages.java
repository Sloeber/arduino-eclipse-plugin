package it.baeyens.arduino.common;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.common.messages"; //$NON-NLS-1$
    public static String Build_before_upload;
    public static String do_you_want_to_build_before_upload;
    public static String Please_wait_for_installer_job;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
