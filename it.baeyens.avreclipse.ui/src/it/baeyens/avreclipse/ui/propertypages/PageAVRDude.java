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
 * $Id: PageAVRDude.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.arduino.globals.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;

import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IMultiConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.osgi.service.prefs.BackingStoreException;


/**
 * The AVRDude property page.
 * <p>
 * This page is the container for all avrdude related tabs.
 * </p>
 * <p>
 * For make the avrdude settings more transparent for the user, this page adds a
 * avrdude command line preview below the tabs. Tabs based on this page should
 * call {@link #updatePreview(AVRProjectProperties)} whenever something is
 * modified in the current target properties. The preview will then be updated
 * accordingly.
 * </p>
 * <p>
 * As a convenience for the user, the state of the preview section (expanded /
 * collapsed) and the size ratio between tabs and preview (based on a
 * <code>SashForm</code>) is saved when the page is disposed and restored
 * when the page is created.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class PageAVRDude extends AbstractAVRPage {

	/** The preview <code>Text</code> Control */
	private Text fPreviewText;

	/** The <code>SashForm</code> for this page */
	private SashForm fSashForm;

	/** The Preview Composite */
	private ExpandableComposite fPreviewCompo;

	// The weights for the sash form, depending on the state of the
	// ExpandableComposite for the preview text control.
	// These values have been determined empirically and look good on my
	// system.
	private final int[][] fSashWeights = new int[][] { { 80, 20 }, { 95, 5 } };
	private final static int EXPANDED = 0;
	private final static int COLLAPSED = 1;

	private final static String TEXT_PREVIEW = "AVRDude command line preview";

	private static final String CLASSNAME = "avrdudepropertypage";
	private static final String QUALIFIER =ArduinoConst.CORE_PLUGIN_ID + "/" + CLASSNAME;

	private static final String KEY_ISEXPANDED = "previewexpanded";
	private static final String KEY_WEIGHT_TOP_EXP = "weight_top_expanded";
	private static final String KEY_WEIGHT_PREVIEW_EXP = "weight_preview_expanded";
	private static final String KEY_WEIGHT_TOP_COLL = "weight_top_collapsed";
	private static final String KEY_WEIGHT_PREVIEW_COLL = "weight_preview_collapsed";

	/**
	 * Set up the page for avrdude tabs.
	 * <p>
	 * The page is set up as a <code>SashForm</code> with two areas: the main
	 * part for the normal tabs and a avrdude command line preview box below it.
	 * </p>
	 */
	@Override
	public void createWidgets(Composite c) {

		// Create the sash form
		fSashForm = new SashForm(c, SWT.VERTICAL);
		fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fSashForm.setLayout(new GridLayout(1, false));

		// Let the superclass draw the tabs
		super.createWidgets(fSashForm);

		// Now add the avrdude command preview box
		// This is am ExpandableComposite...
		fPreviewCompo = new ExpandableComposite(fSashForm, SWT.NONE,
		        ExpandableComposite.CLIENT_INDENT | ExpandableComposite.TWISTIE);
		fPreviewCompo.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		fPreviewCompo.setLayout(new GridLayout(1, false));
		fPreviewCompo.setText(TEXT_PREVIEW);
		fPreviewCompo.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				// Save the current weight of the sashform and
				// load the other set
				int[] newweights;
				if (fPreviewCompo.isExpanded()) {
					fSashWeights[COLLAPSED] = fSashForm.getWeights();
					newweights = fSashWeights[EXPANDED];
				} else {
					fSashWeights[EXPANDED] = fSashForm.getWeights();
					newweights = fSashWeights[COLLAPSED];
				}
				fSashForm.setWeights(newweights);
			}
		});

		// ...with one Text control in it
		fPreviewText = new Text(fPreviewCompo, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		fPreviewText.setEditable(false);
		GridData gd2 = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd2.horizontalIndent = 10; // does not work!
		fPreviewText.setLayoutData(gd2);

		// Add the Text control to the ExpandableComposite

		// Read the previous page state from the preferences
		IScopeContext scope = InstanceScope.INSTANCE;
		IEclipsePreferences prefs = scope.getNode(QUALIFIER);
		boolean isexpanded = prefs.getBoolean(KEY_ISEXPANDED, false);
		fSashWeights[EXPANDED][0] = prefs.getInt(KEY_WEIGHT_TOP_EXP, 80);
		fSashWeights[EXPANDED][1] = prefs.getInt(KEY_WEIGHT_PREVIEW_EXP, 20);
		fSashWeights[COLLAPSED][0] = prefs.getInt(KEY_WEIGHT_TOP_COLL, 95);
		fSashWeights[COLLAPSED][1] = prefs.getInt(KEY_WEIGHT_PREVIEW_COLL, 5);

		int[] currentweights = isexpanded ? fSashWeights[EXPANDED] : fSashWeights[COLLAPSED];
		// and set the SashForm
		// weights (for the collapsed state)
		fPreviewCompo.setClient(fPreviewText);
		fPreviewCompo.setExpanded(isexpanded);
		fSashForm.setWeights(currentweights);

	}

	/**
	 * Update the avrdude command line preview.
	 * 
	 * @param props
	 *            The <code>AVRProjectProperties</code> for which to display
	 *            the preview
	 */
	public void updatePreview(AVRDudeProperties avrdudeprops) {

		// Don't do anything until this page is drawn.
		if (fPreviewText == null) {
			return;
		}

		StringBuffer sb = new StringBuffer("avrdude\t");

		// Get the current configuration...
		IConfiguration buildcfg = ManagedBuildManager.getConfigurationForDescription(getResDesc()
		        .getConfiguration());

		if (buildcfg instanceof IMultiConfiguration) {
			sb.append("command preview not available for multiconfiguration!");
		} else {
			// Get the standard AVRDude arguments as defined in the given
			// properties.
			List<String> allargs = avrdudeprops.getArguments();
	
			for (String arg : allargs) {
				sb.append(arg);
				sb.append(" ");
			}
	
			sb.append("\n");
	
			// ...and all action arguments for the current configuration
			List<String> allactionargs = avrdudeprops.getActionArguments(buildcfg, true);
	
			// append all actions, one per line for better readabilty
			for (String arg : allactionargs) {
				sb.append("\t\t\t");
				sb.append(arg);
				sb.append("\n");
			}
		}
		fPreviewText.setText(sb.toString());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#isSingle()
	 */
	@Override
	protected boolean isSingle() {

		// This page uses multiple tabs

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.ui.newui.AbstractPage#dispose()
	 */
	@Override
	public void dispose() {
		// Get the current SashForm weights
		boolean isexpanded = fPreviewCompo.isExpanded();
		fSashWeights[isexpanded ? EXPANDED : COLLAPSED] = fSashForm.getWeights();

		// Save the current GUI state
		IScopeContext scope = InstanceScope.INSTANCE ;
		IEclipsePreferences prefs = scope.getNode(QUALIFIER);
		prefs.putBoolean(KEY_ISEXPANDED, isexpanded);
		prefs.putInt(KEY_WEIGHT_TOP_EXP, fSashWeights[EXPANDED][0]);
		prefs.putInt(KEY_WEIGHT_PREVIEW_EXP, fSashWeights[EXPANDED][1]);
		prefs.putInt(KEY_WEIGHT_TOP_COLL, fSashWeights[COLLAPSED][0]);
		prefs.putInt(KEY_WEIGHT_PREVIEW_COLL, fSashWeights[COLLAPSED][1]);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			// Log an error that the prefs could not be written
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
			        "Could not write to the preferences", e);
			AVRPlugin.getDefault().log(status);
		}

		super.dispose();
	}

}
