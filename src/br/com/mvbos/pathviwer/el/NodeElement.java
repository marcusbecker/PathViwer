/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.el;

import br.com.mvbos.jeg.element.ElementModel;
import br.com.mvbos.pathviwer.Common;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marcus Becker
 */
public class NodeElement extends ElementModel {

    private NodeElement parent;
    private final List<NodeElement> child = new ArrayList<>(5);
    private boolean selected;

    public NodeElement() {
    }

    public NodeElement(int width, int height, String name) {
        super(width, height, name);
    }

    public NodeElement(float positionX, float positionY, int width, int height, String name) {
        super(positionX, positionY, width, height, name);
    }

    @Override
    public void update() {
        super.update(); //To change body of generated methods, choose Tools | Templates.

        if (Common.AUTO_WIDTH && Common.graphics != null) {
            short w = (short) (Common.graphics.getFontMetrics(Common.textFont).stringWidth(name) + 10);
            w = w < getWidth() ? (short) getWidth() : w;
            setWidth(w);
        }
    }

    @Override
    public void drawMe(Graphics2D g) {
        super.drawMe(g); //To change body of generated methods, choose Tools | Templates.
        //g.drawString((parent != null ? parent.getName() + " -> " : "") + name, getPx() + 5, getPy() + 10);
        //drawEdge(g);

        g.setFont(Common.textFont);

        g.setColor(Common.COLOR_BG);
        g.fillRect(getPx() + 1, getPy() + 1, getWidth() - 1, getHeight() - 1);

        if (parent == null) {
            g.setColor(Common.COLOR_FIRST);
        } else if (child.isEmpty() || (child.size() == 1 && child.contains(parent))) {
            g.setColor(Common.COLOR_LAST);
        } else {
            g.setColor(Common.COLOR_MIDDLE);
        }

        g.drawString(name, getPx() + 5, getPy() + 20);
        g.drawRect(getPx(), getPy(), getWidth(), getHeight());

    }

    @Deprecated
    public void drawEdge(Graphics2D g) {

        for (NodeElement c : getChild()) {
            g.setColor(Color.LIGHT_GRAY);

            if (this == c) {
                g.drawRect(getAllWidth() - 10, getAllHeight() - 10, 15, 15);
                continue;
            }

            int stpxA = (int) getAllWidth() - getHalfWidth();
            int stpxB = (int) c.getAllWidth() - c.getHalfWidth();

            int stpyA = (int) getAllHeight() - getHalfHeight();
            int stpyB = (int) c.getAllHeight() - c.getHalfHeight();

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
            g.drawString(getDesc(getPx() > c.getPx()), middle - fSize / 2, (stpyA + stpyB) / 2);

        }
    }

    private String getDesc(boolean b) {
        return b ? "<" : ">";
    }

    public NodeElement getParent() {
        return parent;
    }

    public void setParent(NodeElement parent) {
        this.parent = parent;
    }

    public List<NodeElement> getChild() {
        return child;
    }

    public void addChild(NodeElement el) {
        child.add(el);
    }

    public void remove(NodeElement t) {
        child.remove(t);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        //return "NodeElement{" + getName() + ", " + "parent=" + parent.getName() + ", child=" + child.size() + ", selected=" + selected + '}';
        return getName();
    }

}
