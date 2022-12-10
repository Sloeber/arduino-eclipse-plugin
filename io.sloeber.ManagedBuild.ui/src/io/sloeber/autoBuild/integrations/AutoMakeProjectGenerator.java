
package io.sloeber.autoBuild.integrations;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.tools.templates.core.IGenerator;
import org.eclipse.tools.templates.freemarker.SourceRoot;
import org.eclipse.tools.templates.freemarker.TemplateManifest;

public class AutoMakeProjectGenerator implements IGenerator {

    public AutoMakeProjectGenerator() {

    }

    @Override
    public void generate(IProgressMonitor monitor) throws CoreException {
        // TODO Auto-generated method stub
        IGenerator.super.generate(monitor);

        List<IPathEntry> entries = new ArrayList<>();
        IProject project = getProject();

        // Create the source and output folders
        IFolder buildFolder = getProject().getFolder("build"); //$NON-NLS-1$

        TemplateManifest manifest = getManifest();
        if (manifest != null) {
            List<SourceRoot> srcRoots = getManifest().getSrcRoots();
            if (srcRoots != null && !srcRoots.isEmpty()) {
                for (SourceRoot srcRoot : srcRoots) {
                    IFolder sourceFolder = project.getFolder(srcRoot.getDir());
                    if (!sourceFolder.exists()) {
                        sourceFolder.create(true, true, monitor);
                    }

                    entries.add(CoreModel.newSourceEntry(sourceFolder.getFullPath(),
                            new IPath[] { buildFolder.getFullPath() }));
                }
            } else {
                entries.add(CoreModel.newSourceEntry(getProject().getFullPath()));
            }
        }

        entries.add(CoreModel.newOutputEntry(buildFolder.getFullPath(), // $NON-NLS-1$
                new IPath[] { new Path("**/CMakeFiles/**") })); //$NON-NLS-1$
        CoreModel.getDefault().create(project).setRawPathEntries(entries.toArray(new IPathEntry[entries.size()]),
                monitor);
    }

    @Override
    public IFile[] getFilesToOpen() {
        // TODO Auto-generated method stub
        return IGenerator.super.getFilesToOpen();
    }

    private TemplateManifest getManifest() {
        // TODO Auto-generated method stub
        return null;
    }

    private IProject getProject() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setProjectName(String projectName) {
        // TODO Auto-generated method stub

    }

    public void setLocationURI(URI locationURI) {
        // TODO Auto-generated method stub

    }

}
