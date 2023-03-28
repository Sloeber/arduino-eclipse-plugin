/*******************************************************************************
 * Copyright (c) 2007, 2013 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.integrations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.model.util.CDTListComparator;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.internal.core.envvar.EnvVarDescriptor;
import org.eclipse.cdt.internal.core.envvar.EnvironmentVariableManager;
import org.eclipse.cdt.internal.core.envvar.UserDefinedEnvironmentSupplier;
import org.eclipse.cdt.internal.ui.newui.Messages;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.EnvDialog;
import org.eclipse.cdt.ui.newui.MultiCfgContributedEnvironment;
import org.eclipse.cdt.ui.newui.PrefPage_Abstract;
import org.eclipse.cdt.ui.newui.StringListModeControl;
import org.eclipse.cdt.utils.envvar.StorableEnvironment;
import org.eclipse.cdt.utils.spawner.EnvironmentReader;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.dialogs.ListSelectionDialog;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EnvironmentTab extends AbstractCPropertyTab {
    private static final String SEPARATOR = System.getProperty("path.separator", ";"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final String LBR = " ["; //$NON-NLS-1$
    private static final String RBR = "]"; //$NON-NLS-1$
    private static final UserDefinedEnvironmentSupplier fUserSupplier = EnvironmentVariableManager.fUserSupplier;

    private final MultiCfgContributedEnvironment ce = new MultiCfgContributedEnvironment();

    private Table table;
    private TableViewer tv;
    private ArrayList<TabData> data = new ArrayList<>();
    private Button b1, b2;
    private StringListModeControl stringListModeControl;

    private ICConfigurationDescription cfgd = null;
    private StorableEnvironment vars = null;

    private class TabData implements Comparable<TabData> {
        IEnvironmentVariable var;

        TabData(IEnvironmentVariable _var) {
            var = _var;
        }

        @Override
        public int compareTo(TabData a) {
            String s = var.getName();
            if (a != null && s != null && a.var != null)
                return (s.compareTo(a.var.getName()));
            return 0;
        }
    }

    private class EnvironmentLabelProvider extends LabelProvider
            implements ITableLabelProvider, IFontProvider, ITableFontProvider, IColorProvider {
        public EnvironmentLabelProvider(boolean user) {
        }

        @Override
        public Image getImage(Object element) {
            return null; // JavaPluginImages.get(JavaPluginImages.IMG_OBJS_REFACTORING_INFO);
        }

        @Override
        public String getText(Object element) {
            return getColumnText(element, 0);
        }

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            TabData td = (TabData) element;
            switch (columnIndex) {
            case 0:
                return td.var.getName();
            case 1:
                if (td.var.getOperation() == IEnvironmentVariable.ENVVAR_REMOVE)
                    return Messages.EnvironmentTab_20;
                return td.var.getValue();
            case 2:
                return ce.getOrigin(td.var);
            }
            return EMPTY_STR;
        }

        @Override
        public Font getFont(Object element) {
            return getFont(element, 0);
        }

        @Override
        public Font getFont(Object element, int columnIndex) {
            TabData td = (TabData) element;
            switch (columnIndex) {
            case 0:
                if (isUsers(td.var))
                    return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
                break;
            default:
                break;
            }
            return null;
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            TabData td = (TabData) element;
            if (isUsers(td.var))
                return BACKGROUND_FOR_USER_VAR;
            return null;
        }
    }

    @Override
    public void createControls(Composite parent) {
        super.createControls(parent);
        usercomp.setLayout(new GridLayout(3, true));
        Label l1 = new Label(usercomp, SWT.LEFT);
        l1.setText(Messages.EnvironmentTab_0);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        l1.setLayoutData(gd);
        table = new Table(usercomp, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateButtons();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                if (buttonIsEnabled(2) && table.getSelectionIndex() != -1)
                    buttonPressed(2);
            }
        });

        tv = new TableViewer(table);
        tv.setContentProvider(new IStructuredContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                if (inputElement != null && inputElement instanceof ArrayList<?>) {
                    @SuppressWarnings("unchecked")
                    ArrayList<TabData> ar = (ArrayList<TabData>) inputElement;
                    return ar.toArray(new TabData[0]);
                }
                return null;
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
        });
        tv.setLabelProvider(new EnvironmentLabelProvider(true));
        // add headers
        TableColumn tc = new TableColumn(table, SWT.LEFT);
        tc.setText(Messages.EnvironmentTab_1);
        tc.setWidth(150);
        tc = new TableColumn(table, SWT.LEFT);
        tc.setText(Messages.EnvironmentTab_2);
        tc.setWidth(150);
        if (this.getResDesc() != null) {
            tc = new TableColumn(table, SWT.LEFT);
            tc.setText(Messages.EnvironmentTab_16);
            tc.setWidth(100);
        }

        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 3;
        table.setLayoutData(gd);

        b1 = new Button(usercomp, SWT.RADIO);
        b1.setText(Messages.EnvironmentTab_3);
        b1.setToolTipText(Messages.EnvironmentTab_3);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        if (page.isForProject())
            gd.horizontalSpan = 2;
        else
            gd.horizontalSpan = 3;
        b1.setLayoutData(gd);
        b1.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (cfgd != null)
                    ce.setAppendEnvironment(true, cfgd);
                else
                    vars.setAppendContributedEnvironment(true);
                updateData();
            }
        });

        if (page.isForProject()) {
            stringListModeControl = new StringListModeControl(page, usercomp, 1);
            stringListModeControl.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    updateData();
                }
            });
        }

        b2 = new Button(usercomp, SWT.RADIO);
        b2.setText(Messages.EnvironmentTab_4);
        b2.setToolTipText(Messages.EnvironmentTab_4);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        b2.setLayoutData(gd);
        b2.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (cfgd != null)
                    ce.setAppendEnvironment(false, cfgd);
                else
                    vars.setAppendContributedEnvironment(false);
                updateData();
            }
        });

        initButtons(new String[] { Messages.EnvironmentTab_5, Messages.EnvironmentTab_6, Messages.EnvironmentTab_7,
                Messages.EnvironmentTab_8, Messages.EnvironmentTab_9 });
    }

    @Override
    public void buttonPressed(int i) {
        switch (i) {
        case 0:
            handleEnvAddButtonSelected();
            break;
        case 1: // select
            handleEnvSelectButtonSelected();
            break;
        case 2: // edit
            handleEnvEditButtonSelected(table.getSelectionIndex());
            break;
        case 3: // remove
            handleEnvDelButtonSelected(table.getSelectionIndex());
            break;
        case 4: // Undefine
            handleEnvUndefButtonSelected(table.getSelectionIndex());
            break;
        }
        table.setFocus();
    }

    @Override
    protected void updateButtons() {
        if (table == null || table.isDisposed())
            return;

        boolean canEdit = table.getSelectionCount() == 1;
        boolean canDel = false;
        boolean canUndef = table.getSelectionCount() >= 1;
        if (canUndef) {
            for (int i : table.getSelectionIndices()) {
                IEnvironmentVariable var = ((TabData) tv.getElementAt(i)).var;
                if (isUsers(var)) {
                    //	if (cfgd == null || !wse.getVariable(var.))
                    canDel = true;
                    break;
                }
            }
        }
        buttonSetEnabled(2, canEdit); // edit
        buttonSetEnabled(3, canDel); // delete
        buttonSetEnabled(4, canUndef); // undefine
    }

    @Override
    protected void updateData(ICResourceDescription _cfgd) {
        // null means preference configuration
        cfgd = (_cfgd != null) ? _cfgd.getConfiguration() : null;
        if (cfgd == null && vars == null)
            vars = fUserSupplier.getWorkspaceEnvironmentCopy();
        else
            ce.setMulti(page.isMultiCfg());
        updateData();
    }

    private void updateData() {
        IEnvironmentVariable[] _vars = null;
        if (cfgd != null) {
            b1.setSelection(ce.appendEnvironment(cfgd));
            b2.setSelection(!ce.appendEnvironment(cfgd));
            _vars = ce.getVariables(cfgd);
        } else {
            if (vars == null)
                vars = fUserSupplier.getWorkspaceEnvironmentCopy();
            b1.setSelection(vars.appendContributedEnvironment());
            b2.setSelection(!vars.appendContributedEnvironment());
            _vars = vars.getVariables();
        }

        data.clear();
        if (_vars != null) {
            for (IEnvironmentVariable _var : _vars) {
                data.add(new TabData(_var));
            }
        }
        Collections.sort(data);
        tv.setInput(data);

        if (stringListModeControl != null) {
            stringListModeControl.updateStringListModeControl();
        }
        updateButtons();
    }

    @Override
    protected void performApply(ICResourceDescription _src, ICResourceDescription _dst) {
        ICConfigurationDescription src = _src.getConfiguration();
        ICConfigurationDescription dst = _dst.getConfiguration();

        ce.setAppendEnvironment(ce.appendEnvironment(src), dst);
        IEnvironmentVariable[] v = ce.getVariables(dst);
        for (IEnvironmentVariable element : v)
            ce.removeVariable(element.getName(), dst);
        v = ce.getVariables(src);
        for (IEnvironmentVariable element : v) {
            if (ce.isUserVariable(src, element))
                ce.addVariable(element.getName(), element.getValue(), element.getOperation(), element.getDelimiter(),
                        dst);
        }
    }

    /**
     *
     */
    private class MyListSelectionDialog extends ListSelectionDialog {
        public boolean toAll = false;

        public MyListSelectionDialog(Shell parentShell, Object input, IStructuredContentProvider contentProvider) {
            super(parentShell, input, contentProvider, new LabelProvider() {
            }, Messages.EnvironmentTab_12);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            Composite composite = (Composite) super.createDialogArea(parent);
            Button b = new Button(composite, SWT.CHECK);
            b.setText(Messages.EnvironmentTab_13);
            b.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            if (cfgd == null)
                b.setVisible(false);
            else
                b.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        toAll = ((Button) e.widget).getSelection();
                    }
                });
            return composite;
        }
    }

    private void handleEnvEditButtonSelected(int n) {
        if (n == -1)
            return;
        IEnvironmentVariable var = ((TabData) tv.getElementAt(n)).var;
        EnvDialog dlg = new EnvDialog(usercomp.getShell(), var, Messages.EnvironmentTab_11, false, page.isMultiCfg(),
                cfgd);
        if (dlg.open() == Window.OK) {
            if (cfgd != null)
                ce.addVariable(var.getName(), dlg.t2.trim(), IEnvironmentVariable.ENVVAR_REPLACE, var.getDelimiter(),
                        cfgd);
            else
                vars.createVariable(dlg.t1.trim(), dlg.t2.trim(), IEnvironmentVariable.ENVVAR_REPLACE,
                        var.getDelimiter());
            updateData();
            table.setSelection(n);
            updateButtons();
        }
    }

    private void handleEnvUndefButtonSelected(int n) {
        if (n == -1)
            return;
        for (int i : table.getSelectionIndices()) {
            IEnvironmentVariable var = ((TabData) tv.getElementAt(i)).var;
            if (cfgd == null)
                vars.createVariable(var.getName(), null, IEnvironmentVariable.ENVVAR_REMOVE, var.getDelimiter());
            else
                ce.addVariable(var.getName(), null, IEnvironmentVariable.ENVVAR_REMOVE, var.getDelimiter(), cfgd);
        }
        updateData();
        table.setSelection(n);
        updateButtons();
    }

    private void handleEnvDelButtonSelected(int n) {
        if (n == -1)
            return;
        for (int i : table.getSelectionIndices()) {
            IEnvironmentVariable var = ((TabData) tv.getElementAt(i)).var;
            if (cfgd == null)
                vars.deleteVariable(var.getName());
            else
                ce.removeVariable(var.getName(), cfgd);
        }
        updateData();
        int x = table.getItemCount() - 1;
        if (x >= 0) {
            table.setSelection(Math.min(x, n));
            updateButtons();
        }
    }

    private void handleEnvAddButtonSelected() {
        IEnvironmentVariable var = null;
        EnvDialog dlg = new EnvDialog(usercomp.getShell(), var, Messages.EnvironmentTab_10, true, page.isMultiCfg(),
                cfgd);
        if (dlg.open() == Window.OK) {
            String name = dlg.t1.trim();
            if (name.length() > 0) {
                ICConfigurationDescription[] cfgs;
                if (dlg.toAll)
                    cfgs = page.getCfgsEditable();
                else
                    cfgs = new ICConfigurationDescription[] { cfgd };
                if (cfgd == null)
                    vars.createVariable(name, dlg.t2.trim(), IEnvironmentVariable.ENVVAR_APPEND, SEPARATOR);
                else
                    for (ICConfigurationDescription cfg : cfgs) {
                        ce.addVariable(name, dlg.t2.trim(), IEnvironmentVariable.ENVVAR_APPEND, SEPARATOR, cfg);
                    }
                updateData();
                setPos(name);
            }
        }
    }

    private void setPos(String name) {
        if (name == null || name.length() == 0)
            return;
        for (int i = 0; i < table.getItemCount(); i++) {
            if (name.equals(table.getItem(i).getText())) {
                table.setSelection(i);
                updateButtons();
                break;
            }
        }
    }

    private void handleEnvSelectButtonSelected() {
        // get Environment Variables from the OS
        Map<?, ?> v = EnvironmentReader.getEnvVars();
        MyListSelectionDialog dialog = new MyListSelectionDialog(usercomp.getShell(), v,
                createSelectionDialogContentProvider());

        dialog.setTitle(Messages.EnvironmentTab_14);
        if (dialog.open() == Window.OK) {
            Object[] selected = dialog.getResult();
            ICConfigurationDescription[] cfgs;
            if (dialog.toAll)
                cfgs = page.getCfgsEditable();
            else
                cfgs = new ICConfigurationDescription[] { cfgd };

            String name = null;
            for (Object element : selected) {
                name = (String) element;
                String value = EMPTY_STR;
                int x = name.indexOf(LBR);
                if (x >= 0) {
                    value = name.substring(x + 2, name.length() - 1);
                    name = name.substring(0, x);
                }

                if (cfgd == null)
                    vars.createVariable(name, value);
                else
                    for (ICConfigurationDescription cfg : cfgs) {
                        ce.addVariable(name, value, IEnvironmentVariable.ENVVAR_APPEND, SEPARATOR, cfg);
                    }
            }
            updateData();
            setPos(name);
        }
    }

    private IStructuredContentProvider createSelectionDialogContentProvider() {
        return new IStructuredContentProvider() {

            @Override
            public Object[] getElements(Object inputElement) {
                String[] els = null;
                if (inputElement instanceof Map<?, ?>) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> m = (Map<String, String>) inputElement;
                    els = new String[m.size()];
                    int index = 0;
                    for (Iterator<String> iterator = m.keySet().iterator(); iterator.hasNext(); index++) {
                        String k = iterator.next();
                        els[index] = TextProcessor.process(k + LBR + m.get(k) + RBR);
                    }
                }
                Arrays.sort(els, CDTListComparator.getInstance());
                return els;
            }

            @Override
            public void dispose() {
            }

            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }
        };
    }

    // This page can be displayed for project only
    @Override
    public boolean canBeVisible() {
        return page.isForProject() || page.isForPrefs();
    }

    @Override
    protected void performOK() {
        if (vars != null) {
            if (fUserSupplier.setWorkspaceEnvironment(vars))
                if (page instanceof PrefPage_Abstract)
                    PrefPage_Abstract.isChanged = true;
        }
        vars = null;
        super.performOK();
        updateData();
    }

    @Override
    protected void performCancel() {
        vars = null;
        super.performCancel();
    }

    @Override
    protected void performDefaults() {
        ce.restoreDefaults(cfgd); // both for proj & prefs
        vars = null;
        updateData();
    }

    private boolean isUsers(IEnvironmentVariable var) {
        return cfgd == null
                || (ce.isUserVariable(cfgd, var) && ((EnvVarDescriptor) var).getContextInfo().getContext() != null);

    }
}
