/*******************************************************************************
 * Copyright (c) 2002, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Rational Software - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.integrations;

import java.net.URL;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import io.sloeber.autoBuild.ui.internal.Messages;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ITool;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ToolListLabelProvider extends LabelProvider {
    private final Image IMG_TOOL = ManagedBuilderUIImages.get(ManagedBuilderUIImages.IMG_BUILD_TOOL);
    private final Image IMG_CAT = ManagedBuilderUIImages.get(ManagedBuilderUIImages.IMG_BUILD_CAT);

    private ImageDescriptor descriptor = null;
    private ResourceManager manager = null;

    /* (non-Javadoc)
     * Returns the Image associated with the icon information retrieved out of OptionCategory.
     */
    private Image getIconFromOptionCategory(URL url, Image defaultImage) {

        Image img = defaultImage;

        // Get the image from the URL.
        if (url != null) {
            descriptor = ImageDescriptor.createFromURL(url);
            manager = JFaceResources.getResources(Display.getCurrent());
            Assert.isNotNull(manager);
            img = manager.createImageWithDefault(descriptor);
            if (img == null) {
                // Report error by displaying a warning message
                System.err.println("Couldn't create image from URL \"" + url + "\", to display icon for Tool Options."); //$NON-NLS-1$ //$NON-NLS-2$
                img = defaultImage;
            }
        }
        return img;
    }

    @Override
    public Image getImage(Object element) {
        if (element instanceof IOptionCategory) {
            IOptionCategory cat = (IOptionCategory) element;
            return getIconFromOptionCategory(cat.getIconPath(), IMG_CAT);
        }
        if (element instanceof ITool) {
            ITool tool = (ITool) element;
            return getIconFromOptionCategory(tool.getIconPath(), IMG_TOOL);
        }

        throw unknownElement(element);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.ILabelProvider#getText(Object)
     */
    @Override
    public String getText(Object element) {
        if (element instanceof IOptionCategory) {
            IOptionCategory cat = (IOptionCategory) element;
            return cat.getName();
        }
        if (element instanceof ITool) {
            ITool tool = (ITool) element;
            return tool.getName();//    getUniqueRealName();
        }

        throw unknownElement(element);
    }

    protected RuntimeException unknownElement(Object element) {
        return new RuntimeException(
                NLS.bind(Messages.BuildPropertyPage_error_Unknown_tree_element, element.getClass().getName()));
    }

    /**
     * Disposing any images that were allocated for it.
     *
     * @since 3.0
     */
    @Override
    public void dispose() {
        if (descriptor != null && manager != null) {
            manager.destroyImage(descriptor);
        }
    }
}
