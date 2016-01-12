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
	    return "frequency";
	case 1:
	    return "base";
	case 2:
	    return "vibration";
	case 3:
	    return "leakage";
	case 4:
	    return "jitter";
	case 5:
	    return "collision";
	case 6:
	    return "resistance";
	case 7:
	    return "speed";
	case 8:
	    return "temperature";
	default:
	    return "connections";
	}
    }

    private static String getComponent() {

	fComponent = getNewNumber(fComponent, 10);

	switch (fComponent) {
	case 0:
	    return "transistor";
	case 1:
	    return "resistor";
	case 2:
	    return "capacitor";
	case 3:
	    return "led";
	case 4:
	    return "pin";
	case 5:
	    return "dipswitch";
	case 6:
	    return "diode";
	case 7:
	    return "sensor";
	case 8:
	    return "cable";
	default:
	    return "board";
	}
    }

    private static String getVerb() {

	fVerb = getNewNumber(fVerb, 10);

	switch (fVerb) {
	case 0:
	    return "Soldering";
	case 1:
	    return "Heating";
	case 2:
	    return "Shaking";
	case 3:
	    return "Checking";
	case 4:
	    return "Examining";
	case 5:
	    return "Testing";
	case 6:
	    return "Optimizing";
	case 7:
	    return "Drilling";
	case 8:
	    return "Improving";
	default:
	    return "Connecting";
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
	return getVerb() + " " + getComponent() + " " + getAction() + "..";
    }

    public static void showIntroduction() {
	PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
	    public void run() {
		MessageDialog.openInformation(null, "Finishing the installation", getMessage());
	    }
	});
    }

    private static String getMessage() {
	StringBuilder message = new StringBuilder();
	message.append("Hi ").append(System.getProperty("user.name")).append(",\n\n");
	message.append("Thank you for downloading the Arduino Eclipse ");
	message.append("IDE. We need to install a few things to get you ");
	message.append("up and running. Depending on your internet speed ");
	message.append("it can take anywhere between 2 minutes and half an hour.").append("\n\n");
	message.append("Press Ok to continue.").append("\n\n");
	return message.toString();
    }

    public static void main(String[] args) {
	InstallProgress.showIntroduction();
    }
}
