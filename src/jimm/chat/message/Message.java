/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

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
 File: src/jimm/comm/Message.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/


package jimm.chat.message;

import DrawControls.text.ListItem;
import DrawControls.icons.*;
import protocol.Contact;
import protocol.Protocol;

public abstract class Message {
    public static final ImageList msgIcons = ImageList.createImageList("/msgs.png");
    public static final int ICON_SYSREQ = 0;
    public static final int ICON_SYS_OK = 1;
    public static final int ICON_TYPE = 2;
    public static final int ICON_MSG_NONE = 3;
    public static final int ICON_MSG_FROM_SERVER = 4;
    public static final int ICON_MSG_FROM_CLIENT = 5;
    //public static final int ICON_ERROR = 6;

    public static final int NOTIFY_OFF = -1;
    public static final int NOTIFY_NONE = ICON_MSG_NONE;
    public static final int NOTIFY_FROM_SERVER = ICON_MSG_FROM_SERVER;
    public static final int NOTIFY_FROM_CLIENT = ICON_MSG_FROM_CLIENT;


    protected boolean isIncoming;
    protected String contactUin;
    protected Contact contact;
    protected Protocol protocol;
    private String senderName;
    protected ListItem visibleIcon = null;
    // Date of dispatch
    private long newDate;
    
    protected Message(long date, Protocol protocol, String contactUin, boolean isIncoming) {
    	newDate          = date;
    	this.protocol = protocol;
    	this.contactUin = contactUin;
        this.isIncoming = isIncoming;
    }
    protected Message(long date, Protocol protocol, Contact contact, boolean isIncoming) {
    	newDate          = date;
    	this.protocol = protocol;
    	this.contact = contact;
        this.isIncoming = isIncoming;
    }
    
    public void setVisibleIcon(ListItem listItem) {
        this.visibleIcon = listItem;
    }
    public void setSendingState(int state) {
        if (null != visibleIcon) {
            visibleIcon.image = msgIcons.iconAt(state);
            Contact contact = getRcvr();
            if (contact.hasChat()) {
                contact.getChat().invalidate();
            }
        }
    }
    public void setName(String name) {
        senderName = name;
    }
    private String getContactUin() {
        return (null == contact) ? contactUin : contact.getUin();
    }
    // Returns the senders UIN
    public String getSndrUin() {
        return isIncoming ? getContactUin() : protocol.getUserId();
    }

    // Returns the receivers UIN
    public String getRcvrUin() {
        return isIncoming ? protocol.getUserId() : getContactUin();
    }
    public boolean isIncoming() {
        return isIncoming;
    }

    // Returns the receiver
    public Contact getRcvr() {
        return (null == contact) ? protocol.getItemByUIN(contactUin) : contact;
    }

    public boolean isOffline() {
    	return false;
    }
    
    public long getNewDate() {
    	return newDate;
    }
    
    public String getName() {
        if (null == senderName) {
            Contact c = getRcvr();
            if (isIncoming) {
                senderName = (null == c) ? getContactUin() : c.getName();
            } else {
                senderName = (null == c) ? protocol.getNick() : c.getMyName();
            }
        }
        return senderName;
    }
    
    public abstract String getText();
    public String getProcessedText() {
        return getText();
    }
    public boolean isWakeUp() {
        return false;
    }
    
    public Icon getIcon() {
        return null;
    }
}