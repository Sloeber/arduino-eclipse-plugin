package multichannel;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "multichannel.messages"; //$NON-NLS-1$
    public static String Oscilloscope_error_invalid_tail_size;
    public static String Oscilloscope_error_stack_size_to_small;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
