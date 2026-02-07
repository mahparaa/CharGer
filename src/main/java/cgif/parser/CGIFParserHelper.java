/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cgif.parser;

import cgif.parser.javacc.Token;
import charger.Global;
import charger.cgx.CGXParser;
import charger.exception.CGEncodingException;
import charger.obj.Actor;
import charger.obj.Arrow;
import charger.obj.Concept;
import charger.obj.DeepIterator;
import charger.obj.GenSpecLink;
import charger.obj.Graph;
import charger.obj.GraphObject;
import charger.obj.Referent;
import charger.obj.Relation;
import charger.obj.TypeLabel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;

/**
 * Auxiliary routines for the CGIF Parser.
 * Fixed to ensure variable replacement and prevent layout parsing crashes.
 * @author Harry S. Delugach (delugach@uah.edu)
 * Modified for variable-to-label replacement.
 */
public class CGIFParserHelper {

    public ReferentMap referents = new ReferentMap();

    public static String extractChargerComment( Token token ) {
        if ( token == null || token.specialToken == null ) {
            return null;
        }
        String comment = token.specialToken.toString();
        String chargerComment = null;
        if ( comment.contains( Global.CharGerCGIFCommentStart ) ) {
            int start = comment.indexOf( Global.CharGerCGIFCommentStart ) + Global.CharGerCGIFCommentStart.length();
            chargerComment = comment.substring( start, comment.length() - 2 );
        }
        return chargerComment;
    }

    public static String extractChargerComment( Token t1, Token t2 ) {
        String chargerComment = extractChargerComment( t1 );
        if ( chargerComment  != null )
            return chargerComment;
        else {
            return extractChargerComment( t2 );
        }
    }

    public void parseChargerLayout( String chargerComment, GraphObject go ) {
        if ( chargerComment == null || chargerComment.trim().isEmpty() ) return;

        CGXParser parser = null;
        try {
            parser = new CGXParser( chargerComment );
            parser.parseLayoutOnly( go );
            go.resizeIfNecessary();
        } catch (Exception | CGEncodingException ex ) {
            // Silently fail if layout is invalid to prevent crashing the whole parse
            Logger.getLogger( CGIFParserHelper.class.getName() ).log( Level.FINE, "Invalid layout comment skipped." );
        }
    }

    /**
     * Corrected to replace *x handles with Type labels in the visual graph.
     */
    public Concept makeConcept( Graph g, String type, Referent referent, String layout ) {
        Concept concept = new Concept();

        // FIX: Use the Type (e.g. "SAP Graph") as the primary label
        concept.setTypeLabel( unquotify( type ) );

        // FIX: Only set referent if it's not just a variable handle
        String refString = referent.getReferentString();
        if (refString != null && !refString.startsWith("*")) {
            concept.setReferent( unquotify( refString ), false );
        }

        // REGISTER the variable handle (e.g., *x8) so relations can find this node
        String varHandle = referent.getVariable();
        if ( varHandle != null ) {
            try {
                referents.putObjectByReferent( varHandle, concept);
            } catch ( CGIFVariableException ex ) {
                Logger.getLogger(CGIFParserHelper.class.getName() ).log( Level.SEVERE, null, ex );
            }
        }

        g.insertObject( concept );
        if ( layout != null ) parseChargerLayout( layout, concept );
        return concept;
    }

    public void makeTypeLabel( Graph g,  String type, String layout )  throws CGIFSubtypeException {
        TypeLabel typelabel = new TypeLabel();
        typelabel.setTypeLabel( type );
        Iterator<GraphObject> iter = new DeepIterator( g, GraphObject.Kind.GNODE );

        boolean found = false;
        while ( iter.hasNext() ) {
            GraphObject obj = iter.next();
            if ( obj instanceof TypeLabel ) {
                if ( obj.getTextLabel().equalsIgnoreCase( type ) ) {
                    found = true;
                }
            }
        }
        if ( found ) throw new CGIFSubtypeException( "Type label " + type + " is already declared.");

        if ( layout != null ) parseChargerLayout( layout, typelabel );
        g.insertObject( typelabel );
    }

    public void makeGenSpecLink( Graph g, String subtype, String supertype, String layout ) throws CGIFSubtypeException {
        GraphObject subobj = null;
        GraphObject superobj = null;
        Iterator<GraphObject> iter = new DeepIterator(g, GraphObject.Kind.GNODE);

        while (iter.hasNext()) {
            GraphObject obj = iter.next();
            if (obj instanceof TypeLabel) {
                if ( obj.getTextLabel().equalsIgnoreCase(subtype) ) subobj = obj;
                if ( obj.getTextLabel().equalsIgnoreCase(supertype) ) superobj = obj;
            }
        }
        if ( subobj == null ) throw new CGIFSubtypeException( "subtype \"" + subtype + "\" not found.");
        if ( superobj == null ) throw new CGIFSubtypeException( "supertype \"" + supertype + "\" not found.");

        GenSpecLink link = new GenSpecLink( subobj, superobj );
        g.insertObject( link );
    }

    public Relation makeRelation( Graph g, String name, ArrayList<String> variables, String layout ) throws CGIFVariableException {
        Relation relation = new Relation();
        relation.setTextLabel( name );

        if ( layout != null ) parseChargerLayout( layout, relation );
        g.insertObject( relation );

        int varnum = 0;
        for ( String var : variables ) {
            String lookupVar = var;
            varnum++;

            // Standardize ?x to *x for lookup
            if ( var.startsWith( "?")) {
                lookupVar = var.replaceFirst( Pattern.quote("?"), "*");
            }

            GraphObject go = referents.getObjectByReferent( lookupVar );
            if ( go == null )
                throw new CGIFVariableException("Variable \"" + var + "\" not found.");
            else {
                Arrow arc;
                if ( varnum == variables.size() ) {
                    arc = new Arrow( relation, go );
                } else {
                    arc = new Arrow( go, relation );
                }
                g.insertObject( arc);
            }
        }
        return relation;
    }

    public Actor makeActor( Graph g, String name, ArrayList<String> inputvariables, ArrayList<String> outputvariables, String layout ) throws CGIFVariableException {
        Actor actor = new Actor();
        actor.setTextLabel( name );

        if ( layout != null ) parseChargerLayout( layout, actor );
        g.insertObject( actor );

        for ( String var : inputvariables ) {
            String lookupVar = var.startsWith("?") ? var.replaceFirst(Pattern.quote("?"), "*") : var;
            GraphObject go = referents.getObjectByReferent( lookupVar );
            if ( go == null ) throw new CGIFVariableException( "Variable \"" + var + "\" not found." );
            g.insertObject( new Arrow( go, actor ) );
        }

        for ( String var : outputvariables ) {
            String lookupVar = var.startsWith("?") ? var.replaceFirst(Pattern.quote("?"), "*") : var;
            GraphObject go = referents.getObjectByReferent( lookupVar );
            if ( go == null ) throw new CGIFVariableException( "Variable \"" + var + "\" not found." );
            g.insertObject( new Arrow( actor, go ) );
        }
        return actor;
    }

    String unquotify( String possiblyQuotedString ) {
        if ( possiblyQuotedString == null ) return "";
        if ( ! possiblyQuotedString.startsWith( "\""))
            return possiblyQuotedString;

        String s = possiblyQuotedString.substring(1, possiblyQuotedString.length() - 1);
        s = s.replaceAll( Pattern.quote("\\\""), "\"");
        return s;
    }
}