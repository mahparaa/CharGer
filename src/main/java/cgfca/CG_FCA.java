/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cgfca;

import java.util.ArrayList;

import charger.obj.Concept;
import charger.obj.Graph;
import charger.obj.ShallowIterator;
import chargerlib.WindowManager;
import kb.BinaryTuple;
import kb.matching.BinaryRelationMatch;
import kb.matching.MatchedBinaryTuple;

/**
 * Module support for exporting binary tuples to a CXT file, according to Polovina and
 * Andrews work.
 *
 * @author Harry S. Delugach (delugach@uah.edu)
 */
public class CG_FCA {

    public static CG_FCA_Window cgFCAWindow = null;
    
    public static boolean enableCoreferents = true;
    
    public static CGFCA_Context gridForCGFCA = null;

    /**
     * Instantiate the main window.
     */
    public static void startupCGFCA() {
        cgFCAWindow = new CG_FCA_Window();
        WindowManager.manageWindow( cgFCAWindow );
        cgFCAWindow.setVisible( true );

        // ADD THIS - refresh the dropdown after the window is shown and visible
        cgFCAWindow.refresh();
    }

    /**
     * If not already instantiated, instantiate the main window, and bring to
     * front.
     */
    public static void activateCGFCA() {
        if ( cgFCAWindow == null ) {
            startupCGFCA();
        }
        cgFCAWindow.toFront();
        cgFCAWindow.requestFocus();

        // ADD THIS - refresh the dropdown every time the window is activated
        cgFCAWindow.refresh();
    }

    public static void shutdownCGFCA() {
        if ( cgFCAWindow != null ) {
            cgFCAWindow.dispose();
            cgFCAWindow = null;
        }
    }

    public static CGFCA_Context getGridForCGFCA() {
        return gridForCGFCA;
    }
    
    /**
     * Show the first concept and relation as a binary tuple.
     * For example, <code>[Cat: Misha] -> (sit)</code> would become <code>(Cat: Misha, sit)</code>
     * @param bt
     * @return
     */
    public static String binaryToString( BinaryTuple bt ) {
        // hsd 07-20-20 from Simon's email
        return bt.getFromConcept().getTextLabel()+ " " + bt.getRelation().getTypeLabel();
//        return "(" + bt.getFromConcept().getTextLabel()+ ", " + bt.getRelation().getTypeLabel() + ")";
    }

    /**
     * The primary method for implementing the CG-FCA algorithms.
     * Creates a PathFinder with a populated repertory grid, such that
     * all the grid cells have a value which has zero or more paths between each attribute and element.
     * @param graph
     * @return
     * @see PathFinder
     */
    public static PathFinder generateCGFCA( Graph graph ) {
       // RepertoryGrid gridForCGFCA = new RepertoryGrid();
        gridForCGFCA = new CGFCA_Context();
        gridForCGFCA.setValueType( new FCA_RGPathValue() );

        BinaryRelationMatch match = new BinaryRelationMatch( graph, graph.getReferent() );
        ArrayList<MatchedBinaryTuple> matchedBinaryTuples = match.makeBinaryTuples( graph );

        ArrayList<BinaryTuple> binaryTuples = new ArrayList<>();
        for ( MatchedBinaryTuple mbt : matchedBinaryTuples ) {
            binaryTuples.add( (BinaryTuple)mbt );
        }

        ArrayList<Concept> targetConcepts = new ArrayList<>();
        // the 1st concept is ignored -- only the relation and the target concept are considered.
        ArrayList<BinaryTuple> binaries = new ArrayList<>();

        // show all concepts

        ShallowIterator conceptIterator = new ShallowIterator( graph, new Concept() );
        while ( conceptIterator.hasNext() ) {
            Concept con = (Concept)conceptIterator.next();
            targetConcepts.add( con );
        }

        for ( BinaryTuple bt : binaryTuples ) {

            BinaryTuple binary = new BinaryTuple();
            binary.setFromConcept( bt.getFromConcept() );
            binary.concept1_label = bt.getFromConcept().getTextLabel();
            binary.setRelation( bt.getRelation() );
            binary.relation_label = bt.getRelation().getTextLabel();
            binary.setToConcept( null );
            binaries.add( binary );
        }

        for ( BinaryTuple bt : binaries ) {
                gridForCGFCA.addAttributeAsTuple( bt );
//            gridForCGFCA.addAttribute( binaryToString( bt ) );
        }

        for ( Concept con : targetConcepts ) {
            gridForCGFCA.addElementAsConcept( con );
        }

        PathFinder pf = new PathFinder( gridForCGFCA, binaries, targetConcepts );

        pf.buildAllPaths();

        return pf;
    }

}
