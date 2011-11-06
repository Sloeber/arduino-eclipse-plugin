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
 * $Id: FuseBytePreviewControl.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.controls;

import it.baeyens.avreclipse.core.toolinfo.fuses.BitFieldDescription;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.ConversionResults.ConversionStatus;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;


/**
 * Custom SWT Control to display the values of a {@link ByteValues} object.
 * <p>
 * This class uses a Tree to display the values of each byte and their associated bitfields. There
 * is also a Label at the top to indicate MCU and Fuse/Lockbit type.
 * </p>
 * <p>
 * It is extended from <code>Composite</code>, but should be used like a <code>Control</code>.
 * For example the {@link #setLayout(org.eclipse.swt.widgets.Layout)} method should not be used,
 * because this class already uses its own layout.
 * </p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>BORDER</dd>
 * <dt><b>Events:</b></dt>
 * <dd>(none)</dd>
 * </dl>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class FuseBytePreviewControl extends Composite {

	// The GUI Widgets
	private final Tree				fTree;
	private final Label				fHeaderLabel;

	/**
	 * The MCU of the values currently displayed. Used to determine if the Tree can be updated or
	 * has to be redrawn for a new ByteValue Object.
	 */
	private String					fMCUid;

	private ByteValues				fCurrentValues;

	/** List of the root TreeItems, the items representing complete Bytes. */
	private final List<TreeItem>	fRootItems				= new ArrayList<TreeItem>();

	/** The font used for the normal bitfield items */
	private final Font				fDialogFont				= JFaceResources.getDialogFont();

	/** The bold font used for the root items */
	private final Font				fBoldDialogFont			= JFaceResources
																	.getFontRegistry()
																	.getBold(
																			JFaceResources.DIALOG_FONT);
	// The three columns used by the tree
	private final static int		COLUMN_NAME				= 0;
	private final static int		COLUMN_VALUE			= 1;
	private final static int		COLUMN_BITS				= 2;

	// Flags to indicated if short or long column content is requested
	private boolean					fShortName				= false;
	private boolean					fShortValue				= false;

	// And the associated column header strings
	private final static String[]	COLUMN_HEADER_NAMES		= new String[] { "Name (short)",
			"Name (full)"									};
	private final static String[]	COLUMN_HEADER_VALUES	= new String[] { "Value (hex)",
			"Value (text)"									};

	// Tags used for the TreeItem.setData(String tag, value) method to pass
	// meta-information about the item to the PaintListener
	/** Property for the bitfield description (BitFieldDescription). */
	private final static String		TAG_BITFIELDDESCRIPTION	= "bfd";

	/** Property for the current value of the byte containing the bitfield (Integer). */
	private final static String		TAG_VALUE				= "value";

	/**
	 * Property to indicate that the bits for this item should be drawn with thicker lines
	 * (Boolean).
	 */
	private final static String		TAG_BOLD				= "bold";

	/**
	 * Used by the ToolTipListener to pass the current item to the
	 * <code>Shell</code< showing the ToolTip (TreeItem).
	 */
	private final static String		TAG_TREEITEM			= "treeitem";

	/**
	 * Constructs a new instance of this class given its parent and a style value describing its
	 * behavior and appearance.
	 * <p>
	 * The style value is either one of the style constants defined in class SWT which is applicable
	 * to instances of this class, or must be built by bitwise OR'ing together (that is, using the
	 * int "|" operator) two or more of those SWT style constants. The class description lists the
	 * style constants that are applicable to the class. Style bits are also inherited from
	 * superclasses.
	 * 
	 * @see SWT#BORDER
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new instance (cannot be null)
	 * 
	 * @param style
	 *            the style of control to construct
	 * @throws IllegalArgumentException
	 *             <ul>
	 *             <li> ERROR_NULL_ARGUMENT - if the parent is null </li>
	 *             </ul>
	 * @throws SWTException
	 *             <ul>
	 *             <li> ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the
	 *             parent</li>
	 *             <li> ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass </li>
	 *             </ul>
	 */
	public FuseBytePreviewControl(Composite parent, int style) {
		super(parent, style);

		GridLayout layout = new GridLayout(1, false);
		setLayout(layout);

		// The label at the top.
		fHeaderLabel = new Label(this, SWT.NONE);
		fHeaderLabel.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

		Composite treecomposite = new Composite(this, SWT.NONE);
		treecomposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// The content Tree
		fTree = new Tree(treecomposite, SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		fTree.setHeaderVisible(true);
		fTree.setBackground(this.getBackground());
		fTree.setLinesVisible(true);

		// Add the listeners required for the bits column
		Listener paintlistener = new PaintListener();
		fTree.addListener(SWT.MeasureItem, paintlistener);
		fTree.addListener(SWT.PaintItem, paintlistener);
		fTree.addListener(SWT.EraseItem, paintlistener);

		// Add the listeners required for the (fake) tooltips
		Listener tooltiplistener = new ToolTipListener();
		fTree.addListener(SWT.Dispose, tooltiplistener);
		fTree.addListener(SWT.KeyDown, tooltiplistener);
		fTree.addListener(SWT.MouseMove, tooltiplistener);
		fTree.addListener(SWT.MouseHover, tooltiplistener);

		// We have three columns for: Name, Value (as Text) and Value (as single bits)

		final TreeColumn namecolumn = new TreeColumn(fTree, SWT.LEFT);
		namecolumn.setWidth(100);
		namecolumn.setText(COLUMN_HEADER_NAMES[fShortName ? 0 : 1]);
		namecolumn.setResizable(true);
		namecolumn.addSelectionListener(new SelectionAdapter() {
			// Toggle between short and long content for the name column
			@Override
			public void widgetSelected(SelectionEvent e) {
				fShortName = !fShortName;
				namecolumn.setText(COLUMN_HEADER_NAMES[fShortName ? 0 : 1]);
				updateTree(fCurrentValues);
			}
		});

		final TreeColumn valuecolumn = new TreeColumn(fTree, SWT.LEFT);
		valuecolumn.setWidth(100);
		valuecolumn.setText(COLUMN_HEADER_VALUES[fShortValue ? 0 : 1]);
		valuecolumn.setResizable(true);
		valuecolumn.addSelectionListener(new SelectionAdapter() {
			// Toggle between short and long content for the values column
			@Override
			public void widgetSelected(SelectionEvent e) {
				fShortValue = !fShortValue;
				valuecolumn.setText(COLUMN_HEADER_VALUES[fShortValue ? 0 : 1]);
				updateTree(fCurrentValues);
			}
		});

		TreeColumn bitscolumn = new TreeColumn(fTree, SWT.LEFT);
		bitscolumn.setWidth(100);
		bitscolumn.setText("Bits");
		// bitscolumn.setResizable(true);

		// Set the Layout
		TreeItem item = new TreeItem(fTree, SWT.NONE);
		TreeColumnLayout treelayout = new TreeColumnLayout();
		treecomposite.setLayout(treelayout);

		treelayout.setColumnData(namecolumn, new ColumnWeightData(35));
		treelayout.setColumnData(valuecolumn, new ColumnWeightData(65));
		treelayout.setColumnData(bitscolumn, new ColumnPixelData(item.getBounds().height * 8 + 2,
				false, true));

	}

	/**
	 * Sets the ByteValues Objects whose contents are shown in the control.
	 * <p>
	 * If the <code>newvalues</code> parameter is <code>null</code> then the control is blanked
	 * except for a short message at the top.
	 * </p>
	 * 
	 * @param newvalues
	 *            <code>ByteValues</code> Object or <code>null</code>
	 */
	public void setByteValues(ByteValues newvalues) {

		if (newvalues == null) {
			clearTree();
			fHeaderLabel.setText("No values set");
			return;
		}

		fCurrentValues = newvalues;

		String type = newvalues.getType().toString();
		String mcu = AVRMCUidConverter.id2name(newvalues.getMCUId());

		String header = MessageFormat.format("{0}  {1} preview", mcu, type);
		fHeaderLabel.setText(header);

		// Check if the mcu has changed.
		// If yes refill the complete table, otherwise only update the bitfields.
		if (!newvalues.getMCUId().equals(fMCUid)) {
			reloadTree(newvalues);
			fMCUid = newvalues.getMCUId();
		} else {
			updateTree(newvalues);
		}

		colorTree(newvalues);

		// Expand all tree items and repack the columns so that always all information is
		// shown (even if this means that the scrollbars have to be used).
		for (TreeItem item : fRootItems) {
			item.setExpanded(true);
		}

		fTree.getColumn(COLUMN_BITS).pack(); // force redraw of the Bits column
	}

	/**
	 * Clear the current tree and refill it with the content of the <code>ByteValues</code>
	 * 
	 * @param newvalues
	 *            Valid <code>ByteValues</code> object.
	 */
	private void reloadTree(ByteValues newvalues) {
		clearTree();

		// First we get all bytes and create the root tree items, one for each byte.
		int[] values = newvalues.getValues();
		int i = 0;
		for (int value : values) {
			TreeItem byteitem = new TreeItem(fTree, SWT.NONE);
			byteitem.setData(TAG_VALUE, newvalues.getValue(i));
			byteitem.setData(TAG_BOLD, true);
			byteitem.setText(COLUMN_NAME, newvalues.getByteName(i++));
			byteitem.setText(COLUMN_VALUE, toHex(value));
			byteitem.setText(COLUMN_BITS, "");
			byteitem.setFont(fBoldDialogFont);
			fRootItems.add(byteitem);
		}

		// Now we get a list of all bitfields that the ByteValues object has and
		// add them to the tree as well.
		List<BitFieldDescription> alldescriptions = newvalues.getBitfieldDescriptions();

		// Sort the bitfield descriptions according to their masks in descending order;
		Collections.sort(alldescriptions, new Comparator<BitFieldDescription>() {
			@Override
			public int compare(BitFieldDescription o1, BitFieldDescription o2) {
				int mask1 = o1.getMask();
				int mask2 = o2.getMask();
				return mask2 - mask1;
			}
		});

		for (BitFieldDescription bitfield : alldescriptions) {
			int byteindex = bitfield.getIndex();
			TreeItem parent = fRootItems.get(byteindex);
			TreeItem newitem = new TreeItem(parent, SWT.NONE);
			newitem.setFont(fDialogFont);

			newitem.setData(TAG_BITFIELDDESCRIPTION, bitfield);
			newitem.setData(TAG_VALUE, newvalues.getValue(byteindex));

			String name = bitfield.getName();
			String desc = bitfield.getDescription();
			newitem.setText(COLUMN_NAME, fShortName ? name : desc);

			String valuetext = newvalues.getNamedValueText(name);
			int value = newvalues.getNamedValue(name);
			newitem.setText(COLUMN_VALUE, fShortValue ? toHex(value) : valuetext);
		}
	}

	/**
	 * Update the tree with the content from the ByteValues object.
	 * <p>
	 * The newvalues object must have the same MCU id as the currently displayed one, otherwise this
	 * method will fail.
	 * </p>
	 * 
	 * @param newvalues
	 *            Valid <code>ByteValues</code> object.
	 */
	private void updateTree(ByteValues newvalues) {

		int[] values = newvalues.getValues();

		// Iterate through all tree items and change the value of each item to the new values.
		for (int i = 0; i < fRootItems.size(); i++) {

			int value = values[i];
			TreeItem byteitem = fRootItems.get(i);
			byteitem.setText(COLUMN_VALUE, toHex(value));
			byteitem.setData(TAG_VALUE, value);

			TreeItem[] bitfielditems = byteitem.getItems();
			for (TreeItem item : bitfielditems) {
				item.setData(TAG_VALUE, value);
				BitFieldDescription bfd = (BitFieldDescription) item
						.getData(TAG_BITFIELDDESCRIPTION);
				String name = bfd.getName();
				int bitfieldvalue = newvalues.getNamedValue(name);
				String valuetext = newvalues.getNamedValueText(name);
				item.setText(COLUMN_NAME, fShortName ? name : bfd.getDescription());
				item.setText(COLUMN_VALUE, fShortValue ? toHex(bitfieldvalue) : valuetext);
			}
		}
	}

	/**
	 * Color the BitFields in the tree according to their {@link ConversionStatus}.
	 * <p>
	 * The colors used are:
	 * <ul>
	 * <li><em>Green</em>: The BitField was successfully converted</li>
	 * <li><em>Yellow</em>: The BitField was converted, but the new value is different</li>
	 * <li><em>Red</em>: The BitField did not exist in the source MCU and was set to the default
	 * value</li>
	 * <li><em>Black</em>: The BitField has been modified after the conversion.</li>
	 * </ul>
	 * 
	 * @param values
	 *            The <code>ByteValues</code> after the conversion.
	 */
	private void colorTree(ByteValues values) {

		// Get the colors used to indicate a conversion result
		final Color colorBlack = fTree.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);
		final Color colorRed = fTree.getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
		final Color colorYellow = fTree.getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
		final Color colorGreen = fTree.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);

		// Iterate though all BitField items in the tree
		for (TreeItem byteitem : fRootItems) {
			for (TreeItem bitfielditem : byteitem.getItems()) {
				BitFieldDescription bfd = (BitFieldDescription) bitfielditem
						.getData(TAG_BITFIELDDESCRIPTION);
				String bitfieldname = bfd.getName();

				ConversionStatus status = values.getConversionStatus(bitfieldname);
				switch (status) {
					case SUCCESS:
						bitfielditem.setForeground(colorGreen);
						break;
					case VALUE_CHANGED:
						bitfielditem.setForeground(colorYellow);
						break;
					case NOT_IN_SOURCE:
					case NOT_IN_TARGET:
						bitfielditem.setForeground(colorRed);
						break;
					case MODIFIED:
					case NO_CONVERSION:
					case UNKNOWN:
					default:
						bitfielditem.setForeground(colorBlack);
				}
			}
		}
	}

	/**
	 * Clear the currently shown content and reset the control.
	 */
	private void clearTree() {
		fTree.removeAll();
		fRootItems.clear();
		fMCUid = null;
	}

	/**
	 * Format the given integer to a String with the format "0xXX".
	 * <p>
	 * Unlike the normal <code>Integer.toHexString(i)</code> method, this method will always
	 * produce two digits, even with the high nibble at zero, and will output the hex value in
	 * uppercase. This should make the value more readable than the standard
	 * <code>Integer.toHexString</code> output.
	 * </p>
	 * <p>
	 * If the given value is <code>-1</code>, then "n/a" is returned.
	 * </p>
	 * 
	 * @param value
	 *            Single byte value
	 * @return String with the byte value as "0xXX"
	 */
	private static String toHex(int value) {
		if (value == -1) {
			return "n/a";
		}
		String hex = "00" + Integer.toHexString(value);
		return "0x" + hex.substring(hex.length() - 2).toUpperCase();
	}

	/**
	 * This Class handles the drawing of the bits in the <code>COLUMN_BITS</code> column.
	 * <p>
	 * It is a listener that must be registered for three events: {@link SWT#MeasureItem},
	 * {@link SWT#PaintItem} and {@link SWT#EraseItem}
	 * </p>
	 */
	private class PaintListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			// ignore all columns except the Bits column
			if (event.index != COLUMN_BITS) {
				return;
			}

			switch (event.type) {
				case SWT.MeasureItem:
					measureEvent(event);
					break;
				case SWT.PaintItem:
					paintEvent(event);
					break;
				case SWT.EraseItem:
					eraseEvent(event);
					break;

			}
		}

		/**
		 * Handle a measure event.
		 * <p>
		 * Sets the width of the bits cell to its height * 8 (for the 8 bits).
		 * </p>
		 * 
		 * @param event
		 */
		private void measureEvent(Event event) {
			event.height = Math.max(event.height, 8);
			event.width = event.height * 8 + 2;
		}

		/**
		 * Handle the paint event.
		 * <p>
		 * Gets the meta data from the tree item properties and draws 8 square bit representations.
		 * </p>
		 * 
		 * @param event
		 */
		private void paintEvent(Event event) {
			final TreeItem item = (TreeItem) event.item;
			BitFieldDescription bfd = (BitFieldDescription) item.getData(TAG_BITFIELDDESCRIPTION);
			Integer value = (Integer) item.getData(TAG_VALUE);
			Boolean bold = (Boolean) item.getData(TAG_BOLD);

			// Set defaults if any tag was missing
			int mask = 0xff;
			if (bfd != null) {
				mask = bfd.getMask();
			}
			if (value == null)
				value = 0x00;
			if (bold == null)
				bold = false;

			// Get the Graphics Context and some colors
			final GC gc = event.gc;
			final Color foreground = gc.getForeground();
			final Color background = gc.getBackground();
			final Color colorGray = fTree.getDisplay().getSystemColor(SWT.COLOR_GRAY);
			final Color colorBlack = fTree.getDisplay().getSystemColor(SWT.COLOR_LIST_FOREGROUND);

			// Now we can iterate over the 8 bits. The counter i increment in drawing order´(left to
			// right), so below we use 7-i to get the actual bit value (right to left).
			for (int i = 0; i < 8; i++) {

				// Calculate the area for the single bit. This is used as reference for all
				// drawings.
				Rectangle bitarea = new Rectangle(event.x + i * event.height, event.y + 1,
						event.height - 3, event.height - 3);

				// determine if the current bit is inside of the mask
				boolean insidemask = (mask & (1 << (7 - i))) != 0;

				if (insidemask) {
					// bit inside of the mask, draw a box around the bit (bold if requested)
					gc.setForeground(colorBlack);
					gc.drawRectangle(bitarea);
					if (bold) {
						gc.drawRectangle(bitarea.x + 1, bitarea.y + 1, bitarea.width - 2,
								bitarea.height - 2);
					}
				}

				// Now for the content: three states are defined:
				// 1. invalid value or outside of mask: small dot
				// 2. inside of mask
				// 2a. 1-bit: filled box
				// 2b. 0-bit: empty box
				if ((value == -1) || !insidemask) {
					// invalid or outside of mask
					// draw a small square dot
					gc.setBackground(colorGray);
					int x = bitarea.x + event.height / 2 - event.height / 8;
					int y = bitarea.y + event.height / 2 - event.height / 8;
					gc.fillRectangle(x, y, event.height / 4, event.height / 4);
				} else {
					// bit inside of mask
					int x = bitarea.x + 2;
					int y = bitarea.y + 2;
					int width = bitarea.width - 3;
					int height = bitarea.height - 3;

					if ((value & (1 << (7 - i))) != 0) {
						// 1-bit inside of the mask
						// draw a large square dot
						gc.setBackground(colorGray);
						gc.fillRectangle(x, y, width, height);
					} else {
						// 0-bit inside of mask
						gc.setBackground(background);
						gc.fillRectangle(x, y, width, height);
					}
				}
			}

			// Restore the colors
			gc.setForeground(background);
			gc.setBackground(foreground);

		}

		/**
		 * Handle the erase event.
		 * 
		 * @param event
		 */
		private void eraseEvent(Event event) {
			// We just tell SWT that we draw the foreground ourself
			// (and let SWT fill the background)
			event.detail &= ~SWT.FOREGROUND;
		}
	}

	/**
	 * Handles <code>SWT.MouseDown</code> events for the small ToolTip shell window.
	 * <p>
	 * It passes the event to the underlying tree item so that the tree control does not loose focus
	 * when the user clicks on the ToolTip.
	 * </p>
	 * 
	 */
	private class ToolTipShellListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			Label label = (Label) event.widget;
			Shell shell = label.getShell();
			switch (event.type) {
				case SWT.MouseDown:
					Event e = new Event();
					e.item = (TreeItem) label.getData(TAG_TREEITEM);
					// Set the selection as if the mouse down event went through to the table
					fTree.setSelection(new TreeItem[] { (TreeItem) e.item });
					fTree.notifyListeners(SWT.Selection, e);
					shell.dispose();
					break;
				case SWT.MouseExit:
					shell.dispose();
					break;
			}
		}
	}

	/**
	 * This class creates fake tooltips for the bitfield descriptions.
	 * <p>
	 * Once the mouse pointer "hovers" over a bitfield description, a tooltip is shown with the
	 * bitfield name. The tooltip is placed at the right edge of the name/description column,
	 * directly adjacent to the value.
	 * </p>
	 * The bitfield name is taken from the {@link FuseBytePreviewControl#TAG_NAME} property of the
	 * tree item.
	 * </p>
	 * 
	 */
	private class ToolTipListener implements Listener {

		// This code is based on Snippet125 from:
		// http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/CreatefaketooltipsforitemsinaSWTtable.htm

		/** The shell that will take our ToolTip */
		Shell	tip		= null;

		/** The content for the ToolTip */
		Label	label	= null;

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
		 */
		@Override
		public void handleEvent(Event event) {

			switch (event.type) {
				// most events will cause disposal of an visible ToolTip window
				case SWT.Dispose:
				case SWT.KeyDown:
				case SWT.MouseMove: {
					if (tip == null)
						break;
					tip.dispose();
					tip = null;
					label = null;
					break;
				}
				case SWT.MouseHover: {
					// Get the item over which the mouse hovers
					TreeItem item = fTree.getItem(new Point(event.x, event.y));
					if (item == null) {
						break;
					}

					// No ToolTips for the root items
					if (item.getParentItem() == null) {
						break;
					}

					// Calculate the column.
					// Accumulate the column widths until the mouse.x lies within the column.
					// Both columnStart and index are then used further down
					TreeColumn[] columns = fTree.getColumns();
					int columnStart = 0;
					int index = 0;
					for (index = 0; index < columns.length; index++) {
						if (event.x < columnStart + columns[index].getWidth()) {
							break;
						}
						columnStart += columns[index].getWidth();
					}

					if (index == COLUMN_BITS) {
						// No ToolTips for the BITS column
						break;
					}

					if (tip != null && !tip.isDisposed()) {
						// dispose an already existing ToolTip window so we don't leave any
						// undisposed zombie ToolTip windows behind once we create a new one.
						tip.dispose();
					}

					tip = new Shell(fTree.getShell(), SWT.ON_TOP | SWT.TOOL);
					tip.setLayout(new FillLayout());

					label = new Label(tip, SWT.NONE);
					label.setForeground(fTree.getDisplay()
							.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
					label.setBackground(fTree.getDisplay()
							.getSystemColor(SWT.COLOR_INFO_BACKGROUND));

					BitFieldDescription bfd = (BitFieldDescription) item
							.getData(TAG_BITFIELDDESCRIPTION);
					if (index == COLUMN_NAME) {
						label.setText(!fShortName ? bfd.getName() : bfd.getDescription());
					} else {
						Integer value = (Integer) item.getData(TAG_VALUE);
						if (value != null && value != -1) {
							int bitfieldvalue = bfd.byteToBitField(value);
							label.setText(!fShortValue ? toHex(bitfieldvalue) : bfd
									.getValueText(bitfieldvalue));
						} else {
							label.setText("undefined");
						}
					}
					// Add a listener to the ToolTip so that any mouse clicks can be passed
					// through the ToolTip to the TreeItem below it. This prevents the loss of
					// focus if a user decides to click on the ToolTip.
					Listener listener = new ToolTipShellListener();
					label.addListener(SWT.MouseExit, listener);
					label.addListener(SWT.MouseDown, listener);
					label.setData(TAG_TREEITEM, item);

					// Now determine the position of the ToolTip.
					// In this case I use not the standard mouse cursor position.
					// Instead the ToolTip is shown aligned with the left edge of
					// the current cell.
					// This is non-standard but looks quite good and serves the purpose.
					Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
					Rectangle rect = item.getBounds(0);
					if (index == COLUMN_NAME) {
						Point pt = fTree.toDisplay(rect.x + rect.width, rect.y);
						tip.setBounds(pt.x - size.x, pt.y, size.x, size.y);
					} else {
						Point pt = fTree.toDisplay(columnStart, rect.y);
						tip.setBounds(pt.x, pt.y, size.x, size.y);
					}

					// After all the setup we can finally show the ToolTip :-)
					tip.setVisible(true);
				}
			}
		}
	}
}
