/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.internal.core.SafeStringInterner;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOptionCategory;
//import org.eclipse.cdt.managedbuilder.core.IOptionCategoryApplicability;
//import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
//import org.eclipse.cdt.managedbuilder.internal.enablement.OptionEnablementExpression;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;
import io.sloeber.autoBuild.api.IResourceConfiguration;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IOptionCategoryApplicability;

/**
 *
 */
public class OptionCategory extends BuildObject implements IOptionCategory {

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final IOptionCategory[] emtpyCategories = new IOptionCategory[0];

    //  Parent and children
    private IHoldsOptions holder;
    private List<OptionCategory> children; // Note: These are logical Option Category children, not "model" children
    //  Managed Build model attributes
    private IOptionCategory owner; // The logical Option Category parent
    private String ownerId;
    private URL iconPathURL;

    private IConfigurationElement applicabilityCalculatorElement = null;
    private IOptionCategoryApplicability applicabilityCalculator = null;
    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator = null;

    public static final String APPLICABILITY_CALCULATOR = "applicabilityCalculator"; //$NON-NLS-1$

    //  Miscellaneous
    private boolean isExtensionOptionCategory = false;
    private boolean isDirty = false;
    private boolean resolved = true;

    /*
     *  C O N S T R U C T O R S
     */

    public OptionCategory(IOptionCategory owner) {
        this.owner = owner;
    }

    /**
     * This constructor is called to create an option category defined by an
     * extension point in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The IHoldsOptions parent of this category, or <code>null</code> if
     *            defined at the top level
     * @param element
     *            The category definition from the manifest file or a dynamic
     *            element
     *            provider
     */
    public OptionCategory(IHoldsOptions parent, IManagedConfigElement element) {
        this.holder = parent;
        isExtensionOptionCategory = true;

        // setup for resolving
        resolved = false;

        loadFromManifest(element);

        // Hook me up to the Managed Build Manager
        ManagedBuildManager.addExtensionOptionCategory(this);

        // Add the category to the parent
        parent.addOptionCategory(this);
    }

    /**
     * Create an <codeOptionCategory</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>IHoldsOptions</code> object the OptionCategory will be
     *            added to.
     * @param element
     *            The XML element that contains the OptionCategory settings.
     */
    public OptionCategory(IHoldsOptions parent, ICStorageElement element) {
        this.holder = parent;
        isExtensionOptionCategory = false;

        // Initialize from the XML attributes
        loadFromProject(element);

        // Add the category to the parent
        parent.addOptionCategory(this);
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    public void loadFromManifest(IManagedConfigElement element) {
        ManagedBuildManager.putConfigElement(this, element);

        // id
        setId(SafeStringInterner.safeIntern(element.getAttribute(IOptionCategory.ID)));

        // name
        setName(SafeStringInterner.safeIntern(element.getAttribute(IOptionCategory.NAME)));

        // owner
        ownerId = SafeStringInterner.safeIntern(element.getAttribute(IOptionCategory.OWNER));

        // icon
        if (element.getAttribute(IOptionCategory.ICON) != null && element instanceof DefaultManagedConfigElement) {
            String icon = element.getAttribute(IOptionCategory.ICON);
            iconPathURL = ManagedBuildManager.getURLInBuildDefinitions((DefaultManagedConfigElement) element,
                    new Path(icon));
        }

        //get enablements
        IManagedConfigElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        if (enablements.length > 0)
            booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(enablements);

        // get the applicability calculator, if any
        String applicabilityCalculatorStr = element.getAttribute(APPLICABILITY_CALCULATOR);
        if (applicabilityCalculatorStr != null && element instanceof DefaultManagedConfigElement) {
            applicabilityCalculatorElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
        } else {
            applicabilityCalculator = booleanExpressionCalculator;
        }
    }

    /* (non-Javadoc)
     * Initialize the OptionCategory information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the OptionCategory information
     */
    protected void loadFromProject(ICStorageElement element) {

        // id (unique, do not intern)
        setId(element.getAttribute(IBuildObject.ID));

        // name
        if (element.getAttribute(IBuildObject.NAME) != null) {
            setName(SafeStringInterner.safeIntern(element.getAttribute(IBuildObject.NAME)));
        }

        // owner
        if (element.getAttribute(IOptionCategory.OWNER) != null) {
            ownerId = SafeStringInterner.safeIntern(element.getAttribute(IOptionCategory.OWNER));
        }
        if (ownerId != null) {
            owner = holder.getOptionCategory(ownerId);
        } else {
            owner = getNullOptionCategory();
        }

        // icon - was saved as URL in string form
        if (element.getAttribute(IOptionCategory.ICON) != null) {
            String iconPath = element.getAttribute(IOptionCategory.ICON);
            try {
                iconPathURL = new URL(iconPath);
            } catch (MalformedURLException e) {
                // Print a warning
                ManagedBuildManager.outputIconError(iconPath);
                iconPathURL = null;
            }
        }

        // Hook me in
        if (owner == null)
            ((HoldsOptions) holder).addChildCategory(this);
        else if (owner instanceof Tool)
            ((Tool) owner).addChildCategory(this);
        else
            ((OptionCategory) owner).addChildCategory(this);
    }

    private IOptionCategory getNullOptionCategory() {
        // Handle difference between Tool and others by using
        // the fact that Tool implements IOptionCategory. If so,
        // the holder is in fact a parent category to this category.
        if (holder instanceof IOptionCategory) {
            return (IOptionCategory) holder;
        }
        return null;
    }

    /**
     * Persist the OptionCategory to the project file.
     */
    public void serialize(ICStorageElement element) {
        element.setAttribute(IBuildObject.ID, id);

        if (name != null) {
            element.setAttribute(IBuildObject.NAME, name);
        }

        if (owner != null)
            element.setAttribute(IOptionCategory.OWNER, owner.getId());

        if (iconPathURL != null) {
            // Save as URL in string form
            element.setAttribute(IOptionCategory.ICON, iconPathURL.toString());
        }

        // I am clean now
        isDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getChildCategories()
     */
    @Override
    public IOptionCategory[] getChildCategories() {
        if (children != null)
            return children.toArray(new IOptionCategory[children.size()]);
        else
            return emtpyCategories;
    }

    public void addChildCategory(OptionCategory category) {
        if (children == null)
            children = new ArrayList<>();
        children.add(category);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptions()
     */
    @Override
    public Object[][] getOptions(IConfiguration configuration, IHoldsOptions optionHolder) {
        IHoldsOptions[] optionHolders = new IHoldsOptions[1];
        optionHolders[0] = optionHolder;
        return getOptions(optionHolders, FILTER_PROJECT);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptions()
     */
    //    @Override
    public Object[][] getOptions(IConfiguration configuration) {
        IHoldsOptions[] optionHolders = null;
        if (configuration != null) {
            IHoldsOptions optionHolder = getOptionHolder();
            if (optionHolder instanceof ITool) {
                optionHolders = configuration.getFilteredTools();
            } else if (optionHolder instanceof IToolChain) {
                // Get the toolchain of this configuration, which is
                // the holder equivalent for this option
                optionHolders = new IHoldsOptions[1];
                optionHolders[0] = configuration.getToolChain();
            }
            // TODO: if further option holders were to be added in future,
            // this function needs to be extended
        }
        return getOptions(optionHolders, FILTER_PROJECT);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptions()
     */
    @Override
    public Object[][] getOptions(IResourceInfo resinfo, IHoldsOptions optionHolder) {
        IHoldsOptions[] optionHolders = new IHoldsOptions[1];
        optionHolders[0] = optionHolder;
        boolean isRoot = false;
        if (resinfo instanceof ResourceInfo)
            isRoot = ((ResourceInfo) resinfo).isRoot();
        else if (resinfo instanceof MultiResourceInfo)
            isRoot = ((MultiResourceInfo) resinfo).isRoot();
        return getOptions(optionHolders, isRoot ? FILTER_PROJECT : FILTER_FILE);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptions()
     */
    //@Override
    public Object[][] getOptions(IResourceConfiguration resConfig) {
        IHoldsOptions[] optionHolders = null;
        if (resConfig != null) {
            IHoldsOptions optionHolder = getOptionHolder();
            if (optionHolder instanceof ITool) {
                optionHolders = resConfig.getTools();
            } else if (optionHolder instanceof IToolChain) {
                // Resource configurations do not support categories that are children
                // of toolchains. The reason for this is that options in such categories
                // are intended to be global. Thus return nothing.
                // TODO: Remove this restriction in future?
                optionHolders = new IHoldsOptions[1];
                optionHolders[0] = null;
            }
            // TODO: if further option holders were to be added in future,
            // this function needs to be extended
        }
        return getOptions(optionHolders, FILTER_FILE);
    }

    private IHoldsOptions getOptionHoldersSuperClass(IHoldsOptions optionHolder) {
        if (optionHolder instanceof ITool)
            return ((ITool) optionHolder).getSuperClass();
        else if (optionHolder instanceof IToolChain)
            return ((IToolChain) optionHolder).getSuperClass();
        return null;
    }

    private Object[][] getOptions(IHoldsOptions[] optionHolders, int filterValue) {
        IHoldsOptions catHolder = getOptionHolder();
        IHoldsOptions optionHolder = null;

        if (optionHolders != null) {
            // Find the child of the configuration/resource configuration that represents the same tool.
            // It could the tool itself, or a "sub-class" of the tool.
            for (int i = 0; i < optionHolders.length; ++i) {
                IHoldsOptions current = optionHolders[i];
                do {
                    if (catHolder == current) {
                        optionHolder = optionHolders[i];
                        break;
                    }
                } while ((current = getOptionHoldersSuperClass(current)) != null);
                if (optionHolder != null)
                    break;
            }
        }
        if (optionHolder == null) {
            optionHolder = catHolder;
        }

        // Get all of the tool's options and see which ones are part of
        // this category.
        IOption[] allOptions = optionHolder.getOptions();
        Object[][] myOptions = new Object[allOptions.length][2];
        int index = 0;
        for (int i = 0; i < allOptions.length; ++i) {
            IOption option = allOptions[i];
            if (option.getCategory().equals(this)) {

                // Check whether this option can be displayed for a specific resource type.
                if ((option.getResourceFilter() == FILTER_ALL) || (option.getResourceFilter() == filterValue)) {
                    myOptions[index] = new Object[2];
                    myOptions[index][0] = optionHolder;
                    myOptions[index][1] = option;
                    index++;
                }
            }
        }

        return myOptions;
    }
    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOwner()
     */
    @Override
    public IOptionCategory getOwner() {
        return owner;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOptionHolder()
     */
    @Override
    public IHoldsOptions getOptionHolder() {
        // This will stop at the parent's top category
        if (owner != null)
            return owner.getOptionHolder();
        return holder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getTool()
     */
    //@Override
    public ITool getTool() {
        // This will stop at the tool's top category
        IHoldsOptions parent = owner.getOptionHolder();
        if (parent instanceof ITool) {
            return (ITool) parent;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getIconPath()
     */
    @Override
    public URL getIconPath() {
        return iconPathURL;
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IOptionCategory#isExtensionElement()
     */
    public boolean isExtensionElement() {
        return isExtensionOptionCategory;
    }


    public void resolveReferences() {
        boolean error = false;
        if (!resolved) {
            resolved = true;
            if (ownerId != null) {
                owner = holder.getOptionCategory(ownerId);
                if (owner == null) {
                    if (holder instanceof IOptionCategory) {
                        // Report error, only if the parent is a tool and thus also
                        // an option category.
                        ManagedBuildManager.outputResolveError("owner", //$NON-NLS-1$
                                ownerId, "optionCategory", //$NON-NLS-1$
                                getId());
                        error = true;
                    } else if (false == holder.getId().equals(ownerId)) {
                        // Report error, if the holder ID does not match the owner's ID.
                        ManagedBuildManager.outputResolveError("owner", //$NON-NLS-1$
                                ownerId, "optionCategory", //$NON-NLS-1$
                                getId());
                        error = true;
                    }
                }
            }
            if (owner == null) {
                owner = getNullOptionCategory();
            }

            // Hook me in
            if (owner == null && error == false)
                ((HoldsOptions) holder).addChildCategory(this);
            else if (owner instanceof Tool)
                ((Tool) owner).addChildCategory(this);
            else
                ((OptionCategory) owner).addChildCategory(this);
        }
    }

    /**
     * @return Returns the managedBuildRevision.
     */
    @Override
    public String getManagedBuildRevision() {
        if (managedBuildRevision == null) {
            if (getOptionHolder() != null) {
                return getOptionHolder().getManagedBuildRevision();
            }
        }
        return managedBuildRevision;
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        if (version == null) {
            if (getOptionHolder() != null) {
                return getOptionHolder().getVersion();
            }
        }
        return version;
    }

    @Override
    public void setVersion(Version version) {
        // Do nothing
    }

    /**
     * Creates a name that uniquely identifies a category. The match name is
     * a concatenation of the tool and categories, e.g. Tool->Cat1->Cat2
     * maps onto the string "Tool|Cat1|Cat2|"
     *
     * @param catOrTool
     *            category or tool for which to build the match name
     * @return match name
     */
    static public String makeMatchName(IBuildObject catOrTool) {
        String catName = EMPTY_STRING;

        // Build the match name.
        do {
            catName = catOrTool.getName() + "|" + catName; //$NON-NLS-1$
            if (catOrTool instanceof ITool)
                break;
            else if (catOrTool instanceof IOptionCategory) {
                catOrTool = ((IOptionCategory) catOrTool).getOwner();
            } else
                break;
        } while (catOrTool != null);

        return catName;
    }

    /**
     * Finds an option category from an array of categories by comparing against
     * a match name. The match name is a concatenation of the tool and categories,
     * e.g. Tool->Cat1->Cat2 maps onto the string "Tool|Cat1|Cat2|"
     *
     * @param matchName
     *            an identifier to search
     * @param cats
     *            as returned by getChildCategories(), i.e. non-flattened
     * @return category or tool, if found and null otherwise
     */
    static public Object findOptionCategoryByMatchName(String matchName, IOptionCategory[] cats) {
        Object primary = null;

        for (int j = 0; j < cats.length; j++) {
            IBuildObject catOrTool = cats[j];
            // Build the match name
            String catName = makeMatchName(catOrTool);
            // Check whether the name matches
            if (catName.equals(matchName)) {
                primary = cats[j];
                break;
            } else if (matchName.startsWith(catName)) {
                // If there is a common root then check for any further children
                primary = findOptionCategoryByMatchName(matchName, cats[j].getChildCategories());
                if (primary != null)
                    break;
            }
        }
        return primary;
    }

    @Override
    public IOptionCategoryApplicability getApplicabilityCalculator() {
        if (applicabilityCalculator == null) {
            if (applicabilityCalculatorElement != null) {
                try {
                    if (applicabilityCalculatorElement.getAttribute(APPLICABILITY_CALCULATOR) != null)
                        applicabilityCalculator = (IOptionCategoryApplicability) applicabilityCalculatorElement
                                .createExecutableExtension(APPLICABILITY_CALCULATOR);
                } catch (CoreException e) {
                    Activator.log(e);
                }
            }
        }
        return applicabilityCalculator;
    }

}
