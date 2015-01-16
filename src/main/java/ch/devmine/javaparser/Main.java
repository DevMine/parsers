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
import com.google.gson.Gson;
import japa.parser.ParseException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
                               "path : the path of the repository to parse");
            return;
        }

        File repoRoot = new File(args[0]);
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
}
