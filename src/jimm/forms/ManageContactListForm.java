/*
 * ManageContactListForm.java
 *
 * Created on 10 Июнь 2007 г., 21:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.forms;

import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.cl.ContactList;
import jimm.comm.*;
import protocol.Contact;
import jimm.search.*;
import jimm.ui.*;
import jimm.ui.menu.*;
import jimm.util.*;
import protocol.*;

/**
 *
 * @author vladimir
 */
public class ManageContactListForm implements CommandListener, SelectListener {
    private static final int ADD_USER     = 1;
    private static final int SEARCH_USER  = 2;
    private static final int ADD_GROUP    = 3;
    private static final int RENAME_GROUP = 4;
    private static final int DEL_GROUP    = 5;

    private Protocol protocol;

    private Group group;
    private MenuModel manageCL;
    private InputTextBox groupName;

    private Contact contact;
    private InputTextBox renameContactTextbox;
    private MenuModel groupList;
    private int action;

    /** Creates a new instance of ManageContactListForm */
    public ManageContactListForm(Protocol protocol) {
        this(protocol, (Group)null);
    }
    public ManageContactListForm(Protocol protocol, Group group) {
        manageCL = new MenuModel();
        if ((null == group) || group.hasMode(Group.MODE_NEW_CONTACTS)) {
            manageCL.addEllipsisItem("add_user",     null, ADD_USER);
            // #sijapp cond.if protocols_JABBER is "true" #
            if (!(protocol instanceof protocol.jabber.Jabber)) {
                manageCL.addEllipsisItem("search_user",  null, SEARCH_USER);
            }
            // #sijapp cond.else #
            manageCL.addEllipsisItem("search_user",  null, SEARCH_USER);
            // #sijapp cond.end #
        }
        manageCL.addEllipsisItem("add_group",    null, ADD_GROUP);
        if (null != group) {
            if (group.hasMode(Group.MODE_EDITABLE)) {
                manageCL.addEllipsisItem("rename_group", null, RENAME_GROUP);
            }
            if ((group.getContacts().size() == 0) && group.hasMode(Group.MODE_REMOVABLE)) {
                manageCL.addEllipsisItem("del_group",    null, DEL_GROUP);
            }
        }
        manageCL.setActionListener(this);
        this.protocol = protocol;
        this.group = group;
    }
    public ManageContactListForm(Protocol protocol, Contact contact) {
        this.protocol = protocol;
        this.contact = contact;
    }
    public void showContactRename() {
        renameContactTextbox = new InputTextBox("rename", 64);
        renameContactTextbox.setString(contact.getName());
        renameContactTextbox.setCommandListener(this);
        renameContactTextbox.show();
    }
    public void showContactMove() {
        /* Show list of groups to select which group to addEllipsisItem to */
        Vector groups = protocol.getGroupItems();
        Group myGroup = contact.getGroup();
        groupList = new MenuModel();
        for (int i = 0; i < groups.size(); i++) {
            Group g = (Group)groups.elementAt(i);
            if ((myGroup != g) && g.hasMode(Group.MODE_NEW_CONTACTS)) {
                groupList.addRawItem(g.getName(), null, g.getId());
            }
        }
        groupList.setActionListener(this);
        new Select(groupList).show();
    }

    public MenuModel getMenu() {
        return manageCL;
    }
    
    public void select(Select select, MenuModel model, int cmd) {
        if (groupList == model) {
            groupList = null;
            protocol.moveContactTo(contact, protocol.getGroupById(cmd));
            protocol.getContactList().activate();
            return;
        }
        action = cmd;
        switch (cmd) {
            case ADD_USER: /* Add user */
                Search search = new Search(protocol);
                search.putToGroup(group);
                search.show(Search.TYPE_LITE);
                break;
                
            case SEARCH_USER: /* Search for User */
                Search searchUser = new Search(protocol);
                searchUser.putToGroup(group);
                searchUser.show(Search.TYPE_FULL);
                break;
                
            case ADD_GROUP: /* Add group */
                showTextBox("add_group", null);
                break;
                
            case RENAME_GROUP: /* Rename group */
                showTextBox("rename_group", group.getName());
                groupName.show();
                break;
                
            case DEL_GROUP: /* Delete group */
                protocol.removeGroup(group);
                protocol.getContactList().activate();
                break;
        }
    }

    /* Show form for adding user */
    private void showTextBox(String caption, String text) {
        groupName = new InputTextBox("group_name", 16);
        groupName.setCaption(ResourceBundle.getString(caption));
        groupName.setString(text);
        groupName.setCommandListener(this);
        groupName.show();
    }

    public void commandAction(Command c, Displayable d) {
        if (null != contact) {
            if (renameContactTextbox.isOkCommand(c)) {
                protocol.renameContact(contact, renameContactTextbox.getString());
                ContactList.activate();
                renameContactTextbox.setString(null);
            }
            return;
        }
        if (!groupName.isOkCommand(c)) {
            return;
        }

        /* Return to contact list */
        String groupName_ = groupName.getString();
        boolean isExist = null != protocol.getGroup(groupName_);
        if (0 == groupName_.length()) {
            protocol.getContactList().activate();
            return;
        }
        switch (action) {
            case ADD_GROUP:
                if (!isExist) {
                    protocol.addGroup(protocol.createGroup(groupName_));
                    protocol.getContactList().activate();
                }
                break;
                
            case RENAME_GROUP:
                boolean isMyName = group.getName().equals(groupName_);
                if (isMyName) {
                    protocol.getContactList().activate();
                    
                } else if (!isExist) {
                    protocol.renameGroup(group, groupName_);
                    protocol.getContactList().activate();
                }
                break;
        }
    }
    public void show() {
        new Select(manageCL).show();
    }
}
