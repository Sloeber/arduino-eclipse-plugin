package io.sloeber.ui;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import io.sloeber.core.api.BoardDescriptor;
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
				ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(proj);
				if (prjDesc == null) {
					return new String();
				}
				ICConfigurationDescription configurationDescription = prjDesc.getActiveConfiguration();
				BoardDescriptor boardDescriptor = BoardDescriptor.makeBoardDescriptor(configurationDescription);
				String boardName = boardDescriptor.getBoardName();
				String portName = boardDescriptor.getActualUploadPort();
				if (portName.isEmpty()) {
					portName = Messages.decorator_no_port; 
				}
				if (boardName.isEmpty()) {
					boardName = Messages.decorator_no_platform; 
				}
				return text + ' ' + boardName + ' ' + ':' + portName;
			}
		}

		return null;
	}

}
