package io.sloeber.ui.project.properties;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.widgets.Composite;

import io.sloeber.core.api.ISloeberConfiguration;

public abstract class SloeberCpropertyTab extends AbstractCPropertyTab {

	protected ISloeberConfiguration mySloeberCfg = null;

	/**
	 * update the screen based on the data stored in the properties if refreshData
	 * is true the data has changed and needs to be retrieved
	 */
	protected abstract void updateScreen(boolean refreshData);

	@Override
	public void createControls(Composite parent, ICPropertyProvider provider) {
		super.createControls(parent, provider);
		mySloeberCfg = ISloeberConfiguration.getConfig(getConfdesc());
	}

	/**
	 * Get the configuration we are currently working in. The configuration is null
	 * if we are in the create sketch wizard.
	 *
	 * @return the configuration to save info into
	 */
	protected ICConfigurationDescription getConfdesc() {
		if (this.page != null) {
			return getResDesc().getConfiguration();
		}
		return null;
	}

	public SloeberCpropertyTab() {
		super();
	}

	@Override
	protected void updateData(ICResourceDescription cfg) {
		mySloeberCfg = ISloeberConfiguration.getConfig(getConfdesc());
		updateScreen(true);
	}


	@Override
	protected void updateButtons() {
		// nothing to do here

	}
}


//@Override
//public void handleTabEvent(int kind, Object data) {
//	switch (kind) {
//	case ICPropertyTab.OK:
//		if (canBeVisible())
//			performOK();
//		break;
//	case ICPropertyTab.APPLY:
//		if (canBeVisible())
//			performApply(getResDesc(), (ICResourceDescription) data);
//		break;
//	case ICPropertyTab.CANCEL:
//		if (canBeVisible())
//			performCancel();
//		break;
//	case ICPropertyTab.DEFAULTS:
//		if (canBeVisible() /* && getResDesc() != null */) {
//			updateData(getResDesc());
//			performDefaults();
//		}
//		break;
//	case ICPropertyTab.UPDATE:
//		mySloeberCfg = ISloeberConfiguration.getConfig(getConfdesc());
//		updateScreen(true);
//		break;
//	case ICPropertyTab.DISPOSE:
//		dispose();
//		break;
//	case ICPropertyTab.VISIBLE:
//		if (canSupportMultiCfg() || !page.isMultiCfg()) {
//			if (canBeVisible()) {
//				setVisible(data != null);
//				setButtonVisible(data != null);
//			} else
//				setVisible(false);
//		} else
//			setAllVisible(false, null);
//		break;
//	case ICPropertyTab.SET_ICON:
//		icon = (Image) data;
//		break;
//	default:
//		break;
//	}
//}