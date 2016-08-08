/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer.core;

import br.com.mvbos.jeg.element.ElementModel;
import br.com.mvbos.pathviwer.el.NodeElement;
import java.util.List;

/**
 *
 * @author Marcus Becker
 */
public interface DelimiterPath {

    public List<ElementModel> delimiter(NodeElement start, NodeElement end, List<ElementModel> lst);

}
