/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package charger.prefs;

import charger.EditFrame;
import charger.EditManager;
import charger.Global;
import charger.exception.CGFileException;
import charger.layout.SpringGraphLayout;
import charger.obj.Graph;
import charger.obj.GraphObject;
import charger.util.CGUtil;
import chargerlib.CDateTime;
import chargerplugin.ModulePlugin;
import craft.CraftPrefPanel;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.regex.Pattern;

/**
 * Handles the formatting and output of preferences to a configuration file.
 * Preferences are saved in a form suitable to be read by a standard Java Properties class.
 * They are read in to the Global class all in one go, and thence accessed by the Global setup()
 * to initialize most of the global constants. 
 * @author Harry S. Delugach (delugach@uah.edu)
 */
public class PreferencesWriter {

    AppearancePrefPanel appearance;
    CompatibilityPrefPanel compat;
    ActorsPrefPanel actors;
//    CraftPrefPanel craftPanel;
    ConfigurationPrefPanel configPanel;
    

    public PreferencesWriter( AppearancePrefPanel appearance, CompatibilityPrefPanel compat, 
            ActorsPrefPanel actors, ConfigurationPrefPanel configPanel ) {
        this.appearance = appearance;
        this.compat = compat;
        this.actors = actors;
//        this.craftPanel = craftPanel;
        this.configPanel = configPanel;
    }

    
    
    
    /**
     * Format the given setting as a string, for output to the preferences file.
     * @param name the key in the Prefs property list
     * @param value an integer value
     * @param help a descriptive string (e.g., one obtained from a tool tip)
     * @return string formatted for a property list file
     */
    protected String intPrefToString( String name, int value, String help ) {
//        Global.Prefs.setProperty( name, value + "" );
        return "#\t" + help + Global.LineSeparator
                + name + " = " + value + Global.LineSeparator + Global.LineSeparator;
    }
    /**
     * Format the given setting as a string, for output to the preferences file.
     * @param name the key in the Prefs property list
     * @param value a float value
     * @param help a descriptive string (e.g., one obtained from a tool tip)
     * @return string formatted for a property list file
     */

    protected String floatPrefToString( String name, float value, String help ) {
        Global.Prefs.setProperty( name, value + "" );
        return "#\t" + help + Global.LineSeparator
                + name + " = " + value + Global.LineSeparator + Global.LineSeparator;
    }

        /**
     * Format the given setting as a string, for output to the preferences file.
     * @param name the key in the Prefs property list
     * @param value a double value
     * @param help a descriptive string (e.g., one obtained from a tool tip)
     * @return string formatted for a property list file
     */

    protected String doublePrefToString( String name, double value, String help ) {
        Global.Prefs.setProperty( name, value + "" );
        return "#\t" + help + Global.LineSeparator
                + name + " = " + value + Global.LineSeparator + Global.LineSeparator;
    }

        /**
     * Format the given setting as a string, for output to the preferences file.
     * @param name the key in the Prefs property list
     * @param value a string value
     * @param help a descriptive string (e.g., one obtained from a tool tip)
     * @return string formatted for a property list file
     */

    protected String StringPrefToString( String name, String value, String help ) {
        Global.Prefs.setProperty( name, value );
        return "#\t" + help + Global.LineSeparator + name + " = " + value + Global.LineSeparator + Global.LineSeparator;
    }

        /**
     * Format the given setting as a string, for output to the preferences file.
     * @param name the key in the Prefs property list
     * @param value a  boolean value
     * @param help a descriptive string (e.g., one obtained from a tool tip)
     * @return string formatted for a property list file
     */

    protected String booleanPrefToString( String name, boolean value, String help ) {
        Global.Prefs.setProperty( name, value + "" );
        return "#\t" + help + Global.LineSeparator + name + " = " + ( value ? "true" : "false" )
                + Global.LineSeparator + Global.LineSeparator;
    }

        /**
     * Given a class name, return two text lines conveying the class's default
     * color scheme suitable for writing to a preferences file. First line is
     * the text color, the second is the fill color.
     *
     * @param classname the (unqualified) class name (i.e., without package
     * info)
     */
    public String textAndFillDefaultToString( String classname ) {
        return colorPrefToString( classname, "text" )
                + colorPrefToString( classname, "fill" );
    }

    /**
     * Handles occurrence of backslash in Windows filenames, replacing them with
     * forward slashes in writing.
     *
     * @param filename
     * @return if Windows, then replace backslashes with forward slashes,
     * otherwise return original.
     */
    public String fileToString( String filename ) {
        // CR-1004 08-16-19 hsd
        if ( System.getProperty( "os.name" ).startsWith( "Windows" ) ) {
            return filename.replaceAll( Pattern.quote( "\\" ), "/" );
        } else {
            return filename;
        }
    }
    /**
     * Given a class name and whether text or fill, creates the preferences file
     * entry for it. It returns a single line of the form:
     * <br><code>defaultColor-classname-&lt;text|fill&lt;=R,G,B</code> <br>e.g.,
     * <code>defaultColor-Concept-text=255,255,255</code>
     *
     */
    public String colorPrefToString( String classname, String foreback ) {
        Color rcolor = null;
        if ( foreback.equals( "text" ) ) {
            rcolor = (Color)Global.userForeground.get( (Object)classname );
        } else {
            rcolor = (Color)Global.userBackground.get( (Object)classname );
        }
        Global.Prefs.setProperty( "defaultColor-" + classname + "-" + foreback,
                rcolor.getRed() + "," + rcolor.getGreen() + "," + rcolor.getBlue() );
        return "#\t" + "default color using integer RGB values" + Global.LineSeparator
                + "defaultColor-" + classname + "-" + foreback + "="
                + rcolor.getRed() + "," + rcolor.getGreen() + "," + rcolor.getBlue()
                + Global.LineSeparator + Global.LineSeparator;
    }

    /** Saves the global preferences to the global preference file.
     * If there are any plugins active, saves their preferences too.
     * @param prefsFilename
     * @throws CGFileException 
     */
    public void savePreferences( String prefsFilename ) throws CGFileException {
        OutputStreamWriter osw = null;
        FileWriter fw = null;

        // for now, updates of the Prefs property list are piggy-backed onto the getPrefsAsString method
        String prefs = prefsToString();

        try {
            new File( prefsFilename ).getParentFile().mkdirs();
            fw = new FileWriter( prefsFilename );
            fw.write( prefs );
            fw.close();
//            Global.Prefs.store( new FileOutputStream( prefsFilename ), Global.getInfoString() );
            CGUtil.showMessageDialog(null, "Preferences saved in:\n" + Global.prefFile.getAbsolutePath() );
        } catch ( IOException e ) {
            throw new CGFileException( "IO Exception: " + e.getMessage() );
        }
        // Save any module properties
        for ( ModulePlugin module : Global.modulePluginsActivated) {
            module.saveProperties();
        }
    }

    /**
     * When creating a new Preference, the following places need to be changed:
     * <ul> <li>the Preferences class (and window) need to have some sort of
     * control for the preference <li>charger.Global (or other appropriate
     * class) needs to have a "global" with the preference current value (and
     * default value) <li>getPrefsAsString needs to have an entry for the
     * preference <li>methods need to be provided in this class to handle
     * changes to the Hub's value </ul>
     */
    protected String prefsToString() {
        return "# " + new CDateTime().formatted( Global.ChargerDefaultDateTimeStyle) + Global.LineSeparator
                // Appearance prefs
                + "# This file generated automatically! " + Global.LineSeparator
                + "# ====== APPEARANCE preferences" + Global.LineSeparator
                + booleanPrefToString( "showGEdgeDisplayRect", Global.showGEdgeDisplayRect,
                appearance.showGEdgeDisplayRect.getToolTipText() )
                + booleanPrefToString( "showShadows", Global.showShadows,
                appearance.showShadows.getToolTipText() )
                + booleanPrefToString( "showBorders", Global.showBorders,
                appearance.showBorders.getToolTipText() )
                + booleanPrefToString( "showQuote", Global.showQuote,
                appearance.showBorders.getToolTipText() )
                + booleanPrefToString( "defaultWrapLabels", GraphObject.defaultWrapLabels,
                appearance.wrapLabels.getToolTipText() )
                + intPrefToString( "defaultWrapColumns", GraphObject.defaultWrapColumns,
                appearance.wrapColumns.getToolTipText() )
                + doublePrefToString( "preferredEdgeLength", Global.preferredEdgeLength,
                appearance.preferredEdgeLength.getToolTipText() )
                + booleanPrefToString( "showFooterOnPrint", Global.showFooterOnPrint,
                appearance.showFooterOnPrint.getToolTipText() )
                + intPrefToString( "arrowHeadWidth", Global.userEdgeAttributes.getArrowHeadWidth(), 
                    appearance.arrowHeadWidth.getToolTipText())
                + intPrefToString( "arrowHeadHeight", Global.userEdgeAttributes.getArrowHeadHeight(), 
                    appearance.arrowHeadHeight.getToolTipText() )
                + doublePrefToString( "edgeThickness", Global.userEdgeAttributes.getEdgeThickness(), 
                    appearance.edgeThickness.getToolTipText())
                // Colors prefs (part of Appearance prefs)
                + "# ---- Colors preferences" + Global.LineSeparator
                + textAndFillDefaultToString( "Concept" )
                + textAndFillDefaultToString( "Relation" )
                + textAndFillDefaultToString( "Graph" )
                + textAndFillDefaultToString( "Actor" )
                + textAndFillDefaultToString( "TypeLabel" )
                + textAndFillDefaultToString( "RelationLabel" )
                + textAndFillDefaultToString( "Note" )
                // Context prefs (part of Appearance prefs)
                + "# ---- Context preferences" + Global.LineSeparator
                + booleanPrefToString( "showCutOrnamented", Global.showCutOrnamented,
                appearance.showCutOrnamented.getToolTipText() )
                + StringPrefToString( "defaultContextLabel", Global.defaultContextLabel,
                appearance.contextLabelList.getToolTipText() )
                + intPrefToString( "contextBorderWidth", Graph.contextBorderWidth,
                appearance.contextBorderWidth.getToolTipText() )
                + doublePrefToString( "imageCopyScaleFactor", Global.imageCopyScaleFactor,
                        appearance.imageCopyScaleFactor.getToolTipText())
                // Font prefs (part of Appearance Panel 
                + "# ------- Font preferences" + Global.LineSeparator
                //                + StringPrefToString( "defaultLogicalFontName", Global.defaultLogicalFontName,
                //                "name of Java platform-independent font to use" )
                + StringPrefToString( "defaultFontName", Global.defaultFont.getFontName(),
                "name of font to use" )
                + intPrefToString( "defaultFontStyle", Global.defaultFont.getStyle(),
                "style of font to use, as defined in java.awt.Font" )
                + intPrefToString( "defaultFontSize", Global.defaultFont.getSize(),
                "size of font to use" )
                + booleanPrefToString( "showAllFonts", Global.showAllFonts,
                appearance.fontPanel.showAllFonts.getToolTipText() )
                + "# =============== COMPATIBILITY preferences" + Global.LineSeparator
                
                
                + booleanPrefToString( "includeCharGerInfoInCGIF", Global.includeCharGerInfoInCGIF,  
                compat.includeCharGerInfoInCGIF.getToolTipText() )
                + booleanPrefToString( "exportSubtypesAsRelations", Global.  exportSubtypesAsRelations,
                compat.exportSubtypesAsRelations.getToolTipText() )
                + booleanPrefToString( "importSubtypeRelationsAsHierarchy", Global.  importSubtypeRelationsAsHierarchy,
                compat.importSubtypeRelationsAsHierarchy.getToolTipText() )
                + booleanPrefToString( "enforceStandardRelations", Global.enforceStandardRelations,
                compat.enforceStandardRelations.getToolTipText() )
                + booleanPrefToString( "saveHistoryRecords", Global.saveHistoryRecords,
                compat.saveHistoryRecords.getToolTipText() )
                + booleanPrefToString( "wordnetEnabled", Global.wordnetEnabled,
                compat.wordnetEnabled.getToolTipText() )
                + StringPrefToString( "wordnetDictionaryFilename", charger.gloss.wn.WordnetManager.wordnetDictionaryFilename,
                compat.wordnetDictField.getToolTipText() )
                + StringPrefToString( "matchingStrategy", Global.matchingStrategy,
                compat.matchingStrategyList.getToolTipText() )
                + booleanPrefToString( "ActorAnimation", Global.ActorAnimation,
                actors.actorAnimation.getToolTipText() )
                + intPrefToString( "AnimationDelay", Global.AnimationDelay,
                actors.animationDelay.getToolTipText() )
                + StringPrefToString( "GraphFolder", fileToString( Global.GraphFolderString ),  // CR-1004 08-16-19 hsd
                compat.GraphFolderField.getToolTipText() )
                + StringPrefToString("DatabaseFolder", fileToString( Global.DatabaseFolderString), // CR-1004 08-16-19 hsd
                actors.DatabaseFolderField.getToolTipText() )
                + booleanPrefToString( "allowActorLinksAcrossContexts", Global.allowActorLinksAcrossContexts,
                actors.allowActorLinksAcrossContexts.getToolTipText() )
                + booleanPrefToString( "allowNullActorArguments", Global.AllowNullActorArguments,
                actors.allowNullActorArguments.getToolTipText() )
                + booleanPrefToString( "enableActors", Global.enableActors,
                actors.enableActors.getToolTipText() )
                + booleanPrefToString( "enableCopyCorefs", Global.enableCopyCorefs,
                actors.enableCopyCorefs.getToolTipText() )
                + booleanPrefToString( "use_1_0_actors", Global.use_1_0_actors,
                actors.use_1_0_actors.getToolTipText() )
                // CRAFT-specific preferences

//                + "# =============== CRAFT (requirements acquisition) preferences" + Global.LineSeparator
//                + StringPrefToString( "CRAFTGridFolder", fileToString( Global.CRAFTGridFolder ),    // CR-1004 08-16-19 hsd
//                craftPanel.gridFolderField.getToolTipText() )
//                + booleanPrefToString( "CRAFTtryGenericSenseInRepGrid", Global.CRAFTtryGenericSenseInRepGrid,
//                craftPanel.tryGenericSenseInRepGrid.getToolTipText() )
//                + booleanPrefToString( "CRAFTuseOnlyBinaryRelationsinCraft", Global.CRAFTuseOnlyBinaryRelationsinCraft,
//                craftPanel.useOnlyBinaryRelationsinCraft.getToolTipText() )
//                + booleanPrefToString( "CRAFTuseOnlyGenericConceptsinCraft", Global.CRAFTuseOnlyGenericConceptsinCraft,
//                craftPanel.useOnlyGenericConceptsinCraft.getToolTipText() )
//
                + "# =============== Config panel preferences" + Global.LineSeparator
                + booleanPrefToString("ShowBoringDebugInfo", Global.ShowBoringDebugInfo,
                configPanel.ShowBoringDebugInfo.getToolTipText() )
                + intPrefToString( "maxIterations", SpringGraphLayout.maxIterations,
                "Maximum number of times to run the spring relaxation algorithm for layout" )
                + StringPrefToString( "moduleNamesToEnableCommaSeparated", Global.moduleNamesToEnableCommaSeparated,
                "Comma-separated list of ModulePlugin class names that the user wants enabled" )

                + "# =============== factory-only preferences" + Global.LineSeparator
//                + booleanPrefToString( "craftEnabled", Global.craftEnabled, "whether the CRAFT subsystem is enabled" )
                + intPrefToString( "defaultFrameHeight", EditFrame.defaultFrameHeight, "height of the editing window" )
                + intPrefToString( "defaultFrameWidth", EditFrame.defaultFrameWidth, "width of the editing window" )
                + intPrefToString( "defaultGraphObjectHeight", (int)GraphObject.defaultDim.getHeight(), "height of a typical object" )
                + intPrefToString( "defaultGraphObjectWidth", (int)GraphObject.defaultDim.getWidth(), "width of a typical object" )
                + intPrefToString( "defaultGraphObjectHeight", (int)GraphObject.defaultDim.getDepth(), "depth (3D) of a typical object" )
                + intPrefToString( "defaultMaxUndo", EditManager.maxUndo, "maximum number of undoable actions" )
                + intPrefToString( "TerritorialLimit", GraphObject.TerritorialLimit,
                "The \"3-mile limit\" for GNodes, inside of which no other GNode can be created, but one can be moved" )
                + intPrefToString( "xoffsetForCopy", EditFrame.xoffsetForCopy, "x offset for copy or duplicate" )
                + intPrefToString( "yoffsetForCopy", EditFrame.yoffsetForCopy, "y offset for copy or duplicate" )
                + doublePrefToString( "arrowPointLocation", Global.arrowPointLocation, "how far down the line to draw arrowhead" ) 
                ;
    }

}
