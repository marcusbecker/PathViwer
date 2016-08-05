/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.el;

import br.com.mvbos.jeg.element.ElementModel;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 *
 * @author Marcus Becker
 */
public class EdgeElement extends ElementModel {

    private final NodeElement parent;
    private final NodeElement child;
    private boolean selected;

    public EdgeElement(NodeElement parent, NodeElement child) {
        super(1, 1, "node_" + parent.getName() + child.getName());
        this.parent = parent;
        this.child = child;

    }

    @Override
    public void drawMe(Graphics2D g) {

        if (!isVisible()) {
            return;
        }

        //g.setColor(selected ? Color.MAGENTA : Color.LIGHT_GRAY);
        g.setColor(getColor());

        if (parent == child) {
            g.drawRect(parent.getAllWidth() - 10, parent.getAllHeight() - 10, 15, 15);
            return;
        }

        int stpxA = (int) parent.getAllWidth() - parent.getHalfWidth();
        int stpxB = (int) child.getAllWidth() - child.getHalfWidth();

        int stpyA = (int) parent.getAllHeight() - parent.getHalfHeight();
        int stpyB = (int) child.getAllHeight() - child.getHalfHeight();

        final int middle = (stpxA + stpxB) / 2;

        //g.drawString(c.getName(), middle, middle);
        //g.setColor(getColor());
        g.drawLine(stpxA, stpyA, middle, stpyA);

        //g.setColor(c.getColor());
        g.drawLine(stpxB, stpyB, middle, stpyB);

        //g.setColor(getColor());
        g.drawLine(middle, stpyA, middle, stpyB);

        final int fSize = g.getFontMetrics(g.getFont()).stringWidth(getDesc(false));
        g.setColor(Color.ORANGE);
        g.drawString(getDesc(parent.getPx() > child.getPx()), middle - fSize / 2, (stpyA + stpyB) / 2);
    }

    private String getDesc(boolean b) {
        return b ? "<" : ">";
    }

    public NodeElement getParent() {
        return parent;
    }

    public NodeElement getChild() {
        return child;
    }

    public void remove(NodeElement nodeElement) {
        if (getParent() == nodeElement || getChild() == nodeElement) {
            setVisible(false);
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

}
