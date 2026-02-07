package charger;

import charger.EditingChangeState.EditChange;
import charger.cgx.CGXGenerator;
import charger.cgx.CGXParser;
import charger.exception.*;
import charger.layout.SpringGraphLayout;
import charger.obj.Actor;
import charger.obj.Concept;
import charger.obj.DeepIterator;
import charger.obj.GEdge;
import charger.obj.GNode;
import charger.obj.Graph;
import charger.obj.GraphObject;
import charger.obj.Relation;
import charger.obj.RelationLabel;
import charger.obj.ShallowIterator;
import charger.obj.TypeLabel;
import charger.util.CGUtil;
import chargerlib.CDateTime;
import chargerlib.FileFormat;
import chargerlib.FontChooser;
import chargerlib.General;
import chargerlib.GenericTextFrame;
import chargerlib.Tag;
import chargerlib.undo.UndoStateManager;
import chargerlib.undo.Undoable;
import chargerlib.undo.UndoableState;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;


/*
 $Header$
 */
/*
 CharGer - Conceptual Graph Editor
 Copyright 1998-2020 by Harry S. Delugach

 This package is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as
 published by the Free Software Foundation; either version 2.1 of the
 License, or (at your option) any later version. This package is
 distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 details. You should have received a copy of the GNU Lesser General Public
 License along with this package; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
/**
 * Serves as the ItemListener and ActionListener for buttons and menus in the
 * edit frame. Contains many of the actions for an EditFrame, including handlers
 * for the tool mode and action buttons. communicates with its container
 * EditFrame by the ef variable.
 *
 * @author Harry S. Delugach ( delugach@uah.edu ) Copyright (c) 1998-2020 by
 * Harry S. Delugach.
 * @see EditFrame
 * @see CanvasPanel
 */
public class EditManager implements ItemListener, ActionListener, ClipboardOwner, Undoable {

    /**
     * This class does a lot of communicating with the edit frame that owns it.
     */
    public EditFrame ef;
    /**
     * Maximum number of actions that are undo-able -- currently set to 10
     */
    public static int maxUndo =
            Integer.parseInt( Global.Prefs.getProperty( "defaultMaxUndo", "10" ) );
    public boolean useNewUndoRedo = true;
    /**
     * Each past copy of the graph, in text form, is added to the list, up to
     * its max
     */
    public ArrayList<String> undoList = new ArrayList<String>( maxUndo );
    /**
     * Whenever an undo is performed, the current ("future") graph, in text
     * form, is added to the list, up to its max
     */
    public ArrayList<String> redoList = new ArrayList<String>( maxUndo );
//    charger.xml.CGXParser parser = new charger.xml.CGXParser();
    // Stuff for undo/redo
    private String holdGraph = null;		// temp string version, saved in case we need a backup
    public UndoStateManager urMgr = null;
    private Iterator<GraphObject> findIterator = null;
    private String findString = null;
    private boolean anyFound = false;		// during a find, were any ever found?
    // NEED to make this a global parameter
    public boolean showGloss = true;		// whether to show a gloss entry when node is clicked

//    public boolean wrapLabels = GraphObject.defaultWrapLabels;
//    public int wrapColumns = GraphObject.defaultWrapColumns;
    /**
     * Only used by the editframe to instantiate its own manager.
     *
     * @param outerFrame the EditFrame that owns this manager.
     */
    public EditManager( EditFrame outerFrame ) {
        // Link to the outer frame
        ef = outerFrame;
        urMgr = new UndoStateManager( this, maxUndo );
//        urMgr = new EditStateMgr( ef, maxUndo );


        // set up rest of color menus
        ef.changeColorMenu.add( ef.ChangeTextItem );
        ef.ChangeTextItem.addActionListener( this );
        ef.changeColorMenu.add( ef.ChangeFillItem );
        ef.ChangeFillItem.addActionListener( this );
        ef.changeColorMenu.add( ef.ChangeColorDefaultItem );
        ef.ChangeColorDefaultItem.addActionListener( this );
        ef.changeColorMenu.add( ef.ChangeColorFactoryItem );
        ef.ChangeColorFactoryItem.addActionListener( this );

        ef.changeColorMenu.add( ef.ChangeColorBlackAndWhiteItem );
        ef.ChangeColorBlackAndWhiteItem.addActionListener( this );
        ef.changeColorMenu.add( ef.ChangeColorGrayscaleItem );
        ef.ChangeColorGrayscaleItem.addActionListener( this );

        makeMenus();
    }

    private void makeMenus() {
        // Here's where the EditFrame's menus are arranged and initialized.
        makeNewMenuItem( ef.fileMenu, Global.strs( "NewWindowLabel" ), KeyEvent.VK_N );
        makeNewMenuItem( ef.fileMenu, Global.strs( "OpenLabel" ), KeyEvent.VK_O );
//        if ( !Global.OfficialRelease ) {
            makeNewMenuItem( ef.fileMenu, Global.strs( "ImportCGIFLabel" ), 0 );
//        }

        makeNewMenuItem( ef.fileMenu, Global.strs( "CloseLabel" ), KeyEvent.VK_W );
        makeNewMenuItem( ef.fileMenu, Global.strs( "SaveLabel" ), KeyEvent.VK_S );
        makeNewMenuItem( ef.fileMenu, Global.strs( "SaveAsLabel" ), 0 );
        makeNewMenuItem( ef.fileMenu, Global.strs( "ExportCGIFLabel" ), 0 );

        JMenu exportMenu = new JMenu( Global.strs( "ExportAsImageLabel" ) );

        if ( IOManager.imageFormats.size() == 0 ) {
            IOManager.initializeImageFormatList();
        }

        for ( String s : IOManager.imageFormats ) {
            makeNewMenuItem( exportMenu, s.toUpperCase(), 0 );
        }

        exportMenu.addSeparator();
        makeNewMenuItem( exportMenu, "PDF", 0 );
        makeNewMenuItem( exportMenu, "EPS", 0 );
        makeNewMenuItem( exportMenu, "SVG", 0 );

        ef.fileMenu.addSeparator();
        ef.fileMenu.add( exportMenu );

        ef.fileMenu.addSeparator();

        makeNewMenuItem( ef.fileMenu, Global.strs( "PageSetupLabel" ), 0 );
        makeNewMenuItem( ef.fileMenu, Global.strs( "PrintLabel" ), KeyEvent.VK_P );
        ef.fileMenu.addSeparator();
//        makeNewMenuItem( ef.fileMenu, Global.strs( "DisplayAsEnglishLabel" ), 0 );
        makeNewMenuItem( ef.fileMenu, Global.strs( "DisplayAsXMLLabel" ), 0 );
        makeNewMenuItem( ef.fileMenu, Global.strs( "DisplayAsCGIFLabel" ), 0 );
        makeNewMenuItem( ef.fileMenu, Global.strs( "DisplayMetricsLabel" ), KeyEvent.VK_M );
        ef.fileMenu.addSeparator();
        makeNewMenuItem( ef.fileMenu, Global.strs( "QuitLabel" ), KeyEvent.VK_Q );  

        ef.editMenu.add( ef.UndoItem );
        
        ef.UndoItem.addActionListener( this );
        ef.editMenu.add( ef.RedoItem );
        ef.RedoItem.addActionListener( this );

        ef.viewMenu.add( ef.ZoomInItem );
        ef.ZoomInItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_EQUALS, Global.AcceleratorKey ) );
        ef.ZoomInItem.addActionListener( new java.awt.event.ActionListener() {
            public void actionPerformed( java.awt.event.ActionEvent e ) {
                performZoomAction( "in" );

            }
        } );

        ef.viewMenu.add( ef.ZoomOutItem );
        ef.ZoomOutItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_MINUS, Global.AcceleratorKey ) );
        ef.ZoomOutItem.addActionListener( new java.awt.event.ActionListener() {
            public void actionPerformed( java.awt.event.ActionEvent e ) {
                performZoomAction( "out" );
            }
        } );

        ef.viewMenu.add( ef.ActualSizeItem );
        ef.ActualSizeItem.addActionListener( new java.awt.event.ActionListener() {
            public void actionPerformed( java.awt.event.ActionEvent e ) {
                performZoomAction( "actual" );
            }
        } );

        
        ef.viewMenu.addSeparator();
        ef.viewMenu.add( ef.CurrentSizeItem );
        ef.CurrentSizeItem.setEnabled( false );
        setViewMenuItems( ef.canvasScaleFactor );

        ef.viewMenu.addSeparator();
        ef.FindMenuItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_F, Global.AcceleratorKey ) );
        ef.viewMenu.add( ef.FindMenuItem );
        ef.FindMenuItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                performFindMenuAction();
            }
        } );

        ef.FindAgainMenuItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_G, Global.AcceleratorKey ) );
        ef.viewMenu.add( ef.FindAgainMenuItem );
        // disabled if there isn't a find string and enumeration already set up
        ef.FindAgainMenuItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                performFindAgain( findString );
            }
        } );

        ef.viewMenu.add( ef.ShowHistoryMenuItem );
        ef.ShowHistoryMenuItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                GraphObject go = ef.EFSelectedObjects.get( 0 );
//                ef.displayOneLiner( go.getHistory().toString() );
                JOptionPane.showMessageDialog( ef, go.getHistory().toString() );
            }
        } );

        ef.viewMenu.add( ef.ShowGlossMenuItem );
        ef.ShowGlossMenuItem.setState( showGloss );
        ef.ShowGlossMenuItem.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent e ) {
                showGloss = !showGloss;
                ef.ShowGlossMenuItem.setState( showGloss );
            }
        } );


        ef.editMenu.addSeparator();
        makeNewMenuItem( ef.editMenu, Global.strs( "CutLabel" ), KeyEvent.VK_X );
        makeNewMenuItem( ef.editMenu, Global.strs( "CopyLabel" ), KeyEvent.VK_C );
        makeNewMenuItem( ef.editMenu, Global.strs( "PasteLabel" ), KeyEvent.VK_V );
        makeNewMenuItem( ef.editMenu, Global.strs( "ClearLabel" ), 0 ); //KeyEvent.VK_DELETE );

        makeNewMenuItem( ef.editMenu, Global.strs( "DuplicateLabel" ), KeyEvent.VK_D );
        makeNewMenuItem( ef.editMenu, Global.strs( "SelectAllLabel" ), KeyEvent.VK_A );
        ef.editMenu.addSeparator();
        makeNewMenuItem( ef.editMenu, Global.strs( "ChangeFontLabel" ), 0 );
        ef.editMenu.addSeparator();
        ef.editMenu.add( ef.changeColorMenu );
        ef.editMenu.addSeparator();
        makeNewMenuItem( ef.editMenu, Global.strs( "MinimizeLabel" ), 0 );
        makeNewMenuItem( ef.editMenu, Global.strs( "AlignVLabel" ), 0 );
        makeNewMenuItem( ef.editMenu, Global.strs( "AlignHLabel" ), 0 );
        makeNewMenuItem( ef.editMenu, "Auto Layout", KeyEvent.VK_L );
        ef.editMenu.addSeparator();
        if ( !Global.OfficialRelease ) {
            // === HERE IS WHERE TESTING ROUTINES CAN BE INVOKED
            // For TESTING purposes, menu items can be placed in the testing menu
//            makeNewMenuItem( ef.testingItemsMenu, Global.strs( "CopyImageLabel"), 0 );
            makeNewMenuItem( ef.testingItemsMenu, "Show Hierarchies", 0 );
            makeNewMenuItem( ef.testingItemsMenu, "Read from jar file", 0 );
//            makeNewMenuItem( ef.testingItemsMenu, Global.strs( "ExportTestXMLLabel" ), 0 );
//            makeNewMenuItem( ef.testingItemsMenu, "moveGraph by 150, 100", 0 );
            makeNewMenuItem( ef.testingItemsMenu, "Update Internal IDs", KeyEvent.VK_BACK_SLASH );
            ef.editMenu.add( ef.testingItemsMenu );
            ef.editMenu.addSeparator();
        }
        //ef.editMenu.add(ef.GraphModalityMenu);
        makeNewMenuItem( ef.editMenu, Global.strs( "PreferencesLabel" ), KeyEvent.VK_COMMA );

        // operateMenu is initialized by the OperManager
    }

    /**
     * Convenience method to set up menu items for some of the various menus.
     * Sets the action listener for the menu item to this edit manager, no
     * matter what.
     *
     * @param m the menu to which the item is to be added.
     * @param label the label for the menu item
     * @param keyCode the accelerator key letter (as in java.awt.event.KeyEvent) for
     * this menu item; 0 if none.
     */
    public void makeNewMenuItem( JMenu m, String label, int keyCode ) {
        // This is one source of memory leaks. Do not create a new menu item each time.
        JMenuItem item = new JMenuItem( new String(label) );
        if ( keyCode != 0 ) {
            item.setAccelerator( KeyStroke.getKeyStroke( keyCode, Global.AcceleratorKey ) );
        }
        m.add( item );
        item.addActionListener( this );
    }
    

    /**
     * Determines for each menu item whether to be enabled or disabled ("gray'ed
     * out" ) Many items are disabled if there is no selection; other items are
     * disabled if the clipboard is empty or nothing's changed, etc.
     *
     * @see EditFrame#somethingHasBeenSelected
     */
    public void setMenuItems() {
        // NEED TO use Toolkit.getDefaultToolkit().getSystemClipboard
        //Global.info("set menu items, somethingHasBeenSelected = " + somethingHasBeenSelected );
        ef.editingToolbar.setAvailableCommands( ef.somethingHasBeenSelected );
        if ( !ef.somethingHasBeenSelected ) {
            ef.formatToolbar.setForNoSelection();
        }
        
        setEditMenuItems();
        setViewMenuItems( ef.canvasScaleFactor);
    }
        
      public void  setEditMenuItems()  {

        for ( int num = 0; num < ef.editMenu.getItemCount(); num++ ) {
            //Global.info( "checking editmenu item " + num + " of " + ef.editMenu.getItemCount() + " items." );
            JMenuItem mi = ef.editMenu.getItem( num );
            String item = null;
            if ( mi != null ) {
                item = mi.getText();
            } else {
                item = "Separator";
                //Global.info( "item " + num + " string is " + item );
            }
            if ( item.equals( Global.strs( "ChangeFontLabel" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
            } else if ( item.equals( Global.strs( "ChangeColorLabel" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
            } else if ( item.equals( Global.strs( "MinimizeLabel" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
            } else if ( item.equals( "Cut" ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
                if ( ef.cp.nodeEditingDialog.isVisible() ) {
                    mi.setEnabled( true );		// user may select some label text while we're not looking!
                    //mi.setEnabled( cp.textLabelEditField.getSelectedText() != null );

                }
            } else if ( item.equals( Global.strs( "CopyLabel" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
                if ( ef.cp.nodeEditingDialog.isVisible() ) {
                    mi.setEnabled( true );		// user may select some label text while we're not looking!
                    //mi.setEnabled( cp.textLabelEditField.getSelectedText() != null );

                }
            } else if ( item.equals( Global.strs( "PasteLabel" ) ) ) {
                if ( Global.cgClipboard != null && Global.cgClipboard.getContents( this ) != null ) {
                    StringSelection clipcontents =
                            (StringSelection)Global.cgClipboard.getContents( this );
                    mi.setEnabled( clipcontents != null );
                } //else if ( Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this) != null )
                //{
                //mi.setEnabled( cp.textLabelEditField.isVisible() );
                //}
                else {
                    mi.setEnabled( ef.cp.nodeEditingDialog.isVisible() );
                }
            } else if ( item.equals( Global.strs( "DuplicateLabel" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
                // only works if a graph object has been selected
            } else if ( item.equals( Global.strs( "ClearLabel" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
            } else if ( item.equals( Global.strs( "UndoLabel" ) ) ) {
//                if ( !useNewUndoRedo ) {
                    mi.setEnabled( undoAvailable() );
//                }
            } else if ( item.equals( Global.strs( "RedoLabel" ) ) ) {
//                if ( !useNewUndoRedo ) {
                    mi.setEnabled( redoAvailable() );
//                }
            }
        }
    }
      

          /**
     * Sets the view menu's scale indicator, and also sets whether Find Again
     * should be enabled
     *
     * @param scale varies from 0 to 3.0 shows actual scaling fraction
     */
    public void setViewMenuItems( double scale ) {
        NumberFormat nformat = NumberFormat.getNumberInstance();
        nformat.setMaximumFractionDigits( 0 );

        //ef.viewMenu.remove( ef.CurrentSizeItem );
        ef.CurrentSizeItem.setText( "Current: " + nformat.format( 100.0 * scale ) + "%" );
        //ef.viewMenu.add( ef.CurrentSizeItem );

        ef.FindAgainMenuItem.setEnabled( findString != null );

        for ( int num = 0; num < ef.viewMenu.getItemCount(); num++ ) {
            //Global.info( "checking editmenu item " + num + " of " + ef.editMenu.getItemCount() + " items." );
            JMenuItem mi = ef.viewMenu.getItem( num );
            String item = null;
            if ( mi != null ) {
                item = mi.getText();
            } else {
                item = "Separator";
                //Global.info( "item " + num + " string is " + item );
            }
            if ( item.equals( Global.strs( "ShowHistory" ) ) ) {
                mi.setEnabled( ef.somethingHasBeenSelected );
            }
        }

    }

    /**
     * @return whether there is a previous state to which we can un-do.
     */
    public boolean undoAvailable() {
        //return ! (undoStack.empty() );
        if ( useNewUndoRedo ) {
            return urMgr.undoAvailable();
        } else {
            return !( undoList.isEmpty() );
        }
    }

    /**
     * @return whether there is a next state which can be restored.
     */
    public boolean redoAvailable() {
        //return ! (redoStack.empty() );
        if ( useNewUndoRedo ) {
            return urMgr.redoAvailable();
        } else {
            return !( redoList.isEmpty() );
        }
    }

    /**
     * Undo the last editing state. Pushes the current copy of the graph
     * (current editing state) onto the redo stack and pops the last copy of the
     * graph from the undoStack, to become the current editing state.
     *
     * @param currentOne The current editing state, ready to be the next
     * restored via "redo". It is in the form of a cgx xml string representing
     * the ENTIRE graph to be restored.
     * @return The previous editing state. * It is in the form of a cgx xml
     * string representing the ENTIRE graph to be restored.
     */
    public String doUndo( String currentOne ) {
        String s = null;

        /*try {
         s = undoStack.pop();
         redoStack.push( currentOne );
         } catch ( EmptyStackException e ) { s = null; }
         */
        if ( undoList.size() == 0 ) {
            s = null;
        } else {
            try {
                s = undoList.get( undoList.size() - 1 );
                undoList.remove( undoList.size() - 1 );
                redoList.add( currentOne );
            } catch ( ArrayIndexOutOfBoundsException e1 ) {
                Global.error( "doUndo had a problem." );
            }
        }

        undoRedoSizes( "doUndo" );
        //Global.info( " === UNDO returns " + s );
        return s;
    }

    /**
     * Announce the number of items on the undo and redo stacks
     *
     * @param label an identifier usually telling where the method was called
     * from
     */
    private void undoRedoSizes( String label ) {
        //Global.info( undoStack.size() + " <== undo . redo ==> " + redoStack.size() + "  at " + label  );
        Global.info( "OLD:   " + undoList.size() + " <== undo . redo ==> " + redoList.size() + "  at " + label );
    }

    /**
     * Restore an editing state, moving forward with respect to the user's
     * actions. Pushes the current copy of the graph (current editing state)
     * onto the undo stack and pops the next "future" copy of the graph from the
     * redo stack, to become the current editing state.
     *
     * @param currentOne The current editing state, ready to be the next
     * restored via "undo". * It is in the form of a cgx xml string representing
     * the ENTIRE graph to be restored.
     *
     * @return The next ("future") editing state. * It is in the form of a cgx
     * xml string representing the ENTIRE graph to be restored.
     */
    public String doRedo( String currentOne ) {
        String s = null;
        /*try {
         s = (String)redoStack.pop();
         undoStack.push( currentOne );
         } catch ( EmptyStackException e ) { s = null; }
         */
        if ( redoList.size() == 0 ) {
            s = null;
        } else {
            try {
                s = redoList.get( redoList.size() - 1 );
                redoList.remove( redoList.size() - 1 );
                undoList.add( currentOne );
            } catch ( ArrayIndexOutOfBoundsException e1 ) {
                Global.error( "doRedo had a problem." );
            }
        }

        //Global.info( " === REDO returns " + s );
        undoRedoSizes( "doRedo" );
        return s;

    }

    /**
     * Track a regular editing action (other than undo or redo). Wipes out the
     * "redo" chain of events -- after this, all the previous actions' future
     * states have been deleted. In science fiction terms, we have thus altered
     * the future timeline and any previous future timelines can no longer
     * occur.
     *
     * @param currentOne The current editing state, to become the most recent
     * state. * It is in the form of a cgx xml string representing the ENTIRE
     * graph to be restored.
     */
    public void doDo( String currentOne ) {
        if ( currentOne == null ) {
            return;
            //undoStack.push( currentOne );
        }
        if ( undoList.size() >= maxUndo ) {
            undoList.remove( 0 );      // discard previous "undo" choices after max is reached.

        }
        undoList.add( currentOne );
        //while ( ! redoStack.empty() ) { String dummy = (String)redoStack.pop();	}
        redoList.clear();
        //Global.info( " === DO kept " + currentOne );
        undoRedoSizes( "doDo" );
    }

    /**
     * Initialize the undo/redo sequences, so that there are no past or future
     * states.
     */
    public void resetUndo() {
        //while ( ! undoStack.empty() ) { String dummy = (String)undoStack.pop(); }
        //while ( ! redoStack.empty() ) { String dummy = (String)redoStack.pop(); }
        undoList.clear();
        redoList.clear();
    }

    protected void finalize() throws Throwable {
        try {
            resetUndo();
            Global.info( "finalizing edit manager of frame " + ef.editFrameNum );

            super.finalize();
        } catch ( Throwable t ) {
            throw t;
        } finally {
            super.finalize();
        }
    }

    /**
     * Handles the check box group for the various tool modes, and the modality
     * choice menu.
     *
     * @param ie the item event that changed
     */
    public void itemStateChanged( ItemEvent ie ) {
        Object source = ie.getSource();
        ef.clearStatus();
        if ( source instanceof JRadioButton ) {
            // Global.info( "at item state changed of EditManager.");
            // all checkboxes go here
            // if user has checked another mode box, then cancel selection
            ef.resetSelection();
            ef.cp.reset();
//            ef.setToolMode( getMode( (JRadioButton)source ) );
            // Global.info( "tool mode is now " + getMode( (JRadioButton) source ) );
        }
    }

    /*
     Actions should be designed as plug-ins.
     Each plug-in would need the following parameters:
     String	actionname
     boolean	needSelection		whether the action requires a selection for its operation
     boolean	requireMovement		whether to require cursor movement for its operation
     boolean	allowMovement		whether to allow cursor movement for its operation
     Frame		canvas			where the graph objects are
     Graph		outer			outermost graph on which to operate
     ArrayList	selection			list of graph objects in the current selection (null if not used)

     */
    /**
     * Here's where the action is (performed)! the EditManager is generally the
     * action listener for the EditFrame, except for the Window Menu whose
     * ActionListener is the master hub frame, and the Canvas Panel which
     * handles text editing actions. This division was made for arbitrary
     * reasons and probably should be changed.
     */
    public void actionPerformed( ActionEvent e ) {
        // handle all button events here
        //Global.info( "at action performed: " + e.toString() );
        Object source = e.getSource();

        JMenuItem sourceMenuItem = null;

        if ( source instanceof JMenuItem ) {
            sourceMenuItem = (JMenuItem)source;
        }

        ef.clearStatus();

        //Global.info( "event e " + e );
        if ( e.getActionCommand().equals( Global.strs( "NewWindowLabel" ) ) ) {
            EditFrame ef = new EditFrame();
            if ( Global.enableEditFrameThreads ) {
                new Thread( Global.EditFrameThreadGroup, ef ).start();
            }
            ef = null;      // 09-05-05 : maybe will help with memory leaks
            //Hub.setCurrentEditFrame( ef );

        } else if ( e.getActionCommand().equals( Global.strs( "CutLabel" ) ) ) {
            performActionClipboardCut();
        } else if ( e.getActionCommand().equals( Global.strs( "CopyLabel" ) ) ) {
            performActionClipboardCopy();
//        } else if ( e.getActionCommand().equals( Global.strs( "CopyImageLabel" ) ) ) {
//            performActionClipboardCopy();
        } else if ( e.getActionCommand().equals( Global.strs( "PasteLabel" ) ) ) {
            performActionClipboardPaste( ef.lastMouseClickPoint );
        } else if ( e.getActionCommand().equals( Global.strs( "DuplicateLabel" ) ) ) {
            performActionDupSelection();
        } else if ( e.getActionCommand().equals( Global.strs( "UndoLabel" ) ) ) {
            performActionUndo();
        } else if ( e.getActionCommand().equals( Global.strs( "RedoLabel" ) ) ) {
            performActionRedo();
        } else if ( e.getActionCommand().equals( Global.strs( "ClearLabel" ) ) ) {
            performActionDeleteSelection();
        } else if ( e.getActionCommand().equals( Global.strs( "SelectAllLabel" ) ) ) {
            performActionSelectAll();
        } else if ( e.getActionCommand().equals( Global.strs( "ChangeFontLabel" ) ) ) {
            performActionChangeFont();
        } else if ( e.getActionCommand().equals( Global.strs( "AlignVLabel" ) ) ) {
            performActionAlign( "vertical" );
        } else if ( e.getActionCommand().equals( Global.strs( "AlignHLabel" ) ) ) {
            performActionAlign( "horizontal" );
        } else if ( e.getActionCommand().equals( Global.strs( "MinimizeLabel" ) ) ) {
            performActionMinimizeSelection();
        } else if ( e.getActionCommand().equals( OperManager.MakeGenericCmdLabel ) ) {
            ef.omgr.performActionMakeGeneric();
        } else if ( e.getActionCommand().equals( OperManager.ValidateCmdLabel ) ) {
            OperManager.performActionValidate( ef.TheGraph );
        } else if ( e.getActionCommand().equals( "Show Internals" ) ) {
            ef.omgr.performActionShowInternals( sortSelectionObjects() );
        } else if ( e.getActionCommand().equals( Global.strs( "AttachOntologyLabel" ) ) ) {
            ef.omgr.performActionAttachOntologyLabel( sortSelectionObjects() );
        } else if ( e.getActionCommand().equals( Global.strs( "DeleteOntologyLabel" ) ) ) {
            ef.omgr.performActionDeleteOntologyLabel( sortSelectionObjects() );
        } else if ( e.getActionCommand().equals( OperManager.CommitToKBLabel ) ) {
            ef.omgr.performActionCommitToKB( ef.TheGraph );
        } else if ( e.getActionCommand().equals( Global.strs( "DisplayAsCGIFLabel" ) ) ) {
            performActionDisplayAsCGIF();
        } else if ( e.getActionCommand().equals( Global.strs( "DisplayAsEnglishLabel" ) ) ) {
            ef.emgr.performActionDisplayAsEnglish();
        } else if ( e.getActionCommand().equals( Global.strs( "DisplayAsXMLLabel" ) ) ) {
            ef.emgr.performActionDisplayAsXML();
        } else if ( e.getActionCommand().equals( Global.strs( "DisplayMetricsLabel" ) ) ) {
            ef.emgr.performActionDisplayMetrics();

            // These are the file export format commands
            // Formats of family FileFormat.Family.TEXT
        } else if ( e.getActionCommand().equals( Global.strs( "ExportCGIFLabel" ) ) ) {
            performActionSaveGraphFormattedAs( FileFormat.CGIF2007 );
        } else if ( e.getActionCommand().equals( Global.strs( "SaveAsLabel" ) ) ) {
            performActionSaveGraphFormattedAs( FileFormat.CHARGER3 );
//        } else if ( e.getActionCommand().equals( Global.strs( "ExportTestXMLLabel" ) ) ) {
//            performActionSaveGraphFormattedAs( FileFormat.CHARGER4 );
        } else if ( e.getActionCommand().equals( Global.strs( "SaveLabel" ) ) ) {
            performActionSaveGraphWOInteraction( ef.TheGraph );
            // Formats of family FileFormat.Family.BITMAP
        } else if ( IOManager.imageFormats.contains( e.getActionCommand().toLowerCase() ) ) {
            performActionSaveGraphFormattedAs( FileFormat.FileFormatOf( e.getActionCommand().toLowerCase() ) );
            // Formats of family FileFormat.Family.VECTOR
        } else if ( e.getActionCommand().startsWith( "PDF" ) ) {
            performActionSaveGraphFormattedAs( FileFormat.PDF );
        } else if ( e.getActionCommand().startsWith( "SVG" ) ) {
            performActionSaveGraphFormattedAs( FileFormat.SVG );
        } else if ( e.getActionCommand().startsWith( "EPS" ) ) {
            performActionSaveGraphFormattedAs( FileFormat.EPS );



        } else if ( e.getActionCommand().equals( Global.strs( "ImportCGIFLabel" ) ) ) {
            performActionImportCGIF();
        } else if ( e.getActionCommand().equals( Global.strs( "PageSetupLabel" ) ) ) {
            Global.performActionPageSetup();
        } else if ( e.getActionCommand().equals( Global.strs( "PrintLabel" ) ) ) {
            performActionPrintGraph();
        } else if ( e.getActionCommand().equals( Global.strs( "OpenLabel" ) ) ) {
            performActionOpenGraph();
        } else if ( e.getActionCommand().equals( Global.strs( "CloseLabel" ) ) ) {
            ef.closeOut();
        } else if ( e.getActionCommand().equals( Global.strs( "PreferencesLabel" ) ) ) {
            performActionPreferences();
        } else if ( e.getActionCommand().equals( Global.strs( "QuitLabel" ) ) ) {
            performActionQuit();
            // some testing items

        } else if ( e.getActionCommand().equals( "Show Hierarchies" ) ) {
            performActionShowHierarchies();
        } else if ( e.getActionCommand().equals( "Read from jar file" ) ) {
            performActionReadFromJarfile();
        } else if ( e.getActionCommand().equals( "Auto Layout" ) ) {
            performActionLayoutUsingSpring();
//        } else if ( e.getActionCommand().equals( "Simple Layout" ) ) {
//            performActionLayoutUsingSimple();
//        } else if ( e.getActionCommand().equals( "moveGraph by 150, 100" ) ) {
//            ef.TheGraph.moveGraph( new Point2D.Double( 150, 100 ) );

        } else if ( e.getActionCommand().equals( "Update Internal IDs" ) ) {
            performActionUpdateGOIDs();

        } /*else if ( e.getActionCommand().equals( Hub.CharGerMasterFrame.BackToHubCmdLabel ) ) {
         Hub.CharGerMasterFrame.performActionBackToHub();
         }*/ // Look for one of the modality labels, as a last resort...
        else if ( sourceMenuItem != null ) {
            Color c = null;
            //if ( sourceMenuItem.getText().equals( "Text" ) )
            if ( sourceMenuItem == ef.ChangeTextItem ) {
                // change the text color of the selection
                c = JColorChooser.showDialog( ef,
                        "Choose text color for selected objects.", c );
                if ( c != null ) {
                    performActionColorSelection( "text", c );
                }
            } else if ( sourceMenuItem == ef.ChangeFillItem ) // if ( sourceMenuItem.getText().equals( "Fill" ) )
            {
                // change the text color of the selection
                c = JColorChooser.showDialog( ef,
                        "Choose fill color for selected objects.", c );
                if ( c != null ) {
                    performActionColorSelection( "fill", c );
                }
            } else if ( sourceMenuItem == ef.ChangeColorDefaultItem ) {
                // change color to match color defaults
                performActionColorSelection( "text", Global.userForeground );
                performActionColorSelection( "fill", Global.userBackground );
            } else if ( sourceMenuItem == ef.ChangeColorFactoryItem ) {
                // change color to match color defaults
                //Hub.useFactoryDefaultColors( true );
                performActionColorSelection( "text", Global.factoryForeground );
                performActionColorSelection( "fill", Global.factoryBackground );
                //Hub.useFactoryDefaultColors( false );
            } else if ( sourceMenuItem == ef.ChangeColorBlackAndWhiteItem ) {
                // change color to match color defaults
                Global.showBorders = false;
                Global.showShadows = false;
                performActionColorSelection( "text", Global.bwForeground );
                performActionColorSelection( "fill", Global.bwBackground );
            } else if ( sourceMenuItem == ef.ChangeColorGrayscaleItem ) {
                // change color to match color defaults
                performActionColorSelection( "text", Global.grayForeground );
                performActionColorSelection( "fill", Global.grayBackground );
            }
            // here, at the end of action performed, yield to prevent an accident
            Thread.yield();
        }
        // here at the end of the action performed tasks
        // Note that if ef was closed out by some action, it might be null at this point
        if ( ef != null ) {
            ef.refreshBorders();
            ef.setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
            if ( ef.somethingHasChanged ) {
                //Global.info( "something changed.");
                // DON'T BE FOOLED!!! This section rarely happens!!! somethingHasChanged obviously toasted.
            }
        }
    }

    private void performExportToTestXML() {
        //copied and modified from performActionSaveGraphFormattedAs method
        try {
            //IOManager iomgr = new IOManager( ef );
            //newFile = iomgr.GraphToFile( ef.graphSourceFile.getAbsolutePath(), ef.TheGraph, ef, format );

            JFileChooser myChooser = new JFileChooser();

            myChooser.addChoosableFileFilter( new javax.swing.filechooser.FileFilter() {
                public boolean accept( File aFile ) {
                    if ( aFile.isDirectory() ) {
                        return true;
                    }

                    String filename = aFile.getName();
                    if ( filename.endsWith( ".cgfx" ) ) {
                        return true;
                    } else {
                        return false;
                    }
                }

                public String getDescription() {
                    return "Test XML (*.cgfx)";
                }
            } );

            int result = myChooser.showSaveDialog( ef );

            if ( result == JFileChooser.APPROVE_OPTION ) {
                File kSaveFile = myChooser.getSelectedFile();

                try {
                    //String homedir = System.getProperties().getProperty("user.home")
                    //        + System.getProperties().getProperty("file.separator");
                    XMLEncoder encoder = new XMLEncoder( new BufferedOutputStream(
                            new FileOutputStream( kSaveFile ) ) );
                    ef.TheGraph.setOwnerFrame( null );
                    encoder.writeObject( ef.TheGraph );
                    ef.TheGraph.setOwnerFrame( ef );
                    encoder.close();
                } catch ( Exception exc ) {
                    CGUtil.showMessageDialog( null, "Exception: " + exc.getMessage() );
                }

                // Another option is to use http://xstream.codehaus.org/download.html

                /* BufferedWriter myKWriter = new BufferedWriter( new FileWriter( kSaveFile ) );
                 // iomgr.saveGraph38XML( myKWriter, ef.TheGraph );
                 javax.xml.bind.JAXB.marshal( ef.TheGraph, myKWriter );
                 myKWriter.flush();
                 myKWriter.close();
                 * */
                // above is a suggestion for saving Charger graphs.

            }
        } catch ( Exception x ) {
            // Hub.warning( "Problem saving " + newFile.getAbsolutePath() + ": " + x.getMessage() );
            System.err.println( x.getMessage() );
            x.printStackTrace();
        }
    }
    //end added by Kevin

    /**
     * Checks to see if one of the label editing features is active
     *
     *
     */
    public boolean labelEditingIsActive() {
        if ( ( ef.cp.nodeEditingDialog != null ) && ef.cp.nodeEditingDialog.isVisible() ) {
            return true;
        }
//        if ( ( ef.cp.labelChooser != null ) && ef.cp.labelChooser.isVisible() ) {
//            return true;
//        }
        return false;
    }

    public JTextField getTextEditField( JDialog dialog ) {
        Component[] components = dialog.getContentPane().getComponents();

        return null;

    }

    /**
     * Performs the cut from clipboard operation for graph objects.
     * Cut/Copy/Paste of text to system clipboard is handled by the
     * nodeEditingDialog
     */
    public void performActionClipboardCut() {
        performActionClipboardCopy();
        performActionDeleteSelection();
        Global.keepClipboardIDs = true;
        //ef.setMenuItems();
        ef.lastMouseClickPoint = null;
        setMenuItems();
    }

    /**
     * Performs the copy to clipboard operation for graph objects.
     * Cut/Copy/Paste of text to system clipboard is handled by the
     * nodeEditingDialog
     */
    public void performActionClipboardCopy() {
        // first make sure that if a context is selected, all its members are also selected
        ef.includeContextMembers();
        // Global.info( ef.summarizeSelection() );
        ArrayList v = sortSelectionObjects();
        // Global.info( "sorted selection vector length is " + v.size() );
        if ( v.size() == 0 ) {
            ef.displayOneLiner( "Please select something to Copy/cut." );
            return;
        }
        //ef.resetSelection();
        String s = GraphObject.listToStringXML( v );
        //Global.info( "vectortostring :\n" + s + "\n" );
        StringSelection sel = new StringSelection( s );


        // Handle copying to CharGer's local clipboard in CGX form for later parsing
        Global.cgClipboard.setContents( sel, this );
        Global.keepClipboardIDs = false;		// need new ID's for copy

        ef.selectionRect = null;
        ef.lastMouseClickPoint = null;
        
        setMenuItems();
        //ef.sp.repaint();

        // CR-1006 08-22-19 hsd !!
        // Handle copying to the system clipboard
        charger.obj.Graph g = new charger.obj.Graph( null );
        //Global.info( g.getBriefSummary() );
        CGXParser parser = new CGXParser();
//        ArrayList newOnes = parser.parseCGXMLString( s, g, new Point2D.Double( 0, 0 ), Global.keepClipboardIDs );
        ArrayList<GraphObject> newOnes = CGXParser.parseForCopying( s, g);
        
        BufferedImage bi = IOManager.graphToImage( g );
        IOManager.TransferableImage timage = new IOManager.TransferableImage( );
        timage.setImage( bi ); // 11-22-2005 : hsd
        
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents( timage, null );

    }

    /**
     * Performs the paste from clipboard operation for graph objects.
     * Cut/Copy/Paste of text to system clipboard is handled by the
     * nodeEditingDialog
     *
     * @param p The new upper left hand corner of the pasted part;      * if <code>null</code> then use default offset
     */
    public void performActionClipboardPaste( Point2D.Double p ) {
        ArrayList newOnes = null;
        String clipcontents = null;
        StringSelection s = (StringSelection)Global.cgClipboard.getContents( this );
        try {
            clipcontents =
                    (String)s.getTransferData( DataFlavor.stringFlavor );
            //Global.info( "just copied " + clipcontents );
        } catch ( UnsupportedFlavorException e ) {
            Global.info( "unsupported flavor " + e.getMessage() );
        } catch ( IOException e2 ) {
            Global.info( "IO Exception " + e2.getMessage() );
        }
        if ( !useNewUndoRedo ) {
            makeHoldGraph();
        }

        newOnes = CGXParser.parseForCopying( clipcontents, ef.TheGraph );


        Global.keepClipboardIDs = false;		// need new ID's if we're going to paste it further

        OperManager.performActionValidate( ef.TheGraph );
        ef.resetSelection();

        Iterator<GraphObject> iter = newOnes.iterator();
        while ( iter.hasNext() ) {
            GraphObject go = iter.next();
            ef.addToSelection( go );
            ef.somethingHasBeenSelected = true;
        }
        ef.EFSelectedObjects = sortSelectionObjects();
        if ( ef.somethingHasBeenSelected ) {
            setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
        }
    }

    /**
     * Clear the current edit frame, deleting all the components in its root
     * graph.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionClearAll() {
        if ( !ef.performCheckSaved() ) {
            return;
        }
        if ( !useNewUndoRedo ) {
            makeHoldGraph();
        }
        clearGraph( false );
        setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
        //ef.sp.repaint();
    }


    /**
     * Removes the frame's resident graph. Doesn't check for whether it's saved
     * or not.
     *
     * @param all whether to reset the graph name  as well. it's a
     * cheap dispose -- NEEDS to be optimized to really get rid of the graph's
     * contents
     */
    public void clearGraph( boolean all ) {
        nothingChanged();
        ef.clearStatus();
        /*if (cp.textLabelEditField != null) {
         cp.closeTextEditors( );
         }
         */
        Global.knowledgeManager.forgetKnowledgeSource( ef.TheGraph );
        //ef.TheGraph.disconnectObject(ef.TheGraph);
        ef.TheGraph = new Graph( null );
        ef.TheGraph.setOwnerFrame( ef );
        // reset();
        Global.knowledgeManager.addKnowledgeSource( ef.TheGraph );
        ef.setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
//        ef.resetSelection();
        if ( all ) {

        }
    }

    /**
     * Clear the current edit frame, deleting all the components in its root
     * graph.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionSelectAll() {
                // CR-2001 11-09-19 hsd initialization problem; needed to explicitly make them null.
        if ( ( ( ef.cp.nodeEditingDialog != null ) && ef.cp.nodeEditingDialog.isVisible() )
                || ( ( ef.cp.labelChooser != null ) && ef.cp.labelChooser.isVisible() ) ) {
        } else {
            Iterator<GraphObject> iter = new DeepIterator( ef.TheGraph );
            while ( iter.hasNext() ) {
                GraphObject go = iter.next();
                ef.addToSelection( go );
            }
            ef.somethingHasBeenSelected = true;
            ef.sp.repaint();
        }
        ef.setMenuItems();
    }

    public void performActionPreferences() {
        ef.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        Global.managePreferencesFrame();
    }

    /**
     * Queues a graphic representation of the entire graph to a printer.
     *
     * @see Graph#print for the details of how to render the graph to a graphics
     * context.
     */
    public void performActionPrintGraph() {
        // initial setup of print job

        PrinterJob pjob = PrinterJob.getPrinterJob();
        pjob.setJobName( ef.graphName );
        // if page setup hasn't been called, make it so
        if ( Global.pformat == null ) {
            Global.performActionPageSetup();
        }
        boolean ok = pjob.printDialog();
        if ( ok ) {
            pjob.setPrintable( ef.TheGraph, Global.pformat );	// renders graph on page

            PrintRequestAttributeSet printAttributes = new HashPrintRequestAttributeSet();
            printAttributes.add( new PrinterResolution( 300, 300, ResolutionSyntax.DPI ) );
            printAttributes.add( PrintQuality.HIGH );

            try {
                pjob.print( printAttributes );	// queues raster image to printer by invoking Graph.print()

            } catch ( PrinterException pe ) {
                JOptionPane.showMessageDialog(
                        ef, "Printer error: " + pe.getMessage(), "Printer Error",
                        JOptionPane.ERROR_MESSAGE );
            }
        }
    }

    /**
     * Prints the outer edit frame's graph to the console window.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionDisplayAsCGIF() {
        // convert to string and strip off extension
        String suggestedPath =
                General.stripFileExtension( ef.graphAbsoluteFile.getAbsolutePath() );
        File suggestedFile = new File( suggestedPath + ".cgif" );


        if ( ef.CGIFDisplay == null ) {
            ef.CGIFDisplay = new GenericTextFrame( ef );
            ef.CGIFDisplay.setEditable( false );
            ef.CGIFDisplay.setLocation( ef.getSize().width / 3, ef.getSize().height / 3 );
            ef.CGIFDisplay.setTitle( "Display CGIF" );
            ef.CGIFDisplay.addWindowListener( new WindowAdapter() {
                public void windowActivated( WindowEvent e ) {
                    performActionDisplayAsCGIF();
                }
            } );
        }
        ef.CGIFDisplay.setTextFont( ef.currentFont );
        ef.CGIFDisplay.setLabel( "CGIF form of " + ef.getTitle() );
        ef.CGIFDisplay.setSuggestedFile( suggestedFile );

        ef.CGIFDisplay.setTheText( cgif.generate.CGIFWriter.graphToString( ef.TheGraph, Global.includeCharGerInfoInCGIF ) );
        refreshCGIF( ef.CGIFDisplay );	// for now, don't include CharGer info in display

        ef.CGIFDisplay.toFront();
        ef.CGIFDisplay.setVisible( true );
    }

    /**
     * Shows the outer edit frame's graph in English in its own window.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionDisplayAsEnglish() {

        String suggestedPath =
                General.stripFileExtension( ef.graphAbsoluteFile.getAbsolutePath() );
        File suggestedFile = new File( suggestedPath + ".txt" );

        if ( ef.textFormDisplay == null ) {
            ef.textFormDisplay = new GenericTextFrame( ef );
            ef.textFormDisplay.setLocation( ef.getSize().width / 3, ef.getSize().height / 3 );
            ef.textFormDisplay.setTitle( "Generated English" );
            ef.textFormDisplay.addWindowListener( new WindowAdapter() {
                public void windowActivated( WindowEvent e ) {
                    performActionDisplayAsEnglish();
                }
            } );
        }
        if ( ef.currentFont != null ) {
            ef.textFormDisplay.setTextFont( ef.currentFont );
            //ef.textFormDisplay.setTextFont( new Font( "Arial", Font.PLAIN, 12 ) );
        }
        ef.textFormDisplay.setLabel( "English form of " + ef.getTitle() );
        refreshEnglish( ef.textFormDisplay );
        ef.textFormDisplay.setSuggestedFile( suggestedFile );

        ef.textFormDisplay.toFront();
        ef.textFormDisplay.setVisible( true );
    }

    /**
     * Shows the outer edit frame's graph in English in its own window.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionDisplayAsXML() {
        String suggestedPath =
                General.stripFileExtension( ef.graphAbsoluteFile.getAbsolutePath() );
        File suggestedFile = new File( suggestedPath + ".txt" );

        if ( ef.XMLDisplay == null ) {
            ef.XMLDisplay = new GenericTextFrame( ef );
            ef.XMLDisplay.setLocation( ef.getSize().width / 3, ef.getSize().height / 3 );
            ef.XMLDisplay.setTitle( "XML Form" );
            ef.XMLDisplay.addWindowListener( new WindowAdapter() {
                public void windowActivated( WindowEvent e ) {
                    performActionDisplayAsXML();
                }
            } );
        }
        if ( ef.currentFont != null ) {
            ef.XMLDisplay.setTextFont( ef.currentFont );
        }
        ef.XMLDisplay.setLabel( "XML version of " + ef.getTitle() );
        ef.XMLDisplay.setTheText(charger.cgx.CGXGenerator.generateXML( ef.TheGraph ) );
        ef.XMLDisplay.setSuggestedFile( suggestedFile );
        ef.XMLDisplay.setEditable( false );

        ef.XMLDisplay.toFront();
        ef.XMLDisplay.setVisible( true );
    }

    /**
     * Shows the outer edit frame's graph metrics in its own window.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionDisplayMetrics() {
        String suggestedPath =
                General.stripFileExtension( ef.graphAbsoluteFile.getAbsolutePath() );
        File suggestedFile = new File( suggestedPath + ".cgmetrics.txt" );

        if ( ef.metricsDisplay == null ) {
            ef.metricsDisplay = new GenericTextFrame( ef );
            ef.metricsDisplay.setLocation( ef.getSize().width / 3, ef.getSize().height / 3 );
            ef.metricsDisplay.setTitle( "Metrics Display" );
            ef.metricsDisplay.addWindowListener( new WindowAdapter() {
                public void windowActivated( WindowEvent e ) {
                    performActionDisplayMetrics();
                }
            } );
        }
        if ( ef.currentFont != null ) {
            ef.metricsDisplay.setTextFont( ef.currentFont );
        }
        ef.metricsDisplay.setLabel( "Metrics for " + ef.getTitle() + ":" );

        GraphMetrics metrics = new GraphMetrics( ef.TheGraph );
//        Date now = Calendar.getInstance().getTime();
//        String today = DateFormat.getDateTimeInstance( DateFormat.MEDIUM, DateFormat.MEDIUM ).format( now );
        String today = new CDateTime().formatted( Global.ChargerDefaultDateTimeStyle);

        String parms =
                "editor=\"" + Global.EditorNameString + "\" "
                + "version=\"" + Global.RELEASE_VERSION + "\" "
                + "created=\"" + today + "\" "
                + "user=\"" + System.getProperty( "user.name" ) + "\"";
        ef.metricsDisplay.theText.setContentType( "text/html" );
        ef.metricsDisplay.setTheText( Tag.p( ef.getTitle() + "\n" + parms + "\n" + metrics.getGraphMetrics( false ) ) );
        //ef.metricsDisplay.setText( charger.xml.CGXGenerator.generateXML( ef.TheGraph ) );
        ef.metricsDisplay.setSuggestedFile( suggestedFile );

        ef.metricsDisplay.toFront();
        ef.metricsDisplay.setVisible( true );
    }

    /**
     * Use the selection and make a context out of them, if possible.
     *
     * @param what valued either "context" or "cut"
     * @see EditManager#actionPerformed
     */
    public void performActionMakeContext( String what ) {
        if ( ef.somethingHasBeenSelected ) {
            if ( ef.selectionRect == null ) {
                return;
            }
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
            try {
                Rectangle2D.Double r = General.make2DDouble( ef.selectionRect );
                ef.makeContext( ef.EFSelectedNodes, what, r );
            } catch ( CGContextException x ) {
                JOptionPane.showMessageDialog(
                        ef, "That action would create overlapping contexts.",
                        "CG Rule not followed", JOptionPane.ERROR_MESSAGE );
                ef.repaint();
            }
            ef.resetSelection();
            setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE );
        } else {
            ef.displayOneLiner( "Please select some thing(s), then make a context." );
        }
        ef.repaint();
    }

    /**
     * Attach the contents of one of the selected contexts to its owner graph.
     */
    public void performActionUnMakeContext() {
        if ( ef.somethingHasBeenSelected ) {
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
            try {
                if ( ef.unMakeContext( ef.EFSelectedNodes, ef.selectionRect ) ) {
                    setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
                    ef.selectionRect = null;
                }
            } catch ( CGContextException x ) {
                //ef.displayOneLiner( "To un-make a context, please select a single context." );
                ef.displayOneLiner( "CG Context Exception: " + x.getMessage() );
            }
            ef.sp.repaint();
        } else {
            ef.displayOneLiner( "First select a single context, then choose to un-make it." );
            ef.repaint();
        }
    }

    /**
     * Delete the nodes in the selection, and their associated links.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionDeleteSelection() {
        // algorithm:
        //		enumerate all selected objects
        //		if object is selected, then execute a delete operation on it
        if ( ef.somethingHasBeenSelected ) {
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
            while ( ef.EFSelectedObjects.size() > 0 ) {
                GraphObject go = ef.EFSelectedObjects.get( 0 );
                if ( go.isSelected ) {
                    ef.EFSelectedObjects.remove( (Object)go );
                    ef.EFSelectedNodes.remove( (Object)go );
                    go.getOwnerGraph().forgetObject( go );
                } else {
                    Global.info( "there's un-selected object on selection list: " + go );
                }
            }
            setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
            ef.resetSelection();	// shouldn't be necessary; we already forgot all the selected objects

        } else {
            ef.displayOneLiner( "Please select some thing(s) you want to delete." );
        }
        ef.repaint();
    }

    /**
     * Changes the text or fill of all selected objects to the specified color.
     *
     * @param foreback one of "text" or "fill"
     * @param c Color to be applied.
     */
    public void performActionColorSelection( String foreback, Color c ) {
        performActionColorSelection( foreback, c, null );
    }

    /**
     * Changes the text or fill of all selected objects using the specified
     * color table.
     *
     * @param foreback one of "text" or "fill"
     * @param table list of classname,color that governs the change.
     */
    public void performActionColorSelection( String foreback, Hashtable<String, Color> table ) {
        performActionColorSelection( foreback, null, table );
    }

    /**
     * Changes the color of the selected graph objects.
     *
     * @param foreback one of "text" or "fill"
     * @param c The color to be applied to all selected objects.
     * @param colorlist a list of <classname, color> pairs governing the change.
     * @see EditManager#actionPerformed
     */
    public void performActionColorSelection( String foreback, Color c, Hashtable<String, Color> colorlist ) {
        // algorithm:
        //		enumerate all selected objects
        //		if object is selected, then change its color
        if ( ef.somethingHasBeenSelected ) {
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
            Iterator<GraphObject> iter = ef.EFSelectedObjects.iterator();
            while ( iter.hasNext() ) {
                GraphObject go = iter.next();
                if ( go.isSelected ) {
                    if ( c != null ) // setting color explicitly
                    {
                        go.setColor( foreback, c );
                    } else // setting color from the table
                    {
                        go.setColor( foreback, colorlist.get( CGUtil.shortClassName( go ) ) );
                    }
                } else {
                    Global.warning( "There's an un-selected object on selection list: " + go );
                }
            }
            setChangedContent( EditChange.APPEARANCE, EditChange.UNDOABLE  );
            //ef.resetSelection();	// shouldn't be necessary; we already forgot all the selected objects
        } else {
            ef.displayOneLiner( "Please select some thing(s) you want to color." );
        }
        ef.repaint();
    }

    /**
     * Changes the font of the selected graph objects.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionChangeFont() {
        // algorithm:
        //		enumerate all selected setFont operation on it
        if ( ef.somethingHasBeenSelected ) {
            Font newFont = ( ef.EFSelectedObjects.get( 0 ) ).getLabelFont();       // use the first object to initially set the font
            FontChooser fontChooser = new FontChooser( ef, true, newFont, "Set the font for all selected objects", Global.showAllFonts );
            newFont = fontChooser.getTheFont();
            if ( newFont == null ) {
                return;
            }
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
            Iterator<GraphObject> iter = ef.EFSelectedObjects.iterator();
            while ( iter.hasNext() ) {
                GraphObject go = iter.next();
                if ( go.isSelected ) {
                    go.setLabelFont( newFont );
                    go.resizeIfNecessary();
                } else {
                    Global.warning( "There's an un-selected object on selection list: " + go );
                }
            }
            setChangedContent( EditChange.APPEARANCE, EditChange.UNDOABLE );
            //ef.resetSelection();	// shouldn't be necessary; we already forgot all the selected objects
        } else {
            ef.displayOneLiner( "Please select some thing(s) whose text you want to change." );
        }
        ef.repaint();
    }

    /**
     * Duplicates the selection, forming new contexts as necessary. Bypasses the
     * CharGer clipboard so that it operates independently of cut and paste.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionDupSelection() {
        ef.includeContextMembers();
        ArrayList objectList = sortSelectionObjects();
        if ( objectList.size() == 0 ) {
            ef.displayOneLiner( "Please select something to duplicate." );
            ef.repaint();
            return;
        }
        ef.resetSelection();
        String s = GraphObject.listToStringXML(objectList );
        ArrayList newOnes = null;
        if ( !useNewUndoRedo ) {
            makeHoldGraph();
        }

        // offset the copy slightly from the original so they don't overlap
        newOnes = CGXParser.parseForCopying( s, ef.TheGraph );
            

        Iterator<GraphObject> iter = newOnes.iterator();
        while ( iter.hasNext() ) {
            GraphObject go = iter.next();
            ef.addToSelection( go );
            ef.somethingHasBeenSelected = true;
        }
        ef.EFSelectedObjects = sortSelectionObjects();
        ef.sp.repaint();
        if ( ef.somethingHasBeenSelected ) {
            setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE );
            //ef.sp.repaint();
        }
        setMenuItems();
    }

    /**
     * Updates all the object IDs to reflect the latest scheme.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionUpdateGOIDs() {

        String graphAsString = CGXGenerator.generateXML( ef.TheGraph );
        if ( !useNewUndoRedo ) {
            makeHoldGraph();
        }

        Point2D.Double offset = new Point2D.Double( 0, 0 );
//        charger.xml.CGXParser parser = new charger.xml.CGXParser();

        ef.TheGraph = new Graph();  // empty for the rest to be added. A *really* cheap dispose of the graph.

        ArrayList newOnes = CGXParser.parseForCopying( graphAsString, ef.TheGraph );


        ef.sp.repaint();
        setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE );

        setMenuItems();
    }

    /**
     * Aligns the graph objects in the selection, adjusting the centers either
     * vertically or horizontally. Any moving in or out of contexts is handled
     * as though they were moved individually. In general, the order of objects
     * in the selection determines the order of moving.
     *
     * @param how either "vertical" or "horizontal"
     */
    public void performActionAlign( String how ) {
        // algorithm:
        //		enumerate all selected objects
        //		average either their x or y values, depending on the parameter.
        ArrayList v = sortSelectionObjects();
        double sum = 0;
        int ocount = 0;
        double average = 0;
        if ( ef.somethingHasBeenSelected ) {
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
            Iterator<GraphObject> iter = v.iterator();
            while ( iter.hasNext() ) {
                GraphObject go = iter.next();
                if ( go.myKind != GraphObject.Kind.GEDGE ) {
                    ocount++;
                    if ( how.equals( "vertical" ) ) {
                        sum = sum + go.getCenter().x;
                    } else {
                        sum = sum + go.getCenter().y;
                    }
                }
            }
            average =  sum / ocount ;

            iter = v.iterator();
            while ( iter.hasNext() ) {
                GraphObject go = iter.next();
                if ( go.myKind != GraphObject.Kind.GEDGE ) {
                    if ( how.equals( "vertical" ) ) {
                        go.setCenter( new Point2D.Double( average, go.getCenter().y ) );
                        //ef.putInCorrectContext( ef.TheGraph, go );
                    } else {
                        go.setCenter( new Point2D.Double( go.getCenter().x, average ) );
                        //ef.putInCorrectContext( ef.TheGraph, go );
                    }
                }
            }
            //ef.resetSelection(); // leave selection as it was.
        } else {
            ef.displayOneLiner( "First select the things you want to be aligned." );
        }
        setChangedContent( EditChange.APPEARANCE, EditChange.UNDOABLE );
        ef.repaint();
    }
    
//    public void alignObjects( ArrayList<GraphObject> objects ) {
////        ArrayList<GraphObject> sortedOnes = CGUtil.sortObjects( objects );
//        if ( objects.size() == 0 ) return;
//        Iterator iter = objects.iterator();
//            // align all the graphs first
//        while ( iter.hasNext() ) {
//            GraphObject go = (GraphObject)iter.next();
//            if ( go instanceof Graph ) {
//                
//            }
//        }
//    }
    
    /**
     * Shrink each selected node to its smallest size.
     */
    public void performActionMinimizeSelection() {
        ef.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        minimizeSelection();
        ef.repaint();
        ef.setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
    }

    /**
     * Save or export the current graph, using the graph's name as the filename
     * destination. If there's an extension, then it's removed and one
     * appropriate to the format is appended.
     *
     * @param format one of the valid formats
     * @return true if save was performed normally; false otherwise
     * @see EditManager#actionPerformed
     * @see IOManager
     * @see FileFormat
     */
    public boolean performActionSaveGraphFormattedAs( FileFormat format ) {
        //Global.info( "performActionSaveGraphWOInteraction " + format );
        File newFile = null;
//        if ( format.family() == FileFormat.Family.VECTOR ) {
//            IOManager.performActionSaveGraphAsVectorGraphic( ef.TheGraph, format, ef.graphSourceFile.getAbsolutePath() );
//        }
//        else
        try {
            newFile = IOManager.GraphToFile( ef.TheGraph, format, ef.graphSourceFile.getAbsolutePath(), ef );
            //Global.info( "newfile in performActionSaveGraphFormattedAs is " + newFile );
            if ( newFile != null ) {
                ef.TheGraph.setTextLabel( "Proposition" );
                ef.displayOneLiner( "Saved " + format + " graph to " + newFile.getAbsolutePath() );
                if ( format == FileFormat.CHARGER3 || format == FileFormat.CHARGER4 ) {
                    ef.renameGraphInFrame( newFile.getAbsolutePath() );
                    nothingChanged();
                }
                ef.repaint();
            }
        } catch ( CGException x ) {
            // Hub.warning( "Problem saving " + newFile.getAbsolutePath() + ": " + x.getMessage() );
            return false;
        }
        return true;
    }

    /**
     * Save a graph in the default form, using the default filename as
     * destination. Don't prompt user.
     *
     * @param g graph to be saved
     * @see EditManager#actionPerformed
     */
    public boolean performActionSaveGraphWOInteraction( Graph g ) {
        //Global.info( "performActionSaveGraphFormattedAs" );
        //String stat = ef.GraphModality;
        //String filename = null;
        String filename = ef.graphAbsoluteFile.getParentFile().getAbsolutePath()
                + File.separator + ef.getGraphName() + Global.ChargerFileExtension;
        //Global.info( "prefixPath is " + ef.prefixPath );
        //Global.info( "made up name " + ef.prefixPath + Hub.makeUpFileName( ef.graphName, stat, Hub.ChargerFileExtension ) );
        //Global.info( "getMyFileLocation is " + ef.getMyFileLocation() );

        if ( !ef.somethingHasChanged ) {
            ef.displayOneLiner( "File " + filename + " unchanged." );
            return true;	// stretching the interpretation of "normally saved"

        }

        if ( ef.graphSourceFile.getName().startsWith( "Untitled" ) ) {
            return performActionSaveGraphFormattedAs( FileFormat.CHARGER3 );
            // probably not the best idea -- what if user just wants to save image of untitled graph?
        }

        BufferedWriter out = null;

        try {
            IOManager.GraphToFile( g, Global.defaultFileFormat, filename, null );
            ef.displayOneLiner( "Saved graph to " + filename );
//                IOManager.saveGraph38XML( out, g );
        } catch ( CGFileException cge ) {
            return false;
        }
        nothingChanged();
        return true;
    }

    /**
     * Loads a graph into the edit frame. Invokes the same handler as the
     * HubFrame's Open menu item.
     *
     * @see EditManager#actionPerformed
     * @see IOManager
     */
    public void performActionOpenGraph() {
        ef.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        Global.CharGerMasterFrame.menuFileOpenActionPerformed(
                new ActionEvent( ef, ActionEvent.ACTION_PERFORMED, "test" ) );
        ef.setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
    }

    /**
     *
     */
    public void performActionImportCGIF() {
        ef.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        Global.CharGerMasterFrame.menuFileOpenCGIFActionPerformed(
                new ActionEvent( ef, ActionEvent.ACTION_PERFORMED, "test" ) );
        ef.setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
    }

    /**
     * Part of the interface needed for the undo redo stack
     *
     * @see chargerlib.undo.Undoable
     */
    public EditorState currentState() {
        EditorState state = new EditorState( charger.cgx.CGXGenerator.generateXML( ef.TheGraph ) );
        state.setSomethingHasChanged( ef.somethingHasChanged );
        if ( ef.contentHasChanged ) {
            state.setKB( Global.sessionKB );  // This needs to be a deep copy of the knowledge base
        }
        return state;
    }

    /**
     * Part of the interface needed for the undo redo stack
     *
     * @see chargerlib.undo.Undoable
     */
    public void restoreState( UndoableState astate ) {
        EditorState state = (EditorState)astate;

        CGXParser.parseForNewGraph( state.getGraph(), ef.TheGraph );
        if ( !CGUtil.verifyIntegrityOfGraph( ef.TheGraph ) ) {
            General.error( "restore state: restored graph failed integrity check.s" );
        }
        ef.cp.repaint();
        if ( state.isSomethingHasChanged() ) {
            ef.somethingHasChanged = true;		// one of the rare times we do this directly
            ef.changedMarker.setBackground( Color.red );
        } else {
            ef.somethingHasChanged = false;		// one of the rare times we do this directly
            ef.changedMarker.setBackground( changeMarkerColor );
        }
    }

    /**
     * @see chargerlib.undo.Undoable
      *
     */
    public void setupMenus() {
//        showStatus();
        ef.UndoItem.setEnabled( urMgr.undoAvailable() );
        ef.RedoItem.setEnabled( urMgr.redoAvailable() );
    }

    public void performActionUndoNEW() {
        clearGraph( false );
        urMgr.doUndo();
        ef.resetSelection();
        ef.sp.repaint();
    }

    /**
     * @see EditManager#actionPerformed
     */
    public void performActionRedoNEW() {
        clearGraph( false );
        urMgr.doRedo();
        ef.resetSelection();
        ef.sp.repaint();
    }

    public void performActionUndo() {
        if ( useNewUndoRedo ) {
            performActionUndoNEW();
        } else {
            performActionUndoOLD();
        }
    }

    public void performActionRedo() {
        if ( useNewUndoRedo ) {
            performActionRedoNEW();
        } else {
            performActionRedoOLD();
        }
    }

    /**
     * Restores the previous graph from whatever the last change was.
     *
     * @see EditManager#actionPerformed
     */
    public void performActionUndoOLD() {
        String backupGraph = null;
        ArrayList newOnes = null;
        if ( !useNewUndoRedo ) {
            makeHoldGraph();		// save the "future" graph, in case we want to go forward in time again :-)
        }
        clearGraph( false );
        backupGraph = doUndo( holdGraph );

        //Global.info( "Undo restoring graph from backup" );
        if ( backupGraph != null && !backupGraph.equals( "" ) ) {
//            newOnes = parser.parseCGXMLString( backupGraph, ef.TheGraph, new Point2D.Double( 0, 0 ), true );
        } else {
            backupGraph = "";
        }
        ef.resetSelection();

        if ( undoList.size() == 0 ) // if we've undone back to the beginning, then nothing has changed
        {
            ef.somethingHasChanged = false;		// one of the rare times we do this directly

            ef.changedMarker.setBackground( changeMarkerColor );
        } else {
            ef.somethingHasChanged = true;		// one of the rare times we do this directly

            ef.changedMarker.setBackground( Color.red );
        }
        ef.setMenuItems();
        ef.sp.repaint();
    }

    /**
     * @see EditManager#actionPerformed
     */
    public void performActionRedoOLD() {
        String backupGraph = null;
        ArrayList newOnes = null;
        makeHoldGraph();
        clearGraph( false );
        backupGraph = doRedo( holdGraph );
        //Global.info( "re-do graph" );
//        newOnes = parser.parseCGXMLString( backupGraph, ef.TheGraph, new Point2D.Double( 0, 0 ), true );

        ef.resetSelection();

        ef.somethingHasChanged = true;		// one of the rare times we do this directly

        ef.changedMarker.setBackground( Color.red );
        ef.setMenuItems();
        ef.sp.repaint();
    }

    /**
     * Since user can invoke quit from the edit window, here's the handler.
     */
    public void performActionQuit() {
        if ( Global.closeOutAll() ) {
            System.exit( 0 );
        }
    }

    /**
     * Do the graph layout algorithm. Number of iterations, etc. are controlled
     * by the layout class itself.
     *
     * @see charger.layout.SpringGraphLayout
     */
    public void performActionLayoutUsingSpring() {
        ef.setCursor( new Cursor(Cursor.WAIT_CURSOR) );
        SpringGraphLayout layout = new SpringGraphLayout( ef.TheGraph );
//        String result = JOptionPane.showInputDialog( ef, "Enter number of iterations: ", SpringGraphLayout.MAX_ITERATIONS );
//        if ( result != null ) {
//            SpringGraphLayout.MAX_ITERATIONS = Integer.parseInt( result );
//        }
        ef.repaint();
        if ( !useNewUndoRedo ) {
            makeHoldGraph();
        }
        layout.setVerbose( false );
        layout.setEquilibriumEdgeLength( Global.preferredEdgeLength );
        layout.setMaxBoundsAvailable( ef.cp.getBounds());
        layout.performLayout();
        layout.copyToGraph();
        setChangedContent( EditChange.APPEARANCE, EditChange.UNDOABLE );
        ef.cp.adjustCanvasSize();
        ef.sp.repaint();
                ef.setCursor( new Cursor(Cursor.DEFAULT_CURSOR) );
    }
    

    public void performActionShowHierarchies() {
        GenericTextFrame frame = new GenericTextFrame( ef );
        frame.setLabel( "Showing hierarchies..." );
        frame.setTheText( Global.sessionKB.showConceptTypeHierarchy()
                + "\n"
                + Global.sessionKB.showRelationTypeHierarchy() );
        frame.setVisible( true );
    }
    
    public void performActionReadFromJarfile() {
        General.getFileFromClassPath( "Graphs", "Samples");
    }

    /**
     * Gather together all the selected nodes, edges and graphs into a list. The
     * list is sorted: graphs first, then nodes then edges... Assumes that
     * EFSelectedObjects is accurate. Responsible for making sure that no object
     * is included more than once (e.g., if it is enclosed in a graph that's in
     * the list as well as appearing by itself.) If an edge is selected (i.e.,
     * its "display rect" center point was inside a selection rectangle), but
     * either of its linked nodes is NOT selected, then the edge is not
     * included. If any two selected nodes are linked by an edge, then that edge
     * should be included whether it was originally selected or not.
     *
     * @return collection of selected objects
     */
//    public ArrayList sortSelectionObjects() {
//        return CGUtil.sortObjects( ef.EFSelectedObjects );
//    }
    
    public ArrayList sortSelectionObjects() {
        Iterator<GraphObject> iter = ef.EFSelectedObjects.iterator();
        GraphObject go = null;

        ArrayList graphs = new ArrayList();
        ArrayList nodes = new ArrayList();
        while ( iter.hasNext() ) {		// looking at all selected elements in the graph, ignoring edges

            go = iter.next();
            //if (go.isSelected) {
            if ( go.myKind == GraphObject.Kind.GRAPH ) {
                graphs.add( go );
            } else if ( go.myKind == GraphObject.Kind.GNODE ) {
                nodes.add( go );
            }
        }
        //}
        ArrayList selectedOnes = (ArrayList)graphs.clone();
        // for every node, only include it if it's not already enclosed in a selected graph
        iter = nodes.iterator();
        while ( iter.hasNext() ) {
            GNode gn = (GNode)iter.next();
            Graph owner = gn.getOwnerGraph();
            if ( !graphs.contains( owner ) ) {
                selectedOnes.add( gn );
            }
        }

        iter = new DeepIterator( ef.TheGraph, GraphObject.Kind.GEDGE );

        while ( iter.hasNext() ) {
            GEdge ge = (GEdge)iter.next();
            if ( ( graphs.contains( ge.fromObj ) || nodes.contains( ge.fromObj ) )
                    && ( graphs.contains( ge.toObj ) || nodes.contains( ge.toObj ) ) ) {
                selectedOnes.add( ge );
            }
        }
        return selectedOnes;
    }

    /**
     * "Remembers" what a graph looked like before a potential change. May be
     * called more often than absolutely necessary; not used until
     * setChangedContent() is called. setChangedContent() looks to see if the
     * hold graph is different from the current graph?
     *
     * @see #setChangedContent
     */
    public void makeHoldGraph() {

        OperManager.performActionValidate( ef.TheGraph );
        ef.sp.repaint();
        holdGraph = charger.cgx.CGXGenerator.generateXML( ef.TheGraph );

        // NOTE this is where the overall graph with owner zero gets into the undo stream.
        // it appears to be altering the way that graphs are being stored after undo
        // Global.info( "copied graph to hold..." );
    }

    /**
     * Tells the edit frame that something has changed. This routine serves as
     * the basic way for letting the editor environment know when various
     * changes have occurred.  
     * @param change the kind of change that was made
     * @see EditingChangeState
     */
//    public void setChangedContent( boolean semanticsChanged, boolean unDoable ) {
    public void setChangedContent( EditingChangeState change ) {
        //makeHoldGraph();
        //Global.info( "SOMETHING CHANGED." );
        // set the changed marker in the lower right corner of the edit frame
        if ( ef.changedMarker.getBackground() != Color.red ) {
            ef.changedMarker.setBackground( Color.red );
            ef.changedMarker.repaint();
        }
        // prepare the undo menu item
        //ef.UndoItem.setLabel( "Undo" );
        ef.somethingHasChanged = true;		// naturally, we're allowed to do this here directly

        // R0005 - 2 Jul 2000
        // make sure the canvas is big enough (plus 5%) to contain the current graph
        Rectangle2D.Double r = ef.TheGraph.getContentBounds();
        Dimension cpSize = ef.cp.getPreferredSize();
        if ( r.width > cpSize.width ) {
            cpSize.width = (int)( r.width * 1.05f );
        }
        if ( r.height > cpSize.height ) {
            cpSize.height = (int)( r.height * 1.05f );
        }
        ef.cp.setSize( cpSize );
        ef.cp.setPreferredSize( cpSize );

        ef.sp.repaint();

        if ( change.anythingChanged() ) {
            ef.refreshBorders();
            ef.contentHasChanged = true;
        }
        if ( change.isChangeUndoable() ) {
            if ( useNewUndoRedo ) {
                urMgr.markAfterUndoableStep();
            } else {
                doDo( holdGraph );
                ef.setMenuItems();
            }
        }
        refreshEnglish( ef.textFormDisplay );
        refreshCGIF( ef.CGIFDisplay );

    }
    
    /**
     * Convenience method to simplify the syntax for editing changes.
     * Converts the change list to an EditChangeState and then calls
     * setChangedContent with that argument
     * @param changes 
     */
    public void setChangedContent( EditChange... changes ) {
        setChangedContent( new EditingChangeState( changes ) );
    }
    
    private static Color changeMarkerColor = Global.oliveGreenColor;

    /**
     * Used to tell the editor window that its current contents have not
     * changed, regardless of what actions have occurred.
     */
    public void nothingChanged() {
        if ( ef.changedMarker.getBackground() != changeMarkerColor ) {
            ef.changedMarker.setBackground( changeMarkerColor );
            ef.changedMarker.repaint();
        }
        ef.somethingHasChanged = false;
        //resetUndo();	// @bug not really tested
    }

    /**
     * Sets every node in the graph to indicate that its changed state is false.
     *
     * @param g the graph whose nodes' state is to be set.
     * @see GNode#setChanged
     */
    public synchronized void clearChanged( Graph g ) {
        //Global.info( "CLEAR CHANGED called in the edit manager" );
        //Global.info( "thread group " + ef.threadgroup.getName() + " has " + ef.threadgroup.activeCount() + " thread(s)." );
        Iterator iter = new DeepIterator( g, GraphObject.Kind.GNODE );
        while ( iter.hasNext() ) {
            GNode gn = (GNode)iter.next();
            gn.setChanged( false );
        }
    }

    /**
     * @param t display frame whose text is to be refreshed with the English
     * paraphrase
     */
    public void refreshEnglish( GenericTextFrame t ) {
        if ( t == null ) {
            return;
        }
        if ( !t.isVisible() ) {
            return;
        }
        t.setTheText( "Feature not implemented." );
        // t.revalidate();
    }

    /**
     * @param t display frame whose text is to be refreshed with its CGIF
     * version
     */
    public void refreshCGIF( GenericTextFrame t ) {
        if ( t == null ) {
            return;
        }
        if ( !t.isVisible() ) {
            return;
        }
    }

    public void lostOwnership( Clipboard clipboard, Transferable contents ) {
        //Global.info( ef.graphName + " lost ownership of clipboard " + clipboard.getName() );
        ef.resetSelection();
    }

    /**
     * Makes all nodes as small as they can be.
     *
     * @see EditManager#enlargeSelection
     * @see EditManager#reduceSelection
     */
    public void minimizeSelection() {
        if ( !useNewUndoRedo ) {
            makeHoldGraph();
        }
        boolean minimized = false;
        boolean changed = false;
        boolean b;

        while ( !minimized ) {
            b = reduceSelection( 1, false );	// force unit reduction in the loop
            //Global.info( "enlarge return " + b );

            changed = changed || b;
            if ( !b ) {
                minimized = true;
            }
        }
        if ( changed ) {
            setChangedContent( EditChange.APPEARANCE, EditChange.UNDOABLE  );	// re-sizing doesn't change content

        }
    }

    /**
     * Increases the displayed size of all graph nodes selected. Arrows are
     * adjusted to reflect the new node sizes. If at a min or max size, no
     * change is made.
     *
     * @param increment amount to increase the selection; negative increment is
     * same as reduce.
     * @param atomicChange indicates whether to consider this an atomic change
     * or part of another operation.
     * @return true if anything was actually changed during this operation;
     * false otherwise.
     */
    public boolean enlargeSelection( int increment, boolean atomicChange ) {
        if ( atomicChange ) {
            if ( !useNewUndoRedo ) {
                makeHoldGraph();
            }
        }
        int size = ef.EFSelectedNodes.size();
        boolean changed = false;
        if ( size > 0 ) {
//            for ( int k = 0; k < size; k++ ) {
            for ( GraphObject go : ef.EFSelectedNodes) {
                
                changed = enlargeObject( go, increment ) || changed;
               ((GNode)go).adjustEdges();

            }
            if ( changed && atomicChange ) {
                setChangedContent(  EditChange.APPEARANCE, EditChange.UNDOABLE  );
            }
        }
        return changed;
    }

    /**
     * Reduces the displayed size of all graph nodes selected. Arrows are
     * adjusted to reflect the new node sizes.
     *
     * @param increment amount to reduce the selection.
     * @param atomicChange indicates whether to consider this an atomic change
     * or part of another operation.
     */
    public boolean reduceSelection( int increment, boolean atomicChange ) {
        //if ( atomicChange ) makeHoldGraph();
        return enlargeSelection( -1 * increment, atomicChange );
    }

    /**
     * Increases the graph object's size by an increment in pixels.
     *
     * @param go Graph object to be enlarged.
     * @param increment pixels to be enlarged by (may be negative for reducing).
     * When used for reducing, it tries to ensure that objects will not be
     * shrunk beyond some heuristic minimum.
     */
    public boolean enlargeObject( GraphObject go, int increment ) {
        Point2D.Double p = go.getCenter();
        Rectangle2D.Double oldR = go.getDisplayRect();
        boolean changed = false;

        if ( go instanceof Graph ) {
            //Global.info( "before setting - graph Dim.size is " + go.getDim() + " incr is " + increment );
            Iterator<GraphObject> iter = new ShallowIterator( (Graph)go );
            while ( iter.hasNext() ) {
                GraphObject ggo = iter.next();
                //Global.info( "look at graph object: " + ggo);
                if ( ggo.myKind != GraphObject.Kind.GEDGE ) {
                    //Global.info( "about to enlarge graph object by " + increment + " pixels: " + ggo);
                    boolean b = enlargeObject( ggo, increment );
                    changed = changed || b;
                }
            }
            Global.info( "before setting - graph rect is " + go.getDisplayRect() );
            go.setDim( go.getDim().width + 2 * increment, go.getDim().height + 2 * increment );
//            go.setCenter( go.getCenter().x - increment, go.getCenter().y - increment );
            ( (Graph)go ).resizeForContents( null );
            // when shrinking, prevent size from going in the wrong direction;
            // i.e., if old rectangle is not larger than new rectangle, revert to the old one.
            Rectangle2D.Double possibleNewRect = (Rectangle2D.Double)oldR.createUnion( go.getDisplayRect() );

             // CR-1007 hsd 11-08-19
//           if ( increment < 0 && ( possibleNewRect.equals( go.getDisplayRect() ) ) ) {
           if ( increment < 0 && ( General.equalsToRoundedInt( possibleNewRect, go.getDisplayRect())   ) ) {
                go.setDisplayRect( oldR );  // restore old rect

                ( (Graph)go ).setTextLabelPos();
            } else {
                changed = true;
                //	Global.info( "after setting - graph rect is " + go.getDisplayRect() );
            }
        } else // changing the size of a GNode
        {
            go.setDim( go.getDim().width + 2 * increment, go.getDim().height + 2 * increment );  // change displayed size
            // check if it's too small for the minimum

//            go.setTextLabel( go.getTextLabel(), ef.currentFontMetrics, p );	// force dimensions to be consistent
            Graphics g = ef.cp.getGraphics().create();
//            g.setFont(  go.getLabelFont());
            go.setTextLabel( go.getTextLabel(), g.getFontMetrics( go.getLabelFont() ), p );	// force dimensions to be consistent
            // since previous statement may or many not change the display rect

            if ( !go.getDisplayRect().equals( oldR ) ) {
                go.adjustCustomDisplayRect();
                // when shrinking, if dimension goes in the wrong direction, prevent it
                // i.e., if old rectangle is not larger than new rectangle, revert to the old one.
            }
            Rectangle2D.Double possibleNewRect = (Rectangle2D.Double)oldR.createUnion( go.getDisplayRect() );
            // CR-1007 hsd 11-08-19
            if ( increment < 0 && ( General.equalsToRoundedInt( possibleNewRect, go.getDisplayRect())  )) {
//                Global.info( "Comparing identical old rect " + oldR + " to new rect "+ possibleNewRect );
                go.setDisplayRect( oldR );          // restore old rect

                go.setCenter( go.getCenter() );
                go.textLabelLowerLeftPt =
                        CGUtil.getStringLowerLeftFromCenter( ef.currentFontMetrics, go.getTextLabel(), p );
                
            } else {
//                Global.info( "Changing old rect " + oldR + " to new rect "+ possibleNewRect );
                changed = true;
            }
        }
        //Global.info( "after setCenter - go.Dim.size is " + go.Dim );
        //Global.info( "changed " + (! (oldR.equals( go.getDisplayRect() )) || changed) + " old r = " + oldD + "---- new dim is " + go.Dim );
        return ( !oldR.equals( go.getDisplayRect() ) ) || changed;
    }

    /**
     * Determines the innermost context in which a point lies
     *
     * @param outermost The graph outside of which we need not bother to look
     * @param p the point of interest
     * @return most deeply nested graph in which the point lies
     */
    public synchronized static Graph innermostContext( Graph outermost, Point2D.Double p ) {
        Graph innermost = outermost;		// initially, assume no nesting

        Graph g = null;
        Iterator graphs = new DeepIterator( outermost, GraphObject.Kind.GRAPH );
        while ( graphs.hasNext() ) {
            g = (Graph)graphs.next();
            if ( g.getDisplayRect().contains( (int)p.x, (int)p.y ) ) {
                if ( g.nestedWithin( innermost ) ) {
                    innermost = g;
                }
            }
        }
        return innermost;
    }

    /**
     * Determines whether an arrow is allowed between the two objects. Runs a
     * variety of tests, based on the objects' classes, editor environment
     * parameters, etc.
     *
     * @param go1 "From" object
     * @param go2 "To" object
     * @return Error message if not allowed, otherwise null if all is well.
     */
    public String arrowAllowed( GraphObject go1, GraphObject go2 ) {
        String msg = "";
        if ( ( ( go1 instanceof Concept || go1 instanceof Graph )
                && ( go2 instanceof Relation || go2 instanceof Actor ) )
                || ( ( go2 instanceof Concept || go2 instanceof Graph )
                && ( go1 instanceof Relation || go1 instanceof Actor ) ) ) {
            // if connecting an actor, may want to allow it to cross context boundaries
            if ( go1.getOwnerGraph() == go2.getOwnerGraph() ) {
                if ( go1 instanceof Relation || go2 instanceof Relation ) {
                    // check for whether input/output constraints are adhered to
                    if ( Global.enforceStandardRelations ) // check if go1 would have more than one outgoing arc.
                    {
                        Relation rTest = null;
                        if ( go1 instanceof Relation ) {
                            rTest = (Relation)go1;
                        } else {
                            return null;
                        }
                        Iterator iter = rTest.getEdges().iterator();
                        while ( iter.hasNext() ) {
                            if ( ( (GEdge)iter.next() ).fromObj == rTest ) // already has an outgoing arc
                            {
                                return "A relation can have only one outgoing arc. " + Global.LineSeparator + "(See Preferences to allow more than one.)";
                            }
                        }
                        return null;
                    } else {
                        return null;
                    }
                }
            } else // different contexts
            {
                msg = msg + "A link is not allowed to cross a context boundary.";
                if ( go1 instanceof Actor || go2 instanceof Actor ) {
                    if ( Global.allowActorLinksAcrossContexts ) {
                        return null;
                    } else {
                        msg = msg + Global.LineSeparator
                                + " (See Preferences to allow actors to be linked across contexts.)";
                        return msg;
                    }
                } else {
                    return msg;
                }
            }
        } else {
            msg = msg + "An arrow must link a concept or graph to a relation or actor." + Global.LineSeparator + "  You tried to link " + CGUtil.shortClassName( go1 )
                    + " to " + CGUtil.shortClassName( go2 ) + ".";
            return msg;
        }
        return null;
    }

    /**
     * Determines whether a coreferent link is allowed between the two objects.
     * Runs a variety of tests, based on the objects' classes, editor
     * environment parameters, etc.
     *
     * @param go1 "From" object
     * @param go2 "To" object
     * @return Error message if not allowed, otherwise null if all is well.
     */
    public String corefAllowed( GraphObject go1, GraphObject go2 ) {
        if ( ( go1 instanceof Concept || go1 instanceof Graph )
                && ( go2 instanceof Concept || go2 instanceof Graph ) ) {
            if ( go1.getOwnerGraph().nestedWithin( go2.getOwnerGraph() )
                    || go2.getOwnerGraph().nestedWithin( go1.getOwnerGraph() ) ) {
                return null;
            } else if ( go1.getOwnerGraph() == go2.getOwnerGraph() ) {
                return null;
            } else {
                return "Cannot create co-referent link between these concepts. " + Global.LineSeparator + "Either (i) one concept/context's scope must enclose the other's, " + Global.LineSeparator + "or (ii) both must be in the same scope.";
            }
        } else {
            return "A coreferent link must link a concept or context to another concept or context." + Global.LineSeparator + " You tried to link " + CGUtil.shortClassName( go1 )
                    + " to " + CGUtil.shortClassName( go2 ) + ".";
        }
    }

    /**
     * Determines whether a generalization/specialization link is allowed
     * between the two objects.
     *
     * @param subobj "From" object
     * @param superobj "To" object
     * @return Error message if not allowed, otherwise null if all is well.
     */
    public String genspecAllowed( GraphObject subobj, GraphObject superobj ) {
        if ( Global.allowAnyGenSpecLink )
            return null;
        String result = null;
        if ( subobj instanceof TypeLabel && superobj instanceof TypeLabel ) {
            result = Global.sessionKB.getConceptTypeHierarchy().isRedundantSuperSubtype( superobj.getTextLabel(), subobj.getTextLabel() );
            if ( result != null ) {
                return result + "." + Global.LineSeparator
                        + " You tried to sub-type " + subobj.getTextLabel() + " to supertype " + superobj.getTextLabel() + ".";
            }
        } else if ( subobj instanceof RelationLabel && superobj instanceof RelationLabel ) {
            result = Global.sessionKB.getRelationTypeHierarchy().isRedundantSuperSubtype( superobj.getTextLabel(), subobj.getTextLabel() );
            if ( result != null ) {
                return result + "." + Global.LineSeparator
                        + " You tried to sub-relation " + subobj.getTextLabel() + " to super-relation " + superobj.getTextLabel() + ".";
            }
        } else {
            return "A generalization link can only connect pairs of type labels," + Global.LineSeparator
                    + " or pairs of relation labels." + Global.LineSeparator + " You tried to link "
                    + CGUtil.shortClassName( subobj ) + " to " + CGUtil.shortClassName( superobj ) + ".";
        }
        return null;
    }

    /**
     * @param direction one of the values "in" "out" or "actual"
     */
    public void performZoomAction( String direction ) {
        if ( direction.equals( "in" ) ) {
            if ( ef.scaleIndex >= 30 ) {
                return;
            } else {
                ef.scaleIndex++;
            }
        } else if ( direction.equals( "out" ) ) {
            if ( ef.scaleIndex <= -9 ) {
                return;
            } else {
                ef.scaleIndex--;
            }
        } else if ( direction.equals( "actual" ) ) {
            ef.scaleIndex = 0;
        }
        ef.canvasScaleFactor = 1.0d + ( ef.scaleIndex * EditFrame.scaleIncrement );
        //JOptionPane.showMessageDialog( ef, "Scale factor is now " + ef.canvasScaleFactor );

        setViewMenuItems( ef.canvasScaleFactor );
        ef.cp.adjustCanvasSize();       //BUG: Keeps raising a null pointer when sooming
        ef.refreshBorders();
        ef.cp.repaint();
    }


    /**
     * Operation resulting from the "Find" menu. Queries the user and then
     * invokes performFindString
     *
     * @see #performFindString
     */
    public void performFindMenuAction() {
        // get the string to find
        findString = (String)JOptionPane.showInputDialog( ef, "Find the following text:",
                "Find", JOptionPane.QUESTION_MESSAGE, null, null, findString );

        // call the actual find routine
        if ( findString != null ) {
            performFindString( findString );
        }
    }

    /**
     * Does the actual work of finding a string, making it the selection and
     * centering the display.
     *
     * @param s the string to search for.
     */
    public void performFindString( String s ) {
        findString = s;
        findIterator = new DeepIterator( ef.TheGraph );
        //Global.info( "a new find enum" );
        anyFound = false;
        setViewMenuItems( ef.canvasScaleFactor );
        performFindAgain( s );
    }

    public void performFindAgain( String s ) {
        boolean found = false;
        while ( findIterator.hasNext() && !found ) {
            GraphObject go = findIterator.next();
            //Global.info( "searching... graph object " + go.toString() );
            //Global.info( "searching... graph object " + go.getTextLabel() );
            // lower case the text label and see if string s is found within it
            if ( ( new StringBuilder( go.getTextLabel().toLowerCase() ).indexOf( s.toLowerCase() ) ) != -1 ) {
                ef.resetSelection();
                ef.addToSelection( go );
                //Global.info( "graph object's display rect is " + go.getDisplayRect().toString() );
                Rectangle2D.Double rectToShow = go.getDisplayRect();
                CGUtil.grow( rectToShow, 25, 25 );
                ef.cp.scrollRectToVisible( ef.antiscaled( rectToShow ).getBounds() );
                found = true;
                anyFound = true;
                break;
            }
        }
        if ( !found ) {
            ef.resetSelection();
            String more = " ";
            if ( anyFound ) {
                more = "more ";
            }
            ef.displayOneLiner( "No " + more + "occurrences of \"" + findString + "\" found." );
        }
        ef.repaint();
    }
} // class end
