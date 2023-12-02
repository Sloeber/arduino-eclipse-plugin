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

import org.eclipse.cdt.core.cdtvariables.CdtVariable;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariable;

import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacro;


/**
 * This is the trivial implementation of the IBuildMacro used internaly by the MBS
 *
 * @since 3.0
 */
public class BuildMacro extends CdtVariable implements IBuildMacro {

	public BuildMacro() {
		super();
	}

	public BuildMacro(ICdtVariable var) {
		super(var);
	}

	public BuildMacro(String name, int type, String value) {
		super(name, type, value);
	}

	public BuildMacro(String name, int type, String[] value) {
		super(name, type, value);
	}

	@Override
	public int getMacroValueType() {
		return getValueType();
	}

	@Override
	public String[] getStringListValue() throws BuildMacroException {
		// TODO Auto-generated method stub
		try {
			return super.getStringListValue();
		} catch (CdtVariableException e) {
			throw new BuildMacroException(e);
		}
	}

	@Override
	public String getStringValue() throws BuildMacroException {
		try {
			return super.getStringValue();
		} catch (CdtVariableException e) {
			throw new BuildMacroException(e);
		}
	}

}
