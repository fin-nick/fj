/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-04  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/jimm/comm/Systemjava
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher
 *******************************************************************************/

package jimm.chat.message;

import DrawControls.icons.Icon;
import jimm.Options;
import jimm.comm.*;
import jimm.util.ResourceBundle;
import protocol.Protocol;

public class SystemNotice extends Message {

	// Types of system messages
	public static final int SYS_NOTICE_YOUWEREADDED = 1;
	public static final int SYS_NOTICE_AUTHDENY = 2;
	public static final int SYS_NOTICE_AUTHREQ = 3;
	public static final int SYS_NOTICE_AUTHORISE = 4;
	public static final int SYS_NOTICE_REQUAUTH = 5;
	public static final int SYS_NOTICE_GRAND    = 6;
	public static final int SYS_NOTICE_GRANDED  = 7;

	public static final int SYS_NOTICE_ERROR = 8;
	public static final int SYS_NOTICE_MESSAGE  = 9;
        // #sijapp cond.if modules_PRESENCE is "true" #    //presence
        public static final int SYS_NOTICE_PRESENCE  = 10; //presence
        // #sijapp cond.end#                               //presence

	/****************************************************************************/

	// Type of the note
	private int sysnotetype;

	// Was the Authorisation granted
	private boolean AUTH_granted;

	// What was the reason
	private String reason;

	// Constructs system notice
	public SystemNotice(Protocol protocol, int _sysnotetype, String _uin, String _reason) {
		super(Util.createCurrentDate(false), protocol, _uin, true);
		sysnotetype = _sysnotetype;
        reason = StringConvertor.isEmpty(_reason) ? "" : _reason;
	}
	public SystemNotice(Protocol protocol, int _sysnotetype, String _uin, String _reason, boolean granded) {
		super(Util.createCurrentDate(false), protocol, _uin, true);
		sysnotetype = _sysnotetype;
        reason = StringConvertor.isEmpty(_reason) ? "" : _reason;
        AUTH_granted = granded;
	}

    // Get AUTH_granted
	public boolean isAUTH_granted() {
		return AUTH_granted;
	}
    public String getName() {
        // #sijapp cond.if modules_PRESENCE is "true" # //presence
        if (SYS_NOTICE_PRESENCE == getSysnoteType()) {  //presence
        return ResourceBundle.getString("**");          //presence
        } else {                                        //presence
        // #sijapp cond.end#                            //presence
        return ResourceBundle.getString("sysnotice");
        // #sijapp cond.if modules_PRESENCE is "true" # //presence
        }                                               //presence
        // #sijapp cond.end#                            //presence
    }

	// Get Reason
	public String getReason() {
		return reason;
	}

	// Get Sysnotetype
	public int getSysnoteType() {
		return sysnotetype;
	}
    
    public String getText() {
        String text = "";
        if (getSysnoteType() == SYS_NOTICE_MESSAGE) {
            return "* " + getReason();
            
        } else if (getSysnoteType() == SYS_NOTICE_YOUWEREADDED) {
            text = ResourceBundle.getString("youwereadded") + getSndrUin();
            
        } else if (getSysnoteType() == SYS_NOTICE_AUTHREQ) {
            text = getSndrUin() + ResourceBundle.getString("wantsyourauth");
            
        } else if (getSysnoteType() == SYS_NOTICE_GRANDED) {
            text = ResourceBundle.getString("grantedby") + getSndrUin();
            
        } else if (getSysnoteType() == SYS_NOTICE_AUTHDENY) {
            text = ResourceBundle.getString("denyedby") + getSndrUin();
        }

        String reason = getReason();
        if (StringConvertor.isEmpty(text)) {
            return reason;
        }
        text += ".";
        if (!StringConvertor.isEmpty(reason)) {
            text += "\n" + ResourceBundle.getString("reason") + ": " + reason;
        }
        return text;
    }
    public Icon getIcon() {
        if (SYS_NOTICE_MESSAGE == getSysnoteType()) {
            return null;
        }

        // #sijapp cond.if modules_PRESENCE is "true" # //presence
        if (SYS_NOTICE_PRESENCE == getSysnoteType()) {  //presence
            return null;                                //presence
        }                                               //presence
        // #sijapp cond.end#                            //presence
        
        return msgIcons.iconAt((SYS_NOTICE_GRANDED == getSysnoteType())
                ? ICON_SYS_OK : ICON_SYSREQ);
    }
}
