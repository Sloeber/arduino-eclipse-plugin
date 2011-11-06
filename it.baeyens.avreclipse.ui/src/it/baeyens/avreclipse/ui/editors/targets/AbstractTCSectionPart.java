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
 * $Id: AbstractTCSectionPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.editors.targets;

import it.baeyens.avreclipse.core.targets.ITargetConfigChangeListener;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.Result;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.ValidationResult;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IMessage;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;


/**
 * Default implementation of the {@link ITCEditorPart} which creates a Section.
 * <p>
 * This implementation completely hides the <code>IFormPart</code> interface and its extension, the
 * {@link ITCEditorPart}.
 * </p>
 * <p>
 * Subclasses only need to implement the following methods:
 * <ul>
 * <li>{@link #getTitle()} to set the title of the section.</li>
 * <li>{@link #getPartAttributes()} for a list of managed attributes.</li>
 * <li>{@link #createSectionContent(Composite, FormToolkit)} to create the static parts of the user
 * interface.</li>
 * <li>{@link #refreshSectionContent()} to refresh the static parts and create the dynamic parts of
 * the user interface.</li>
 * </ul>
 * Optionally the following methods can be overridden to give this part more information:
 * <ul>
 * <li>{@link #getDescription()} for an optional Section description.</li>
 * <li>{@link #getDependentAttributes()} for a list of attributes that will cause this part to
 * become stale when changed.</li>
 * <li>{@link #getSectionStyle()} to override the style bits for the section.</li>
 * <li>{@link #refreshMessages()} to let the part update the messages in the shared header, even
 * when the form is not active.</li>
 * </ul>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public abstract class AbstractTCSectionPart implements ITCEditorPart {

	/** The managed form this object is a part of. */
	private IManagedForm						fManagedForm;

	/**
	 * The current message manager. Can be changed with the
	 * {@link #setMessageManager(IMessageManager)} method.
	 */
	private IMessageManager						fMessageManager;

	/** The parent composite for this part. */
	private Composite							fParent;

	/** The Section control created by this part. */
	private Section								fSection;

	/** The content of the Section created by this part. */
	private Composite							fContentCompo;

	/** The target configuration (working copy) this form part works on. */
	private ITargetConfigurationWorkingCopy		fTCWC;

	/** Map of attributes managed by this part and their last saved values. */
	private final Map<String, String>			fLastValues				= new HashMap<String, String>();

	/** Map of attributes that will cause this part to become stale and their last saved values. */
	private final Map<String, String>			fLastDependentValues	= new HashMap<String, String>();

	private final ITargetConfigChangeListener	fListener				= new ChangeListener();

	/** <code>true</code> if the part has been initialized. The part may only be initialized once. */
	private boolean								fIsInitialized			= false;

	/** <code>true</code> if the part has become stale because a dependent attribute was changed. */
	private boolean								fIsStale				= false;

	public final static String[]				EMPTY_LIST				= new String[] {};

	/**
	 * A target configuration change listener that will mark this part as stale when any of the
	 * attributes in the list returned by {@link AbstractTCSectionPart#getDependentAttributes()} has
	 * a value different from its last saved value.
	 */
	private class ChangeListener implements ITargetConfigChangeListener {


		@Override
		public void attributeChange(ITargetConfiguration config, String attribute, String oldvalue,
				String newvalue) {

			if (fLastDependentValues.containsKey(attribute)) {
				String lastValue = fLastDependentValues.get(attribute);
				// the attribute is on the list of dependent values.
				// The part is stale when the new value is different from the last saved value
				if (lastValue == null) {
					fIsStale = true;
				} else {
					fIsStale = !lastValue.equals(newvalue);
				}

				fManagedForm.staleStateChanged();
				fLastDependentValues.put(attribute, newvalue);
			}
			// When the form is marked as stale it will only be refreshed when it is (or
			// becomes)
			// active. But the changes might cause some problems with this form that need to be
			// shown immediately in the form header.
			// So we call this method here to give the subclass a chance to update its problem
			// messages.
			refreshMessages();
		}
	}

	/**
	 * Returns a list of attributes that are managed by this form part.
	 * <p>
	 * This list is used to associate attributes with the form part that edits them. Internally the
	 * list is also used to manage the dirty state of the form part.
	 * </p>
	 * 
	 * @return Array with attributes. May be empty but never <code>null</code>
	 */
	protected abstract String[] getPartAttributes();

	/**
	 * Returns a list of attributes whose changes cause the form part to become stale.
	 * <p>
	 * Changes to any of these attributes will cause {@link #refresh()} to be called, although the
	 * call may be delayed if the part is not currently visible.
	 * </p>
	 * 
	 * @return Array with attributes. May be empty but never <code>null</code>
	 */
	protected String[] getDependentAttributes() {
		return EMPTY_LIST;
	}

	/**
	 * Returns a String to be used as the title for this section part.
	 * 
	 * @return The title string.
	 */
	protected abstract String getTitle();

	/**
	 * Returns an optional description for this section part.
	 * <p>
	 * If <code>null</code> is returned the section will not have a description.
	 * </p>
	 * 
	 * @return A description string or <code>null</code>.
	 */
	protected String getDescription() {
		return null;
	}

	/**
	 * Returns the style bits used for creating the section.
	 * <p>
	 * The default is: EXPANDED, TITLE_BAR and CLIENT_INDENT. Subclasses can override this method if
	 * they need different style bits.
	 * </p>
	 * 
	 * @return
	 */
	protected int getSectionStyle() {
		return Section.EXPANDED | Section.TITLE_BAR | Section.CLIENT_INDENT;
	}

	/**
	 * Create the actual content of the section.
	 * <p>
	 * This method is called during the initialization of this form part. Implementations can add
	 * their static user interface parts to the given parent here.
	 * </p>
	 * <p>
	 * The given parent composite does not have a layout. It is up to the implementation to add the
	 * appropriate layout to the composite.
	 * </p>
	 * 
	 * @param parent
	 * @param toolkit
	 */
	abstract protected void createSectionContent(Composite parent, FormToolkit toolkit);

	/**
	 * Update the content in reaction to changes in the source target configuration.
	 * <p>
	 * This method is called when the form part receives a <code>refresh()</code> event.
	 * Implementations can update the static parts of the user interface and/or create the dynamic
	 * parts (dependent on the current target configuration).
	 * </p>
	 */
	abstract protected void refreshSectionContent();

	/**
	 * Update the messages in the shared header of the editor.
	 * <p>
	 * This method is called every time the part becomes stale, i.e. a dependent attribute has
	 * changed its value. The implementation must then create or remove the appropriate messages via
	 * the message manager of this form part.
	 * </p>
	 * <p>
	 * Unlike {@link #refreshSectionContent()} this method will be called even when the part is not
	 * active and therefore not visible.
	 * </p>
	 * 
	 * @see #getMessageManager();
	 * 
	 */
	protected void refreshMessages() {
		// empty default.
	}

	/**
	 * Create the section which is the root control of this form part.
	 * <p>
	 * This method calls {@link #createSectionContent(Composite, FormToolkit)} from the subclass to
	 * fill the content of the section.
	 * </p>
	 * <p>
	 * If the {@link #getDescription()} method returns a non-<code>null</code> string, then a label
	 * with the description is created as the first child of the section. In this case it is
	 * important that the parent and all composites above it (up to the managed form body) use a
	 * TableWrapLayout so that the description will get wrapped correctly when the form is resized.
	 * </p>
	 * 
	 * @param parent
	 *            Parent composite from the {@link #setParent(Composite)} method.
	 * @param toolkit
	 *            the form toolkit
	 * @return the new section control.
	 */
	protected Control createContent(Composite parent, FormToolkit toolkit) {
		fSection = toolkit.createSection(parent, getSectionStyle());
		fSection.setText(getTitle());

		Composite sectionClient = toolkit.createComposite(fSection);
		String description = getDescription();

		if (description != null) {
			// Why not use section.setDescription()?
			// Well, I had problems with that:
			// a) This is implemented as an read-only Text control instead of a label, so the
			// description could get the focus, have a cursor and participate in the tab cycle - not
			// very professional looking!
			// b) Probably due to a) the description would flicker whenever the editor was resized.
			//
			// So instead we use a normal Label to display the description. To have the label wrap
			// correctly on resize it is put into a TableWrapLayout().
			sectionClient.setLayout(new TableWrapLayout());
			Label label = toolkit.createLabel(sectionClient, description, SWT.WRAP);
			label.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

			// Create new virgin Composite for the subclass content.
			Composite compo = toolkit.createComposite(sectionClient);
			compo.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
			fContentCompo = compo;
		} else {
			fContentCompo = sectionClient;
		}

		// Call the subclass to have it create its user interface stuff.
		createSectionContent(fContentCompo, toolkit);

		fSection.setClient(sectionClient);

		// If the Section is expandable we add a listener that will reflow the complete form
		// whenever our section is expanded or collapsed.
		if ((fSection.getExpansionStyle() & Section.TWISTIE) != 0
				|| (fSection.getExpansionStyle() & Section.TREE_NODE) != 0) {
			fSection.addExpansionListener(new ExpansionAdapter() {
				@Override
				public void expansionStateChanging(ExpansionEvent e) {
					// Do nothing
				}

				@Override
				public void expansionStateChanged(ExpansionEvent e) {
					getManagedForm().getForm().reflow(false);
				}
			});
		}

		return fSection;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.ui.editors.targets.ITCEditorPart#setParent(org.eclipse.swt.widgets.Composite
	 * )
	 */
	@Override
	public void setParent(Composite parent) {
		fParent = parent;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.ui.editors.targets.ITCEditorPart#setMessageManager(org.eclipse.ui.forms
	 * .IMessageManager)
	 */
	@Override
	public void setMessageManager(IMessageManager manager) {
		fMessageManager = manager;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void initialize(IManagedForm form) {
		// Only initialize once (to avoid duplicate UI elements)
		if (!fIsInitialized) {
			fIsInitialized = true;
			fManagedForm = form;

			if (fParent == null) {
				fParent = form.getForm().getBody();
			}

			createContent(fParent, form.getToolkit());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#setFormInput(java.lang.Object)
	 */
	@Override
	public boolean setFormInput(Object input) {
		if (input instanceof ITargetConfigurationWorkingCopy) {
			fTCWC = (ITargetConfigurationWorkingCopy) input;

			// Save the current values for dirty state tracking
			String[] managedAttributes = getPartAttributes();
			for (String attr : managedAttributes) {
				String currValue = fTCWC.getAttribute(attr);
				fLastValues.put(attr, currValue);
			}

			// Save the current values for stale state tracking
			String[] dependentAttributes = getDependentAttributes();
			for (String attr : dependentAttributes) {
				String currValue = fTCWC.getAttribute(attr);
				fLastDependentValues.put(attr, currValue);
			}

			// Add the listener to the config to do the stale state management
			fTCWC.addPropertyChangeListener(fListener);

			// Let the form redraw itself for the new input.
			refresh();

			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#refresh()
	 */
	@Override
	public void refresh() {
		refreshSectionContent();
		fIsStale = false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#commit(boolean)
	 */
	@Override
	public void commit(boolean onSave) {
		// if the form is actually saved (not just a page change),
		// then we take all managed attributes of the subclass and store their current
		// value. This is used to check if the form part is dirty or not.
		if (onSave) {
			String[] managedAttributes = getPartAttributes();
			for (String attr : managedAttributes) {
				String newvalue = fTCWC.getAttribute(attr);
				fLastValues.put(attr, newvalue);
			}
		}

		// this will in turn cause isDirty() to be called which will determine if this form part
		// still has unsaved changes.
		getManagedForm().dirtyStateChanged();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#dispose()
	 */
	@Override
	public void dispose() {

		// Remove the listener.
		// overriding classes need to call super.dispose();

		if (fTCWC != null) {
			fTCWC.removePropertyChangeListener(fListener);
		}

		Control control = getControl();
		if (control != null) {
			control.dispose();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#isDirty()
	 */
	@Override
	public boolean isDirty() {

		// Compare the current values of the target configuration with the last saved values. If at
		// least on of them is different, then the part is dirty.
		boolean isDirty = false;
		for (String attr : fLastValues.keySet()) {
			String currValue = fTCWC.getAttribute(attr);
			String lastValue = fLastValues.get(attr);
			if (!currValue.equals(lastValue)) {
				isDirty = true;
				break; // no need to compare further
			}
		}

		return isDirty;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#isStale()
	 */
	@Override
	public boolean isStale() {
		// The stale flag is set by the config change listener.
		return fIsStale;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.forms.IFormPart#setFocus()
	 */
	@Override
	public void setFocus() {
		if (fSection != null) {
			Control client = fSection.getClient();
			if (client != null) {
				client.setFocus();
			}
		}
	}

	/**
	 * Set the focus to the control that can change the given attribute.
	 * 
	 * @param attribute
	 *            A target configuration attribute.
	 * @return <code>false</code> if this form part has no controls for the attribute.
	 */
	@Override
	public boolean setFocus(String attribute) {
		// This is the default.
		// Subclasses should override to set the focus on the correct control for the attribute.
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.ui.editors.targets.ITCEditorPart#getControl()
	 */
	@Override
	public Section getControl() {
		return fSection;
	}

	/**
	 * Returns the Managed Form used by this form part.
	 * <p>
	 * This method must not be called before the part has been initialized.
	 * </p>
	 * 
	 * @return
	 */
	public IManagedForm getManagedForm() {
		Assert.isNotNull(fManagedForm, "getManagedForm() called before initialize()");
		return fManagedForm;
	}

	/**
	 * Gets the current message manager for this form.
	 * <p>
	 * If a message manager has not been set via {@link #setMessageManager(IMessageManager)}, then
	 * the message manager from the managed form is used.
	 * </p>
	 * <p>
	 * This method must not be called before the part has been initialized.
	 * </p>
	 * 
	 * @return
	 */
	protected IMessageManager getMessageManager() {
		if (fMessageManager == null) {
			Assert.isNotNull(fManagedForm, "getMessageManager() called before initialize()");
			fMessageManager = fManagedForm.getMessageManager();
		}
		return fMessageManager;
	}

	/**
	 * Get the target configuration that the editor works on.
	 * <p>
	 * This method must not be called before the form input has been set (
	 * {@link #setFormInput(Object)}).
	 * </p>
	 * 
	 * @return A reference to the current target configuration (working copy)
	 */
	protected ITargetConfigurationWorkingCopy getTargetConfiguration() {
		Assert.isNotNull(fTCWC, "getTargetConfiguration() called before setFormInput()");
		return fTCWC;
	}

	/**
	 * Get the width in pixels of the given String.
	 * <p>
	 * The width is calculated by using the current font of the given control.
	 * </p>
	 * 
	 * @param control
	 *            The parent control
	 * @param text
	 *            The text for which to get the width.
	 * @return Width in pixels.
	 */
	protected int calcTextWidth(Control control, String text) {
		GC gc = new GC(control);
		gc.setFont(control.getFont());
		int value = gc.stringExtent(text).x;
		gc.dispose();

		return value;
	}

	/**
	 * Enable / Disable the given Composite.
	 * <p>
	 * This method will call the <code>setEnabled(value)</code> method of all children of the given
	 * composite.
	 * </p>
	 * <p>
	 * Note: The method is not recursive. If any child is a composite itself, then its children will
	 * not be affected by this method.
	 * </p>
	 * 
	 * @param compo
	 *            A <code>Composite</code> with some controls.
	 * @param value
	 *            <code>true</code> to enable, <code>false</code> to disable the given composite.
	 */
	protected void setEnabled(Composite compo, boolean value) {
		Control[] children = compo.getChildren();
		for (Control child : children) {
			child.setEnabled(value);
		}
	}

	public interface IValidationListener {

		public void result(ValidationResult result);
	}

	/**
	 * Validate an attribute and set the form messages for the given control.
	 * <p>
	 * If the validation result is either {@link Result#WARNING} or {@link Result#ERROR}, then a
	 * message is added to the control with the result of the validation. If the validation
	 * indicates no problems ({@link Result#OK}), then any existing message for the attribute /
	 * control pair is removed.
	 * </p>
	 * 
	 * @param attribute
	 *            The attribute to be validated
	 * @param control
	 *            The control that will receive any warning or error messages.
	 */
	protected void validate(final String attribute, final Control control) {

		validate(attribute, control, null);
	}

	protected void validate(final String attribute, final Control control,
			final IValidationListener validationhook) {

		// Validation can take some time, so we run it in a separate background job.
		// Once the validation is completed we need to check if the control still exists (the user
		// might have closed the editor), and if yes we need to run the actual update in the GUI
		// thread again.
		Job validatejob = new Job("validate") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					monitor.beginTask("Validating " + attribute, 100);
					ITargetConfiguration tc = getTargetConfiguration();
					final ValidationResult res = tc.validateAttribute(attribute);
					monitor.worked(90);
					if (control != null && !control.isDisposed()) {
						Display display = control.getDisplay();
						if (display != null && !display.isDisposed()) {
							// The actual update is run in the GUI threat.
							display.syncExec(new Runnable() {

								@Override
								public void run() {
									IMessageManager msgmngr = getMessageManager();
									switch (res.result) {
										case OK:
											msgmngr.removeMessage(attribute, control);
											break;
										case WARNING:
											msgmngr.addMessage(attribute, res.description,
													attribute, IMessage.WARNING, control);
											break;
										case ERROR:
											msgmngr.addMessage(attribute, res.description,
													attribute, IMessage.ERROR, control);
											break;
										default:
											// do nothing
									}

									// Now execute the optional hook method from the caller
									if (validationhook != null) {
										validationhook.result(res);
									}
								}
							});
						}
					}
					monitor.worked(10);
				} catch (SWTException e) {
					// Probably the control has been disposed after the check.
					// In this case we just ignore the validation result.
					e.printStackTrace();
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};

		validatejob.setSystem(true);
		validatejob.setPriority(Job.SHORT);
		validatejob.schedule();
	}
}
