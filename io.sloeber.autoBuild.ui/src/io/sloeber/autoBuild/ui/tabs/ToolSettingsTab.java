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

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.cdt.ui.newui.MultiLineTextFieldEditor;
import org.eclipse.cdt.ui.newui.PageLayout;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import io.sloeber.autoBuild.api.ICustomBuildOptionEditor;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.integrations.ToolListLabelProvider;
import io.sloeber.autoBuild.ui.internal.Activator;
import io.sloeber.autoBuild.ui.internal.Messages;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ITool;

/**
 * Tool Settings Tab in project properties Build Settings
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ToolSettingsTab extends AbstractAutoBuildPropertyTab {
    private static final String WHITESPACE = " "; //$NON-NLS-1$
    //	private static ToolListElement selectedElement;

    /*
     * Dialog widgets
     */
    private TreeViewer optionList;
    private StyledText tipText;
    private StyleRange styleRange;
    private SashForm sashForm;
    private SashForm sashForm2;
    private Composite mySettingsPageContainer;
    private ScrolledComposite containerSC;

    private ToolListContentProvider listprovider;
    private IResource mySelectedResource;

    private boolean displayFixedTip = CDTPrefUtil.getBool(CDTPrefUtil.KEY_TIPBOX);
    private int[] defaultWeights = new int[] { 4, 1 };

    private boolean isIndexerAffected;
    private static String COMMAND_LINE_PATTERN = "command Line Pattern"; //$NON-NLS-1$
    private static String COMMAND = "command"; //$NON-NLS-1$

    //  Label class for a preference page.
    class LabelFieldEditor extends FieldEditor {
        private String fTitle;
        private Label fTitleLabel;

        public LabelFieldEditor(Composite parent, String title) {
            fTitle = title;
            this.createControl(parent);
        }

        @Override
        protected void adjustForNumColumns(int numColumns) {
            ((GridData) fTitleLabel.getLayoutData()).horizontalSpan = 2;
        }

        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns) {
            fTitleLabel = new Label(parent, SWT.WRAP);
            fTitleLabel.setText(fTitle);
            GridData gd = new GridData();
            gd.verticalAlignment = SWT.TOP;
            gd.grabExcessHorizontalSpace = false;
            gd.horizontalSpan = 2;
            fTitleLabel.setLayoutData(gd);
        }

        @Override
        public int getNumberOfControls() {
            return 1;
        }

        @Override
        protected void doLoad() {
            /**
             * The label field editor is only used to present a text label on a preference
             * page.
             */
        }

        @Override
        protected void doLoadDefault() {
            /**
             * The label field editor is only used to present a text label on a preference
             * page.
             */
        }

        @Override
        protected void doStore() {
            /**
             * The label field editor is only used to present a text label on a preference
             * page.
             */
        }
    }

    protected FieldEditor createLabelEditor(Composite parent, String title) {
        return new LabelFieldEditor(parent, title);
    }

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

        IAdaptable pageElement = page.getElement();
        if (pageElement instanceof IResource) {
            mySelectedResource = (IResource) pageElement;
        } else {
            //the line below is only here for development so I know there will only be resources
            //TODO remove the line below after evaluation this never ever happens
            System.err.println("Element should be resource " + pageElement);
        }

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
                handleOptionSelection(false);
            }
        });
        optionList.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        optionList.setLabelProvider(new ToolListLabelProvider());
        optionList.addFilter(new ViewerFilter() {
            @Override
            public boolean select(Viewer viewer, Object parent1, Object element) {
                if (element instanceof ITool) {
                    return !((ITool) element).getCustomBuildStep();
                }
                return true;
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
    private void displayOptionsForCategory(IOptionCategory category, ITool tool, boolean forceDefaultValues) {
        //erase current content
        for (Control curChild : mySettingsPageContainer.getChildren()) {
            curChild.dispose();
        }
        Set<IOption> options = tool.getOptionsOfCategory(category, mySelectedResource, myAutoConfDesc);
        for (IOption curOption : options) {
            if (forceDefaultValues) {
                String defaultValue = curOption.getDefaultValue(mySelectedResource, tool, myAutoConfDesc);
                myAutoConfDesc.setOptionValue(mySelectedResource, tool, curOption, defaultValue);
            }
            final String nameStr = curOption.getName();
            String tipStr = curOption.getToolTip();
            String contextId = curOption.getContextId();
            String optId = curOption.getId();
            String optionValue = myAutoConfDesc.getOptionValue(mySelectedResource, tool, curOption);

            if (tipStr == null || tipStr.isBlank()) {
                tipStr = Messages.BuildOptionSettingsUI_0;
            }

            try {
                // Figure out which type the option is and add a proper field
                // editor for it
                FieldEditor fieldEditor = null;

                String customFieldEditorId = curOption.getFieldEditorId();
                if (!customFieldEditorId.isBlank()) {
                    fieldEditor = createCustomFieldEditor(customFieldEditorId);
                    if (fieldEditor != null) {
                        ICustomBuildOptionEditor customFieldEditor = (ICustomBuildOptionEditor) fieldEditor;
                        if (customFieldEditor.init(curOption, curOption.getFieldEditorExtraArgument(), optId,
                                mySettingsPageContainer)) {
                            Control[] toolTipSources = customFieldEditor.getToolTipSources();
                            if (toolTipSources != null) {
                                for (Control control : toolTipSources) {
                                    //                                    if (pageHasToolTipBox) {
                                    //                                        control.setData(new TipInfo(nameStr, tipStr));
                                    //                                        control.addListener(selectAction, tipSetListener);
                                    //                                    } else {
                                    control.setToolTipText(tipStr);
                                    //                                    }
                                }
                            }
                        } else {
                            fieldEditor = null;
                        }
                    }
                }

                if (fieldEditor == null) {
                    switch (curOption.getValueType()) {
                    case IOption.STRING: {
                        StringFieldEditor stringField;
                        String filterPath = null;
                        if (curOption.getBrowseFilterPath() != null) {
                            filterPath = AutoBuildCommon.resolve(curOption.getBrowseFilterPath(), myAutoConfDesc);
                        }

                        // If browsing is set, use a field editor that has a
                        // browse button of the appropriate type.
                        switch (curOption.getBrowseType()) {
                        case IOption.BROWSE_DIR: {
                            DirectoryFieldEditor field = new DirectoryFieldEditor(optId, nameStr,
                                    mySettingsPageContainer);
                            if (filterPath != null) {
                                field.setFilterPath(new File(filterPath));
                            }
                            stringField = field;
                            break;
                        }

                        case IOption.BROWSE_FILE: {
                            FileFieldEditor field = new FileFieldEditor(optId, nameStr, mySettingsPageContainer);
                            //This issue is supposedly fixed
                            //                            {
                            //                                /**
                            //                                 * Do not perform validity check on the file name due to losing focus,
                            //                                 * see http://bugs.eclipse.org/289448
                            //                                 */
                            //                                @Override
                            //                                protected boolean checkState() {
                            //                                    clearErrorMessage();
                            //                                    return true;
                            //                                }
                            //                            };
                            if (filterPath != null) {
                                field.setFilterPath(new File(filterPath));
                            }
                            field.setFileExtensions(curOption.getBrowseFilterExtensions());
                            stringField = field;
                            break;
                        }

                        case IOption.BROWSE_NONE: {
                            final StringFieldEditorM field = new StringFieldEditorM(optId, nameStr,
                                    mySettingsPageContainer);

                            Text localText = field.getTextControl();
                            localText.addModifyListener(new ModifyListener() {

                                @Override
                                public void modifyText(ModifyEvent e) {
                                    field.valueChanged();
                                }
                            });
                            stringField = field;
                            break;
                        }

                        default: {
                            throw new Exception();
                        }
                        }

                        Label label = stringField.getLabelControl(mySettingsPageContainer);
                        Text text = stringField.getTextControl(mySettingsPageContainer);
                        text.setText(optionValue);
                        //    if(pageHasToolTipBox)
                        //    {
                        //        label.setData(new TipInfo(nameStr, tipStr));
                        //        label.addListener(selectAction, tipSetListener);
                        //        text.setData(new TipInfo(nameStr, tipStr));
                        //        text.addListener(selectAction, tipSetListener);
                        //    }else
                        //    {
                        //        label.setToolTipText(tipStr);
                        //        text.setToolTipText(tipStr);
                        //    }
                        if (!contextId.equals(AbstractPage.EMPTY_STR)) {
                            PlatformUI.getWorkbench().getHelpSystem().setHelp(text, contextId);
                        }
                        fieldEditor = stringField;
                        break;
                    }

                    case IOption.BOOLEAN: {
                        boolean originalValue = Boolean.valueOf(optionValue).booleanValue();
                        fieldEditor = new TriStateBooleanFieldEditor(optId, nameStr,  mySettingsPageContainer,
                                contextId, originalValue);
                        // tipStr is handled in TriStateBooleanFieldEditor constructor
                        break;
                    }

                    case IOption.ENUMERATED: {
                        String selId = optionValue;
                        String sel = curOption.getEnumName(selId);

                        // if (displayFixedTip==false), tooltip was already set in BuildOptionComboFieldEditor constructor.
                        String tooltipHoverStr = displayFixedTip ? null : tipStr;
                        fieldEditor = new BuildOptionComboFieldEditor(optId, nameStr, tooltipHoverStr, contextId,
                                curOption, sel, curOption.getDefaultValue(mySelectedResource, tool, myAutoConfDesc),
                                mySettingsPageContainer);

                        //    if(pageHasToolTipBox)
                        //    {
                        //        Combo combo = ((BuildOptionComboFieldEditor) fieldEditor).getComboControl();
                        //        Label label = fieldEditor.getLabelControl(mySettingsPageContainer);
                        //        combo.setData(new TipInfo(nameStr, tipStr));
                        //        combo.addListener(selectAction, tipSetListener);
                        //        label.setData(new TipInfo(nameStr, tipStr));
                        //        label.addListener(selectAction, tipSetListener);
                        //    }
                        break;
                    }

                    case IOption.TREE:
                        fieldEditor = new TreeBrowseFieldEditor(optId, nameStr, mySettingsPageContainer, nameStr,
                                curOption, contextId);
                        ((StringButtonFieldEditor) fieldEditor).setChangeButtonText("..."); //$NON-NLS-1$

                        //    if(pageHasToolTipBox){
                        //    Text text = ((StringButtonFieldEditor) fieldEditor).getTextControl(mySettingsPageContainer);
                        //    Label label = fieldEditor.getLabelControl(
                        //            mySettingsPageContainer);text.setData(new TipInfo(nameStr,tipStr));text.addListener(selectAction,tipSetListener);label.setData(new TipInfo(nameStr,tipStr));label.addListener(selectAction,tipSetListener);
                        //            }
                        break;

                    case IOption.INCLUDE_PATH:
                    case IOption.STRING_LIST:
                    case IOption.PREPROCESSOR_SYMBOLS:
                    case IOption.LIBRARIES:
                    case IOption.OBJECTS:
                    case IOption.INCLUDE_FILES:
                    case IOption.LIBRARY_PATHS:
                    case IOption.LIBRARY_FILES:
                    case IOption.MACRO_FILES:
                    case IOption.UNDEF_INCLUDE_PATH:
                    case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
                    case IOption.UNDEF_INCLUDE_FILES:
                    case IOption.UNDEF_LIBRARY_PATHS:
                    case IOption.UNDEF_LIBRARY_FILES:
                    case IOption.UNDEF_MACRO_FILES: {
                        // if (displayFixedTip==false), tooltip was already set in FileListControlFieldEditor constructor.
                        String tooltipHoverStr = displayFixedTip ? null : tipStr;
                        fieldEditor = new FileListControlFieldEditor(optId, nameStr, tooltipHoverStr, contextId,
                                mySettingsPageContainer, optionValue, curOption.getBrowseType());
                        if (curOption.getBrowseFilterPath() != null) {
                            String filterPath = AutoBuildCommon.resolve(curOption.getBrowseFilterPath(),
                                    myAutoConfDesc);
                            ((FileListControlFieldEditor) fieldEditor).setFilterPath(filterPath);
                        }
                        ((FileListControlFieldEditor) fieldEditor)
                                .setFilterExtensions(curOption.getBrowseFilterExtensions());

                        //    if(pageHasToolTipBox){
                        //    Label label = fieldEditor.getLabelControl(
                        //            mySettingsPageContainer);label.setData(new TipInfo(nameStr,tipStr));label.addListener(selectAction,tipSetListener);
                        //            }
                        break;
                    }

                    default:
                        throw new Exception();
                    }
                }

                fieldEditor.setPropertyChangeListener(new IPropertyChangeListener() {

                    @Override
                    public void propertyChange(PropertyChangeEvent event) {
                        String newValue = null;
                        if (event.getNewValue() instanceof String) {
                            newValue = (String) event.getNewValue();
                        }
                        if (event.getNewValue() instanceof Boolean) {
                            newValue = Boolean.toString(((Boolean) event.getNewValue()).booleanValue());
                        }
                        if (event.getSource() instanceof BuildOptionComboFieldEditor) {
                            BuildOptionComboFieldEditor comboClass = (BuildOptionComboFieldEditor) event.getSource();
                            newValue = comboClass.getSelectionAsID();
                        }
                        myAutoConfDesc.setOptionValue(mySelectedResource, tool, curOption, newValue);
                    }
                });

                //    addField(fieldEditor);
                //    fieldsMap.put(optId,fieldEditor);
                //    fieldEditorsToParentMap.put(fieldEditor,mySettingsPageContainer);

            } catch (@SuppressWarnings("unused") Exception e) {
                //ignore
            }
        }
        mySettingsPageContainer.layout();
        mySettingsPageContainer.redraw();
    }

    /* (non-Javadoc)
     * Method displayOptionsForTool
     * @param tool
     */
    private void displayOptionsForTool(ITool tool) {
        //erase current content
        for (Control curChild : mySettingsPageContainer.getChildren()) {
            curChild.dispose();
        }
        FontMetrics fm = AbstractCPropertyTab.getFontMetrics(mySettingsPageContainer);
        StringFieldEditor commandStringField = new StringFieldEditor(COMMAND,
                Messages.BuildToolSettingsPage_tool_command, mySettingsPageContainer);
        commandStringField.setEmptyStringAllowed(false);
        GridData gd = ((GridData) commandStringField.getTextControl(mySettingsPageContainer).getLayoutData());
        gd.grabExcessHorizontalSpace = true;
        gd.minimumWidth = Dialog.convertWidthInCharsToPixels(fm, 3);
        //addField(commandStringField);
        // Add a field editor that displays overall build options
        MultiLineTextFieldEditor allOptionFieldEditor = new MultiLineTextFieldEditor(EMPTY_STRING,
                Messages.BuildToolSettingsPage_alloptions, mySettingsPageContainer);
        allOptionFieldEditor.getTextControl(mySettingsPageContainer).setEditable(false);
        gd = ((GridData) allOptionFieldEditor.getTextControl(mySettingsPageContainer).getLayoutData());
        gd.grabExcessHorizontalSpace = true;
        gd.minimumWidth = Dialog.convertWidthInCharsToPixels(fm, 20);
        //addField(allOptionFieldEditor);

        //addField(
        createLabelEditor(mySettingsPageContainer, WHITESPACE);//);
        //addField(
        createLabelEditor(mySettingsPageContainer, Messages.BuildToolSettingsPage_tool_advancedSettings);//);

        // Add a string editor to edit the tool command line pattern
        //parent = getFieldEditorParent();
        StringFieldEditor commandLinePatternField = new StringFieldEditor(COMMAND_LINE_PATTERN,
                Messages.BuildToolSettingsPage_tool_commandLinePattern, mySettingsPageContainer);
        gd = ((GridData) commandLinePatternField.getTextControl(mySettingsPageContainer).getLayoutData());
        gd.grabExcessHorizontalSpace = true;
        gd.widthHint = Dialog.convertWidthInCharsToPixels(fm, 30);
        gd.minimumWidth = Dialog.convertWidthInCharsToPixels(fm, 20);
        //addField(commandLinePatternField);
        commandStringField.setStringValue(myAutoConfDesc.getToolCommand(tool, mySelectedResource));
        commandLinePatternField.setStringValue(myAutoConfDesc.getToolPattern(tool, mySelectedResource));
        Map<IOption, String> selectedOptions = myAutoConfDesc.getSelectedOptions(mySelectedResource, tool);
        Map<String, String> vars = tool.getToolCommandVars(myAutoConfDesc, selectedOptions);
        String flagsVar = vars.get(FLAGS_PRM_NAME);
        allOptionFieldEditor.setStringValue(flagsVar);
        commandStringField.setPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                myAutoConfDesc.setCustomToolCommand(tool, mySelectedResource, commandStringField.getStringValue());

            }
        });
        commandLinePatternField.setPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                myAutoConfDesc.setCustomToolPattern(tool, mySelectedResource, commandLinePatternField.getStringValue());

            }
        });
        mySettingsPageContainer.layout();
        mySettingsPageContainer.redraw();
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
        mySettingsPageContainer = new Composite(containerSC, SWT.NULL);
        mySettingsPageContainer.setLayout(new PageLayout());

        containerSC.setContent(mySettingsPageContainer);
        containerSC.setMinSize(mySettingsPageContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        mySettingsPageContainer.layout();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            updateData(page.getResDesc());
        }
        super.setVisible(visible);
    }

    protected void setValues() {
        if (myAutoConfDesc == null) {
            return;
        }
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
            listprovider = new ToolListContentProvider(mySelectedResource, myAutoConfDesc);
            optionList.setContentProvider(listprovider);
        }

        optionList.setInput(myAutoConfDesc);
        //                newElements = (ToolListElement[]) listprovider.getElements(fInfo);
        optionList.expandAll();

    }

    private void handleOptionSelection(boolean forceDefaultValues) {
        // Get the selection from the tree list
        if (optionList == null) {
            return;
        }
        IStructuredSelection selection = (IStructuredSelection) optionList.getSelection();
        ITreeSelection treeSel = optionList.getStructuredSelection();
        TreePath[] path = treeSel.getPaths();
        ITool tool = null;
        if (path.length == 1) {
            Object parent = path[0].getParentPath().getLastSegment();
            if (parent instanceof ITool) {
                tool = (ITool) parent;
            }
        }

        // Set the option page based on the selection
        Object toolListElement = selection.getFirstElement();
        if (toolListElement != null) {
            if (toolListElement instanceof IOptionCategory) {
                IOptionCategory cat = (IOptionCategory) toolListElement;
                //TODO JABA null needs to be the parent tool
                //if no parent tool use toolchain method (to be created)
                displayOptionsForCategory(cat, tool, forceDefaultValues);
            }
            if (toolListElement instanceof ITool) {
                tool = (ITool) toolListElement;
                displayOptionsForTool(tool);
            }
        }
    }

    /*
     *  (non-Javadoc)
     * @see org.eclipse.cdt.ui.dialogs.ICOptionPage#performDefaults()
     */
    @Override
    protected void performDefaults() {
        //        if (page.isForProject()) {
        //            ManagedBuildManager.resetConfiguration(page.getProject(), getCfg());
        handleOptionSelection(true);
        //        } else {
        //            ManagedBuildManager.resetOptionSettings(fInfo);
        //        }

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

    @Override
    public void updateData(ICResourceDescription cfgd) {
        //        fInfo = getResCfg(cfgd);
        super.updateData(cfgd);
        setValues();
        //        specificResize();
        //        handleOptionSelection();
    }

    @Override
    public void performApply(ICResourceDescription src, ICResourceDescription dst) {

        updateData(getResDesc());
    }

    @Override
    protected void performOK() {
        super.performOK();
    }

    @Override
    protected boolean isIndexerAffected() {
        return isIndexerAffected;
    }

    @Override
    public void updateButtons() {
    	//ignore
    }

    private Map<String, CustomFieldEditorDescriptor> customFieldEditorDescriptorIndex;

    /**
     * Instantiates the custom-field editor registered under the given id.
     */
    private FieldEditor createCustomFieldEditor(String customFieldEditorId) {
        if (this.customFieldEditorDescriptorIndex == null) {
            loadCustomFieldEditorDescriptors();
        }

        CustomFieldEditorDescriptor editorDescriptor = this.customFieldEditorDescriptorIndex.get(customFieldEditorId);
        if (editorDescriptor != null) {
            return editorDescriptor.createEditor();
        }

        return null;
    }

    /**
     * Holds all the information necessary to instantiate a custom field-editor.
     * Also acts as a factory - instantiates and returns a non-initialized
     * field-editor.
     */
    private class CustomFieldEditorDescriptor {
        private final IConfigurationElement element;

        public CustomFieldEditorDescriptor(IConfigurationElement providerElement) {
            this.element = providerElement;
        }

        FieldEditor createEditor() {
            try {
                Object editor = element.createExecutableExtension("class"); //$NON-NLS-1$
                if (editor instanceof FieldEditor && editor instanceof ICustomBuildOptionEditor) {
                    return (FieldEditor) editor;
                }
            } catch (Exception x) {
                Activator.log(x);
            }

            return null;
        }
    }

    /**
     * Loads all the registered custom field-editor descriptors.
     * Synchronization is not necessary as this would always be invoked on the UI
     * thread.
     */
    private void loadCustomFieldEditorDescriptors() {
        if (this.customFieldEditorDescriptorIndex != null)
            return;

        this.customFieldEditorDescriptorIndex = new HashMap<>();

        IExtensionPoint ep = Platform.getExtensionRegistry()
                .getExtensionPoint(Activator.getId() + ".buildDefinitionsUI"); //$NON-NLS-1$

        for (IExtension e : ep.getExtensions()) {
            for (IConfigurationElement providerElement : e.getConfigurationElements()) {
                String editorId = providerElement.getAttribute("id"); //$NON-NLS-1$

                this.customFieldEditorDescriptorIndex.put(editorId, new CustomFieldEditorDescriptor(providerElement));
            }
        }
    }

    class TriStateBooleanFieldEditor extends BooleanFieldEditor {
        protected Button button = null;
        private boolean myOriginalValue = true;

        public TriStateBooleanFieldEditor(String name, String labelText, Composite parent,
                String contextId, boolean curValue) {
            super(name, labelText, parent);
            myOriginalValue = curValue;
            button = getChangeControl(parent);
            button.setSelection(myOriginalValue);
            //            if (displayFixedTip && isToolTipBoxNeeded()) {
            //                button.setData(new TipInfo(labelText, tooltip));
            //                button.addListener(selectAction, tipSetListener);
            //            } else {
            //                button.setToolTipText(tooltip);
            //            }
            if (!contextId.equals(AbstractPage.EMPTY_STR)) {
                PlatformUI.getWorkbench().getHelpSystem().setHelp(button, contextId);
            }

        }

        @Override
        protected void valueChanged(boolean oldValue, boolean newValue) {
            button.setGrayed(false);
            super.valueChanged(!newValue, newValue);
        }

        @Override
        protected void doLoad() {
        	//nothing to do
        }

    }

    private final class TreeBrowseFieldEditor extends StringButtonFieldEditor {
        private final String nameStr;
        private final IOption option;
        private String contextId;

        private TreeBrowseFieldEditor(String name, String labelText, Composite parent, String nameStr, IOption option,
                String contextId) {
            super(name, labelText, parent);
            this.nameStr = nameStr;
            this.option = option;
            this.contextId = contextId;
        }

        @Override
        protected String changePressed() {
            return null;
        }
    }
}
