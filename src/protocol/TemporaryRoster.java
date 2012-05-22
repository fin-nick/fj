/*
 * TemporaryRoster.java
 *
 * Created on 1 Август 2010 г., 17:38
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol;

import java.util.Vector;
import jimm.Options;
import jimm.comm.Util;
import protocol.jabber.*;
import protocol.mrim.*;

/**
 *
 * @author Vladimir Kryukov
 */
public class TemporaryRoster {
    
    /** Creates a new instance of TemporaryRoster */
    public TemporaryRoster(Protocol protocol) {
        this.protocol = protocol;
        this.oldGroups = protocol.getGroupItems();
        this.oldContacts = protocol.getContactItems();
    }
    private Protocol protocol;
    private Vector oldGroups;
    private Vector oldContacts;
    private Vector groups = new Vector();
    private Vector contacts = new Vector();
    public Contact makeContact(String userId) {
        Contact c;
        for (int i = oldContacts.size() - 1; 0 <= i; --i) {
            c = (Contact)oldContacts.elementAt(i);
            if (userId.equals(c.getUin())) {
                return c;
            }
        }
        return protocol.createContact(userId, userId);
    }
    private Group getGroup(Vector list, String name) {
        for (int j = list.size() - 1; 0 <= j; --j) {
            Group g = (Group)list.elementAt(j);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }
    public Group makeGroup(String name) {
        if (null == name) {
            return null;
        }
        Group g = getGroup(oldGroups, name);
        return (null == g) ? protocol.createGroup(name) : g;
    }
    public Group getGroup(String name) {
        return (null == name) ? null : getGroup(groups, name);
    }
    public Group getOrCreateGroup(String name) {
        if (null == name) {
            return null;
        }
        Group g = getGroup(name);
        if (null == g) {
            g = makeGroup(name);
            addGroup(g);
        }
        return g;
    }
    
    public final Vector mergeContacts() {
        Vector newContacts = contacts;
        if (Options.getBoolean(Options.OPTION_SAVE_TEMP_CONTACT)) {
            Contact o;
            for (int i = oldContacts.size() - 1; 0 <= i; --i) {
                o = (Contact)oldContacts.elementAt(i);
                if (-1 != Util.getIndex(newContacts, o)) {
                    continue;
                }
                o.setTempFlag(true);
                o.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                o.setGroup(null);

                // #sijapp cond.if protocols_MRIM is "true" #
                if (o instanceof MrimPhoneContact) {
                    o.dismiss();
                    continue;
                }
                // #sijapp cond.end #
                // #sijapp cond.if protocols_JABBER is "true" #
                if (o instanceof JabberServiceContact) {
                    if (o.isSingleUserContact()) {
                        continue;
                    }
                    o.setGroup(getOrCreateGroup(o.getDefaultGroupName()));
                }
                // #sijapp cond.end #
                newContacts.addElement(o);
            }
        }
        return newContacts;
    }
    
    public void addGroup(Group g) {
        groups.addElement(g);
    }
    public void addContact(Contact c) {
        c.setTempFlag(false);
        contacts.addElement(c);
    }
    public Vector getContacts() {
        return contacts;
    }
    public Vector getGroups() {
        return groups;
    }

    public Group getGroupById(int groupId) {
        Group group;
        for (int i = oldGroups.size() - 1; 0 <= i; --i) {
            group = (Group)oldGroups.elementAt(i);
            if (group.getId() == groupId) {
                return group;
            }
        }
        return null;
    }
}
