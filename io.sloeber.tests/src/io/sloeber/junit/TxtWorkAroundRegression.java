
package io.sloeber.junit;

import static io.sloeber.core.txt.WorkAround.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.Shared;

@SuppressWarnings({ "nls", "static-method" })
@RunWith(Parameterized.class)
public class TxtWorkAroundRegression {
    final static private String ROOTFOLDER = "E:\\arduinoTxt-Backup-2021-08-19";
    private Path myPath;

    public TxtWorkAroundRegression(String pathName) {
        myPath = new Path(pathName);

    }

    @SuppressWarnings({ "rawtypes" })
    @Parameters(name = " {0}")
    public static Collection examples() {

        LinkedList<Object[]> folders = new LinkedList<>();
        boolean fullList = true; // set to false to limit the number of test cases
        if (fullList) {
            searchFolders(new File(ROOTFOLDER), folders, 10);
        } else {
            folders.add(new Object[] { "E:\\arduinoTxt-Backup-2021-08-19\\STM32\\hardware\\stm32\\1.9.0" });
            folders.add(new Object[] { "E:\\arduinoTxt-Backup-2021-08-19\\digistump\\hardware\\sam\\1.6.7" });
            folders.add(new Object[] { "E:\\arduinoTxt-Backup-2021-08-19\\nucDuino\\hardware\\nucDuino\\1.0.3" });

            folders.add(new Object[] { "E:\\arduinoTxt-Backup-2021-08-19\\TL7788\\hardware\\arm\\1.0.3" });
        }
        return folders;
    }

    private static void searchFolders(File folder, LinkedList<Object[]> txtFolders, int depth) {
        if (depth > 0) {
            File[] a = folder.listFiles();
            if (a == null) {
                return;
            }
            boolean isParentAdded = false;
            for (File f : a) {

                if (f.isDirectory()) {
                    searchFolders(f, txtFolders, depth - 1);
                } else if (f.getName().endsWith(".txt")) {
                    if (!isParentAdded) {
                        txtFolders.add(new Object[] { f.getParent() });
                        isParentAdded = true;
                    }
                }
            }
        }
    }

    @Test
    public void boardsTxt() throws Exception {
        String txtType = "boards";
        File inputFile = myPath.append(txtType + ".txt").toFile();
        File expectedFile = myPath.append(txtType + ".sloeber.txt").toFile();
        if (!inputFile.exists()) {
            System.out.println("file does not exists " + inputFile);
            assumeFalse(true);// skip the test
        }
        if (!expectedFile.exists()) {
            System.out.println("file does not exists " + expectedFile);
            assumeFalse(true);// skip the test
        }
        String input = FileUtils.readFileToString(inputFile, Charset.defaultCharset());
        input = input.replace("\r\n", "\n");
        String expected = FileUtils.readFileToString(expectedFile, Charset.defaultCharset());
        String actual = boardsApplyWorkArounds(input);
        String cleanedExpected = clean(expected);
        String cleanedActual = clean(actual);

        if (!cleanedExpected.equals(cleanedActual)) {
            System.err.println("ERROR for " + inputFile);
            String[] difference = Shared.difference(cleanedExpected, cleanedActual);
            for (String curDiff : difference) {
                System.err.println(curDiff);
            }
            fail(difference[0]);
        }

    }

    @Test
    public void platformTxt() throws Exception {
        String txtType = "platform";
        File inputFile = myPath.append(txtType + ".txt").toFile();
        File expectedFile = myPath.append(txtType + ".sloeber.txt").toFile();
        if (!inputFile.exists()) {
            System.out.println("file does not exists " + inputFile);
            assumeFalse(true);// skip the test
        }
        String input = FileUtils.readFileToString(inputFile, Charset.defaultCharset());
        input = input.replace("\r\n", "\n");
        String currentWorkAround = platformApplyWorkArounds(input, inputFile);
        String cleanedCurrentWorkAround = clean(currentWorkAround);

        //compare the Sloeber generated workaround file content to the content of the file on disk
        if (!expectedFile.exists()) {
            System.out.println("file does not exists " + expectedFile);
            assumeFalse(true);// skip the test
        }

        String expected = FileUtils.readFileToString(expectedFile, Charset.defaultCharset());
        String cleanedExpected = clean(expected);

        if (!cleanedExpected.equals(cleanedCurrentWorkAround)) {
            System.err.println("ERROR for " + inputFile);
            String[] difference = Shared.difference(cleanedExpected, cleanedCurrentWorkAround);
            for (String curDiff : difference) {
                System.err.println(curDiff);
            }
            fail(difference[0]);
        }

    }

    @Test
    public void programmerTxt() throws Exception {
        String txtType = "programmers";
        File inputFile = myPath.append(txtType + ".txt").toFile();
        File expectedFile = myPath.append(txtType + ".sloeber.txt").toFile();
        if (!inputFile.exists()) {
            System.out.println("file does not exists " + inputFile);
            assumeFalse(true);// skip the test
        }
        if (!expectedFile.exists()) {
            System.out.println("file does not exists " + expectedFile);
            return;
        }
        String input = FileUtils.readFileToString(inputFile, Charset.defaultCharset());
        input = input.replace("\r\n", "\n");
        String expected = FileUtils.readFileToString(expectedFile, Charset.defaultCharset());
        String actual = programmersApplyWorkArounds(input);
        String cleanedExpected = clean(expected);
        String cleanedActual = clean(actual);

        if (!cleanedExpected.equals(cleanedActual)) {
            System.err.println("ERROR for " + inputFile);
            String[] difference = Shared.difference(cleanedExpected, cleanedActual);
            for (String curDiff : difference) {
                System.err.println(curDiff);
            }
            fail(difference[0]);
        }

    }

    private String clean(String expected) {
        return expected.replace("\r\n", "\n").replaceAll("(?m)^#.*", "").replaceAll("(?m)^\\s*", "")
                .replaceAll("(?m)\\s*$", "").replaceAll("(?m)^(\\S*)\\s*=", "$1=").replace("\n\n", "\n")
                .replace("\n\n", "\n");
    }

}
