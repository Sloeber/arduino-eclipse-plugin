package it.baeyens.arduino.ui;

/*******************************************************************************
 * Copyright (c) 2004 BitMethods Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * BitMethods Inc - Initial API and implementation
 *******************************************************************************/

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * MultiLineTextFieldEditor. Field editor that is same as string field editor
 * but will have the multi line text field for user input.
 */
public class MultiLineTextFieldEditor extends FieldEditor {

    /**
     * Text limit constant (value <code>-1</code>) indicating unlimited text
     * limit and width.
     */
    public static int UNLIMITED = -1;

    /**
     * Cached valid state.
     */
    private boolean isValid;

    /**
     * Old text value.
     */
    private String oldValue;
    private String compTitle;
    private Label title;

    /**
     * The text field, or <code>null</code> if none.
     */
    protected Text textField;

    /**
     * Text limit of text field in characters; initially unlimited.
     */
    private int textLimit = UNLIMITED;

    /**
     * Indicates whether the empty string is legal; <code>true</code> by
     * default.
     */
    private boolean emptyStringAllowed = true;

    /**
     * Creates a new string field editor
     */
    protected MultiLineTextFieldEditor() {
    }

    /**
     * Creates a string field editor. Use the method <code>setTextLimit</code>
     * to limit the text.
     * 
     * @param name
     *            the name of the preference this field editor works on
     * @param labelText
     *            the label text of the field editor
     * @param width
     *            the width of the text input field in characters, or
     *            <code>UNLIMITED</code> for no limit
     * @param parent
     *            the parent of the field editor's control
     * @since 2.0
     */
    public MultiLineTextFieldEditor(String name, String labelText, Composite parent) {
	init(name, labelText);
	this.isValid = false;
	createControl(parent);
    }

    /**
     * Adjusts the horizontal span of this field editor's basic controls
     * <p>
     * Subclasses must implement this method to adjust the horizontal span of
     * controls so they appear correct in the given number of columns.
     * </p>
     * <p>
     * The number of columns will always be equal to or greater than the value
     * returned by this editor's <code>getNumberOfControls</code> method.
     * 
     * @param numColumns
     *            the number of columns
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
	GridData gd = (GridData) this.textField.getLayoutData();
	// grab all the space we can
	gd.horizontalSpan = numColumns;
	gd.grabExcessHorizontalSpace = true;
	gd.grabExcessVerticalSpace = true;
    }

    /**
     * Checks whether the text input field contains a valid value or not.
     *
     * @return <code>true</code> if the field value is valid, and
     *         <code>false</code> if invalid
     */
    protected boolean checkState() {
	String txt = this.textField.getText();

	if (txt == null)
	    return false;

	return (txt.trim().length() > 0) || this.emptyStringAllowed;
    }

    /**
     * Fills this field editor's basic controls into the given parent.
     * <p>
     * The string field implementation of this <code>FieldEditor</code>
     * framework method contributes the text field. Subclasses may override but
     * must call <code>super.doFillIntoGrid</code>.
     * </p>
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {

	this.title = new Label(parent, SWT.UP);
	this.title.setFont(parent.getFont());
	this.compTitle = getLabelText();
	this.title.setText(this.compTitle);
	this.title.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

	this.textField = getTextControl(parent);
	GridData gd = new GridData(GridData.FILL_BOTH);
	this.textField.setLayoutData(gd);

    }

    /**
     * Initializes this field editor with the preference value from the
     * preference store.
     * <p>
     * Subclasses must implement this method to properly initialize the field
     * editor.
     * </p>
     */
    @Override
    protected void doLoad() {
	if (this.textField != null) {
	    String value = getPreferenceStore().getString(getPreferenceName());
	    this.textField.setText(value);
	    this.oldValue = value;
	}
    }

    /**
     * Initializes this field editor with the default preference value from the
     * preference store.
     * <p>
     * Subclasses must implement this method to properly initialize the field
     * editor.
     * </p>
     */
    @Override
    protected void doLoadDefault() {
	if (this.textField != null) {
	    String value = getPreferenceStore().getDefaultString(getPreferenceName());
	    this.textField.setText(value);
	}
	valueChanged();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditor#doStore()
     */
    @Override
    protected void doStore() {
	getPreferenceStore().setValue(getPreferenceName(), this.textField.getText());
    }

    /**
     * Returns the field editor's value.
     *
     * @return the current value
     */
    public String getStringValue() {
	if (this.textField != null)
	    return this.textField.getText();
	return getPreferenceStore().getString(getPreferenceName());
    }

    /**
     * Returns this field editor's text control.
     *
     * @param parent
     *            the parent
     * @return the text control, or <code>null</code> if no text field is
     *         created yet
     */
    protected Text getTextControl() {
	return this.textField;
    }

    /**
     * Returns this field editor's text control.
     * <p>
     * The control is created if it does not yet exist
     * </p>
     *
     * @param parent
     *            the parent
     * @return the text control
     */
    public Text getTextControl(Composite parent) {
	if (this.textField == null) {
	    this.textField = new Text(parent, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER | SWT.WRAP);
	    this.textField.setFont(parent.getFont());

	    this.textField.addDisposeListener(new DisposeListener() {

		@Override
		public void widgetDisposed(DisposeEvent event) {
		    MultiLineTextFieldEditor.this.textField = null;
		}

	    });

	    if (this.textLimit > 0) { // Only set limits above 0 - see SWT spec
		this.textField.setTextLimit(this.textLimit);
	    }
	} else {
	    checkParent(this.textField, parent);
	}
	return this.textField;
    }

    /**
     * Returns whether an empty string is a valid value.
     *
     * @return <code>true</code> if an empty string is a valid value, and
     *         <code>false</code> if an empty string is invalid
     * @see #setEmptyStringAllowed
     */

    public boolean isEmptyStringAllowed() {
	return this.emptyStringAllowed;
    }

    /**
     * Returns whether this field editor contains a valid value.
     * <p>
     * The default implementation of this framework method returns
     * <code>true</code>. Subclasses wishing to perform validation should
     * override both this method and <code>refreshValidState</code>.
     * </p>
     * 
     * @return <code>true</code> if the field value is valid, and
     *         <code>false</code> if invalid
     * @see #refreshValidState
     */
    @Override
    public boolean isValid() {
	return this.isValid;
    }

    /**
     * Refreshes this field editor's valid state after a value change and fires
     * an <code>IS_VALID</code> property change event if warranted.
     * <p>
     * The default implementation of this framework method does nothing.
     * Subclasses wishing to perform validation should override both this method
     * and <code>isValid</code>.
     * </p>
     * 
     * @see #isValid
     */
    @Override
    protected void refreshValidState() {
	this.isValid = checkState();
    }

    /**
     * Sets whether the empty string is a valid value or not.
     *
     * @param b
     *            <code>true</code> if the empty string is allowed, and
     *            <code>false</code> if it is considered invalid
     */
    public void setEmptyStringAllowed(boolean b) {
	this.emptyStringAllowed = b;
    }

    /**
     * Sets the focus to this field editor.
     * <p>
     * The default implementation of this framework method does nothing.
     * Subclasses may reimplement.
     * </p>
     */
    @Override
    public void setFocus() {
	if (this.textField != null) {
	    this.textField.setFocus();
	}
    }

    /**
     * Sets this field editor's value.
     *
     * @param value
     *            the new value, or <code>null</code> meaning the empty string
     */
    public void setStringValue(String value) {
	String safeValue = value;
	if (value == null)
	    safeValue = ""; //$NON-NLS-1$
	if (this.textField != null) {

	    this.oldValue = this.textField.getText();
	    if (!this.oldValue.equals(safeValue)) {
		this.textField.setText(safeValue);
		valueChanged();
	    }
	}
    }

    /**
     * Sets this text field's text limit.
     *
     * @param limit
     *            the limit on the number of character in the text input field,
     *            or <code>UNLIMITED</code> for no limit
     */
    public void setTextLimit(int limit) {
	this.textLimit = limit;
	if (this.textField != null)
	    this.textField.setTextLimit(limit);
    }

    /**
     * Informs this field editor's listener, if it has one, about a change to
     * the value (<code>VALUE</code> property) provided that the old and new
     * values are different.
     * <p>
     * This hook is <em>not</em> called when the text is initialized (or reset
     * to the default value) from the preference store.
     * </p>
     */
    protected void valueChanged() {
	setPresentsDefaultValue(false);
	boolean oldState = this.isValid;
	refreshValidState();

	if (this.isValid != oldState)
	    fireStateChanged(IS_VALID, oldState, this.isValid);

	String newValue = this.textField.getText();
	if (!newValue.equals(this.oldValue)) {
	    fireValueChanged(VALUE, this.oldValue, newValue);
	    this.oldValue = newValue;
	}
    }

    @Override
    public int getNumberOfControls() {
	return 2;
    }
}