package io.sloeber.core.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

public class FileModifiers {
	/**
	 * method to add at the top of a file copied from
	 * http://stackoverflow.com/questions/6127648/writing-in-the-beginning-of-a-text-file-java
	 *
	 * @param input
	 * @param prefix
	 * @throws IOException
	 */
	public static void prependPrefix(File input, String prefix) throws IOException {
		LineIterator li = FileUtils.lineIterator(input);
		File tempFile = File.createTempFile("prependPrefix", ".tmp"); //$NON-NLS-1$ //$NON-NLS-2$

		try (BufferedWriter w = new BufferedWriter(new FileWriter(tempFile))) {
			w.write(prefix);
			while (li.hasNext()) {
				w.write(li.next());
				w.write("\n"); //$NON-NLS-1$
			}
			IOUtils.closeQuietly(w);
			LineIterator.closeQuietly(li);
		}
		FileUtils.deleteQuietly(input);
		FileUtils.moveFile(tempFile, input);
	}

	/**
	 * Add pragma once to all .h files from this path recursively
	 */
	public static void addPragmaOnce(Path startingDir) {

		class Finder extends SimpleFileVisitor<Path> {

			// Compares the glob pattern against
			// the file or directory name.
			void find(Path file) {
				Path filePath = file.getFileName();
				if (filePath != null) {
					String fileName = filePath.toString();
					if (fileName.length() > 2) {
						if (".h".equals(fileName.substring(fileName.length() - 2))) { //$NON-NLS-1$
							try {
								prependPrefix(file.toFile(), "#pragma once\n"); //$NON-NLS-1$
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}

			// Invoke the pattern matching
			// method on each file.
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				find(file);
				return FileVisitResult.CONTINUE;
			}

			// Invoke the pattern matching
			// method on each directory.
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				find(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				System.err.println(exc);
				return FileVisitResult.CONTINUE;
			}
		}

		Finder finder = new Finder();
		try {
			Files.walkFileTree(startingDir, finder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void addPragmaOnce(org.eclipse.core.runtime.Path curPath) {
		addPragmaOnce(Paths.get(curPath.toOSString()));

	}
}
