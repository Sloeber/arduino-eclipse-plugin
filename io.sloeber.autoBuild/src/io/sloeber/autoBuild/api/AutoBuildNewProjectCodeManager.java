package io.sloeber.autoBuild.api;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import io.sloeber.autoBuild.internal.AutoBuildTemplateCodeProvider;

public class AutoBuildNewProjectCodeManager {
	private static final String CODE_TEMPLATE_PROVIDER_EXTENSION_POINT = "io.sloeber.autoBuild.templateCodeProvider"; //$NON-NLS-1$
	private static final String CODE_TEMPLATE_PROVIDER_EXTENSION = "templateCode"; //$NON-NLS-1$
	private static AutoBuildNewProjectCodeManager autoBuildTemplateCodeManager = null;
	// myCodeProviders should actually be per plugin ID in case the code provider ID
	// is
	// used more than once
	// However to avoid the hassle of that I decided to change the code provider
	// name
	// to "pluginID.codeProviderID" when codeProviderID already exists
	private final HashMap<String, ICodeProvider> myCodeProviders = new HashMap<>();

	public static AutoBuildNewProjectCodeManager getDefault() {
		if (autoBuildTemplateCodeManager == null) {
			autoBuildTemplateCodeManager = new AutoBuildNewProjectCodeManager();
			autoBuildTemplateCodeManager.initialize();
		}
		return autoBuildTemplateCodeManager;
	}

	public HashMap<String, ICodeProvider> getCodeProviders() {
		return new HashMap<>(myCodeProviders);
	}

	public HashMap<String, ICodeProvider> getCodeProviders(String buildArtifactType) {
		if(buildArtifactType==null) {
			return getCodeProviders();
		}
		HashMap<String, ICodeProvider> ret = new HashMap<>();
		for (ICodeProvider curProvider : myCodeProviders.values()) {
			if (curProvider.supports(buildArtifactType)) {
				ret.put(curProvider.getName(), curProvider);
			}
		}
		return ret;
	}

	public ICodeProvider getCodeProvider(String Id) {
		return myCodeProviders.get(Id);
	}

	private void initialize() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint(CODE_TEMPLATE_PROVIDER_EXTENSION_POINT);
		if (extensionPoint == null) {
			return;
		}
		IExtension[] extensions = extensionPoint.getExtensions();
		for (IExtension curExtension : extensions) {
			IConfigurationElement[] elements = curExtension.getConfigurationElements();
			for (IConfigurationElement element : elements) {
				if (element.getName().equals(CODE_TEMPLATE_PROVIDER_EXTENSION)) {
					IContributor contributor = element.getContributor();
					Bundle contributingBundle = Platform.getBundle(contributor.getName());
					try {
						ICodeProvider newCodeProvider = new AutoBuildTemplateCodeProvider(contributingBundle, element);
						myCodeProviders.put(newCodeProvider.getID(), newCodeProvider);
					} catch (IOException | URISyntaxException e) {
						e.printStackTrace();
					}

				}
			}
		}
	}

	public HashMap<String, ICodeProvider> getCodeProviders(String buildArtifactType, String natureID) {
		HashMap<String, ICodeProvider> ret = new HashMap<>();
		for (ICodeProvider curProvider : myCodeProviders.values()) {
			if (curProvider.supports(buildArtifactType,natureID)) {
				ret.put(curProvider.getName(), curProvider);
			}
		}
		return ret;
	}
}
