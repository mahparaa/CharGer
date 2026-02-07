/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cgfca;

import java.util.ArrayList;
import java.util.Iterator;

import charger.Global;
import charger.obj.Concept;
import charger.obj.GEdge;
import charger.obj.GNode;
import charger.obj.Relation;
import kb.BinaryTuple;
import repgrid.RGCell;

/**
 * The methods needed to support path finding in a repertory grid. The repertory
 * grid is used for convenience since its structure is similar to a formal
 * context.
 *
 * @author Harry S. Delugach (delugach@uah.edu)
 */
public class PathFinder {

    CGFCA_Context grid = null;
    ArrayList<BinaryTuple> binaries = null;
    ArrayList<Concept> targetConcepts = null;

    String cxtContent = null;
    String reportContent = null;
    String filename = null;

    boolean reportOnlyInputOutputPaths = true;

    public PathFinder( CGFCA_Context grid, ArrayList<BinaryTuple> binaries, ArrayList<Concept> targetConcepts ) {
        this.grid = grid;
        this.binaries = binaries;
        this.targetConcepts = targetConcepts;
    }

    /**
     * Should reporting of paths only include ones with "input" and "output" concepts as terminals (also cycles).
     * @return
     */
    public boolean isReportOnlyInputOutputPaths() {
        return reportOnlyInputOutputPaths;
    }

    public void setReportOnlyInputOutputPaths( boolean reportOnlyInputOutputPaths ) {
        this.reportOnlyInputOutputPaths = reportOnlyInputOutputPaths;
    }

    /**
     * The filename of the graph being analyzed. Includes the ".cgx" extension.
     * @return
     */
    public String getFilename() {
        return filename;
    }

    public void setFilename( String filename ) {
        this.filename = filename;
    }




    /**
     * Accesses the current paths (if any) between a binary tuple and the given
     * concept.
     *
     * @param bt
     * @param con
     * @return empty list if there are no current paths; otherwise, the list of
     * current paths.
     */
    public ArrayList<Path> getCurrentPaths( BinaryTuple bt, Concept con ) {
        ArrayList<Path> existingPaths = new ArrayList<>();
        RGCell cell = grid.getCell( grid.getAttribute(  bt ), grid.getElement( con ) );
//        RGCell cell = grid.getCell( grid.getAttribute( CG_FCA.binaryToString( bt ) ), grid.getElement( con.getTextLabel() ) );
//        if ( cell == null || !cell.hasValue() ) {
//            return existingPaths;
//        }
        FCA_RGPathValue value = (FCA_RGPathValue)cell.getRGValue();
        return value.getPaths();
    }

    /**
     *
     * Add the given path to the list of current paths, if it is not already there.
     * @param path the path to add
     * @param bt
     * @param con
     * @return true if the path was added; false if it was already there.
     * @see Path#equals(cgfca.Path)
     */
    public boolean addToCurrentPaths( Path path, BinaryTuple bt, Concept con ) {
//        ArrayList<Path> existingPaths = new ArrayList<>();
//        RGCell cell = grid.getCell( grid.getAttribute( CG_FCA.binaryToString( bt ) ), grid.getElement( con.getTextLabel() ) );
        RGCell cell = grid.getCell( grid.getAttribute( bt ), grid.getElement( con ) );

//        if ( cell == null || !cell.hasValue() ) {
//            // this is an error that should never occur
//            return false;
//        }
        FCA_RGPathValue valueObj = (FCA_RGPathValue)cell.getRGValue();
        if ( isPathAlreadyInList( path, valueObj.getPaths()) )
            return false;
        else {
//            valueObj.getPaths().add( path );
            valueObj.addPath( path );
            valueObj.booleanvalue = true;
            return true;
        }

    }

    /** Whether a path belongs in an input-output report.
     *
     * @param path
     * @return true if the path is a cycle or if the path ends are input and output concepts.
     */
    public boolean belongsInInputOutputReport( Path path ) {
        if ( path.isCycle())
            return true;
        if ( isInput( (Concept)path.firstNode() ) && isOutput( (Concept)path.lastNode())) {
            return true;
        }
        return false;
    }

    public String getReportContent() {
        reportContent = new String( "" );
        String eol = System.getProperty( "line.separator" );

        String inputs = new String( "" );
        String outputs = new String( "" );
        for ( Concept con : targetConcepts ) {
            if ( isInput( con )) {
                inputs += "\"" + con.getTextLabel() + "\", ";
            }
            if ( isOutput( con )) {
                outputs += "\"" + con.getTextLabel() + "\", ";
            }
        }

        reportContent += "Inputs: " + inputs + eol + eol;
        reportContent += "Outputs: " + outputs + eol + eol;


//        this.reportContent += eol;
        for ( BinaryTuple bt : binaries ) {
            for ( Concept con : targetConcepts ) {
//                Global.info( "Path(s) from attr " + binaryToString( bt ) + " to " + con.getTextLabel() + ":" );
                ArrayList<Path> currentPaths = this.getCurrentPaths( bt, con );
                if ( currentPaths.size() == 0 ) {
//                    Global.info( "   none" );
                } else {
                    for ( Path p : currentPaths ) {
                        if ( ! this.reportOnlyInputOutputPaths || this.belongsInInputOutputReport( p )) {
                            String pathString = p.toString();
                            if ( pathString.endsWith( " - "))
                                pathString = pathString.substring( 0, pathString.length() - 3 ) ;
//                        Global.info( "   " + pathString);
                        reportContent += pathString + eol + eol;
                        }
                    }
                }
            }
        }

        return this.reportContent;
    }

    public String getCxtContent() {
        // CR-1008 hsd the grid is probably incorrect
        return this.grid.toCXTString( filename );
    }


    /**
     * The main method of this class, which starts the process of building and storing paths.
     * Modifies the grid such that each cell's values contain the set of paths from
     * binaries to concepts.
     *
     * @param grid
     * @param binaries
     * @param targetConcepts
     */
    public void buildAllPaths() {
        for ( BinaryTuple binary : binaries ) {
            // start with this binary -- find all paths to all target concepts
            for ( Concept con : targetConcepts ) {
                buildPaths( binary, con );
            }
        }

        this.pruneCycles();

    }

    /**
     * Build all possible paths from the binary tuple to the concept.
     * Store the paths as FCA_RGPathValue objects in this object's repertory grid.
     * @param bt
     * @param con
     */
    public void buildPaths( BinaryTuple bt, Concept con ) {
        Path possiblePath = new Path();
        possiblePath.addToPath( bt );
        buildPaths( possiblePath, bt, con );
    }

    /**
     * Find a path from the concept-relation binary to the given concept.
     * Along the way, save any paths that are new in the intermediate cells of the grid.
     * @param possiblePath contains a path ending in the binary tuple that's the
     * argument
     * @param startingTuple The original tuple that started this path. It is used
     * for determining which column the path belongs in.
     * @param c2
     * @return null if no path, otherwise, the complete path.
     */
    public void buildPaths( Path possiblePath, BinaryTuple startingTuple, Concept con ) {
        // Find all concepts that are downstream of the binary
        GNode pathTail = possiblePath.lastNode();
        if (! ( pathTail instanceof Relation ) ) {
            Global.info( "buildPaths: Path " + possiblePath + " doesn't end with a relation ");
            return;
        }
//        ArrayList<GNode> linkedConcepts = startingTuple.relation.getLinkedNodes( GEdge.Direction.TO );
        ArrayList<GNode> linkedConcepts = ( (Relation)pathTail ).getLinkedNodes( GEdge.Direction.TO );

        if ( linkedConcepts.contains( con ) ) {
            Path possiblePathClone = possiblePath.clone();
            // we've found a one-hop path to the concept!
            possiblePathClone.addToPath( con );
            addToCurrentPaths( possiblePathClone, startingTuple, con ); // note that bt and con are used to locate grid cell
        }
        // Regardless of whether we found a path to save, keep looking for others, since
        // there may be multiple paths.
        for ( GNode linkedConcept : linkedConcepts ) {
            if ( possiblePath.contains( (Concept)linkedConcept ) ) {        // a cycle!
                Path possiblePathClone = possiblePath.clone();
                possiblePathClone.addToPath( (Concept)linkedConcept );
                addToCurrentPaths( possiblePathClone, startingTuple, con ); // note that bt and con are used to locate grid cell
            } else {    // try the next hop and continue
                ArrayList<BinaryTuple> binariesWithConcept = this.findConceptInBinaries( (Concept)linkedConcept );
                for ( BinaryTuple nextTuple : binariesWithConcept ) {
                    Path possiblePathClone = possiblePath.clone();
                    possiblePathClone.addToPath( nextTuple );
                    buildPaths( possiblePathClone, startingTuple, con );
                }
            }
        }
    }

    /**
     * Is the given concept an "output" concept as defined in the CGFCA world.
     *
     * @param c
     * @return true if it has no outgoing edges, false otherwise.
     */
    public static boolean isOutput( Concept c ) {
        ArrayList<GEdge> edges = c.getEdges();
        for ( GEdge edge : edges ) {
            if ( edge.fromObj == c ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Is the given concept an "input" concept as defined in the CGFCA world.
     *
     * @param c
     * @return true if it has no incoming edges, false otherwise.
     */
    public static boolean isInput( Concept c ) {
        ArrayList<GEdge> edges = c.getEdges();
        for ( GEdge edge : edges ) {
            if ( edge.toObj == c ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get the list of all binaries with the concept in them.
     * Note that it must be the first concept, because that's all a binary cares about.
     * @param con
     * @return a possibly zero-length list.
     */
    public ArrayList<BinaryTuple> findConceptInBinaries( Concept con ) {
        ArrayList<BinaryTuple> foundBinaries = new ArrayList<>();
        for ( BinaryTuple bt : binaries ) {
            if ( con == bt.concept1 ) {
                foundBinaries.add( bt );
            }
        }
        return foundBinaries;
    }

    /**
     * Test whether a path list already contains the given path.
     * @param path The path to test
     * @param paths A list of paths
     * @return true if either the path itself or the path reversed are in the list.
     * @see Path#equals(java.lang.Object)
     */
    public boolean isPathAlreadyInList( Path path, ArrayList<Path> paths ) {
        for ( Path p : paths ) {
            if ( p.equals( path ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look for cycles in every cell, and if one is found, remove its equivalent
     * cycle in any other cell in which it appears. For example, if a cycle
     * A-B-C-D-A exists, then the cycle B-C-D-A-B also exists. Arbitrarily keep
     * the first one found and remove the others.
     */
    public void pruneCycles() {
        for ( RGCell cell : grid.getCells() ) {
            ArrayList<Path> paths = ( (FCA_RGPathValue)cell.getRGValue() ).getPaths();
            for ( Path p : paths ) {
                if ( p.isCycle() ) {    // only bother with n**2 algorithm when there's a cycle.
                    for ( RGCell cellToSearch : grid.getCells() ) {
                        if ( cellToSearch != cell ) {
                            ArrayList<Path> pathsToSearch = ( (FCA_RGPathValue)cellToSearch.getRGValue() ).getPaths();
                            Iterator<Path> iterator = pathsToSearch.iterator();
                            while ( iterator.hasNext() ) {
                                Path pathToSearch = iterator.next();
                                if ( p.cycleSame( pathToSearch ) ) {
                                    iterator.remove();  // probably throw exception
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
