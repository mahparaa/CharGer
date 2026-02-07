/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cgfca;

import java.util.ArrayList;

import repgrid.RGBooleanValue;

/**
 * Captures the knowledge of one node on a path from an attribute binary tuple to a target concept element in a repertory grid.
 * This reflects the structure of the CG-FCA workflow.
 * @author Harry S. Delugach (delugach@uah.edu)
 */
public class FCA_RGPathValue extends RGBooleanValue {

    ArrayList<Path> paths = new ArrayList<>();
    boolean cycle = false;

    public FCA_RGPathValue() {
        super();
    }

    public boolean addPath( Path path ) {
        if ( isAlreadyThere( path ))
            return false;
        paths.add( path );
        return true;
    }

    public ArrayList<Path> getPaths() {
        return paths;
    }

    public boolean isAlreadyThere( Path path ) {
        for ( Path p : paths ) {
            if ( p.equals( path ))
                return true;
        }
        return false;
    }

    /**
     * Safely remove the path from this cell.
     */
    public void removePath( Path path) {
        if ( paths.contains( path )) {
            paths.remove( path );
        }
    }

}
