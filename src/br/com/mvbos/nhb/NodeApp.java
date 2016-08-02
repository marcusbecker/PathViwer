package br.com.mvbos.nhb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NodeApp {

    private static final String LN = "\r\n";

    private static final boolean userRegex = false;

    private File dir = new File("D:/");

    private Map<String, List<String>> map = new LinkedHashMap<>();

    private Set<String> toRemove;

    public void toEnd(String startPath, String endPath) {

        toRemove = new HashSet<>();

        for (String key : getMap().keySet()) {
            List<String> lst = new ArrayList<>(getMap().get(key));

            for (String s : lst) {

                if (remove(startPath, endPath, s)) {
                    toRemove.add(s);
                }
            }
        }

        for (String s : toRemove) {
            getMap().remove(s);
        }

		// System.out.println(toRemove);
    }

    private final Set<String> temp = new LinkedHashSet<>(10);

    private boolean remove(String startPath, String endPath, String path) {
        if (startPath.equals(path) || endPath.equals(path)) {
            return false;
        }

        /*
         * if(!getMap().containsKey(path)) return true;
         */
        List<String> lst = getMap().get(path);
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

    public StringBuilder createGraph() {
        // System.out.println(app.getMap());
        String[] keyArray = getMap().keySet().toArray(new String[0]);

        StringBuilder sb = new StringBuilder(600);

        Map<String, Integer> index = new HashMap<>(keyArray.length);

        sb.append("digraph {").append(LN);
        sb.append("node [rx=5 ry=5 labelStyle=\"font: 300 14px 'Helvetica Neue', Helvetica\"]").append(LN);
        sb.append("edge [labelStyle=\"font: 300 14px 'Helvetica Neue', Helvetica\"]").append(LN);

        for (int i = 0; i < keyArray.length; i++) {

            /*
             * if (getMap().get(keyArray[i]).isEmpty()) continue;
             */
            index.put(keyArray[i], i + 1);

            sb.append(i + 1).append(" [");

            if (i == 0) {
                sb.append("labelType=\"html\"").append(" ");
            }

            if (getMap().get(keyArray[i]).isEmpty()) {
                sb.append("style=\"fill: #f77; font-weight: bold\"").append(" ");
            }

            sb.append("label=\"").append(keyArray[i]).append("\"").append("];\r\n");
        }

        for (int i = 0; i < keyArray.length; i++) {
            int idx = i + 1;
            List<String> lst = getMap().get(keyArray[i]);

            if (lst.isEmpty()) {
                continue;
            }

            for (String f : lst) {
                if (!getMap().containsKey(f)) {
                    continue;
                }

                sb.append(idx).append(" -> ").append(index.get(f)).append(";\r\n");
            }

        }

        sb.append("}");

        return sb;
    }

    public File getDir() {
        return dir;
    }

    public void setDir(File dir) {
        this.dir = dir;
    }

    public Map<String, List<String>> getMap() {
        return map;
    }

    public void run(String rootFile, File dir, FileFilter filter) {
        File[] fArr = dir.listFiles(filter);
        String[] sArr = new String[fArr.length];

        for (int i = 0; i < fArr.length; i++) {
            sArr[i] = fArr[i].getName();
        }

        run(rootFile, sArr);
    }

    private void run(String rootFile, String[] filesArr) {

        if (map.containsKey(rootFile)) {
            return;
        }

        File root = new File(dir, rootFile);

        List<String> names = new ArrayList<>(10);

        StringBuilder sb = loadContent(root);

        for (String f : filesArr) {

            if (f.equals(root.getName()) || !serchText(sb, f)) {
                continue;
            }

            names.add(f);
        }

        map.put(root.getName(), names);

        for (String f : names) {
            run(f, filesArr);
        }
    }

    private boolean serchText(StringBuilder sb, String f) {

        if (!userRegex) {
			// Pattern p = Pattern.compile(".*= *(\"|')" + f);
            // Matcher m = p.matcher(sb);

			// System.out.println(p.pattern());
            // System.out.println(m.toString());
            return sb.indexOf(f) != -1;
        }

        Pattern p = Pattern.compile("^.*= *(\"|')" + f);
        Matcher m = p.matcher(sb);

        m.matches();

        return m.find();
    }

    private StringBuilder loadContent(File root) {
        StringBuilder sb = new StringBuilder(800);
        try {
            BufferedReader br = new BufferedReader(new FileReader(root));
            String ln;
            while ((ln = br.readLine()) != null) {
                sb.append(ln).append(LN);
            }

            br.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb;
    }

}
