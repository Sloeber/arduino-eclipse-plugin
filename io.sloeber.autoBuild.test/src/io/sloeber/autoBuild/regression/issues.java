package io.sloeber.autoBuild.regression;

import static org.junit.Assert.fail;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.junit.Test;

@SuppressWarnings("nls")
public class issues {

    @Test
    public void cxx_associates_with_c() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (contentType.isAssociatedWith("test.c")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with c files");

        }
    }

    @Test
    public void cxx_associates_with_cpp() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (!contentType.isAssociatedWith("test.cpp")) {
            fail("org.eclipse.cdt.core.cxxSource should associate with cpp files");

        }
    }

    @Test
    public void cxx_associates_with_C() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (!contentType.isAssociatedWith("test.C")) {
            fail("org.eclipse.cdt.core.cxxSource should associate with C files");

        }
    }

    @Test
    public void cxx_associates_with_CPP() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (contentType.isAssociatedWith("test.CPP")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with CPP files");

        }
    }

    @Test
    public void c_associates_with_C() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cSource");
        if (contentType.isAssociatedWith("test.C")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with CPP files");

        }
    }

}
