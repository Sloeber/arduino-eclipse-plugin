/*******************************************************************************
 * Copyright (c) 2004, 2016 BitMethods Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * BitMethods Inc - Initial API and implementation
 * ARM Ltd. - basic tooltip support
 * Miwako Tokugawa (Intel Corporation) - Fixed-location tooltip support
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.cdt.utils.ui.controls.FileListControl;
import org.eclipse.cdt.utils.ui.controls.IFileListChangeListener;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.PlatformUI;

/**
 * Field editor that uses FileListControl for user input.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class FileListControlFieldEditor extends FieldEditor {

    // file list control
    private FileListControl myListControl;
    private int browseType;
    private Composite topLayout;
    private static final String DEFAULT_SEPARATOR = ";"; //$NON-NLS-1$

    /**
     * Creates a file list control field editor.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param parent
     *            the parent of the field editor's control
     * @param type
     *            the browseType of the file list control
     */
    public FileListControlFieldEditor(String name, String labelText, Composite parent, int type) {
        super();
        browseType = type;
        init(name, labelText);
        createControl(parent);
    }

    /**
     * Creates a file list control field editor.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param tooltip
     *            the tooltip text of the field editor
     * @param contextId
     * @param parent
     *            the parent of the field editor's control
     * @param type
     *            the browseType of the file list control
     */
    public FileListControlFieldEditor(String name, String labelText, String tooltip, String contextId, Composite parent,
            String value, int type) {
        this(name, labelText, parent, type);
        // can't use setToolTip(tooltip) as label not created yet
        getLabelControl(parent).setToolTipText(tooltip);
        if (!contextId.isEmpty())
            PlatformUI.getWorkbench().getHelpSystem().setHelp(myListControl.getListControl(), contextId);
        myListControl.setList(parseString(value));
    }
    /**
     * Creates a file list control field editor.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param parent
     *            the parent of the field editor's control
     * @param value
     *            the field editor's value
     * @param type
     *            the browseType of the file list control
     */
    //    public FileListControlFieldEditor(String name, String labelText, Composite parent, String value, int type) {
    //        this(name, labelText, parent, type);
    //        browseType = type;
    //        //        this.values = parseString(value);
    //    }

    /**
     * Sets the field editor's tool tip text to the argument, which
     * may be null indicating that no tool tip text should be shown.
     *
     * @param tooltip
     *            the new tool tip text (or null)
     *
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the field editor has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the field editor</li>
     *                </ul>
     */
    public void setToolTip(String tooltip) {
        // Currently just the label has the tooltip
        getLabelControl().setToolTipText(tooltip);
    }

    /**
     * Returns the field editor's tool tip text, or null if it has
     * not been set.
     *
     * @return the field editor's tool tip text
     *
     * @exception SWTException
     *                <ul>
     *                <li>ERROR_WIDGET_DISPOSED - if the field editor has been
     *                disposed</li>
     *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
     *                thread that created the field editor</li>
     *                </ul>
     */
    public String getToolTipText() {
        return getLabelControl().getToolTipText();
    }

    /**
     * Sets the filter-path for the underlying Browse dialog. Only applies when
     * browseType is 'file' or 'dir'.
     * 
     * @param filterPath
     *
     * @since 7.0
     */
    public void setFilterPath(String filterPath) {
        myListControl.setFilterPath(filterPath);
    }

    /**
     * Sets the filter-extensions for the underlying Browse dialog. Only applies
     * when browseType is 'file'.
     * 
     * @param filterExtensions
     *
     * @since 7.0
     */
    public void setFilterExtensions(String[] filterExtensions) {
        myListControl.setFilterExtensions(filterExtensions);
    }

    /**
     * Fills this field editor's basic controls into the given parent.
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        topLayout = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = numColumns;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.makeColumnsEqualWidth = false;
        topLayout.setLayout(layout);
        GridData gddata = new GridData(GridData.FILL_BOTH);
        gddata.horizontalSpan = 2;
        topLayout.setLayoutData(gddata);
        // file list control
        myListControl = new FileListControl(topLayout, getLabelText(), browseType, false);
        myListControl.addChangeListener(new IFileListChangeListener() {

            @Override
            public void fileListChanged(FileListControl fileList, String oldValue[], String newValue[]) {
                handleFileListChange(fileList, oldValue, newValue);
            }

        });
        topLayout.setLayout(layout);
    }

    private void handleFileListChange(FileListControl fileList, String oldValue[], String newValue[]) {
        //		values = fileList.getItems();
        fireValueChanged(VALUE, createList(oldValue), createList(newValue));
    }

    /**
     * @return the file list control
     */
    protected List getListControl() {
        return myListControl.getListControl();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doLoad()
     */
    @Override
    protected void doLoad() {
        myListControl.selectionChanged();
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
     */
    @Override
    protected void doLoadDefault() {
        if (myListControl != null) {
            myListControl.removeAll();
            String s = getPreferenceStore().getDefaultString(getPreferenceName());
            String[] array = parseString(s);
            myListControl.setList(array);
            myListControl.selectionChanged();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#doStore()
     */
    @Override
    protected void doStore() {
        String s = createList(myListControl.getItems());
        if (s != null)
            getPreferenceStore().setValue(getPreferenceName(), s);
    }

    public String[] getStringListValue() {
        return myListControl.getItems();
    }

    /**
     * Returns the number of basic controls this field editor consists of.
     *
     * @return the number of controls
     */
    @Override
    public int getNumberOfControls() {
        return 1;
    }

    /**
     * Answers a <code>String</code> containing the strings passed in the
     * argument separated by the DEFAULT_SEPERATOR
     *
     * @param items
     *            An array of strings
     * @return
     */
    private String createList(String[] items) {
        StringBuilder path = new StringBuilder();

        for (int i = 0; i < items.length; i++) {
            path.append(items[i]);
            if (i < (items.length - 1)) {
                path.append(DEFAULT_SEPARATOR);
            }
        }
        return path.toString();
    }

    /**
     * Parse the string with the separator and returns the string array.
     * 
     * @param stringList
     * @return
     */
    private String[] parseString(String stringList) {
        StringTokenizer tokenizer = new StringTokenizer(stringList, DEFAULT_SEPARATOR);
        ArrayList<String> list = new ArrayList<>();
        while (tokenizer.hasMoreElements()) {
            list.add((String) tokenizer.nextElement());
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Set style
     */
    public void setStyle() {
        ((GridLayout) topLayout.getLayout()).marginWidth = 0;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        ((GridData) topLayout.getLayoutData()).horizontalSpan = numColumns;
    }

    @Override
    public Label getLabelControl(Composite parent) {
        return myListControl.getLabelControl();
    }

    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        myListControl.setEnabled(enabled);
    }

}
