package io.sloeber.core.toolchain;

import static io.sloeber.core.common.Const.*;

import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;

@SuppressWarnings({ "nls", "unused" })
public class ArduinoLanguageProvider extends GCCBuiltinSpecsDetector {

    @Override
    protected String getCompilerCommand(String languageId) {

        if (languageId.equals("org.eclipse.cdt.core.gcc")) {
            return "${" + CODAN_C_to_O + "}";
        } else if (languageId.equals("org.eclipse.cdt.core.g++")) {
            return "${" + CODAN_CPP_to_O + "}";
        } else {
            ManagedBuilderCorePlugin.error(
                    "Unable to find compiler command for language " + languageId + " in toolchain=" + getToolchainId());
        }

        return null;
    }

}