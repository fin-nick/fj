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
 * File: src/DrawControls/TreeNode.java
 * Version: ###VERSION###  Date: ###DATE###
 * Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/


package DrawControls.tree;

import DrawControls.icons.Icon;
import javax.microedition.lcdui.Font;
import jimm.comm.Sortable;
import jimm.ui.base.*;
import jimm.ui.menu.*;

//! Tree node
/*! This class is used to handle tree nodes (adding, deleting, moveing...) */
public abstract class TreeNode implements Sortable {
    boolean isSecondLevel;
    
    public MenuModel getContextMenu() {
        return null;
    }

    public void getLeftIcons(Icon[] icons) {
    }
    public void getRightIcons(Icon[] icons) {
    }

    public TreeNode() {
    }
}

