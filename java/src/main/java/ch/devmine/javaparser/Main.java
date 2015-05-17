/*
 * Copyright 2014-2015 The DevMine Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style
 * license that can be found in the LICENSE file.
 */
package ch.devmine.javaparser;

import ch.devmine.javaparser.structures.Language;
import ch.devmine.javaparser.structures.Package;
import ch.devmine.javaparser.structures.Project;
import ch.devmine.javaparser.structures.SourceFile;
import ch.devmine.javaparser.utils.FileTypeFinder;
import ch.devmine.javaparser.utils.FileWalkerUtils;
import ch.devmine.javaparser.utils.GsonFactory;
import ch.devmine.javaparser.utils.Log;
import com.github.javaparser.ParseException;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Main class of the parser
 *
 * @author lweingart
 */
public class Main {

    /**
     * class tag
     */
    public static final String TAG = "Main class::";

    /**
     * language
     */
    private static final String JAVA = "java";

    /**
     * paradigms
     */
    private static final String OOP = "object-oriented";
    private static final String STRUCTURED = "structured";
    private static final String IMPERATIVE = "imperative";
    private static final String GENERIC = "generic";
    private static final String REFLECTIVE = "reflective";
    private static final String CONCURRENT = "concurrent";

    /**
     * Main function
     * @param args
     *      the path of the repository to parse
     */
    public static void main(String[] args) {

        // parse one repository
        if (args.length != 1) {
            System.err.println("usage : javaparser <path>\n" +
                               "path : the path to the folder or the tar archive to parse");
            return;
        }

        if (new File(args[0]).isDirectory()) {
            parseAsDirectory(args[0]);
        } else {
            parseAsTarArchive(args[0]);
        }
    }

    private static void parseAsTarArchive(String arg) {
        Project project = new Project();

        String projectName = arg.substring(arg.lastIndexOf("/") + 1, arg.lastIndexOf("."));
        project.setName(projectName);

        Language language = defineJavaLang();
        List<Language> languages = new ArrayList<>();
        languages.add(language);
        project.setLanguages(languages);

        HashMap<String, Package> packs =  new HashMap<>();

        try {
            TarArchiveInputStream tarInput = new TarArchiveInputStream(new FileInputStream(arg));
            TarArchiveEntry entry;
            File toParse;
            while (null != (entry = tarInput.getNextTarEntry())) {
                if (entry.getName().endsWith(".java")) {
                    // add package containing source file to hashmap
                    Package pack = new Package();
                    String packName = FileWalkerUtils.extractFolderName(entry.getName());
                    String packPath = extractPackagePath(entry.getName());
                    pack.setName(packName);
                    pack.setPath(packPath);
                    packs.put(packName, pack);

                    // parse java file
                    toParse = File.createTempFile(entry.getName(), null);
                    OutputStream os =  new FileOutputStream(toParse);
                    IOUtils.copy(tarInput, os);
                    Parser parser = new Parser(toParse.getPath());
                    SourceFile sourceFile = new SourceFile();
                    try {
                        parser.parse();
                        sourceFile.setPath(entry.getName());
                        sourceFile.setLanguage(language);
                        sourceFile.setImports(parser.getImports());
                        sourceFile.setInterfaces(parser.getInterfaces());
                        sourceFile.setClasses(parser.getClasses());
                        sourceFile.setEnums(parser.getEnums());
                        sourceFile.setLoc(parser.getLoc());

                        packs.get(packName).getSourceFiles().add(sourceFile);

                    } catch (ParseException ex) {
                        Log.e(TAG, "File skipped, error : ".concat(ex.getMessage()));
                    }
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }

        List<Package> packages = new ArrayList<>(packs.values());
        int projLoc = 0;
        for (Package pack : packages) {
            int packLoc = 0;
            for (SourceFile file : pack.getSourceFiles()) {
                packLoc += file.getLoc();
            }
            pack.setLoc(packLoc);
            projLoc += packLoc;
        }
        project.setPackages(packages);
        project.setLoc(projLoc);
        Gson gson = GsonFactory.build();
        String jsonProject = gson.toJson(project);

        // the result is written in the system output in order to be
        // used in chain with the source analyzer
        // see https://github.com/devmine/scranlzr
        System.out.println(jsonProject);
    }

    private static void parseAsDirectory(String arg) {
        File repoRoot = new File(arg);
        Path repoPath = repoRoot.toPath();

        FilesWalker fileWalker = new FilesWalker();
        try {
            Files.walkFileTree(repoPath, fileWalker);
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }

        Project project = new Project();
        String path = repoPath.toString();
        String name = path.substring(path.lastIndexOf("/") + 1);
        project.setName(name);

        Language language = defineJavaLang();
        List<Language> languages = new ArrayList<>();
        languages.add(language);
        project.setLanguages(languages);

        HashMap<String, Package> packs = new HashMap<>();
        for (String dir : fileWalker.getDirectories()) {
            dir = dir.replaceAll("\n", "");
            if (FileTypeFinder.search(dir, ".java")) {
                Package pack = new Package();
                String packName = dir.substring(dir.lastIndexOf("/") + 1);
                pack.setName(packName);
                pack.setPath(dir);
                packs.put(packName, pack);
            }
        }

        List<String> javaFiles = FileWalkerUtils.extractJavaFiles(fileWalker.getRegularFiles());
        for (String javaFile : javaFiles) {
            Parser parser = new Parser(javaFile);
            SourceFile sourceFile = new SourceFile();
            try {
                parser.parse();
                sourceFile.setPath(javaFile);
                sourceFile.setLanguage(language);
                sourceFile.setImports(parser.getImports());
                sourceFile.setInterfaces(parser.getInterfaces());
                sourceFile.setClasses(parser.getClasses());
                sourceFile.setEnums(parser.getEnums());
                sourceFile.setLoc(parser.getLoc());

                String folderName = FileWalkerUtils.extractFolderName(javaFile);
                packs.get(folderName).getSourceFiles().add(sourceFile);
            } catch (ParseException e) {
                Log.e(TAG, "File skipped, error : ".concat(e.getMessage()));
            } catch (FileNotFoundException ex) {
                Log.e(TAG, ex.getMessage());
            }
        }

        List<Package> packages = new ArrayList<>(packs.values());
        int projLoc = 0;
        for (Package pack : packages) {
            int packLoc = 0;
            for (SourceFile file : pack.getSourceFiles()) {
                packLoc += file.getLoc();
            }
            pack.setLoc(packLoc);
            projLoc += packLoc;
        }
        project.setPackages(packages);
        project.setLoc(projLoc);
        Gson gson = GsonFactory.build();
        String jsonProject = gson.toJson(project);

        // the result is written in the system output in order to be
        // used in chain with the source analyzer
        // see https://github.com/devmine/scranlzr
        System.out.println(jsonProject);
    }

    private static Language defineJavaLang() {
        Language language = new Language();
        language.setLanguage(JAVA);
        List<String> paradigms = new ArrayList<>();
        paradigms.add(OOP);
        paradigms.add(STRUCTURED);
        paradigms.add(IMPERATIVE);
        paradigms.add(GENERIC);
        paradigms.add(REFLECTIVE);
        paradigms.add(CONCURRENT);
        language.setParadigms(paradigms);

        return language;
    }

    private static String extractPackagePath(String sourceFilePath) {
        int lastSlashIndex = sourceFilePath.lastIndexOf("/");
        return sourceFilePath.substring(0, lastSlashIndex);
    }

}
