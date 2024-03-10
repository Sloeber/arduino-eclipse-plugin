package io.sloeber.ui.wizard.newsketch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.ui.Messages;

public class SampleSelector {
	private static final String EXAMPLEPATH = "examplePath"; //$NON-NLS-1$

	protected Tree mySampleTree;
	protected Label myLabel;
	TreeMap<String, IExample> myExamples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	protected Listener mylistener;
	protected int myNumSelected = 0;
	protected Label myNumSelectedLabel;

	@SuppressWarnings("unused")
	public SampleSelector(Composite composite, int style, String label, int ncols) {

		this.myLabel = new Label(composite, SWT.NONE);
		this.myLabel.setText(label);
		GridData theGriddata = new GridData(SWT.LEFT, SWT.TOP, true, false);
		theGriddata.horizontalSpan = ncols;
		this.myLabel.setLayoutData(theGriddata);

		this.mySampleTree = new Tree(composite, SWT.CHECK | SWT.BORDER);
		theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
		theGriddata.horizontalSpan = ncols;
		this.mySampleTree.setLayoutData(theGriddata);
		// Get the data in the tree
		this.mySampleTree.setRedraw(false);

		this.mySampleTree.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {

				if (event.detail == SWT.CHECK) {
					TreeItem thechangeItem = (TreeItem) event.item;
					if (thechangeItem.getItemCount() > 0) {
						event.detail = SWT.NONE;
						event.type = SWT.None;
						event.doit = false;
						thechangeItem.setChecked(!thechangeItem.getChecked());
					} else {
						if (thechangeItem.getChecked()) {
							SampleSelector.this.myNumSelected += 1;
							SampleSelector.this.myNumSelectedLabel
									.setText(Integer.toString(SampleSelector.this.myNumSelected));
						} else {
							SampleSelector.this.myNumSelected -= 1;
							SampleSelector.this.myNumSelectedLabel
									.setText(Integer.toString(SampleSelector.this.myNumSelected));
						}
						if (SampleSelector.this.mylistener != null) {
							SampleSelector.this.mylistener.handleEvent(null);
						}
						setParentCheck(thechangeItem.getParentItem());

					}
				}
			}

			private void setParentCheck(TreeItem parentItem) {
				if (parentItem != null) {
					boolean isChecked = false;
					for (TreeItem curItem : parentItem.getItems()) {
						isChecked = isChecked || curItem.getChecked();
					}
					if (isChecked != parentItem.getChecked()) {
						parentItem.setChecked(isChecked);
						parentItem.setGrayed(isChecked);
						setParentCheck(parentItem.getParentItem());
					}
				}
			}

		});
		Label label1 = new Label(composite, SWT.NONE);
		label1.setText(Messages.sampleSelector_num_selected);
		this.myNumSelectedLabel = new Label(composite, SWT.NONE);
		this.myNumSelectedLabel.setText(Integer.toString(this.myNumSelected));
		theGriddata = new GridData(SWT.LEFT, SWT.TOP, true, false);
		theGriddata.horizontalSpan = ncols - 2;
		this.myNumSelectedLabel.setLayoutData(theGriddata);

		this.mySampleTree.setRedraw(true);

	}

	/**
	 * This method adds all examples to the selection listbox All examples
	 * already in the listbox are removed first.
	 *
	 * @param paths
	 *
	 * @param arduinoExample
	 *            The folder with the arduino samples
	 * @param privateLibrary
	 *            The folder with the private libraries
	 * @param hardwareLibrary
	 *            The folder with the hardware libraries
	 * @param mPlatformPathPath
	 */

	public void AddAllExamples(BoardDescription platformPath, Set<IExample> arrayList) {
		myNumSelected = 0;
		myExamples.putAll( LibraryManager.getAllExamples(platformPath));

		mySampleTree.removeAll();

		// Add the examples to the tree
		for (IExample curExample : this.myExamples.values()) {
			String keys[] = curExample.getBreadCrumbs();
			TreeItem curItems[] = this.mySampleTree.getItems();
			TreeItem curItem = findItem(curItems, keys[0]);
			if (curItem == null) {
				curItem = new TreeItem(this.mySampleTree, SWT.NONE);
				curItem.setText(keys[0]);
			}
			curItems = this.mySampleTree.getItems();
			TreeItem prefItem = curItem;
			for (String curKey : keys) {
				curItem = findItem(curItems, curKey);
				if (curItem == null) {

					curItem = new TreeItem(prefItem, SWT.NONE);
					curItem.setText(curKey);
				}
				prefItem = curItem;
				curItems = curItem.getItems();
			}

			curItem.setData(EXAMPLEPATH, curExample);

		}
		// Mark the examples selected
		setLastUsedExamples(arrayList);
	}

	private static TreeItem findItem(TreeItem items[], String text) {
		for (TreeItem curitem : items) {
			if (text.equals(curitem.getText())) {
				return curitem;
			}
		}
		return null;
	}

	public void setEnabled(boolean enable) {
		this.mySampleTree.setEnabled(enable);
		this.myLabel.setEnabled(enable);
	}

	/**
	 * is at least 1 sample selected in this tree
	 *
	 * @return true if at least one sample is selected. else false
	 */
	public boolean isSampleSelected() {
		return this.myNumSelected > 0;
	}

	/**
	 * you can only set 1 listener. The listener is triggered each time a item
	 * is selected or deselected
	 *
	 * @param listener
	 */
	public void addchangeListener(Listener listener) {
		this.mylistener = listener;
	}

	/**
	 * Marks the previous selected example(s) as selected and expands the items
	 * plus all parent items
	 *
	 * @param arrayList
	 */
	private void setLastUsedExamples(Set<IExample> arrayList) {

		TreeItem[] startIems = this.mySampleTree.getItems();
		for (TreeItem curItem : startIems) {
			recursiveSetExamples(curItem, arrayList);
		}
		this.myNumSelectedLabel.setText(Integer.toString(this.myNumSelected));
	}

	private void recursiveSetExamples(TreeItem curTreeItem, Set<IExample> arrayList) {
		for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
			if (curchildTreeItem.getItems().length == 0) {
				for (IExample curLastUsedExample : arrayList) {
					IExample ss = (IExample) curchildTreeItem.getData(EXAMPLEPATH);
					if (curLastUsedExample.getID().equals(ss.getID())) {
						curchildTreeItem.setChecked(true);
						curchildTreeItem.setExpanded(true);
						TreeItem parentTreeItem = curTreeItem;
						while (parentTreeItem != null) {
							parentTreeItem.setExpanded(true);
							parentTreeItem.setChecked(true);
							parentTreeItem.setGrayed(true);
							parentTreeItem = parentTreeItem.getParentItem();
						}
						this.myNumSelected += 1;
					}
				}
			} else {
				recursiveSetExamples(curchildTreeItem, arrayList);
			}
		}
	}

	public Set<IExample> GetSampleFolders() {
		this.mySampleTree.getItems();
		Set<IExample> ret = new HashSet<>();
		for (TreeItem curTreeItem : this.mySampleTree.getItems()) {
			ret.addAll(recursiveGetSelectedExamples(curTreeItem));
		}
		return ret;
	}

	private List<IExample> recursiveGetSelectedExamples(TreeItem TreeItem) {
		List<IExample> ret = new ArrayList<>();
		for (TreeItem curchildTreeItem : TreeItem.getItems()) {
			if (curchildTreeItem.getChecked() && (curchildTreeItem.getData(EXAMPLEPATH) != null)) {
				IExample location = (IExample) curchildTreeItem.getData(EXAMPLEPATH);
				ret.add(location);

			}
			ret.addAll(recursiveGetSelectedExamples(curchildTreeItem));
		}
		return ret;
	}
}
