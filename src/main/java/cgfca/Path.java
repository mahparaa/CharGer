/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cgfca;

import java.util.ArrayList;

import charger.obj.Concept;
import charger.obj.GNode;
import kb.BinaryTuple;

/**
 * A path consists of a sequence of one or more Concepts and Relations.
 * If there is a cycle, then the  beginning concept is repeated at the end of the path.
 *
 * @author Harry S. Delugach (delugach@uah.edu)
 */
public class Path {

    ArrayList<GNode> path = new ArrayList<>();

    public boolean contains( Concept concept ) {
        return path.contains( concept);
    }


    /**
     * Add the concept to the end of the path.
     * @param c concept to be added.
     */
    public void addToPath( Concept c ) {
        path.add( c );
    }

    public void addToPath( BinaryTuple bt ) {
        path.add( bt.concept1 );
        path.add( bt.relation );
    }

    public boolean isEmpty() {
        return path.size() == 0;
    }

     public GNode firstNode( ) {
        if ( isEmpty())
            return null;
        return path.get( 0 );
    }

   public GNode lastNode( ) {
        if ( isEmpty())
            return null;
        return path.get( path.size() - 1);
    }

   public boolean isCycle() {
       return length() > 1 && firstNode() == lastNode();
   }

   public Path clone() {
       Path newPath = new Path();
       newPath.path = (ArrayList<GNode>)this.path.clone();
       return newPath;
   }

   /**
    * Show the path as a string of the form <code>"Cat - sits-on - Mat - has-attribute - Colour: Grey</code>
    * @return
    */
    public String toString() {
        String result = "";
        if ( isCycle() ) {
            result += "Cycle: ";
        } else {
            result += "Direct Pathway: ";
        }
        for ( GNode gn : path ) {
            result += gn.getTextLabel();
//            if ( lastNode() != gn && ! isCycle() )
                result += " - ";
        }
        return result;
    }

    /**
     * How many concepts and relations are in the path.
     * If it's a cycle, then the first and last nodes (which are identical) are both counted.
     * @return number of concepts and relations in the path
     */
    public int length() {
        return this.path.size();
    }

    public boolean isPathBetweenTerminals() {
        if ( isEmpty())
            return false;
        return PathFinder.isInput( (Concept)firstNode() ) && PathFinder.isOutput( (Concept)lastNode());
    }

    /** Compares two paths for equality as follows.
     * If either path is empty, then they're not equal.
     * If the two paths are the same length and contain the same objects (in the == sense),
     * then the paths are equal.
     * If one path is the exact <strong>reverse</strong> of the other path, then they are equal.
     * Otherwise false.
     * @param otherPath
     * @return
     */
    public boolean equals( Path otherPath ) {
        // If either is empty, call them not equal;
        if ( length() == 0 || otherPath.length() == 0 ) return false;
        // if different lengths, must be not different
        if ( this.length() != otherPath.length() )
            return false;

        // See if the same in order
        boolean same = true;
        for ( int nodenum = 0; nodenum < this.length(); nodenum++ ) {
            if ( this.path.get( nodenum ) != otherPath.path.get( nodenum ))
                same = false;
        }
        if ( same ) return true;
        // Check if reverse path is equal
        same = true;
        for ( int nodenum = 0; nodenum < this.length(); nodenum++ ) {
            if ( this.path.get( nodenum ) != otherPath.path.get( path.size() - 1 - nodenum ))
                same = false;
        }
        return same;
    }

    /**
     * Determine whether this path is equivalent to the given path if they are cycles.
     * If either is not a cycle, return false.
     * Otherwise walk the path to see if they're equivalent
     * @param otherPath
     * @return
     */
    public boolean cycleSame( Path otherPath ) {
        if ( this.length() != otherPath.length() ) {
            return false;
        }
        if ( !this.isCycle() || !otherPath.isCycle() ) {
            return false;
        }
        if ( !otherPath.contains( (Concept)this.firstNode() ) ) {
            return false;
        }
        if ( this.equals( otherPath ) ) {
            return true;
        }

        int nodeNum = 0;
        int otherNodeNum = 0;
        // walk the other path until we find the
        while ( otherPath.path.get( otherNodeNum ) != this.path.get( nodeNum ) ) {
            otherNodeNum++;
        }
        // Now otherNodeNum points to this path's start concept in the other path.
        // Let's walk the path
        int otherNodeNumStart = otherNodeNum;
//        otherNodeNum = ( otherNodeNum == otherPath.length() - 1 ) ? 0 : otherNodeNum + 1;
        while ( ++nodeNum < this.length() ) {
            otherNodeNum++;
            if ( otherNodeNum == otherPath.length() ) {
                otherNodeNum = 1; // because we've already checked the the 1st one matches
            }
            if ( otherPath.path.get( otherNodeNum ) != this.path.get( nodeNum ) ) {
                return false;
            }
        }

        return true;
    }

}
