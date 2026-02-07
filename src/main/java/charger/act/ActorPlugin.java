package charger.act;

import java.util.ArrayList;
//import charger.*;
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

import charger.act.GraphUpdater;
import charger.exception.CGActorException;
import charger.obj.Concept;

/**
 * Provides a mechanism for creating external actors that can be incorporated
 * into CharGer. Implementers of this interface are allowed to write actor
 * classes that "plug in" to CharGer and whose names will show up in the actor
 * list just as the primitive actors do. Classes implementing the ActorPlugin
 * interface must be in the package named "actorplugin". Responsibility for
 * extracting referents from the charger.obj.Concept arguments lies with the
 * class implementing the interface; some convenience methods are found in
 * charger.act.GraphUpdater.
 *
 * @see charger.act.GraphUpdater
 * @author Harry S. Delugach ( delugach@uah.edu ) Copyright 1998-2020 by Harry
 * S. Delugach
 */
public interface ActorPlugin {

    /**
     * @return name by which the actor will be known throughout the system; this
     * is the string that will be used as the label on an actor in CharGer.
     */
    String getPluginActorName();

    /**
     * Assumption about input and output lists is that inputs are numbered 1..n
     * and outputs are numbered n+1 .. m.
     *
     * @return List of input concepts (or graphs), each with a constraining type
     * label (or "T" )
     */
    ArrayList<Concept> getPluginActorInputConceptList();

    /**
     * Assumption about input and output lists is that inputs are numbered 1..n
     * and outputs are numbered n+1 .. m.
     *
     * @return List of output concepts (or graphs), each with a constraining
     * type label (or "T" )
     */
    ArrayList<Concept> getPluginActorOutputConceptList();

    /**
     * @return ArrayList of objects, usually in string form, indicating other
     * actor constraints.
     * @see charger.act.GraphUpdater#GraphObjectAttributes
     */
    ArrayList getPluginActorAttributes();

    /**
     * Perform the actor's operation. Called by GraphUpdater when input or
     * output concepts change.
     *
     * @param gu the current graph updater, in case the operation requires
     * further updating
     * @param inputs ArrayList of charger.obj.Concept
     * @param outputs ArrayList of charger.obj.Concept, set by the operation and
     * usable by the caller.
     */
    void performActorOperation( GraphUpdater gu, ArrayList<Concept> inputs, ArrayList<Concept> outputs )
            throws CGActorException;

    /**
     * Perform any clean-up required by the actor when it is deleted or its
     * graph is de-activated.
     */
    void stopActor();

    /**
     * @return a string identifying the author, version, and/or email address of
     * this plugin
     */
    String getSourceInfo();
}
