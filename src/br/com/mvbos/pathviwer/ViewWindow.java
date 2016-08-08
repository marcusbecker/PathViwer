/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.mvbos.pathviwer;

import br.com.mvbos.pathviwer.el.StageElement;
import br.com.mvbos.jeg.element.ElementModel;
import br.com.mvbos.jeg.element.SelectorElement;
import br.com.mvbos.jeg.engine.GraphicTool;
import br.com.mvbos.jeg.engine.SpriteTool;
import br.com.mvbos.jeg.window.Camera;
import br.com.mvbos.jeg.window.IMemory;
import br.com.mvbos.jeg.window.impl.MemoryImpl;
import br.com.mvbos.pathviwer.core.Core;
import br.com.mvbos.pathviwer.core.DelimiterPath;
import br.com.mvbos.pathviwer.core.DirectDelimiterPath;
import br.com.mvbos.pathviwer.core.FileCore;
import br.com.mvbos.pathviwer.el.EdgeElement;
import br.com.mvbos.pathviwer.el.NodeElement;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 *
 * @author MarcusS
 */
public class ViewWindow extends javax.swing.JFrame {

    private boolean priority = true;

    private enum EditTool {

        SELECTOR, HAND, RELATION;
    }

    private MyPanel canvas;
    private final Timer timer;

    private final int camSize = 1500;

    private boolean isAltDown;
    private boolean isControlDown;

    private short zoom;

    private Point mousePos = new Point();
    private Point startDrag;

    //private ElementModel selectedElement;
    private final ElementModel[] selectedElements = new ElementModel[30];

    private final IMemory memoryTemp = new MemoryImpl(60);

    private final SelectorElement selector = new SelectorElement("selector");

    private final ElementModel mouseElement = new ElementModel(10, 10, "mouseElement");

    private List<ElementModel> elements = new ArrayList<>(100);

    private final ElementModel stageEl = new StageElement();

    private EditTool mode = EditTool.SELECTOR;

    private final Camera cam = Camera.createNew();

    private boolean inLoad;

    private Project project;

    private final Map<String, NodeElement> hist = new HashMap<>(50);

    private final StringBuilder tStringBuilder = new StringBuilder(100);

    private class MyDispatcher implements KeyEventDispatcher {

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {

            isAltDown = e.isAltDown();
            isControlDown = e.isControlDown();

            if (getFocusOwner() == null) {
                return false;
            }

            if (e.getID() == KeyEvent.KEY_PRESSED) {

                if (KeyEvent.VK_PAGE_DOWN == e.getKeyCode() || KeyEvent.VK_PAGE_UP == e.getKeyCode()) {

                    if (isControlDown) {
                        cam.rollX(KeyEvent.VK_PAGE_DOWN == e.getKeyCode() ? 100 : -100);
                    } else {
                        cam.rollY(KeyEvent.VK_PAGE_DOWN == e.getKeyCode() ? 100 : -100);
                    }

                    e.consume();

                } else if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    mode = EditTool.SELECTOR;

                } else if (KeyEvent.VK_LEFT == e.getKeyCode()) {

                    if (isAltDown && getSingleSelected(NodeElement.class) != null) {
                        NodeElement ne = getSingleSelected(NodeElement.class);
                        foccus(ne.getParent());
                    }
                }

            } else if (e.getID() == KeyEvent.KEY_RELEASED) {

                if (107 == e.getKeyCode() || KeyEvent.VK_PLUS == e.getKeyCode()) {
                    applyZoom(10);

                } else if (109 == e.getKeyCode() || KeyEvent.VK_MINUS == e.getKeyCode()) {
                    applyZoom(-10);

                } else if (KeyEvent.VK_EQUALS == e.getKeyCode()) {
                    applyZoom(0);

                } else if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    removeSelNodes();
                }

            } else if (e.getID() == KeyEvent.KEY_TYPED) {
            }

            if (isAltDown) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }

            return false;
        }

        private <T extends ElementModel> T getSingleSelected(Class<T> type) {
            if (type.isInstance(selectedElements[0])) {
                return type.cast(selectedElements[0]);
            }

            return null;
        }

    }

    private void foccus(ElementModel el) {
        if (el == null) {
            return;
        }

        singleSelection(el);
        cam.center(el);
    }

    private void createNodes() {
        elements.clear();
        hist.clear();
        inLoad = true;

        new Thread() {
            @Override
            public void run() {
                addElement(project.getRootNode(), null, 1);

                for (ElementModel e : elements) {
                    //order((NodeElement) e);
                }

                if (priority) {
                    for (ElementModel e : elements) {
                        NodeElement n = (NodeElement) e;
                        Set<String> lst = project.getTree().get(n.getName());
                        for (String k : lst) {
                            if (!project.getTree().containsKey(k)) {
                                continue;
                            }

                            NodeElement ee = (NodeElement) Core.filter(k, elements);
                            if (!n.getChild().contains(ee)) {
                                n.getChild().add(ee);
                            }
                        }
                    }
                }

                foccus(Core.filter(project.getRootNode(), elements));

                createEdges();
                recalcStegeELSize();

                inLoad = false;
            }
        }.start();
    }

    private void delimiterPathDirect(final String start, final String end) {
        new Thread() {

            @Override
            public void run() {
                inLoad = true;
                singleSelection(null);

                NodeElement startPath = (NodeElement) Core.filter(start, elements);
                NodeElement endPath = (NodeElement) Core.filter(end, elements);

                DelimiterPath dp = new DirectDelimiterPath();
                List<ElementModel> lst = dp.delimiter(startPath, endPath, elements);

                for (ElementModel e : lst) {
                    elements.remove(e);
                    project.getTree().remove(e.getName());
                }

                createNodes();
            }

        }.start();
    }

    private void addElement(String name, NodeElement parent, int deep) {

        if (deep == Common.LIMIT) {
            return;
        }

        if (!project.getTree().containsKey(name)) {
            return;
        }

        if (hist.containsKey(name)) {
            parent.addChild(hist.get(name));
            return;
        }

        String[] lst = project.getTree().get(name).toArray(new String[0]);

        NodeElement el = new NodeElement(140, 40, name);
        el.update();

        if (parent == null) {
            el.setPxy(Common.W_SPACE, 0);

        } else {
            Point p = Core.getLastPosition(parent.getParent());
            el.setParent(parent);
            el.setPxy(parent.getAllWidth() + 30, p.y);

            parent.addChild(el);
        }

        hist.put(name, el);
        elements.add(el);

        order(el);

        for (int i = 0; i < lst.length; i++) {
            String newNode = lst[i];

            if (priority) {
                NodeElement p = parent;
                while (p != null) {
                    if (project.getTree().get(p.getName()).contains(newNode)) {
                        break;
                    }

                    p = p.getParent();
                }

                if (p == null) {
                    addElement(newNode, el, i + 1);
                }

            } else {
                addElement(newNode, el, i + 1);
            }
        }
    }

    private void order(NodeElement el) {
        boolean again;
        boolean colide = false;

        do {
            again = false;
            for (ElementModel e : elements) {
                if (GraphicTool.g().bcollide(e, el)) {
                    colide = true;
                    again = true;
                    el.setPy(e.getAllHeight() + Common.H_SPACE);
                    break;
                }
            }
        } while (again);

        if (colide) {
            NodeElement p = el.getParent();
            while (p != null) {
                //System.out.println("repex " + p.getName());

                int max = el.getAllHeight(), min = el.getPy();
                for (NodeElement temp : p.getChild()) {
                    min = temp.getPy() < min ? temp.getPy() : min;
                    max = temp.getAllHeight() > max ? temp.getAllHeight() : max;
                }

                p.setPy(Math.round(max / 2f) + (min / 2) - p.getHalfHeight());
                p = p.getParent();
            }
        }
    }

    private void createEdges() {
        List<ElementModel> temp = new ArrayList<>(elements.size());

        for (ElementModel e : elements) {
            if (!(e instanceof NodeElement)) {
                continue;
            }

            NodeElement n = (NodeElement) e;
            for (NodeElement c : n.getChild()) {
                temp.add(new EdgeElement(n, c));
            }
        }

        elements.addAll(temp);
    }

    public ViewWindow() {

        initComponents();

        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new MyDispatcher());

        cam.config(camSize, camSize, canvas.getWidth(), canvas.getHeight());
        cam.setAllowOffset(true);
        stageEl.setSize(camSize, camSize);
        stageEl.setColor(Color.decode("#323232"));

        selector.setColor(Color.LIGHT_GRAY);

        SpriteTool s = SpriteTool.s(new ImageIcon("multimedia-153230_960_720.png")).matriz(5, 3);
        btnStart.setIcon(s.createIcon(2, 0));

        timer = new Timer(60, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.repaint();
            }
        });

        timer.start();
    }

    private boolean isSelected(ElementModel e) {
        for (ElementModel sel : selectedElements) {
            if (sel == null) {
                break;
            }
            if (sel == e) {
                return true;
            }
        }

        return false;
    }

    private void reConfigCam() {
        int w = cam.getSceneWidth() > stageEl.getWidth() ? cam.getSceneWidth() : stageEl.getWidth();
        int h = cam.getSceneHeight() > stageEl.getHeight() ? cam.getSceneHeight() : stageEl.getHeight();

        cam.config(w, h, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnFromTo = new javax.swing.JFrame();
        tfFrom = new javax.swing.JTextField();
        tfTo = new javax.swing.JTextField();
        btnFromTo = new javax.swing.JButton();
        cbCancelReverse = new javax.swing.JCheckBox();
        pnsearch = new javax.swing.JFrame();
        tfSearch = new javax.swing.JTextField();
        btnSearchNext = new javax.swing.JButton();
        pnToolBar = new javax.swing.JPanel();
        lblNodeChildren = new javax.swing.JLabel();
        lblNodeParent = new javax.swing.JLabel();
        pnCanvas = createCanvas();
        jPanel1 = new javax.swing.JPanel();
        btnStart = new javax.swing.JButton();
        mbMain = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        miRun = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        miOpenSearch = new javax.swing.JMenuItem();
        miToEnd = new javax.swing.JMenuItem();

        pnFromTo.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        pnFromTo.setResizable(false);

        btnFromTo.setText("Execute");
        btnFromTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFromToActionPerformed(evt);
            }
        });

        cbCancelReverse.setSelected(true);
        cbCancelReverse.setText("Cancel Reverse");

        javax.swing.GroupLayout pnFromToLayout = new javax.swing.GroupLayout(pnFromTo.getContentPane());
        pnFromTo.getContentPane().setLayout(pnFromToLayout);
        pnFromToLayout.setHorizontalGroup(
            pnFromToLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnFromToLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnFromToLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnFromToLayout.createSequentialGroup()
                        .addComponent(tfFrom, javax.swing.GroupLayout.DEFAULT_SIZE, 145, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(tfTo, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFromTo))
                    .addComponent(cbCancelReverse, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(17, Short.MAX_VALUE))
        );
        pnFromToLayout.setVerticalGroup(
            pnFromToLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnFromToLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnFromToLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfFrom, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tfTo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFromTo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbCancelReverse)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnsearch.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        pnsearch.setTitle("Search");
        pnsearch.setAlwaysOnTop(true);
        pnsearch.setUndecorated(true);
        pnsearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                pnsearchKeyReleased(evt);
            }
        });

        tfSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                tfSearchKeyReleased(evt);
            }
        });

        btnSearchNext.setText(">");
        btnSearchNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchNextActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnsearchLayout = new javax.swing.GroupLayout(pnsearch.getContentPane());
        pnsearch.getContentPane().setLayout(pnsearchLayout);
        pnsearchLayout.setHorizontalGroup(
            pnsearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnsearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tfSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearchNext)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnsearchLayout.setVerticalGroup(
            pnsearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnsearchLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnsearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tfSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearchNext))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PathViwer");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowStateListener(new java.awt.event.WindowStateListener() {
            public void windowStateChanged(java.awt.event.WindowEvent evt) {
                formWindowStateChanged(evt);
            }
        });

        pnToolBar.setBackground(new java.awt.Color(204, 204, 204));

        lblNodeChildren.setText("...");

        lblNodeParent.setText("...");

        javax.swing.GroupLayout pnToolBarLayout = new javax.swing.GroupLayout(pnToolBar);
        pnToolBar.setLayout(pnToolBarLayout);
        pnToolBarLayout.setHorizontalGroup(
            pnToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnToolBarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblNodeChildren, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblNodeParent, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnToolBarLayout.setVerticalGroup(
            pnToolBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnToolBarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblNodeParent)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblNodeChildren)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnCanvasLayout = new javax.swing.GroupLayout(pnCanvas);
        pnCanvas.setLayout(pnCanvasLayout);
        pnCanvasLayout.setHorizontalGroup(
            pnCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 960, Short.MAX_VALUE)
        );
        pnCanvasLayout.setVerticalGroup(
            pnCanvasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        btnStart.setToolTipText("Start from selected node");
        btnStart.setBorder(null);
        btnStart.setBorderPainted(false);
        btnStart.setPreferredSize(new java.awt.Dimension(35, 35));
        btnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(btnStart, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 397, Short.MAX_VALUE))
        );

        jMenu1.setText("Project");

        miRun.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        miRun.setText("Run");
        miRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRunActionPerformed(evt);
            }
        });
        jMenu1.add(miRun);

        mbMain.add(jMenu1);

        jMenu2.setText("Edit");

        miOpenSearch.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        miOpenSearch.setText("Search");
        miOpenSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenSearchActionPerformed(evt);
            }
        });
        jMenu2.add(miOpenSearch);

        miToEnd.setText("Delimiter Path");
        miToEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miToEndActionPerformed(evt);
            }
        });
        jMenu2.add(miToEnd);

        mbMain.add(jMenu2);

        setJMenuBar(mbMain);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnCanvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(pnToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnCanvas, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing

        timer.stop();

    }//GEN-LAST:event_formWindowClosing

    private void formWindowStateChanged(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowStateChanged

        reConfigCam();

    }//GEN-LAST:event_formWindowStateChanged

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        reConfigCam();

    }//GEN-LAST:event_formComponentResized

    private void miRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRunActionPerformed

        singleSelection(null);
        project = Core.loadProject("alpha");
        Common.selProject = project;

        if (project.isNew()) {

            project.setRootNode("mrsicc.htm");

            new Thread() {
                @Override
                public void run() {
                    FileCore fc = new FileCore(project, new File("D:/"));

                    fc.load(project.getRootNode(), new FileFilter() {

                        @Override
                        public boolean accept(File f) {
                            return f.isFile() && f.getName().toLowerCase().endsWith(".htm");
                        }
                    });

                    Core.save(project);
                    createNodes();
                }
            }.start();

        } else {
            createNodes();
        }

    }//GEN-LAST:event_miRunActionPerformed

    private void miToEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miToEndActionPerformed

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                tfFrom.setText(project.getRootNode());

                if (selectedElements[0] != null) {
                    tfTo.setText(selectedElements[0].getName());
                }

                pnFromTo.pack();
                pnFromTo.setLocationRelativeTo(ViewWindow.this);
                pnFromTo.setVisible(true);
            }
        });


    }//GEN-LAST:event_miToEndActionPerformed

    private void btnFromToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFromToActionPerformed

        //(tfFrom.getText().trim(), tfTo.getText().trim());
        delimiterPathDirect(tfFrom.getText().trim(), tfTo.getText().trim());


    }//GEN-LAST:event_btnFromToActionPerformed

    private int last;

    private void search() {
        Pattern p = Pattern.compile(tfSearch.getText());

        //System.out.println(stageEl);
        //cam.config(stageEl.getWidth(), stageEl.getHeight(), canvas.getWidth(), canvas.getHeight());
        //System.out.println(cam);
        for (; last < elements.size(); last++) {
            ElementModel el = elements.get(last);

            if (!(el instanceof NodeElement)) {
                continue;
            }

            Matcher m = p.matcher(el.getName());
            if (m.find()) {
                foccus(el);
                break;
            }
        }

        last++;
        if (last >= elements.size()) {
            last = 0;
        }
    }

    private void btnSearchNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSearchNextActionPerformed
        search();
    }//GEN-LAST:event_btnSearchNextActionPerformed

    private void miOpenSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miOpenSearchActionPerformed
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                pnsearch.pack();
                pnsearch.setLocationRelativeTo(ViewWindow.this);
                pnsearch.setVisible(true);
            }
        });


    }//GEN-LAST:event_miOpenSearchActionPerformed

    private void btnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartActionPerformed

        if (selectedElements[0] instanceof NodeElement) {
            project.setRootNode(selectedElements[0].getName());
            singleSelection(null);
            createNodes();
        }

    }//GEN-LAST:event_btnStartActionPerformed

    private void tfSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tfSearchKeyReleased
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            search();
        } else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            pnsearch.dispose();
        }

    }//GEN-LAST:event_tfSearchKeyReleased

    private void pnsearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_pnsearchKeyReleased

        if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            pnsearch.dispose();
        }

    }//GEN-LAST:event_pnsearchKeyReleased

    private final StringBuilder clip = new StringBuilder(100);


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnFromTo;
    private javax.swing.JButton btnSearchNext;
    private javax.swing.JButton btnStart;
    private javax.swing.JCheckBox cbCancelReverse;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblNodeChildren;
    private javax.swing.JLabel lblNodeParent;
    private javax.swing.JMenuBar mbMain;
    private javax.swing.JMenuItem miOpenSearch;
    private javax.swing.JMenuItem miRun;
    private javax.swing.JMenuItem miToEnd;
    private javax.swing.JPanel pnCanvas;
    private javax.swing.JFrame pnFromTo;
    private javax.swing.JPanel pnToolBar;
    private javax.swing.JFrame pnsearch;
    private javax.swing.JTextField tfFrom;
    private javax.swing.JTextField tfSearch;
    private javax.swing.JTextField tfTo;
    // End of variables declaration//GEN-END:variables

    private class MyPanel extends JPanel {

        private final Color BACKGROUND_COLOR = new Color(235, 235, 235);

        @Override
        public void paintComponent(Graphics gg) {
            //super.paintComponent(gg);

            Graphics2D g = (Graphics2D) gg;
            Common.graphics = g;

            g.setColor(BACKGROUND_COLOR);
            g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            //stageEl.setColor(btnCanvasColor.getBackground());
            if (zoom != 0) {
                float scale = 1 + zoom / 100f;
                g.scale(scale, scale);
            }

            cam.draw(g, stageEl);

            int sp = 5;
            g.setColor(selector.getColor());

            for (ElementModel el : selectedElements) {
                if (el == null) {
                    break;
                }

                g.drawRect(cam.fx(el.getPx() - sp), cam.fy(el.getPy() - sp), el.getWidth() + sp * 2, el.getHeight() + sp * 2);
            }

            if (startDrag != null) {
                int npx = mousePos.x - startDrag.x;
                int npy = mousePos.y - startDrag.y;
                for (ElementModel el : selectedElements) {
                    if (el == null) {
                        break;
                    }

                    g.drawRect(cam.fx(el.getPx() + npx), cam.fy(el.getPy() + npy), el.getWidth(), el.getHeight());
                }
            }

            if (Common.updateAll) {
                for (ElementModel el : elements) {
                    el.update();
                }

                Common.updateAll = false;

            } else {

                memoryTemp.reset();

                List<ElementModel> temp = elements;
                if (inLoad) {
                    temp = new ArrayList<>(elements);
                }

                for (ElementModel el : temp) {
                    if (el instanceof EdgeElement) {
                        EdgeElement ed = (EdgeElement) el;
                        if (isSelected(ed.getChild())) {
                            ed.setSelected(true);
                            ed.setColor(Color.PINK);
                            memoryTemp.registerElement(el);

                        } else if (isSelected(ed.getParent())) {
                            ed.setSelected(true);
                            ed.setColor(Color.MAGENTA);
                            memoryTemp.registerElement(el);

                        } else {
                            ed.setSelected(false);
                            ed.setColor(Color.LIGHT_GRAY);
                            cam.draw(g, el);
                        }
                    }
                }

                for (int i = 0; i < memoryTemp.getElementCount(); i++) {
                    ElementModel el = memoryTemp.getByElement(i);
                    cam.draw(g, el);
                }

                for (ElementModel el : temp) {
                    if (el instanceof NodeElement) {
                        //((NodeElement) el).setSelected(isSelected(el));
                        cam.draw(g, el);
                    }
                }

                selector.drawMe(g);
            }
        }
    }

    private JPanel createCanvas() {
        canvas = new MyPanel();

        canvas.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) {

                    mouseElement.setPxy(e.getX() + cam.getCpx(), e.getY() + cam.getCpy());

                    switch (mode) {
                        case SELECTOR:
                            selectElementOnStage(hasColision(mouseElement));
                            return;
                        case RELATION:
                            //createRelationship(hasColision(mouseElement));
                            return;
                        case HAND:
                            return;
                    }

                } else {
                    selector.setEnabled(false);
                    singleSelection(null);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {

                    mouseElement.setPxy(e.getX() + cam.getCpx(), e.getY() + cam.getCpy());

                    if (!isAltDown) {

                        if (selectedElements[0] == null || !isValidSelecion(mouseElement)) {
                            selector.setEnabled(true);
                            selector.setPxy(e.getX(), e.getY());
                        } else {
                            startDrag = e.getPoint();
                        }

                    } else /*if (EditTool.HAND == EditTool.SELECTOR)*/ {
                        //cam.move(e.getX() - mousePos.x, getY() - mousePos.y);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (selector.isEnabled()) {
                        selector.adjustInvertSelection();
                        selector.setPx(selector.getPx() + cam.getCpx());
                        selector.setPy(selector.getPy() + cam.getCpy());

                        singleSelection(null);

                        for (ElementModel el : elements) {

                            if (GraphicTool.g().bcollide(el, selector)) {
                                multiSelect(el);
                            }
                        }

                    } else if (startDrag != null) {
                        int npx = e.getPoint().x - startDrag.x;
                        int npy = e.getPoint().y - startDrag.y;
                        for (ElementModel el : selectedElements) {
                            if (el == null) {
                                break;
                            }
                            el.incPx(npx);
                            el.incPy(npy);
                        }

                        recalcStegeELSize();
                    }

                    selector.setEnabled(false);
                    startDrag = null;
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {
                mousePos.x = -1;
            }

        });

        canvas.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                //p = e.getPoint();

                if (selector.isVisible()) {
                    selector.setWidth(e.getX());
                    selector.setHeight(e.getY());
                }

                if (isAltDown) {
                    positionCam(e);
                }

                updateMousePosition(e);

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateMousePosition(e);
            }

            private void updateMousePosition(MouseEvent e) {
                mousePos = e.getPoint();
                //lblCanvasInfo.setText(String.format("%d %d Cam: %.0f %.0f ", mousePos.x, mousePos.y, mousePos.x + cam.getCpx(), mousePos.y + cam.getCpy()));
            }
        });

        canvas.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {

                if (isControlDown) {
                    cam.rollX(e.getWheelRotation() * 5);
                } else {
                    cam.rollY(e.getWheelRotation() * 5);
                }

            }
        });

        return canvas;
    }

    private void applyZoom(int val) {
        zoom = (short) (val != 0 ? zoom + val : val);
    }

    private void singleSelection(ElementModel el) {
        for (int i = 1; i < selectedElements.length; i++) {
            selectedElements[i] = null;
        }

        selectedElements[0] = el;

        lblNodeChildren.setText(null);
        lblNodeParent.setText(null);
        tStringBuilder.delete(0, tStringBuilder.length());

        if (el instanceof NodeElement) {
            NodeElement ne = (NodeElement) el;
            tStringBuilder.append(ne.getName());

            List<NodeElement> l = ne.getChild();
            for (NodeElement n : l) {
                tStringBuilder.append(" > ").append(n.getName());
            }

            lblNodeChildren.setText(tStringBuilder.toString());
            tStringBuilder.delete(0, tStringBuilder.length());

            NodeElement p = ne.getParent();
            while (p != null) {
                tStringBuilder.append(p.getName()).append(" > ");
                p = p.getParent();
            }

            tStringBuilder.append(ne.getName());
            lblNodeParent.setText(tStringBuilder.toString());
        }

    }

    private void multiSelect(ElementModel el) {
        for (int i = 0; i < selectedElements.length; i++) {
            if (selectedElements[i] != null) {
                continue;
            }
            selectedElements[i] = el;
            break;
        }
    }

    private boolean isValidSelecion(ElementModel element) {
        for (ElementModel el : selectedElements) {
            if (el == null) {
                return false;
            }

            if (GraphicTool.g().bcollide(el, element)) {
                return true;
            }
        }

        return false;
    }

    private void selectElementOnStage(ElementModel elementModel) {
        singleSelection(elementModel);
    }

    private ElementModel hasColision(ElementModel element) {
        ElementModel e = selectedElements[0];

        for (ElementModel el : elements) {
            if (EditTool.SELECTOR == mode && selectedElements[0] == el) {
                continue;
            }

            if (GraphicTool.g().bcollide(el, element)) {
                e = el;
                break;
            }
        }

        return e;
    }

    private void positionCam(ElementModel el) {
        cam.move(el.getPx() - canvas.getWidth() / 2, el.getPy() - canvas.getHeight() / 2);
    }

    private void positionCam(int px, int py) {
        cam.move(px - 15, py - 15);
    }

    private void positionCam(MouseEvent e) {
        cam.rollX(mousePos.x - e.getX());
        cam.rollY(mousePos.y - e.getY());
    }

    private boolean addTable(ElementModel tb) {

        if (elements.contains(tb)) {
            return false;
        }

        elements.add(tb);
        return true;
    }

    private int pos;

    public void init(String name) {
        singleSelection(null);
        setTitle(String.format("Name: %s", name));

        elements = null;

        recalcStegeELSize();
        cam.center(stageEl);
    }

    private void recalcStegeELSize() {
        Point xy = new Point();
        Dimension wh = new Dimension(800 - 15, 600 - 15);

        for (ElementModel te : elements) {
            xy.x = xy.x > te.getPx() ? te.getPx() : xy.x;
            xy.y = xy.y > te.getPy() ? te.getPy() : xy.y;

            wh.width = wh.width < te.getAllWidth() ? te.getAllWidth() : wh.width;
            wh.height = wh.height < te.getAllHeight() ? te.getAllHeight() : wh.height;
        }

        stageEl.setPxy(xy.x, xy.y);
        stageEl.setSize(wh.width + 15, wh.height + 15);
        reConfigCam();
    }

    private void removeSelNodes() {
        if (selectedElements[0] != null) {
            if (JOptionPane.showConfirmDialog(this, "Discart node " + selectedElements[0].getName() + " ?", "Do you want to discart this node?", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {

                for (ElementModel t : selectedElements) {
                    if (t == null) {
                        break;
                    }
                    if (t instanceof NodeElement) {
                        project.getTree().remove(((NodeElement) t).getName());
                    }

                    /*if (t instanceof NodeElement) {
                     elements.remove(t);

                     for (ElementModel e : elements) {
                     if (e instanceof NodeElement) {
                     ((NodeElement) e).remove((NodeElement) t);

                     } else if (e instanceof EdgeElement) {
                     ((EdgeElement) e).remove((NodeElement) t);
                     }
                     }
                     }*/
                }

                singleSelection(null);
                createNodes();
            }
        }
    }

}
