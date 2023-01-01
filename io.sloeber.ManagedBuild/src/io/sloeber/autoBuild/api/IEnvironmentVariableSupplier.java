/*******************************************************************************
 * Copyright (c) do as you want
 * This is the interface for the autobuild variable providers
 * There is only a list all vars; selection; resolving and so on are done elsewhere
 *******************************************************************************/
package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.envvar.IEnvironmentVariable;

import java.util.Map;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @implement This interface is intended to be implemented by variable providers in autobuild variable providers.
 */
public interface IEnvironmentVariableSupplier {

    /**
	 *
	 * @param the resource this provider works on. This is a project or a configuration
	 * @return a map of variablenames, IEnvironmentVariable that represents the environment variables contributed by this provider
	 */
	Map<String,IEnvironmentVariable> getVariables(IConfiguration context);
}
