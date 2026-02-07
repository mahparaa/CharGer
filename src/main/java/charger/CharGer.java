package charger;

import static charger.Global.getLogFile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

import chargerlib.CDateTime;

/**
	@author Harry S. Delugach, University of Alabama in Huntsville
	Version History
	v0.3	1997-09-26
	v0.4    1997-09-29
	v0.5    1997-09-30
	v0.7	1997-12-27
	v0.8	1998-01-04
	v0.9	1998-01-06
	v1.0	1998-01-08
	v1.1 	1998-01-27
	v1.2	1998-01-29
	v1.3	1998-02-04
	v1.4	1998-03-31
	Re-incarnated as CharGer
	v1.5	1998-04-30
	v1.6	1998-05-10
	New interest from ICCS'99 conference
	v1.7	1999-07-21
	v1.8b	1999-07-27
	v1.9b	1999-07-30
	v2.0b	1999-08-17
	v2.1b	1999-09-01
	v2.2b	1999-11-15
	Beginning to incorporate conceptual graph operations
	v2.3b	2000-06-01
	v2.4b	2000-09-02
	v2.5b	2002-11-19
	v2.6b   2003-01-03
	v3.0b	2003-03-10
	v3.1b   2003-04-01
	Repertory grid interface begun
	v3.2b	2003-05-30
	WordNet features incorporated
	v3.3b2	2004-10-10
        Cuts introduced
        v3.4b1  2005-01-30
        v3.5b1  2005-11-30
        v3.5b2  2006-01-10
	v3.6	2008-08-06
        v3.7    2009-12-31
        *   Added in MMAT hooks but not yet operational
        v3.8    2012-08-12
        *   Made MMAT operational
        v3.8.2  2012-10-22
        *   Began using MMAT on experimental data, streamlined configuration
        v3.8.4  2013-07-14
        v3.8.5  2013-09-17
        v3.8.6  2013-10-22
        v3.8.7  2014-08-13
        *   Re-designed editing screen and preferences, adding auto layout
        v4.0.0  2014-09-01
        *   Incorporated more modern CGIF features
        v4.0.5  2014-11-15
        v4.1.1  2015-04-30
        v4.1.2  2015-11-04
        v4.2    2019-09-01
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
 * This is the main entry point to Charger when run as a standalone application.
*/

public class CharGer {

    public CharGer() {
    }

    /**
     * Arguments possible are: -p path -infoOn -module modulename -logFile (true|false)
     * See manual for descriptions.
     * Any other arguments are ignored.
     */
    public static void main(String[] args) {
        
        java.util.Locale.setDefault( Locale.ENGLISH );

//        if (Float.parseFloat(System.getProperty("java.specification.version")) < 1.7) {
//            JOptionPane.showMessageDialog(null, "Your version of Java is "
//                    + System.getProperty("java.specification.version") + "."
//                    + System.getProperty("line.separator") + "Version 1.7 or greater is required.",
//                    "Outdated Java version", JOptionPane.ERROR_MESSAGE);
//        }

        String graphFolder = System.getProperty( "user.home");
        boolean setInfoOn = true;
        System.out.println( "osName/version is " + System.getProperty( "os.name" ) + " / " + System.getProperty( "os.version" ) );

        System.out.println( "Java version is " + System.getProperty( "java.specification.version" ) + " (VM "
                + System.getProperty( "java.vm.version" ) + ")" );
        ArrayList<String> extraArgs = new ArrayList<>();
        for ( int argnum = 0; argnum < args.length; argnum++ ) {
            System.out.println( "Command line arg " + argnum + " is: \"" + args[argnum] + "\"." );
            if ( args[argnum].equalsIgnoreCase( "-p" ) ) {
                if ( ( argnum + 1 ) >= args.length || args[argnum + 1].startsWith( "-" ) ) {
                    System.out.println( "Command line ERROR: -p must be followed by path argument." );
                    System.exit( 1 );
                } else {
                    graphFolder = args[++argnum];
                }
            } else if ( args[argnum].equalsIgnoreCase( "-logFile" ) ) {
                if ( ( argnum + 1 ) >= args.length || args[argnum + 1].startsWith( "-" )
                        || !( args[argnum + 1].equalsIgnoreCase( "true" ) || args[argnum + 1].equalsIgnoreCase( "false" ) ) ) {
                    System.out.println( "Command line ERROR: -logFile must be followed by \"true\" or \"false\"." );
                    System.exit( 1 );
                } else {
                    String standardOutString = args[++argnum];
                    if ( standardOutString.equalsIgnoreCase( "false" ) ) {
                        Global.standardOutputToLog = false;
                    } else {
                        Global.standardOutputToLog = true;
                    }
                }
//            } else if ( args[argnum].equalsIgnoreCase( "-module" ) ) {
//                if ( ( argnum + 1 ) >= args.length || args[argnum + 1].startsWith( "-" ) ) {
//                    System.out.println( "Command line ERROR: -module must be followed by a plugin name." );
//                    System.exit( 1 );
//                } else {
//                    String modulePLuginName = args[++argnum];
//                    Global.modulePluginNamesToEnable.add( modulePLuginName );
//                }
            } else if ( args[argnum].equalsIgnoreCase( "-infoOn" ) ) {
                setInfoOn = true;
//            } else if ( args[argnum].equalsIgnoreCase( "-craft" ) ) {
//                Global.craftEnabled = true;
            } else {        // assume "unknown" arguments
                extraArgs.add( args[argnum] );
            }
        }

        charger.Global.setupAll(/* me,*/ graphFolder, extraArgs, setInfoOn );	// tell Charger who its controlling process is.
 
           if ( Global.standardOutputToLog ) {
            try {
                  Global.info( "StdOutput and StdError directed to " + getLogFile().getAbsolutePath() );
              Global.fileStdOutputPrintStream = new PrintStream( new FileOutputStream( getLogFile(), true ) );
                System.setOut( Global.fileStdOutputPrintStream );
                Global.fileStdErrorPrintStream = new PrintStream( new FileOutputStream( getLogFile(), true ) );
                System.setErr( Global.fileStdErrorPrintStream );
                Global.info( "\n============================================================");
                Global.info( new CDateTime().formatted( CDateTime.STYLE.ZONED_WEEKDAY_TIMESTAMP ) );

                Global.info( "StdOutput and StdError directed to " + getLogFile().getAbsolutePath() );
            } catch ( FileNotFoundException ex ) {
                Logger.getLogger(CharGer.class.getName() ).log( Level.SEVERE, null, ex );
            }
        } else {
                Global.info( "StdOutput and StdError directed to their defaults." );
           }

        
        System.setProperty( "file.encoding", "UTF-8");
        System.setProperty( "client.encoding.override", "UTF-8");
        
        
//        try {
//            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
//        } catch ( ClassNotFoundException ex ) {
//            Global.warning( "Setting universal look-and-feel: " + ex.getMessage());
//        } catch ( InstantiationException ex ) {
//            Global.warning( "Setting universal look-and-feel: " + ex.getMessage());
//        } catch ( IllegalAccessException ex ) {
//            Global.warning( "Setting universal look-and-feel: " + ex.getMessage());
//        } catch ( UnsupportedLookAndFeelException ex ) {
//            Global.warning( "Setting universal look-and-feel: " + ex.getMessage());
//        }

        CGSplashFrame splash = new CGSplashFrame();
        splash.setVisible( true );

        //System.out.println( "System Properties:" );		
        //System.getProperties().list(System.out);
        String prop;
        prop = "user.dir";
        Global.info( prop + " = " + System.getProperty( prop ) );	

        prop = "user.home";
        Global.info( prop + " = " + System.getProperty( prop ) );	

        prop = "java.class.path";
        //charger.Global.info( prop + " = " + System.getProperty( prop ) );	
        Global.info( "file.encoding = \"" + System.getProperty(  "file.encoding") + "\"");

//        CGMain me = new CGMain();
        

        splash.setVisible( false );
    }
}

/*

	DESIGN ISSUES CONCERNING ACTORS
		If a varible-arity actor is created, how should the firing proceed? If we follow the usual algorithm,
			each time any of the inputs changed, we'll completely fire the actor, without waiting on other inputs
			to possibly change. Perhaps it's not really an issue, since the same situation is present for fixed arity
			actors: if more than input is going to change, one of them will be detected first, and the firing will result.
			
	DESIGN ISSUES CONCERNING CONTEXTS
	It was all well and good to treat each graph as a collection of objects which can be enumerated,
	and thereby make sure all were handled. But for contexts, there is a whole raft of issues. The
	cleanest approach is to make a context merely a sub-graph, with object type "Graph" and that's it.
	
	+ enumerating all objects in a graph 
		might want to see only top level objects or might want to see all nested ones too.
		this is "solved" by implemented both a Shallow and a deep enumerate.
	+ selecting objects on a sheet
		a selection might span more than one context; in which case, need to scan with deep enumerate
		we adopt the rule that contexts cannot overlap, which essentially means that no matter how
		much is selected, there can be only one dominating context whose contents are only partially
		selected.
	+ creating a context
		draw a line around it, and use 'selection' rules to determine which concepts are to be 
			disconnected from their owner graphs and then nested in the context. 
		relations? any that are wholly nested (i.e., both ends in the context) are no problem;
			if not wholly nested, then attach to context.
			but some coreferent edges should remain as they were
	+ drawing a context
		draw border as a graph object, then draw contents
	+ All graphs have a context border except for the outermost graph, which has no border and 
		cannot be selected as a context, although all of its constituents can be selected individually.
	+ Should selecting a context automatically select its contents? For Copying, pasting this
		seems a reasonable approach. For re-sizing, etc. user would want more control.

	DESIGN ISSUES CONCERNING DATABASES
	
	For CharGer's connection to a database, the main issues are:
	1. How to validate the database reference in a graph, so that an actor can actually work?
	2. How to efficiently access the database once it is attached.
	The strategy to be adopted is as follows:
	The first time a database name is mentioned (e.g., in the Database Linking Tool) or used in a 
	concept type/referent combination (e.g., when an actor fires), the database is validated and
	"attached" meaning its Java wrapper is allocated and the file itself is open.
	
	Knowledge Bases:
		In the current design, there is a global knowledge base, storing type and relation hierarchies,
		but having no graphs of its own. Graphs will be stored somewhere else, usually an edit frame.
	Type Definitions:
		The idiom for type definitions is as follows:
			Any context of type "Definition" forms a differentia graph. The parameter concept(s) 
				of the differentia graph are part of a coreferent set that must have at least one
				member in the Definition graph's enclosing context. This means that the definition
				cannot enclose members of any other coreferent set.
			OR
			The parameter concept type is the name designator of the Definition's referent. 
			This means that all concepts of that type would have to be coreferent, disallowing
				definitions of person such that person has mother person, unless other constraints
				were provided to artificially exclude it. 

 */
 

