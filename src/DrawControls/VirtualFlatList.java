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
 File: src/DrawControls/VirtualList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Igor Palkin, Vladimir Kryukov
 *******************************************************************************/

package DrawControls;

import DrawControls.icons.ImageList;
import javax.microedition.lcdui.Font;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;

/**
 *
 * @author Vladimir Krukov
 */
public abstract class VirtualFlatList extends VirtualList {
    
    /**
     * Creates a new instance of VirtualFlatList
     */
    public VirtualFlatList(String capt) {
        super(capt);
    }
    
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (type == CanvasEx.KEY_PRESSED) {
            switch (keyCode) {
                case NativeCanvas.LEFT_SOFT:
                    new Select(getMenu()).show();
                    return;
                case NativeCanvas.RIGHT_SOFT:
                case NativeCanvas.CLOSE_KEY:
                    getMenu().exec(null, MenuModel.BACK_COMMAND_CODE);
                    return;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    protected void restoring() {
        NativeCanvas.setCommands("menu", "back");
    }
    protected abstract MenuModel getMenu();
//    protected abstract int getItemHeight(int itemIndex);
//    protected abstract void drawItemData(GraphicsEx g, int index, int x1, int y1, int x2, int y2);
//    protected abstract void itemSelected();
    //protected abstract void onCursorMove();
    
}
