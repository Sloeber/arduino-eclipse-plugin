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
 * $Id: SectionGDBServerTool.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.avreclipse.core.targets.IGDBServerTool;
import it.baeyens.avreclipse.core.targets.ITargetConfigConstants;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;
import it.baeyens.avreclipse.core.targets.ToolManager;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class SectionGDBServerTool extends AbstractTCSectionPart implements ITargetConfigConstants {

	private Combo						fGDBServerToolCombo;

	/** Reverse mapping of gdbserver tool name to id. */
	final private Map<String, String>	fMapNameToId	= new HashMap<String, String>();

	private final static String[]		PART_ATTRS		= new String[] { ATTR_GDBSERVER_ID };
	private final static String[]		PART_DEPENDS	= new String[] { ATTR_MCU };

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getTitle()
	 */
	@Override
	protected String getTitle() {
		return "GDBServer Tool";
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getDescription()
	 */
	@Override
	protected String getDescription() {
		return "External Tool that acts as a server for GDB, required for debugging.";
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.AbstractTargetConfigurationEditorPart#getPartAttributes
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
		// The Programmer Tool Combo
		// 
		Label label = toolkit.createLabel(parent, "GDBServer Tool:");
		label.setLayoutData(new TableWrapData(TableWrapData.FILL, TableWrapData.MIDDLE));

		fGDBServerToolCombo = new Combo(parent, SWT.READ_ONLY);
		toolkit.adapt(fGDBServerToolCombo, true, true);
		fGDBServerToolCombo.setLayoutData(new TableWrapData(TableWrapData.LEFT, TableWrapData.TOP));
		fGDBServerToolCombo.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				ITargetConfigurationWorkingCopy tcwc = getTargetConfiguration();
				String name = fGDBServerToolCombo.getText();
				String id = fMapNameToId.get(name);

				tcwc.setGDBServerTool(id);

				getManagedForm().dirtyStateChanged();
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	@Override
	public void refreshSectionContent() {

		ToolManager manager = ToolManager.getDefault();
		ITargetConfigurationWorkingCopy wc = getTargetConfiguration();

		// Get the list of GDBServer Tools and fill the name -> id map
		fMapNameToId.clear();
		List<String> allgdbserverids = manager.getAllTools(ToolManager.AVRGDBSERVER);
		for (String id : allgdbserverids) {
			String name = manager.getToolName(id);
			fMapNameToId.put(name, id);
		}

		// Sort the tools alphabetically
		Set<String> nameset = fMapNameToId.keySet();
		String[] allnames = nameset.toArray(new String[nameset.size()]);
		Arrays.sort(allnames, new Comparator<String>() {
			// Custom Comparator to ignore upper/lower case
			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		});

		// Get the id of the currently selected gdbserver (or the default gdbserver if no gdbserver
		// has been set in the hardware configuration.
		IGDBServerTool currenttool = wc.getGDBServerTool();
		String currentid = currenttool != null ? currenttool.getId() : DEF_GDBSERVER_ID;

		// finally tell the fProgrammerToolsCombo about the new list but keep the previously
		// selected GDBServer Tool
		fGDBServerToolCombo.setItems(allnames);
		fGDBServerToolCombo.setVisibleItemCount(Math.min(allnames.length, 25));

		fGDBServerToolCombo.setText(manager.getToolName(currentid));
	}
}
