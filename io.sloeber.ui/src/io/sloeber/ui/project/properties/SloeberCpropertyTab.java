package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.cdt.ui.newui.ICPropertyTab;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import io.sloeber.core.api.SloeberProject;
import io.sloeber.ui.Activator;

public abstract class SloeberCpropertyTab extends AbstractCPropertyTab {

	private final QualifiedName sloeberQualifiedName = new QualifiedName(Activator.NODE_ARDUINO, getQualifierString());
	protected SloeberProject mySloeberProject = null;
	/*
	 * object used to store the value in case of new project wizard in that case
	 * there is not configuration to save it
	 */
	private Object myLocalObject = null;

	private void clearSessionProperties(ICConfigurationDescription confDesc) {
		Object sessionProperty = confDesc.getSessionProperty(sloeberQualifiedName);
		if (null != sessionProperty) {
			confDesc.setSessionProperty(sloeberQualifiedName, null);
		}
	}

	protected abstract String getQualifierString();

	/**
	 * updte the screen based on the data stored in the properties
	 */
	protected abstract void updateScreen();

	protected abstract Object getFromScreen();

	protected abstract void updateSloeber(ICConfigurationDescription confDesc);

	protected abstract Object getFromSloeber(ICConfigurationDescription confDesc);

	protected abstract Object makeCopy(Object srcObject);

	protected abstract Object getnewDefaultObject();

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		// make sure all configurations hold their description
		// This is needed so that when a copy is made
		// the root compile description is known
		ICProjectDescription projDesc = getConfdesc().getProjectDescription();
		mySloeberProject = SloeberProject.getSloeberProject(projDesc.getProject(), false);
		for (ICConfigurationDescription curConfig : projDesc.getConfigurations()) {
			Object description = getFromSloeber(curConfig);
			if (description == null) {
				description = getnewDefaultObject();
			}
			setDescription(curConfig, description);
		}

		// We can now safely assume confDesc.getSessionProperty(sloeberQualifiedName);
		// is not null

	}

	@Override
	protected void performDefaults() {
		Object description = getnewDefaultObject();

		setDescription(getConfdesc(), description);
		updateScreen();
	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		ICConfigurationDescription confDesc = dst.getConfiguration();

		getFromScreen();
		updateSloeber(confDesc);
	}

	/**
	 * Get the configuration we are currently working in. The configuration is null
	 * if we are in the create sketch wizard.
	 *
	 * @return the configuration to save info into
	 */
	protected ICConfigurationDescription getConfdesc() {
		if (this.page != null) {
			ICConfigurationDescription curConfDesc = getResDesc().getConfiguration();
//			if (myLastSavedConfdesc != curConfDesc) {
//				if (myLastSavedConfdesc != null) {
//					setDescription(myLastSavedConfdesc, getFromScreen());
//				}
//				myLastSavedConfdesc = curConfDesc;
//			}
			return curConfDesc;
		}
		return null;
	}

	@Override
	protected void performOK() {
		// Get the project description
		ICConfigurationDescription confDesc = getConfdesc();
		ICProjectDescription projDesc = confDesc.getProjectDescription();


		// Copy local info to sloeber project and clean local info up
		for (ICConfigurationDescription curConfDesc : projDesc.getConfigurations()) {
			updateSloeber(curConfDesc);
			clearSessionProperties(curConfDesc);
		}
		super.performOK();
	}

	protected Object getDescription(ICConfigurationDescription confDesc) {
		if (confDesc == null) {
			// This is the case when a new project wizard is used
			if (myLocalObject == null) {
				myLocalObject = getnewDefaultObject();
			}
			return myLocalObject;
		}
		// Now we are sure we are in project properties->arduino
		Object storedDesc = confDesc.getSessionProperty(sloeberQualifiedName);

		if (storedDesc == null) {
			// this should not happen
			storedDesc = getnewDefaultObject();
			return storedDesc;
		}
		// Below is some wierd code to handle the creation of new configurations
		// The description is a pointer copy but we need a real copy
		// We assume in that case that SloeberProject doesn't know this config
		// so if sloeberProject doesn't know the config we make a copy
		Object sloeberDesc = getFromSloeber(confDesc);
		if (sloeberDesc == null) {
			Object copyDesc = makeCopy(storedDesc);
			confDesc.setSessionProperty(sloeberQualifiedName, copyDesc);
			return copyDesc;
		}
		return storedDesc;
	}

	protected void setDescription(ICConfigurationDescription confDesc, Object theDescription) {
		confDesc.setSessionProperty(sloeberQualifiedName, makeCopy(theDescription));
	}

	public SloeberCpropertyTab() {
		super();
	}

	@Override
	public void handleTabEvent(int kind, Object data) {
		switch (kind) {
		case ICPropertyTab.OK:
			if (canBeVisible())
				performOK();
			break;
		case ICPropertyTab.APPLY:
			if (canBeVisible())
				performApply(getResDesc(), (ICResourceDescription) data);
			break;
		case ICPropertyTab.CANCEL:
			if (canBeVisible())
				performCancel();
			break;
		case ICPropertyTab.DEFAULTS:
			if (canBeVisible() /* && getResDesc() != null */) {
				updateData(getResDesc());
				performDefaults();
			}
			break;
		case ICPropertyTab.UPDATE:
//			Object description = getDescription(getConfdesc());
			updateScreen();
			break;
		case ICPropertyTab.DISPOSE:
			dispose();
			break;
		case ICPropertyTab.VISIBLE:
			if (canSupportMultiCfg() || !page.isMultiCfg()) {
				if (canBeVisible()) {
					setVisible(data != null);
					setButtonVisible(data != null);
				} else
					setVisible(false);
			} else
				setAllVisible(false, null);
			break;
		case ICPropertyTab.SET_ICON:
			icon = (Image) data;
			break;
		default:
			break;
		}
	}

	@Override
	protected void updateData(ICResourceDescription cfg) {
		// updateScreen(getDescription(cfg.getConfiguration()));
		updateScreen();
	}

	@Override
	public boolean canBeVisible() {
		return true;
	}

	@Override
	protected void updateButtons() {
		// nothing to do here

	}

}