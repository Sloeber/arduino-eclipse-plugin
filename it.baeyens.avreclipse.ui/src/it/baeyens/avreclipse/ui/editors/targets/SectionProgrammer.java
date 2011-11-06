/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: SectionProgrammer.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.avreclipse.core.targets.HostInterface;
import it.baeyens.avreclipse.core.targets.IProgrammer;
import it.baeyens.avreclipse.core.targets.ITargetConfigConstants;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class SectionProgrammer extends AbstractTCSectionPart implements ITargetConfigConstants {

	private Combo								fProgrammersCombo;

	private Combo								fHostPortCombo;

	/** Reverse mapping of programmer description to id. */
	final private Map<String, String>			fMapDescToId		= new HashMap<String, String>();

	/** Reverse mapping of host interface description to host interface. */
	final private Map<String, HostInterface>	fMapDescToHostPort	= new HashMap<String, HostInterface>();

	private final static String[]				PART_ATTRS			= new String[] {
			ATTR_PROGRAMMER_ID, ATTR_HOSTINTERFACE					};
	private final static String[]				PART_DEPENDS		= new String[] {
			ATTR_PROGRAMMER_TOOL_ID, ATTR_GDBSERVER_ID				};

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getTitle()
	 */
	@Override
	protected String getTitle() {
		return "Programmer Hardware / Interface";
	}

	/*
	 * (non-Javadoc)
	 * @see	 * it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getDescription()
	 */
	@Override
	protected String getDescription() {
		return null; // TODO: add a description
	}

	/*
	 * (non-Javadoc)
	 * @see	 * it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getPartAttributes
	 * ()
	 */
	@Override
	public String[] getPartAttributes() {
		return PART_ATTRS;
	}

	/*
	 * (non-Javadoc)
	 * @seeit.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#
	 * getDependentAttributes()
	 */
	@Override
	protected String[] getDependentAttributes() {
		return PART_DEPENDS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void createSectionContent(Composite parent, FormToolkit toolkit) {

		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 12;
		parent.setLayout(layout);

		//
		// The Programmers Combo
		// 
		Label label = toolkit.createLabel(parent, "Programmer:");
		label.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.MIDDLE));

		fProgrammersCombo = new Combo(parent, SWT.READ_ONLY);
		toolkit.adapt(fProgrammersCombo, true, true);
		fProgrammersCombo.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP));
		fProgrammersCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();
				String description = fProgrammersCombo.getText();
				String id = fMapDescToId.get(description);
				IProgrammer programmer = tcwc.getProgrammer(id);

				tcwc.setAttribute(ATTR_PROGRAMMER_ID, id);
				fProgrammersCombo.setToolTipText(programmer.getAdditionalInfo());

				updateHostInterfaceCombo(programmer);

				getManagedForm().dirtyStateChanged();
			}
		});

		//
		// The host port selector combo
		//
		label = toolkit.createLabel(parent, "Host interface:");
		label.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.MIDDLE));

		fHostPortCombo = new Combo(parent, SWT.READ_ONLY);
		toolkit.adapt(fHostPortCombo, true, true);
		fHostPortCombo.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP));
		fHostPortCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String description = fHostPortCombo.getText();
				HostInterface hi = fMapDescToHostPort.get(description);
				getTargetConfiguration().setAttribute(ATTR_HOSTINTERFACE, hi.name());

				// Ensure that the HostPortCombo is still visible after the layout reflow caused
				// by the new layout
				IManagedForm form = getManagedForm();
				form.getForm().showControl(fProgrammersCombo);
				form.dirtyStateChanged();
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	@Override
	public void refreshSectionContent() {

		// Get the list of valid Programmers and fill the description -> id map
		fMapDescToId.clear();
		Set<String> allprogrammers = getTargetConfiguration().getAllProgrammers(true);

		for (String id : allprogrammers) {
			IProgrammer progger = getTargetConfiguration().getProgrammer(id);
			String description = progger.getDescription();
			fMapDescToId.put(description, id);
		}

		// Check if the currently selected programmer is still in the list
		String currentprogrammerid = getTargetConfiguration().getAttribute(ATTR_PROGRAMMER_ID);
		if (!fMapDescToId.containsValue(currentprogrammerid)) {
			// No -- The Programmer is not supported by the current config.
			// Add the current programmer back to the list.
			// This prevents the combo from becoming empty at the cost of one
			// 'invalid' programmer in the list
			IProgrammer currentprogrammer = getTargetConfiguration().getProgrammer(
					currentprogrammerid);
			fMapDescToId.put(currentprogrammer.getDescription(), currentprogrammerid);

		}

		// Get all descriptions and sort them alphabetically
		Set<String> descset = fMapDescToId.keySet();
		String[] alldescs = descset.toArray(new String[descset.size()]);
		Arrays.sort(alldescs, new Comparator<String>() {
			// Custom Comparator to ignore upper/lower case
			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		});

		// Tell the fProgrammersCombo about the new list but keep the previously selected
		// Programmer
		fProgrammersCombo.setItems(alldescs);
		fProgrammersCombo.setVisibleItemCount(Math.min(alldescs.length, 25));

		IProgrammer currentprogrammer = getTargetConfiguration().getProgrammer(currentprogrammerid);
		fProgrammersCombo.setText(currentprogrammer.getDescription());
		fProgrammersCombo.setToolTipText(currentprogrammer.getAdditionalInfo());

		// Now set the host interface
		updateHostInterfaceCombo(currentprogrammer);

		// Finally show an error if the Programmer is not supported by either tool.
		refreshMessages();
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTCSectionPart#refreshMessages()
	 */
	@Override
	protected void refreshMessages() {
		validate(ATTR_PROGRAMMER_ID, fProgrammersCombo);
	}

	/**
	 * @param programmer
	 */
	private void updateHostInterfaceCombo(IProgrammer programmer) {
		HostInterface[] availableHIs = programmer.getHostInterfaces();

		// update the combo to only show available interfaces
		fMapDescToHostPort.clear();
		for (HostInterface hi : availableHIs) {
			fMapDescToHostPort.put(hi.toString(), hi);
		}
		String[] allhostinterfaces = fMapDescToHostPort.keySet().toArray(
				new String[fMapDescToHostPort.size()]);
		fHostPortCombo.setItems(allhostinterfaces);
		fHostPortCombo.setEnabled(allhostinterfaces.length > 1);

		// Check if the currently selected port is still valid
		String currentHI = getTargetConfiguration().getAttribute(ATTR_HOSTINTERFACE);
		for (HostInterface hi : availableHIs) {
			if (hi.name().equals(currentHI)) {
				// The set port is valid. Just set the name and be done
				fHostPortCombo.setText(hi.toString());
				return;
			}
		}

		// The selected programmer uses a different host interface.
		// Update the combo and the target configuration
		HostInterface newHI = availableHIs[0];
		fHostPortCombo.setText(newHI.toString());
		getTargetConfiguration().setAttribute(ATTR_HOSTINTERFACE, newHI.name());

	}
}
