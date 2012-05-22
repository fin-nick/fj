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
import jimm.*;
import jimm.comm.*;
import jimm.modules.*;
import jimm.modules.traffic.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import protocol.*;

/**
 *
 * @author Vladimir Krukov
 */
public final class VirtualContactList extends VirtualList {
    
    protected boolean isTreeBuilded = false;
    private Vector drawItems = new Vector();
    private int itemHeight = 0;
    private ContactListListener clListener;
    private Icon[] leftIcons = new Icon[5];
    private Icon[] rightIcons = new Icon[1];
    private int stepSize;
    private int nodeRectHeight;
    private boolean useGroups;
    private boolean showStatusLine;
    private ContactListInterface model;

    private TreeNode currentNode = null;
    private boolean setCurrentItemAnyway = false;

    /** Creates a new instance of VirtualContactList */
    public VirtualContactList() {
        super("");
        // #sijapp cond.if modules_MULTI isnot "true" #
        model = new SimpleContactList();
        // #sijapp cond.else #
        model = new MultiContactList();
        // #sijapp cond.end #
        stepSize = Math.max(getDefaultFont().getHeight() / 4, 2);
        updateOption();
    }
    public ContactListInterface getModel() {
        return model;
    }
    public void setCLListener(ContactListListener listener) {
        clListener = listener;
    }

    protected final int getItemHeight(int itemIndex) {
        return itemHeight;
    }
    protected final void setItemHeight(int height) {
        itemHeight = Math.max(height, CanvasEx.minItemHeight);
        nodeRectHeight = Math.max(itemHeight / 2, 7);
        if (0 == (nodeRectHeight & 1)) {
            nodeRectHeight--;
        }
    }

    /**
     * Tree control call this function for request of data
     * for tree node to be drawn
     */
    private final TreeNode getDrawItem(int index) {
        return (TreeNode) drawItems.elementAt(index);
    }

    //! For internal use only
    protected final int getSize() {
        return drawItems.size();
    }


    protected void restoring() {
        NativeCanvas.setCommands("menu", "context_menu");
        setFontSet(GraphicsEx.contactListFontSet);
    }
    protected final void beforShow() {
        update(getCurrentProtocol());
        setItemHeight(calcItemHeight(getCurrentProtocol()));
    }
    public final void update(Protocol p) {
        stepSize = Math.max(getDefaultFont().getHeight() / 4, 2);
        updateOption();
        lock();
        if (null != p) {
            buildTree(p);
            updateTitle();
            setItemHeight(calcItemHeight(p));
        }
        unlock();
    }

    /** Returns current selected node */
    private final TreeNode getCurrentNode() {
        return getSafeNode(getCurrItem());
    }
    private final TreeNode getSafeNode(int index) {
        if ((index < drawItems.size()) && (index >= 0)) {
            return getDrawItem(index);
        }
        return null;
    }

    /**
     * Remove all nodes from tree
     */
    protected final void clear() {
        model.clear();
        setCurrentItem(0);
        
    }
    public final void clearUI() {
        drawItems.removeAllElements();
    }

    private void updateOption() {
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS);
        showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
    }
    /**
     * Build path to node int tree.
     */
    private void expandNodePath(TreeNode node) {
        if ((node instanceof Contact) && useGroups) {
            Contact c = (Contact)node;
            Protocol p = c.getProtocol();
            if (null != p) {
                Group group = p.getGroupById(c.getGroupId());
                if (null != group) {
                    group.setExpandFlag(true);
                }
            }
        }
    }

    private Vector[] listOfContactList = new Vector[]{new Vector(), new Vector()};
    private int visibleListIndex = 0;
    protected final void afterUnlock() {
        TreeNode current = currentNode;
        boolean anyway = setCurrentItemAnyway;
        setCurrentItemAnyway = false;
        currentNode = null;
        int prevIndex = -1;
        if (null != current) {
            expandNodePath(current);
            prevIndex = anyway ? -1 : Util.getIndex(drawItems, current);
        }
        visibleListIndex ^= 1;
        drawItems = model.rebuildFlatItems(listOfContactList[visibleListIndex]);
        if (null != current) {
            int currentIndex = Util.getIndex(drawItems, current);
            if ((prevIndex != currentIndex) && (-1 != currentIndex)) {
                setCurrentItem(currentIndex);
            }
        }
        if (drawItems.size() <= getCurrItem()) {
            setCurrentItem(0);
        }
        listOfContactList[visibleListIndex ^ 1].removeAllElements();
    }

    /**
     * Set node as current. Make autoscroll if needs.
     *
     * Usage:
     * <code>
     * lock();
     * ...
     * setCurrentNode(currentNode);
     * unlock();
     * </code>
     */
    private final void setCurrentNode(TreeNode node, boolean anyway) {
        if (null == node) return;
        currentNode = node;
        setCurrentItemAnyway = anyway;
    }
    /**
     * Expand or collapse tree node.
     * NOTE: this is not recursive operation!
     */
    private final void setExpandFlag(TreeBranch node, boolean value) {
        lock();
        TreeNode currentNode = getCurrentNode();
        node.setExpandFlag(value);
        setCurrentNode(currentNode, false);
        unlock();
    }
    protected final void itemSelected() {
        TreeNode item = getCurrentNode();
        if (null == item) {
            return;
        }
        if (item instanceof Contact) {
            ((Contact)item).activate();

        } else if (item instanceof Group) {
            Group group = (Group)item;
            setExpandFlag(group, !group.isExpanded());

        } else if (item instanceof TreeBranch) {
            TreeBranch root = (TreeBranch)item;
            setExpandFlag(root, !root.isExpanded());
        }
    }

    /**
     * Update visual list
     *
     * Must be called after any changes in contacts
     */
    private void updateContact_(Contact contact, boolean fullUpdate, boolean setCurrent) {
        if (!isTreeBuilded) {
            return;
        }
        lock();
        try {
            final TreeNode current = setCurrent ? contact : getCurrentNode();

            if (fullUpdate) {
                model._updateContactFully(contact);
            } else {
                model._updateContact(contact);
            }

            // change status for chat (if exists)
            contact.setStatusImage();

            updateTitle();

            setCurrentNode(current, setCurrent);
        } catch (Exception e) {
        }
        unlock();
    }
    private void groupChanged_(Group group, boolean addGroup) {
        if (useGroups) {
            lock();
            final TreeNode current = getCurrentNode();
            model.groupChanged_(group, addGroup);
            setCurrentNode(current, false);
            unlock();
        }
    }
    /** Add a contact list item */
    public final void updateContact(Contact cItem) {
        updateContact_(cItem, false, false);
    }
    public final void updateContactStatus(Contact cItem) {
        updateContact_(cItem, false, false);
    }
    public final void setActiveContact(Contact cItem) {
        updateContact_(cItem, false, true);
    }
    public final void updateContactWithNewMessage(Contact cItem, boolean isPrivate) {
        updateContact_(cItem, false, isPrivate && (Jimm.getCurrentDisplay() == this));
    }
    public final void addContact(Contact cItem) {
        updateContact_(cItem, true, true);
    }
    public final void moveContact(Contact cItem) {
        updateContact_(cItem, true, true);
    }
    public final void addLocalContact(Contact cItem) {
        updateContact_(cItem, true, false);
    }
    public final void removeContact(Contact cItem) {
        updateContact_(cItem, true, false);
    }

    /** Add new group */
    public final void addGroup(Group group) {
        groupChanged_(group, true);
    }

    /** remove existing group */
    public final void removeGroup(Group group) {
        groupChanged_(group, false);
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    protected final void captionTapped() {
        jimm.chat.ChatHistory.instance.showChatList();
    }
    // #sijapp cond.end#

    //Updates the title of the list
    private Icon[] capIcons = new Icon[2];
    private Icon[] getCapIcons(Protocol p) {
        capIcons[0] = p.getCurrentStatusIcon();
        // #sijapp cond.if modules_XSTATUSES is "true" #
        Icon x = null;
        // #sijapp cond.if protocols_ICQ is "true" #
        if (p  instanceof protocol.icq.Icq) {
    	    x = ((protocol.icq.Icq)p).getXStatus().getIcon();
    	}
        // #sijapp cond.end #
        // #sijapp cond.if protocols_MRIM is "true" #
        if (p  instanceof protocol.mrim.Mrim) {
    	    x = ((protocol.mrim.Mrim)p).getXStatus().getIcon();
    	}
        // #sijapp cond.end #
        // #sijapp cond.if protocols_JABBER is "true" #
        if (p  instanceof protocol.jabber.Jabber) {
    	    x = ((protocol.jabber.Jabber)p).getXStatus().getIcon();
    	}
        // #sijapp cond.end #
        if (!Options.getBoolean(Options.OPTION_REPLACE_STATUS_ICON) || null == x) {
            capIcons[1] = x;
        } else {
            capIcons[0] = x;
            capIcons[1] = null;
        }
        // #sijapp cond.end #
        return capIcons;
    }
    public void updateTitle() {
        String text = "";
        // #sijapp cond.if modules_MULTI isnot "true" #
        Protocol protocol = getCurrentProtocol();
        setCapImages(getCapIcons(protocol));
        text += protocol.getOnlineCount() + "/" + protocol.getContactItems().size();
        if (!Options.getBoolean(Options.OPTION_SHOW_SOFTBAR)) { //add
            text += "-" + Util.getDateString(true);             //add
        }                                                       //add
        // #sijapp cond.end #

        // #sijapp cond.if modules_TRAFFIC is "true" #
        int traffic = Traffic.getInstance().getSessionTraffic();
        if (1024 <= traffic) {
            // #sijapp cond.if modules_MULTI isnot "true" #
            text += "-";
            // #sijapp cond.end#
            text += StringConvertor.bytesToSizeString(traffic, false);
        }
        // #sijapp cond.end#

        // #sijapp cond.if target is "SIEMENS2"#
        String accuLevel = System.getProperty("MPJC_CAP");
        if (null != accuLevel) {
            text += "-" + accuLevel + "%";
        }
        // #sijapp cond.end#
        // #sijapp cond.if modules_MULTI is "true" #
        if (StringConvertor.isEmpty(text)) {
            text = "Jimm";
        }
        // #sijapp cond.end#
        setCaption(text);
    }

    public final void invalidateTree() {
        isTreeBuilded = false;
    }
    // Builds contacts tree
    public final void buildTree(Protocol p) {
        if (null == p) {
            return;
        }
        if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
            Vector groups = p.getGroupItems();
            for (int i = 0; i < groups.size(); ++i) {
                ((Group)groups.elementAt(i)).updateGroupData();
            }
        }
        if (!isTreeBuilded) {
            TreeNode currentNode = getCurrentNode();
            model.buildTree_(p);
            setCurrentNode(currentNode, false);
            isTreeBuilded = true;
        }
    }

    protected int calcItemHeight(Protocol p) {
        int height = Math.max(getDefaultFont().getHeight(),
                GraphicsEx.getMaxImagesHeight(getCapIcons(p)));

        Vector cItems = p.getContactItems();
        int count = Math.min(cItems.size(), 10);
        Icon[] icons = new Icon[leftIcons.length];
        for (int i = 0; i < count; ++i) {
            TreeNode node = (TreeNode) cItems.elementAt(i);
            node.getLeftIcons(icons);
            height = Math.max(height, GraphicsEx.getMaxImagesHeight(icons));
            node.getRightIcons(icons);
            height = Math.max(height, GraphicsEx.getMaxImagesHeight(icons));
        }
        height += 2;
        if (showStatusLine) {
            int fontH = getDefaultFont().getHeight() + GraphicsEx.statusLineFont.getHeight();
            height = Math.max(fontH, height);
        }
        return height;
    }

    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        TreeNode item = getCurrentNode();
        Contact current = (item instanceof Contact) ? (Contact)item : null;
        if (CanvasEx.KEY_PRESSED == type) {
            clListener.setCurrentContact(current);
            switch (keyCode) {
                case NativeCanvas.LEFT_SOFT:
                    clListener.activateMainMenu();
                    return;
                    
                case NativeCanvas.RIGHT_SOFT:
                    if ((null == item) || getProtocol(item).isConnecting()) {
                        // #sijapp cond.if modules_MULTI is "true"#
                        if (!(item instanceof ProtocolBranch)) {
                            return;
                        }
                        // #sijapp cond.else#
                        return;
                        // #sijapp cond.end#
                    }
                    MenuModel menu = item.getContextMenu();
                    if (null != menu) {
                        new Select(menu).show();
                    }
                    return;

                case NativeCanvas.CLEAR_KEY:
                    if ((item instanceof Contact) && ((Contact)item).hasChat()) {
                        ((Contact)item).getChat().deleteChats();
                    }
                    return;
            }
        }

        if (JimmUI.execHotKey(current, keyCode, type)) {
            return;
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    private final Protocol getProtocol(TreeNode node) {
        // #sijapp cond.if modules_MULTI is "true" #
        if (node instanceof Contact) {
            return ((Contact)node).getProtocol();
        }
        if (node instanceof Group) {
            return ((Group)node).getProtocol();
        }
        if (node instanceof ProtocolBranch) {
            return ((ProtocolBranch)node).getProtocol();
        }        
        if (0 < getCurrItem()) {
            setCurrentItem(0);
            return getCurrentProtocol();
        }
        // #sijapp cond.end #
        return model.getProtocol(0);
    }
    public final Protocol getCurrentProtocol() {
        return getProtocol(getCurrentNode());
    }
    
    
    /** draw + or - before node text */
    private void drawNodeRect(GraphicsEx g, TreeBranch branch,
            int x, int y1, int y2) {

        int height = nodeRectHeight;
        final int half = (height + 1) / 2;
        final int quarter = (half + 1) / 2;
        int y = (y1 + y2 - height) / 2;
        Graphics gr = g.getGraphics();
        if (0 == branch.getSubnodesCount()) {
            x += quarter;
            gr.drawLine(x, y, x, y + height);
            while (0 < height) {
                gr.drawLine(x, y, x, y + 1);
                gr.drawLine(x, y + height - 1, x, y + height);
                height -= 2;
                y += 1;
                x += 1;
            }
            return;
        }
        if (branch.isExpanded()) {
            y += quarter;
            while (0 < height) {
                gr.drawLine(x, y, x + height, y);
                height -= 2;
                y += 1;
                x += 1;
            }

        } else {
            x += quarter;
            while (0 < height) {
                gr.drawLine(x, y, x, y + height);
                height -= 2;
                y += 1;
                x += 1;
            }
        }
    }

    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int x2, int y2) {
        TreeNode node = getDrawItem(index);

        for (int i = 0; i < leftIcons.length; ++i) {
            leftIcons[i] = null;
        }
        rightIcons[0] = null;
        node.getLeftIcons(leftIcons);
        node.getRightIcons(rightIcons);

        int x = x1;
        // #sijapp cond.if modules_MULTI is "true" #
        if (node instanceof ProtocolBranch) {
            g.setThemeColor(CanvasEx.THEME_PROTOCOL_BACK);
            byte progress = ((ProtocolBranch)node).getProtocol().getConnectingProgress();
            int width = x2 - x + 4;
            if (progress < 100) {
                width = width * progress / 100;
            }
            g.fillRect(x - 2, y1, width, y2 - y1);

            g.setThemeColor(CanvasEx.THEME_PROTOCOL);
            g.setFont(getFontSet()[Font.STYLE_PLAIN]);
            drawNodeRect(g, (TreeBranch)node, x, y1, y2);
            x += nodeRectHeight + 2;
            g.drawString(leftIcons, node.getText(), rightIcons, x, y1, x2 - x, y2 - y1);
            return;
        }
        // #sijapp cond.end #
        if (useGroups) {
            if (node instanceof TreeBranch) {
                g.setThemeColor(CanvasEx.THEME_GROUP);
                g.setFont(getFontSet()[Font.STYLE_PLAIN]);

                drawNodeRect(g, (TreeBranch)node, x, y1, y2);
                x += nodeRectHeight + 4; //distance to a group name
                g.drawString(leftIcons, node.getText(), rightIcons, x, y1, x2 - x, y2 - y1);
                return;
            }
            x += node.isSecondLevel ? stepSize : nodeRectHeight + 2;
        }

        Contact c = (Contact)node;
        g.setThemeColor(c.getTextTheme());
        g.setFont(getFontSet()[c.hasChat() ? Font.STYLE_BOLD : Font.STYLE_PLAIN]);
        if (showStatusLine) {
            drawContact(g, (Contact)node, x, y1, x2 - x, y2 - y1);
        } else {
            g.drawString(leftIcons, node.getText(), rightIcons, x, y1, x2 - x, y2 - y1);
        }
    }

    private final void drawContact(GraphicsEx g, Contact c, int x, int y, int w, int h) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, w, h);

        int lWidth = g.drawImages(leftIcons, x, y, h);
        if (lWidth > 0) {
            lWidth++;
        }
        int rWidth = g.getImagesWidth(rightIcons);
        if (rWidth > 0) {
            rWidth++;
        }
        g.drawImages(rightIcons, x + w - rWidth, y, h);
        g.setClip(x + lWidth, y, w - lWidth - rWidth, h);
        g.drawString(c.getText(), x + lWidth, y, Graphics.LEFT + Graphics.TOP);

        Font f = GraphicsEx.statusLineFont;
        g.setFont(f);
        g.setThemeColor(THEME_CONTACT_STATUS);
        int fh = f.getHeight();
        g.drawString(c.getStatusMessage(), x + lWidth, y + fh, Graphics.LEFT + Graphics.TOP);

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
}
