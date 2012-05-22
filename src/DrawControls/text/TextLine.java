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
 File: src/DrawControls/TextLine.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis
 *******************************************************************************/
/*
 * TextLine.java
 *
 * Created on 6 Август 2007 г., 18:14
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package DrawControls.text;

import DrawControls.*;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.ui.base.GraphicsEx;

/**
 *
 * @author Artyomov Denis
 * @author Vladimir Kryukov
 */
public final class TextLine {
	public final Vector items = new Vector();
	public int height = 0;
  	public int bigTextIndex = -1;
  	public char last_charaster;
	
	public ListItem elementAt(int index) {
		return (ListItem) items.elementAt(index); 
	}
    void addElement(ListItem item, Font font) {
        item.calcMetrics(font);
        items.addElement(item);
        height = Math.max(height, item.getHeight());
    }
	
	public int getHeight() {
		return height;
	}

    int getWidth() {
		int width = 0;
		for (int i = items.size() - 1; i >= 0; --i) {
            width += elementAt(i).getWidth();
        }  
		return width;
	}

	public void paint(Font[] fontSet, int xpos, int ypos, GraphicsEx g) {
		int count = items.size();
		int lineHeight = getHeight();
		
		for (int i = 0; i < count; ++i) {
			ListItem item = elementAt(i);
			int drawYPos = ypos + lineHeight - item.getHeight();
			if (null != item.image) {
                if (null == item.text) {
                    drawYPos = ypos + (lineHeight - item.getHeight()) / 2;
                    g.drawByLeftTop(item.image, xpos, drawYPos);
                } else {
                    g.drawByLeftTop(item.image, xpos, drawYPos);
                }

			} else if (null != item.text) {
                g.setThemeColor(item.colorType);
                g.setFont(fontSet[item.fontStyle]);
                g.drawString(item.text, xpos, drawYPos, Graphics.TOP|Graphics.LEFT);
			}
			xpos += item.getWidth();
		}
	}
}

