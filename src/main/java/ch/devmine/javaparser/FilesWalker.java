/*
 * Copyright 2014-2015 The DevMine Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package ch.devmine.javaparser;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import static java.nio.file.FileVisitResult.CONTINUE;

public class FilesWalker extends SimpleFileVisitor<Path> {

    public static final String TAG = "FilesWalker class::";
    private final List<String> regularFiles;
    private final List<String> directories;

    public FilesWalker() {
        super();
        regularFiles = new ArrayList<>();
        directories = new ArrayList<>();
    }

    // Print information about
    // each type of file.
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
            regularFiles.add(String.format("%s", file));
        }
        return CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        directories.add(String.format("%s%n", dir));
        return CONTINUE;
    }

    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException
    // is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        System.err.println(exc);
        return CONTINUE;
    }

    public List<String> getRegularFiles() {
        return this.regularFiles;
    }

    public List<String> getDirectories() {
        return this.directories;
    }
}
