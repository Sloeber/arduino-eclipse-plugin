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
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.macros.IFileContextData;
//import org.eclipse.cdt.managedbuilder.macros.IOptionContextData;
import org.eclipse.core.runtime.IPath;

import io.sloeber.schema.api.IOption;
import io.sloeber.schema.internal.IBuildObject;

/**
 * This is a trivial implementation of the IFileContextData used internaly by
 * the MBS
 *
 * @since 3.0
 */
public class FileContextData implements IFileContextData {
    private IPath fInputFileLocation;
    private IPath fOutputFileLocation;
    private IOptionContextData fOptionContextData;

    public FileContextData(IPath inputFileLocation, IPath outputFileLocation, IOption option,
            IBuildObject optionParent) {
        this(inputFileLocation, outputFileLocation, new OptionContextData(option, optionParent));
    }

    public FileContextData(IPath inputFileLocation, IPath outputFileLocation, IOptionContextData optionContextData) {
        fInputFileLocation = inputFileLocation;
        fOutputFileLocation = outputFileLocation;
        fOptionContextData = optionContextData;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IFileContextData#getInputFileLocation()
     */
    @Override
    public IPath getInputFileLocation() {
        return fInputFileLocation;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IFileContextData#getOutputFileLocation()
     */
    @Override
    public IPath getOutputFileLocation() {
        return fOutputFileLocation;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.macros.IFileContextData#getOption()
     */
    @Override
    public IOptionContextData getOptionContextData() {
        return fOptionContextData;
    }

}
