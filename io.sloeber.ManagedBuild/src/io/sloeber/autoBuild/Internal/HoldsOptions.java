/*******************************************************************************
 * Copyright (c) 2005, 2016 Symbian Ltd and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Symbian Ltd - Initial API and implementation
 * Baltasar Belyavsky (Texas Instruments) - [405643] HoldsOptions performance improvements
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.core.Activator;

/**
 * Implements the functionality that is needed to hold options and option
 * categories. In CDT 3.0, the functionality has been moved from ITool and
 * Tool to this class.
 *
 * This class is intended to be used as base class for all MBS grammar
 * elements that can hold Options and Option Categories. These are currently
 * Tool and ToolChain.
 *
 * Note that the member <code>superClass</code> must be shared with the
 * derived class. This requires to wrap this member by access functions
 * in the derived class or frequent casts, because the type of
 * <code>superClass</code>
 * in <code>HoldsOptions</code> must be <code>IHoldOptions</code>. Further
 * note that the member <code>resolved</code> must inherit the value of its
 * derived class. This achieved through the constructor.
 *
 * @since 3.0
 */
public abstract class HoldsOptions extends BuildObject implements IHoldsOptions {

    private static final List<IOptionCategory> EMPTY_CATEGORIES = new LinkedList<>();

    //  Members that are to be shared with the derived class
    protected IHoldsOptions superClass;
    //  Parent and children
    protected Set<String> categoryIds= new HashSet<>();
    protected Map<String, IOptionCategory> categoryMap =new HashMap<>();
    private List<IOptionCategory> childOptionCategories;
    protected Map<String, Option> myOptionMap=new HashMap<>();
    //  Miscellaneous

    /*
     *  C O N S T R U C T O R S
     */

    HoldsOptions() {
        // prevent accidental construction of class without setting up
        // resolved
    }


    /**
     * Copies children of <code>HoldsOptions</code>. Helper function for
     * derived constructors.
     *
     * @param source
     *            The children of the source will be cloned and added
     *            to the class itself.
     */
    //    protected void copyChildren(HoldsOptions source) {
    //
    //        //  Note: This function ignores OptionCategories since they should not be
    //        //        found on an non-extension tools
    //
    //        boolean copyIds = id.equals(source.id);
    //        if (source.optionMap != null) {
    //            for (Option option : source.getOptionCollection()) {
    //                int nnn = ManagedBuildManager.getRandomNumber();
    //                String subId;
    //                String subName;
    //                if (option.getSuperClass() != null) {
    //                    subId = copyIds ? option.getId() : option.getSuperClass().getId() + "." + nnn; //$NON-NLS-1$
    //                    subName = option.getSuperClass().getName();
    //                } else {
    //                    subId = copyIds ? option.getId() : option.getId() + "." + nnn; //$NON-NLS-1$
    //                    subName = option.getName();
    //                }
    //                Option newOption = new Option(this, subId, subName, option);
    //                addOption(newOption);
    //            }
    //        }
    //
    //        if (copyIds) {
    //            //isDirty = source.isDirty;
    //            //rebuildState = source.rebuildState;
    //        }
    //    }

    //    void copyNonoverriddenSettings(HoldsOptions ho) {
    //        if (ho.optionMap == null || ho.optionMap.size() == 0)
    //            return;
    //
    //        IOption options[] = getOptions();
    //        for (int i = 0; i < options.length; i++) {
    //            if (!options[i].getParent().equals(ho))
    //                continue;
    //
    //            Option option = (Option) options[i];
    //            int nnn = ManagedBuildManager.getRandomNumber();
    //            String subId;
    //            String subName;
    //            if (option.getSuperClass() != null) {
    //                subId = option.getSuperClass().getId() + "." + nnn; //$NON-NLS-1$
    //                subName = option.getSuperClass().getName();
    //            } else {
    //                subId = option.getId() + "." + nnn; //$NON-NLS-1$
    //                subName = option.getName();
    //            }
    //            Option newOption = new Option(this, subId, subName, option);
    //            addOption(newOption);
    //
    //        }
    //    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */



    /*
     *  M E T H O D S   M O V E D   F R O M   I T O O L   I N   3 . 0
     */



    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptions()
     */
    @Override
    public List<IOption> getOptions() {
        List<IOption> opts =new LinkedList<>();
        opts.addAll( doGetOptions().values());
        return opts;
    }

    /**
     * This method returns an intermediate object, ultimately used by
     * {@link #getOptions()}.
     *
     * NOTE: The keys in the returned map are only used to efficiently override the
     * values as this method
     * is invoked recursively. Once the recursion unwinds, the keys in the resulting
     * map are a mixture of
     * actual option IDs and option superClass IDs. So the keys of the resulting map
     * should not be relied
     * upon - only the values hold significance at this point.
     */
    private Map<String, IOption> doGetOptions() {
        Map<String, IOption> map = null;

        map = new LinkedHashMap<>(); // LinkedHashMap ensures we maintain option ordering

        for (Option ourOpt : getOptionCollection()) {
            if (ourOpt.isValid()) {
                map.put(ourOpt.getId(), ourOpt);
            }
        }

        return map;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOption(java.lang.String)
     */
    public IOption getOption(String id) {
        return getOptionById(id);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptionById(java.lang.String)
     */
    @Override
    public IOption getOptionById(String id) {
        IOption opt = getOptionMap().get(id);
        if (opt == null) {
            if (superClass != null) {
                return superClass.getOptionById(id);
            }
        }
        if (opt == null)
            return null;
        return opt.isValid() ? opt : null;
    }


    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getChildCategories()
     */
    @Override
    public List<IOptionCategory> getChildCategories() {
        List<IOptionCategory> retCats =   new LinkedList<>();;
        // Merge our option categories with our superclass' option categories.
        // Note that these are two disjoint sets of categories because
        // categories do not use derivation AND object Id's are unique. Thus
        // they are merely sequentially added.
        if (superClass != null) {
        	retCats.addAll( superClass.getChildCategories());
        }
        if (childOptionCategories != null) {
            retCats.addAll( childOptionCategories);
        }

        // Nothing found, return EMPTY_CATEGORIES
        return retCats;
    }

    /*
     *  M E T H O D S   M O V E D   F R O M   T O O L   I N   3 . 0
     */



    /**
     * Memory-safe way to access the map of category IDs to categories
     */
    private Map<String, IOptionCategory> getCategoryMap() {
        return categoryMap;
    }

    /**
     * Memory-safe way to access the list of options
     */
    private Collection<Option> getOptionCollection() {
            return myOptionMap.values();
    }

    /**
     * Memory-safe way to access the list of IDs to options
     */
    private Map<String, Option> getOptionMap() {
        return myOptionMap;
    }

    /* (non-Javadoc)
     * org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptionCategory()
     */
    @Override
    public IOptionCategory getOptionCategory(String id) {
        return categoryMap.get(id);
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptionToSet(org.eclipse.cdt.managedbuilder.core.IOption, boolean)
     */
    @Override
    public IOption getOptionToSet(IOption option, boolean adjustExtension) throws BuildException {
        //  IOption setOption = null;
        // start changes
//        if (option.getOptionHolder() != this) {
            // option = getOptionBySuperClassId(option.getId());
            //            IOption op = getOptionBySuperClassId(option.getId());
            //            if (op == null && option.getSuperClass() != null) {
            //                op = getOptionBySuperClassId(option.getSuperClass().getId());
            //                if (op == null) {
            //                    Activator.log(new Status(IStatus.ERROR, Activator.getId(), IStatus.OK,
            //                            "Cannot get OptionToSet for option " + //$NON-NLS-1$
            //                                    option.getId() + " @ holder " + //$NON-NLS-1$
            //                                    option.getOptionHolder().getId() + "\nI'm holder " + //$NON-NLS-1$
            //                                    getId(),
            //                            null));
            //                } else
            //                    option = op;
            //            } else
            //                option = op;
//            IOption op = getOptionBySuperClassId(option.getId());
//            if (op == null) {
//                Activator.log(
//                        new Status(IStatus.ERROR, Activator.getId(), IStatus.OK, "Cannot get OptionToSet for option " + //$NON-NLS-1$
//                                option.getId() + " @ holder " + //$NON-NLS-1$
//                                option.getOptionHolder().getId() + "\nI'm holder " + //$NON-NLS-1$
//                                getId(), null));
//            }
//            return op;
            //        }
            //        // end changes
            //
            //        if (adjustExtension) {
            //            for (; option != null && !option.isExtensionElement(); option = option.getSuperClass()) {
            //            }
            //
            //            if (option != null) {
            //                IHoldsOptions holder = option.getOptionHolder();
            //                if (holder == this)
            //                    setOption = option;
            //                else {
            //                    IOption newSuperClass = option;
            //                    if (((Option) option).wasOptRef()) {
            //                        newSuperClass = option.getSuperClass();
            //                    }
            //                    //  Create a new extension Option element
            //                    String subId;
            //                    String version = ManagedBuildManager.getVersionFromIdAndVersion(newSuperClass.getId());
            //                    String baseId = ManagedBuildManager.getIdFromIdAndVersion(newSuperClass.getId());
            //                    if (version != null) {
            //                        subId = baseId + ".adjusted." + Integer.toString(ManagedBuildManager.getRandomNumber()) + "_" //$NON-NLS-1$//$NON-NLS-2$
            //                                + version;
            //                    } else {
            //                        subId = baseId + ".adjusted." + Integer.toString(ManagedBuildManager.getRandomNumber()); //$NON-NLS-1$
            //                    }
            //                    setOption = createOption(newSuperClass, subId, null, true);
            //                    ((Option) setOption).setAdjusted(true);
            //                    setOption.setValueType(option.getValueType());
            //                }
//        }
        //        } else {
//        if (!option.isExtensionElement()) {
//            return option;
            //            } else {
            //                IOption newSuperClass = option;
            //                for (; newSuperClass != null
            //                        && !newSuperClass.isExtensionElement(); newSuperClass = newSuperClass.getSuperClass()) {
            //                }
            //
            //                if (((Option) newSuperClass).wasOptRef()) {
            //                    newSuperClass = newSuperClass.getSuperClass();
            //                }
            //
            //                if (((Option) newSuperClass).isAdjustedExtension()) {
            //                    newSuperClass = newSuperClass.getSuperClass();
            //                }
            //                //  Create an Option element for the managed build project file (.CDTBUILD)
            //                String subId;
            //                subId = ManagedBuildManager.calculateChildId(newSuperClass.getId(), null);
            //                setOption = createOption(newSuperClass, subId, null, false);
            //                setOption.setValueType(option.getValueType());
            //            }
//        }
        return null;
    }

    //    public void adjustOptions(boolean extensions) {
    //        IOption options[] = getOptions();
    //
    //        for (IOption opt : options) {
    //            if (opt.isExtensionElement()) {
    //                Option option = (Option) opt;
    //                BooleanExpressionApplicabilityCalculator calc = option.getBooleanExpressionCalculator(extensions);
    //
    //                if (calc != null)
    //                    calc.adjustOption(getParentResourceInfo(), this, option, extensions);
    //            }
    //        }
    //    }

//    public abstract boolean isExtensionElement();

    protected abstract IResourceInfo getParentResourceInfo();

    boolean hasCustomSettings() {
        return ( myOptionMap.size() != 0) ;
    }
}
