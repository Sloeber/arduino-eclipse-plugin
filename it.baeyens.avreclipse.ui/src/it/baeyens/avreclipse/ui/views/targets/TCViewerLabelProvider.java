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
 * $Id: TCViewerLabelProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.ui.views.targets;

import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


/**
 * Label provider for the Target Configurations Viewer.
 * 
 * @author Thomas Holland
 * @since
 * 
 */
public class TCViewerLabelProvider extends LabelProvider implements ITableLabelProvider {

	private enum Column {
		IMAGE("") {
			@Override
			protected Image getColumnImage(ITargetConfiguration tc) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(
						ISharedImages.IMG_OBJ_ELEMENT);
			}

			@Override
			protected ColumnWeightData getLayoutData() {
				return new ColumnWeightData(5, 0);
			}

		},
		NAME("Name") {
			@Override
			protected String getColumnText(ITargetConfiguration tc) {
				return tc.getName();
			}

			@Override
			protected ColumnWeightData getLayoutData() {
				return new ColumnWeightData(15, 100);
			}

		},
		MCU("MCU") {
			@Override
			protected String getColumnText(ITargetConfiguration tc) {
				String mcuname = AVRMCUidConverter.id2name(tc.getMCU());
				String mcuclck = convertFCPU(tc.getFCPU());
				return mcuname + " @ " + mcuclck;
			}

			@Override
			protected ColumnWeightData getLayoutData() {
				return new ColumnWeightData(10, 150);
			}

		},
		TYPE("Type") {
			@Override
			protected String getColumnText(ITargetConfiguration tc) {
				// TODO Auto-generated method stub
				return "TODO Type";
			}

			@Override
			protected ColumnWeightData getLayoutData() {
				return new ColumnWeightData(50, 200);
			}

		};

		private String	fLabel;

		private Column(String label) {
			fLabel = label;
		}

		protected String getColumnText(ITargetConfiguration tc) {
			return null;
		}

		protected Image getColumnImage(ITargetConfiguration tc) {
			return null;
		}

		protected abstract ColumnWeightData getLayoutData();

		/*
		 * (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return fLabel;
		}
	}

	/** List of all columns. Used to map an index to the corresponding column */
	private List<Column>	fColumns;

	/**
	 * Construct a new TCViewerLabelProvider.
	 */
	public TCViewerLabelProvider() {

		// Initialize the array of columns.
		fColumns = new ArrayList<Column>();
		for (Column col : Column.values()) {
			fColumns.add(col);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
	 */
	@Override
	public String getColumnText(Object obj, int index) {
		if (obj instanceof ITargetConfiguration) {
			ITargetConfiguration tc = (ITargetConfiguration) obj;
			return fColumns.get(index).getColumnText(tc);
		}
		return "N/A";
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
	 */
	@Override
	public Image getColumnImage(Object obj, int index) {
		if (obj instanceof ITargetConfiguration) {
			ITargetConfiguration tc = (ITargetConfiguration) obj;
			return fColumns.get(index).getColumnImage(tc);
		}
		return null;
	}

	public void initColumns(TableViewer tableviewer, TableColumnLayout layout) {

		// Create the columns
		for (Column col : Column.values()) {
			TableColumn tableColumn = new TableColumn(tableviewer.getTable(), SWT.NONE);
			tableColumn.setText(col.toString());
			tableColumn.setWidth(100);
			layout.setColumnData(tableColumn, col.getLayoutData());
		}
	}

	protected static String convertFCPU(int fcpu) {

		if (fcpu >= 1000000) {
			// convert to MHz
			double mhz = ((double) fcpu) / 1000000;
			return Double.toString(mhz) + " MHz";
		} else if (fcpu >= 1000) {
			// convert to KHz
			double khz = ((double) fcpu) / 1000;
			return Double.toString(khz) + " KHz";
		} else {
			// Unlikely but just in case
			return Integer.toString(fcpu) + " Hz";
		}

	}
}
