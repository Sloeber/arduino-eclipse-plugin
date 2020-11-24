package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.dom.IPDOMManager;
import org.eclipse.cdt.core.index.IIndexManager;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.providers.Arduino;

@SuppressWarnings({ "nls", "static-method" })
public class ProjectCreationTest {

    private static String savedDefaultIndxerID;
    private static BoardDescriptor boardDescriptor;

    @BeforeClass
    public static void installMyStuff() {

        Arduino.installLatestAVRBoards();
        IIndexManager indexManager = CCorePlugin.getIndexManager();
        savedDefaultIndxerID = indexManager.getDefaultIndexerId();
        indexManager.setDefaultIndexerId(IPDOMManager.ID_NO_INDEXER);
        Shared.waitForAllJobsToFinish();

        Map<String, String> boardOptions = new HashMap<>();
        boardDescriptor = PackageManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
                "uno", boardOptions);

    }

    @AfterClass
    public static void resetStuff() {
        IIndexManager indexManager = CCorePlugin.getIndexManager();
        indexManager.setDefaultIndexerId(savedDefaultIndxerID);
    }

    @Test
    public void testManagedLibraryExample() {
        Map<String, IPath> examples = LibraryManager.getAllLibraryExamples();
        Entry<String, IPath> entry = examples.entrySet().iterator().next();
        String exampleName = entry.getKey();
        IPath examplePath = entry.getValue();
        testExample(exampleName, examplePath);
    }

    @Test
    public void testHardwareLibraryExample() {
        boardDescriptor.getReferencingLibraryPath().append("Wire/slave_sender");
        IPath example = new Path("");
        testExample("slave_sender", example);
    }

    public static void testExample(String projectName, IPath example) {

        ArrayList<IPath> paths = new ArrayList<>();


        paths.add(example);
        CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

        if (!Shared.BuildAndVerify(projectName, boardDescriptor, codeDescriptor, new CompileOptions(null))) {
            fail(Shared.getLastFailMessage());
        }

    }

}
