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
 File: src/DrawControls/ListItem.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Vladimir Kryukov
 *******************************************************************************/


package DrawControls.text;

import DrawControls.icons.Icon;
import javax.microedition.lcdui.*;

//! Data for list item
/*! All members of class are made as public 
    in order to easy access. 
 */
public final class ListItem {
	public String text; //!< Text of node

	public Icon image;

	private short itemWidth = -1;
    private short itemHeigth = -1;

	public byte fontStyle; //!< Font style
	public byte colorType; //!< Color of node text

	ListItem() {
		colorType = 0;
		fontStyle = Font.STYLE_PLAIN;
	}

	ListItem(String text, byte colorType, byte fontStyle) {
		this.text = text;
		this.colorType = colorType;
		this.fontStyle = fontStyle;
	}

	public ListItem(Icon image, String text, int itemWidth, int itemHeigth) {
		this.image = image;
		this.text = text;
		this.itemWidth = (short)itemWidth;
		this.itemHeigth = (short)itemHeigth;
	}

	//! Set all member to default values
	public void clear() {
		text = "";
		image = null;
		colorType = 0;
		fontStyle = Font.STYLE_PLAIN;
	}

    void calcMetrics(Font font) {
        if (null != image) {
            itemHeigth = (short)Math.max(image.getHeight(), itemHeigth);
            itemWidth = (short)Math.max(image.getWidth() + 1, itemWidth);

        } else if (null == text) {
            itemHeigth = (short)0;
            itemWidth = 0;
        
        } else {
            itemHeigth = (short)font.getHeight();
            itemWidth = (short)font.stringWidth(text);
        }
    }
    public int getHeight() {
		return itemHeigth;
	}
	
    public int getWidth() {
		return itemWidth;
	}
}