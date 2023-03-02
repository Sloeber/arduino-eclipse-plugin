package io.sloeber.autoBuild.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import io.sloeber.autoBuild.api.ICodeProvider;

public class TemplateTestCodeProvider implements ICodeProvider {
    private String myTemplateFolder = null;

    public TemplateTestCodeProvider(String templateFolder) {
        myTemplateFolder = templateFolder;
    }

    @SuppressWarnings("nls")
    @Override
    public boolean createFiles(IFolder srcFolder, IProgressMonitor monitor) {
        try {
            IPath folderName = getTemplateFolder();
            String FileNames[] = folderName.toFile().list();

            for (String file : FileNames) {

                if (!(file.equals(".") || file.equals(".."))) {
                    File sourceFile = folderName.append(file).toFile();
                    IFile targetFile = srcFolder.getFile(file);

                    try (InputStream theFileStream = new FileInputStream(sourceFile.toString())) {
                        targetFile.create(theFileStream, true, monitor);
                    } catch (IOException e) {
                        int a = 0;
                    }

                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private IPath getTemplateFolder() throws Exception {
        Bundle bundle = Platform.getBundle("io.sloeber.autoBuild.test");
        Path path = new Path("templates/" + myTemplateFolder);
        URL fileURL = FileLocator.find(bundle, path, null);
        URL resolvedFileURL = FileLocator.toFileURL(fileURL);
        return new Path(resolvedFileURL.toURI().getPath());
    }
}
