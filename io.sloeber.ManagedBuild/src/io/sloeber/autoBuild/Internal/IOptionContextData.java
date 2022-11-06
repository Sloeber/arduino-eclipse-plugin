/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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
 * Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;

/**
 * This interface is used to represent an option context data
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOptionContextData {
    /**
     * Returns an option
     *
     * @return IOption, could be {@code null}
     */
    public IOption getOption();

    /**
     * Returns an option category
     *
     * @return IOptionCategory, could be {@code null}
     *
     * @since 8.0
     */
    public IOptionCategory getOptionCategory();

    /**
     * Returns IBuildObject that represents the option holder.
     * For the backward compatibility MBS will also support the cases
     * when this method returns either an IToolChain or IResourceConfiguration.
     * In this case MBS will try to obtain the option holder automatically,
     * but it might fail in case the tool-chain/resource configuration contains
     * more than one tools with the same super-class
     *
     * @return IBuildObject
     */
    public IBuildObject getParent();
}
