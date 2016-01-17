package it.baeyens.arduino.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

public class ExplorerLabelDecorator implements ILabelDecorator {

    @Override
    public void addListener(ILabelProviderListener listener) {
	// TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
	// TODO Auto-generated method stub

    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
	// TODO Auto-generated method stub
	return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
	// TODO Auto-generated method stub

    }

    @Override
    public Image decorateImage(Image image, Object element) {
	// TODO Auto-generated method stub
	return null;
    }

    @Override
    public String decorateText(String text, Object element) {
	// TODO Auto-generated method stub
	IProject proj = (IProject) element;
	try {
	    if (proj.hasNature(ArduinoConst.ArduinoNatureID)) {
		String boardName = Common.getBuildEnvironmentVariable(proj, ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME,
			"Board Error");
		String portName = Common.getBuildEnvironmentVariable(proj, ArduinoConst.ENV_KEY_JANTJE_COM_PORT,
			"no port");
		return text + ' ' + boardName + ' ' + ':' + portName;
	    }
	} catch (CoreException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return null;
    }

}
