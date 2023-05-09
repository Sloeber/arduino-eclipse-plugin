/*******************************************************************************
 * Copyright (c) 2007, 2012 Intel Corporation, QNX Software Systems, and others.
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
 * Miwako Tokugawa (Intel Corporation) - Fixed-location tooltip support
 * QNX Software Systems - [269571] Apply button failure on tool changes
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.cdt.ui.newui.PageLayout;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;

import io.sloeber.autoBuild.integrations.ToolListLabelProvider;
import io.sloeber.autoBuild.ui.internal.Messages;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ITool;

/**
 * Tool Settings Tab in project properties Build Settings
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ToolSettingsTab extends AbstractAutoBuildPropertyTab {
    //	private static ToolListElement selectedElement;

    /*
     * Dialog widgets
     */
    private TreeViewer optionList;
    private StyledText tipText;
    private StyleRange styleRange;
    private SashForm sashForm;
    private SashForm sashForm2;
    private Composite settingsPageContainer;
    private ScrolledComposite containerSC;

    /*
     * Bookeeping variables
     */
    //	private Map<String, List<AbstractToolSettingUI>> configToPageListMap;
    //	private IPreferenceStore settingsStore;
    //	private AbstractToolSettingUI currentSettingsPage;
    private ToolListContentProvider listprovider;
    private Object propertyObject;

    //	private IResourceInfo fInfo;

    private boolean displayFixedTip = CDTPrefUtil.getBool(CDTPrefUtil.KEY_TIPBOX);
    private int[] defaultWeights = new int[] { 4, 1 };
    private int[] hideTipBoxWeights = new int[] { 1, 0 };

    private boolean isIndexerAffected;

    @Override
    public void createControls(Composite par) {
        super.createControls(par);
        usercomp.setLayout(new GridLayout());

        //		configToPageListMap = new HashMap<>();
        //		settingsStore = ToolSettingsPrefStore.getDefault();

        // Create the sash form
        sashForm = new SashForm(usercomp, SWT.NONE);
        sashForm.setOrientation(SWT.HORIZONTAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 5;
        sashForm.setLayout(layout);
        if (displayFixedTip == false) {
            createSelectionArea(sashForm);
            createEditArea(sashForm);
        } else {
            createSelectionArea(sashForm);
            sashForm2 = new SashForm(sashForm, SWT.NONE);
            sashForm2.setOrientation(SWT.VERTICAL);
            createEditArea(sashForm2);
            createTipArea(sashForm2);
            sashForm2.setWeights(defaultWeights);
        }
        usercomp.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                specificResize();
            }
        });

        propertyObject = page.getElement();
        setValues();
        specificResize();
    }

    private void specificResize() {
        Point p1 = optionList.getTree().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point p2 = optionList.getTree().getSize();
        Point p3 = usercomp.getSize();
        p1.x += calcExtra();
        if (p3.x >= p1.x && (p1.x < p2.x || (p2.x * 2 < p3.x))) {
            optionList.getTree().setSize(p1.x, p2.y);
            sashForm.setWeights(new int[] { p1.x, (p3.x - p1.x) });
        }
    }

    private int calcExtra() {
        int x = optionList.getTree().getBorderWidth() * 2;
        ScrollBar sb = optionList.getTree().getVerticalBar();
        if (sb != null && sb.isVisible())
            x += sb.getSize().x;
        return x;
    }

    protected void createSelectionArea(Composite parent) {
        optionList = new TreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        optionList.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                handleOptionSelection();
            }
        });
        optionList.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        optionList.setLabelProvider(new ToolListLabelProvider());
        optionList.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parent, Object element) {
                if (/*parent instanceof IResourceConfiguration &&*/ element instanceof ITool) {
                    return !((ITool) element).getCustomBuildStep();
                } else {
                    return true;
                }
            }
        });
    }

    /**
     * @param name
     *            - header of the tooltip help
     * @param tip
     *            - tooltip text
     * @since 7.0
     */
    protected void updateTipText(String name, String tip) {
        if (tipText == null) {
            return;
        }
        tipText.setText(name + "\n\n" + tip); //$NON-NLS-1$
        styleRange.length = name.length();
        tipText.setStyleRange(styleRange);
        tipText.update();
    }

    /* (non-Javadoc)
     * Method resetTipText
     * @since 7.0
     */
    private void resetTipText() {
        if (tipText == null) {
            return;
        }
        tipText.setText(Messages.ToolSettingsTab_0);
        tipText.update();
    }

    /* (non-Javadoc)
     * Method displayOptionsForCategory
     * @param category
     */
    private void displayOptionsForCategory(IOptionCategory category) {

        //        selectedElement = toolListElement;
        //        IOptionCategory category = toolListElement.getOptionCategory();
        //        IHoldsOptions optionHolder = toolListElement.getHoldOptions();
        //
        //        AbstractToolSettingUI oldPage = currentSettingsPage;
        //        currentSettingsPage = null;
        //
        //        // Create a new settings page if necessary
        //        List<AbstractToolSettingUI> pages = getPagesForConfig();
        //        for (AbstractToolSettingUI page : pages) {
        //            if (page.isFor(optionHolder, category)) {
        //                currentSettingsPage = page;
        //                break;
        //            }
        //        }
        //        if (currentSettingsPage == null) {
        //            currentSettingsPage = new BuildOptionSettingsUI(this, fInfo, optionHolder, category, displayFixedTip);
        //            boolean needToolTipBox = false;
        //            if (displayFixedTip == true) {
        //                needToolTipBox = ((BuildOptionSettingsUI) currentSettingsPage).needToolTipBox(optionHolder, category);
        //            }
        //            pages.add(currentSettingsPage);
        //            currentSettingsPage.setContainer(this);
        //            currentSettingsPage.setToolTipBoxNeeded(needToolTipBox);
        //            if (currentSettingsPage.getControl() == null) {
        //                currentSettingsPage.createControl(settingsPageContainer);
        //            }
        //        }

        //        // Make all the other pages invisible
        //        Control[] children = settingsPageContainer.getChildren();
        //        Control currentControl = currentSettingsPage.getControl();
        //        for (Control element : children) {
        //            if (element != currentControl)
        //                element.setVisible(false);
        //        }

        //        if (displayFixedTip == true) {
        //            if (currentSettingsPage.isToolTipBoxNeeded() == false) {
        //                // eliminate the option tip box
        //                sashForm2.setWeights(hideTipBoxWeights);
        //                sashForm2.layout();
        //            } else {
        //                // display the option tip box
        //                sashForm2.setWeights(defaultWeights);
        //                sashForm2.layout();
        //            }
        //        }
        //        currentSettingsPage.setVisible(true);
        //        currentSettingsPage.updateFields();
        //
        //        if (oldPage != null && oldPage != currentSettingsPage) {
        //            oldPage.setVisible(false);
        //            resetTipText();
        //        }
        //
        //        // Set the size of the scrolled area
        //        containerSC.setMinSize(currentSettingsPage.computeSize());
        //        settingsPageContainer.layout();
    }

    /* (non-Javadoc)
     * Method displayOptionsForTool
     * @param tool
     */
    private void displayOptionsForTool(ITool tool) {
        //        selectedElement = toolListElement;
        //        ITool tool = toolListElement.getTool();

        // Cache the current build setting page
        //        AbstractToolSettingUI oldPage = currentSettingsPage;
        //        currentSettingsPage = null;

        // Create a new page if we need one
        //        List<AbstractToolSettingUI> pages = getPagesForConfig();
        //        for (AbstractToolSettingUI page : pages) {
        //            if (page.isFor(tool, null)) {
        //                currentSettingsPage = page;
        //                break;
        //            }
        //        }

        //        if (currentSettingsPage == null) {
        //            currentSettingsPage = new BuildToolSettingUI(this, fInfo, tool);
        //            pages.add(currentSettingsPage);
        //            currentSettingsPage.setContainer(this);
        //            if (currentSettingsPage.getControl() == null) {
        //                currentSettingsPage.createControl(settingsPageContainer);
        //            }
        //        }
        //        // Make all the other pages invisible
        //        Control[] children = settingsPageContainer.getChildren();
        //        Control currentControl = currentSettingsPage.getControl();
        //        for (Control element : children) {
        //            if (element != currentControl)
        //                element.setVisible(false);
        //        }

        //        if (displayFixedTip == true) {
        //            // eliminate the tool tip area
        //            sashForm2.setWeights(hideTipBoxWeights);
        //            sashForm2.layout();
        //        }
        //
        //        // Make the current page visible
        //        currentSettingsPage.setVisible(true);
        //
        //        // Save the last page build options.
        //        if (oldPage != null && oldPage != currentSettingsPage) {
        //            oldPage.storeSettings();
        //        }
        //currentSettingsPage.
        setValues();

        //        if (oldPage != null && oldPage != currentSettingsPage)
        //            oldPage.setVisible(false);

        // Set the size of the scrolled area
        // containerSC.setMinSize(currentSettingsPage.computeSize());
        settingsPageContainer.layout();
    }

    /* (non-Javadoc)
     * Add the fixed-location tool tip box.
     */
    private void createTipArea(Composite parent) {
        tipText = new StyledText(parent, SWT.V_SCROLL | SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        tipText.setLayoutData(new GridData(GridData.FILL_BOTH));
        tipText.setText(Messages.ToolSettingsTab_0);

        styleRange = new StyleRange();
        styleRange.start = 0;
        FontData data = new FontData();
        data.setHeight(10);
        //data.setName("sans");
        data.setStyle(SWT.BOLD);
        Font font = new Font(parent.getDisplay(), data);
        styleRange.font = font;
    }

    /* (non-Javadoc)
     * Add the tabs relevant to the project to edit area tab folder.
     */
    protected void createEditArea(Composite parent) {
        int style = (SWT.H_SCROLL | SWT.V_SCROLL);
        if (displayFixedTip) {
            style |= SWT.BORDER;
        }
        containerSC = new ScrolledComposite(parent, style);
        containerSC.setExpandHorizontal(true);
        containerSC.setExpandVertical(true);

        // Add a container for the build settings page
        settingsPageContainer = new Composite(containerSC, SWT.NULL);
        settingsPageContainer.setLayout(new PageLayout());

        containerSC.setContent(settingsPageContainer);
        containerSC.setMinSize(settingsPageContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        settingsPageContainer.layout();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateData(page.getResDesc());
        }
        super.setVisible(visible);
    }

    protected void setValues() {
        /*
         *  This method updates the context of the build property pages
         *   - Which configuration/resource configuration is selected
         *   - Which tool/option category is selected
         *
         *  It is called:
         *   - When a property page becomes visible
         *   - When the user changes the configuration selection
         *   - When the user changes the "exclude" setting for a resource
         */

        //  Create the Tree Viewer content provider if first time
        if (listprovider == null) {
            IResource resource = (IResource) propertyObject;
            listprovider = new ToolListContentProvider(resource, myAutoConfDesc);
            optionList.setContentProvider(listprovider);
        }

        //        //  Update the selected configuration and the Tree Viewer
        //        ToolListElement[] newElements;
        //
        //        optionList.setInput(fInfo);
        //        newElements = (ToolListElement[]) listprovider.getElements(fInfo);
        //        optionList.expandAll();
        //
        //        //  Determine what the selection in the tree should be
        //        //  If the saved selection is not null, try to match the saved selection
        //        //  with an object in the new element list.
        //        //  Otherwise, select the first tool in the tree
        //        Object primaryObject = null;
        //        if (selectedElement != null && newElements != null) {
        //            selectedElement = matchSelectionElement(selectedElement, newElements);
        //        }
        //
        //        if (selectedElement == null) {
        //            selectedElement = (newElements != null && newElements.length > 0 ? newElements[0] : null);
        //        }
        //
        //        if (selectedElement != null) {
        //            primaryObject = selectedElement.getTool();
        //            if (primaryObject == null) {
        //                primaryObject = selectedElement.getOptionCategory();
        //            }
        //            if (primaryObject != null) {
        //                if (primaryObject instanceof IOptionCategory) {
        //                    ((ToolSettingsPrefStore) settingsStore).setSelection(getResDesc(), selectedElement,
        //                            (IOptionCategory) primaryObject);
        //                }
        //                optionList.setSelection(new StructuredSelection(selectedElement), true);
        //            }
        //        }
    }

    //    private ToolListElement matchSelectionElement(ToolListElement currentElement, ToolListElement[] elements) {
    //        //  First, look for an exact match
    //        ToolListElement match = exactMatchSelectionElement(currentElement, elements);
    //        if (match == null)
    //            //  Else, look for the same tool/category in the new set of elements
    //            match = equivalentMatchSelectionElement(currentElement, elements);
    //        return match;
    //    }
    //
    //    private ToolListElement exactMatchSelectionElement(ToolListElement currentElement, ToolListElement[] elements) {
    //        for (ToolListElement e : elements) {
    //            if (e == currentElement) {
    //                return currentElement;
    //            }
    //            e = exactMatchSelectionElement(currentElement, e.getChildElements());
    //            if (e != null)
    //                return e;
    //        }
    //        return null;
    //    }
    //
    //    private ToolListElement equivalentMatchSelectionElement(ToolListElement currentElement,
    //            ToolListElement[] elements) {
    //        for (ToolListElement e : elements) {
    //            if (e.isEquivalentTo(currentElement)) {
    //                return e;
    //            }
    //            e = equivalentMatchSelectionElement(currentElement, e.getChildElements());
    //            if (e != null)
    //                return e;
    //        }
    //        return null;
    //    }

    private void handleOptionSelection() {
        // Get the selection from the tree list
        //        if (optionList == null)
        //            return;
        //        IStructuredSelection selection = (IStructuredSelection) optionList.getSelection();
        //
        //        // Set the option page based on the selection
        //        ToolListElement toolListElement = (ToolListElement) selection.getFirstElement();
        //        if (toolListElement != null) {
        //            IOptionCategory cat = toolListElement.getOptionCategory();
        //            if (cat == null)
        //                cat = (IOptionCategory) toolListElement.getTool();
        //            if (cat != null)
        //                ((ToolSettingsPrefStore) settingsStore).setSelection(getResDesc(), toolListElement, cat);
        //
        //            cat = toolListElement.getOptionCategory();
        //            if (cat != null) {
        //                displayOptionsForCategory(toolListElement);
        //            } else {
        //                displayOptionsForTool(toolListElement);
        //            }
        //        }
    }

    /*
     *  (non-Javadoc)
     * @see org.eclipse.cdt.ui.dialogs.ICOptionPage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        //        if (page.isForProject()) {
        //            ManagedBuildManager.resetConfiguration(page.getProject(), getCfg());
        //        } else {
        //            ManagedBuildManager.resetOptionSettings(fInfo);
        //        }
        //        ITool tools[];
        //        if (page.isForProject())
        //            tools = getCfg().getFilteredTools();
        //        else
        //            tools = getResCfg(getResDesc()).getTools();
        //        for (int i = 0; i < tools.length; i++) {
        //            if (!tools[i].getCustomBuildStep()) {
        //                tools[i].setToolCommand(null);
        //                tools[i].setCommandLinePattern(null);
        //            }
        //        }
        //        // Reset the category or tool selection and run selection event handler
        //        selectedElement = null;
        //        setDirty(true);
        //
        //        fInfo = getResCfg(getResDesc());
        //        setValues();
        //        handleOptionSelection();
    }

    //    /*
    //     *  (non-Javadoc)
    //     * @see org.eclipse.cdt.ui.dialogs.ICOptionPage#performApply(IProgressMonitor)
    //     */
    //    private void copyHoldsOptions(IHoldsOptions src, IHoldsOptions dst, IResourceInfo res) {
    //        if (src instanceof ITool) {
    //            ITool t1 = (ITool) src;
    //            ITool t2 = (ITool) dst;
    //            if (t1.getCustomBuildStep())
    //                return;
    //            t2.setToolCommand(t1.getToolCommand());
    //            t2.setCommandLinePattern(t1.getCommandLinePattern());
    //        }
    //        IOption op1[] = src.getOptions();
    //        IOption op2[] = dst.getOptions();
    //        for (int i = 0; i < op1.length; i++) {
    //            setOption(op1[i], op2[i], dst, res);
    //        }
    //    }

    /**
     * @param filter
     *            - a viewer filter
     * @see StructuredViewer#addFilter(ViewerFilter)
     *
     * @since 5.1
     */
    protected void addFilter(ViewerFilter filter) {
        optionList.addFilter(filter);
    }

    /**
     * Copy the value of an option to another option for a given resource.
     * 
     * @param op1
     *            - option to copy the value from
     * @param op2
     *            - option to copy the value to
     * @param dst
     *            - the holder/parent of the option
     * @param res
     *            - the resource configuration the option belongs to
     *
     * @since 5.1
     */
    //    protected void setOption(IOption op1, IOption op2, IHoldsOptions dst, IResourceInfo res) {
    //        try {
    //            if (((Option) op1).isDirty())
    //                isIndexerAffected = true;
    //            switch (op1.getValueType()) {
    //            case IOption.BOOLEAN:
    //                boolean boolVal = op1.getBooleanValue();
    //                ManagedBuildManager.setOption(res, dst, op2, boolVal);
    //                break;
    //            case IOption.ENUMERATED:
    //            case IOption.TREE:
    //                String enumVal = op1.getStringValue();
    //                String enumId = op1.getId(enumVal);
    //                String out = (enumId != null && enumId.length() > 0) ? enumId : enumVal;
    //                ManagedBuildManager.setOption(res, dst, op2, out);
    //                break;
    //            case IOption.STRING:
    //                ManagedBuildManager.setOption(res, dst, op2, op1.getStringValue());
    //                break;
    //            case IOption.INCLUDE_PATH:
    //            case IOption.PREPROCESSOR_SYMBOLS:
    //            case IOption.INCLUDE_FILES:
    //            case IOption.MACRO_FILES:
    //            case IOption.UNDEF_INCLUDE_PATH:
    //            case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
    //            case IOption.UNDEF_INCLUDE_FILES:
    //            case IOption.UNDEF_LIBRARY_PATHS:
    //            case IOption.UNDEF_LIBRARY_FILES:
    //            case IOption.UNDEF_MACRO_FILES:
    //                @SuppressWarnings("unchecked")
    //                String[] data = ((List<String>) op1.getValue()).toArray(new String[0]);
    //                ManagedBuildManager.setOption(res, dst, op2, data);
    //                break;
    //            case IOption.LIBRARIES:
    //            case IOption.LIBRARY_PATHS:
    //            case IOption.LIBRARY_FILES:
    //            case IOption.STRING_LIST:
    //            case IOption.OBJECTS:
    //                @SuppressWarnings("unchecked")
    //                String[] data2 = ((List<String>) op1.getValue()).toArray(new String[0]);
    //                ManagedBuildManager.setOption(res, dst, op2, data2);
    //                break;
    //            default:
    //                break;
    //            }
    //        } catch (BuildException e) {
    //        } catch (ClassCastException e) {
    //        }
    //    }
    //
    //    protected boolean containsDefaults() {
    //        IConfiguration parentCfg = fInfo.getParent().getParent();
    //        ITool tools[] = fInfo.getParent().getTools();
    //        for (ITool tool : tools) {
    //            if (!tool.getCustomBuildStep()) {
    //                ITool cfgTool = parentCfg.getToolChain().getTool(tool.getSuperClass().getId());
    //                //  Check for a non-default command or command-line-pattern
    //                if (cfgTool != null) {
    //                    if (!(tool.getToolCommand().equals(cfgTool.getToolCommand())))
    //                        return false;
    //                    if (!(tool.getCommandLinePattern().equals(cfgTool.getCommandLinePattern())))
    //                        return false;
    //                }
    //                //  Check for a non-default option
    //                IOption options[] = tool.getOptions();
    //                for (IOption option : options) {
    //                    if (option.getParent() == tool) {
    //                        IOption ext = option;
    //                        do {
    //                            if (ext.isExtensionElement())
    //                                break;
    //                        } while ((ext = ext.getSuperClass()) != null);
    //
    //                        if (ext != null) {
    //                            if (cfgTool != null) {
    //                                IOption defaultOpt = cfgTool.getOptionBySuperClassId(ext.getId());
    //                                try {
    //                                    if (defaultOpt != null && defaultOpt.getValueType() == option.getValueType()) {
    //                                        Object value = option.getValue();
    //                                        Object defaultVal = defaultOpt.getValue();
    //
    //                                        if (value.equals(defaultVal))
    //                                            continue;
    //                                        //TODO: check list also
    //                                    }
    //                                } catch (BuildException e) {
    //                                }
    //                            }
    //                        }
    //                        return false;
    //                    }
    //                }
    //            }
    //        }
    //        return true;
    //    }
    //
    //    /* (non-Javadoc)
    //     * Answers the list of settings pages for the selected configuration
    //     */
    //    private List<AbstractToolSettingUI> getPagesForConfig() {
    //        if (getCfg() == null)
    //            return null;
    //        List<AbstractToolSettingUI> pages = configToPageListMap.get(getCfg().getId());
    //        if (pages == null) {
    //            pages = new ArrayList<>();
    //            configToPageListMap.put(getCfg().getId(), pages);
    //        }
    //        return pages;
    //    }
    //
    //    @Override
    //    public IPreferenceStore getPreferenceStore() {
    //        return settingsStore;
    //    }
    //
    //    /**
    //     * Sets the "dirty" state
    //     * 
    //     * @param b
    //     *            - the new dirty state, {@code true} or {@code false}
    //     */
    //    public void setDirty(boolean b) {
    //        List<AbstractToolSettingUI> pages = getPagesForConfig();
    //        if (pages == null)
    //            return;
    //
    //        for (AbstractToolSettingUI page : pages) {
    //            if (page == null)
    //                continue;
    //            page.setDirty(b);
    //        }
    //    }
    //
    //    /**
    //     * @return the "dirty" state
    //     */
    //    public boolean isDirty() {
    //        // Check each settings page
    //        List<AbstractToolSettingUI> pages = getPagesForConfig();
    //        // Make sure we have something to work on
    //        if (pages == null) {
    //            // Nothing to do
    //            return false;
    //        }
    //
    //        for (AbstractToolSettingUI page : pages) {
    //            if (page == null)
    //                continue;
    //            if (page.isDirty())
    //                return true;
    //        }
    //        return false;
    //    }
    //
    //    /**
    //     * @return the build macro provider to be used for macro resolution
    //     *         In case the "Build Macros" tab is available, returns the
    //     *         BuildMacroProvider
    //     *         supplied by that tab.
    //     *         Unlike the default provider, that provider also contains
    //     *         the user-modified macros that are not applied yet
    //     *         If the "Build Macros" tab is not available, returns the default
    //     *         BuildMacroProvider
    //     *
    //     * @noreference This method is not intended to be referenced by clients.
    //     */
    //    public BuildMacroProvider obtainMacroProvider() {
    //        return (BuildMacroProvider) ManagedBuildManager.getBuildMacroProvider();
    //    }

    @Override
    public void updateData(ICResourceDescription cfgd) {
        //        fInfo = getResCfg(cfgd);
        setValues();
        //        specificResize();
        //        handleOptionSelection();
    }

    @Override
    public void performApply(ICResourceDescription src, ICResourceDescription dst) {
        //        IResourceInfo ri1 = getResCfg(src);
        //        IResourceInfo ri2 = getResCfg(dst);
        //        isIndexerAffected = false;
        //        copyHoldsOptions(ri1.getParent().getToolChain(), ri2.getParent().getToolChain(), ri2);
        //        ITool[] t1, t2;
        //        if (ri1 instanceof IFolderInfo) {
        //            t1 = ((IFolderInfo) ri1).getFilteredTools();
        //            t2 = ((IFolderInfo) ri2).getFilteredTools();
        //        } else if (ri1 instanceof IFileInfo) {
        //            t1 = ((IFileInfo) ri1).getToolsToInvoke();
        //            t2 = ((IFileInfo) ri2).getToolsToInvoke();
        //        } else
        //            return;
        //
        //        // get the corresponding pairs of tools for which we can copy settings
        //        // and do the copy
        //        for (Map.Entry<ITool, ITool> pair : getToolCorrespondence(t1, t2).entrySet()) {
        //            copyHoldsOptions(pair.getKey(), pair.getValue(), ri2);
        //        }
        //        setDirty(false);

        updateData(getResDesc());
    }

    @Override
    protected void performOK() {
        //        // We need to override performOK so we can determine if any option
        //        // was chosen that affects the indexer and the user directly chooses
        //        // to press OK instead of Apply.
        //        isIndexerAffected = false;
        //        if (!isDirty()) {
        //            super.performOK();
        //            return; // don't bother if already applied
        //        }
        //        ICResourceDescription res = getResDesc();
        //        IResourceInfo info = getResCfg(res);
        //        ITool[] t1;
        //        if (info instanceof IFolderInfo) {
        //            t1 = ((IFolderInfo) info).getFilteredTools();
        //        } else if (info instanceof IFileInfo) {
        //            t1 = ((IFileInfo) info).getToolsToInvoke();
        //        } else
        //            return;
        //        for (ITool t : t1) {
        //            IOption op1[] = t.getOptions();
        //            for (IOption op : op1) {
        //                if (((Option) op).isDirty()) {
        //                    isIndexerAffected = true;
        //                }
        //            }
        //        }
        super.performOK();
    }

    @Override
    protected boolean isIndexerAffected() {
        return isIndexerAffected;
    }

    /**
     * Computes the correspondence of tools in the copy-from set (<tt>t1</tt>) and
     * the
     * copy-to set (<tt>t2</tt>) in an apply operation. The resulting pairs are in
     * the order
     * of the <tt>t2</tt> array. Note that tools that have no correspondence do not
     * appear in
     * the result, and that order is not significant. Also, in case of replication
     * of tools
     * in a chain (?) they are matched one-for one in the order in which they are
     * found in
     * each chain.
     *
     * @param t1
     *            - first group of tools. May not be <code>null</code>
     * @param t2
     *            - second group of tools. May not be <code>null</code>
     * @return the one-for-one correspondence of tools, in order of <tt>t2</tt>
     */
    //    private Map<ITool, ITool> getToolCorrespondence(ITool[] t1, ITool[] t2) {
    //        Map<ITool, ITool> result = new java.util.LinkedHashMap<>();
    //        Map<ITool, List<ITool>> realT1Tools = new java.util.LinkedHashMap<>();
    //
    //        for (ITool next : t1) {
    //            ITool real = ManagedBuildManager.getRealTool(next);
    //            List<ITool> list = realT1Tools.get(real);
    //            if (list == null) {
    //                // the immutable singleton list is efficient in storage
    //                realT1Tools.put(real, Collections.singletonList(next));
    //            } else {
    //                if (list.size() == 1) {
    //                    // make the list mutable
    //                    list = new java.util.ArrayList<>(list);
    //                    realT1Tools.put(real, list);
    //                }
    //                list.add(next);
    //            }
    //        }
    //
    //        for (ITool next : t2) {
    //            ITool real = ManagedBuildManager.getRealTool(next);
    //            List<ITool> correspondents = realT1Tools.get(real);
    //            if (correspondents != null) {
    //                result.put(correspondents.get(0), next);
    //
    //                // consume the correspondent
    //                if (correspondents.size() == 1) {
    //                    // remove the list; no more entries to consume
    //                    realT1Tools.remove(real);
    //                } else {
    //                    // cost of removal in array-list is not a concern
    //                    // considering that this is a UI Apply button and
    //                    // replication of tools is a fringe case
    //                    correspondents.remove(0);
    //                }
    //            }
    //        }
    //
    //        return result;
    //    }
    //

    @Override
    public void updateButtons() {
    }
    //
    //    @Override
    //    public void updateMessage() {
    //    }
    //
    //    @Override
    //    public void updateTitle() {
    //    }
    //
    //    @Override
    //    public boolean canBeVisible() {
    //        IConfiguration cfg = getCfg();
    //        if (cfg instanceof MultiConfiguration)
    //            return ((MultiConfiguration) cfg).isManagedBuildOn();
    //        else
    //            return cfg.getBuilder().isManagedBuildOn();
    //    }
}
