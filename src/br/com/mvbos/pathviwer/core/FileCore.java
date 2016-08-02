/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.core;

import br.com.mvbos.pathviwer.Common;
import br.com.mvbos.pathviwer.Project;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author MarcusS
 */
public class FileCore {

    private final Project project;
    private final File folder;

    public FileCore(Project project, File folder) {
        this.project = project;
        this.folder = folder;
    }

    public void load(String rootFile, FileFilter filter) {
        File[] fArr = folder.listFiles(filter);
        String[] sArr = new String[fArr.length];

        for (int i = 0; i < fArr.length; i++) {
            sArr[i] = fArr[i].getName();
        }

        load(rootFile, sArr);
    }

    private void load(String rootFile, String[] filesArr) {

        if (project.getTree().containsKey(rootFile)) {
            return;
        }

        File root = new File(folder, rootFile);

        LinkedHashSet<String> names = new LinkedHashSet<>(10);

        StringBuilder sb = loadContent(root);

        for (String f : filesArr) {

            if (f.equals(root.getName()) || !serchText(sb, f)) {
                continue;
            }

            names.add(f);
        }

        project.getTree().put(root.getName(), names);

        for (String f : names) {
            load(f, filesArr);
        }
    }

    public static boolean serchText(StringBuilder sb, String f) {

        if (Common.userRegex) {
            Pattern p = Pattern.compile(".*= *(\"|')" + f);
            Matcher m = p.matcher(sb);

            m.matches();

            return m.find();

        }

        return sb.indexOf(f) != -1;
    }

    public static StringBuilder loadContent(File root) {
        StringBuilder sb = new StringBuilder(800);

        try (BufferedReader br = new BufferedReader(new FileReader(root))) {
            String ln;
            while ((ln = br.readLine()) != null) {
                sb.append(ln).append(Common.LN);
            }

        } catch (FileNotFoundException e) {
            Logger.getLogger(FileCore.class.getName()).log(Level.SEVERE, e.getMessage());
        } catch (IOException e) {
            Logger.getLogger(FileCore.class.getName()).log(Level.SEVERE, e.getMessage());
        }

        return sb;
    }

}
