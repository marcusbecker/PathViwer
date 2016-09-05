/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.core;

import br.com.mvbos.pathviwer.Project;
import java.io.File;
import java.io.FileFilter;
import java.util.LinkedHashSet;

/**
 *
 * @author MarcusS
 */
public class FileCoreCache {

    private final Project project;
    private final File folder;

    public FileCoreCache(Project project, File folder) {
        this.project = project;
        this.folder = folder;
    }

    public void load(FileFilter filter) {
        File[] fArr = folder.listFiles(filter);
        String[] sArr = new String[fArr.length];

        for (int i = 0; i < fArr.length; i++) {
            sArr[i] = fArr[i].getName();
        }

        for (File f : fArr) {
            String name = f.getName();
            LinkedHashSet<String> names = new LinkedHashSet<>(10);

            project.getTree().put(name, names);

            StringBuilder sb = FileCore.loadContent(f);

            for (String search : sArr) {
                if (name.equals(search) || !FileCore.serchText(sb, search)) {
                    continue;
                }

                names.add(search);
            }

            System.out.println("Load " + f.getName());

        }
    }

}
