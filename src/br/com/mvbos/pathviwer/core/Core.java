/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.core;

import br.com.mvbos.jeg.element.ElementModel;
import br.com.mvbos.pathviwer.Project;
import br.com.mvbos.pathviwer.el.NodeElement;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MarcusS
 */
public class Core {

    private static File DEF_DIR = new File(".");

    public static Project loadProject(String name) {
        return loadProjectFrom(name, DEF_DIR);
    }

    public static Project loadProjectFrom(String name, File dir) {

        File f = new File(dir, name.concat(".pvp"));
        Project p = null;

        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {

                p = (Project) ois.readObject();

                Logger.getLogger(Core.class.getName()).log(Level.INFO, "Load successful!");

            } catch (IOException | ClassNotFoundException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        return p == null ? new Project(name, null) : p;
    }

    public static boolean save(Project project) {
        return saveAs(project, DEF_DIR);
    }

    public static boolean saveAs(Project project, File dir) {
        File f = new File(dir, project.getName().concat(".pvp"));
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(project);

            Logger.getLogger(Core.class.getName()).log(Level.INFO, "Save successful!");

            return true;

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
        }

        return false;
    }

    public static int sumHeight(ElementModel e) {
        int total = 0;

        if (e instanceof NodeElement) {
            NodeElement ne = (NodeElement) e;
            for (NodeElement n : ne.getChild()) {
                total += n.getAllHeight();
            }

        } else {
            total = e.getAllHeight();
        }

        return total;
    }

    public static ElementModel filter(String name, List<ElementModel> list) {
        for (ElementModel e : list) {
            if (name.equals(e.getName())) {
                return e;
            }
        }

        return null;
    }

    public static Point getLastPosition(NodeElement node) {
        Point p = new Point();

        if (node != null) {
            for (NodeElement n : node.getChild()) {
                if (n.getPx() > p.x) {
                    p.x = n.getPx();
                }

                if (n.getPy() > p.y) {
                    p.y = n.getPy();
                }
            }
        }

        return p;
    }

}
