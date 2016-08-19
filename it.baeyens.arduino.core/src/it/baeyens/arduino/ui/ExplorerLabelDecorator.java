package it.baeyens.arduino.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;

public class ExplorerLabelDecorator implements ILabelDecorator {

    @Override
    public void addListener(ILabelProviderListener listener) {
	// nothing to do here

    }

    @Override
    public void dispose() {
	// nothing to do here

    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
	return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
	// nothing to do here

    }

    @Override
    public Image decorateImage(Image image, Object element) {
	return null;
    }

    @Override
    public String decorateText(String text, Object element) {
	IProject proj = (IProject) element;
	try {
	    if (proj.isOpen()) {
		if (proj.hasNature(Const.ARDUINO_NATURE_ID)) {
		    String boardName = Common.getBuildEnvironmentVariable(proj, Const.ENV_KEY_JANTJE_BOARD_NAME, "Board Error"); //$NON-NLS-1$
		    String portName = Common.getBuildEnvironmentVariable(proj, Const.ENV_KEY_JANTJE_UPLOAD_PORT, "no port"); //$NON-NLS-1$
		    return text + ' ' + boardName + ' ' + ':' + portName;
		}
	    }
	} catch (CoreException e) {
	    e.printStackTrace();
	    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Decoration failed", e)); //$NON-NLS-1$
	}
	return null;
    }

}
