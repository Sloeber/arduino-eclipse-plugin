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

    @Override
    public boolean createFiles(IFolder targetFolder, IProgressMonitor monitor) {
        try {
            IPath folderName = getTemplateFolder();
            File templateFolder = folderName.toFile();
            return recursiveCreateFiles(templateFolder, targetFolder, monitor);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean recursiveCreateFiles(File templateFolder, IFolder targetFolder, IProgressMonitor monitor) {
        try {
            for (File curMember : templateFolder.listFiles()) {
                if (curMember.isFile()) {
                    File sourceFile = curMember;
                    IFile targetFile = targetFolder.getFile(sourceFile.getName());

                    try (InputStream theFileStream = new FileInputStream(sourceFile.toString())) {
                        targetFile.create(theFileStream, true, monitor);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else {
                    // curmember is a folder
                    IFolder newtargetFolder = targetFolder.getFolder(curMember.getName());
                    newtargetFolder.create(true, true, monitor);
                    recursiveCreateFiles(curMember, newtargetFolder, monitor);
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
        Bundle bundle = Platform.getBundle("io.sloeber.autoBuild.test"); //$NON-NLS-1$
        Path path = new Path("templates/" + myTemplateFolder); //$NON-NLS-1$
        URL fileURL = FileLocator.find(bundle, path, null);
        URL resolvedFileURL = FileLocator.toFileURL(fileURL);
        return new Path(resolvedFileURL.toURI().getPath());
    }
}
