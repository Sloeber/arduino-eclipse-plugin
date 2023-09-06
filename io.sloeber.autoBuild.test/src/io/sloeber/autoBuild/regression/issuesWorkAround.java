package io.sloeber.autoBuild.regression;

import static org.junit.Assert.fail;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.junit.Test;

/*
 * This test is to test my replacement code
 * 
 */

@SuppressWarnings({ "nls", "static-method" })
public class issuesWorkAround {
    private static String getFileExtension(String fileName) {
        int dotPosition = fileName.lastIndexOf('.');
        return (dotPosition == -1 || dotPosition == fileName.length() - 1) ? "" : fileName.substring(dotPosition + 1); //$NON-NLS-1$
    }

    private static boolean hasFileSpec(IContentType contentType, String text, int typeMask) {
        String[] fileSpecs = contentType.getFileSpecs(typeMask);
        for (String fileSpec : fileSpecs)
            if (text.equals(fileSpec))
                return true;
        return false;
    }

    private static boolean isAssociatedWith(IContentType contentType, String fileName) {
        if (hasFileSpec(contentType, fileName, IContentType.FILE_NAME_SPEC))
            return true;
        String fileExtension = getFileExtension(fileName);
        if (hasFileSpec(contentType, fileExtension, IContentType.FILE_EXTENSION_SPEC))
            return true;
        return false;
    }

    @Test
    public void cxx_associates_with_c() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (isAssociatedWith(contentType, "test.c")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with c files");

        }
    }

    @Test
    public void cxx_associates_with_cpp() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (!isAssociatedWith(contentType, "test.cpp")) {
            fail("org.eclipse.cdt.core.cxxSource should associate with cpp files");

        }
    }

    @Test
    public void cxx_associates_with_C() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (!isAssociatedWith(contentType, "test.C")) {
            fail("org.eclipse.cdt.core.cxxSource should associate with C files");

        }
    }

    @Test
    public void cxx_associates_with_CPP() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (isAssociatedWith(contentType, "test.CPP")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with CPP files");

        }
    }

    @Test
    public void c_associates_with_C() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cSource");
        if (isAssociatedWith(contentType, "test.C")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with CPP files");

        }
    }

}
