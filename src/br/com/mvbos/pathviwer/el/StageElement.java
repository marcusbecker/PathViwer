/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.el;

import br.com.mvbos.jeg.element.ElementModel;
import java.awt.Graphics2D;

/**
 *
 * @author MarcusS
 */
public class StageElement extends ElementModel {

    @Override
    public void drawMe(Graphics2D g) {
        g.setColor(getColor());
        g.fillRect(0, 0, getWidth(), getHeight());
    }

}
