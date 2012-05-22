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
 File: src/DrawControls/VirtualTree.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/

package DrawControls.tree;

import DrawControls.*;
import DrawControls.icons.Icon;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.Options;
import jimm.ui.base.*;
import protocol.*;

/**
 * Tree implementation, which allows programmers to store node data themself
 * 
 * SimpleContactList is successor of VirtualList. It store tree structure in.
 * It shows itself on display and handles user key commands.
 * You must inherit new class from SimpleContactList and reload next functions:
 */
public class SimpleContactList implements ContactListInterface {
    //! Constructor
    public SimpleContactList() {
    }

    private TreeBranch root = new TreeRoot();

    /**
     * Returns root node.
     * Root node is parent for all nodes and never visible.
     */
    private final TreeBranch getRoot() {
        return root;
    }

    public Vector rebuildFlatItems(Vector drawItems) {
        TreeBranch root = getRoot();
        int count = root.getSubnodesCount();
        for (int i = 0; i < count; ++i) {
            TreeNode item = root.elementAt(i);
            drawItems.addElement(item);
            item.isSecondLevel = false;
            if (item instanceof TreeBranch) {
                TreeBranch group = (TreeBranch)item;
                if (group.isExpanded()) {
                    int gcount = group.getSubnodesCount();
                    for (int j = 0; j < gcount; ++j) {
                        TreeNode contact = group.elementAt(j);
                        drawItems.addElement(contact);
                        contact.isSecondLevel = true;
                    }
                }
            }
        }
        return drawItems;
    }

    public void clear() {
        getRoot().clear();
    }
    protected final boolean useGroups() {
        return Options.getBoolean(Options.OPTION_USER_GROUPS);
    }
    protected final boolean hideEmptyGroups() {
        return Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }
    protected final boolean showOffline() {
        return !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }
    public final synchronized void buildTree_(Protocol protocol) {
        boolean showOffline = showOffline();
        Vector contacts = protocol.getContactItems();

        int cCount = contacts.size();
        TreeBranch root = getRoot();
        root.setExpandFlag(false);
        root.clear();
        if (useGroups()) {
            Vector groups = protocol.getGroupItems();
            int gCount = groups.size();
            for (int groupIndex = 0; groupIndex < gCount; ++groupIndex) {
                Group group = (Group)groups.elementAt(groupIndex);
                TreeBranch branch = (TreeBranch)group;
                boolean isExpanded = group.isExpanded();
                group.setExpandFlag(false);

                branch.clear();
                Vector groupContacts = group.getContacts();
                int contactCount = groupContacts.size();
                for (int contactIndex = 0; contactIndex < contactCount; ++contactIndex) {
                    Contact contact = (Contact)groupContacts.elementAt(contactIndex);
                    if (contact.isVisibleInContactList()) {
                        branch.addNode(contact);
                    }
                }
                group.setExpandFlag(isExpanded);
                if (showOffline || (0 < group.getSubnodesCount())) {
                    root.addNode(group);
                }
            }
            for (int contactIndex = 0; contactIndex < cCount; ++contactIndex) {
                Contact contact = (Contact)contacts.elementAt(contactIndex);
                if ((Group.NOT_IN_GROUP == contact.getGroupId()) && contact.isVisibleInContactList()) {
                    root.addNode(contact);
                }
            }
                
        } else {
            for (int contactIndex = 0; contactIndex < cCount; ++contactIndex) {
                Contact contact = (Contact)contacts.elementAt(contactIndex);
                if (contact.isVisibleInContactList()) {
                    root.addNode(contact);
                }
            }
        }
        root.setExpandFlag(true);
    }

    private TreeBranch branch(Group g) {
        return (null != g) ? g : getRoot();
    }
    private void updateBranch(Group g) {
        if (null != g) {
            g.updateGroupData();
        }
    }
    private void hideEmptyGroup(Group g) {
        if ((null != g) && hideEmptyGroups()) {
            if (0 == g.getSubnodesCount()) {
                getRoot().removeChild(g);
            }
        }
    }
    public final synchronized void _updateContactFully(Contact contact) {
        TreeBranch root = getRoot();

        root.removeChild(contact);
        boolean useGroups = useGroups();
        if (useGroups) {
            Vector groups = contact.getProtocol().getGroupItems();
            for (int i = 0; i < groups.size(); ++i) {
                TreeBranch b = (TreeBranch)groups.elementAt(i);
                if (b.removeChild(contact)) {
                    updateBranch((Group)b);
                    hideEmptyGroup((Group)b);
                    break;
                }
            }
        }

        if (contact.getProtocol().inContactList(contact)) {
            Group g = useGroups ? contact.getGroup() : null;
            if (showOffline() || contact.isVisibleInContactList()) {
                if ((null != g) && hideEmptyGroups()) {
                    addGroup(g);
                }
                branch(g).addNode(contact);
            }
            updateBranch(g);
        }
    }
    public final synchronized void _updateContact(Contact contact) {
        TreeBranch root = getRoot();
        Group g = useGroups() ? contact.getGroup() : null;
        if (!contact.isVisibleInContactList()) {
            branch(g).removeChild(contact);
            hideEmptyGroup(g);

        } else if (!branch(g).contains(contact)) {
            if ((null != g) && hideEmptyGroups()) {
                addGroup(g);
            }
            branch(g).addNode(contact);
            
        } else {
            branch(g).sort();
        }
        updateBranch(g);
    }
    private void addGroup(TreeBranch group) {
        if (!getRoot().contains(group)) {
            getRoot().addNode(group);
        }
    }
    public synchronized void groupChanged_(Group group, boolean addGroup) {
        if (addGroup) {
            group.updateGroupData();
            addGroup(group);
        } else {
            root.removeChild(group);
        }
    }

    private Protocol protocol = null;
    public final void addProtocol(Protocol prot) {
        if (protocol != prot) {
            protocol = prot;
            clear();
        }
    }
    public void removeAllProtocols() {
    }
    public final Protocol getProtocol(int accountIndex) {
        return protocol;
    }
    public final int getProtocolCount() {
        return 1;
    }
}
