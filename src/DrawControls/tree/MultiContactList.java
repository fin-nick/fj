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

// #sijapp cond.if modules_MULTI is "true" #
package DrawControls.tree;

import DrawControls.*;
import DrawControls.icons.Icon;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.Options;
import jimm.comm.*;
import jimm.ui.base.*;
import protocol.*;

/**
 * Tree implementation, which allows programmers to store node data themself
 * 
 * VirtualContactList is successor of VirtualList. It store tree structure in.
 * It shows itself on display and handles user key commands.
 * You must inherit new class from VirtualContactList and reload next functions:
 */
public class MultiContactList implements ContactListInterface {
    private Protocol[] protocolList = new Protocol[10];
    private ProtocolBranch[] protocolRoots = new ProtocolBranch[10];
    private Vector root = new Vector();

    //! Constructor
    public MultiContactList() {
    }
    public void clear() {
        root.removeAllElements();
    }
    protected final boolean useGroups() {
        return Options.getBoolean(Options.OPTION_USER_GROUPS);
    }
    protected final boolean hideEmptyGroups() {
        return useGroups() && Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }
    protected final boolean showOffline() {
        return !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
    }


    /**
     * Returns root node.
     * Root node is parent for all nodes and never visible.
     */
    private final TreeBranch getRoot(Protocol p) {
        for (int i = 0; i < protocolRoots.length; ++i) {
            if ((null != protocolRoots[i]) && protocolRoots[i].isProtocol(p)) {
                return protocolRoots[i];
            }
        }
        return null;
    }

    private Vector drawItems = null;
    public Vector rebuildFlatItems(Vector drawItems) {
        this.drawItems = drawItems;
        for (int i = 0; i < protocolRoots.length; ++i) {
            if (null == protocolRoots[i]) continue;
            drawItems.addElement(protocolRoots[i]);
            makeFlatItems(protocolRoots[i], false);
        }
        return drawItems;
    }

    private void makeFlatItems(TreeBranch top, boolean secondLevel) {
        if (top.isExpanded()) {
            int count = top.getSubnodesCount();
            for (int i = 0; i < count; ++i) {
                TreeNode item = top.elementAt(i);
                drawItems.addElement(item);
                item.isSecondLevel = secondLevel;
                if (item instanceof TreeBranch) {
                    makeFlatItems((TreeBranch)item, true);
                }
            }
        }
    }
    
    


    public synchronized final void buildTree_(Protocol protocol) {
        boolean useGroups = useGroups();
        boolean showOffline = showOffline();
        Vector contacts = protocol.getContactItems();

        int cCount = contacts.size();
        TreeBranch root = getRoot(protocol);
        if (null == root) {
            return;
        }
        boolean rootExpanded = root.isExpanded();
        root.setExpandFlag(false);
        root.clear();
        if (useGroups) {
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
        root.setExpandFlag(rootExpanded);
    }

    private TreeBranch branch(Group g, TreeBranch root) {
        return ((null != g) && useGroups()) ? g : root;
    }
    private void updateBranch(Group g) {
        if (null != g) {
            g.updateGroupData();
        }
    }
    private void hideEmptyGroup(Group g, TreeBranch root) {
        if ((null != g) && hideEmptyGroups()) {
            if (0 == g.getSubnodesCount()) {
                root.removeChild(g);
            }
        }
    }
    public final synchronized void _updateContactFully(Contact contact) {
        TreeBranch root = getRoot(contact.getProtocol());

        root.removeChild(contact);
        boolean useGroups = useGroups();
        if (useGroups) {
            Vector groups = contact.getProtocol().getGroupItems();
            for (int i = 0; i < groups.size(); ++i) {
                TreeBranch b = (TreeBranch)groups.elementAt(i);
                if (b.removeChild(contact)) {
                    updateBranch((Group)b);
                    hideEmptyGroup((Group)b, root);
                    break;
                }
            }
        }

        if (contact.getProtocol().inContactList(contact)) {
            Group g = useGroups ? contact.getGroup() : null;
            if (showOffline() || contact.isVisibleInContactList()) {
                if ((null != g) && hideEmptyGroups()) {
                    addGroup(root, g);
                }
                branch(g, root).addNode(contact);
            }
            updateBranch(g);
        }
    }
    /**
     * Update visual list
     *
     * Must be called after any changes in contacts
     */
    public final synchronized void _updateContact(Contact contact) {
        TreeBranch root = getRoot(contact.getProtocol());

        Group g = useGroups() ? contact.getGroup() : null;
        if (!contact.isVisibleInContactList()) {
            branch(g, root).removeChild(contact);
            hideEmptyGroup(g, root);

        } else if (!branch(g, root).contains(contact)) {
            if ((null != g) && hideEmptyGroups()) {
                addGroup(root, g);
            }
            branch(g, root).addNode(contact);
        } else {
            branch(g, root).sort();
        }
        updateBranch(g);
    }
    private void addGroup(TreeBranch root, TreeBranch group) {
        if (!root.contains(group)) {
            root.addNode(group);
        }
    }
    public synchronized void groupChanged_(Group group, boolean addGroup) {
        TreeBranch root = getRoot(group.getProtocol());
        if (addGroup) {
            group.updateGroupData();
            addGroup(root, group);
        } else {
            root.removeChild(group);
        }
    }

    public synchronized void removeAllProtocols() {
        for (int i = 0; i < protocolList.length; ++i) {
            protocolRoots[i] = null;
            protocolList[i] = null;
        }
    }
    public synchronized void addProtocol(Protocol prot) {
        int num = -1;
        for (int i = 0; i < protocolList.length; ++i) {
            if (null == protocolList[i]) {
                num = i;
                break;
            }
        }
        if (-1 == num) {
            return;
        }
        protocolRoots[num] = (null == prot) ? null : new ProtocolBranch(prot);
        protocolList[num] = prot;

        root.removeAllElements();
        for (int i = 0; i < protocolRoots.length; ++i) {
            if (null != protocolRoots[i]) {
                root.addElement(protocolRoots[i]);
            }
        }
        Util.sort(root);
    }

    public final Protocol getProtocol(int accountIndex) {
        return protocolList[accountIndex];
    }
    public final int getProtocolCount() {
        return protocolList.length;
    }
}
// #sijapp cond.end #