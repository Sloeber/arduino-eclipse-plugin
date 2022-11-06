/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
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
package io.sloeber.autoBuild.api;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IManagedBuilderMakefileGenerator2 extends IManagedBuilderMakefileGenerator {

    public void initialize(int buildKind, IConfiguration cfg, IBuilder builder, IProgressMonitor monitor);

}
