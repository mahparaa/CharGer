//
//  Harry S. Delugach.java
//  CharGer 2003
//
//  Created by Harry Delugach on Sun May 18 2003.
//
package chargerlib;


import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/*
 CharGer - Conceptual Graph Editor
 Copyright reserved 1998-2017 by Harry S. Delugach
        
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
 * Manages multiple windows, where windows may be added and removed dynamically.
 * Its basic use is in creating and handling events for a "Windows" menu. Since
 * all methods are static, there is effectively only one WindowManager per
 * session.
 * Assumes that a managed window is of class Window or one of its sub-classes.
 *
 * @see ManagedWindow
 */
public class WindowManager {

    public static boolean enableTracing = false;
    public static final    int MAX_MENU_LABEL_LENGTH = 50;		// only this length string will be used in menu
    public static final String MENU_LABEL_PREFIX_IF_TOO_LONG = "...";

    /**
     * Keeps the window entries in the form of (MenuItemLabel, ManagedWindow)
     * pairs
     */
    private static Hashtable<JMenuItem, ManagedWindow> menuWindowLookup = new Hashtable<>();

    /** Each window has a particular string that appears in the menu label. */
    private static Hashtable< ManagedWindow, String> windowMenuLabelLookup = new Hashtable<>();
    /**
     * Keeps the window file entries in the form of ( file, ManagedWindow) pairs
     */
    private static Hashtable<ManagedWindow, String> fileWindowLookup = new Hashtable<>();
    /**
     * The global list of all managed windows, independent of any menus
     */
    private static ArrayList<ManagedWindow> windowList = new ArrayList<ManagedWindow>();
    /**
     * for each window, keeps whatever accelerator key has been stored for it
     */
    private static Hashtable acceleratorList = new Hashtable();
    private static boolean listToBeSorted = false;
    

    /**
     * Notify this window manager that a window wants to be managed.
     *
     * @param mw the window to be managed; must implement the ManagedWindow
     * interface.
     */
    public static void manageWindow( ManagedWindow mw ) {
        manageWindow( mw, null );
    }

    /**
     * Notify this window manager that a window wants to be managed.
     * Set up listeners so that the window menu is refreshed whenever the window is activated,
     * and that it is removed when the window is closed.
     *
     * @param mw the window to be managed; must implement the ManagedWindow
     * interface.
     * @param key the accelerator to be associated with this window's menu item.
     * If <code>null</code> then do not include an accelerator.
     */
    public static void manageWindow( ManagedWindow mw, KeyStroke key ) {
        if ( windowList.contains( mw ))
            return;
        if ( ! (mw instanceof Window) ) {
                Logger.getLogger(WindowManager.class.getName() ).log( Level.SEVERE, null, 
                        new Exception( "Can't manage instance of " + mw.getClass().getCanonicalName()) );
                return;
        }
        if ( enableTracing ) System.out.println( "manage window: window is " + mw.getClass().getName() );
        if ( listToBeSorted ) {
            insertNewWindowIntoList( mw );
        } else {
            windowList.add( mw );
        }
        if ( key != null ) {
            acceleratorList.put( mw, key );
        }
        changeFilename( mw, mw.getFilename() );
        if ( mw instanceof JFrame ) {
            ( (JFrame)mw ).addWindowListener( new WindowAdapter() {
                public void windowActivated( WindowEvent e ) {
                    makeMenu( mw );
                }

                public void windowClosing( WindowEvent e ) {
                    forgetWindow( mw );
                }
                
                public void windowClosed( WindowEvent e ) {
                    forgetWindow( mw );
                }
                
            } );
            
        }
    }

    /**
     * Notify this window manager that a window no longer needs to be managed.
     * If the window wasn't already in the list to be managed, then ignore this.
     *
     * @param mw the window to be removed; if not found, then do nothing.
     */
    public static void forgetWindow( ManagedWindow mw ) {
        if ( enableTracing ) System.out.println( "WindowManager: forgetWindow: window: " + mw.getMenuItemLabel() );
        windowList.remove( mw );
        acceleratorList.remove( mw );
        fileWindowLookup.remove( mw );
    }

    public static ManagedWindow getWindowFromMenuItem( JMenuItem mi ) {
        if ( enableTracing ) {
            System.out.println( "At get window from menu item, windows are: " );
            for ( ManagedWindow window : menuWindowLookup.values() ) {
                System.out.println( "  window " + window.getClass().getSimpleName() + " " + window.getMenuItemLabel() );
            }
        }
//        ManagedWindow result = (ManagedWindow)menuWindowLookup.get( mi );
//        if ( result != null ) {
//            return result;
//        }

        String label = mi.getText();
        for ( ManagedWindow win : windowList ) {
            if ( label.equals( shortenMenuLabel( win.getMenuItemLabel())) ) {
                return win;
            } 
        }
        return null;
    }

    /**
     * Brings to the front whatever window goes with the menu item specified.
     *
     * @param mi A menu item previously associated with a particular window by
     * makeMenu.
     * @see #makeMenu
     */
    public static ManagedWindow chooseWindowFromMenu( JMenuItem mi ) {
        ManagedWindow mw = getWindowFromMenuItem( mi );
        if ( mw != null ) {
            ((Window)mw).toFront();
            ((Window)mw).requestFocus();
        }
        return mw;
    }
    

    private static void insertNewWindowIntoList( ManagedWindow mw ) {
        String label = mw.getMenuItemLabel();
        //LibGlobal.info( "insert new window \"" + label + "\" into list" );
        if ( windowList.size() == 0 ) {
            windowList.add( mw );
        } else {
            int num = 0;
            while ( num < windowList.size()
                    && ( windowList.get( num ) ).getMenuItemLabel().compareToIgnoreCase( label ) < 0 ) {
                //LibGlobal.info( ((ManagedWindow)windowList.get( num )).getMenuItemLabel() +
                //		" is less than " + label );
                num++;
            }
            windowList.add( num, mw );
        }
        //LibGlobal.info( "after inserting " + mw.getMenuItemLabel() + " into list, the window list has " +
        //	windowList.toString() );
    }

//    /**
//     * Create menu entries for every managed window. Uses only the last
//     *
//     * @param menu The menu (usually a "Windows" menu) to which items are to be
//     * added.
//     * @param currentWindow put a check box next to this window's entry.
//     * @see ManagedWindow
//     */
//    public static synchronized void makeMenu( JMenu menu, ManagedWindow currentWindow ) {
//        int tailLength = 50;		// only this length string will be used in menu
//        //menu.removeAll();       // should dispose of each item, not just remove from menu
//        General.tearDownMenu( menu );
//        menuWindowLookup.clear();
//        Iterator wins = windowList.iterator();
//        while ( wins.hasNext() ) {
//            ManagedWindow mw = (ManagedWindow)wins.next();
//            String label = mw.getMenuItemLabel();
//            if ( label.length() > tailLength ) {
//                label = "..." + label.substring( label.length() - tailLength );
//            }
//            // TODO: Figure out where this should be deleted from the menu
//            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem( label );
//            //LibGlobal.info( "in refreshmenu, about to add menu item for " + mw.getMenuItemLabel() );
//            menu.add( menuItem );
//            if ( acceleratorList.get( mw ) != null ) {
//                menuItem.setAccelerator( (KeyStroke)acceleratorList.get( mw ) );
//            }
//
//            if ( mw == currentWindow ) {
//                menuItem.setState( true );
//            } else {
//                menuItem.setState( false );
//            }
//            //LibGlobal.info( "in refreshmenu, about to addactionlistener." );
//            menuWindowLookup.put( menuItem, mw );
//        }
//    }

    /**
     * Create menu entries for every managed window. Uses only the last
     *
     * @param menu The menu (usually a "Windows" menu) to which items are to be
     * added.
     * @param currentWindow put a check box next to this window's entry.
     * @see ManagedWindow
     */
    public static synchronized void makeMenu( ManagedWindow currentWindow ) {
        if ( enableTracing ) System.out.println( "WindowManager: makeMenu: window: " + currentWindow.getMenuItemLabel());
        //menu.removeAll();       // should dispose of each item, not just remove from menu
        JMenu menu = currentWindow.getWindowMenu();
        General.tearDownMenu( menu );
        menuWindowLookup.clear();
        Iterator wins = windowList.iterator();
        while ( wins.hasNext() ) {
            ManagedWindow mw = (ManagedWindow)wins.next();
            if ( mw instanceof Window && ! ((Window)mw).isVisible()) {
                continue;
            }
            String label = shortenMenuLabel( mw.getMenuItemLabel() );
            
            // TODO: Figure out where this should be deleted from the menu
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem( label );
            //LibGlobal.info( "in refreshmenu, about to add menu item for " + mw.getMenuItemLabel() );
            menu.add( menuItem );
            if ( acceleratorList.get( mw ) != null ) {
                menuItem.setAccelerator( (KeyStroke)acceleratorList.get( mw ) );
            }

            if ( mw == currentWindow ) {
                menuItem.setState( true );
            } else {
                menuItem.setState( false );
            }
            //LibGlobal.info( "in refreshmenu, about to addactionlistener." );
            menuWindowLookup.put( menuItem, mw );
            menuItem.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    windowActionPerformed( e );
                }
            } );
        }
    }

    public static String shortenMenuLabel( String label ) {
        String newLabel = label;
        if ( label.length() > MAX_MENU_LABEL_LENGTH ) {
            newLabel = MENU_LABEL_PREFIX_IF_TOO_LONG + label.substring( label.length() - MAX_MENU_LABEL_LENGTH );
        }
        return newLabel;
    }

    /**
     * Handles the window menu selections for the EditFrame, HubFrame, Craft and
     * CraftWindow classes.
     */
    public static synchronized void windowActionPerformed( ActionEvent e ) {
        // handle all menu events here
        Object source = e.getSource();
        String menuText = e.getActionCommand();
        chooseWindowFromMenu( (JMenuItem)source );
    }


    public static void setSorted( boolean sortme ) {
        listToBeSorted = sortme;
    }

    /** Tell the window manager that it needs to change the filename it's using for this window. 
     * 
     * @param mw The current window being managed
     * @param filename The new filename
     */
    public static void changeFilename( ManagedWindow mw, String filename ) {
        String dummy = (String)fileWindowLookup.remove( mw );
        if ( filename != null ) {
            fileWindowLookup.put( mw, filename );
        }
    }


    
    public static ManagedWindow getWindowFromFile( File f ) {
        return getWindowFromFile( f, null  );
    }

    /**
     * Find the window that corresponds to a particular file. This is the
     * reverse lookup from what the usual menu item does.
     *
     * @param f A file that is associated with some window.
     * @return The window that's associated with this file. If there is no such
     * window, return null.
     */
    public static ManagedWindow getWindowFromFile( File f, Class desiredClass ) {
        Iterator keys = fileWindowLookup.keySet().iterator();
        while ( keys.hasNext() ) {
            ManagedWindow mw = (ManagedWindow)keys.next();
            String filename = (String)fileWindowLookup.get( mw );
            if ( filename == null )
                return null;
            if ( desiredClass == null ) {
                if ( filename.equals( f.getAbsolutePath() ) )
                    return mw;
            } else  if ( ( mw.getClass() == desiredClass) && filename.equals( f.getAbsolutePath() ) ) {
                return mw;
            }
        }
        return null;
    }

    /**
     * Refreshes the windows menu lists for the given window. Uses whatever JMenu the window decides it wants filled.
     * This is usually a "Windows" menu in the frame itself, but it might be one from
     * a sub-class or associated class.
     * @param m
     * @param activeFrame
     * @see ManagedWindow#getWindowMenu() 
     */
    public static synchronized void refreshWindowMenuList( ManagedWindow activeFrame ) {
        WindowManager.makeMenu( activeFrame );
    }

    public static ArrayList<ManagedWindow> getManagedWindows() {
        return new ArrayList<>(windowList);
    }
}
