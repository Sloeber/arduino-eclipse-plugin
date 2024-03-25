/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableStatus;
import org.eclipse.cdt.core.cdtvariables.IStorableCdtVariables;
import org.eclipse.cdt.core.cdtvariables.IUserVarSupplier;
import org.eclipse.cdt.core.model.util.CDTListComparator;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiItemsHolder;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.cdt.ui.newui.PrefPage_Abstract;
import org.eclipse.cdt.ui.newui.StringListModeControl;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.envvar.EnvVarOperationProcessor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLayoutData;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import io.sloeber.autoBuild.integrations.NewVarDialog;
import io.sloeber.autoBuild.ui.internal.Messages;

/**
 * displays the build macros for the given context
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class CPropertyVarsTab extends AbstractCPropertyTab {
    private static final String VALUE_DELIMITER = " || "; //$NON-NLS-1$

    private static final ICdtVariableManager vmgr = CCorePlugin.getDefault().getCdtVariableManager();
    private static final IUserVarSupplier fUserSup = CCorePlugin.getUserVarSupplier();
    private static final EnvCmp comparator = new EnvCmp();

    private ICConfigurationDescription cfgd = null;
    private IStorableCdtVariables prefvars = null;

    //currently the "CWD" and "PWD" macros are not displayed in UI
    private static final String fHiddenMacros[] = new String[] { "CWD", //$NON-NLS-1$
            "PWD" //$NON-NLS-1$
    };

    private boolean fShowSysMacros = false;
    private Set<String> fIncorrectlyDefinedMacrosNames = new HashSet<>();

    private TableViewer tv;
    private Label fStatusLabel;
    private StringListModeControl stringListModeControl;

    private static final String[] fEditableTableColumnProps = new String[] { "editable name", //$NON-NLS-1$
            "editable type", //$NON-NLS-1$
            "editable value", //$NON-NLS-1$
    };

    private static final String[] fTableColumnNames = new String[] { Messages.MacrosBlock_label_header_name,
            Messages.MacrosBlock_label_header_type, Messages.MacrosBlock_label_header_value, };

    private static final ColumnLayoutData[] fTableColumnLayouts = { new ColumnPixelData(100), new ColumnPixelData(100),
            new ColumnPixelData(250) };

    private class MacroContentProvider implements IStructuredContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
            return (Object[]) inputElement;
        }

        @Override
        public void dispose() {
        	//nothing to do
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        	//nothing to do
        }
    }

    private class MacroLabelProvider extends LabelProvider
            implements ITableLabelProvider, IFontProvider, ITableFontProvider, IColorProvider {
        @Override
        public Image getImage(Object element) {
            return null;
        }

        @Override
        public String getText(Object element) {
            return getColumnText(element, 0);
        }

        @Override
        public Font getFont(Object element) {
            return getFont(element, 0);
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            ICdtVariable var = (ICdtVariable) element;
            if (isUserVar(var))
                return BACKGROUND_FOR_USER_VAR;
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            ICdtVariable var = (ICdtVariable) element;
            switch (columnIndex) {
            case 0:
                return var.getName();
            case 1:
                switch (var.getValueType()) {
                case ICdtVariable.VALUE_PATH_FILE:
                    return Messages.MacrosBlock_label_type_path_file;
                case ICdtVariable.VALUE_PATH_FILE_LIST:
                    return Messages.MacrosBlock_label_type_path_file_list;
                case ICdtVariable.VALUE_PATH_DIR:
                    return Messages.MacrosBlock_label_type_path_dir;
                case ICdtVariable.VALUE_PATH_DIR_LIST:
                    return Messages.MacrosBlock_label_type_path_dir_list;
                case ICdtVariable.VALUE_PATH_ANY:
                    return Messages.MacrosBlock_label_type_path_any;
                case ICdtVariable.VALUE_PATH_ANY_LIST:
                    return Messages.MacrosBlock_label_type_path_any_list;
                case ICdtVariable.VALUE_TEXT:
                    return Messages.MacrosBlock_label_type_text;
                case ICdtVariable.VALUE_TEXT_LIST:
                    return Messages.MacrosBlock_label_type_text_list;
                default:
                    return "? " + var.getValueType(); //$NON-NLS-1$
                }
            case 2:
                return getString(var);
			default:
				break;
            }
            return EMPTY_STR;
        }

        @Override
        public Font getFont(Object element, int columnIndex) {
            ICdtVariable var = (ICdtVariable) element;
            if (columnIndex == 0 && isUserVar(var))
                return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
            return null;
        }

        @Override
        public Color getForeground(Object element) {
            if (fIncorrectlyDefinedMacrosNames.contains(((ICdtVariable) element).getName()))
                return JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR);
            return null;
        }
    }

    /*
     * called when the user macro selection was changed
     */
    private void handleSelectionChanged() {
        updateButtons();
    }

    @Override
    protected void updateButtons() {
        Object[] obs = ((IStructuredSelection) tv.getSelection()).toArray();
        boolean canEdit = false;
        boolean canDel = false;
        if (obs != null && obs.length > 0) {
            canEdit = (obs.length == 1);
            for (int i = 0; i < obs.length; i++) {
                if (obs[i] instanceof ICdtVariable && isUserVar((ICdtVariable) obs[i])) {
                    canDel = true;
                    break;
                }
            }
        }
        buttonSetEnabled(1, canEdit);
        buttonSetEnabled(2, canDel);
    }

    /*
     * called when a custom button was pressed
     */
    @Override
    public void buttonPressed(int index) {
        switch (index) {
        case 0:
            handleAddButton();
            break;
        case 1:
            handleEditButton();
            break;
        case 2:
            handleDelButton();
            break;
		default:
			break;
        }
        tv.getTable().setFocus();
    }

    private void replaceMacros() {
        if (!page.isMultiCfg() || cfgd == null
                || CDTPrefUtil.getMultiCfgStringListWriteMode() != CDTPrefUtil.WMODE_REPLACE)
            return;
        ICdtVariable[] vars = getVariables();
        for (int i = 0; i < vars.length; i++)
            if (!isUserVar(vars[i]))
                vars[i] = null;
        for (ICConfigurationDescription c : getCfs()) {
            fUserSup.deleteAll(c);
            for (ICdtVariable macro : vars)
                if (macro != null)
                    fUserSup.createMacro(macro, c);
        }
    }

    private ICConfigurationDescription[] getCfs() {
        if (cfgd instanceof ICMultiItemsHolder) {
            return (ICConfigurationDescription[]) ((ICMultiItemsHolder) cfgd).getItems();
        }
            return new ICConfigurationDescription[] { cfgd };
    }

    private void addOrEdit(ICdtVariable macro, boolean forAll) {
        if (!canCreate(macro))
            return;
        if (cfgd != null) {
            if (forAll) {
                for (ICConfigurationDescription c : page.getCfgsEditable())
                    fUserSup.createMacro(macro, c);
            } else {
                if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
                    for (ICConfigurationDescription c : getCfs())
                        fUserSup.createMacro(macro, c);
                    replaceMacros();
                } else
                    fUserSup.createMacro(macro, cfgd);
            }
        } else if (chkVars())
            prefvars.createMacro(macro);
        updateData();
    }

    private void handleAddButton() {
        NewVarDialog dlg = new NewVarDialog(usercomp.getShell(), null, cfgd, getVariables());
        if (dlg.open() == Window.OK)
            addOrEdit(dlg.getDefinedMacro(), dlg.isForAllCfgs);
    }

    private void handleEditButton() {
        ICdtVariable _vars[] = getSelectedUserMacros();
        if (_vars != null && _vars.length == 1) {
            NewVarDialog dlg = new NewVarDialog(usercomp.getShell(), _vars[0], cfgd, getVariables());
            if (dlg.open() == Window.OK)
                addOrEdit(dlg.getDefinedMacro(), false);
        }
    }

    private void handleDelButton() {
        ICdtVariable macros[] = getSelectedUserMacros();
        if (macros != null && macros.length > 0) {
            if (MessageDialog.openQuestion(usercomp.getShell(), Messages.MacrosBlock_label_delete_confirm_title,
                    Messages.MacrosBlock_label_delete_confirm_message)) {
                for (int i = 0; i < macros.length; i++) {
                    if (cfgd != null) {
                        if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
                            ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) ((ICMultiItemsHolder) cfgd)
                                    .getItems();
                            for (int k = 0; k < cfs.length; k++)
                                fUserSup.deleteMacro(macros[i].getName(), cfs[k]);
                            replaceMacros();
                        } else
                            fUserSup.deleteMacro(macros[i].getName(), cfgd);
                    } else if (chkVars())
                        prefvars.deleteMacro(macros[i].getName());
                }
                updateData();
            }
        }
    }

    /*
     * returnes the selected user-defined macros
     */
    @SuppressWarnings("unchecked")
    private ICdtVariable[] getSelectedUserMacros() {
        if (tv == null)
            return null;
        List<ICdtVariable> list = ((IStructuredSelection) tv.getSelection()).toList();
        return list.toArray(new ICdtVariable[list.size()]);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.ui.dialogs.ICOptionPage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        if (MessageDialog.openQuestion(usercomp.getShell(), Messages.MacrosBlock_label_delete_all_confirm_title,
                Messages.MacrosBlock_label_delete_all_confirm_message)) {
            if (cfgd != null) {
                if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
                    ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) ((ICMultiItemsHolder) cfgd)
                            .getItems();
                    for (int i = 0; i < cfs.length; i++)
                        fUserSup.deleteAll(cfs[i]);
                } else
                    fUserSup.deleteAll(cfgd);
            } else if (chkVars())
                prefvars.deleteAll();
            updateData();
        }
    }

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(2, true));
        Label desc = new Label(usercomp.getParent(), SWT.WRAP);
        desc.setText(Messages.CPropertyVarsTab_Description);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(desc);
        initButtons(new String[] { ADD_STR, EDIT_STR, DEL_STR });
        createTableControl();

        // Create a "show parent levels" button
        final Button b = new Button(usercomp, SWT.CHECK);
        b.setFont(usercomp.getFont());
        b.setText(Messages.CPropertyVarsTab_0);
        b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        b.setSelection(fShowSysMacros);
        b.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fShowSysMacros = b.getSelection();
                updateData(getResDesc());
            }
        });

        stringListModeControl = new StringListModeControl(page, usercomp, 1);
        stringListModeControl.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                updateData();
            }
        });

        fStatusLabel = new Label(usercomp, SWT.LEFT);
        fStatusLabel.setFont(usercomp.getFont());
        fStatusLabel.setText(EMPTY_STR);
        fStatusLabel.setLayoutData(new GridData(GridData.BEGINNING));
        fStatusLabel.setForeground(JFaceResources.getColorRegistry().get(JFacePreferences.ERROR_COLOR));
    }

    private void createTableControl() {
        TableViewer tableViewer;
        tableViewer = new TableViewer(usercomp,
                SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);

        Table table = tableViewer.getTable();
        TableLayout tableLayout = new TableLayout();
        for (int i = 0; i < fTableColumnNames.length; i++) {
            tableLayout.addColumnData(fTableColumnLayouts[i]);
            TableColumn tc = new TableColumn(table, SWT.NONE, i);
            tc.setResizable(fTableColumnLayouts[i].resizable);
            tc.setText(fTableColumnNames[i]);
        }
        table.setLayout(tableLayout);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        tableViewer.setContentProvider(new MacroContentProvider());
        tableViewer.setLabelProvider(new MacroLabelProvider());
        tableViewer.setComparator(new ViewerComparator());

        tableViewer.setColumnProperties(fEditableTableColumnProps);
        tv = tableViewer;
        tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                handleSelectionChanged();
            }
        });
        tableViewer.addDoubleClickListener(new IDoubleClickListener() {

            @Override
            public void doubleClick(DoubleClickEvent event) {
                if (!tv.getSelection().isEmpty()) {
                    buttonPressed(1);
                }
            }
        });

        table.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.DEL)
                    buttonPressed(2);
            }

            @Override
            public void keyReleased(KeyEvent e) {
            	//Nothing to do
            }
        });
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        table.setLayoutData(gd);
    }

    /*
     * answers whether the macro of a given name can be sreated
     */
    private static boolean canCreate(ICdtVariable v) {
        if (v == null)
            return false;
        String name = v.getName();
        if (name == null || (name = name.trim()).length() == 0)
            return false;
        if (fHiddenMacros != null) {
            for (int i = 0; i < fHiddenMacros.length; i++) {
                if (fHiddenMacros[i].equals(EnvVarOperationProcessor.normalizeName(name)))
                    return false;
            }
        }
        return true;
    }

    @Override
    public void updateData(ICResourceDescription _cfgd) {
        if (_cfgd == null) {
            cfgd = null;
            chkVars();
        } else {
            cfgd = _cfgd.getConfiguration();
            prefvars = null;
        }
        updateData();
    }

    private boolean chkVars() {
        if (prefvars == null)
            prefvars = fUserSup.getWorkspaceVariablesCopy();
        return (prefvars != null);
    }

    private void checkVariableIntegrity() {
        try {
            if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
                ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) ((ICMultiItemsHolder) cfgd)
                        .getItems();
                for (int i = 0; i < cfs.length; i++)
                    vmgr.checkVariableIntegrity(cfs[i]);
            } else
                vmgr.checkVariableIntegrity(cfgd);
            updateState(null);
        } catch (CdtVariableException e) {
            updateState(e);
        }
    }

    private ICdtVariable[] getVariables() {
        if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
            ICMultiItemsHolder mih = (ICMultiItemsHolder) cfgd;
            ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) mih.getItems();
            ICdtVariable[][] vs = new ICdtVariable[cfs.length][];
            for (int i = 0; i < cfs.length; i++)
                vs[i] = vmgr.getVariables(cfs[i]);
            Object[] obs = CDTPrefUtil.getListForDisplay(vs, comparator);
            ICdtVariable[] v = new ICdtVariable[obs.length];
            System.arraycopy(obs, 0, v, 0, obs.length);
            return v;
        }
		return vmgr.getVariables(cfgd);
    }

    private void updateData() {
        if (tv == null)
            return;

        checkVariableIntegrity();
        // get variables
        ICdtVariable[] _vars = getVariables();
        if (_vars == null)
            return;

        stringListModeControl.updateStringListModeControl();

        if (cfgd == null) {
            chkVars();
            if (fShowSysMacros) {
                List<ICdtVariable> lst = new ArrayList<>(_vars.length);
                ICdtVariable[] uvars = prefvars.getMacros();
                for (int i = 0; i < uvars.length; i++) {
                    lst.add(uvars[i]);
                    for (int j = 0; j < _vars.length; j++) {
                        if (_vars[j] != null && _vars[j].getName().equals(uvars[i].getName())) {
                            _vars[j] = null;
                            break;
                        }
                    }
                }
                // add system vars not rewritten by user's
                for (int j = 0; j < _vars.length; j++) {
                    if (_vars[j] != null && !vmgr.isUserVariable(_vars[j], null))
                        lst.add(_vars[j]);
                }
                _vars = lst.toArray(new ICdtVariable[lst.size()]);
            } else {
                _vars = prefvars.getMacros();
            }
        }

        ArrayList<ICdtVariable> list = new ArrayList<>(_vars.length);
        for (int i = 0; i < _vars.length; i++) {
            if (_vars[i] != null && (fShowSysMacros || isUserVar(_vars[i])))
                list.add(_vars[i]);
        }
        Collections.sort(list, CDTListComparator.getInstance());
        tv.setInput(list.toArray(new ICdtVariable[list.size()]));
        updateButtons();
    }

    private void updateState(CdtVariableException e) {
        fIncorrectlyDefinedMacrosNames.clear();
        if (e != null) {
            fStatusLabel.setText(e.getMessage());
            fStatusLabel.setVisible(true);
            ICdtVariableStatus statuses[] = e.getVariableStatuses();
            for (int i = 0; i < statuses.length; i++) {
                String name = statuses[i].getVariableName();
                if (name != null)
                    fIncorrectlyDefinedMacrosNames.add(name);
            }
        } else
            fStatusLabel.setVisible(false);
    }

    /**
     * Checks whether variable is user's.
     *
     * @param v
     *            - variable to check.
     * @return {@code true} if the variable is user's or {@code false} otherwise.
     */
    private boolean isUserVar(ICdtVariable v) {
        if (cfgd == null)
            return chkVars() && prefvars.contains(v);
        if (page.isMultiCfg() && cfgd instanceof ICMultiItemsHolder) {
            ICConfigurationDescription[] cfs = (ICConfigurationDescription[]) ((ICMultiItemsHolder) cfgd).getItems();
            for (int i = 0; i < cfs.length; i++)
                if (vmgr.isUserVariable(v, cfs[i]))
                    return true;
            return false;
        }
		return vmgr.isUserVariable(v, cfgd);
    }

	private String getString(ICdtVariable v) {
		if (fUserSup.isDynamic(v))
			return Messages.MacrosBlock_label_value_eclipse_dynamic;
		String value = EMPTY_STR;
		try {
			if (CdtVariableResolver.isStringListVariable(v.getValueType()))
				value = vmgr.convertStringListToString(v.getStringListValue(), VALUE_DELIMITER);
			else
				value = v.getStringValue();
		} catch (@SuppressWarnings("unused") CdtVariableException e1) {
			//Ignore
		}
		return value;
	}

    @Override
    protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
        if (cfgd != null) {// only for project, not for prefs
            if (page.isMultiCfg()) {
                if (src instanceof ICMultiItemsHolder && dst instanceof ICMultiItemsHolder) {
                    ICMultiItemsHolder s = (ICMultiItemsHolder) src;
                    ICMultiItemsHolder d = (ICMultiItemsHolder) dst;
                    ICResourceDescription[] r0 = (ICResourceDescription[]) s.getItems();
                    ICResourceDescription[] r1 = (ICResourceDescription[]) d.getItems();
                    if (r0.length != r1.length)
                        return; // unprobable
                    for (int i = 0; i < r0.length; i++) {
                        ICdtVariable[] vs = fUserSup.getMacros(r0[i].getConfiguration());
                        fUserSup.setMacros(vs, r1[i].getConfiguration());
                    }
                }
            } else {
                ICdtVariable[] vs = fUserSup.getMacros(src.getConfiguration());
                fUserSup.setMacros(vs, dst.getConfiguration());
            }
        } else if (chkVars())
            fUserSup.storeWorkspaceVariables(true);
    }

    /**
     * Unlike other pages, workspace variables
     * should be stored explicitly on "OK".
     */
    @Override
    protected void performOK() {
        if (chkVars())
            try {
                if (fUserSup.setWorkspaceVariables(prefvars))
                    if (page instanceof PrefPage_Abstract)
                        PrefPage_Abstract.isChanged = true;
            } catch (@SuppressWarnings("unused") CoreException e) {
            	//Nothing
            }
        prefvars = null;
        super.performOK();
    }

    @Override
    protected void performCancel() {
        prefvars = null;
        super.performCancel();
    }

    // This page can be displayed for project only
    @Override
    public boolean canBeVisible() {
        return page.isForProject() || page.isForPrefs();
    }

    private static class EnvCmp implements Comparator<Object> {

        @Override
        public int compare(Object a0, Object a1) {
            if (a0 == null || a1 == null)
                return 0;
            if (a0 instanceof ICdtVariable && a1 instanceof ICdtVariable) {
                ICdtVariable x0 = (ICdtVariable) a0;
                ICdtVariable x1 = (ICdtVariable) a1;
                String s0 = x0.getName();
                if (s0 == null)
                    s0 = AbstractPage.EMPTY_STR;
                String s1 = x1.getName();
                if (s1 == null)
                    s1 = AbstractPage.EMPTY_STR;
                return (s0.compareTo(s1));
            }
			return 0;
        }
    }

}
