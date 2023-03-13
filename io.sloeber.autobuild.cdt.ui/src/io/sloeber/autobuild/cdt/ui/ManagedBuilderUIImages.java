/*******************************************************************************
 * Copyright (c) 2002, 2016 Rational Software Corporation and others.
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
package io.sloeber.autobuild.cdt.ui;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;

/**
 * Bundle of all images used by the C plugin.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ManagedBuilderUIImages {

	// The plugin registry
	private static ImageRegistry imageRegistry = new ImageRegistry();

	// Subdirectory (under the package containing this class) where 16 color images are
	private static URL iconBaseURL = null;
	static {
		iconBaseURL = Platform.getBundle("io.sloeber.autobuild.cdt.ui").getEntry("icons/"); //$NON-NLS-1$
	}

	private static final String NAME_PREFIX = "io.sloeber.autobuild.cdt.ui.";
	private static final int NAME_PREFIX_LENGTH = NAME_PREFIX.length();

	public static final String T_BUILD = "elcl16/"; //$NON-NLS-1$
	// list icons dir
	public static final String T_LIST = "elcl16/"; //$NON-NLS-1$
	public static final String T_OBJ = "obj16/"; //$NON-NLS-1$

	// For the managed build images
	public static final String IMG_BUILD_CONFIG = NAME_PREFIX + "build_configs.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_CONFIG = createManaged(T_BUILD, IMG_BUILD_CONFIG);
	public static final String IMG_BUILD_COMPILER = NAME_PREFIX + "config-compiler.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_COMPILER = createManaged(T_BUILD, IMG_BUILD_COMPILER);
	public static final String IMG_BUILD_LINKER = NAME_PREFIX + "config-linker.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_LINKER = createManaged(T_BUILD, IMG_BUILD_LINKER);
	public static final String IMG_BUILD_LIBRARIAN = NAME_PREFIX + "config-librarian.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_LIBRARIAN = createManaged(T_BUILD, IMG_BUILD_LIBRARIAN);
	public static final String IMG_BUILD_COMMAND = NAME_PREFIX + "config-command.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_COMMAND = createManaged(T_BUILD, IMG_BUILD_COMMAND);
	public static final String IMG_BUILD_PREPROCESSOR = NAME_PREFIX + "config-preprocessor.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_PREPROCESSOR = createManaged(T_BUILD, IMG_BUILD_PREPROCESSOR);
	public static final String IMG_BUILD_TOOL = NAME_PREFIX + "config-tool.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_TOOL = createManaged(T_BUILD, IMG_BUILD_TOOL);
	public static final String IMG_BUILD_CAT = NAME_PREFIX + "config-category.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILD_CAT = createManaged(T_BUILD, IMG_BUILD_CAT);

	// new images
	public static final String IMG_READ_ONLY = NAME_PREFIX + "read_only.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_READ_ONLY = createManaged(T_OBJ, IMG_READ_ONLY);
	public static final String IMG_EDITABLE = NAME_PREFIX + "editable.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_EDITABLE = createManaged(T_OBJ, IMG_EDITABLE);
	public static final String IMG_BUILT_IN = NAME_PREFIX + "built_in.gif"; //$NON-NLS-1$
	public static final ImageDescriptor DESC_BUILT_IN = createManaged(T_OBJ, IMG_BUILT_IN);

	// refresh policy tab
	/**
	 * @since 8.0
	 */
	public static final String IMG_FILE_OBJ = NAME_PREFIX + "file_obj.gif"; //$NON-NLS-1$
	/**
	 * @since 8.0
	 */
	public static final ImageDescriptor DESC_FILE_OBJ = createManaged(T_OBJ, IMG_FILE_OBJ);
	/**
	 * @since 8.0
	 */
	public static final String IMG_TEXTFILE_OBJ = NAME_PREFIX + "textfile_obj.gif"; //$NON-NLS-1$
	/**
	 * @since 8.0
	 */
	public static final ImageDescriptor DESC_TEXTFILE_OBJ = createManaged(T_OBJ, IMG_TEXTFILE_OBJ);
	/**
	 * @since 8.0
	 */
	public static final String IMG_FILE_FOLDER_OBJ = NAME_PREFIX + "fileFolder_obj.gif"; //$NON-NLS-1$
	/**
	 * @since 8.0
	 */
	public static final ImageDescriptor DESC_FILE_FOLDER_OBJ = createManaged(T_OBJ, IMG_FILE_FOLDER_OBJ);

	private static ImageDescriptor createManaged(String prefix, String name) {
		return createManaged(imageRegistry, prefix, name);
	}

	private static ImageDescriptor createManaged(ImageRegistry registry, String prefix, String name) {
		ImageDescriptor result = ImageDescriptor
				.createFromURL(makeIconFileURL(prefix, name.substring(NAME_PREFIX_LENGTH)));
		registry.put(name, result);
		return result;
	}

	public static Image get(String key) {
		return imageRegistry.get(key);
	}

	private static URL makeIconFileURL(String prefix, String name) {
		StringBuilder buffer = new StringBuilder(prefix);
		buffer.append(name);
		try {
			return new URL(iconBaseURL, buffer.toString());
		} catch (MalformedURLException e) {
			e.printStackTrace();
//			ManagedBuilderUIPlugin.log(e);
			return null;
		}
	}

	/**
	 * Helper method to access the image registry from the JavaPlugin class.
	 */
	static ImageRegistry getImageRegistry() {
		return imageRegistry;
	}
}
