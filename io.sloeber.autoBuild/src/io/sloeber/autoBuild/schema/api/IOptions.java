/*******************************************************************************
 * Copyright (c) 2005, 2010 Symbian Ltd and others.
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
 *******************************************************************************/
package io.sloeber.autoBuild.schema.api;

import java.util.List;

/**
 * Implements the functionality that is needed to hold options and option
 * categories. The functionality has been moved from ITool to here in CDT 3.0.
 * Backwards compatibility of interfaces has been maintained because ITool
 * extends IHoldOptions.
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOptions {

    public static final String OPTION = "option"; //$NON-NLS-1$
    public static final String OPTION_CAT = "optionCategory"; //$NON-NLS-1$
    public static final String OPTION_REF = "optionReference"; //$NON-NLS-1$
    /*
     *  M E T H O D S   M O V E D   F R O M   I T O O L   I N   3 . 0
     */

    /**
     * Get the <code>IOption</code> in the receiver with the specified
     * ID. This is an efficient search in the receiver.
     *
     * <p>
     * If the receiver does not have an option with that ID, the method
     * returns <code>null</code>. It is the responsibility of the caller to
     * verify the return value.
     *
     * @param id
     *            unique identifier of the option to search for
     * @return <code>IOption</code>
     * @since 2.0
     */
    public IOption getOptionById(String id);

    /**
     * Returns the complete list of options that are available for this object.
     * The list is a merging of the options specified for this object with the
     * options of its superclasses. The lowest option instance in the hierarchy
     * takes precedence.
     *
     * @return List<IOption>
     */
    public List<IOption> getOptions();

}
