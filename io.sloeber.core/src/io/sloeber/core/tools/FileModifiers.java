package io.sloeber.core.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.InstancePreferences;

public class FileModifiers {
    static final String PRAGMA_ONCE = System.lineSeparator() + "//Added by Sloeber \n#pragma once" //$NON-NLS-1$
            + System.lineSeparator();

    /**
     * method to add at the top of a file copied from
     * http://stackoverflow.com/questions/6127648/writing-in-the-beginning-of-a-text-file-java
     *
     * @param input
     * @param addString
     * @throws IOException
     */
    public static void appendString(File input, String addString) throws IOException {
        Path pathFile = Path.of(input.toString());
        try {
        String fileString = Files.readString(pathFile,StandardCharsets.UTF_8) + addString;
        Files.write(pathFile, fileString.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        }catch(IOException e) {
            String fileString = Files.readString(pathFile,Charset.forName("Cp1252")) + addString;
            Files.write(pathFile, fileString.getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        	
        }
        
    }

    /**
     * Add pragma once to all .h files from this path recursively if the option
     * is set
     */
    public static void addPragmaOnce(IPath startingDir) {
        if (!InstancePreferences.getPragmaOnceHeaders()) {
            return;
        }
        class Finder extends SimpleFileVisitor<java.nio.file.Path> {

            // Compares the glob pattern against
            // the file or directory name.
            void find(java.nio.file.Path file) {
                java.nio.file.Path fileNamePath = file.getFileName();
                if (fileNamePath != null) {
                    String fileName = fileNamePath.toString();
                    if (fileName.length() > 2) {
                        if (fileName.endsWith(".h")) { //$NON-NLS-1$
                            try {
                                appendString(file.toFile(), PRAGMA_ONCE);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            // Invoke the pattern matching
            // method on each file.
            @Override
            public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs) {
                find(file);
                return FileVisitResult.CONTINUE;
            }

            // Invoke the pattern matching
            // method on each directory.
            @Override
            public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) {
                find(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(java.nio.file.Path file, IOException exc) {
                System.err.println(exc);
                return FileVisitResult.CONTINUE;
            }
        }

        Finder finder = new Finder();
        try {
            Files.walkFileTree(Paths.get(startingDir.toString()), finder);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void replaceInFile(File file, boolean regex, String find, String replace) {
        try {
            Path pathFile = Path.of(file.toString());
            String textFromFile = Files.readString(pathFile, Charset.defaultCharset());

            if (regex) {
                textFromFile = textFromFile.replaceAll(find, replace);
            } else {
                textFromFile = textFromFile.replace(find, replace);
            }
            Files.write(pathFile, textFromFile.getBytes(), StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.CREATE);
        } catch (IOException e) {
            Common.log(new Status(IStatus.WARNING, Activator.getId(),
                    "Failed to replace " + find + " with " + replace + " in file " + file.toString(), e)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

}
