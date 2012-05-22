/*******************************************************************************
 * Jimm - Mobile Messaging - J2ME ICQ clone
 * Copyright (C) 2003-05  Jimm Project
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
 * File: src/jimm/JimmException.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Manuel Linsmayer, Andreas Rossbacher, Vladimir Kryukov
 *******************************************************************************/


package jimm;


import jimm.cl.ContactList;
import jimm.modules.*;
import jimm.util.*;
import jimm.comm.*;
import javax.microedition.lcdui.*;

public class JimmException extends Exception {
    
    // Returns the error description for the given error code
    public static String getErrDesc(int errCode, int extErrCode) {
        return ResourceBundle.getString("error_" + errCode)
                + " (" + errCode + "." + extErrCode + ")";
    }
    
    
    /****************************************************************************/
    
    
    // True, if this is a critial exception
    protected boolean critical;
    
    
    private int errorCode;
    private int errorCodeExt;
    
    public int getErrCode() {
        return errorCode;
    }
    
    // Constructs a critical JimmException
    public JimmException(int errCode, int extErrCode) {
        super(JimmException.getErrDesc(errCode, extErrCode));
        this.errorCode = errCode;
        this.errorCodeExt = extErrCode;
        this.critical = true;
        //  #sijapp cond.if modules_FILES is "true"#
        this.peer = false;
        //  #sijapp cond.end#
    }
    
    
    // Constructs a non-critical JimmException
    public JimmException(int errCode, int extErrCode, boolean displayMsg) {
        this(errCode, extErrCode);
        this.critical = false;
    }
    
    // #sijapp cond.if modules_FILES is "true"#
    // Constructs a non-critical JimmException with peer info
    // True, if this is an exceptuion for an peer connection
    private boolean peer;
    public JimmException(int errCode, int extErrCode, boolean displayMsg, boolean _peer) {
        this(errCode, extErrCode);
        this.peer = _peer;
        this.critical = !_peer;
    }
    //  #sijapp cond.end#
    
    
    // Returns true if this is a critical exception
    public boolean isCritical() {
        return this.critical;
    }
    
    // #sijapp cond.if modules_FILES is "true"#
    // Returns true if this is a peer exception
    public boolean isPeer() {
        return this.peer;
    }
    //  #sijapp cond.end#
    
    public boolean isReconnectable() {
        // #sijapp cond.if modules_FILES is "true"#
        if (isPeer()) {
            return false;
        }
        //  #sijapp cond.end#
        return Options.getBoolean(Options.OPTION_RECONNECT) && isCritical()
                && (errorCode < 110 || errorCode > 117)
                && errorCode != 127 && errorCode != 140;
    }

    private String getTitle() {
        return ResourceBundle.getString(isCritical() ? "error" : "warning")
                    + " #" + errorCode + "." + errorCodeExt;
    }
    // Exception handler
    public static void handleException(JimmException e) {
        // #sijapp cond.if modules_LIGHT is "true" #
        CustomLight.setLightMode(CustomLight.ACTION_ERROR);
        // #sijapp cond.end#
        Jimm.unlockJimm();
        ContactList.activate();
        new jimm.ui.PopupWindow(e.getTitle(), e.getMessage()).show();
    }
}
