/*
 *     CharGer - Conceptual Graph Editor
 *     Copyright reserved 1998-2016 by Harry S. Delugach

 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cgfca;

import charger.EditFrame;
import charger.Global;
import charger.obj.Graph;
import chargerlib.ManagedWindow;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * The user interface for performing CG-FCA tasks.
 * @author hsd
 */
public class CG_FCA_Window extends JFrame implements ManagedWindow {

    PathFinder pf = null;

    /**
     * Create a new CG FCA Window.
     */
    public CG_FCA_Window() {
        initComponents();
        getContentPane().setBackground( this.getBackground());

        addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                CG_FCA.shutdownCGFCA();
            }

            public void windowActivated( WindowEvent e ) {
                SwingUtilities.invokeLater(() -> refresh());
            }
        } );

        // Check if there are any open graphs
        refresh();

        // If no graphs are open, prompt user to open one
        if (editFrameList.getItemCount() == 0) {
            int result = javax.swing.JOptionPane.showConfirmDialog(
                    this,
                    "No graphs are currently open.\n\nWould you like to open a graph?",
                    "No Graphs Open",
                    javax.swing.JOptionPane.YES_NO_OPTION
            );

            if (result == javax.swing.JOptionPane.YES_OPTION) {
                // Open the file dialog
                String filename = charger.Global.openGraphInNewFrame(null);
                if (filename != null) {
                    // Wait for EditFrame to be created, then refresh
                    SwingUtilities.invokeLater(() -> {
                        try {
                            Thread.sleep(500);
                            refresh();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
        }
    }

    @Override
    public String getMenuItemLabel() {
        return "CG_CXT Window";
    }

    @Override
    public String getFilename() {
        return null;
    }

    @Override
    public JMenu getWindowMenu() {
        return windowMenu;
    }

    public void refresh() {
        editFrameList.removeAllItems();

        int count = 0;

        // Try Global.editFrameList first
        if (Global.editFrameList != null && Global.editFrameList.size() > 0) {
            System.out.println("DEBUG: Using Global.editFrameList with " + Global.editFrameList.size() + " frames");

            for (charger.EditFrame ef : Global.editFrameList.values()) {
                System.out.println("DEBUG: Adding EditFrame from Global: " + ef.getTitle());
                editFrameList.addItem(ef);
                count++;
            }
        } else {
            // Fallback to WindowManager
            System.out.println("DEBUG: Global.editFrameList is empty, trying WindowManager");

            java.util.ArrayList<chargerlib.ManagedWindow> allWindows = chargerlib.WindowManager.getManagedWindows();

            for (chargerlib.ManagedWindow mw : allWindows) {
                if (mw instanceof charger.EditFrame) {
                    charger.EditFrame ef = (charger.EditFrame) mw;
                    System.out.println("DEBUG: Adding EditFrame from WindowManager: " + ef.getTitle());
                    editFrameList.addItem(ef);
                    count++;
                }
            }
        }

        if (count == 0) {
            System.out.println("DEBUG: No edit frames found anywhere!");
            graphSelectedNameLabel.setText("No graphs open or selected");
            cxtText.setText("No graphs open or selected");
            graphReport.setText("No graphs open or selected");
            this.editFrameList.setEnabled(true);
            return;
        }

        System.out.println("DEBUG: Found " + count + " edit frames");

        if (editFrameList.getItemCount() > 0) {
            editFrameList.setSelectedIndex(0);
        }

        this.editFrameList.setEnabled(true);
    }

    /**
     * Clear out the contents of the window, including the cxt text and the report.
     * Also clear out the path finder.
     */
    public void clearContent() {
        pf = null;
        this.graphSelectedNameLabel.setText( "No graph selected");
        this.cxtText.setText( "");
        this.graphReport.setText( "");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        editFrameList = new javax.swing.JComboBox<>();
        graphSelectedNameLabel = new javax.swing.JLabel();
        refreshGraphList = new javax.swing.JButton();
        openGraphButton = new javax.swing.JButton();
        graphReportScroller = new javax.swing.JScrollPane();
        cxtText = new javax.swing.JTextArea();
        exportButton = new javax.swing.JButton();
        graphReportScroller1 = new javax.swing.JScrollPane();
        graphReport = new javax.swing.JTextArea();
        enableCorefs = new javax.swing.JCheckBox();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        windowMenu = new javax.swing.JMenu();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("CG to FCA Features");
        setBackground(new java.awt.Color(204, 204, 255));
        setBounds(new java.awt.Rectangle(300, 300, 1024, 700));
        setLocation(new java.awt.Point(300, 300));
        setMinimumSize(new java.awt.Dimension(875, 700));
        setPreferredSize(new java.awt.Dimension(1024, 650));
        setSize(new java.awt.Dimension(1024, 700));
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Choose a graph to check");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 10, -1, 20));

        editFrameList.setMinimumSize(new java.awt.Dimension(250, 27));
        editFrameList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editFrameListActionPerformed(evt);
            }
        });
        getContentPane().add(editFrameList, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 40, -1, -1));

        graphSelectedNameLabel.setBackground(new java.awt.Color(255, 255, 255));
        graphSelectedNameLabel.setFont(new java.awt.Font("Lucida Grande", 1, 14));
        graphSelectedNameLabel.setText("..graph name goes here...");
        graphSelectedNameLabel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        graphSelectedNameLabel.setOpaque(true);
        getContentPane().add(graphSelectedNameLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 80, 900, 30));

        // Refresh button at top
        refreshGraphList.setText("Refresh");
        refreshGraphList.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refresh();
            }
        });
        getContentPane().add(refreshGraphList, new org.netbeans.lib.awtextra.AbsoluteConstraints(380, 10, -1, -1));

        // Open Graph button at top
        openGraphButton.setText("Open Graph");
        openGraphButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openGraphButtonActionPerformed(evt);
            }
        });
        getContentPane().add(openGraphButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 10, -1, -1));

        // Left side: CXT Text Area
        cxtText.setColumns(20);
        cxtText.setRows(5);
        cxtText.setWrapStyleWord(true);
        graphReportScroller.setViewportView(cxtText);
        getContentPane().add(graphReportScroller, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 120, 350, 490));

        // Export button at top
        exportButton.setText("Export .cxt and .txt");
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        getContentPane().add(exportButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(590, 10, -1, -1));

        // Right side: Graph Report Text Area
        graphReport.setColumns(20);
        graphReport.setRows(5);
        graphReport.setWrapStyleWord(true);
        graphReportScroller1.setViewportView(graphReport);
        getContentPane().add(graphReportScroller1, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 120, 530, 490));

        // Enable Coreferents checkbox at top
        enableCorefs.setSelected(true);
        enableCorefs.setText("Enable Co-referents");
        enableCorefs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableCorefsActionPerformed(evt);
            }
        });
        getContentPane().add(enableCorefs, new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 10, -1, -1));

        fileMenu.setText("File");
        menuBar.add(fileMenu);

        jMenu2.setText("Edit");
        menuBar.add(jMenu2);

        windowMenu.setText("Window");
        menuBar.add(windowMenu);

        setJMenuBar(menuBar);

        pack();
    }

    private void editFrameListActionPerformed(java.awt.event.ActionEvent evt) {
        // Get the selected EditFrame directly from the dropdown
        EditFrame selectedFrame = (EditFrame) editFrameList.getSelectedItem();

        if (selectedFrame == null) {
            graphSelectedNameLabel.setText("....No graph selected...");
            cxtText.setText("");
            graphReport.setText("");
            return;
        }

        Graph graphSelected = selectedFrame.TheGraph;
        String frameNameSelected = selectedFrame.getTitle();

        graphSelectedNameLabel.setText(frameNameSelected);
        cxtText.setText(frameNameSelected + System.getProperty("line.separator") + graphSelected.getBriefSummary());

        pf = cgfca.CG_FCA.generateCGFCA(graphSelected);
        pf.setFilename(frameNameSelected);

        this.cxtText.setText(pf.getCxtContent());
        this.graphReport.setText("Report on: " + frameNameSelected + System.getProperty("line.separator")
                + System.getProperty("line.separator") + pf.getReportContent());
    }

    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (pf == null) {
            JOptionPane.showMessageDialog(this, "A graph must be chosen in order to export its information.");
            return;
        }
        try {
            String basename = pf.filename.substring(0, pf.filename.lastIndexOf(".cgx"));

            FileWriter cxtWriter = new FileWriter(basename + ".cxt");
            cxtWriter.write(pf.getCxtContent());
            cxtWriter.close();

            FileWriter reportWriter = new FileWriter(basename + ".txt");
            reportWriter.write(pf.getReportContent());
            reportWriter.close();

            JOptionPane.showMessageDialog(this, "Context saved to: \n\"" + basename + ".cxt" + "\"\n\n"
                    + "Report saved to: \n\"" + basename + ".txt" + "\"\n");

        } catch (IOException ex) {
            Logger.getLogger(CG_FCA_Window.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void enableCorefsActionPerformed(java.awt.event.ActionEvent evt) {
        CG_FCA.enableCoreferents = this.enableCorefs.isSelected();
        JOptionPane.showMessageDialog(this, "Enable coreferents is " + CG_FCA.enableCoreferents);
    }

    private void openGraphButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // This calls the same method HubFrame uses to open graphs
        String filename = charger.Global.openGraphInNewFrame(null);
        if (filename != null) {
            // Give it a moment to create the EditFrame, then refresh
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(500);
                    refresh();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    public javax.swing.JTextArea cxtText;
    private javax.swing.JComboBox<EditFrame> editFrameList;
    public javax.swing.JCheckBox enableCorefs;
    private javax.swing.JButton exportButton;
    private javax.swing.JMenu fileMenu;
    public javax.swing.JTextArea graphReport;
    private javax.swing.JScrollPane graphReportScroller;
    private javax.swing.JScrollPane graphReportScroller1;
    private javax.swing.JLabel graphSelectedNameLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton refreshGraphList;
    private javax.swing.JButton openGraphButton;
    private javax.swing.JMenu windowMenu;
    // End of variables declaration//GEN-END:variables
}