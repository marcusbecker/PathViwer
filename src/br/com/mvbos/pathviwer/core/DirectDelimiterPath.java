/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.core;

import br.com.mvbos.jeg.element.ElementModel;
import br.com.mvbos.pathviwer.el.NodeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Marcus Becker
 */
public class DirectDelimiterPath implements DelimiterPath {

    private NodeElement endPath = null;
    private NodeElement startPath = null;

    private final Set<NodeElement> goodPath = new HashSet<>(50);

    @Override
    public List<ElementModel> delimiter(NodeElement startPath, NodeElement endPath, List<ElementModel> elements) {

        List<ElementModel> lst = new ArrayList<>(elements);

        if (startPath == null || endPath == null) {
            return Collections.EMPTY_LIST;
        }

        goodPath.add(endPath);

        for (ElementModel e : lst) {
            if (!(e instanceof NodeElement)) {
                continue;
            }

            NodeElement n = (NodeElement) e;
            if (n.getChild().contains(endPath)) {
                goodPath.add(n);

                NodeElement p = n.getParent();
                while (p != null) {
                    goodPath.add(p);
                    p = p.getParent();
                }
            }
        }

        findParents(startPath, endPath, goodPath, lst);

        List<ElementModel> toRemove = new ArrayList<>(lst.size());
        for (ElementModel e : lst) {
            if (e instanceof NodeElement && goodPath.contains((NodeElement) e)) {
                continue;
            }

            toRemove.add(e);
        }

        return toRemove;
    }

    private void findParents(NodeElement startPath, NodeElement endPath, Set<NodeElement> goodPath, List<ElementModel> lst) {
        for (ElementModel e : lst) {
            if (!(e instanceof NodeElement)) {
                continue;
            }

            Set<NodeElement> temp = new HashSet<>(goodPath);
            NodeElement n = (NodeElement) e;

            for (NodeElement nn : temp) {

                if (startPath.equals(nn)) {
                    continue;
                }

                if (n.getChild().size() == 1) {
                    continue;
                }

                if (n.getChild().contains(nn)) {
                    goodPath.add(n);

                    NodeElement p = n.getParent();
                    while (p != null) {
                        goodPath.add(p);
                        p = p.getParent();
                    }
                }
            }
        }
    }

}
