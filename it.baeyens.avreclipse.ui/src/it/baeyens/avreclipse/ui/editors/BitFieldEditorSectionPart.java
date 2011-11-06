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
 * $Id: BitFieldEditorSectionPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldValueDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValueChangeEvent;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.IByteValuesChangeListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.ColumnLayoutData;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;


/**
 * A <code>SectionPart</code> that can edit a single BitField.
 * <p>
 * This class takes a JFace Form Section and adds the Widgets needed to edit the value of a
 * BitField. Depending on the size of the BitField and the possible values from the
 * {@link BitFieldDescription} (derived from the part description files), the Section will use
 * different representations.
 * <ul>
 * <li>Single bit
 * <p>
 * <ul>
 * <li>no predefined values: <em>Yes</em> and <em>No</em> Radiobuttons.</li>
 * <li>Single bit, one predefined value: Checkbox.</li>
 * </ul>
 * </p>
 * <li>Multiple bits
 * <p>
 * <ul>
 * <li>no predefined values: Textbox for direct user input (decimal or hexadecimal).</li>
 * <li>up to 6 predefined values: Radiobuttons for all values.</li>
 * <li>up to 16 predefined values: Single drop down Combo.</li>
 * <li>more than 16 predefined values: Two drop down Combos, values split at first ";" character.</li>
 * </ul>
 * </p>
 * </ul>
 * </p>
 * <p>
 * This class automatically creates a <code>Section</code> and adds it to the parent composite.
 * After the class has been instantiated it must be added to a <code>IManagedForm</code> to
 * participate in the lifecycle management of the managed form.
 * 
 * <pre>
 *     Composite parent = ...
 *     FormToolkit toolkit = ...
 *     IManagedForm managedForm = ...
 *     BitFieldDescription bfd = ...
 *     
 *     IFormPart part = new BitFieldEditorSectionPart(parent, toolkit, Section.TITLE_BAR, bfd);
 *     managedForm.addPart(part);
 * </pre>
 * 
 * </p>
 * <p>
 * This class implements the {@link IFormPart} interface to participate in the lifecycle management
 * of a managed form. To set the value of the BitField use
 * 
 * <pre>
 *     ByteValues bytevalues = ...
 *     managedForm.setInput(bytevalues);
 * </pre>
 * 
 * The <code>ByteValues</code> passed to the managedForm is the model for this
 * <code>SectionPart</code>. Unlike normal IFormParts all changes to the source ByteValues are
 * applied immediately, because other BitFields or even other editors might be affected by the
 * change. Therefore this class uses its own dirty / stale management and does not use the one
 * provided by the superclass {@link SectionPart}.
 * </p>
 * <p>
 * This part also adds itself as a listener for changes to the ByteValues model. If the BitField
 * managed by this section gets changed from outside, then this part is marked as stale. The new
 * value will be set during the refresh method.
 * 
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class BitFieldEditorSectionPart extends SectionPart implements IByteValuesChangeListener {

	/** The BitField this Section should take care of. */
	private final BitFieldDescription	fBFD;

	/**
	 * The model for this <code>SectionPart</code>. Will only be written to in the
	 * {@link #commit(boolean)} method.
	 */
	private ByteValues					fByteValues;

	/** Last clean BitField value. Used to check if this part is currently dirty. */
	private int							fLastCleanValue	= -1;

	/** Current BitField value. Used to check if this part is currently stale. */
	private int							fCurrentValue	= -1;

	/**
	 * Part is currently committing the value. Used to inhibit reacting to ByteValueChangeEvents
	 * caused by this class.
	 */
	private boolean						fInCommit		= false;

	/** Part is currently refreshing. Used to inhibit any modification listeners. */
	private boolean						fInRefresh		= false;

	/**
	 * The Control that handles the different visual representations.
	 * 
	 * @see IOptionPart
	 */
	private IOptionPart					fOptionPart;

	/**
	 * Create a new <code>SectionPart</code> to handle a single BitField.
	 * <p>
	 * This constructor automatically creates a new section part inside the provided parent and
	 * using the provided toolkit.
	 * </p>
	 * 
	 * @param parent
	 *            the parent
	 * @param toolkit
	 *            the toolkit to use
	 * @param style
	 *            the section widget style
	 * @param description
	 *            <code>BitFieldDescription</code> for the BitField.
	 */
	public BitFieldEditorSectionPart(Composite parent, FormToolkit toolkit, int style,
			BitFieldDescription description) {
		super(parent, toolkit, style);

		fBFD = description;

		getSection().setLayoutData(new ColumnLayoutData(200, SWT.DEFAULT));
		getSection().setText(fBFD.getName() + " - " + fBFD.getDescription());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);

		Section parent = getSection();
		FormToolkit toolkit = form.getToolkit();

		// Create the Section client area.
		// The layout of the client area is set later in the individual IOptionPart classes
		Composite clientarea = form.getToolkit().createComposite(parent);
		parent.setClient(clientarea);

		// Determine the number of possible options and the list of possible values.
		// Then create a new IOptionPart according to the rules described in the class JavaDoc.

		int maxoptions = fBFD.getMaxValue();
		List<BitFieldValueDescription> allvalues = fBFD.getValuesEnumeration();

		if (maxoptions == 1 && allvalues.size() == 0) {
			fOptionPart = new OptionYesNo();

		} else if (maxoptions == 1) {
			fOptionPart = new OptionCheckbox();

		} else if (/* maxoptions > 1 && */allvalues.size() == 0) {
			fOptionPart = new OptionText();

		} else if (/* maxoptions > 1 && */allvalues.size() < 6) {
			fOptionPart = new OptionRadioButtons();

		} else {
			// Check if all possible values are splitable. It is splitable if
			// it contains (at least) one ';'
			// While at the time it would be enough to just check the number of values this more
			// expensive check is here to ensure that even future MCUs will not cause problems.
			// (OptionDualCombo will probably fail if even a single value has no ';' in it.)
			boolean splitable = true;
			for (BitFieldValueDescription bfvd : allvalues) {
				if (bfvd.getDescription().indexOf(';') == -1) {
					splitable = false;
					break;
				}
			}
			if (splitable && allvalues.size() > 16) {
				fOptionPart = new OptionDualCombo();
			} else {
				fOptionPart = new OptionSingleCombo();
			}
		}

		fOptionPart.addControl(clientarea, toolkit, fBFD);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	@Override
	public void dispose() {
		if (fByteValues != null) {
			fByteValues.removeChangeListener(this);
		}
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#setFormInput(java.lang.Object)
	 */
	@Override
	public boolean setFormInput(Object input) {

		if (!(input instanceof ByteValues)) {
			return false;
		}

		fByteValues = (ByteValues) input;
		fByteValues.addChangeListener(this);

		refresh();
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	@Override
	public void refresh() {
		// refresh() was once called before setInput(), but I am not sure if this was a bug in the
		// Editor. I have left this test just in case, even if it is probably not required.
		if (fByteValues == null) {
			return;
		}

		int value = fByteValues.getNamedValue(fBFD.getName());
		fInRefresh = true;
		fOptionPart.setValue(value);
		fInRefresh = false;
		fLastCleanValue = fCurrentValue = value;

		super.refresh();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	@Override
	public void commit(boolean onSave) {
		fLastCleanValue = fCurrentValue = fByteValues.getNamedValue(fBFD.getName());
		super.commit(onSave);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		// This part is dirty if the source ByteValues has a different value than what it had on the
		// last setInput(), refresh() or commit()
		try {
			if (fByteValues.getNamedValue(fBFD.getName()) != fLastCleanValue) {
				return true;
			}
		} catch (IllegalArgumentException iae) {
			// The ByteValues source has changed (but our parent editorpart has not yet received
			// news about this)
			// In this case we consider ourself to be clean as this section will be
			// disposed of shortly.
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#isStale()
	 */
	@Override
	public boolean isStale() {
		// This part is stale if the source ByteValues has a different value than what this part
		// thinks it should have.
		if (fByteValues.getNamedValue(fBFD.getName()) != fCurrentValue) {
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.fuses.IByteValuesChangeListener#byteValuesChanged(it.baeyens.avreclipse.core.toolinfo.fuses.ByteValueChangeEvent[])
	 */
	@Override
	public void byteValuesChanged(ByteValueChangeEvent[] events) {

		if (fInCommit) {
			// don't listen to our own changes to the BitField
			return;
		}

		// go through all events and if any event changes our
		// BitField to a different value then mark ourself as (probably) stale.
		for (ByteValueChangeEvent event : events) {
			if (event.name.equals(fBFD.getName())) {
				// Our stale state might have changed, depending on the new value.
				// Inform the parent ManagedForm - it will call our isStale() implementation to get
				// the actual state.
				getManagedForm().staleStateChanged();
			}
		}
	}

	/**
	 * Sets the value of this Section part.
	 * <p>
	 * This is called by the <code>IOptionPart</code>s when the user has changed the selection.
	 * The new value is applied immediately to the model and the parent IManagedForm is informed
	 * that the dirty state might have changed.
	 * </p>
	 * 
	 * @param newvalue
	 *            The new value from the user selection.
	 */
	private void internalSetValue(int newvalue) {
		fInCommit = true;
		fByteValues.setNamedValue(fBFD.getName(), newvalue);
		fInCommit = false;

		fCurrentValue = newvalue;

		// Our dirty state might have changed, depending on the new value.
		// Inform the parent ManagedForm - it will call our isDirty() implementation to get the
		// actual state.
		getManagedForm().dirtyStateChanged();
	}

	/**
	 * A simple interface to abstract the different visual representations of a BitField.
	 * <p>
	 * All six different presentations are implemented as classes implementing this interface. The
	 * parent class will instantiate one of them and then use the interface to initialize them
	 * (generate the UI) and to set their value.
	 * </p>
	 */
	private interface IOptionPart {

		/**
		 * Generate a user interface for a <code>BitFieldDescription</code>.
		 * 
		 * @param parent
		 *            Composite to which the generated Widgets are added. Layout will be set by this
		 *            method.
		 * @param toolkit
		 *            for the visual style of the form.
		 * @param bfd
		 *            source BitFieldDescription.
		 */
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd);

		/**
		 * Set the new BitField value.
		 * <p>
		 * The OptionPart will update itself to show the new value. The new value may be
		 * <code>-1</code>. The implementation should try to visualize this if possible.
		 * </p>
		 * 
		 * @param value
		 *            <code>int</code> between <code>0</code> and
		 *            {@link BitFieldDescription#getMaxValue()}. The value is not checked. Illegal
		 *            values may cause undetermined behaviour.
		 */
		public void setValue(int value);
	}

	/**
	 * <code>IOptionPart</code> to represent a single bit as "Yes" / "No" Radio buttons.
	 * <p>
	 * Note: a <code>0</code> is interpreted as <em>Yes</em>, while a <code>1</code> is
	 * <em>No</em>. This is according to the Atmel definition for fuses and them being flash
	 * memory bits, where unset means <code>1</code>.
	 * </p>
	 */
	private class OptionYesNo implements IOptionPart {

		private Button	fYesButton;
		private Button	fNoButton;

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#addControl(org.eclipse.swt.widgets.Composite,   org.eclipse.ui.forms.widgets.FormToolkit,    it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldDescription)
		 */
		@Override
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd) {

			parent.setLayout(new GridLayout(2, false));

			fYesButton = toolkit.createButton(parent, "Yes", SWT.RADIO);
			fNoButton = toolkit.createButton(parent, "No", SWT.RADIO);

			Listener listener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}
					int value = (event.widget == fYesButton) ? 0x00 : 0x01;
					internalSetValue(value);
				}
			};
			fYesButton.addListener(SWT.Selection, listener);
			fNoButton.addListener(SWT.Selection, listener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#setValue(int)
		 */
		@Override
		public void setValue(int value) {
			if (value == -1) {
				fYesButton.setSelection(false);
				fNoButton.setSelection(false);
			} else {
				boolean valueYes = value == 0 ? true : false;
				fYesButton.setSelection(valueYes);
				fNoButton.setSelection(!valueYes);
			}
		}
	}

	/**
	 * <code>IOptionPart</code> to represent a single bit with one possible option as a CheckBox
	 * button.
	 * <p>
	 * While rare, some fuses descriptions contain a single bit with a single value. If the CheckBox
	 * is selected, then the BitField value is set to the single value. If the CheckBox is not
	 * selected, the negation of the value is used.
	 * </p>
	 */
	private class OptionCheckbox implements IOptionPart {

		private Button	fCheckButton;
		private int		fSetValue;


		@Override
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd) {

			parent.setLayout(new FillLayout());

			fSetValue = bfd.getValuesEnumeration().get(0).getValue();
			String buttontext = bfd.getValuesEnumeration().get(0).getDescription();
			fCheckButton = toolkit.createButton(parent, buttontext, SWT.CHECK);

			Listener listener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}
					int value = fCheckButton.getSelection() ? fSetValue : 0x01 & ~fSetValue;
					internalSetValue(value);
				}
			};
			fCheckButton.addListener(SWT.Selection, listener);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#setValue(int)
		 */
		@Override
		public void setValue(int value) {
			if (value == -1) {
				fCheckButton.setGrayed(true);
			} else {
				fCheckButton.setGrayed(false);
				boolean valueYes = value == fSetValue ? true : false;
				fCheckButton.setSelection(valueYes);
			}
		}

	}

	/**
	 * <code>IOptionPart</code> to represent all legal values for the BitField as radio buttons.
	 */
	private class OptionRadioButtons implements IOptionPart {

		private Button[]	fButtons;
		private int[]		fValues;


		@Override
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd) {

			parent.setLayout(new RowLayout(SWT.VERTICAL));

			List<BitFieldValueDescription> allvalues = bfd.getValuesEnumeration();
			fButtons = new Button[allvalues.size()];
			fValues = new int[allvalues.size()];

			Listener listener = new Listener() {

				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}
					int value = -1;
					for (int i = 0; i < fButtons.length; i++) {
						if (fButtons[i] == event.widget) {
							value = fValues[i];
						}
					}
					internalSetValue(value);
				}
			};

			for (int i = 0; i < allvalues.size(); i++) {
				BitFieldValueDescription bfvd = allvalues.get(i);
				fButtons[i] = toolkit.createButton(parent, bfvd.getDescription(), SWT.RADIO);
				fButtons[i].addListener(SWT.Selection, listener);
				fValues[i] = bfvd.getValue();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#setValue(int)
		 */
		@Override
		public void setValue(int value) {
			for (int i = 0; i < fValues.length; i++) {
				if (fValues[i] == value) {
					fButtons[i].setSelection(true);
				} else {
					fButtons[i].setSelection(false);
				}
			}
		}

	}

	/**
	 * <code>IOptionPart</code> to represent all legal values for the BitField as two combos.
	 * <p>
	 * Two combos are used to split very long lists of values into two more managable lists. All
	 * values are split at the first ';' character in their description and group according to the
	 * first part of the description.
	 * </p>
	 * <p>
	 * Whenever the first root combo changes its value, the second combo is filled with the
	 * appropriate subitems.
	 * </p>
	 */
	private class OptionDualCombo implements IOptionPart {

		private Combo								fRootCombo;
		private Combo								fSubCombo;

		/**
		 * The list of root names, once filled this is static and used as the content for the root
		 * combo.
		 */
		private String[]							fRootTexts;

		/**
		 * Map for all possible values to the index of the root and the sub combo. For undefined
		 * values this will map to <code>{-1,-1}</code>.
		 */
		private int[][]								fReverseLookup;

		/**
		 * Map of root names to a list of all possible sub names. This list is used to fill the sub
		 * combo.
		 */
		private final Map<String, List<String>>		fRootToSubnames	= new HashMap<String, List<String>>();

		/**
		 * Map of root names to a list of values. The list is contains the value associated for the
		 * sub name at the same index.
		 */
		private final Map<String, List<Integer>>	fRootToValues	= new HashMap<String, List<Integer>>();


		@Override
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd) {

			parent.setLayout(new GridLayout(2, false));

			List<BitFieldValueDescription> allvalues = bfd.getValuesEnumeration();

			// initialize the reverse lookup array and fill it with {-1, -1} to indicate
			// undefined BitField values.
			int maxvalue = bfd.getMaxValue();
			fReverseLookup = new int[maxvalue + 1][];
			Arrays.fill(fReverseLookup, new int[] { -1, -1 });

			List<String> rootnames = new ArrayList<String>();

			// iterate over all BitFieldValueDescriptions and split their names.
			// If the first part is new, then add it to the list of rootnames.
			// The second part and the values are added to the lists associated with the rootname
			// from the two <code>rootTo...</code> HashMaps.
			// Finally the value of the BitFieldValueDescription is added to the reverse lookup map
			// with the current indices for root and sub combo.
			String lastroottext = null;
			for (BitFieldValueDescription bfvd : allvalues) {
				String desc = bfvd.getDescription();
				int value = bfvd.getValue();

				// Split the current text
				int splitat = desc.indexOf(';');
				String firstpart = desc.substring(0, splitat).trim();
				if (!firstpart.equalsIgnoreCase(lastroottext)) {
					rootnames.add(firstpart);
					fRootToSubnames.put(firstpart, new ArrayList<String>());
					fRootToValues.put(firstpart, new ArrayList<Integer>());
					lastroottext = firstpart;
				}
				String subtext = desc.substring(splitat + 1).trim();
				List<String> subnames = fRootToSubnames.get(firstpart);
				subnames.add(subtext);
				List<Integer> subvalues = fRootToValues.get(firstpart);
				subvalues.add(value);

				// Map the index values of both name parts to the value, so we can get the
				// indices for a given value.
				// As we fill the arrays sequentially we can just take the size of the arrays to
				// get the index of the last addition (instead of the more expensive
				// list.indexOf())
				fReverseLookup[value] = new int[] { rootnames.size() - 1, subnames.size() - 1 };
			}

			// Convert the root list to an array usable as content for the root combo.
			fRootTexts = rootnames.toArray(new String[rootnames.size()]);

			// Now create the root combo
			fRootCombo = new Combo(parent, SWT.READ_ONLY);
			toolkit.adapt(fRootCombo);
			fRootCombo.setItems(fRootTexts);
			fRootCombo.setVisibleItemCount(fRootTexts.length);
			fRootCombo.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}
					int index = fRootCombo.getSelectionIndex();
					String name = fRootTexts[index];
					List<String> subnames = fRootToSubnames.get(name);
					String[] subnamesarray = subnames.toArray(new String[subnames.size()]);

					// remember the last selection of the sub combo. If it is still in range for the
					// new sub combo content then the selection index is reset to it after we have
					// set the new content. Often the same index has the same description, so the
					// user won't see a change in the sub combo, even though the actual BitField
					// values are totally different.
					int oldselection = fSubCombo.getSelectionIndex();
					fSubCombo.setItems(subnamesarray);
					fSubCombo.setVisibleItemCount(subnamesarray.length);

					if (0 <= oldselection && oldselection < subnamesarray.length) {
						fSubCombo.select(oldselection);
					} else {
						fSubCombo.select(0);
					}

					// Because the root combo does not affect the value directly we need to call the
					// selection event handler of the sub combo so that internalSetValue() gets
					// actually called.
					// Combo.select(x) unfortunatly does not do this automatically.
					fSubCombo.notifyListeners(SWT.Selection, new Event());
				}
			});

			// ... and now the sub combo
			fSubCombo = new Combo(parent, SWT.READ_ONLY);
			toolkit.adapt(fSubCombo);
			fSubCombo.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}
					List<Integer> subvalues;
					int rootindex = fRootCombo.getSelectionIndex();
					String name = fRootTexts[rootindex];
					subvalues = fRootToValues.get(name);
					int subindex = fSubCombo.getSelectionIndex();
					int newvalue = subvalues.get(subindex);

					internalSetValue(newvalue);
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#setValue(int)
		 */
		@Override
		public void setValue(int value) {
			if (value == -1) {
				fRootCombo.clearSelection();
				fSubCombo.clearSelection();
				return;
			}

			int[] indices = fReverseLookup[value];

			if (indices[0] == -1) {
				// invalid value: deselect both combos
				fRootCombo.select(-1);
				fSubCombo.select(-1);
				return;
			}

			fRootCombo.select(indices[0]);
			String name = fRootTexts[indices[0]];
			List<String> subnames = fRootToSubnames.get(name);
			String[] subnamesarray = subnames.toArray(new String[subnames.size()]);
			fSubCombo.setItems(subnamesarray);
			fSubCombo.setVisibleItemCount(subnamesarray.length);
			fSubCombo.select(indices[1]);

		}

	}

	/**
	 * <code>IOptionPart</code> to represent all legal values for the BitField in a drop down
	 * combo.
	 */
	private class OptionSingleCombo implements IOptionPart {

		private Combo		fCombo;

		/**
		 * The list of value names. Once filled this is static and used as the content for the
		 * combo.
		 */
		private String[]	fTexts;

		/**
		 * List of values. Once filled this is static and used to map the index of the combo to the
		 * actual BitField value.
		 */
		private Integer[]	fValues;

		/**
		 * Map for all possible values to the index of the combo. For undefined values this will map
		 * to <code>-1</code>.
		 */
		private int[]		fReverseLookup;


		@Override
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd) {

			parent.setLayout(new RowLayout(SWT.HORIZONTAL));

			List<BitFieldValueDescription> allvalues = bfd.getValuesEnumeration();

			// initialize the reverse lookup array and fill it with {-1} to indicate
			// undefined BitField values.
			int maxvalue = bfd.getMaxValue();
			fReverseLookup = new int[maxvalue + 1];
			Arrays.fill(fReverseLookup, -1);

			// Iterate over all BitFieldValueDescriptions and extract name and value.
			List<String> names = new ArrayList<String>();
			List<Integer> values = new ArrayList<Integer>();
			for (BitFieldValueDescription bfvd : allvalues) {
				String desc = bfvd.getDescription();
				int value = bfvd.getValue();

				names.add(desc);
				values.add(value);
				fReverseLookup[value] = names.size() - 1;
			}

			// Convert the lists to arrays for easier use with the combo
			fTexts = names.toArray(new String[names.size()]);
			fValues = values.toArray(new Integer[values.size()]);

			// and create the combo
			fCombo = new Combo(parent, SWT.READ_ONLY);
			toolkit.adapt(fCombo);
			fCombo.setItems(fTexts);
			fCombo.setVisibleItemCount(fTexts.length);
			fCombo.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}

					int index = fCombo.getSelectionIndex();
					int newvalue = fValues[index];

					internalSetValue(newvalue);
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#setValue(int)
		 */
		@Override
		public void setValue(int value) {
			if (value == -1) {
				fCombo.clearSelection();
				fCombo.deselectAll();
				return;
			}

			int index = fReverseLookup[value];
			fCombo.select(index);

		}
	}

	/**
	 * <code>IOptionPart</code> to edit the value of a BitField directly in a text box.
	 * <p>
	 * The new value can be entered either as decimal, hexadecimal or (for oldtimers) as octal. The
	 * new value is decoded by the {@link Integer#decode(String)} method, so all its features and
	 * restrictions apply to this class.
	 * </p>
	 * <p>
	 * If the entered value is illegal, either out of range or malformed, then the text is shown in
	 * red.
	 * </p>
	 */
	private class OptionText implements IOptionPart {

		/**
		 * Highest integer value that can be entered. Higher values will be ignored and the error is
		 * visualized.
		 */
		private int		fMaxValue;

		private Text	fText;


		@Override
		public void addControl(Composite parent, FormToolkit toolkit, BitFieldDescription bfd) {

			parent.setLayout(new RowLayout());

			fMaxValue = bfd.getMaxValue();

			fText = toolkit.createText(parent, "", SWT.NONE);
			fText.setTextLimit(5);
			fText.setToolTipText("Decimal, Hexadecimal (0x..) or Octal (0...)");
			fText.addListener(SWT.Modify, new Listener() {
				// The Modify Listener checks if the value is valid.
				// If yes then the value is set in the parent,
				// if no then the foreground is colored red.
				@Override
				public void handleEvent(Event event) {
					if (fInRefresh) {
						// don't listen to events originating from refresh()
						return;
					}
					try {
						int value = Integer.decode(fText.getText());
						if (value <= fMaxValue) {
							fText.setForeground(fText.getDisplay().getSystemColor(SWT.COLOR_BLACK));
							internalSetValue(value);
							return;
						}
					} catch (NumberFormatException nfe) {
					}
					fText.setForeground(fText.getDisplay().getSystemColor(SWT.COLOR_RED));
				}
			});
			fText.addVerifyListener(new VerifyListener() {
				// The verify listener to only accept (hex) digits and convert them to
				// upper case
				@Override
				public void verifyText(VerifyEvent event) {
					String text = event.text.toUpperCase();
					text = text.replace('X', 'x');
					if (!text.matches("[0-9A-Fx]*")) {
						event.doit = false;
					}
					event.text = text;
				}
			});

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see it.baeyens.avreclipse.ui.editors.BitFieldEditorSectionPart.IOptionPart#setValue(int)
		 */
		@Override
		public void setValue(int value) {
			if (value == -1) {
				fText.setText("");
			} else {
				fText.setText("0x" + Integer.toHexString(value));
			}
		}

	}

}
