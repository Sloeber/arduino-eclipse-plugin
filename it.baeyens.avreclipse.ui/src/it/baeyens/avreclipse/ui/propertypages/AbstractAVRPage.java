/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: AbstractAVRPage.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;

import java.util.List;

import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.cdt.ui.newui.ICPropertyTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PropertyPage;


/**
 * This is the parent for all AVR Project property pages.
 * <p>
 * This class extends CDT AbstractPage to participate in the build configuration
 * handling.
 * </p>
 * <p>
 * It acts as an interface to the {@link ProjectPropertyManager}, which manages
 * the list of all {@link AVRProjectProperties} of the current project. It also
 * maintains the current status of the "per Config" flag and informs all other
 * registered AVR Pages when the flag is changed via the
 * {@link AbstractAVRPage#setPerConfig(boolean)} method.
 * </p>
 * 
 * @see AbstractPage
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public abstract class AbstractAVRPage extends AbstractPage {

	private final static String TEXT_COPYBUTTON = "Copy &Project Settings";

	/** The configuration selection group from the AbstractPage class */
	private Group fConfigGroup;

	/** The "Copy from Project" Button */
	private Button fCopyButton;

	/** The ProjectPropertyManager for the current project */
	protected ProjectPropertyManager fPropertiesManager = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#contentForCDT(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void contentForCDT(Composite composite) {

		// We override this method to get a reference to the configuration
		// selection group.
		// This is a hack, but as far as I can see this is the only way to get
		// the group without reimplementing most of the AbstractPage class.

		super.contentForCDT(composite);

		// Get the configuration selection group and set its visibility to the
		// current setting of the "per config" flag.
		fConfigGroup = findFirstGroup(composite);
		loadPropertiesManager();
		internalSetPerConfig(fPropertiesManager.isPerConfig());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#contributeButtons(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void contributeButtons(Composite parent) {

		// Add a "Copy Project Settings" Button in addition to the "Default" and
		// "Apply" Buttons from the PreferencePage superclass.
		// This button is only actived in the "per Config" mode and will copy
		// the project properties to the current configuration.
		fCopyButton = new Button(parent, SWT.NONE);
		fCopyButton.setText(TEXT_COPYBUTTON);
		fCopyButton.setEnabled(isPerConfig());

		fCopyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performCopy();
			}
		});

		// Increase the number of columns in the parent layout
		((GridLayout) parent.getLayout()).numColumns++;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#performCancel()
	 */
	@Override
	public boolean performCancel() {
		// First remove any modifications made to the AVR properties,
		// then let the superclass handle the CDT specific stuff.
		AVRPropertyPageManager.performCancel(this);
		return super.performCancel();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#performOk()
	 */
	@Override
	public boolean performOk() {
		// First save and sync the AVR specific Properties,
		// then let the superclass handle the CDT specific modifications.
		AVRPropertyPageManager.performOK(this, getCfgsEditable());
		return super.performOk();
	}

	/**
	 * Notifies that the "Copy from Project" button has been pressed.
	 */
	public void performCopy() {
		// Get the per project properties and send them to all out tabs.
		AVRProjectProperties projectprops = fPropertiesManager
				.getProjectProperties();
		if (!noContentOnPage && displayedConfig)
			forEach(AbstractAVRPropertyTab.COPY, projectprops);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#setVisible(boolean)
	 */
	@Override
	public void setVisible(boolean visible) {
		// I override this method just to make sure that the
		// ProjectPropertiesManager has been loaded.
		// There were some problems before.
		if (visible) {
			loadPropertiesManager();
		}
		super.setVisible(visible);
	}

	/**
	 * Returns the value of the "per config" flag for this project.
	 * 
	 * @return <code>true</code> if each build configuration has its own
	 *         properties.
	 */
	protected boolean isPerConfig() {
		loadPropertiesManager();
		return fPropertiesManager.isPerConfig();
	}

	/**
	 * Set the project "per config" flag.
	 * <p>
	 * This method will set the flag and inform all its tabs and all other AVR
	 * Property pages registered with the page manager about the change.
	 * </p>
	 * 
	 * @param flag
	 *            <code>true</code> to enable "per config" settings.
	 */
	protected void setPerConfig(boolean flag) {
		// Test if flag value has changed to avoid the overhead of informing
		// everyone for non-changes.
		if (flag == isPerConfig()) {
			return;
		}

		// inform all open AVRAbstractPages (including ourself) about the
		// changed "per config" flag.
		List<PropertyPage> allpages = AVRPropertyPageManager.getPages();
		for (PropertyPage page : allpages) {
			if ((page != null) && (page instanceof AbstractAVRPage)) {
				AbstractAVRPage ap = (AbstractAVRPage) page;
				ap.internalSetPerConfig(flag);
			}
		}
	}

	/**
	 * Set the "per config" flag for this page and inform all child tabs about
	 * the change.
	 * 
	 * @param flag
	 *            New value of the "per config" flag.
	 */
	private void internalSetPerConfig(boolean flag) {
		fPropertiesManager.setPerConfig(flag);
		if (fConfigGroup != null) {
			setEnabled(fConfigGroup, flag);
		}

		// Inform all our Tabs about the change.
		// We pass a ICResourceDescription, even if it is not used.
		forEach(ICPropertyTab.UPDATE, getResDesc());

		// Enable / disable the "Copy from Project" Button
		if (fCopyButton != null) {
			fCopyButton.setEnabled(flag);
		}
	}

	/**
	 * get the Properties Manager from the page manager.
	 */
	private void loadPropertiesManager() {
		// This call makes sure that the internal value for the getProject()
		// call below has been initialized
		checkElement();

		// Get the Project Properties Manager (if it has not yet been loaded by
		// another page)
		fPropertiesManager = AVRPropertyPageManager.getPropertyManager(this,
				getProject());
	}

	/**
	 * Get the configuration selection group from the parent.
	 * <p>
	 * This is a hack to get a reference to the configuration selection group of
	 * a standard {@link AbstractPage}. The returned reference can be used to
	 * enable/disable the group as required.
	 * </p>
	 * 
	 * @param parent
	 *            a composite having the configuration selection as its first
	 *            group.
	 * @return A reference to the first group within the given composite
	 */
	private Group findFirstGroup(Composite parent) {
		Control[] children = parent.getChildren();
		if (children == null || children.length == 0) {
			return null;
		}
		for (Control child : children) {
			if (child instanceof Group) {
				return (Group) child;
			}
			if (child instanceof Composite) {
				Group recursive = findFirstGroup((Composite) child);
				if (recursive != null) {
					return recursive;
				}
			}
		}

		return null;
	}

	/**
	 * Enable / Disable the given Composite.
	 * 
	 * @param compo
	 *            A <code>Composite</code> with some controls.
	 * @param value
	 *            <code>true</code> to enable, <code>false</code> to disable
	 *            the given group.
	 */
	private void setEnabled(Composite compo, boolean value) {
		Control[] children = compo.getChildren();
		for (Control child : children) {
			child.setEnabled(value);
		}
	}

}
