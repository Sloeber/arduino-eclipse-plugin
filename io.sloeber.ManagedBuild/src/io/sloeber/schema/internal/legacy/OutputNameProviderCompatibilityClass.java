package io.sloeber.schema.internal.legacy;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import io.sloeber.autoBuild.Internal.BuildMacroProvider;
import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.Options;

/**
 * This class is a port of
 * 
 * @author jan
 *
 */
public class OutputNameProviderCompatibilityClass implements IOutputNameProvider {

    @Override
    public String getOutputFileName(IFile inputFile, ICConfigurationDescription confDesc, IInputType inputType,
            IOutputType outputType) {
        ITool tool = inputType.getParent();

        //  Determine a default name from the input file name
        String fileName = inputFile.getProjectRelativePath().removeFileExtension().lastSegment();
        if (fileName.startsWith("$(") && fileName.endsWith(")")) { //$NON-NLS-1$ //$NON-NLS-2$
            fileName = fileName.substring(2, fileName.length() - 1);
        }

        //  If we are building a shared library, determine if the user has specified a name using the
        //  soname option
        boolean isSO = false;
        //TOFIX JABA the options here ar the selected options
        IOptions options = new Options();
        String soName = ""; //$NON-NLS-1$
        if (tool.hasAncestor("cdt.managedbuild.tool.gnu.c.linker")) { //$NON-NLS-1$
            IOption optShared = options.getOptionById("gnu.c.link.option.shared"); //$NON-NLS-1$
            if (optShared != null) {
                try {
                    isSO = optShared.getBooleanValue();
                } catch (Exception e) {
                    Activator.log(e);
                }
            }
            if (isSO) {
                IOption optSOName = options.getOptionById("gnu.c.link.option.soname"); //$NON-NLS-1$
                if (optSOName != null) {
                    try {
                        soName = optSOName.getStringValue();
                    } catch (Exception e) {
                        Activator.log(e);
                    }
                }
            }
        } else if (tool.hasAncestor("cdt.managedbuild.tool.gnu.cpp.linker")) { //$NON-NLS-1$
            IOption optShared = options.getOptionById("gnu.cpp.link.option.shared"); //$NON-NLS-1$
            if (optShared != null) {
                try {
                    isSO = optShared.getBooleanValue();
                } catch (Exception e) {
                    Activator.log(e);
                }
            }
            if (isSO) {
                IOption optSOName = options.getOptionById("gnu.cpp.link.option.soname"); //$NON-NLS-1$
                if (optSOName != null) {
                    try {
                        soName = optSOName.getStringValue();
                    } catch (Exception e) {
                        Activator.log(e);
                    }
                }
            }
        }

        //  If this is a shared library, use the specified name
        if (isSO && soName != null && soName.length() > 0) {
            fileName = soName;
        } else {
            //  Add the outputPrefix
            String outputPrefix = outputType.getOutputPrefix();
            // Resolve any macros in the outputPrefix
            // Note that we cannot use file macros because if we do a clean
            // we need to know the actual
            // name of the file to clean, and cannot use any builder
            // variables such as $@. Hence
            // we use the next best thing, i.e. configuration context.

            // figure out the configuration we're using
            if (confDesc != null) {

                boolean explicitRuleRequired = false;

                // if any input files have spaces in the name, then we must
                // not use builder variables
                if (inputFile.toString().indexOf(" ") != -1) //$NON-NLS-1$
                    explicitRuleRequired = true;

                try {

                    if (explicitRuleRequired) {
                        outputPrefix = BuildMacroProvider.getDefault().resolveValue(outputPrefix, "", //$NON-NLS-1$
                                " ", //$NON-NLS-1$
                                IBuildMacroProvider.CONTEXT_CONFIGURATION, confDesc);
                    }

                    else {
                        outputPrefix = BuildMacroProvider.getDefault().resolveValueToMakefileFormat(outputPrefix, "", //$NON-NLS-1$
                                " ", //$NON-NLS-1$
                                IBuildMacroProvider.CONTEXT_CONFIGURATION, confDesc);
                    }
                }

                catch (BuildMacroException e) {
                    Activator.log(e);
                }

            }

            if (outputPrefix != null && outputPrefix.length() > 0) {
                fileName = outputPrefix + fileName;
            }
            //  Add the primary output type extension
            String exts = outputType.getOutputExtension();
            if (!exts.isBlank()) {
                fileName += "." + exts; //$NON-NLS-1$
            }
        }

        return fileName;
    }

}
