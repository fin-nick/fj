/*
 * Group.java
 *
 * Created on 14 Май 2008 г., 21:35
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import DrawControls.*;
import DrawControls.tree.*;
import DrawControls.icons.Icon;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.Options;
import jimm.cl.ContactList;
import jimm.chat.message.Message;
import jimm.forms.ManageContactListForm;
import jimm.search.*;
import jimm.ui.menu.*;
import jimm.ui.base.*;
import jimm.util.ResourceBundle;

/**
 *
 * @author vladimir
 */
public abstract class Group extends TreeBranch {
    
    protected Protocol protocol;
    /** Creates a new instance of Group */
    public Group(Protocol protocol, String name) {
        this.protocol = protocol;
        setName(name);
        caption = name;
        setMode(Group.MODE_FULL_ACCESS);
    }
    
    private String name;
    // Returns the group item name
    public final String getName() {
        return this.name;
    }
    
    // Sets the group item name
    public final void setName(String name) {
        this.name = name;
    }
    public final Protocol getProtocol() {
        return protocol;
    }
    
    public static final int NOT_IN_GROUP = -1;

    public static final byte MODE_NONE         = 0x00;
    public static final byte MODE_REMOVABLE    = 0x01;
    public static final byte MODE_EDITABLE     = 0x02;
    public static final byte MODE_NEW_CONTACTS = 0x04;
    public static final byte MODE_FULL_ACCESS  = 0x0F;

    public static final byte MODE_TOP          = 0x10;
    public static final byte MODE_BOTTOM       = 0x20;
    public static final byte MODE_BOTTOM2      = 0x40;

    private byte mode;
    public final void setMode(int newMode) {
        mode = (byte)newMode;
    }
    public final boolean hasMode(byte type) {
        return (mode & type) != 0;
    }
    
    public int getNodeWeight() {
        if (hasMode(MODE_TOP)) return -4;
        if (hasMode(MODE_BOTTOM)) return -2;
        if (hasMode(MODE_BOTTOM2)) return -1;
        //if (!hasMode(MODE_EDITABLE)) return -2;
        //if (!hasMode(MODE_REMOVABLE)) return -1;
        return -3;
    }
    
    private int groupId;
    public final int getId() {
        return groupId;
    }
    
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }
    
    private String caption = null;
    public final String getText() {
        return caption;
    }
    public final void getLeftIcons(Icon[] icons) {
    }
    
    public final void getRightIcons(Icon[] rightIcons) {
        if (!isExpanded() && getUnreadMessageCount() > 0) {
            rightIcons[0] = Message.msgIcons.iconAt(Message.ICON_MSG_NONE);
        }
    }
        
    public final int getUnreadMessageCount() {
        int count = 0;
        Vector items = getContacts();
        int size = items.size();
        for (int i = 0; i < size; ++i) {
            count += ((Contact)items.elementAt(i)).getUnreadMessageCount();
        }
        return count;
    }
    
    private final Vector contacts = new Vector();
    final void updateContacts() {
        Vector items = protocol.getContactItems();
        contacts.removeAllElements();
        int size = items.size();
        for (int i = 0; i < size; ++i) {
            Contact item = (Contact)items.elementAt(i);
            if (item.getGroupId() == groupId) {
                contacts.addElement(item);
            }
        }
    }
    final void addContact(Contact c) {
        contacts.addElement(c);
    }
    final boolean removeContact(Contact c) {
        return contacts.removeElement(c);
    }

    public final Vector getContacts() {
        return contacts;
    }
    
    // Calculates online/total values for group
    public final void updateGroupData() {
        int onlineCount = 0;
        int total = contacts.size();
        for (int i = 0; i < total; ++i) {
            Contact item = (Contact)contacts.elementAt(i);
            if (item.isOnline()) {
                onlineCount++;
            }
        }
        caption = getName();
        if (0 < total) {
            caption += " (" + onlineCount + "/" + total + ")";
        }
    }
    
    
    public final MenuModel getContextMenu() {
        if (protocol.isConnected()) {
            return new ManageContactListForm(protocol, this).getMenu();
        }
        return null;
    }

    public void dismiss() {
        contacts.removeAllElements();
        protocol = null;
    }
}
