/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-06  Jimm Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 * File: src/jimm/modules/traffic/t.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author: Andreas Rossbacher
 *******************************************************************************/

// #sijapp cond.if modules_TRAFFIC is "true" #

package jimm.modules.traffic;

import javax.microedition.lcdui.Font;
import jimm.*;
import jimm.cl.ContactList;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.ui.base.*;
import jimm.util.ResourceBundle;
import jimm.comm.*;


// Screen for Traffic information
public class TrafficScreen extends TextListEx implements SelectListener {
    
    // Number of kB defines the threshold when the screen should be update
    private int updateThreshold;
    // Traffic value to compare to in kB
    private int compareTraffic;
    
    private Traffic t;
    // Constructor
    public TrafficScreen() {
        super(ResourceBundle.getString("traffic_lng"));
        updateThreshold = 1;
        t = Traffic.getInstance();
        compareTraffic = t.getSessionTraffic();
        
        MenuModel menu = new MenuModel();
        menu.addItem("reset", MENU_RESET);
        menu.addItem("back",  MENU_BACK);
        menu.setActionListener(this);
        setMenu(menu, MENU_BACK);
        update();
    }
    private static final int MENU_RESET = 1;
    private static final int MENU_BACK  = 2;
    
    protected void restoring() {
        if ((t.getSessionTraffic() - compareTraffic) >= updateThreshold) {
            compareTraffic = t.getSessionTraffic();
            update();
        }
        NativeCanvas.setCommands("menu", "back");
    }
    
    private void addTrafficInfo(String title, int value) {
        addBigText(ResourceBundle.getString(title) + ": ", THEME_TEXT, Font.STYLE_BOLD, -1);
        addBigText(StringConvertor.bytesToSizeString(value, false), THEME_TEXT, Font.STYLE_PLAIN, -1);
        addBigText("\n", THEME_TEXT, Font.STYLE_BOLD, -1);
    }
    private void update() {
        int sessionIn  = t.getSessionInTraffic();
        int sessionOut = t.getSessionOutTraffic();
        int session    = sessionIn + sessionOut;
        int totalIn    = t.getAllInTraffic();
        int totalOut   = t.getAllOutTraffic();
        int total      = totalIn + totalOut;
        //int sessionInCost  = t.generateCostSum(sessionIn,          0, false);
        //int sessionOutCost = t.generateCostSum(        0, sessionOut, false);
        int sessionCost    = t.generateCostSum(sessionIn, sessionOut, false);
        //int totalInCost    = t.generateCostSum(  totalIn,          0, false);
        //int totalOutCost   = t.generateCostSum(        0,   totalOut, false);
        int totalCost      = t.generateCostSum(  totalIn,   totalOut, true);
        
        lock();
        clear();
        String trafficDelimiter = ResourceBundle.getString("traffic_delimiter");
        // Traffic for a session
        addBigText(ResourceBundle.getString("session") + "\n", THEME_TEXT, Font.STYLE_BOLD, -1);
        addTrafficInfo("in_traffic", sessionIn);
        addTrafficInfo("out_traffic", sessionOut);
        addTrafficInfo("total_traffic", session);
        
        // The cost of the traffic
        if (0 < sessionCost) {
            addBigText(t.costToString(sessionCost) + "\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        }
        addBigText("\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        
        // Traffic since date
        addBigText(ResourceBundle.getString("traffic_since")+" ", THEME_TEXT, Font.STYLE_BOLD, -1);
        addBigText(t.getTrafficString()+"\n", THEME_TEXT, Font.STYLE_BOLD, -1);
        addTrafficInfo("in_traffic", totalIn);
        addTrafficInfo("out_traffic", totalOut);
        addTrafficInfo("total_traffic", total);

        // The cost of the traffic
        if (0 < totalCost) {
            addBigText(t.costToString(totalCost) + "\n", THEME_TEXT, Font.STYLE_PLAIN, -1);
        }
        
        unlock();
    }
    
    // Command listener
    public void select(Select select, MenuModel model, int cmd) {
        switch (cmd) {
            case MENU_RESET:
                t.reset();
                update();
                restore();
                break;
            case MENU_BACK:
                back();
                break;
        }
    }
}
// #sijapp cond.end#
