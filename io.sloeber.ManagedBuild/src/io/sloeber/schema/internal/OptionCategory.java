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
package io.sloeber.schema.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.Internal.BooleanExpressionApplicabilityCalculator;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.OptionEnablementExpression;
import io.sloeber.autoBuild.extensionPoint.IOptionCategoryApplicability;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ITool;

/**
 *
 */
public class OptionCategory extends BuildObject implements IOptionCategory {
	private IOptionCategory owner; // The logical Option Category parent
	private URL iconPathURL;

	private IOptionCategoryApplicability applicabilityCalculator = null;
	private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator = null;
	List<OptionEnablementExpression> myOptionEnablementExpressions = new ArrayList<>();
	private String[] modelOwner;
	private String[] modelIcon;

	/*
	 * C O N S T R U C T O R S
	 */

	public OptionCategory(IOptionCategory owner) {
		this.owner = owner;
	}

	/**
	 * This constructor is called to create an option category defined by an
	 * extension point in a plugin manifest file, or returned by a dynamic element
	 * provider
	 *
	 * @param parent  The IHoldsOptions parent of this category, or
	 *                <code>null</code> if defined at the top level
	 * @param element The category definition from the manifest file or a dynamic
	 *                element provider
	 */
	public OptionCategory(IHoldsOptions parent, IExtensionPoint root, IConfigurationElement element) {

		loadNameAndID(root, element);

		modelOwner = getAttributes(OWNER);
		modelIcon = getAttributes(ICON);

		myOptionEnablementExpressions.clear();
		IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
		for (IConfigurationElement curEnablement : enablements) {
			myOptionEnablementExpressions.add(new OptionEnablementExpression(curEnablement));
		}

		booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(myOptionEnablementExpressions);

		applicabilityCalculator = (IOptionCategoryApplicability) createExecutableExtension(APPLICABILITY_CALCULATOR);

		if (!modelIcon[SUPER].isBlank()) {
			try {
				iconPathURL = new URL(modelIcon[SUPER]);
			} catch (MalformedURLException e) {
				ManagedBuildManager.outputIconError(modelIcon[SUPER]);
				iconPathURL = null;
			}
		}
		resolveFields();

	}

	private void resolveFields() {
		//TOFIX JABA need to find out what this holder is all about
//		if (!modelOwner[SUPER].isBlank()) {
//			owner = holder.getOptionCategory(modelOwner[SUPER]);
//			if (owner == null) {
//				if (holder instanceof IOptionCategory) {
//					// Report error, only if the parent is a tool and thus also
//					// an option category.
//					ManagedBuildManager.outputResolveError("owner", //$NON-NLS-1$
//							modelOwner[SUPER], "optionCategory", //$NON-NLS-1$
//							getId());
//				} else if (false == holder.getId().equals(modelOwner[SUPER])) {
//					// Report error, if the holder ID does not match the owner's ID.
//					ManagedBuildManager.outputResolveError("owner", //$NON-NLS-1$
//							modelOwner[SUPER], "optionCategory", //$NON-NLS-1$
//							getId());
//				}
//			}
//		}
//		if (owner == null) {
//			owner = getNullOptionCategory();
//		}

	}

	/**
	 * Create an <codeOptionCategory</code> based on the specification stored in the
	 * project file (.cdtbuild).
	 *
	 * @param parent  The <code>IHoldsOptions</code> object the OptionCategory will
	 *                be added to.
	 * @param element The XML element that contains the OptionCategory settings.
	 */
	public OptionCategory(IHoldsOptions parent, ICStorageElement element) {
//        this.holder = parent;
//        isExtensionOptionCategory = false;
//
//        // Initialize from the XML attributes
//        loadFromProject(element);
//
//        // Add the category to the parent
//        parent.addOptionCategory(this);
	}

	/*
	 * (non-Javadoc) Initialize the OptionCategory information from the XML element
	 * specified in the argument
	 *
	 * @param element An XML element containing the OptionCategory information
	 */
	protected void loadFromProject(ICStorageElement element) {

//        // id (unique, do not intern)
//        id = (element.getAttribute(IBuildObject.ID));
//
//        // name
//        if (element.getAttribute(IBuildObject.NAME) != null) {
//            name = (element.getAttribute(IBuildObject.NAME));
//        }
//
//        // owner
//        if (element.getAttribute(IOptionCategory.OWNER) != null) {
//            ownerId = element.getAttribute(IOptionCategory.OWNER);
//        }
//        if (ownerId != null) {
//            owner = holder.getOptionCategory(ownerId);
//        } else {
//            owner = getNullOptionCategory();
//        }
//
//        // icon - was saved as URL in string form
//        if (element.getAttribute(IOptionCategory.ICON) != null) {
//            String iconPath = element.getAttribute(IOptionCategory.ICON);
//            try {
//                iconPathURL = new URL(iconPath);
//            } catch (MalformedURLException e) {
//                // Print a warning
//                ManagedBuildManager.outputIconError(iconPath);
//                iconPathURL = null;
//            }
//        }
//
//        // Hook me in
//        if (owner == null)
//            ((HoldsOptions) holder).addChildCategory(this);
//        else if (owner instanceof Tool)
//            ((Tool) owner).addChildCategory(this);
//        else
//            ((OptionCategory) owner).addChildCategory(this);
	}

//    /**
//     * Persist the OptionCategory to the project file.
//     */
//    public void serialize(ICStorageElement element) {
//        element.setAttribute(IBuildObject.ID, id);
//
//        if (name != null) {
//            element.setAttribute(IBuildObject.NAME, name);
//        }
//
//        if (owner != null)
//            element.setAttribute(IOptionCategory.OWNER, owner.getId());
//
//        if (iconPathURL != null) {
//            // Save as URL in string form
//            element.setAttribute(IOptionCategory.ICON, iconPathURL.toString());
//        }
//
//    }

	/*
	 * P A R E N T A N D C H I L D H A N D L I N G
	 */

	/*
	 * M O D E L A T T R I B U T E A C C E S S O R S
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getOwner()
	 */
	@Override
	public IOptionCategory getOwner() {
		return owner;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.build.managed.IOptionCategory#getIconPath()
	 */
	@Override
	public URL getIconPath() {
		return iconPathURL;
	}

	/**
	 * Creates a name that uniquely identifies a category. The match name is a
	 * concatenation of the tool and categories, e.g. Tool->Cat1->Cat2 maps onto the
	 * string "Tool|Cat1|Cat2|"
	 *
	 * @param catOrTool category or tool for which to build the match name
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

	@Override
	public IOptionCategoryApplicability getApplicabilityCalculator() {

		return applicabilityCalculator;
	}

}
