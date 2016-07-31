/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer;

import java.awt.EventQueue;

/**
 *
 * @author Marcus Becker
 */
public class App {

    public static void main(String args[]) {

        try {

            String lookAndFeelName = "Nimbus";

            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {

                //System.out.println("info.getName " + info.getName());
                if (lookAndFeelName.equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(App.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ViewWindow().setVisible(true);
            }
        });
    }

}
