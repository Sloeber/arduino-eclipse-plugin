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

import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.ui.Activator;

public abstract class SloeberCpropertyTab extends AbstractCPropertyTab {

	private QualifiedName sloeberQualifiedName = new QualifiedName(Activator.NODE_ARDUINO, getQualifierString());
	protected SloeberProject mySloeberProject = null;
	private ICConfigurationDescription prefConDescUser = null;
	private ICConfigurationDescription prefConDescSystem = null;

	private void clearSessionProperties(ICConfigurationDescription confDesc) {
		Object sessionProperty = confDesc.getSessionProperty(sloeberQualifiedName);
		if (null != sessionProperty) {
			confDesc.setSessionProperty(sloeberQualifiedName, null);
		}
	}

	protected abstract String getQualifierString();

	protected abstract void updateScreen(Object object);

	protected abstract Object getFromScreen();

	protected abstract void updateSloeber(ICConfigurationDescription confDesc, Object theObjectToStore);

	protected abstract Object getFromSloeber(ICConfigurationDescription confDesc);

	protected abstract Object makeCopy(Object srcObject);

	protected abstract Object getnewDefaultObject();

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		// make sure all configurations hold their compileDescription
		// This is needed so that when a copy is made
		// the root compile description is known
		ICProjectDescription projDesc = getConfdesc().getProjectDescription();
		mySloeberProject = SloeberProject.getSloeberProject(projDesc.getProject());
		for (ICConfigurationDescription curConfig : projDesc.getConfigurations()) {
			Object description = getFromSloeber(curConfig);
			if (description == null) {
				description = new CompileDescription();
			} else {
				description = makeCopy(description);
			}
			setDescription(curConfig, description);
		}
		// We can now safely assume myCompileDesc is not null
		// and confDesc.getSessionProperty(sloeberQualifiedName); is not null
	}

	@Override
	protected void performDefaults() {
		Object description = getnewDefaultObject();

		setDescription(getConfdesc(), description);
		updateScreen(description);
	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		ICConfigurationDescription confDesc = dst.getConfiguration();
		clearSessionProperties(confDesc);

		Object theObject = getFromScreen();
		setDescription(confDesc, theObject);
		updateSloeber(confDesc, theObject);
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
			if (prefConDescSystem != curConfDesc) {
				prefConDescUser = prefConDescSystem;
			}
			prefConDescSystem = curConfDesc;
			return curConfDesc;
		}
		return null;
	}

	@Override
	protected void performOK() {
		ICConfigurationDescription confDesc = getConfdesc();
		ICProjectDescription projDesc = confDesc.getProjectDescription();

		setDescription(confDesc, getFromScreen());
		for (ICConfigurationDescription curConfDesc : projDesc.getConfigurations()) {
			Object theObjectToStore = getDescription(curConfDesc);
			clearSessionProperties(curConfDesc);
			updateSloeber(curConfDesc, theObjectToStore);
		}
		super.performOK();
	}

	protected Object getDescription(ICConfigurationDescription confDesc) {
		Object storedDesc = confDesc.getSessionProperty(sloeberQualifiedName);
		// here is some wierd code to handle the creation of new configurations
		// The compile description is a copy in that case but SloeberProject doesn't
		// know this config
		// so if sloeberProject doesn't know the config we ùmake a copy
		if (storedDesc == null) {
			// this should not happen
			storedDesc = getnewDefaultObject();
		}
		Object sloeberDesc = getFromSloeber(confDesc);
		if (sloeberDesc == null) {
			Object copyDesc = makeCopy(storedDesc);
			confDesc.setSessionProperty(sloeberQualifiedName, copyDesc);
			return copyDesc;
		}
		return storedDesc;
	}

	protected void setDescription(ICConfigurationDescription confDesc, Object theDescription) {
		confDesc.setSessionProperty(sloeberQualifiedName, theDescription);
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
			if (prefConDescUser != null) {
				setDescription(prefConDescUser, getFromScreen());
			}
			Object description = getDescription(getConfdesc());
			updateScreen(description);
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
		updateScreen(getDescription(cfg.getConfiguration()));
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