/*******************************************************************************
 * Copyright (c) 2010, 2011 Texas Instruments, and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Texas Instruments - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import io.sloeber.autoBuild.schema.api.IOption;

/**
 * This interface can be implemented by clients to contribute custom
 * build-option
 * editors to the CDT Build Settings page in the project Properties dialog.
 *
 * In addition to implementing this interface, the custom build-option editor
 * class
 * must also extend the {@link org.eclipse.jface.preference.FieldEditor} class.
 * The
 * custom build-option editor class should be contributed through the
 * <fieldEditor>
 * element of the org.eclipse.cdt.managedbuilder.ui.buildDefinitionsUI
 * extension-point,
 * and then referenced, by its ID, from the <option>/fieldEditorId attribute of
 * the
 * org.eclipse.cdt.managedbuilder.core.buildDefinitions extension-point.
 *
 * @since 8.0
 * @see {@link ICustomBuildOptionEditor2}
 */
public interface ICustomBuildOptionEditor {
    /**
     * Initializes the custom field-editor.
     *
     * @param option
     *            the underlying build-option.
     * @param extraArgument
     *            an optional {@link IOption#getFieldEditorExtraArgument() extra
     *            argument}
     *            for the field-editor. May be {@code null}.
     * @param preferenceName
     *            the name of the preference this field editor binds to.
     * @param parent
     *            the parent of the field editor's control.
     * @return {@code true} if the custom field-editor can be successfully
     *         displayed. Returning {@code false}
     *         would cause the built-in field-editor to be displayed based on the
     *         option's {@link IOption#getValueType() valueType}.
     */
    boolean init(IOption option, String extraArgument, String preferenceName, Composite parent);

    /**
     * Returns the list of controls for which tool-tips should be displayed by the
     * Build Settings dialog-page.
     *
     * @return the list of controls for which tool-tips should automatically be
     *         displayed by the Build Settings dialog-page.
     *         May return {@code null} to signify that tool-tips are handled by the
     *         custom field-editor itself.
     */
    Control[] getToolTipSources();

}
