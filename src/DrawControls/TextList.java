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
 File: src/DrawControls/TextList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis
 *******************************************************************************/


package DrawControls;

import DrawControls.icons.Icon;
import DrawControls.text.FormatedText;
import DrawControls.text.TextLine;
import java.util.*;
import javax.microedition.lcdui.*;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.menu.*;
import jimm.util.ResourceBundle;


//! Text list
/*! This class store text and data of lines internally
    You may use it to show text with colorised lines :) */
public class TextList extends VirtualList {
    protected FormatedText formatedText = new FormatedText();

	// protected int getSize()
	public int getSize() {
		int size = formatedText.getSize();
		if (0 == size) return 0;
		return (formatedText.getLine(size - 1)).items.isEmpty() ? size - 1 : size;
	}

	protected boolean isItemSelected(int index) {
		int selIndex = getCurrItem();
		int textIndex = (selIndex >= formatedText.getSize())
                ? -1 : formatedText.getLine(selIndex).bigTextIndex;
		if (textIndex == -1) return false;
		return (formatedText.getLine(index).bigTextIndex == textIndex);
	}

    public void removeFirstText() {
        int size = formatedText.getSize();
        if (0 < size) {
            formatedText.removeFirstText();
            // TODO: save cursor position
            int delta = formatedText.getSize() - size;
            topItem = Math.max(0, topItem + delta);
            setCurrentItem(Math.max(0, getCurrItem() + delta));
        }
    }

	//! Construct new text list with default values of colors, font size etc...
	public TextList(String capt) {
		super(capt);
        int width = NativeCanvas.getInstance().getMinScreenMetrics();
		formatedText.setWidth(width - scrollerWidth - 3);
	}
	
	public int getItemHeight(int itemIndex) {
		if (itemIndex >= formatedText.getSize()) return 1;
		return formatedText.getLine(itemIndex).getHeight();
	}
	
	// Overrides VirtualList.drawItemData
	protected void drawItemData(
            GraphicsEx g,
            int index,
            int x1, int y1,
            int x2, int y2) {
		formatedText.getLine(index).paint(getFontSet(), 1, y1, g);
	}
	

	// Overrides VirtualList.moveCursor
	protected void moveCursor(int step) {
        // #sijapp cond.if modules_STYLUS is "true"#
        if (stylusMoveCursor(step)) {
            return;
        }
        // #sijapp cond.end#
        int currItem = getCurrItem();

		switch (step) {
		case -1:
		case 1:
			int currTextIndex = getCurrTextIndex();
			int size = formatedText.getSize();
            int halfSize = getVisCount() / 2;
            int changeCounter = 0;
			for (int i = 0; i < halfSize;) {
				currItem += step;
				if ((currItem < 0) || (currItem >= size)) break;
				TextLine item = formatedText.getLine(currItem);
				if (currTextIndex != item.bigTextIndex) {
					currTextIndex = item.bigTextIndex;
					changeCounter++;
					if ((changeCounter == 2) || (!isVisibleItem(currItem) && (i > 0))) {
						currItem -= step;
						break;
					}
				}
				
				if (!isVisibleItem(currItem) || (changeCounter != 0)) i++;
			}

            setCurrentItem(currItem);
			break;

		default:
            setCurrentItem(currItem + step);
			return;
		}
	}

	// Returns lines of text which were added by 
	// methon addBigText in current selection
	public String getCurrText(int offset, boolean wholeText) {
        return formatedText.getText(getCurrTextIndex(), offset, wholeText);
	}

	public int getCurrTextIndex() {
		return formatedText.getTextIndex(getCurrItem());
	}
    public void setCurrTextIndex(int textIndex) {
        for (int i = 0; i < formatedText.getSize(); ++i) {
            if (textIndex == formatedText.getLine(i).bigTextIndex) {
                setCurrentItem(i);
                return;
            }
        }
        setCurrentItem(0);
        return;
    }

	//! Remove all lines form list
	public void clear() {
        formatedText.clear();
		setCurrentItem(0);
		invalidate();
	}

    public TextList doCRLF(int blockTextIndex) {
        formatedText.doCRLF(blockTextIndex);
		return this;
	}
	
	public TextList addImage(Icon image, String altarnateText, int imageWidth, int imageHeight, int blockTextIndex) {
        formatedText.addImage(image, altarnateText, imageWidth, imageHeight, blockTextIndex);
		return this;
	}

	public TextList addBigText(String text, byte colorType, int fontStyle, int textIndex) {
        formatedText.addBigText(getFontSet(), text, colorType, (byte)fontStyle, textIndex);
		invalidate();
		return this;
	}

	public void addTextWithEmotions(String text, byte colorType, int fontStyle, int textIndex) {
        formatedText.addTextWithEmotions(getFontSet(), text, colorType, (byte)fontStyle, textIndex);
		invalidate();
	}

    protected int backCode = -1;
    protected int defaultCode = -1;
    
    protected void restoring() {
        NativeCanvas.setCommands("menu", "back");
    }
    public final void setMenuCodes(int backCode, int defCode) {
        this.backCode = backCode;
        this.defaultCode = defCode;
    }
    protected MenuModel getMenu() {
        return null;
    }

    protected void itemSelected() {
        MenuModel defaultActionMenu = getMenu();
        if ((-1 != defaultCode) && (null != defaultActionMenu)) {
            defaultActionMenu.exec(null, defaultCode);
        }
    }
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (type == KEY_PRESSED) {
            switch (actionCode) {
                case NativeCanvas.LEFT_SOFT:
                    MenuModel menu = getMenu();
                    if (null != menu) {
                        new Select(menu).show();
                    }
                    return;
                    
                case NativeCanvas.RIGHT_SOFT:
                case NativeCanvas.CLOSE_KEY:
                    backAct();
                    return;
            }
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    private void backAct() {
        if (backCode == -1) {
            back();
        } else {
            getMenu().exec(null, backCode);
        }
    }    
}