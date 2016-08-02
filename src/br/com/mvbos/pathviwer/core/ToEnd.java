/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.core;

import br.com.mvbos.pathviwer.Project;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author MarcusS
 */
public class ToEnd {

    private final Project project;
    private Set<String> toRemove;

    private final Set<String> temp = new LinkedHashSet<>(10);

    public ToEnd(Project project) {
        this.project = project;
    }

    public void toEnd(String endPath) {

        toRemove = new HashSet<>();

        for (String key : project.getTree().keySet()) {

            LinkedHashSet<String> lst = new LinkedHashSet(project.getTree().get(key));

            for (String s : lst) {

                if (remove(project.getRootNode(), endPath, s)) {
                    toRemove.add(s);
                }
            }
        }

        for (String s : toRemove) {
            project.getTree().remove(s);
        }

        // System.out.println(toRemove);
    }

    private boolean remove(String startPath, String endPath, String path) {
        if (startPath.equals(path) || endPath.equals(path)) {
            return false;
        }

        /*
         * if(!getMap().containsKey(path)) return true;
         */
        LinkedHashSet<String> lst = project.getTree().get(path);
        if (lst.isEmpty() || toRemove.contains(path)) {
            return true;
        }

        /*
         * if (lst.contains(endPath)){ return false; }
         */
        if (temp.size() > 100) {
            return true;
        }

        temp.add(path);

        for (String ss : lst) {
            if (ss.equals(path) || temp.contains(ss)) {
                continue;
            }

            temp.add(ss);

            if (remove(startPath, endPath, ss)) {
                toRemove.add(ss);
            }

            temp.remove(ss);
        }

        temp.remove(path);

        lst.removeAll(toRemove);

        return lst.isEmpty();
    }

}
