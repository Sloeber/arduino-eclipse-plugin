package it.baeyens.arduino.managers;

import java.util.Random;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.PlatformUI;

/**
 * A class to give the user something to read.
 * 
 * @author Wim Jongman
 *
 */
public class InstallProgress {

    private static int fVerb;
    private static int fComponent;
    private static int fAction;

    private static String getAction() {
	fAction = getNewNumber(fAction, 10);
	switch (fAction) {
	case 0:
	    return "frequency"; //$NON-NLS-1$
	case 1:
	    return "base"; //$NON-NLS-1$
	case 2:
	    return "vibration"; //$NON-NLS-1$
	case 3:
	    return "leakage"; //$NON-NLS-1$
	case 4:
	    return "jitter"; //$NON-NLS-1$
	case 5:
	    return "collision"; //$NON-NLS-1$
	case 6:
	    return "resistance"; //$NON-NLS-1$
	case 7:
	    return "speed"; //$NON-NLS-1$
	case 8:
	    return "temperature"; //$NON-NLS-1$
	default:
	    return "connections"; //$NON-NLS-1$
	}
    }

    private static String getComponent() {

	fComponent = getNewNumber(fComponent, 10);

	switch (fComponent) {
	case 0:
	    return "transistor"; //$NON-NLS-1$
	case 1:
	    return "resistor"; //$NON-NLS-1$
	case 2:
	    return "capacitor"; //$NON-NLS-1$
	case 3:
	    return "led"; //$NON-NLS-1$
	case 4:
	    return "pin"; //$NON-NLS-1$
	case 5:
	    return "dipswitch"; //$NON-NLS-1$
	case 6:
	    return "diode"; //$NON-NLS-1$
	case 7:
	    return "sensor"; //$NON-NLS-1$
	case 8:
	    return "cable"; //$NON-NLS-1$
	default:
	    return "board"; //$NON-NLS-1$
	}
    }

    private static String getVerb() {

	fVerb = getNewNumber(fVerb, 10);

	switch (fVerb) {
	case 0:
	    return "Soldering"; //$NON-NLS-1$
	case 1:
	    return "Heating"; //$NON-NLS-1$
	case 2:
	    return "Shaking"; //$NON-NLS-1$
	case 3:
	    return "Checking"; //$NON-NLS-1$
	case 4:
	    return "Examining"; //$NON-NLS-1$
	case 5:
	    return "Testing"; //$NON-NLS-1$
	case 6:
	    return "Optimizing"; //$NON-NLS-1$
	case 7:
	    return "Drilling"; //$NON-NLS-1$
	case 8:
	    return "Improving"; //$NON-NLS-1$
	default:
	    return "Connecting"; //$NON-NLS-1$
	}
    }

    private static int getNewNumber(int pNumber, int pMax) {
	int result = new Random().nextInt(pMax);
	while (result == pNumber) {
	    result = new Random().nextInt(pMax);
	}
	return result;
    }

    public static String getRandomMessage() {
	return getVerb() + " " + getComponent() + " " + getAction() + ".."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public static void showIntroduction() {
	PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
	    @Override
	    public void run() {
		MessageDialog.openInformation(null, "Finishing the installation", getMessage()); //$NON-NLS-1$
	    }
	});
    }

    protected static String getMessage() {
	StringBuilder message = new StringBuilder();
	message.append("Hi ").append(System.getProperty("user.name")).append(",\n\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	message.append("Thank you for downloading the Arduino Eclipse "); //$NON-NLS-1$
	message.append("IDE. We need to install a few things to get you "); //$NON-NLS-1$
	message.append("up and running. Depending on your internet speed "); //$NON-NLS-1$
	message.append("it can take anywhere between 2 minutes and half an hour.").append("\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
	message.append("Press Ok to continue.").append("\n\n"); //$NON-NLS-1$ //$NON-NLS-2$
	return message.toString();
    }

    public static void main(String[] args) {
	InstallProgress.showIntroduction();
    }
}
