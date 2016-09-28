package io.sloeber.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import io.sloeber.core.api.Sketch;

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
	if (proj.isOpen()) {
	    if (Sketch.isSketch(proj)) {
		String boardName = Sketch.getBoardName(proj);
		String portName = Sketch.getComport(proj);
		if (portName.isEmpty()) {
		    portName = "no port"; //$NON-NLS-1$
		}
		if (boardName.isEmpty()) {
		    boardName = "no Board"; //$NON-NLS-1$
		}
		return text + ' ' + boardName + ' ' + ':' + portName;
	    }
	}

	return null;
    }

}
