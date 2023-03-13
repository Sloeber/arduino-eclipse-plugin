/*******************************************************************************
 * Copyright (c) 2007, 2011 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 * IBM Corporation
 * Dmitry Kozlov (CodeSourcery) - save build output preferences (bug 294106)
 * Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 *******************************************************************************/
package io.sloeber.autobuild.cdt.ui;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;

import io.sloeber.schema.api.IConfiguration;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class BuilderSettingsTab extends AbstractAutoBuildPropertyTab {
	// Widgets
	//1
	private Button b_useDefaultBuildCommand;
	private Combo c_builderType;
	private Text t_buildCmd;
	//2
	private Button b_genMakefileAuto;
	private Button b_expandVars;
	//5
	private Text t_dir;
	private Button b_dirWsp;
	private Button b_dirFile;
	private Button b_dirVars;
	private Group group_dir;

	private boolean canModify = true;

	@Override
	public void createControls(Composite parent) {
		super.createControls(parent);
		usercomp.setLayout(new GridLayout(1, false));

		// Builder group
		Group g1 = setupGroup(usercomp, Messages.BuilderSettingsTab_0, 3, GridData.FILL_HORIZONTAL);
		setupLabel(g1, Messages.BuilderSettingsTab_1, 1, GridData.BEGINNING);
		c_builderType = new Combo(g1, SWT.READ_ONLY | SWT.DROP_DOWN | SWT.BORDER);
		setupControl(c_builderType, 2, GridData.FILL_HORIZONTAL);
		c_builderType.add(Messages.BuilderSettingsTab_2);
		c_builderType.add(Messages.BuilderSettingsTab_3);
		c_builderType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				enableInternalBuilder(c_builderType.getSelectionIndex() == 1);
				updateButtons();
			}
		});

		b_useDefaultBuildCommand = setupCheck(g1, Messages.BuilderSettingsTab_4, 3, GridData.BEGINNING);

		setupLabel(g1, Messages.BuilderSettingsTab_5, 1, GridData.BEGINNING);
		t_buildCmd = setupBlock(g1, b_useDefaultBuildCommand);
		t_buildCmd.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!canModify)
					return;
				String buildCommand = t_buildCmd.getText().trim();
//				if (!buildCommand.equals(bldr.getCommand())) {
//					setCommand(buildCommand);
//				}
			}
		});

		setupLabel(g1, Messages.BuilderSettingsTab_Configure_Build_Arguments_In_the_Behavior_tab, 2,
				GridData.BEGINNING);

		Group g2 = setupGroup(usercomp, Messages.BuilderSettingsTab_6, 2, GridData.FILL_HORIZONTAL);
		((GridLayout) (g2.getLayout())).makeColumnsEqualWidth = true;

		b_genMakefileAuto = setupCheck(g2, Messages.BuilderSettingsTab_7, 1, GridData.BEGINNING);
		b_expandVars = setupCheck(g2, Messages.BuilderSettingsTab_8, 1, GridData.BEGINNING);

		// Build location group
		group_dir = setupGroup(usercomp, Messages.BuilderSettingsTab_21, 2, GridData.FILL_HORIZONTAL);
		setupLabel(group_dir, Messages.BuilderSettingsTab_22, 1, GridData.BEGINNING);
		t_dir = setupText(group_dir, 1, GridData.FILL_HORIZONTAL);
		t_dir.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (canModify)
					setBuildPath(t_dir.getText());
			}
		});
		Composite c = new Composite(group_dir, SWT.NONE);
		setupControl(c, 2, GridData.FILL_HORIZONTAL);
		GridLayout f = new GridLayout(4, false);
		c.setLayout(f);
		Label dummy = new Label(c, 0);
		dummy.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		b_dirWsp = setupBottomButton(c, WORKSPACEBUTTON_NAME);
		b_dirFile = setupBottomButton(c, FILESYSTEMBUTTON_NAME);
		b_dirVars = setupBottomButton(c, VARIABLESBUTTON_NAME);
	}

	private void setManagedBuild(boolean enable) {
		setManagedBuildOn(enable);
		page.informPages(MANAGEDBUILDSTATE, null);
		updateButtons();
	}

	/**
	 * sets widgets states
	 */
	@Override
	protected void updateButtons() {
		//bldr = icfg.getEditableBuilder();

		canModify = false; // avoid extra update from modifyListeners
		int[] extStates = BuildBehaviourTab.calc3states(page,  0);

//		b_genMakefileAuto.setEnabled(icfg.supportsBuild(true));
		if (extStates == null) { // no extended states available
//			BuildBehaviourTab.setTriSelection(b_genMakefileAuto, bldr.isManagedBuildOn());
//			BuildBehaviourTab.setTriSelection(b_useDefaultBuildCommand, bldr.isDefaultBuildCmdOnly());
//			// b_expandVars.setGrayed(false);
//			if (!bldr.canKeepEnvironmentVariablesInBuildfile())
//				b_expandVars.setEnabled(false);
//			else {
//				b_expandVars.setEnabled(true);
//				BuildBehaviourTab.setTriSelection(b_expandVars, !bldr.keepEnvironmentVariablesInBuildfile());
//			}
		} else {
			BuildBehaviourTab.setTriSelection(b_genMakefileAuto, extStates[0]);
			BuildBehaviourTab.setTriSelection(b_useDefaultBuildCommand, extStates[4]);
			if (extStates[2] != BuildBehaviourTab.TRI_YES)
				b_expandVars.setEnabled(false);
			else {
				b_expandVars.setEnabled(true);
				BuildBehaviourTab.setTriSelection(b_expandVars, extStates[3]);
			}
		}
		c_builderType.select(isInternalBuilderEnabled() ? 1 : 0);
		c_builderType.setEnabled(canEnableInternalBuilder(true) && canEnableInternalBuilder(false));

		//t_buildCmd.setText(nonNull(icfg.getBuildCommand()));

		if (page.isMultiCfg()) {
			group_dir.setVisible(false);
		} else {
			group_dir.setVisible(true);
//			t_dir.setText(bldr.getBuildPath());
//			boolean mbOn = bldr.isManagedBuildOn();
//			t_dir.setEnabled(!mbOn);
//			b_dirVars.setEnabled(!mbOn);
//			b_dirWsp.setEnabled(!mbOn);
//			b_dirFile.setEnabled(!mbOn);
		}
		boolean external = (c_builderType.getSelectionIndex() == 0);

		b_useDefaultBuildCommand.setEnabled(external);
		t_buildCmd.setEnabled(external);
		((Control) t_buildCmd.getData()).setEnabled(external & !b_useDefaultBuildCommand.getSelection());

//		b_genMakefileAuto.setEnabled(external && icfg.supportsBuild(true));
		if (b_expandVars.getEnabled())
			b_expandVars.setEnabled(external && b_genMakefileAuto.getSelection());

		if (external) { // just set relatet text widget state,
			checkPressed(b_useDefaultBuildCommand, false); // do not update
		}
		canModify = true;
	}

	private Button setupBottomButton(Composite c, String name) {
		Button b = new Button(c, SWT.PUSH);
		b.setText(name);
		GridData fd = new GridData(GridData.CENTER);
		fd.minimumWidth = BUTTON_WIDTH;
		b.setLayoutData(fd);
		b.setData(t_dir);
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				buttonVarPressed(event);
			}
		});
		return b;
	}

	/**
	 * Sets up text + corresponding button
	 * Checkbox can be implemented either by Button or by TriButton
	 */
	private Text setupBlock(Composite c, Control check) {
		Text t = setupText(c, 1, GridData.FILL_HORIZONTAL);
		Button b = setupButton(c, VARIABLESBUTTON_NAME, 1, GridData.END);
		b.setData(t); // to get know which text is affected
		t.setData(b); // to get know which button to enable/disable
		b.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				buttonVarPressed(event);
			}
		});
		if (check != null)
			check.setData(t);
		return t;
	}

	/*
	 * Unified handler for "Variables" buttons
	 */
	private void buttonVarPressed(SelectionEvent e) {
		Widget b = e.widget;
		if (b == null || b.getData() == null)
			return;
		if (b.getData() instanceof Text) {
			String x = null;
			if (b.equals(b_dirWsp)) {
				x = getWorkspaceDirDialog(usercomp.getShell(), EMPTY_STR);
				if (x != null)
					((Text) b.getData()).setText(x);
			} else if (b.equals(b_dirFile)) {
				x = getFileSystemDirDialog(usercomp.getShell(), EMPTY_STR);
				if (x != null)
					((Text) b.getData()).setText(x);
			} else {
				x = AbstractCPropertyTab.getVariableDialog(usercomp.getShell(), getResDesc().getConfiguration());
				if (x != null)
					((Text) b.getData()).insert(x);
			}
		}
	}

	@Override
	public void checkPressed(SelectionEvent e) {
		checkPressed((Control) e.widget, true);
		updateButtons();
	}

	private void checkPressed(Control b, boolean needUpdate) {
		if (b == null)
			return;

		boolean val = false;
		if (b instanceof Button)
			val = ((Button) b).getSelection();

		if (b.getData() instanceof Text) {
			Text t = (Text) b.getData();
			if (b == b_useDefaultBuildCommand) {
				val = !val;
			}
			t.setEnabled(val);
			if (t.getData() != null && t.getData() instanceof Control) {
				Control c = (Control) t.getData();
				c.setEnabled(val);
			}
		}
		// call may be used just to set text state above
		// in this case, settings update is not required
		if (!needUpdate)
			return;

		if (b == b_useDefaultBuildCommand) {
			setUseDefaultBuildCmd(!val);
		} else if (b == b_genMakefileAuto) {
			setManagedBuild(val);
		} else if (b == b_expandVars) {
//			if (bldr.canKeepEnvironmentVariablesInBuildfile())
//				setKeepEnvironmentVariablesInBuildfile(!val);
		}
	}

	/**
	 * Return an empty string is parameter is null
	 */
	private String nonNull(String maybeNullString) {
		if (maybeNullString == null) {
			return EMPTY_STR;
		}
		return maybeNullString;
	}

	/**
	 * Performs common settings for all controls
	 * (Copy from config to widgets)
	 */
	@Override
	public void updateData(ICResourceDescription cfgd) {
		if (cfgd == null)
			return;
	//	icfg = getCfg(cfgd.getConfiguration());
		updateButtons();
	}

	@Override
	public void performApply(ICResourceDescription src, ICResourceDescription dst) {
		BuildBehaviourTab.apply(src, dst, page.isMultiCfg());
	}

	// This page can be displayed for project only
	@Override
	public boolean canBeVisible() {
		return page.isForProject() || page.isForPrefs();
	}

	@Override
	public void setVisible(boolean b) {
		super.setVisible(b);
	}

	@Override
	protected void performDefaults() {
//		if (icfg instanceof IMultiConfiguration) {
//			IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
//			for (int i = 0; i < cfs.length; i++) {
////				IBuilder b = cfs[i].getEditableBuilder();
////				BuildBehaviourTab.copyBuilders(b.getSuperClass(), b);
//			}
//		} 
////		else
////			BuildBehaviourTab.copyBuilders(bldr.getSuperClass(), bldr);
//		updateData(getResDesc());
	}

	private boolean canEnableInternalBuilder(boolean v) {
//		if (icfg instanceof Configuration)
//			return ((Configuration) icfg).canEnableInternalBuilder(v);
//		if (icfg instanceof IMultiConfiguration)
//			return ((IMultiConfiguration) icfg).canEnableInternalBuilder(v);
		return false;
	}

	private void enableInternalBuilder(boolean v) {
//		if (icfg instanceof Configuration)
//			((Configuration) icfg).enableInternalBuilder(v);
//		if (icfg instanceof IMultiConfiguration)
//			((IMultiConfiguration) icfg).enableInternalBuilder(v);
	}

	private boolean isInternalBuilderEnabled() {
//		if (icfg instanceof Configuration)
//			return ((Configuration) icfg).isInternalBuilderEnabled();
//		if (icfg instanceof IMultiConfiguration)
//			return ((MultiConfiguration) icfg).isInternalBuilderEnabled();
		return false;
	}

	private void setUseDefaultBuildCmd(boolean val) {
//		try {
//			if (icfg instanceof IMultiConfiguration) {
//				IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
//				for (int i = 0; i < cfs.length; i++) {
//					IBuilder b = cfs[i].getEditableBuilder();
//					if (b != null)
//						b.setUseDefaultBuildCmdOnly(val);
//				}
//			} 
//			else {
//				icfg.getEditableBuilder().setUseDefaultBuildCmdOnly(val);
//			}
//		} catch (CoreException e) {
//			e.printStackTrace();
////			ManagedBuilderUIPlugin.log(e);
//		}
	}

	private void setKeepEnvironmentVariablesInBuildfile(boolean val) {
//		if (icfg instanceof IMultiConfiguration) {
//			IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
//			for (int i = 0; i < cfs.length; i++) {
//				IBuilder b = cfs[i].getEditableBuilder();
//				if (b != null)
//					b.setKeepEnvironmentVariablesInBuildfile(val);
//			}
//		} else {
//			icfg.getEditableBuilder().setKeepEnvironmentVariablesInBuildfile(val);
//		}
	}

	private void setCommand(String buildCommand) {
//		if (icfg instanceof IMultiConfiguration) {
//			IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
//			for (int i = 0; i < cfs.length; i++) {
//				IBuilder b = cfs[i].getEditableBuilder();
//				b.setCommand(buildCommand);
//			}
//		} else {
//			icfg.getEditableBuilder().setCommand(buildCommand);
//		}
	}

	private void setBuildPath(String path) {
//		if (icfg instanceof IMultiConfiguration) {
//			IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
//			for (int i = 0; i < cfs.length; i++) {
//				IBuilder b = cfs[i].getEditableBuilder();
//				b.setBuildPath(path);
//			}
//		} else {
//			icfg.getEditableBuilder().setBuildPath(path);
//		}
	}

	private void setManagedBuildOn(boolean on) {
//		try {
//			if (icfg instanceof IMultiConfiguration) {
//				IConfiguration[] cfs = (IConfiguration[]) ((IMultiConfiguration) icfg).getItems();
//				for (int i = 0; i < cfs.length; i++) {
//					IBuilder b = cfs[i].getEditableBuilder();
//					b.setManagedBuildOn(on);
//				}
//			} else {
//				icfg.getEditableBuilder().setManagedBuildOn(on);
//			}
//		} catch (CoreException e) {
//			e.printStackTrace();
////			ManagedBuilderUIPlugin.log(e);
//		}
	}
}
