package charger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.print.PageFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import charger.EditingChangeState.EditChange;
import charger.exception.CGContextException;
import charger.gloss.AbstractTypeDescriptor;
import charger.obj.Actor;
import charger.obj.Arrow;
import charger.obj.Concept;
import charger.obj.Coref;
import charger.obj.DeepIterator;
import charger.obj.GEdge;
import charger.obj.GNode;
import charger.obj.GenSpecLink;
import charger.obj.Graph;
import charger.obj.GraphObject;
import charger.obj.Note;
import charger.obj.Relation;
import charger.obj.RelationLabel;
import charger.obj.ShallowIterator;
import charger.obj.TypeLabel;
import charger.util.CGUtil;
import chargerlib.FileFormat;
import chargerlib.General;
import chargerlib.GenericTextFrame;
import chargerlib.ManagedWindow;
import chargerlib.WindowManager;
import chargerlib.history.UserHistoryRecord;
import craft.Craft;

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
 * EditFrame is subordinate to the main frame; this allows more than one graph
 * frame to be created. A graph frame is a window that contains any number of
 * graph objects or sub-graphs. All its contents are considered one "CharGer
 * graph" whether all components are connected or not. Manages basic user
 * interface with graphs so that CanvasPanel and EditManager can operate on
 * them. 12-03-02 : trying to get this class free of responsibility for
 * opening/loading file, etc.
 *
 * @author Harry S. Delugach ( delugach@uah.edu ) Copyright (c) 1998-2020 by
 * Harry S. Delugach.
 * @see CanvasPanel
 * @see EditManager
 * @see OperManager
 * @see HubFrame
 */
public class EditFrame extends JFrame
        implements MouseListener, MouseMotionListener, /* ActionListener, */ Runnable, KeyListener, ManagedWindow {


    public EditToolbar editingToolbar = new EditToolbar( this );

    public JTextField messageBox = new JTextField();
    public JPanel messagePanel = new JPanel();
    public JPanel descriptorPanel = new JPanel();
    private JScrollPane dsp = new JScrollPane();
//    public JPanel BlankPanel = new JPanel();	// spacer for toolbar area
    // Save listeners for teardown
    private ComponentListener componentAdapter;
    private WindowAdapter windowAdapter;
    private FocusListener focusAdapter;
//========================================================================================
//	 SIZE AND APPEARANCE OF FRAME
    // static (class) variables
    // distance from used portion of EditFrame and the actual edge of the frame
    public static int borderWidth = Integer.parseInt( Global.Prefs.getProperty( "borderWidth", "4" ) );
    // each EditFrame has this width to start with; is overridden by default and user prefs
    public static int defaultFrameWidth =
            Integer.parseInt( Global.Prefs.getProperty( "defaultFrameWidth", "930" ) );
    public static int defaultFrameHeight =
            Integer.parseInt( Global.Prefs.getProperty( "defaultFrameHeight", "840" ) );
    public static Dimension defaultFrameDim = new Dimension( defaultFrameWidth, defaultFrameHeight );
    // at the bottom of a frame is a status line
    public static int messageBoxHeight =
            Integer.parseInt( Global.Prefs.getProperty( "messageBoxHeight", "25" ) );
    ;
	public static Font messageBoxFont = Global.defaultBoldFont;
    /**
     * The text to be displayed in the status bar of the edit frame
     */
    public static Font borderFont = new Font( "SansSerif", 1, 9 );
    public static Color selectionRectColor = GraphObject.defaultSelectColor;
    // instance variables
    public Color LEDcolor = new Color( 100, 200, 100 );   // to be complemented to make a flashing LED
    public Font currentFont =
            new Font( Global.defaultFont.getName(), Global.defaultFont.getStyle(), Global.defaultFont.getSize() );
    public FontMetrics currentFontMetrics = this.getFontMetrics( currentFont );
    public JPanel changedMarker = new JPanel();

    //========================================================================================
//	 MENUBAR SETUP AND CONTROL
    // the menus themselves, including the menubar
    // all instance variables
    public JMenuBar editFrameMenuBar = new JMenuBar();
    public JMenu fileMenu = new JMenu( Global.strs( "FileMenuLabel" ) );
    public JMenu editMenu = new JMenu( Global.strs( "EditMenuLabel" ), true ); 	// a tear-off menu??
    public JMenu viewMenu = new JMenu( Global.strs( "ViewMenuLabel" ), true ); 	// a tear-off menu??
    public JMenu examineMenu = new JMenu( Global.strs( "ExamineMenuLabel" ), true ); 	// a tear-off menu??
    public JMenu operateMenu = new JMenu( Global.strs( "OperationMenuLabel" ), true ); 	// a tear-off menu??
//    public JMenu GraphModalityMenu = new JMenu( Global.strs( "GraphModalityLabel" ), true ); 	// a tear-off menu??
    public  JMenu changeFontItem = new JMenu( Global.strs( "ChangeFontLabel" ), true );
    public JMenu changeColorMenu = new JMenu( Global.strs( "ChangeColorLabel" ), true );
    public JMenu windowMenu = new JMenu( Global.strs( "WindowMenuLabel" ), true );

    public  JMenuItem BackToHubCmdItem = new JMenuItem( Global.strs( "BackToHubCmdLabel" ) );
    public  JMenuItem UndoItem = new JMenuItem( Global.strs( "UndoLabel" ) );
    public  JMenuItem RedoItem = new JMenuItem( Global.strs( "RedoLabel" ) );

    public JMenu testingItemsMenu = new JMenu( Global.strs( "TestingItemsMenuLabel" ) );
//    public JMenuItem SpringAlgorithmItem = new JMenuItem( Global.strs( "SpringAlgorithmLabel" ) );
//    public JMenuItem SugiyamaAlgorithmItem = new JMenuItem( Global.strs( "SugiyamaAlgorithmLabel" ) );
    public  JMenuItem ChangeTextItem = new JMenuItem( Global.strs( "ChangeTextColorLabel" ) );
    public  JMenuItem ChangeFillItem = new JMenuItem( Global.strs( "ChangeFillColorLabel" ) );
    public  JMenuItem ChangeColorDefaultItem = new JMenuItem( Global.strs( "ChangeColorDefaultLabel" ) );
    public  JMenuItem ChangeColorFactoryItem = new JMenuItem( Global.strs( "ChangeColorFactoryLabel" ) );
    public  JMenuItem ChangeColorBlackAndWhiteItem = new JMenuItem( Global.strs( "ChangeColorBlackAndWhiteLabel" ) );
    public  JMenuItem ChangeColorGrayscaleItem = new JMenuItem( Global.strs( "ChangeColorGrayscaleLabel" ) );
    public  JMenuItem ZoomInItem = new JMenuItem( Global.strs( "ZoomInLabel" ) );
    public  JMenuItem ZoomOutItem = new JMenuItem( Global.strs( "ZoomOutLabel" ) );
    public  JMenuItem ActualSizeItem = new JMenuItem( Global.strs( "ActualSizeLabel" ) );
    public  JMenuItem CurrentSizeItem = new JMenuItem( "(no size yet)" );		// to be set as zooming occurs
    public  JMenuItem FindMenuItem = new JMenuItem( Global.strs( "FindLabel" ) );
    public  JMenuItem FindAgainMenuItem = new JMenuItem( Global.strs( "FindAgainLabel" ) );

    public  JMenuItem ShowHistoryMenuItem = new JMenuItem(Global.strs( "ShowHistory"));
    public  JCheckBoxMenuItem ShowGlossMenuItem = new JCheckBoxMenuItem( Global.strs( "ShowGlossLabel" ) );
//========================================================================================
//	 OPERATIONAL INFORMATION ABOUT THIS FRAME
    // all instance variables
    /**
     * Global unique number assigned to this frame; never used again in this
     * session.
     */
    public int editFrameNum = 0;
    /**
     * basename of graph, not including any suffixes, modalities, etc.
     */
    public String graphName = "Untitled";
    /**
     * graph's modality, as an abbreviation
     */
    //public String PragmaticSense = new String ( Global.GEN_abbr );
    public PragmaticSense purpose = PragmaticSense.NONE;
    /**
     * an absolute or relative path including trailing sep (e.g., "CG/Graphs/" )
     */
    // eventually I want all file references to be in the Java.io.File format
    public File graphSourceFile = new File( graphName );
    public File graphAbsoluteFile = graphSourceFile.getAbsoluteFile();

    public PageFormat pformat = null;	// as a way of making sure we initialize it
    /**
     * The root graph of the edit frame
     */
    public Graph TheGraph = new Graph( null );		// TheGraph is this frame's graph
    public EditManager emgr;	// allocated later so that constructor will work
    public OperManager omgr;	// allocated later so that constructor will work
    public FormatToolbar formatToolbar;

    /**
     * whether anything (including positioning) in the graph has changed
     */
    public boolean somethingHasChanged = false;
    /**
     * whether logical content of the graph has changed
     */
    public boolean contentHasChanged = false;
    /**
     * whether there is anything already selected
     */
    public boolean somethingHasBeenSelected = false;
    /**
     * list containing all selected nodes; maintained as nodes are
     * selected/unselected
     */
    public ArrayList<GraphObject> EFSelectedNodes = new ArrayList<>( 10 );
    /**
     * list containing all selected objects (including edges); maintained as
     * objects are selected/unselected
     */
    public ArrayList<GraphObject> EFSelectedObjects = new ArrayList<>( 10 );
    /**
     * the group to which all this window's updaters belong
     */
    public ThreadGroup threadgroup = new ThreadGroup( "EditFrameNum " + editFrameNum );
//========================================================================================
//	 CANVAS SETUP AND OPERATION
    // static variables
    // Used for Copy/paste operations
    public static int xoffsetForCopy =
            Integer.parseInt( Global.Prefs.getProperty( "xoffsetForCopy", "80" ) );
    public static int yoffsetForCopy =
            Integer.parseInt( Global.Prefs.getProperty( "yoffsetForCopy", "50" ) );
    // instance variables
    /**
     * Panel that contains the actual drawing objects
     */
    public CanvasPanel cp = null;
    /**
     * the pane in which cp is contained, for scrolling purposes only
     */
    public JScrollPane sp = null;
    /**
     * physical rectangle of the selection
     */
    public Rectangle2D.Double selectionRect = null;
    /**
     * the single object under the cursor
     */
    private GraphObject cursorObject = null;
    /**
     * whether to display a rubber band while dragging
     */
    public boolean showRubberBand = false;
    /**
     * to tell routines whether dragging in progress
     */
    private boolean alreadyDragging = false;
    private GraphObject ObjectBeingDragged = null;
    public Point2D.Double dragStartPt = null, dragStopPt = null, dragCurrPt = null;  // used in dealing with dragging
    public Point2D.Double lastMouseClickPoint = null;
    //public boolean		isMouseDown = false;
    /**
     * Whether we're dragging a selection or just dragging for the fun of it.
     */
    public boolean selectionIsBeingDragged = false;
    /**
     * Whether we're dragging the end of a GEdge or not
     */
    public boolean dotIsBeingDragged = false;
    /**
     * Color of an edge whenever we're in the process of an edge being drawn
     */
    public Color lineDragColor;
    /**
     * Scaling factor for this canvas panel. Initialized to be the same as the
     * Global's. Scale factor means the percentage of full-size as displayed on the
     * canvas. For example a scale factor of 0.50 means that the graph will
     * appear to be half its normal size.
     *
     * @see Global#ScalingFactor
     */
    public double canvasScaleFactor = Global.ScalingFactor;
    /**
     * Quantum increment for scaling. Each unit of scale index corresponds to
     * this much scaling factor.
     *
     * @see #scaleIndex
     */
    public static double scaleIncrement = 0.10d;
    /**
     * number of increments to add or subtract from 1. Varies from -9 to 20;
     * these values are to be multiplied by an increment
     *
     * @see EditFrame#scaleIncrement
     */
    public int scaleIndex = (int)Math.round( (double)( canvasScaleFactor - 1.0d ) / (double)scaleIncrement );
//========================================================================================
//	 USER HELP AND OTHER DISPLAYS
    /**
     * Utility windows possibly used by this frame
     */
//    public TextDisplayFrame textFormDisplay = null;
    public GenericTextFrame textFormDisplay = null;
//    public TextDisplayFrame CGIFDisplay = null;
    public GenericTextFrame CGIFDisplay = null;
//    public TextDisplayFrame XMLDisplay = null;
    public GenericTextFrame XMLDisplay = null;
//    public TextDisplayFrame metricsDisplay = null;
    public GenericTextFrame metricsDisplay = null;

//
//========================================================================================
    /**
     * Creates an empty editing window, with an empty graph and untitled
     * filename. Starts this editing window's thread, and requests focus for the
     * drawing panel.
     *
     * @see EditFrame#attachGraphToFrame
     */
    public EditFrame() {
        setup();
        graphSourceFile = new File( Global.GraphFolderFile, "Untitled" );
        WindowManager.manageWindow( this );
        Global.knowledgeManager.addKnowledgeSource( TheGraph );
        if ( emgr.useNewUndoRedo ) {
            emgr.urMgr.resetAndMark();
        }
        repaint();
        requestFocus();
        this.editFrameNum = Global.addEditFrame( this );
    }

    /**
     * Creates a new editing window based on the filename being a suffix-ed CG
     * file. Starts this editing window's thread, and requests focus for the
     * drawing panel.
     *
     * @param graphFile file in which the graph to be edited is supposed to
     * reside
     * @param g a graph to be displayed in the frame, if one already exists in
     * graph form
     * @param needsSaving whether the graph is to be marked as un-saved or not.
     * @see Global
     * @see EditManager
     */
    public EditFrame( File graphFile, Graph g, boolean needsSaving ) {
        // if ( EventQueue.isDispatchThread() ) Global.info( "allocating an edit frame in the dispatch thread" );
        // else Global.info( "allocating an edit frame but NOT in dispatch thread.");
        //setEnabled( false );
        graphSourceFile = graphFile;
        graphAbsoluteFile = graphSourceFile.getAbsoluteFile();
        //Global.info( "setting up edit frame." );
        setup();
        //Global.info( "attaching graph to frame" );
        attachGraphToFrame( graphAbsoluteFile.getPath(), g, needsSaving );	// denoting graph is shown as saved
//        renameGraphInFrame( graphAbsoluteFile.getPath() ); //initEditGraph( filename );
        //Global.info( "source file is " + graphSourceFile.getPath() );
        //Global.info( "absolute file is " + graphAbsoluteFile.getPath() );
        emgr.nothingChanged();		// we didn't really rename a file
        WindowManager.manageWindow( this );
        Global.knowledgeManager.addKnowledgeSource( TheGraph );
        if ( emgr.useNewUndoRedo ) {
            emgr.urMgr.resetAndMark();      // make sure the knowledge source is included in the state
        }
        repaint();
        requestFocus();

        //setEnabled( true );
    }

    protected void finalize() throws Throwable {
        try {
            Global.info( "-- EditFrame " + editFrameNum + " finalizer invoked." );
            closeOut();
            super.finalize();
        } catch ( Throwable t ) {
            throw t;
        } finally {
            super.finalize();
        }
    }


    public void setupComponents() throws Exception {

        messageBox.setForeground( Color.black );
        //messageBox.setLocation(new Point(60, 490));
        messageBox.setVisible( true );
        messageBox.setEditable( false );
        //messageBox.setBackground(new Color(255, 0, 255));
        messageBox.setBackground( Color.white );
        messageBox.setFont( new Font( "Dialog", Font.BOLD, 12 ) );
        messageBox.setOpaque( true );
        messageBox.setToolTipText( "Here's where informative messages and tips will appear." );
        //messageBox.setMinimumSize(new Dimension(500, 25));
        messageBox.setPreferredSize( new Dimension( 500, 50 ) );
        messageBox.setDoubleBuffered( true );
        messageBox.setBorder( Global.BeveledBorder );
        messageBox.setBorder( BorderFactory.createTitledBorder( Global.BeveledBorder,
                Global.EditorNameString + " " + Global.getChargerVersion(), TitledBorder.RIGHT, TitledBorder.TOP,
                new Font( "SansSerif", Font.BOLD + Font.ITALIC, 10 ), Global.chargerBlueColor ) );

        messagePanel.setLayout( new BoxLayout( messagePanel, BoxLayout.Y_AXIS ) );
        //messagePanel.setPreferredSize( messagePanel.getPreferredSize() );

        setLocation( new Point( 5, 5 ) );
        setTitle( "Untitled" + editFrameNum );
        setBackground( Global.chargerBlueColor );
        getContentPane().setLayout( new BorderLayout( 2, 2 ) );
        setSize( new Dimension( 1200, 900 ) );
        //setPreferredSize( new Dimension(450, 750) );

        getContentPane().add( editingToolbar, BorderLayout.WEST );

        messagePanel.add( messageBox );
        getContentPane().add( messagePanel, BorderLayout.SOUTH );


        addComponentListener( new ComponentAdapter() {
            public void componentResized( ComponentEvent e ) {
                thisComponentResized( e );
            }
        } );

        addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                thisWindowClosing( e );
            }

            public void windowActivated( WindowEvent e ) {
                thisWindowActivated();
            }

            public void windowOpened( WindowEvent e ) {
                thisWindowOpened( e );
            }

            public void windowDeactivated( WindowEvent e ) {
                thisWindowDeactivated( e );
            }
        } );

        addFocusListener( new FocusAdapter() {
            public void focusGained( FocusEvent e ) {
                thisFocusGained( e );
            }

            public void focusLost( FocusEvent e ) {
                thisFocusLost( e );
            }
        } );



        getContentPane().setBackground( Global.chargerBlueColor );

        // set up menu items here
        UndoItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_Z, Global.AcceleratorKey ) );
        RedoItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_Z, Global.AcceleratorKey | InputEvent.SHIFT_MASK ) );
    }
    private boolean mShown = false;

    /**
     * Attaches a graph to an edit frame, using the filename as a guide. Assumes
     * that the starting graph is already set up (i.e., non-null).
     *
     * @param filename String to use in setting up the frame's title, etc.
     * @param startingGraph A ready-to-use CharGer graph
     * @param needsSaving Whether the resulting window thinks the graph has
     * changed since saving
     *
     */
    public void attachGraphToFrame( String filename, Graph startingGraph, boolean needsSaving ) {
        setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        TheGraph = startingGraph;
        TheGraph.setOwnerFrame( this );
        renameGraphInFrame( filename );
        if ( !needsSaving ) {
            emgr.nothingChanged();
        } else {
            emgr.setChangedContent( EditChange.SEMANTICS  );
        }
        toFront();  // Global.info( "to front in attach graph to frame" );

        if ( !emgr.useNewUndoRedo ) {
            emgr.resetUndo();
            emgr.makeHoldGraph();
        }
        setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );

    }
    Thread editFrameThread = Thread.currentThread();

    public /*synchronized*/ void run() {
        Thread myThread = Thread.currentThread();
        //cp.requestFocus(); //commented 12-13-02
        Global.info( "====== START OF EditFrame " + Thread.currentThread() );
        while ( editFrameThread == myThread ) {
            //repaint();
            try {
                Global.info( "in wait loop " + Thread.currentThread() );
                myThread.wait( 1000 );
            } catch ( InterruptedException e ) {
                // the VM doesn't want us to sleep anymore,
                // so get back to work
            }
        }
        Global.info( "======== END OF EditFrame " + Thread.currentThread() );
    }

    /**
     * Sets the editFrameThread to null.
     */
    public void requestToStop() {
        editFrameThread = null;
    }

    /**
     * Handles close events on this frame. Invokes closeOut() to see if user
     * wants to save, etc., if not, then makes frame visible again.
     */
    public void thisWindowClosing( WindowEvent e ) {
        Global.info( "window closing on edit frame " + editFrameNum );
        if ( !closeOut() ) {
            setVisible( true );
            pack();
            validate();
        } else {
//            WindowListener list[] = getWindowListeners();
//            for ( int k = 0; k < list.length; k++ ) {
//                removeWindowListener( list[k] );
//            }
//
//            if ( omgr != null ) {
//                omgr.ef = null;
//                omgr = null;
//            }
//
//            if ( emgr != null ) {
//                emgr.ef = null;
//                emgr = null;
//            }
//
//            if ( TheGraph != null ) {
//                TheGraph.setOwnerFrame( null );
//                TheGraph = null;
//            }
//            EFSelectedNodes = null;
//            EFSelectedObjects = null;
//            threadgroup = null;
        }
    }

    public void thisWindowActivated() {
        if ( Global.getCurrentEditFrame() != this ) {
            Global.setCurrentEditFrame( this );
        }

        // remake the window menu
//        WindowManager.refreshWindowMenuList( windowMenu, this );	// "this" here seems important..
        emgr.setEditMenuItems();
        //repaint();
        setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
        setEnabled( true );
    }

    public void thisWindowOpened( WindowEvent e ) {
    }

    public void thisWindowDeactivated( WindowEvent e ) {
    }

    /**
     * Called by both constructors; for convenience. Sets up the frame and its
     * listener(s), sets up the mode and canvas panels, creates the message box,
     */
    public void setup() {
        setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );

        Global.addGraph( TheGraph );
        try {
            setupComponents();
        } catch ( Exception goodbye ) {
            Global.error( "Couldn't initialize EditFrame: " + goodbye.getMessage() );
        }

        TheGraph.setOwnerFrame( this );
        TheGraph.setTextLabel( "" );
        TheGraph.setDisplayRect( new Rectangle2D.Double( getLocation().x, getLocation().y, getSize().width, getSize().height ) );
        TheGraph.setTextLabelPos();
        setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
        setSize( defaultFrameDim );
        emgr = new EditManager( this );
        omgr = new OperManager( this );
        formatToolbar = new FormatToolbar( emgr );
        getContentPane().add( formatToolbar, BorderLayout.NORTH );

        // hsd 07-30-20
        setLocation( 100, 100 );

        // the order here matters, unfortunately, so be careful!!
        setupCanvasScrollPane();
        setupDescriptorPanel();
        setupMenus();
        refreshBorders();
        editingToolbar.setMode( EditToolbar.Mode.Select );


        setEditFrameSizes();
        // Global.info( "about to set up menu items" );
        setMenuItems();

        TheGraph.setTextLabel( "Proposition" );


        setVisible( true );
        formatToolbar.setForNoSelection();
        emgr.nothingChanged();
        repaint();
        addKeyListener( this );
        setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
        // Global.info( "finished EditFrame's setup()" );
        //cp.requestFocus();		// commented 12-11-02
        // Global.info("after cp gets focus, drawing toolbar size is " + drawingToolbar.getSize().toString() );

    }

    /**
     * Break down the edit frame and set things to null.
     * This may help garbage collection.
     */
    public void tearDown() {

        if ( TheGraph != null ) {
            TheGraph.setOwnerFrame( null );
        }

        if ( Global.getCurrentEditFrame() == this ) {
            Global.setCurrentEditFrame( null );
        }

        removeComponentListener( componentAdapter );
        componentAdapter = null;

        ComponentListener compListeners[] = getComponentListeners();
        for ( int i = 0; i < compListeners.length; i++ ) {
            Global.consoleMsg( "Removing compListener: " + compListeners[i] );
            removeComponentListener( compListeners[i] );
        }

        removeWindowListener( windowAdapter );
        windowAdapter = null;

        WindowListener winListeners[] = getWindowListeners();
        for ( int i = 0; i < winListeners.length; i++ ) {
            Global.consoleMsg( "Removing windowListener: " + winListeners[i] );
            removeWindowListener( winListeners[i] );
        }

        removeFocusListener( focusAdapter );
        focusAdapter = null;

        FocusListener focusListeners[] = getFocusListeners();
        for ( int i = 0; i < focusListeners.length; i++ ) {
            Global.consoleMsg( "Removing focusListener: " + focusListeners[i] );
            removeFocusListener( focusListeners[i] );
        }

        currentGraph = null;

        purpose = null;

        removeKeyListener( this );

        while ( editFrameMenuBar.getMenuCount() > 0 ) {
            JMenu menu = editFrameMenuBar.getMenu( 0 );
            General.tearDownMenu( menu );
            editFrameMenuBar.remove( menu );
            menu = null;
        }

        removeAll();

        System.gc();
    }


    private void setupCanvasScrollPane() {
        // ________ set up canvas panel _________
        cp = new CanvasPanel( this );
        cp.addMouseListener( this );
        cp.addMouseMotionListener( this );

        sp = new JScrollPane( cp );
        sp.setPreferredSize( new Dimension( 725, 625 ) );
        //sp.setBackground( Color.magenta );
        sp.setOpaque( true );
        sp.setSize( new Dimension( 640, 520 ) );

        sp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS );
        sp.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
        sp.getHorizontalScrollBar().setBackground( Color.white );
        sp.getVerticalScrollBar().setBackground( Color.white );
        sp.setBorder( Global.BeveledBorder );

        // a little colored square at lower right corner of scroll pane
        changedMarker.setSize( 8, 8 );
        sp.setCorner( JScrollPane.LOWER_RIGHT_CORNER, changedMarker );

        // inform current EditFrame that the scrollpane is ready
        getContentPane().add( sp, BorderLayout.CENTER );


        setEditFrameSizes();

                // Disable the arrow keys for the scrollpane. Fixed issue #1
        sp.getActionMap().put( "unitScrollLeft", new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
            }
        } );
        sp.getActionMap().put( "unitScrollUp", new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
            }
        } );
        sp.getActionMap().put( "unitScrollRight", new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
            }
        } );
        sp.getActionMap().put( "unitScrollDown", new AbstractAction() {
            @Override
            public void actionPerformed( ActionEvent e ) {
            }
        } );
    }

    /**
     * Adds menus to the menu bar. Actual menu contents are initialized by the
     * tool panel's constructor (bad design!)
     */
    private void setupMenus() {
        setJMenuBar( editFrameMenuBar );
        editFrameMenuBar.add( fileMenu );
        editFrameMenuBar.add( editMenu );
        editFrameMenuBar.add( editingToolbar.getMenu());
        editFrameMenuBar.add( viewMenu );
        if ( !Global.OfficialRelease ) {
            editFrameMenuBar.add( examineMenu );
        }
        editFrameMenuBar.add( operateMenu );
        editFrameMenuBar.add( windowMenu );
        editFrameMenuBar.add( Global.CharGerMasterFrame.getHelpMenu() );

    }

    public void windowMenuActionPerformed() {
//        WindowManager.refreshWindowMenuList( windowMenu, this );
    }

    /**
     * Tell the EditManager and OperManager to examine menus and gray out items
     * as needed
     */
    public void setMenuItems() {
        emgr.setMenuItems();        // Why does undo sometimes raise a null pointer here?
        omgr.setMenuItems();
    }

    /**
     * Only bothers with the toolbar for now...
     */
    public void setEditFrameSizes() {
        //drawingToolbar.setSize( new Dimension( 40, 480 ) );
    }

    /**
     * Refreshes the top border containing the scale factor for the graph, the
     * count for each kind of graph node, and an indication of whether actors
     * are active.
     */
    public void refreshBorders() {
        if ( TheGraph == null ) {
            return;
        }
        sp.setBorder( BorderFactory.createTitledBorder( Global.BeveledBorder,
                " " + Math.round( canvasScaleFactor * 100 ) + " %     "
                + TheGraph.getBriefSummary() + "   ACTIVE: " + Global.enableActors + " ",
                TitledBorder.CENTER,
                TitledBorder.TOP, borderFont, Global.chargerBlueColor ) );
        //sp.repaint();
    }

    public void thisFocusGained( FocusEvent e ) {
        //refresh();
        //Global.info( "focus gained on " + identifyComponent( this ) + " from " + identifyComponent( e.getOppositeComponent() ) );
        //requestFocus();		// commented 12-16-02
    }

    public void thisFocusLost( FocusEvent e ) {
    }

    private String identifyComponent( Component c ) {
        if ( c instanceof EditFrame ) {
            return c.getClass().getName() + ( (EditFrame)c ).editFrameNum;
        } else if ( c instanceof CanvasPanel ) {
            return c.getClass().getName() + "of edit frame " + ( (CanvasPanel)c ).ef.editFrameNum;
        } else {
            return c.toString();
        }
    }

    /**
     * Handles key-presses while on the canvas.
     * The following keys are handled by this method: up,down,left,right arrow keys (with shift moves selection faster); plus, minus to expand
     * or contract; delete key.<br>Any other keystrokes are passed to the toolbar.
     *
     */
    public void keyPressed( KeyEvent e ) {
        boolean b;
        clearStatus();
//        Global.info( "key pressed " + e );
        int key = e.getKeyCode();
        int delta = 1;
        if ( e.isShiftDown() ) {
            delta = 4;	// if we're moving with the arrow keys, or expanding/contracting, move  faster with the shift key
        }
        if ( e.isControlDown() ) {
            return;
        }
        if ( e.isAltDown() ) {
            return;
        }
        if ( e.isMetaDown() ) {
            return;
        }

        switch ( key ) {
            case KeyEvent.VK_LEFT:
                moveSelectedObjects( TheGraph, new Point2D.Double( -1 * delta, 0 ) );
                break;
            case KeyEvent.VK_RIGHT:
                moveSelectedObjects( TheGraph, new Point2D.Double( +1 * delta, 0 ) );
                break;
            case KeyEvent.VK_DOWN:
                moveSelectedObjects( TheGraph, new Point2D.Double( 0, +1 * delta ) );
                break;
            case KeyEvent.VK_UP:
                moveSelectedObjects( TheGraph, new Point2D.Double( 0, -1 * delta ) );
                break;


            case KeyEvent.VK_SUBTRACT:
            case KeyEvent.VK_MINUS:
                b = emgr.reduceSelection( delta, true );
                repaint();
                break;
            case KeyEvent.VK_EQUALS:
            case KeyEvent.VK_ADD:
            case KeyEvent.VK_PLUS:
                b = emgr.enlargeSelection( delta, true );
                repaint();
                break;

            case KeyEvent.VK_DELETE:
                emgr.performActionDeleteSelection();
                repaint();
                break;
            default:
                editingToolbar.shortcutKeys( key );
        }
    }

    public void keyReleased( KeyEvent e ) {
        // Global.info( "edit frame's keyreleased" );
    }

    public void keyTyped( KeyEvent e ) {
        //Global.info( "edit frame's keytypedf" );
    }


    public void setGraphPragmatics( PragmaticSense mode ) {
            purpose = mode;
    }
    /**
     * used so that mousepressed and mousereleased can communicate
     */
    GraphObject currentObject = null;
    /**
     * used so that mousepressed and mousereleased can communicate
     */
    GNode currentNode = null;
    /**
     * used so that mousepressed and mousereleased can communicate
     */
    GEdge currentEdge = null;
    /**
     * used so that mousepressed and mousereleased can communicate
     */
    Graph currentGraph = TheGraph;

    /**
     * Convert a physical screen point into its logical scaled point in the
     * context. Primarily useful for mouse-oriented operations.
     *
     * @param p The actual point on the canvas ( relative to the canvas panel's
     * 0,0 point
     */
    public Point2D.Double scaled( Point2D.Double p ) {
        return new Point2D.Double( (int)( p.x / canvasScaleFactor ), (int)( p.y / canvasScaleFactor ) );
    }

    /**
     * Convert a logical point of the canvas's graphics context into its
     * physical location in the panel.
     *
     * @param p The actual point on the canvas ( relative to the canvas panel's
     * 0,0 point
     */
    public Point2D.Double antiscaled( Point2D.Double p ) {
        return new Point2D.Double(  p.x * canvasScaleFactor ,  p.y * canvasScaleFactor  );
    }

    /**
     * Convert a logical rectangle of the canvas's graphics context into its
     * physical rectangle in the panel.
     *
     * @param r The actual rectangle on the canvas ( relative to the canvas
     * panel's 0,0 point
     */
    public Rectangle2D.Double antiscaled( Rectangle2D.Double r ) {
        Point2D.Double p = new Point2D.Double( r.x, r.y );
        Rectangle2D.Double rect = new Rectangle2D.Double();
        rect.setFrameFromCenter( CGUtil.getCenter( rect ), new Point2D.Double( antiscaled( p ).x, antiscaled( p ).y ) );
        Point2D.Double lowerRight = antiscaled( new Point2D.Double( r.x + r.width, r.y + r.height ) );
        rect.setFrame( rect.x, rect.y, lowerRight.x - rect.x, lowerRight.y - rect.y );
        return rect;
    }

    /**
     * Handles the operations needed when the mouse is pressed. Choose the
     * appropriate object (if any) and set variables for mouse released to use.
     * Just for processing a mouse being pressed, no dragging or release yet.
     * Needs to be fast, so we can get it out of the way for real dragging
     * actions. Purpose is to decide what's being selected or chosen for
     * operation. Some operations only require a mouse released to operate
     * (e.g., edit text)
     *
     * @see EditFrame#mouseReleased
     */
    public void mousePressed( MouseEvent me ) {
        // no matter what, we want to know the scaled position, not the absolute one
        Point2D.Double ThisPt = scaled( new Point2D.Double( me.getPoint().x, me.getPoint().y ) );


        boolean shiftDown = me.isShiftDown();
        boolean rightClick = me.getButton() == MouseEvent.BUTTON3;
        // Global.info( "mouse pressed; right click is " + rightClick );
        String objectTextLabel = null;
        if ( !me.isShiftDown() ) {
            clearStatus();
            currentObject = null;
            currentNode = null;
            currentEdge = null;
            currentGraph = null;
            cursorObject = null;
            objectTextLabel = null;
        }

        // should reorganize and do a mousePressed and then a mouseRelease
        dragStartPt = new Point2D.Double( ThisPt.x, ThisPt.y );
        // Global.info( "drag start pt " + dragStartPt );

        if ( !cp.contains( (int)ThisPt.x, (int)ThisPt.y ) ) {
            cp.closeTextEditors();		// just in case
            displayOneLiner( "Are you clicking outside the canvas?" );
            //repaint();
            return;
        }

        if ( cp.nodeEditingDialog != null ) cp.closeTextEditors();
        // determine the innermost context of the point (may still be TheGraph outermost)
        currentGraph = EditManager.innermostContext( TheGraph, ThisPt );

        //currentObject = (GraphObject)insideGNode( ThisPt );	// changed 04-04-03
        currentObject = onTopOfGNode( ThisPt );

        if ( currentObject == null ) {
            currentObject = insideGEdge( ThisPt );
        }

        if ( currentObject == TheGraph ) {
            currentObject = null;		// it is prohibited (for now) to select the outermost graph
        }

        if ( !emgr.useNewUndoRedo ) {
            emgr.makeHoldGraph();
        }

        if ( ( currentObject != null ) ) {
            objectTextLabel = CGUtil.shortClassName( currentObject ) + " \"" + currentObject.getTextLabel() + "\"";
            if ( Global.wordnetEnabled && currentObject instanceof GNode
                    && emgr.showGloss
                    && ( (GNode)currentObject ).getTypeDescriptor() != null ) {
                objectTextLabel = objectTextLabel + //" " + ((GNode)currentObject).getTypeDescriptor().getTrimmedString();
                        " - " + ( (GNode)currentObject ).getTypeDescriptor().toString();
                //charger.Global.info( ((GNode)currentObject).getTypeDescriptor().toXML( "  " ) );
                // HERE is where we should display the descriptor table
                refreshDescriptorPanel( ( (GNode)currentObject ).getTypeDescriptors() );
            }
            cursorObject = currentObject;
            // determine what (if any) object the user clicked on
            if ( currentObject.myKind == GraphObject.Kind.GNODE ) {
                currentNode = (GNode)currentObject;
            } else if ( currentObject.myKind == GraphObject.Kind.GEDGE ) {
                currentEdge = (GEdge)currentObject;
            } else if ( currentObject.myKind == GraphObject.Kind.GRAPH ) {
                currentNode = (GNode)currentObject;
            }


            if ( editingToolbar.isNodeInsertMode() ) {
                if ( currentNode.myKind != GraphObject.Kind.GRAPH ) {
                    displayOneLiner( " Sorry. There is another object already in this spot." );
                }
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Select ) {	// current node is to be selected; don't worry about dragging a selection yet
                if ( currentNode != null ) {	// process a right click before worrying about whether it's selected or not
                    if ( rightClick ) {
                        // Here is where we handle right clicking on currentNode!!  12-20-07
                        if ( currentObject instanceof Actor ) {
                            displayOneLiner( "You've right-clicked an actor. Congratulations." );
                        } else {
                            displayOneLiner( "Sorry, only actors can be right-clicked at this time." );
                        }
                    } else if ( currentNode.isSelected ) {
                        if ( shiftDown ) {
                            takeFromSelection( currentNode );
                        }
                        // if selecting a context, display informative editing msg disabled 07-13-03
                        //if ( currentNode.myKind != GraphObject.GRAPH )
                        displayOneLiner( objectTextLabel );
                        //else
                        //    displayOneLiner( objectTextLabel +
                        //	" - double-click on context BORDER to edit context name" );
                    } else // current node not selected; if shift key, then de-select all before selecting
                    {
                        if ( !shiftDown ) {
                            resetSelection();
                        }
                        addToSelection( currentNode );
                        // if selecting a context, display informative editing msg - disabled 7-13-03
                        //if ( currentNode.myKind != GraphObject.GRAPH )
                        displayOneLiner( objectTextLabel );
                        //else
                        //    displayOneLiner( objectTextLabel +
                        //	" - double-click on context BORDER to edit context name" );
                    }
                }
            }
        } else { // no object was selected or it's within a context but still not any object
            // if adding something, go ahead and add it to TheGraph; putInCorrectContext will fix
            if ( editingToolbar.getMode() == EditToolbar.Mode.Delete ) {
                resetSelection();
                displayOneLiner( "Click on something you want to delete. To delete a context, use its BORDER." );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Select ) {
                if ( !shiftDown ) {
                    resetSelection();
                    displayOneLiner( "Click on something you want to select. To select a context, use its BORDER." );
                }
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Arrow ) {
                resetSelection();
                displayOneLiner( "Drag the cursor between concepts/actors/relations/context-border in a line drawing mode." );
                // supposed to be dragging in this ToolMode
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Note ) {
                resetSelection();
                displayOneLiner( "Click on a concept, relation, actor, context-border, or type name to edit its text." );
            }
        }
        setMenuItems();

    }

    /**
     * Handles any actions where the mouse is released. This includes clicks.
     * mouseclicked() is really most useful for double clicks..
     *
     * @see #mouseClicked
     * @see #stopDragging
     * @param me
     */
    public void mouseReleased( MouseEvent me ) {
        //isMouseDown = false;
        // Global.info( "mouse released; current object is " + currentObject );
        Point2D.Double ThisPt = scaled( new Point2D.Double( me.getPoint().x, me.getPoint().y ) );
        if ( alreadyDragging ) {
            dragStopPt = ThisPt;
            stopDragging( dragStartPt, dragStopPt, me.isShiftDown() );
            currentNode = null;
            currentEdge = null;
            currentGraph = null;
            currentObject = null;
            return;
        }

        dragStartPt = null;
        dragStopPt = null;

        if ( currentObject != null ) {
            // an existing GNode was clicked on, either just clicked or we just finished dragging it
            if ( editingToolbar.getMode() == EditToolbar.Mode.Note ) {
                if ( currentNode != null ) {
                    cp.userStartedEditingText( currentNode, ThisPt );
                } else if ( currentEdge != null ) {
                    if ( currentEdge instanceof Arrow ) // if edge, then only arrows can have an edited label
                    {
                        cp.userStartedEditingText( currentEdge, ThisPt );
                    } else {
                        displayOneLiner( "Only actor/relation to graph/context arrows can have labels." );
                    }
                }
            } else if ( editingToolbar.isNodeInsertMode() ) {
                if ( currentNode.myKind != GraphObject.Kind.GRAPH ) {
                    displayOneLiner( " Sorry. There is another object already in this spot." );
                }
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Delete ) {
                EditingChangeState changeState = new EditingChangeState( EditChange.APPEARANCE, EditChange.SEMANTICS, EditChange.UNDOABLE );
                if ( currentNode != null ) {        // User wants to delete a node
                    if ( currentNode instanceof Note )
                        changeState = new EditingChangeState( EditChange.APPEARANCE, EditChange.UNDOABLE );
                    currentNode.getOwnerGraph().forgetObject( currentNode );
                }
                if ( currentEdge != null ) {    // user wants to delete an edge
                    currentEdge.getOwnerGraph().forgetObject( currentEdge );
                }
                emgr.setChangedContent( changeState );
            }
            lastMouseClickPoint = null;
        } else { // no object was selected or it's within a context but still not any object
            // if adding something, go ahead and add it to TheGraph; putInCorrectContext will fix
            showRubberBand = false;
            somethingHasBeenSelected = false;
            // no existing object was clicked upon
            if ( editingToolbar.getMode() == EditToolbar.Mode.Relation ) {
                currentNode = new Relation();
                currentGraph.insertObject( currentNode );
                currentNode.setCenter( ThisPt );
                emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Concept ) {
                currentNode = new Concept();
                currentGraph.insertObject( currentNode );
                currentNode.setCenter( ThisPt );
                emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Actor ) {
                currentNode = new Actor();
                currentGraph.insertObject( currentNode );
                currentNode.setCenter( ThisPt );
                emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.TypeLabel ) {
                currentNode = new TypeLabel();
                currentGraph.insertObject( currentNode );
                currentNode.setCenter( ThisPt );
                emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.RelationLabel ) {
                currentNode = new RelationLabel();
                currentGraph.insertObject( currentNode );
                currentNode.setCenter( ThisPt );
                emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Delete ) {
                resetSelection();
                displayOneLiner( "Click on something you want to delete. To delete a context, use its BORDER." );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Select ) {
                resetSelection();
                displayOneLiner( "Click on something you want to select. To select a context, use its BORDER." );
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Arrow ) {
                resetSelection();
                displayOneLiner( "Drag the cursor between concepts, actors, relations and context-border in a line drawing mode." );
                // supposed to be dragging in this ToolMode
            } else if ( editingToolbar.getMode() == EditToolbar.Mode.Note ) {
                currentNode = new Note();
                currentGraph.insertObject( currentNode );
                currentNode.setCenter( ThisPt );
                emgr.setChangedContent( EditChange.UNDOABLE  );
            }
            // add a note in the history of this object
            if ( currentNode != null ) {
                UserHistoryRecord he = new UserHistoryRecord( currentNode, System.getProperty( "user.name") );
                he.appendDescription( "User created object.");

                currentNode.addHistoryRecord( he );
            }
        }

        setMenuItems();
        //Global.info( summarizeSelection() );

        // sp.repaint();  	// eliminate??
        setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
    }

    /**
     * Handles actual mouse clicks, noting that most work is done by press and
     * release.
     *
     * @see EditFrame#mouseReleased
     * @see EditFrame#mouseClicked
     */
    public void mouseClicked( MouseEvent me ) {
        //Global.info( "mouse clicked" );
        if ( me.getClickCount() == 2 ) {
            doDoubleClick( me );
            return;
        }
        lastMouseClickPoint = scaled( new Point2D.Double( me.getPoint().x, me.getPoint().y ) );
    }

    /**
     * Processes 2-click events; called by mouseClicked.
     */
    public void doDoubleClick( MouseEvent me ) {
        Point2D.Double ThisPt = scaled( new Point2D.Double( me.getPoint().x, me.getPoint().y ) );

        if ( !cp.contains( new Point( (int)ThisPt.x, (int)ThisPt.y ) ) ) {
            displayOneLiner( "Why are you double-clicking outside the canvas?" );
            return;
        }
        if ( currentNode != null ) {
            if ( !emgr.useNewUndoRedo ) {
                emgr.makeHoldGraph();
            }
            cp.userStartedEditingText( currentNode, ThisPt );
        }
        if ( currentEdge != null ) {
            if ( !emgr.useNewUndoRedo ) {
                emgr.makeHoldGraph();
            }
            cp.userStartedEditingText( currentEdge, ThisPt );
        }
    }

    public void mouseEntered( MouseEvent me ) {
        //Global.setCurrentEditFrame(this);  // tell the master that I'm current
    }

    public void mouseExited( MouseEvent me ) {
        //Global.removeMeAsCurrentEditFrame(this); // tell the master that I'm outta here
    }

    /**
     * Performed while the mouse is being dragged. Handles moving selections and
     * drawing edges between nodes.
     * @see #mouseReleased
     */
    public void mouseDragged( MouseEvent me ) {
        Point2D.Double p = scaled( new Point2D.Double( me.getPoint().x, me.getPoint().y ) );
        dragCurrPt = p;
        GraphObject go;
        String objectTextLabel = null;
        if ( !alreadyDragging ) {
            // This section done once per drag, if we have just started dragging.
            //Global.info( "mouse dragged" );
            showRubberBand = false;
            if ( dragStartPt == null ) {
                dragStartPt = p;
            }
            go = onTopOfGNode( dragStartPt );
            alreadyDragging = true;
            if ( editingToolbar.getMode() == EditToolbar.Mode.Select || editingToolbar.getMode() == EditToolbar.Mode.Delete ) {
                if ( go == null ) // no object is being dragged
                {
                    if ( !me.isShiftDown() ) { // starting this selection
                        resetSelection();
                    }
                    somethingHasBeenSelected = true;
                    showRubberBand = true;
                } else // we're really starting to drag an object
                {
                    if ( !go.isSelected ) {
                        resetSelection(); // if not selected already, select it
                        if ( ! emgr.useNewUndoRedo ) emgr.makeHoldGraph();
                        addToSelection( go );
                        somethingHasBeenSelected = true;	// not quite sure what this does.....???
                        ObjectBeingDragged = go;
                        setMenuItems();
                    }
                    objectTextLabel = CGUtil.shortClassName( go ) + " \"" + go.getTextLabel() + "\"";
                    cursorObject = go;
                    showRubberBand = false;
                    displayOneLiner( "moving " + objectTextLabel + "." );
                    selectionIsBeingDragged = true;
                    sp.repaint();
                }
            } else { // one of the edge drawing modes...
                if ( go == null ) {
                    ObjectBeingDragged = null;
                    objectTextLabel = "no object";
                    displayOneLiner( "To draw an edge, please indicate the starting object." );
                    sp.repaint();
                } else {
                    dotIsBeingDragged = true;
                    if ( editingToolbar.getMode() == EditToolbar.Mode.Arrow ) {
                        lineDragColor = Global.getDefaultColor( "Arrow", "fill" );
                    } else if ( editingToolbar.getMode() == EditToolbar.Mode.Coref ) {
                        lineDragColor = Global.getDefaultColor( "Coref", "fill" );
                    } else if ( editingToolbar.getMode() == EditToolbar.Mode.GenSpecLink ) {
                        lineDragColor = Global.getDefaultColor( "GenSpecLink", "fill" );
                    }
                    //Global.info( "is dragging a dot, starting at " + p );
                    sp.repaint();
                }
            }
        } else {  // we're already in the process of dragging

            if ( showRubberBand ) {
                getGraphics().setColor( selectionRectColor );
                selectionRect = CGUtil.adjustForCartesian( dragStartPt.x, dragStartPt.y, p.x, p.y );
                somethingHasBeenSelected = true;
                sp.repaint();
            }
            if ( selectionIsBeingDragged ) {
                sp.repaint();
            }
            if ( dotIsBeingDragged ) {
                sp.repaint();
            }
        }
    }

    /**
     * Do whatever gets done by dragging; performed when dragging has stopped.
     * Does not matter the relative orientation of the two points on the
     * coordinate plane.
     *
     * There's a bug in moving nested contexts. Need to move all the context's
     * inner contents BEFORE determining its new display rect.
     *
     * @param dragStartPt the point at which the user started to drag
     * @param dragStopPt the point at which the user stopped the dragging
     * @param shiftDown whether the user is holding the shift key when the drag
     * stops.
     */
    public void stopDragging( Point2D.Double dragStartPt, Point2D.Double dragStopPt, boolean shiftDown ) {
        //Global.info( "stop dragging" );
				/*
         Change: 07-01-03 : alterations so that an entire selection is
         inserted into whatever context the user drags to.
         */
        GraphObject go1, go2;
        alreadyDragging = false;
        selectionIsBeingDragged = false;
        dotIsBeingDragged = false;
        clearStatus();
                if ( ! emgr.useNewUndoRedo ) emgr.makeHoldGraph();
        // start processing modes
        //   if one of the edge-drawing modes... ignore other edges
        if ( editingToolbar.isEdgeInsertMode() ) {
            go1 = onTopOfGNode( dragStartPt );
            go2 = onTopOfGNode( dragStopPt );
            if ( go1 == go2 ) {
                return;
            }
            // perhaps not necessary, since the drag start checks for a null object
            if ( go1 == null || go2 == null ) {
                displayOneLiner( "To draw an edge, drag between two objects." );
            } else {  // really did drag between two nodes; let's process each one...
                GEdge Aobj = null;
                // do we need to draw an arrow? are we allowed?
                if ( editingToolbar.getMode() == EditToolbar.Mode.Arrow ) {
                    // arrow can only link concept or graph to relation or actor
                    String usermsg = emgr.arrowAllowed( go1, go2 );
                    if ( usermsg == null ) {
                        Aobj = new Arrow( go1, go2 );
                        //emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE ) );
                    } else {
                        JOptionPane.showMessageDialog(
                                this, usermsg, "CG Rule not followed", JOptionPane.ERROR_MESSAGE );
                    }
                } // if asked to draw a coref link, are we allowed?
                else if ( editingToolbar.getMode() == EditToolbar.Mode.Coref ) {
                    String usermsg = emgr.corefAllowed( go1, go2 );
                    if ( usermsg == null ) {
                        Aobj = new Coref( go1, go2 );
                        //emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE ) );
                    } else {
                        JOptionPane.showMessageDialog(
                                this, usermsg, "CG Rule not followed", JOptionPane.ERROR_MESSAGE );
                    }
                } // if asked to draw a type link, are we allowed?
                else if ( editingToolbar.getMode() == EditToolbar.Mode.GenSpecLink ) {
                    String usermsg = emgr.genspecAllowed( go1, go2 );
                    if ( usermsg == null ) {
                        Aobj = new GenSpecLink( go1, go2 );
                        //emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE ) );
                    } else {
                        JOptionPane.showMessageDialog(
                                this, usermsg, "CG Rule not followed", JOptionPane.ERROR_MESSAGE );
                    }
                }
                // if we passed at least one test, then add the object!
                if ( Aobj != null ) {
                    // NEED to check: if arrow crosses context boundaries, insert into inner one
                    // if not relative to each other, then insert into the deepest graph that encloses both
                    TheGraph.insertObject( Aobj );
                    emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
                }
            }
        } else // not one of the edge-drawing modes, maybe we moved something
        if ( editingToolbar.getMode() == EditToolbar.Mode.Select ) {
            go1 = onTopOfGNode( dragStartPt );
            if ( go1 == null ) {
                // no object dragged, must be making a selection
                selectionRect = CGUtil.adjustForCartesian( dragStartPt, dragStopPt );
                setSelectedGraphObjects( TheGraph, selectionRect, shiftDown );
            } else {
                // object go1 was dragged, so we must have intended to move something
                //  handle moving a graph here; show all its contents as selected
                // if a graph, then set its display rect to the selected rect using setSelectedGraphObjects
                if ( go1.isSelected ) { // a selection was dragged
                    // Global.info( "stopped dragging. isSelected " + go1.isSelected );
                    Point2D.Double delta = new Point2D.Double( dragStopPt.x - dragStartPt.x, dragStopPt.y - dragStartPt.y );
                    moveSelectedObjects( TheGraph, delta );
                    // emgr.setChangedContent(  true ); 	// done inside the translate
                } else {	// ...not clear why we'd ever be dragging something that wasn't selected
                    Global.error( "dragging something that wasn't selected???" );
                    ( (GNode)go1 ).setCenter( dragStopPt );
                }
            }
        }
        if ( somethingHasChanged ) {
        }
        dragStartPt = null;
        dragStopPt = null;
        lastMouseClickPoint = null;
        setMenuItems();
        sp.repaint();
        //Global.info( "end of stop and do drag." );
    }

    /**
     * Track the mouse's movements. Sets the cursor to the move cursor when the
     * mouse is able to select an object (edge, graph included).
     *
     * @param me used for determining where the mouse is.
     */
    public void mouseMoved( MouseEvent me ) {
        //Global.info( "mouseMoved");
        Point2D.Double p = scaled( new Point2D.Double( me.getPoint().x, me.getPoint().y ) );
        dragCurrPt = p;
        GraphObject go = onTopOfObject( p );
        //if ( go instanceof Graph )
        if ( go != null ) {
            setMyCursor();
            if ( go instanceof Graph ) {
                cp.setToolTipText( "click border to select, double-click border to edit, or drag border to move" );
            } else {
                cp.setToolTipText( null );
            }
            //displayOneLiner( "on graph border" );
        } else {
            cp.setToolTipText( null );
            setCursor( new Cursor( Cursor.DEFAULT_CURSOR ) );
            //displayOneLiner( "" );
        }

    }

    /**
     * Determine whether an object's display rectangle contains a point on the
     * canvas. A context is treated just as a (possibly large) rectangle
     * boundary.
     *
     * @param p the point on the canvas
     * @return the object if there is one, null otherwise
     * @see EditFrame#insideGNode
     */
    public GraphObject onTopOfObject( Point2D.Double p ) {
        Iterator<GraphObject> iter = new DeepIterator( TheGraph );
        GraphObject go;
        while ( iter.hasNext() ) {  // does not check for object type here!
            go = iter.next();
            if ( go == null ) {
                Global.info( "Nullptr object in graph" );
            } else if ( go.myKind == GraphObject.Kind.GRAPH ) {
//                Rectangle2D.Double drect = Util.make2DDouble( go.displayRect);
                Rectangle2D.Double drect = go.getDisplayRect();
                CGUtil.grow( drect, ( 0.5f * Graph.contextBorderWidth ), ( 0.5f * Graph.contextBorderWidth ) );
                if ( drect.contains( p ) ) {
                    CGUtil.grow( drect, -1 * Graph.contextBorderWidth, -1 * Graph.contextBorderWidth );
                    if ( !drect.contains( p ) ) {
                        return go;
                    }
                }
            } else if ( go.getShape().contains( (Point2D.Double)p ) ) {
                return go; // should be p
            }
        }
        return null;
    }

    /**
     * Determine whether a given point is on top of a GNode's relevent portion.
     * Same as insideGNode() except when applied to a Graph: a point inside the
     * context will not return the context unless the point lies on the thin
     * border enclosing the context.
     *
     * @param p the point on the canvas
     * @return the GNode if there is one, null otherwise
     * @see #onTopOfObject
     * @see #insideGNode
     */
    public GraphObject onTopOfGNode( Point2D.Double p ) {
        Iterator<GraphObject> iter = new DeepIterator( TheGraph );
        GraphObject go;
        while ( iter.hasNext() ) {  // does not check for object type here!
            go = iter.next();
            if ( go == null ) {
                Global.info( "Nullptr object in graph" );
                return null;
            }
            if ( go.myKind == GraphObject.Kind.GRAPH ) {
//                Rectangle2D.Double drect = Util.make2DDouble( go.displayRect);
                Rectangle2D.Double drect = go.getDisplayRect();
//                CGUtil.grow( drect, (  * Graph.contextBorderWidth ), ( 0.5f * Graph.contextBorderWidth ) );
                if ( drect.contains( p ) ) {
                    CGUtil.grow( drect, -1 * Graph.contextBorderWidth, -1 * Graph.contextBorderWidth );
                    if ( !drect.contains( p ) ) {
                        return go;
                    }
                }
            } else if ( go.getDisplayRect().contains( p ) ) {
                if ( go.myKind == GraphObject.Kind.GNODE ) {
                    return go;
                }
            }
        }
        return null;
    }

    /**
     * Determine whether a given point is conntained in any GNode's display
     * rectangle.
     *
     * @param p the point on the canvas
     * @return the GNode if there is one, null otherwise
     * @see #onTopOfObject
     * @see #onTopOfGNode
     */
    public GraphObject insideGNode( Point2D.Double p ) {
        Iterator<GraphObject> iter = new DeepIterator( TheGraph );
        GraphObject go;
        while ( iter.hasNext() ) {  // does not check for object type here!
            go = iter.next();
            if ( go == null ) {
                Global.info( "Nullptr object in graph" );
                return null;
            }
            if ( go.getDisplayRect().contains( p ) ) {
                if ( go.myKind == GraphObject.Kind.GNODE ) {
                    return go;
                }
            }
        }
        return null;
    }

    /**
     * Determine whether there is any GEdge whose display rectangle contains a
     * point on the canvas.
     *
     * @param p the point on the canvas
     * @return the GEdge if there is one, null otherwise
     * @see EditFrame#onTopOfObject
     * @see EditFrame#insideGNode
     */
    public GraphObject insideGEdge( Point2D.Double p ) {
        Iterator<GraphObject> iter = new DeepIterator( TheGraph );
        GraphObject go;
        while ( iter.hasNext() ) {  // does not check for object type here!
            go = iter.next();
            if ( go == null ) {
                Global.info( "Nullptr object in graph" );
                return null;
            }
            if ( go.getDisplayRect().contains( p ) ) {
                if ( go.myKind == GraphObject.Kind.GEDGE ) {
                    return go;
                }
            }
        }
        return null;
    }

    /**
     * For every selected object, if it's a context, make sure all its
     * components are marked HERE!!
     */
    public void includeContextMembers() {
        for ( int k = 0; k < EFSelectedObjects.size(); k++ ) {
            GraphObject go = EFSelectedObjects.get( k );
            if ( go instanceof Graph ) {
                includeContextMembers( (Graph)go );
            }
        }
    }

    /**
     *
     * For the given graph, mark its contents (recursively) as selected
     */
    public void includeContextMembers( Graph g ) {
        Iterator<GraphObject> iter = new ShallowIterator( g );
        while ( iter.hasNext() ) {
            GraphObject go = iter.next();
            addToSelection( go );	// note that if it's already in selection, this has no effect
            if ( go instanceof Graph ) {
                includeContextMembers( (Graph)go );
            }
        }
    }

    /**
     * For all graph objects in a graph, mark each as selected if it lies within
     * the rect. NEED TO LOOK HERE!!! but why?
     *
     * @param g look through all objects in this graph
     * @param r bounding rectangle within which to select
     * @param select whether to select or de-select the objects in question
     */
    protected void setSelectedGraphObjects( Graph g, Rectangle2D.Double r, boolean select ) {
        Iterator<GraphObject> iter = new DeepIterator( g );
        GraphObject go;
        while ( iter.hasNext() ) {  // does not check for object type here!
            go = iter.next();
            if ( go.getDisplayRect().createUnion( r ).equals( r ) ) // if go is inside selection rectangle
            {
                //Global.info("inside selection rect: " + go );
                if ( go.isSelected ) {
                    if ( select ) {
                        takeFromSelection( go );
                    } else {
                        addToSelection( go );
                    }
                } else //if ( select )
                {
                    addToSelection( go );
                }
                //else
                //takeFromSelection( go );
            }
        }
    }

    /**
     * Translates all selected objects by a common x,y displacement.
     *
     * @param g outermost graph in which anything could be moved into or out of
     * @param delta the translation displacement
     */
    public void moveSelectedObjects( Graph g, Point2D.Double delta ) {
        // also removes graph contents from selection, if present, so they don't get moved twice
        ArrayList selection = emgr.sortSelectionObjects();

        if ( !emgr.useNewUndoRedo ) {
            emgr.makeHoldGraph();
        }
        EditingChangeState changed = g.moveGraphObjects( selection, delta );
        changed.setChangeUndoable( true );
        if ( changed.anythingChanged() ) {
            emgr.setChangedContent( changed );
        }
        g.resizeForContents( null );		// commented 07-31-03

        repaint();
    }

    /**
     * Mark an object as being selected, and let the edit frame know that
     * something was selected.
     *
     * @param go Object to be marked as selected. If already selected, has no
     * effect.
     */
    public void addToSelection( GraphObject go ) {
        if ( EFSelectedObjects.contains( go ) ) {
            return;
        }
        if ( !go.isSelected ) {
            // Global.info( "add to selection " + go );
            go.isSelected = true;
            EFSelectedObjects.add( go );
            if ( go.myKind != GraphObject.Kind.GEDGE ) {
                EFSelectedNodes.add( go );
            }
            fixToolbarDisplay();
            somethingHasBeenSelected = true;
        }
    }

    /**
     * If object is already selected, remove it from the selection. If not
     * selected, has no effect.
     *
     * @param go Object to be removed if possible.
     */
    public void takeFromSelection( GraphObject go ) {
        //if ( ( go.getOwnerGraph() != null ) && ( go.getOwnerGraph().isSelected ) ) return;
        if ( go.isSelected ) {
            // Global.info( "add to selection " + go );
            go.isSelected = false;
            EFSelectedObjects.remove( go );
            if ( go.myKind != GraphObject.Kind.GEDGE ) {
                EFSelectedNodes.remove( go );
            }
            if ( EFSelectedObjects.size() == 0 ) {
                somethingHasBeenSelected = false;
            }
        }
    }

    /**
     * Set selection to be empty. Don't show a "rubber band" selection rectangle and
     */
    public void resetSelection() {
        //if ( cp != null ) cp.closeTextEditors( );
        if ( somethingHasBeenSelected ) {
            showRubberBand = false;
            somethingHasBeenSelected = false;
            selectionRect = null;

            while ( EFSelectedObjects.size() > 0 ) {
                GraphObject go = EFSelectedObjects.get( 0 );
                go.isSelected = false;
                EFSelectedObjects.remove( go );
            }

            while ( EFSelectedNodes.size() > 0 ) {
                GraphObject go = EFSelectedNodes.get( 0 );
                go.isSelected = false;
                EFSelectedNodes.remove( go );
            }

            TheGraph.isSelected = false;
            EFSelectedNodes.clear();
            EFSelectedObjects.clear();
        }
        lastMouseClickPoint = null;
        setMenuItems();
        cp.repaint();
        requestFocus();
    }

    public String summarizeSelection() {
        return "There are " + EFSelectedObjects.size() + " selected objects."
                + "There are " + EFSelectedNodes.size() + " selected nodes.";
    }

    /**
     * @return whether "rubber band" border is to be shown or not
     */
    public boolean getShowRubberBand() {
        return showRubberBand;
    }

    /**
     * For all the selected nodes, determines what best to show on the format toolbar.
     */
    public void  fixToolbarDisplay() {
                // loop through all the nodes. If
        if ( EFSelectedNodes.size() == 0 ) {
            formatToolbar.setDisplayForSelection(  null, null, null );
            return;
        }
        Color foreColor = ((GNode)EFSelectedNodes.get( 0)).foreColor;
        Color backColor = ((GNode)EFSelectedNodes.get( 0)).backColor;
        Font font = ((GNode)EFSelectedNodes.get( 0)).getLabelFont();
            for ( Object node : EFSelectedNodes) {
                if ( ((GNode)node).foreColor != foreColor )
                    foreColor = null;
                 if ( ((GNode)node).backColor != backColor )
                    backColor = null;
           }
            formatToolbar.setDisplayForSelection( foreColor, backColor, font );
    }

    /**
     * Gathers a collection of objects into a single graph we call a context
     * Assumes all selected objects have isSelected set Assumes all selected
     * objects are in the same outer graph
     *
     * @param selectedNodes Collection of the objects to be placed into the
     * context
     * @param what either "context" or "cut"
     * @param border The minimum rectangle to form the context boundary; may be
     * expanded if necessary.
     */
    public void makeContext( ArrayList selectedNodes, String what, Rectangle2D.Double border ) throws CGContextException {
        //= algorithm:
        //=		loop for each selected node
        //=			if node is not in graph OG then exit with an error
        //=			else add it the list of selected nodes
        //=		end loop
        GraphObject go = null;
        Graph dominantContext = null;

//        try {
                    // note: this may throw a cg context exception
            dominantContext = GraphObject.findDominantContext( selectedNodes );
//        } catch ( CGContextException x ) {
//            throw x;
//        }

        // dominant context has been identified
        if ( dominantContext == null ) {
            JOptionPane.showMessageDialog(
                    this,
                    "No dominant context in which to place the newly-formed context.",
                    "CG Rule not followed", JOptionPane.ERROR_MESSAGE );
            return;
        }
        //Global.info( "dominant context is " +
        //	((dominantContext==null)?"null": ("" + dominantContext.objectID) ));
        // create the new context to be owned by the others' owner
        Graph contextG = new Graph( dominantContext );

        GraphObject nodeN, nodeN1;	// although they're graph objects, they should be GNodes
        // loop for each selected node N
        //   move N to graph contextG
        // end loop
        Iterator<GraphObject> nodes = selectedNodes.iterator();
        while ( nodes.hasNext() ) {
            nodeN = nodes.next();
            //Global.info( "checking on node " + nodeN.objectID );
            //Global.info( "ownergraph is " + nodeN.getOwnerGraph().objectID + "; new context is " + contextG.objectID );
            if ( ( nodeN.getOwnerGraph() != null ) && !( selectedNodes.contains( nodeN.getOwnerGraph() ) ) ) {
                // NOte we may want to use forgetObject if this needs to be uncommitted from knowledge base.
                nodeN.getOwnerGraph().removeFromGraph( nodeN );


                contextG.insertInCharGerGraph( nodeN );
                // don't insert into new notio graph, or
                //contextG.insertObject( nodeN );	// was insertInCharGerGraph
            }
        }
        //	Handle the GEdges now
        //		loop for each selected node N
        //		can probably be optimized, assuming 0..N enclosed contexts and 1 dominant one
        //for ( int nodenum = 0; nodenum < selectedNodes.size(); nodenum++ ) {
        //	nodeN = (GraphObject)selectedNodes.get( nodenum );
        nodes = selectedNodes.iterator();
        while ( nodes.hasNext() ) {
            nodeN = nodes.next();

            // loop for each link L to N
            GEdge lineL;
            //int maxedges = ((GNode)nodeN).myedges.size();
            // what's happening here is that as lineL is deleted, the enumeration knows
            // it's null and scoots the rest of the enumeration up one.
            //Iterator edges = ((GNode)nodeN).myedges.iterator();
            // ArrayList.toArray() didn't work because we need to cast every element anyway
            GEdge[] edges = new GEdge[ ( (GNode)nodeN ).getEdges().size() ];
            for ( int k = 0; k < edges.length; k++ ) {
                edges[k] = (GEdge)( (GNode)nodeN ).getEdges().get( k );
            }
            //while ( edges.hasNext() ) {
            for ( int L = 0; L < edges.length; L++ ) {
                //lineL = (GEdge) edges.nextElement();
                lineL = edges[ L];
                //Global.info("making context,handling edge " + lineL.toString() );
                // if L is to another selected node N1 or it's a coref
                //  or if one end is an actor and that's allowed, then
                nodeN1 = lineL.linkedTo( (GNode)nodeN );
                if ( selectedNodes.contains( nodeN1 ) || // linked node is in my context
                        CGUtil.shortClassName( lineL ).equals( "Coref" ) || // link is a coref
                        ( Global.allowActorLinksAcrossContexts && // one end is actor and that's allowed
                        ( CGUtil.shortClassName( nodeN ).equals( "Actor" )
                        || CGUtil.shortClassName( nodeN1 ).equals( "Actor" ) ) ) ) {
                    if ( lineL.getOwnerGraph() != contextG ) // if edge not already moved
                    {
                        // change L's ownergraph to G (i.e., keep link intact)
                        // means that links will be owned by the nested context
                        // Note we may want to use forgetObject if this needs to be uncommitted from knowledge base.
                        lineL.getOwnerGraph().removeFromGraph( lineL );
                        //lineL.getOwnerGraph().disconnectNotioCounterpart( lineL );

                        contextG.insertObject( lineL );	// was insertInCharGerGraph
                        lineL.placeEdge();
                    }
                } else // end nodes are in different context and it's not allowed, cut the edge
                {
                    lineL.getOwnerGraph().forgetObject( lineL );
                }
                //Global.info( "finished handling an edge.");
            }
        }
        contextG.setNegated( what.equals( "cut" ) );
        // add graph contextG to outer graph
        if ( dominantContext != null ) {
            if ( border == null ) {     // figure out what the border should be
                border = CGUtil.unionDisplayRects( selectedNodes );
                if ( border.width <= currentFontMetrics.stringWidth( contextG.getTextLabel() ) ) {
                    border.width = currentFontMetrics.stringWidth( contextG.getTextLabel() );
                }
                CGUtil.grow( border, Graph.contextInnerPadding, currentFontMetrics.getHeight() + Graph.contextInnerPadding );
            }
            contextG.setDisplayRect( border );
            dominantContext.insertObject( contextG );
//            contextG.resizeForContents( contextG.getCenter() );
        }
        TheGraph.handleContextLinks();
    }

    /**
     * Takes a single context and un-makes its border, moving all its contents
     * out one graph level. Leaves everything in the selection set, but they may
     * be in different contexts.
     *
     * @param selectedNodes Nodes to be un-made; must be a single context.
     * @param border the enclosed area -- should contain exactly one context.
     * @return whether any change was made Actually, this probably works as long
     * as only one dominated context is enclosed, ignoring others
     */
    public boolean unMakeContext( ArrayList<GraphObject> selectedNodes, Rectangle2D.Double border ) throws CGContextException {
        //= algorithm:
        //=    find the graph node G that is the context
        //=		loop for each selected node
        //=			if node is not in graph G then exit with an error
        //=			else
        //=              detach it from the graph G and attach it to G's owner
        //=              add it to a new list of selected nodes
        //=		end loop
        GraphObject go = null;
        Graph dominantContext = null;
        boolean keepGoing = true;

        // create the new context to be owned by the others' owner
        Graph contextG = null;	// start by assuming 1st selected node is context

        GraphObject nodeN = null;
        // Global.info( "there are " + selectedNodes.size() + " selected nodes." );
        Iterator<GraphObject> nodes = selectedNodes.iterator();
        while ( nodes.hasNext() ) {
            nodeN = nodes.next();
            // Global.info( "traversing next node " + nodeN.toString() );
            if ( ( nodeN.myKind == GraphObject.Kind.GRAPH ) ) //&& ((Graph)nodeN).containsGraphObjects( selectedNodes ) )
            {
                //Global.info( "graph traversed: " + nodeN.getTextLabel() );
                if ( contextG == null ) {
                    contextG = (Graph)nodeN;
                } else if ( contextG.nestedWithin( (Graph)nodeN ) ) {
                    contextG = (Graph)nodeN;
                }
            }
        }

        if ( contextG == null ) {
            return false;
        }

        if ( contextG.getOwnerGraph() == null ) {
            throw new CGContextException( "Somehow an inner context had no owner." );
        }
        // contextG is now the context to be disconnected, and connected to dominant context

        dominantContext = contextG.getOwnerGraph();

        nodes = new ShallowIterator( contextG );
        while ( nodes.hasNext() ) {
            nodeN = nodes.next();
            //Global.info( "checking on node " + nodeN.objectID );
            //Global.info( "ownergraph is " + nodeN.getOwnerGraph().objectID + "; new context is " + contextG.objectID );
            // Note we may want to use forgetObject if this needs to be uncommitted from knowledge base.
            nodeN.getOwnerGraph().removeFromGraph( nodeN );
            dominantContext.insertObject( nodeN );	// was insertInCharGerGraph
        }
        //	Handle the GEdges now, disconnect any GEdges which linked contextG
        contextG.getOwnerGraph().forgetObject( contextG );
        return true;
    }

    /**
     * Checks to see if the entire (deep) contents of the target context graph
     * are selected
     *
     * @param g The graph to be checked
     * @return Whether all objects (nodes) are selected
     */
    public boolean entireGraphSelected( Graph g ) {
        Iterator<GraphObject> iter = new ShallowIterator( g, GraphObject.Kind.GNODE );
        if ( !g.isSelected ) {
            return false;
        }
        while ( iter.hasNext() ) {
            if ( ! iter.next().isSelected ) {
                return false;
            }
        }
        iter = new ShallowIterator( g, GraphObject.Kind.GRAPH );
        while ( iter.hasNext() ) {
            if ( !entireGraphSelected( (Graph)iter.next() ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Decide which cursor to show if we're positioned on something interesting.
     * If we're deleting something, use a crosshair. If we're editing text, use
     * a text cursor. Otherwise, use a move cursor.
     */
    public void setMyCursor() {
        if ( editingToolbar.getMode() == EditToolbar.Mode.Delete ) {
            setCursor( new Cursor( Cursor.CROSSHAIR_CURSOR ) );
        } else if ( editingToolbar.getMode() == EditToolbar.Mode.Note ) {
            setCursor( new Cursor( Cursor.TEXT_CURSOR ) );
        } else {
            setCursor( new Cursor( Cursor.MOVE_CURSOR ) );
        }
        //cp.requestFocus();
        //Global.info( "in setmycursor focus; cp " + cp.hasFocus() );
    }

    /**
     * Sets the font for the edit frame.
     *
     * @param f font to which the edit frame should be set.
     */
    public void setMyFont( Font f ) {
        cp.setFont( f );
        currentFont = f;
        currentFontMetrics = cp.getFontMetrics( currentFont );
        // set every label to the new font
        Iterator<GraphObject> iter = new DeepIterator( TheGraph );
        while ( iter.hasNext() ) {
            GraphObject go = iter.next();
            go.setTextLabel( go.getTextLabel() );
        }
        cp.repaint();
    }

    /**
     * Blanks out the status line at the bottom of the editing frame
     */
    public void clearStatus() {
        // Global.info( "clear status" );
        displayOneLiner( " " );
        //cp.requestFocus();	// 12-16-02
        Iterator<GraphObject> iter = new DeepIterator( TheGraph, GraphObject.Kind.GNODE );
        while ( iter.hasNext() ) {
            GNode gn = (GNode)iter.next();
            gn.setActive( false );
        }
        if ( Global.wordnetEnabled && descriptorPanel.isVisible() ) {
            //Global.info( "before closing desc panel, editframe is " + this.getSize() + "; desc panel is " + descriptorPanel.getSize() );
            this.setSize( new Dimension( getWidth(), getHeight() - descriptorPanel.getHeight() ) );
            descriptorPanel.setVisible( false );
            //Global.info( "after closing desc panel, editframe is " + this.getSize() + "; desc panel is " + descriptorPanel.getSize() );
        }
    }

    /**
     * Shows a string in the message box.
     *
     * @param s String to be displayed.
     */
    public void displayOneLiner( String s ) {
        messageBox.setText( s );
        messageBox.repaint();
    }

    // inner class declaration
    public class ManageWinEvents extends WindowAdapter {
    }

    // inner class declaration
    public class HandleCompEvents extends ComponentAdapter {
        // need to handle a COMPONENT_RESIZED

        public void componentResized( ComponentEvent e ) {
            super.componentResized( e );
            setEditFrameSizes();
        }
    }

    /**
     * Does a logical close (with respect to the user). This is where most of
     * the real cleanup code goes -- since we cannot reliably depend on
     * finalize.
     *
     * @return true if everything worked out okay; false if we still need to do
     * something (e.g., save it, or cancel).
     */
    public boolean closeOut() {
        // Global.info( "Ready to close out edit frame " + editFrameNum );
        if ( !performCheckSaved() ) {
            // Global.info( "after perform check saved cancelled in close out" );
            return false;
        }
        Global.removeEditFrame( this );
        Global.removeGraph( TheGraph );
            // PR-132 02-18-18 hsd - complete rework of the window manager.
//        WindowManager.forgetWindow( this );
        Global.knowledgeManager.forgetKnowledgeSource( TheGraph );
        TheGraph.dispose();
        if ( textFormDisplay != null ) {
            textFormDisplay.setVisible( false );
            textFormDisplay.dispose();
            textFormDisplay = null;
        }
        if ( CGIFDisplay != null ) {
            CGIFDisplay.setVisible( false );
            CGIFDisplay.dispose();
            CGIFDisplay = null;
        }
        if ( XMLDisplay != null ) {
            XMLDisplay.setVisible( false );
            XMLDisplay.dispose();
            XMLDisplay = null;
        }

        if ( metricsDisplay != null ) {
            metricsDisplay.setVisible( false );
            metricsDisplay.dispose();
            metricsDisplay = null;
        }

        setVisible( false );
        dispose();

        WindowListener wlist[] = getWindowListeners();
        for ( int k = 0; k < wlist.length; k++ ) {
            removeWindowListener( wlist[k] );
        }

        ComponentListener clist[] = getComponentListeners();
        for ( int k = 0; k < clist.length; k++ ) {
            removeComponentListener( clist[k] );
        }

        FocusListener flist[] = getFocusListeners();
        for ( int k = 0; k < flist.length; k++ ) {
            removeFocusListener( flist[k] );
        }

        KeyListener klist[] = getKeyListeners();
        for ( int k = 0; k < klist.length; k++ ) {
            removeKeyListener( klist[k] );
        }

        omgr = null;

        emgr.resetUndo();
        emgr = null;

        cp.teardown();

        cp = null;
        TheGraph.setOwnerFrame( null );
        TheGraph = null;
        EFSelectedNodes = null;
        EFSelectedObjects = null;

        if ( Global.CharGerMasterFrame != null ) {
            Global.CharGerMasterFrame.requestFocus();
        }
        requestToStop();		// stop the thread
        threadgroup.interrupt();
        //if ( ! threadgroup.isDestroyed() ) threadgroup.destroy();		// remove all graph updaters if any still exist
        // Global.info( "done with closing frame" + efnum );

        tearDown();

        return true;
    }

    /**
     * Checks whether graph in the edit frame has been saved. If not, then
     * prompts user for whether to save it or not.
     *
     * @return false if user cancels, true if user either saves or chooses not
     * to save.
     */
    public boolean performCheckSaved() {
        int filenameSubstringLength = 27;
        // Global.info( "at perform checksaved; anything changed is " + outerFrame.somethingHasChanged );
        boolean okay = true;
        if ( somethingHasChanged ) {
            requestFocus();
            String displayableFilename = graphAbsoluteFile.getAbsolutePath();
            Object[] possibleValues = { Global.strs( "SaveLabel" ),
                Global.strs( "DontSaveLabel" ),
                Global.strs( "CancelLabel" ) };
            int d = JOptionPane.showOptionDialog( this,
                    displayableFilename + "\nnot saved.\n\nDo you want to save it?",
                    "File Not Saved",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null,
                    possibleValues, Global.strs( "SaveLabel" ) );

            //String s = d.getSelectedText();
            //if ( s.equals( "Cancel" ) ) return false;
            if ( d == JOptionPane.CANCEL_OPTION ) {
                return false; // @bug -- cancel acts like NO
            }
            if ( d == JOptionPane.CLOSED_OPTION ) {
                return false;
            }
            //if ( s.equals( "Save" ) ) {
            if ( d == JOptionPane.YES_OPTION ) {
                okay = emgr.performActionSaveGraphFormattedAs( FileFormat.CHARGER3 );
            }
            if ( okay ) {
                emgr.nothingChanged();	// If it's saved, then reset to say nothing's changed.
            }
            return okay;
        }
        return true;	// nothing happened, there was nothing needed to be done
    }

    /**
     * Returns the graph name (NOT the filename) of the current window.
     *
     * @return the graph name plus a modality if they are active
     */
    public String getGraphName() {
//        if ( Global.modalityLabelsActive ) {
//            return graphName + "." + purpose.getAbbr();
//        } else {
            return graphName;
//        }
    }

    /**
     * Get purpose from the filename, so it works for a newly-opened graph too.
     *
     * @param filename fully qualified path/file for the new graph
     */
    public void renameGraphInFrame( String filename ) {
        // let the editframe know it has a new actual file
        graphSourceFile = new File( filename );
        graphAbsoluteFile = graphSourceFile.getAbsoluteFile();
        String withoutExtension = null;

        // pick apart the name and look for relevant extensions, etc.
        // first strip off extension if there is one that matters
        if ( Global.acceptCGXFileName( filename ) || Global.acceptCGIFFileName( filename ) ) {
            withoutExtension = General.stripFileExtension( filename );
        }

        // decide whether to look for a purpose abbreviation
//        /*if ( Global.modalityLabelsActive ) {
//            // grab just the purpose suffix, if there is one
//            String Purpose_abbr = filename
//                    .substring( withoutExtension.lastIndexOf( '.' ) + 1, withoutExtension.length() );
//            if ( PragmaticSense.isValidAbbr( Purpose_abbr ) ) {
//                purpose = new PragmaticSense( Purpose_abbr );
//                graphName = Util.stripFileExtension( withoutExtension );
//            } else {
//                purpose = new PragmaticSense();   // a default one
//            }
//            Global.info( "renaming graph, modality is " + purpose.getAbbr() );
//            ///   setPurposeMenu( Purpose );	// doesn't exist yet!
//            setTitle( purpose.getLabel() + ": " + graphName );
//        } else { */
            setTitle( graphAbsoluteFile.getAbsolutePath() );
            graphName = General.stripFileExtension( graphSourceFile.getName() );
//        }
                Global.info( "Title of edit frame is " + getTitle() );
        if ( emgr != null ) {
            emgr.setChangedContent( EditChange.SEMANTICS, EditChange.UNDOABLE  );
        }
        WindowManager.refreshWindowMenuList( this );
    }

    public void thisComponentResized( ComponentEvent e ) {
        setEditFrameSizes();
        sp.revalidate();	// this statement is very important!!! otherwise viewport will appear wrong
    }

    private void refreshDescriptorPanel( AbstractTypeDescriptor[] something ) {
        //TypeDescriptor[] something = new TypeDescriptor[0];
        JTable descriptorTable = charger.gloss.wn.WNUtil.getDescriptorTable( something );
        dsp.setViewportView( descriptorTable );
        descriptorTable.setGridColor( Color.gray );
        descriptorTable.setShowGrid( true );

        descriptorPanel.validate();

        if ( Global.wordnetEnabled && !descriptorPanel.isVisible() ) {
            //Global.info( "before opening desc panel, editframe is " + this.getSize() + "; desc panel is " + descriptorPanel.getSize() );
            this.setSize( new Dimension( getWidth(), getHeight() + descriptorPanel.getPreferredSize().height ) );
            descriptorPanel.setVisible( true );
            //Global.info( "after opening desc panel, editframe is " + this.getSize() + "; desc panel is " + descriptorPanel.getSize() );
        }
    }

    private void setupDescriptorPanel() {
        int panelHeight = 100;
        descriptorPanel.setSize( new Dimension( getWidth(), panelHeight ) );
        descriptorPanel.setLayout( new BorderLayout() );
        descriptorPanel.setPreferredSize( new Dimension( getWidth(), panelHeight ) );
        descriptorPanel.setOpaque( true );
        // set up the scroll pane's characteristics
        dsp.setPreferredSize( new Dimension( getWidth(), panelHeight ) );
        dsp.setBackground( Color.white );
        dsp.getViewport().setBackground( Craft.craftPink );
        //dsp.setLocation( new Point( 0, 0 ) );
        dsp.setOpaque( true );

        dsp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        dsp.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
        descriptorPanel.add( dsp );

        descriptorPanel.validate();
        descriptorPanel.setVisible( false );
        messagePanel.add( descriptorPanel );
    }

//    /**
//     * Part of the ManagedWindow interface
//     *
//     * @see ManagedWindow
//     */
//    public void bringToFront() {
//        WindowManager.bringToFront( this );
//    }

    /**
     * Part of the ManagedWindow interface
     *
     * @see ManagedWindow
     */
    public String getMenuItemLabel() {
        return getTitle();
    }

    /** @return Absolute path name for the file in this frame */
    public String getFilename() {
        return graphAbsoluteFile.getAbsolutePath();
    }

    @Override
    public JMenu getWindowMenu() {
        return windowMenu;
    }

    @Override
    public String toString() {
        return getTitle();
    }

}
