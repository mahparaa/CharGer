package charger;

import charger.act.GraphUpdater;
import charger.exception.CGFileException;
import charger.exception.CGStorageError;
import charger.obj.EdgeAttributes;
import charger.obj.Graph;
import charger.obj.GraphObjectID;
import charger.prefs.PreferencesFrame;
import charger.prefs.PreferencesWriter;
import chargerlib.CDateTime;
import chargerlib.FileFormat;
import chargerlib.General;
import chargerlib.JarResources;
import chargerlib.ManagedWindow;
import chargerlib.WindowManager;
import chargerplugin.ModulePlugin;
import craft.Craft;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.border.Border;


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
 * Central CharGer class, which spawns the rest of the CharGer system.
 * Repository for global parameters, shared by all parts of the system. Some of
 * them are only used in a single class, but it seems reasonable to keep them
 * here so that they can be configured all at once. There will only be one of
 * these per "session". If you have your own Java application (or possibly some
 * other language's application) you should be able to easily invoke CharGer.
 * The steps are as follows: <ul> <li>In your Java program, invoke the following
 * call.
 * <pre>
 * charger.Global.setup();
 * </pre> <li>In the places where the driver needs to exit, the call
 * <pre>
 * charger.Global.closeOutAll();
 * </pre> should allow CharGer to exit gracefully. </ul>
 *
 * @author Harry S. Delugach ( delugach@uah.edu ) Copyright (c) 1998-2020 by
 * Harry S. Delugach.
 * $Date: 2019-06-27 18:42:07 -0500 (Thu, 27 Jun 2019) $   $Revision: 864 $   $Author: hsd $
 */
public class Global {

//========================================================================================
//	 GENERAL CONFIGURATION INFORMATION
        /**
     * Used as a human-readable rendition of the current version. Examples are
     * "7.6" "7.6.1" or "7.6.1b2" or "7.6.2rc1". Previous versions included a
     * date, but that has been omitted as of 7.5.
     *
     * @see #BUILD_NUM
     */
    public final static String RELEASE_VERSION = "4.3.0b4";

     /**
     * The date assigned to this version in the form (YYYY-MM-DD)
     */
    public static final String CharGerDate = "(2020-08-07)";

    /**
     * The build number (e.g., the string " (678)") for developers and support.
     * Has a leading space. The underlying code is somewhat obscure because the
     * revision header is automatically inserted by the svn commit, and then
     * must be massaged a bit to extract just the brief build number. Updated
     * every time the MConst file is committed to a revision control system. Do
     * not edit this directly!
     */
    public final static String BUILD_NUM = new String( " ("
            + new String( "$Revision: 864 $ " ).replaceAll( "[^0-9]", "" )
            + ")" );

    /**
     * Currently set to "CharGer"
     */
    public static final String EditorNameString = "CharGer";
    public static final String copyrightNotice = "Copyright 1998-2020 by Harry S. Delugach";
    /**
     * if false, then enables Global.info output and enables various
     * experimental menu items and operations.
     *
     * @see #info
     */
    public static final boolean OfficialRelease = false;
    /**
     * whether to show some tracing information.
     */
    public static boolean infoOn = true;
    /**
     * Whether to display (on the console) tracing information as each actor
     * fires. Different from the "animate actors" preferences, which are
     * displayed on the editing canvas.
     */
    public static final boolean traceActors = false;


//========================================================================================
//	 CGIF IMPORT AND EXPORT
    /**
     * Used in setting CGIF comments so that they are clearly recognizable by
     * CharGer
     */
    public static final String CharGerCGIFCommentStart =  "*CG4L;";
    /**
     * Whether to include CharGer data (e.g., element positions, source names)
     * in exported graphs
     */
    public static boolean includeCharGerInfoInCGIF = true;
    /**
     * Whether to export type hierarchies as (subtype ... ... ) in CGIF
     */
    public static boolean exportSubtypesAsRelations = true;

    /**
     * Whether to treat [Type] concepts and (subtype) relations as special type hierarchy denotations or not.
     */
    public static boolean importSubtypeRelationsAsHierarchy = true;
//========================================================================================
//	 LOCAL PLATFORM CONFIGURATION ("platform" means whether Mac OS, Linux, Windows, etc.)
    public static final String LineSeparator = System.getProperty( "line.separator" );
    public static final String imagePath = "images" + "/";
    /**
     * Key to use with shortcut commands; i.e., CNTL on PC, Cmd on Mac
     */
    public static int AcceleratorKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    /**
     * @see TextProperties
     */
    public static final TextProperties LocaleStrings = new TextProperties( "en" );
    public static String HubFolder = "";
    public static String GraphFolderString = null;
    public static String DatabaseFolderString = null;
    public static File GraphFolderFile = null;
    public static File DatabaseFolderFile = null;
    public static File LastFolderUsedForSave = null;	// 12-17-02
    public static File LastFolderUsedForOpen = null;
    /**
     * Charger's current file format.
     */
//    public static FileFormat defaultFileFormat = FileFormat.CHARGER3;
    public static FileFormat defaultFileFormat = FileFormat.CHARGER4;
    /**
     * The default extension for Charger files. Currently ".cgx". Note that the
     * period "." is part of the extension.
     */
    public static final String ChargerFileExtension = "." + FileFormat.DEFAULT.extension();

    public static final CDateTime.STYLE ChargerDefaultDateTimeStyle = CDateTime.STYLE.ISO_OFFSET;
//========================================================================================
//  KNOWLEDGE BASE STUFF
    public static kb.KnowledgeBase sessionKB = new kb.KnowledgeBase( "sessionKB" );
////========================================================================================
////	 ANYTHING CHARGER NEEDS TO KEEP TRACK OF CRAFT (requirements tool)
//    public static boolean craftEnabled = false;
//    public static craft.Craft craftModule = null;
//========================================================================================
//	 ANYTHING CHARGER NEEDS FOR WORDNET
    public static boolean wordnetEnabled = false;


    public static ArrayList<String> extraArguments = new ArrayList<>();

//========================================================================================

//	 ANYTHING CHARGER NEEDS FOR MATCHING AND METRICS
    /**
     * Name of the class that is our default matcher for binary relation
     * matching.
     *
     * @see kb.KnowledgeManager#createCurrentTupleMatcher
     */
    public static boolean mmatEnabled = false;
    public static kb.KnowledgeManager knowledgeManager = new kb.KnowledgeManager();
    /** Temporary fix to allow any gen spec links to be drawn, because defaulting implicit subtypes of "T" is
     * problematic when users want to explicitly show it.
     */
    public static boolean allowAnyGenSpecLink = true;
    public static String matchingStrategy = "BasicTupleMatcher";
//    public static mm.MMAnalysisMgr mma = null;
//========================================================================================
//	 REFERENCE TO THE DRIVER (our invoking application)
//    public static CGMain driverClass = null;
//========================================================================================
//	 ACTOR SETUP, FIRING AND ANIMATION
    public static boolean ActorAnimation = false;
    /**
     * Number of milliseconds between "animation" frames.
     */
    public static int AnimationDelay = 0;
    public static boolean AllowNullActorArguments = false;
    /**
     * If "restricted" then actor links cannot cross context boundaries;
     * otherwise they can
     */
    public static boolean allowActorLinksAcrossContexts = true;
    /**
     * A list of file names and charger.db.Database objects, representing all
     * known Databases
     */
    public static Hashtable ActiveDatabases = new Hashtable();
    /**
     * Whether to use 1-0 as true-false or else use "top" and "absurd"
     */
    public static boolean use_1_0_actors = false;
    public static boolean enableActors = true;
    public static boolean enableCopyCorefs = false;
//========================================================================================
//	 ZOOM CONTROL
    public static double ScalingFactor = 1.00;
    public static boolean useBufferedImage = false;
//========================================================================================
//	 BOOKKEEPING, HOUSEKEEPING FOR THE CHARGER PACKAGE
    /**
     * Stores list of (FrameNum, EditFrame) pairs for referencing
     */
    public static Hashtable<Integer, EditFrame> editFrameList = new Hashtable();	// room for 10 edit frames to start with
    /**
     * Stores list of (GraphNum, Graph ) pairs for referencing
     */
    public static Hashtable graphList = new Hashtable();
    /**
     * Stores list of (objectID, GraphObject) pairs for keeping track of all
     * graph's objects. (Not used)
     */
    public static Hashtable allObjectList = new Hashtable();
    /**
     * Number assigned to last editframe window that was allocated
     */
    private static int LastWindow = 0;
    /**
     * Ever-increasing sequence number assigned to last graph object that was
     * allocated
     */
//    private static long globalID = System.currentTimeMillis()
//            - (new GregorianCalendar(2003, Calendar.JUNE, 25)).getTimeInMillis();
    /**
     * Actual graph object count.
     */
    public static int TotalGobjs = 0;
    /**
     * The editing window being edited; null if there is none
     */
    public static EditFrame CurrentEditFrame = null;

    public static JMenuItem BackToHubCmdItem = new JMenuItem( Global.strs( "BackToHubCmdLabel" ) );
    /**
     * So we always know where our hub is. Don't initialize until Hub is
     * initialized.
     */
    public static HubFrame CharGerMasterFrame;
    /**
     * keep a global page format instance for all the editing windows to use
     */
    public static java.awt.print.PageFormat pformat = null;
    public static java.text.Collator ignoreCase = java.text.Collator.getInstance();
//========================================================================================
//	 PURPOSE LABELING CONSTANTS AND CONTROLS
    /**
     * Controls whether graph modality types are to be used at all.
     */
    /**
     * Disabled for the time being since some graphs have one and some don't
     */
    public static boolean modalityLabelsActive = false;
    /**
     * Stores list of (Purpose, Suffix) pairs
     */
    // public static Properties PurposeSuffixLookup = new Properties();
    /**
     * Stores list of (Purpose, Label) pairs
     */
//========================================================================================
//	 THREAD CONTROL
    /**
     * since each EditFrame instance runs in its own thread, keep track of them
     */
    public static Thread MainThread = Thread.currentThread();
    public static ThreadGroup EditFrameThreadGroup = new ThreadGroup( "EditFrameThreads" );
    public static boolean enableEditFrameThreads = false;
    // threads are disabled, taking the advice on http://java.sun.com/docs/books/tutorial/uiswing/overview/threads.html
    public static ThreadGroup orphanUpdaters = new ThreadGroup( "Orphan Updaters" );
//========================================================================================
//	 MANAGING PREFERENCES AND POLICIES
    /**
     * Prefs simply stores (Name,Value) pairs as strings. Interpreting them is
     * the responsibility of whichever class needs them.
     */
    public static Properties Prefs = new Properties();
    // when making a context, a link to an inner object can be "cut" or "link"-ed.
    public static String LinkToContextStrategy;
//    public static String userPreferencesFilename = PreferencesWriter.getPrefFile().getAbsolutePath();
    public static boolean enforceStandardRelations = true;
    public static boolean saveHistoryRecords = true;
    public static byte[] prefFileAsBytes;	// used when reading prefs from a jar file

    public static final String LOG_FILENAME = "CharGerLog.txt";
    public static final String APPLICATION_CONFIG_FOLDER = ".edu.uah.Charger";
    public static final String PREFERENCES_FILENAME = "CharGerUserPrefs.props";
    public static File prefFile = new File( new File( System.getProperty( "user.home" ), APPLICATION_CONFIG_FOLDER ), PREFERENCES_FILENAME );
    public static File logFile = new File( new File( System.getProperty( "user.home" ), APPLICATION_CONFIG_FOLDER ), LOG_FILENAME );
    public static boolean standardOutputToLog = false;

    public static File getPrefFile() {
        return prefFile;
    }

    public static File getLogFile() {
        return logFile;
    }


    public static PrintStream fileStdOutputPrintStream = null;
    public static PrintStream fileStdErrorPrintStream =  null;


//========================================================================================
//      MODULE ARCHITECTURE AND ARGUMENT INFORMATION
//    public static String modulePluginClassSuffix = "ModulePlugin.class";
    /**
     * The list of all known module plugins. If a module is known, then it will be instantiated
     * and that instance added to this list. It is the intention of the design that only
     * one instance of each plugin exist at a time.
     * @see ModulePlugin
     */
    public static ArrayList<ModulePlugin> modulePluginsAvailable = new ArrayList<>();

    /**
     * The list of module instances which are considered to be enabled (i.e., eligible to
     * be activated if a user chooses them from the Tools menu).
     * @see ModulePlugin
     */
    public static ArrayList<ModulePlugin> modulePluginsEnabled = new ArrayList<>();

    /**
     * The module instances which are activated (e.g., selected through the Tools menu).
     * The system has special obligations for handling these, such as making sure
     * their preferences are saved, and making sure a shutdown is called before system
     * shutdown.
     * @see ModulePlugin#setProperties(java.util.Properties)
     * @see ModulePlugin#shutdown()
     */
    public static ArrayList<ModulePlugin> modulePluginsActivated = new ArrayList<>();
//    public static ArrayList<String> modulePluginNamesToEnable = new ArrayList<>();
    public static String moduleNamesToEnableCommaSeparated = null;
    public static String chargerpluginPackage = "chargerplugin";
    /** Indicates whether the list of modules have been changed, so that some methods can respond
     * appropriately. For example, the preferences panel may need to be re-created.
     * This is not affected by an module's activation or operation.
     */
    public static boolean modulesChanged = false;




//========================================================================================
//	 GLOBAL APPEARANCE INFORMATION

//      static URL applicationIconURL = ClassLoader.getSystemResource( Global.imagePath + "icon_256x256.png" );
//      public final static Image applicationIconImage64x64 =
//            Toolkit.getDefaultToolkit().createImage( applicationIconURL ).getScaledInstance( 64, 64, Image.SCALE_FAST );
//    public final static ImageIcon applicationIcon = new ImageIcon(  applicationIconImage64x64  );

          static URL applicationIconURL = ClassLoader.getSystemResource( Global.imagePath + "icon_32x32@2x.png" );
//      public final static Image applicationIconImage64x64 =
//            Toolkit.getDefaultToolkit().createImage( applicationIconURL ).getScaledInstance( 64, 64, Image.SCALE_FAST );
      public final static ImageIcon applicationIcon = new ImageIcon(  applicationIconURL  );

    /**
     * These globals require an active frame to make sense, mostly graphics.
     * Initialized after a real frame has been allocated (from which to get a
     * graphics context)
     */
    public static Graphics defaultGraphics = null;
    public static FontMetrics defaultFontMetrics;
    public static Font defaultFont;
//    public static String defaultLogicalFontName;
    public static String defaultFontName;
    public static int defaultFontStyle;
    public static int defaultFontSize;
    public static Font defaultBoldFont;
    public static boolean showAllFonts;	// whether to use all system fonts or just Java fonts

    public static Color fuchsiaColor = new Color( 255, 210, 210 );
//    public static Color chargerBlueColor = new Color(1, 55, 153);
    public static Color chargerBlueColor = new Color( 0, 111, 185 );
    public static Color oliveGreenColor = new Color( 0, 127, 0 );
    public static Color shadowColor = new Color( 150, 150, 150 );
    public static Point shadowOffset = new Point( 3, 3 );
    // Things needed for doing the shadows
    public static boolean showShadows = true;
    public static boolean showQuote = true;

        private static Border raisedBevel = BorderFactory.createRaisedBevelBorder();
        private static Border loweredBevel = BorderFactory.createLoweredBevelBorder();
        public static Border BeveledBorder = BorderFactory.createCompoundBorder( raisedBevel, loweredBevel );
    //BorderFactory.createRaisedBevelBorder();
    /**
     * whether to display an edge selection "handle" on edges between things
     */
    public static boolean showGEdgeDisplayRect = true;
    public static double preferredEdgeLength = 35;
//    public static int arrowHeadWidth = Integer.parseInt( Global.Prefs.getProperty( "arrowWidth", "4" ) );
//    public static int arrowHeadHeight = Integer.parseInt( Global.Prefs.getProperty( "arrowHeight", "8" ) );
//    public static double edgeThickness = Double.parseDouble( Global.Prefs.getProperty( "edgeThickness", "1.0" ) );

    /** Considered the minimum difference in object size and position that is worth considering. */
        public static double PIXEL_EPSILON = 0.001;

     /**
     * How far toward the "to" object to draw an arrowhead. Ranges from 0.0 to
     * 1.0
     */
    public static Double arrowPointLocation = Double.parseDouble( Global.Prefs.getProperty( "arrowPointLocation", "1.0" ) );
       // For now (4.0.2) all lines have the same attributes
    public static EdgeAttributes factoryEdgeAttributes = new EdgeAttributes( 5, 8, 1.5f, 1.0f );
    public static EdgeAttributes userEdgeAttributes = new EdgeAttributes();
//    public static Hashtable<String, EdgeAttributes> factoryEdgeAttributes = new Hashtable<String, EdgeAttributes>(3);
//    public static Hashtable<String, EdgeAttributes> userEdgeAttributes = new Hashtable<String, EdgeAttributes>(3);
    public static BasicStroke defaultStroke = new BasicStroke( (float)Global.factoryEdgeAttributes.getEdgeThickness() );

    // --- arrow calculations done once at construct time when possible
    /**
     * Whether to show an "outline" for each displayed node in a graph.
     */
    public static boolean showBorders = false;
    public static boolean showFooterOnPrint = true;

    /**
     * Image scaling when converting to image or copying for external paste.
     */
    public static double imageCopyScaleFactor = 1.0;

    /**
     * Whether to shade the inside of a cut so that nesting is easier to
     * understand
     */
    public static boolean showCutOrnamented = true;
    /**
     * the current factory color scheme, foreground where entries are
     * charger.obj class names, default foreground (text) color
     */
    public static Hashtable<String, Color> factoryForeground = new Hashtable<String, Color>( 10 );
    /**
     * the current factory color scheme, background where entries are
     * charger.obj class names, default background (fill) color
     */
    public static Hashtable<String, Color> factoryBackground = new Hashtable<String, Color>( 10 );
    /**
     * the user's color scheme, with any changes as per their prefs or session
     * choices
     */
    public static Hashtable<String, Color> userForeground = null;
    public static Hashtable<String, Color> userBackground = null;
    public static Hashtable<String, Color> bwForeground = null;
    public static Hashtable<String, Color> bwBackground = null;
    public static Hashtable<String, Color> grayForeground = null;
    public static Hashtable<String, Color> grayBackground = null;


    public static String defaultContextLabel = null;

    public static int springLayoutMaxIterations = 0;
//========================================================================================
//	 CLIPBOARD MANAGEMENT
    public static Clipboard cgClipboard = new Clipboard( "CG Clipboard" );
    public static boolean keepClipboardIDs = false;
    public static String cgClipboardType = null;
//========================================================================================
//	 DEBUGGING CONTROL
    public static boolean ShowBoringDebugInfo = false;
    /* This stuff is so that we can get some information about what's remaining as un-collected garbage */
    java.lang.ref.ReferenceQueue EditFrameReferenceQueue = new java.lang.ref.ReferenceQueue<EditFrame>();

//========================================================================================
    /**
     * Sets up all one-time decisions and values.
     * @see #setupAll for things done to apply a new set of preferences.
     */
    public Global() {

    }

    public static String getChargerVersion() {
        return Global.RELEASE_VERSION + Global.BUILD_NUM;
    }

    /** When Charger is being used as a library, not as a standalone.
     *
     */
    public static void setupAsLibrary() {
        setupFonts(  );
        setupGraphAppearance();
    }

    public static void setupAll( String startupFolder, ArrayList<String> extraArgs, boolean diagnostics) {
        setupArguments( startupFolder,  extraArgs,  diagnostics);
        setup1( );
    }


    public static void setupArguments( String startupFolder, ArrayList<String> extraArgs, boolean diagnostics ) {
        //========================================================================================
        //	 LOAD PREFERENCES (both default ones and user ones, if any) and put them into Prefs property list
        infoOn = diagnostics;

        try {
            loadConfig(getPrefFile().getAbsolutePath() );
        } catch ( CGFileException e ) {
            warning( "Default config file exception: " + e.getMessage() );
        }


        // do this after preferences have been set.
        // BUG: FIXED: 08-14-19 hsd: should be getting folder from the prefs in all cases.
//        if ( startupFolder != null ) {
//            File startupFolderFile = new File( startupFolder );
//            Prefs.setProperty( "GraphFolderString", startupFolderFile.getAbsolutePath() );
////            Prefs.setProperty( "GraphFolderString", startupFolder );
//        }
//
        if ( extraArgs != null )
            extraArguments = extraArgs;

    }

    public static void setupFonts( ) {

//        defaultLogicalFontName = Prefs.getProperty("defaultLogicalFontName", "SansSerif");
        defaultFontName = Prefs.getProperty( "defaultFontName", "SansSerif" );
        showAllFonts = Prefs.getProperty( "showAllFonts", "false" ).equals( "true" );

        defaultFontStyle = Integer.parseInt( Prefs.getProperty( "defaultFontStyle", "1" ) );
        defaultFontSize = Integer.parseInt( Prefs.getProperty( "defaultFontSize", "12" ) );
        defaultFont = new java.awt.Font( defaultFontName, defaultFontStyle, defaultFontSize );
        defaultBoldFont = new Font( defaultFont.getName(), Font.BOLD, defaultFont.getSize() );
    }

    public static void setupGraphAppearance() {
                showFooterOnPrint = Global.Prefs.getProperty( "showFooterOnPrint", "true" ).equals( "true" );
        showCutOrnamented = Global.Prefs.getProperty( "showCutOrnamented", "true" ).equals( "true" );

        defaultContextLabel = Global.Prefs.getProperty( "defaultContextLabel", "Proposition" );

        springLayoutMaxIterations = Integer.parseInt( Global.Prefs.getProperty( "maxIterations", "5000" ) );

        showGEdgeDisplayRect = Global.Prefs.getProperty( "showGEdgeDisplayRect", "true" ).equals( "true" );
        preferredEdgeLength = Double.parseDouble( Global.Prefs.getProperty( "preferredEdgeLength", "35" ) );

        showShadows = Global.Prefs.getProperty( "showShadows", "true" ).equals( "true" );
        showBorders = Global.Prefs.getProperty( "showBorders", "true" ).equals( "true" );
        showQuote   = Global.Prefs.getProperty( "showQuote", "true" ).equals( "true" );
        initializeDefaultColors();

        userEdgeAttributes.setArrowHeadWidth(
                Integer.parseInt( Global.Prefs.getProperty( "arrowHeadWidth", ""+factoryEdgeAttributes.getArrowHeadWidth()  ) ) );
        userEdgeAttributes.setArrowHeadHeight(
                Integer.parseInt( Global.Prefs.getProperty( "arrowHeadHeight", ""+factoryEdgeAttributes.getArrowHeadHeight()  ) ) );
        userEdgeAttributes.setEdgeThickness(
                Double.parseDouble( Global.Prefs.getProperty( "edgeThickness", ""+factoryEdgeAttributes.getEdgeThickness()  ) ) );
        userEdgeAttributes.setArrowPointLocation( Global.arrowPointLocation );
        defaultStroke = new BasicStroke( (float)Global.userEdgeAttributes.getEdgeThickness() );

        imageCopyScaleFactor = Double.parseDouble( Global.Prefs.getProperty( "imageCopyScaleFactor", "1.0" ) );


    }

    /**
     * Starts up the main charger application.
     *
     * @param driver
     * @param startupFolder the graphs folder, if one is set
     * @param extraArgs any command line options that aren't in the known set
     * @param diagnostics whether to turn on Global.info and other output
     * routines.
     */
    public static void setup1(  ) {
//        driverClass = driver;	// so we'll know who our owner is
        //========================================================================================
        //	 MAKE PLATFORM-SPECIFIC DECISIONS HERE

        // choose the right accelerator key -- APPLE specific here!
        Global.AcceleratorKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

        System.setProperty( "file.separator", "/");

//        if ( System.getProperty( "os.name" ).startsWith( "Mac " ) ) {
//            //java.awt.event.InputEvent.META_MASK;
//        } else {
//            if ( System.getProperty( "os.name" ).startsWith( "Linux" ) ) {
//                Global.consoleMsg( "Linux users: Please ignore \"Cannot convert ... to type VirtualBinding\" messages." );
//            }
//        }

        ignoreCase.setStrength( java.text.Collator.PRIMARY );

        // determine whether the JVM is up to date
        if ( Double.parseDouble( System.getProperty( "java.specification.version" ) ) < 1.7 ) {
            JOptionPane.showMessageDialog( null, "Your version of Java is "
                    + System.getProperty( "java.specification.version" ) + "."
                    + System.getProperty( "line.separator" ) + "Version 1.7 or greater is required.",
                    "Outdated Java version", JOptionPane.ERROR_MESSAGE );
        }
        WindowManager.setSorted( true );



        //========================================================================================
        //	 SET INTERNAL PROPERTIES, UNLESS THEY ARE ALREADY SET BY THE PREVIOUS LOADING

        GraphFolderString =  Prefs.getProperty( "GraphFolder" ,
                new File( System.getProperty( "user.home"), "Graphs" ).getAbsolutePath() );
//            Global.GraphFolderFile = new File( Global.GraphFolderString );
     Global.setGraphFolder(Global.GraphFolderString, true );
//        try {
//            JOptionPane.showMessageDialog(null,
//                    "graph folder string " + GraphFolderString + "\n"
//                            + "graph folder file absolute path " + GraphFolderFile.getAbsolutePath() + "\n"
//                                    + "graph folder file canonical path " + GraphFolderFile.getCanonicalPath() + "\n");
//
//        } catch ( IOException ex ) {
//            Logger.getLogger( Global.class.getDisplayName() ).log( Level.SEVERE, null, ex );
//        }
                    // After this point, can look for a preferences file in the graph folder
        DatabaseFolderString = Prefs.getProperty( "DatabaseFolder", "Databases" );
        DatabaseFolderFile = new File( DatabaseFolderString );
        LastFolderUsedForSave = new File( GraphFolderFile.getAbsoluteFile().getAbsolutePath() );
        LastFolderUsedForOpen = new File( GraphFolderFile.getAbsoluteFile().getAbsolutePath() );

        graphList = new Properties();

        Global.moduleNamesToEnableCommaSeparated = Global.Prefs.getProperty( "moduleNamesToEnableCommaSeparated", "" );


//        //========================================================================================
//
//        //	 ANYTHING TO DO WITH CRAFT (REQUIREMENTS TOOL)
////        craftEnabled = Global.Prefs.getProperty("craftEnabled", "false").equals("true");
//        Craft.CRAFTGridFolder = charger.Global.Prefs.getProperty( "GridFolder", "Graphs" );
//        Craft.CRAFTGridFolderFile = new File( Craft.CRAFTGridFolder);
//
//        Craft.CRAFTtryGenericSenseInRepGrid =
//                charger.Global.Prefs.getProperty( "CRAFTtryGenericSenseInRepGrid", "false" ).equals( "true" );
//        Craft.CRAFTuseOnlyBinaryRelationsinCraft =
//                charger.Global.Prefs.getProperty( "CRAFTuseOnlyBinaryRelationsinCraft", "false" ).equals( "true" );
//        Craft.CRAFTuseOnlyGenericConceptsinCraft =
//                charger.Global.Prefs.getProperty( "CRAFTuseOnlyGenericConceptsinCraft", "false" ).equals( "true" );
//
        wordnetEnabled = Global.Prefs.getProperty( "wordnetEnabled", "false" ).equals( "true" );
        matchingStrategy = Global.Prefs.getProperty( "matchingStrategy", "kb.matching.BasicTupleMatcher" );
//        mmatEnabled = Global.Prefs.getProperty("mmatEnabled", "false").equals("true");


        //========================================================================================
        //	 SET APPEARANCE AND FONT CHARACTERISTICS FOR OVERALL SYSTEM
        // HubFrame actually created here; if it's moved to the constructor, then multiple invocations
        //  of setup might be possible. Things will break in the constructor, however.
//           JOptionPane.showMessageDialog( null, "About to create master frame...");
      try {
            CharGerMasterFrame = new HubFrame();
//              JOptionPane.showMessageDialog( null, "Master frame created.");
     } catch ( ExceptionInInitializerError e ) {
    	 charger.Global.error( "Hub.setup internal error: can't create master frame: " + e.getMessage() );
           e.printStackTrace();
        }


//        WindowManager.setSorted( true );

        //BackToHubCmdItem.addActionListener( CharGerMasterFrame );
        //BackToHubCmdItem.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_B, AcceleratorKey ) );


        // here are the things that are dependent on having an actual frame
//                 JOptionPane.showMessageDialog( null, "Checking CharGerMasterFrame: " + CharGerMasterFrame);

        if ( CharGerMasterFrame == null ) {
            CharGerMasterFrame = new HubFrame();
        }
        defaultGraphics = (Graphics)CharGerMasterFrame.getGraphics();
        defaultGraphics.setFont( defaultFont );
        defaultFontMetrics = defaultGraphics.getFontMetrics();

//        JOptionPane.showMessageDialog( CharGerMasterFrame, "Launching hub frame:\n"
//                + "user.home = " + System.getProperty( "user.home") + "\n"
//                + "user.dir = " + System.getProperty( "user.dir") + "\n"
//                + "Stdout to " + ( Global.standardOutputToLog ? Global.getLogFile().getAbsolutePath() : "default" ) + "\n"
//        );

       setupFonts(  );


//========================================================================================
//	 GRAPH MODALITIES (see GraphModality class)

//        modalityLabelsActive = Prefs.getProperty("modalityLabelsActive", "false").equals("true");
        // even if modality stuff not active, still useful for defaults

        //NotioEnabled = Hub.Prefs.getProperty( "NotioEnabled", "false").equals( "true" );
//========================================================================================
//	 SET ACTOR AND ANIMATION CHARACTERISTICS

        ActorAnimation = Global.Prefs.getProperty( "ActorAnimation", "false" ).equals( "true" );
        AnimationDelay = Integer.parseInt( Global.Prefs.getProperty( "AnimationDelay", "1000" ) );
        AllowNullActorArguments = Global.Prefs.getProperty( "AllowNullActorArguments", "true" ).equals( "true" );
        use_1_0_actors = Global.Prefs.getProperty( "use_1_0_actors", "false" ).equals( "true" );
        enableActors = Global.Prefs.getProperty( "enableActors", "true" ).equals( "true" );
        enableCopyCorefs = Global.Prefs.getProperty( "enableCopyCorefs", "false" ).equals( "true" );
        allowActorLinksAcrossContexts = Global.Prefs.getProperty( "allowActorLinksAcrossContexts", "true" ).equals( "true" );
        GraphUpdater.registerPrimitives();

        GraphUpdater.registerPlugins( GraphUpdater.getPluginList() );

        //ExportCharGerInfo = Hub.Prefs.getProperty( "ExportCharGerInfo", "false").equals( "true" );
        includeCharGerInfoInCGIF = Global.Prefs.getProperty( "includeCharGerInfoInCGIF", "false" ).equals( "true" );

        exportSubtypesAsRelations = Global.Prefs.getProperty( "exportSubtypesAsRelations", "true" ).equals( "true" );
        importSubtypeRelationsAsHierarchy = Global.Prefs.getProperty( "importSubtypeRelationsAsHierarchy", "true" ).equals( "true" );

        enforceStandardRelations = Global.Prefs.getProperty( "enforceStandardRelations", "true" ).equals( "true" );
        saveHistoryRecords = Global.Prefs.getProperty( "saveHistoryRecords", "true" ).equals( "true" );

        ScalingFactor = Double.parseDouble( Global.Prefs.getProperty( "ScalingFactor", "1.0" ) );

        LinkToContextStrategy = Prefs.getProperty( "LinkToContextStrategy", "cut" );

        ShowBoringDebugInfo = Global.Prefs.getProperty( "ShowBoringDebugInfo", "false" ).equals( "true" );

        setupGraphAppearance();

        // NOTE: here is where the Craft window would be automatically opened
//        if (craftEnabled) {
//            CharGerMasterFrame.menuToolsCraftToolActionPerformed();
//        }

//        consoleMsg( "CRAFT Subsystem enabled: " + craftEnabled );
//        consoleMsg( "MMAT Subsystem enabled: " + mmatEnabled );
        consoleMsg( "WORDNET Interface enabled: " + wordnetEnabled );
    }

    /**
     * Marks which edit frame is the active frame. Null if there is none.
     *
     * @param ef the editing window that will become the new current window
     */
    public synchronized static void setCurrentEditFrame( EditFrame ef ) {
        // this method has been quite a hassle; seems to be causing frames to compete
        // for focus and get into ugly loops.
        String current = "null";
        String newone = "null";
        if ( CurrentEditFrame != null ) {
            current = "" + CurrentEditFrame.editFrameNum;
        }
        if ( ef != null ) {
            newone = "" + ef.editFrameNum;
        }
        //Global.info( "edit frame switch from " + current + "  to  " + newone );

        //Thread.dumpStack();

        if ( CurrentEditFrame != null ) {
            //removeCurrentEditFrame( CurrentEditFrame );
            // perhaps need to take away the focus, but how do we do that?
        }
        CurrentEditFrame = ef;
    }

    /**
     * @return the editing window that's supposed to be in front; null if some
     * other window (e.g., the Hub itself) is in focus.
     */
    public static EditFrame getCurrentEditFrame() {
        return CurrentEditFrame;
    }

    /**
     * Returns the newest edit frame created. Usually will be the one with the
     * highest frame number
     */
    public static EditFrame getNewestEditFrame() {
        Integer max = new Integer( -1 );
        // the newest edit frame has the highest frame number
        Iterator iter = editFrameList.keySet().iterator();
        while ( iter.hasNext() ) {
            Integer next = (Integer)iter.next();
            if ( next.intValue() > max.intValue() ) {
                max = next;
            }
        }
        return (EditFrame)editFrameList.get( max );
    }

    /**
     * Shouldn't be used; just use setCurrentEditFrame( null );
     */
    public synchronized static void removeCurrentEditFrame( EditFrame ef ) {
        if ( ef != CurrentEditFrame && ef != null && CurrentEditFrame != null ) {
            Global.error( "EditFrame " + ef.editFrameNum + " thinks it's current. "
                    + "EditFrame " + CurrentEditFrame.editFrameNum + " is actually current." );
        } else {
            CurrentEditFrame = null;
        }
    }

    /**
     * Housekeeping before we quit: calls closeOut on every open edit frame,
     * detaches every database, calls shutdown on any modules.
     */
    public static boolean closeOutAll() {
        EditFrame efToClose;
        //Iterator framelist = editFrameList.values().iterator();
        //info( "closing out all" );
        //while ( framelist.hasNext() )
        EditFrame framelist[] = (EditFrame[])editFrameList.values().toArray( new EditFrame[ 0 ] );
        for ( int framenum = 0; framenum < framelist.length; framenum++ ) {
            //efToClose = (EditFrame)framelist.next();
            efToClose = (EditFrame)framelist[ framenum];
            if ( !efToClose.closeOut() ) {
                return false;
            }
            efToClose = null; // 09-05-05 : maybe help with memory leaks
        }

        Iterator dbs = Global.ActiveDatabases.values().iterator();
        while ( dbs.hasNext() ) {
        	charger.db.CGDatabase db = (charger.db.CGDatabase)dbs.next();
            if ( db != null ) {
                ( (charger.db.TextDatabase)db ).closeDB();
            }
        }

        // Here is where we should call shutdown for all active modules.
        for ( ModulePlugin module : Global.modulePluginsActivated ) {
            module.saveProperties();
            module.shutdown();
        }

        return true;
    }

    /**
     * Find the module plugin instance for a given classname.
     * @param pluginClassName a simple name for a plugin class (e.g., "TestModulePlugin")
     * @return the singleton plugin instance for this classname
     */
    public static ModulePlugin getPluginInstanceForClassName( String pluginClassName ) {
        for ( ModulePlugin module : Global.modulePluginsAvailable ) {
            if ( module.getClass().getSimpleName().equals( pluginClassName )) {
                return module;
            }
        }
        return null;
    }

    /**
     * Gives a unique ID to be used in identifying graph objects
     *
     * @return A unique (ever-increasing) number, to be used for a graph objects
     */
    public synchronized static GraphObjectID applyForID() {
//        info( "UID is " + ( new java.rmi.server.UID()).toString() );
//        info( "UID with num is " + ( new java.rmi.server.UID( (short)50 )).toString() );
//        globalID++;
        TotalGobjs++;
//        return globalID;
        return new GraphObjectID();
    }

    /**
     * Informs the global store that an ID is no longer in use. Note: IDs are
     * not re-used; this merely reduces the count of active objects.
     */
    public static void deactivateID( GraphObjectID id ) {
        TotalGobjs--;
    }

    /**
     * Get the number of editing windows currently open. Does not include other
     * kinds of windows.
     *
     * @return The number of edit frames currently open
     */
    public static int getWindowCount() {
        return editFrameList.size();
    }

    /**
     * Get the number of graphs available to CharGer. The entire contents of a
     * single file is considered one graph, regardless of whether its components
     * are connected or not.
     *
     * @return The number of graphs available in the system
     */
    public static int getGraphCount() {
        return graphList.size();
    }

    /**
     * @return The number of graph objects currently being used in any system
     * graph
     */
    public static int getObjectCount() {
        return TotalGobjs;
    }

//    /**
//     *
//     */
//    public static String getInfoString() {
//        Date now = Calendar.getInstance().getTime();
//        String today = DateFormat.getDateTimeInstance( DateFormat.LONG, DateFormat.LONG ).format( now );
//        return EditorNameString + " " + CharGerVersion + " " + CharGerDate + " timestamp: " + today;
//
//    }


    /**
     * Set up the lookup tables that keep the colors for each kind of object.
     */
    public static void initializeDefaultColors() {
        //Global.info( "initializing default colors" );
        factoryForeground.put( "Concept", Color.white );
        factoryBackground.put( "Concept", Global.chargerBlueColor );

        factoryForeground.put( "Relation", Color.black );
        factoryBackground.put( "Relation", new Color( 255, 231, 100 ) );

        factoryForeground.put( "Graph", Global.chargerBlueColor );
        factoryBackground.put( "Graph", Global.chargerBlueColor );

        factoryForeground.put( "Actor", Color.black );
        factoryBackground.put( "Actor", Color.white );

        factoryForeground.put( "TypeLabel", Color.black );
        factoryBackground.put( "TypeLabel", Color.white );

        factoryForeground.put( "RelationLabel", Color.black );
        factoryBackground.put( "RelationLabel", Color.white );

        factoryForeground.put( "Coref", (Color)factoryBackground.get( "Concept" ) );
        factoryBackground.put( "Coref", (Color)factoryBackground.get( "Concept" ) );

        factoryForeground.put( "Arrow", Color.black );
        factoryBackground.put( "Arrow", Color.black );

        factoryForeground.put( "GenSpecLink", Color.black );
        factoryBackground.put( "GenSpecLink", Color.black );

        factoryForeground.put( "Note", Global.chargerBlueColor );
        factoryBackground.put( "Note", Color.white );


        // initialize black and white color scheme
        bwForeground = new Hashtable( 10 );
        bwBackground = new Hashtable( 10 );

        bwForeground.put( "Concept", Color.black );
        bwBackground.put( "Concept", Color.white );

        bwForeground.put( "Relation", Color.black );
        bwBackground.put( "Relation", Color.white );

        bwForeground.put( "Graph", Color.black );
        bwBackground.put( "Graph", Color.black );

        bwForeground.put( "Actor", Color.black );
        bwBackground.put( "Actor", Color.white );

        bwForeground.put( "TypeLabel", Color.black );
        bwBackground.put( "TypeLabel", Color.white );

        bwForeground.put( "RelationLabel", Color.black );
        bwBackground.put( "RelationLabel", Color.white );

        //bwForeground.put( "Coref", (Color)bwBackground.get( "Concept" ) );
        //bwBackground.put( "Coref", (Color)bwBackground.get( "Concept" )  );

        bwForeground.put( "Coref", Color.black );
        bwBackground.put( "Coref", Color.black );

        bwForeground.put( "Arrow", Color.black );
        bwBackground.put( "Arrow", Color.black );

        bwForeground.put( "GenSpecLink", Color.black );
        bwBackground.put( "GenSpecLink", Color.black );

        bwForeground.put( "Note", Color.black );
        bwBackground.put( "Note", Color.white );


        // initialize grayscale color scheme
        grayForeground = new Hashtable( 10 );
        grayBackground = new Hashtable( 10 );

        grayForeground.put( "Concept", Color.white );
        grayBackground.put( "Concept", new Color( 64, 64, 64 ) );

        grayForeground.put( "Relation", Color.black );
        grayBackground.put( "Relation", new Color( 216, 216, 216 ) );

        grayForeground.put( "Graph", Color.black );
        grayBackground.put( "Graph", Color.black );

        grayForeground.put( "Actor", Color.black );
        grayBackground.put( "Actor", Color.white );

        grayForeground.put( "TypeLabel", Color.black );
        grayBackground.put( "TypeLabel", Color.white );

        grayForeground.put( "RelationLabel", Color.black );
        grayBackground.put( "RelationLabel", Color.white );

        grayForeground.put( "Note", Color.black );
        grayBackground.put( "Note", Color.white );

        grayForeground.put( "Coref", (Color)grayBackground.get( "Concept" ) );
        grayBackground.put( "Coref", (Color)grayBackground.get( "Concept" ) );

        grayForeground.put( "Arrow", Color.black );
        grayBackground.put( "Arrow", Color.black );

        grayForeground.put( "GenSpecLink", Color.black );
        grayBackground.put( "GenSpecLink", Color.black );

        // start with default color scheme
        userForeground = (Hashtable)factoryForeground.clone();
        userBackground = (Hashtable)factoryBackground.clone();

        // override from user preferences
        setDefaultColorsFromPrefs( "Concept" );
        setDefaultColorsFromPrefs( "Relation" );
        setDefaultColorsFromPrefs( "Graph" );
        setDefaultColorsFromPrefs( "Actor" );
        setDefaultColorsFromPrefs( "TypeLabel" );
        setDefaultColorsFromPrefs( "RelationLabel" );
        setDefaultColorsFromPrefs( "Note" );

        // start user off with these colors
        //userForeground = (Hashtable)defaultForeground.clone();
        //userBackground = (Hashtable)defaultBackground.clone();
    }

    /**
     * Sets the colors to be used in applying colors.
     *
     * @param use whether to use factory default colors; if false, then user
     * defaults apply
     */
    /*public static void useColors( Hashtable<String, Color> fore, Hashtable<String, Color> back )
     {
     if ( use )
     {
     defaultForeground = factoryForeground;
     defaultBackground = factoryBackground;
     }
     else
     {
     defaultForeground = userForeground;
     defaultBackground = userBackground;
     }
     }
     */
    /**
     * sets the default colors from preference entries of the form
     * "defaultColor-Concept-fill" and "255,255,255". If there is no entry in
     * the prefs file, then do nothing.
     */
    public static void setDefaultColorsFromPrefs( String classname ) {

        String RGBtriplet = null;
        RGBtriplet = Global.Prefs.getProperty( "defaultColor-" + classname + "-text" );
        if ( RGBtriplet != null ) {
            userForeground.put( classname, parseRGB( RGBtriplet ) );
        }
        RGBtriplet = Global.Prefs.getProperty( "defaultColor-" + classname + "-fill" );
        if ( RGBtriplet != null ) {
            userBackground.put( classname, parseRGB( RGBtriplet ) );
        }
    }

    private static Color parseRGB( String RGBstring ) {
        int r, g, b;
        StringTokenizer toks = new StringTokenizer( RGBstring, "," );
        r = Integer.parseInt( new String( toks.nextToken() ) );
        g = Integer.parseInt( new String( toks.nextToken() ) );
        b = Integer.parseInt( new String( toks.nextToken() ) );
        return new Color( r, g, b );
    }

    /**
     * Gets the default foreground or the default background color scheme for
     * the given object.
     *
     * @param classname one of "Concept" "Relation" etc.
     * @param foreback either "text" or "fill"
     * @return the designated color
     */
    public static Color getDefaultColor( String classname, String foreback ) {
        Color rcolor = null;
        if ( foreback.equals( "text" ) ) {
            rcolor = (Color)userForeground.get( (Object)classname );
        } else {
            rcolor = (Color)userBackground.get( (Object)classname );
        }
        //Global.info( "default " + foreback + " color for " + classname + " is " + rcolor );
        return rcolor;
    }

    /**
     * Set a default color to override factory defaults. Factory defaults are
     * always available through Preferences.
     *
     * @param classname Name of the class whose text/fill is to be defaulted
     * @param foreback one of either "text" or "fill"
     * @param c The color to be made the current default.
     */
    public static void setDefaultColor( String classname, String foreback, Color c ) {
        if ( foreback.equals( "text" ) ) {
            userForeground.put( classname, c );
        } else {
            userBackground.put( classname, c );
        }
        Global.info( "new default " + foreback + " color for " + classname + " is " + c );
    }

    /**
     * Resets appearance on all editing frames. Sets the default font for every
     * open EditFrame.
     */
    public static void setAppearanceAll() {
        Font f = Global.defaultFont;	// assumes default Font is already set
        EditFrame efToSet;
        Iterator framelist = editFrameList.values().iterator();
        while ( framelist.hasNext() ) {
            efToSet = (EditFrame)framelist.next();
            efToSet.setMyFont( f );
        }

    }

    /**
     * Stores an entry for the "database" as indicated by its file name, and
     * sets up the file for reading, by getting the field headers from the first
     * line. If it's already active, then rewind the file and return its
     * reference from the table.
     *
     * @return the CGDatabase holding the active information.
     * @see charger.db.CGDatabase
     */
    public /*synchronized*/ static charger.db.CGDatabase activateDatabase( String filename )
            throws CGFileException {
        // info("hub activating " + filename );
        String simplename = General.getSimpleFilename( filename );
        charger.db.CGDatabase db = (charger.db.CGDatabase)ActiveDatabases.get( simplename );
        if ( db == null ) {
            try {
                db = new charger.db.TextDatabase( filename );
                ActiveDatabases.put( simplename, db );
                // info( "hub: it's a new database." );
            } catch ( CGFileException fe ) {
                // info( "Could not activate database " + filename );
                throw new CGFileException( "Database " + filename + " not found." );
            }
        } else {
            db.resetDB();
        }
        return db;
    }

    /**
     * Removes the entry for the edit frame, and removes the edit frame's graph
     * from the global list. Does not actually change the edit frame itself.
     */
    public static void deactivateDatabase( String filename ) {
        String simplename = General.getSimpleFilename( filename );
        charger.db.CGDatabase db = (charger.db.CGDatabase)ActiveDatabases.get( simplename );
        if ( db == null ) {
            return;
        } else {
            db.closeDB();
            ActiveDatabases.remove( simplename );
        }
    }

    /**
     * Stores an entry for the edit frame
     *
     * @return the window number (CharGer internal) that it created
     */
    public synchronized static int addEditFrame( EditFrame ef ) {
        // make sure that the key we're putting in the list is the same one we're returning
        LastWindow++;
        EditFrame a = (EditFrame)editFrameList.put( new Integer( LastWindow ), ef );
        if ( a != null ) {
            error( "Error adding frame number " + LastWindow + "." );
        }
        return LastWindow;
    }

    /**
     * Removes the entry for the edit frame, and removes the edit frame's graph
     * from the global list. Does not actually change the edit frame itself.
     *
     * @see EditFrame#closeOut
     */
    public synchronized static void removeEditFrame( EditFrame ef ) {
        //info( getWindowCount() + " edit frames at remove edit frame, edit frame list is " + editFrameList.toString() );
        EditFrame a = null;
        a = (EditFrame)editFrameList.remove( new Integer( ef.editFrameNum ) );
        if ( a == null ) {
            error( "Error removing frame number " + ef.editFrameNum + "." );
        }
        removeGraph( ef.TheGraph );
        //info( "now the window count is " + getWindowCount() );
        ef = null;
    }


    /**
     * Sets the default canvas font for all new edit frames. Also sets the
     * default font metrics, etc. Does not change the font for any edit frames
     * already open.
     *
     * @param f New font to be set.
     */
    public /*synchronized*/ static void setDefaultFont( Font f ) {
        defaultFont = f;
        defaultGraphics.setFont( f );
        defaultFontMetrics = defaultGraphics.getFontMetrics();
        if ( Global.CurrentEditFrame != null ) {
            CurrentEditFrame.getGraphics().setFont( f );
        }
        setAppearanceAll();
    }

    /**
     * Stores an entry for a given graph
     *
     * @return the graph's already-assigned ID number
     * @see Global#graphList
     */
    public synchronized static String addGraph( Graph g ) {
        Graph gg = (Graph)graphList.put( g.objectID, g );
        if ( gg != null ) {
            error( "Error adding graph id " + g.objectID + "." );
        }

        return g.objectID.toString();
    }

    /**
     * Removes the entry for the graph
     *
     * @see Global#graphList
     */
    public synchronized static void removeGraph( Graph g ) {
        Graph gg = (Graph)graphList.remove( g.objectID );
        if ( g == null ) {
            error( "Error removing graph number " + g.objectID + "." );
        }
    }

    /**
     * Query the user for a new graphs folder. Sets the global graph folder;
     * it's the HubFrame's job to refresh itself.
     *
     * @param f parent frame for the dialog.
     */
    public static void queryForGraphFolder( JFrame f ) {
        File newF = queryForFolder( f, GraphFolderFile, "Choose any file to use its parent directory for graphs" );
        if ( newF == null ) {
            return;
        } else {
            try {
                // CR-1002 here
                GraphFolderString = newF.getAbsolutePath();
//            Global.GraphFolderFile = new File( Global.GraphFolderString );
                Global.setGraphFolder(Global.GraphFolderString, true );
// CR-1001 08-16-19 hsd need found a way to write preferences without opening preferences panel
                PreferencesFrame pFrame = new PreferencesFrame();
                String prefsFilename = Global.getPrefFile().getAbsolutePath();
                pFrame.writer.savePreferences( prefsFilename );
                pFrame = null;
// Look for a preferences file in this new folder
            } catch ( CGFileException ex ) {
                Logger.getLogger( Global.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }
    }

    /**
     * Set the global graph folder and look for a new preferences file.
     *
     * @param newGraphFolder the absolute path for the new graph folder
     */
    public static void setGraphFolder( String newGraphFolder, boolean queryToReplacePrefs ) {
            GraphFolderFile =  new File( newGraphFolder );
                // look for a preferences file

                // if not found then return
                // else if no query then replace current preferences with new ones and call setup again
                // else query user whether to replace current preferences
    }

    /**
     * Query the user for a new graphs folder.
     *
     * @param f parent frame for the dialog.
     */
    public static void queryForDatabaseFolder( JFrame f ) {
        File newF = queryForFolder( f, DatabaseFolderFile, "Choose any file to use its parent directory for databases" );
        if ( newF == null ) {
            return;
        } else {
            DatabaseFolderString = newF.getAbsolutePath();
            DatabaseFolderFile = new File( DatabaseFolderString );
        }
    }

    /**
     * Query the user to identify a folder by choosing a file within it.
     *
     * @param f parent frame for the dialog.
     * @param initialDirectory where to initially point the chooser dialog
     * @param prompt title string for the dialog box
     * @return Folder of the file the user chooses; null if cancelled
     */
    public static File queryForFolder( JFrame f, File initialDirectory, String prompt ) {
        String userdir = System.getProperty( "user.dir" );
        String fname = null;
        //JFileChooser filechooser = new JFileChooser( initialDirectory.getAbsolutePath() );
        JFileChooser filechooser = new JFileChooser( initialDirectory );
        filechooser.setDialogTitle( prompt );

        int returned = filechooser.showOpenDialog( f );

        // if approved, then continue
        if ( returned == JFileChooser.APPROVE_OPTION ) {
            return filechooser.getSelectedFile().getParentFile();
        } else {
            return null;
        }
    }

//    /**
//     * Attempts to locate given file in "current" (unqualified) directory. If
//     * that doesn't work, then if useclasspath is true, try to find in either
//     * .jar file or classpath Raise CGFIleException if never found.
//     */
//    public static InputStream findInSearchPath( String fname, boolean useclasspath ) throws CGFileException {
//        FileInputStream fis = null;
//        ByteArrayInputStream bais = null;
//
//        String classpath = System.getProperty( "java.class.path" );
//
//        JOptionPane.showMessageDialog( null, "Looking for file " + fname + "\njava.class.path =\n" + System.getProperty( "java.class.path" )
//                + "\nuser.dir = \n" + System.getProperty( "user.dir" ) );
//        try {
//            fis = new FileInputStream( fname );
//            return fis;
//        } catch ( FileNotFoundException e ) {
//            if ( !useclasspath ) {
//                throw new CGFileException( "Can't find file " + fname + " in " + System.getProperty( "user.dir" ) );
//            } else if ( classpath.endsWith( ".jar" ) ) {
//                try {
//                    JarFile myJar = new JarFile( classpath );
//                    //Global.info( "jar file found " + classpath );
//                    JarResources jar = new JarResources( classpath );
//                    prefFileAsBytes = jar.getResource( fname );
//                    // if ( prefFileAsBtyes.length == 0 )
//                    bais = new ByteArrayInputStream( prefFileAsBytes );
//                    return bais;
//                } catch ( IOException ee ) {
//                    throw new CGFileException( "Can't find file " + fname + " in " + classpath + " "
//                            + ee.getMessage() );
//                }
//            } else {
//                // here if reading from normmal classpath
//                try {
//                    fis = new FileInputStream( classpath + File.separator + fname );
//                    return fis;
//                } catch ( FileNotFoundException ee ) {
//                    throw new CGFileException( "Can't find file " + fname + " in " + classpath + " "
//                            + ee.getMessage() );
//                }
//            }
//        }
//    }

    /**
     * Loads in the configuration from a text file.
     * Config file is always kept in the ".edu.uah.CharGer"
     * under the user's home directory.
     * @see Global#APPLICATION_CONFIG_FOLDER
     * @see Global#PREFERENCES_FILENAME
     * @param fname file name from preferences are to be loaded. File format is
     * <br> OptionName=OptionValue	<br> //=comment to be ignored <br>
     */
    public static void loadConfig( String fname ) throws CGFileException {
        InputStream is = null;
        try {
            // First look in the jar file under Config
            String fullname = "/Config/" + fname;
            info( "Load config: looking for config resource in jar at " + fullname );
            is = Global.class.getResourceAsStream( fullname );
            if ( is == null ) {
                // If that doesn't succeed, then look in the working directory.
                fullname = getPrefFile().getAbsolutePath();
                info( "Load config: looking for config file at " + fullname );
                is = new FileInputStream( new File( fullname ) );
            }
            if ( is != null ) {
                Prefs.load( is );
                consoleMsg( "Loaded config from: " + fullname );
            } else {
                warning( "Load config: Didn't find config file " + fname );
            }
        } catch ( IOException e ) {
            throw new CGFileException( e.getMessage() );
        }


    }

    /**
     * Invokes the system's page setup routine. Each editframe has its own page
     * format, if that's important.
     */
    public static void performActionPageSetup() {
        PrinterJob dummy = PrinterJob.getPrinterJob();
        if ( pformat == null ) {
            // initialize page setup for printing
            // want to default to Windows Landscape
            pformat = new PageFormat();
            pformat.setOrientation( PageFormat.LANDSCAPE );
        }

        pformat = dummy.pageDialog( pformat );
        Paper p = Global.pformat.getPaper();
        double pwidth = p.getWidth();
        double pheight = p.getHeight();
        double margin = 42.0; // 42/72 inch margin
        p.setImageableArea( margin, margin, pwidth - ( margin * 2 ), pheight - ( margin * 2 ) );
        pformat.setPaper( p );

        /*JOptionPane.showMessageDialog( ef,
         "height " + p.getHeight() + ", width " + p.getWidth() +
         ";  imageableh,w " + p.getImageableHeight() + ", " + p.getImageableWidth() +
         ";  imageablex,y " + p.getImageableX() + ", " + p.getImageableY(),
         "Paper" );
         */
    }

    /**
     * Wrapper for the LocaleStrings property list.
     *
     * @param key logical name of the string
     * @return text label that corresponds to the key
     */
    public static String strs( String key ) {
        return LocaleStrings.getProperty( key );
    }
//    /**
//     * Constructs an appropriate filename from the graphname and purpose
//     * constant
//     *
//     * @param name an arbitrary string
//     * @param modality one of the modality constants (e.g., DEF_abbr, etc. )
//     * @return file name with appropriate suffix appended
//     */
//    public static String makeUpFileName(String name, /*String modality,*/ String suffix) {
    // Global.info( "name " + name + " modality " + modality );
    // if ( name == null ) name = new String( "" );
    // if ( purpose == null ) purpose = new String( "" );
//        if (modalityLabelsActive) {
//            // if there's already a purpose suffix, strip it off first
//            String possiblesuffix = Util.getFileExtension(name);
//            if (GraphModality.isValidAbbr(possiblesuffix)) {
//                name = Util.stripFileExtension(name);
//            }
//            return name + "." + modality + suffix;
//        } // return new String( name + modality );
//        else {
//            return name + Hub.ChargerFileExtension;
//        }
//    }
//    public static PreferencesWindow pw;
    public static PreferencesFrame pf;

    /**
     * Should be called whenever the mix of external modules has changed,
     * either one or more being enabled, or one or more being disabled.
     */
    public static void setModulesChanged() {
        // Invalidate the preferences frame so that it will be re-built from scratch.
        pf = null;
    }

    public static void managePreferencesFrame() {

        if ( pf == null ) {
            pf = new PreferencesFrame();
            pf.setLocation( 400, 75 );
        }
        //
        pf.setVisible( true );
        pf.toFront();
    }


    /**
     * Checks for strings ending in charger cgx extension, but not starting with ".". Used in
     * <b>accept</b> methods required in various file filters. Treats all names
     * as lower case.
     *
     * @see FileFormat
     *
     * @return true if the file meets the criteria; false otherwise.
     */
    public static boolean acceptCGXFileName( String name ) {
        String n = name.toLowerCase();
        if ( n.startsWith( "." ) ) {
            return false;
        }
        if ( n.endsWith( "." + FileFormat.CHARGER4.extension() ) ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks for strings ending in ".cgif" or "CGIF", but not starting with
     * ".". Used in <b>accept</b> methods required in various file filters.
     *
     * @return true if the file meets the criteria; false otherwise.
     */
    public static boolean acceptCGIFFileName( String name ) {
        if ( name.startsWith( "." ) ) {
            return false;
        }
        if ( name.toLowerCase().endsWith( "." + FileFormat.CGIF2007.extension() ) ) {
            return true;
        }
            return false;
    }
    public static javax.swing.filechooser.FileFilter CGIFFileFilter = new javax.swing.filechooser.FileFilter() {
        public boolean accept( File f ) {
            if ( f.isDirectory() ) {
                return true;
            }
            return Global.acceptCGIFFileName( f.getName() );
        }

        public String getDescription() {
            return FileFormat.CGIF2007.description();
        }
    };
    public static javax.swing.filechooser.FileFilter CGXFileFilter = new javax.swing.filechooser.FileFilter() {
        public boolean accept( File f ) {
            if ( f.isDirectory() ) {
                return true;
            }
            return Global.acceptCGXFileName( f.getName() );
        }

        public String getDescription() {
            return FileFormat.CHARGER4.description();
        }
    };

    public static File queryForGraphFile( File sourceDirectoryFile ) {
        File sourceFile = null;

        JFileChooser filechooser = new JFileChooser( sourceDirectoryFile );
        filechooser.setDialogTitle( "Open existing CharGer graph" );
        filechooser.setFileFilter(CGXFileFilter );
        filechooser.setCurrentDirectory( Global.LastFolderUsedForOpen );


        int returned = filechooser.showOpenDialog( CharGerMasterFrame );

        // if approved, then continue
        if ( returned == JFileChooser.APPROVE_OPTION ) {
            sourceFile = filechooser.getSelectedFile();
            Global.LastFolderUsedForOpen = sourceFile.getAbsoluteFile().getParentFile();
            //sourceDirectoryFile = filechooser.getSelectedFile().getParentFile();
            return sourceFile.getAbsoluteFile();
        } else {
            return null; // throw new CGFileException( "user cancelled." );
        }
    }

    /**
     * Opens a new graph in its own editframe.
     *
     * @param filename the file from which to open a graph; null if a dialog is
     * to be invoked. Called from various open menus, buttons and routines. Has
     * two parts: getting the graph from the file and then setting up the
     * editing window. Not responsible for focus, etc.
     * @return the (short) filename of the file actually opened, generally only
     * useful when passed "null" to let the invoker know what file the user
     * chose. Returns null if the user canceled a dialog or there was any kind
     * of error.
     *
     * Should return a full File descriptor so that we can use it all over the
     * place
     */
    public synchronized static String openGraphInNewFrame( String filename ) {
        EditFrame ef = null;
        File sourceFile = null;
        File sourceAbsoluteFile = null;
        File sourceDirectoryFile = Global.GraphFolderFile;
        NumberFormat nformat = NumberFormat.getNumberInstance();
        nformat.setMaximumFractionDigits( 2 );
        nformat.setMinimumFractionDigits( 2 );
        Global.info( nformat.format( Runtime.getRuntime().freeMemory() / ( 1024d * 1024d ) ) + " M free" );

        if ( filename != null ) {
            sourceFile = new File( filename );
            if ( sourceFile.isAbsolute() ) {
                sourceAbsoluteFile = sourceFile;
            } else {
                sourceAbsoluteFile = new File( sourceDirectoryFile, sourceFile.getName() );
            }
            sourceDirectoryFile = sourceAbsoluteFile.getParentFile();
        } else {
            sourceAbsoluteFile = queryForGraphFile( sourceDirectoryFile );
        }

        if ( sourceAbsoluteFile == null ) {
            return null;
        }

        File newFile = null;

        try {
            Graph attempt = new Graph( null );
            IOManager iomgr = new IOManager( CharGerMasterFrame );
//                Global.info("in openGraphInFrame, file about to open is " + sourceAbsoluteFile.getAbsolutePath());
            newFile = IOManager.FileToGraph( sourceAbsoluteFile, attempt );
//                Global.info("file just opened is " + newFile.getAbsolutePath());
            if ( newFile == null ) {
                return null;
            }

            ef = new EditFrame( newFile, attempt, false );
            if ( ef != null ) {
                if ( Global.enableEditFrameThreads ) {
                    new Thread( Global.EditFrameThreadGroup, ef, newFile.getName() ).start();
                }
            }


            // Global.info("    and started its thread" );
        } catch ( CGFileException cgfe ) {
            //{ Hub.warning( "Problem opening " + filename + ": " + cgfe.getMessage() );
            JFrame current = CharGerMasterFrame;
            if ( CurrentEditFrame != null ) {
                current = CurrentEditFrame;
            }
            JOptionPane.showMessageDialog( current, cgfe.getMessage(), "Input File Error",
                    JOptionPane.ERROR_MESSAGE, Global.applicationIcon );
            // Should probably collect all warnings here and just dump them when done (if loading multiple files)
        } catch ( CGStorageError cgse ) { //Hub.warning( "Problem creating graph for " + newFile.getAbsolutePath() + ": " + cgse.getMessage());
        }
        //Global.info( "finished openGraphInFrame with file " + newFile.getAbsolutePath() );
        if ( newFile == null ) {
            //Hub.warning( "Problem creating graph for " +
            //	sourceAbsoluteFile.getAbsolutePath()  + "\nFile is probably not recognized.");
            //ef = null; // 09-05-05 : maybe help with memory leaks
            return "invalid file.";
        } else {
            // here is a good place to check for any autonomous actors and start them up!!!
            GraphUpdater.startupAllActors( ef.TheGraph );
            return newFile.getAbsolutePath();
        }
    }

    /**
     * Displays an error message on System.out
     *
     * @param s the error message to be shown
     */
    public static void error( String s ) {
        System.out.println( "CharGer Internal ERROR: " + s );
    }

    /**
     * Displays a warning message on System.out
     *
     * @param s the warning message to be shown
     */
    public static void warning( String s ) {
        System.out.println( "WARNING: " + s );
    }

    /**
     * Displays a status message on System.out
     *
     * @param s the status message to be shown
     * @see Global#infoOn
     * @see Global#ShowBoringDebugInfo
     */
    public static void info( String s ) {
        if ( !infoOn  ) {
            return;
        }
        if ( s.equals( "" ) ) {
            System.out.println( "" );
        } else {
            System.out.println( "INFO-MSG: " + s );
        }
    }

    /**
     * Displays a status message on System.out
     *
     * @param s the status message to be shown
     */
    public static void consoleMsg( String s ) {
        if ( s.equals( "" ) ) {
            System.out.println( "" );
        } else {
            System.out.println( "CharGer: " + s );
        }
    }
}
