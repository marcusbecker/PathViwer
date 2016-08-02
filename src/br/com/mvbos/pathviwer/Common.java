/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 *
 * @author Marcus Becker
 */
public class Common {

    public static final Font textFont = new Font("Consolas", Font.BOLD, 14);

    public static boolean updateAll;
    public static Graphics2D graphics;
    public static int H_SPACE = 30;
    public static int W_SPACE = 10;
    public static final int LIMIT = 100;
    public static Project selProject;

    public static final String LN = "\r\n";
    public static final boolean userRegex = false;

    public static Color COLOR_FIRST = Color.RED;
    public static Color COLOR_MIDDLE = Color.BLUE;
    public static Color COLOR_LAST = Color.decode("#228B22"); //Color.GREEN;
    public static Color COLOR_BG = Color.WHITE;
    public static boolean AUTO_WIDTH = true;

}
