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
import DrawControls.icons.Icon;
import javax.microedition.lcdui.*;
import jimm.comm.StringConvertor;
import jimm.ui.*;
import jimm.ui.base.*;

/**
 * This class is base class of owner draw list controls
 *
 * It allows you to create list with different colors and images. 
 * Base class of VirtualDrawList if Canvas, so it draw itself when
 * paint event is heppen. VirtualList have cursor controlled of
 * user
 */

public abstract class VirtualList extends CanvasEx {
    public final static int MEDIUM_FONT = Font.SIZE_MEDIUM;
    public final static int LARGE_FONT = Font.SIZE_LARGE;
    public final static int SMALL_FONT = Font.SIZE_SMALL;
    
    // Commands to react to VL events
    private VirtualListCommands vlCommands;

    // Caption of VL
    private Icon[] capImages;
    private String caption;
    private static Icon messageIcon;
    private String ticker;
    private static int captionHeight = -1;

    // Index for current item of VL
    private int currItem = 0;
    // Index of top visilbe item 
    protected int topItem = 0;

    // Set of fonts for quick selecting
    private Font[] fontSet;


    //! Create new virtual list with default values  
    public VirtualList(String capt) {
        setCaption(capt);
        fontSet = GraphicsEx.chatFontSet;
    }
    public static final void setMessageIcon(Icon icon) {
        messageIcon = icon;
    }
    public static final Icon getMessageIcon() {
        return messageIcon;
    }

    /**
     * Request number of list elements to be shown in list.
     *
     * You must return number of list elements in successtor of
     * VirtualList. Class calls method "getSize" each time before it drawn
     */
    abstract protected int getSize();

    protected final Font[] getFontSet() {
        return fontSet;
    }

    public final Font getDefaultFont() {
        return fontSet[Font.STYLE_PLAIN];
    }
    
    // returns height of draw area in pixels
    private int getDrawHeight() {
        return getHeight() - getCapHeight();
    }

    //! Sets new font size and invalidates items
    protected final void setFontSet(Font[] set) {
        fontSet = set;
        setTopItem(calcOptimalTopItem(currItem));
    }

    public final void setCapImages(Icon[] images) {
        capImages = images;
        invalidate();
    }
    
    public final void setVLCommands(VirtualListCommands vlCommands) {
        this.vlCommands = vlCommands;
    }
    
    /** Returns number of visibled lines of text which fits in screen */
    protected final int getVisCount() {
        return getVisCountFrom(topItem);
    }
    private final int getVisCountFrom(int top) {
        int size = getSize();
        if ((0 == size) || (top < 0)) return 0;

        int height = getDrawHeight();
        int counter = 0;
        for (int i = top; i < size; ++i) {
            height -= getItemHeight(i);
            if (height < 0) return counter;
            counter++;
        }
        
        for (int i = top - 1; i >= 0; --i) {
            height -= getItemHeight(i);
            if (height < 0) return counter;
            counter++;
        }
        
        return counter;
    }
    
    //! Returns height of each item in list
    protected abstract int getItemHeight(int itemIndex);

    // check for position of top element of list and change it, if nesessary
    private int calcOptimalTopItem(int currItem) {
        int size = getSize();
        if (size == 0) {
            return 0;
        }
        if (currItem <= topItem) {
            return Math.max(0, currItem);
        }
        
        int height = getDrawHeight();
        int item = currItem;
        while (item >= topItem) {
            height -= getItemHeight(item--);
            if (height <= 0) {
                item += 2;
                break;
            }
        }
        return Math.max(item, topItem);
    }
    private void setTopItem(int top) {
        topItem = Math.max(0, top);
    }

    // Check does item with index visible
    protected final boolean isVisibleItem(int index) {
        return (index >= topItem) && (index <= (topItem + getVisCount()));
    }

    protected void onCursorMove() {
    }

    public final void setCurrentItem(int index) {
        int lastCurrItem = currItem;
        index = Math.max(Math.min(index, getSize() - 1), 0);
        if (lastCurrItem != index) {
            currItem = index;
            setTopItem(calcOptimalTopItem(currItem));
            invalidate();
            onCursorMove();
        }
    }

    public final int getCurrItem() {
        return currItem;
    }

    protected void moveCursor(int step) {
        // #sijapp cond.if modules_STYLUS is "true"#
        if (stylusMoveCursor(step)) {
            return;
        }
        // #sijapp cond.end#
        setCurrentItem(getCurrItem() + step);
    }

    protected void itemSelected() {}
    
    private void navigationKeyReaction(int keyCode, int actionCode) {
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_DOWN:
                moveCursor(1);
                break;
            case NativeCanvas.NAVIKEY_UP:
                moveCursor(-1);
                break;
            case NativeCanvas.NAVIKEY_FIRE:
                itemSelected();
                break;
        }
        switch (keyCode) {
        case NativeCanvas.KEY_NUM1:
            setCurrentItem(0);
            break;
            
        case NativeCanvas.KEY_NUM7:
            setCurrentItem(getSize() - 1);
            break;

        case NativeCanvas.KEY_NUM3:
            moveCursor(-getVisCount());
            break;
            
        case NativeCanvas.KEY_NUM9:
            moveCursor(getVisCount());
            break;
        }

    }

    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if ((null != vlCommands) && (KEY_PRESSED == type)) {
            if (vlCommands.onKeyPress(this, keyCode, actionCode)) {
                return;
            }
        }
        if ((KEY_REPEATED == type) || (KEY_PRESSED == type)) {
            navigationKeyReaction(keyCode, actionCode);
        }
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    protected final boolean stylusMoveCursor(int step) {
        int top = topItem;
        int visible = getVisCount();
        int current = currItem;
        if ((current < top) || (top + visible < current)) {
            if (step < 0) {
                setCurrentItem(top + visible - 1);
            } else {
                setCurrentItem(top);
            }
            return true;
        }
        return false;
    }
    private int calcTopItem(final int top) {
        int size = getSize();
        if (size == 0) {
            return 0;
        }
        if (topItem < top) {
            return Math.max(0, Math.min(top, size - getVisCountFrom(size - 1)));
        }
        return Math.max(top, 0);
    }
    protected int getItemByCoord(int x, int y) {
        // is pointing on scroller
        if (x >= (getWidth() - 3 * scrollerWidth)) {
            return -1;
        }
        
        int size = getSize();
        // is pointing on data area
        int itemY1 = getCapHeight();
        if (y < itemY1) {
            for (int i = topItem; 0 <= i; --i) {
                if (itemY1 <= y) {
                    return i;
                }
                itemY1 -= getItemHeight(i);
            }
            
        } else {
            for (int i = topItem; i < size; ++i) {
                itemY1 += getItemHeight(i);
                if (y < itemY1) {
                    return i;
                }
            }
        }
        return -1;
    }
    private int getStartYByItem(int top) {
        int y = 0;
        for (int i = 0; i < top; ++i) {
            y += getItemHeight(i);
        }
        return y;
    }

    protected boolean pointerPressed(int item, int x, int y) {
        final int currentItem = getCurrItem();
        if (currentItem != item) {
            setCurrentItem(item);
            return true;
        }
        return false;
    }
    
    private int calcTopByMetrix(int delta, int inc) {
        TouchControl nat = NativeCanvas.getInstance().touchControl;
        int top = nat.prevTopItem;
        delta += nat.prevHeight / 2;
        int offset = (0 < inc) ? 0 : +1;
        while (0 <= delta) {
            delta -= getItemHeight(top + offset);
            if (delta < 0) return top;
            top += inc;
            if (top < 0) return 0;
        }
        return top;
    }
    protected void stylusGeneralMoved(int fromX, int fromY, int toX, int toY) {
        if (fromX >= (getWidth() - 2 * scrollerWidth)) {
            TouchControl nat = NativeCanvas.getInstance().touchControl;
            int top = calcTopItem(nat.calcScrollPosition(getSize()));

            boolean update = (top != topItem);
            setTopItem(top);
            if (update) {
                invalidate();
            }

        } else {
            int item = getItemByCoord(toX, toY);
            if (0 <= item) {
                int delta = fromY - toY;
                int pos = calcTopByMetrix(Math.abs(delta), (delta < 0) ? -1 : 1);
                int top = calcTopItem(pos);

                boolean update = (top != topItem);
                setTopItem(top);
                if (update) {
                    invalidate();
                }
            }
        }
    }

    protected final void stylusPressed(int x, int y) {
        TouchControl nat = NativeCanvas.getInstance().touchControl;
        if (y < getCapHeight()) {
            nat.scrollingOn = false;
            return;
        }

        if (x >= (getWidth() - 2 * scrollerWidth)) {
            y -= getCapHeight();
            int[] metrix = GraphicsEx.getVertScrollMetrix(getDrawHeight(),
                    topItem, getVisCount(), getSize());
            
            nat.tappingOn = false;
            nat.scrollingOn = false;
            nat.kineticOn = false;
            if (null == metrix) {
                return;
            }
            int scrollHeight = (metrix[1] - metrix[0]);
            int scrollMiddle = (metrix[1] + metrix[0]) / 2;
            int scrollLikeHeight = Math.max(scrollHeight, minItemHeight);
            int someY = y - (scrollMiddle - scrollLikeHeight / 2);
            if ((0 < someY) && (someY < scrollLikeHeight)) {
                nat.scrollingOn = true;
                nat.prevTopY = metrix[0];
                nat.prevHeight = getDrawHeight() - scrollHeight;
            }

        } else {
            int item = getItemByCoord(x, y);
            if (0 <= item) {
                nat.prevTopItem = topItem;
                nat.prevTopY = getStartYByItem(topItem);
                nat.prevHeight = getItemHeight(item);
                nat.isSecondTap = !pointerPressed(item, x, y);
            }
        }
    }

    protected void captionTapped() {
    }
    
    protected void stylusTap(int x, int y, boolean longTap) {
        if (x >= (getWidth() - scrollerWidth)) {
            return;
        }
        if (y < getCapHeight()) {
            captionTapped();
            return;
        }
        int item = getItemByCoord(x, y);
        if (item >= 0) {
            if (longTap || NativeCanvas.getInstance().touchControl.isSecondTap) {
                itemSelected();
            }
        }
    }
    // #sijapp cond.end#

    /**
     * Set caption text for list
     */
    public final void setCaption(String capt) {
        // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
        if ((null != caption) && caption.equals(capt)) return;
        caption = capt;
        if (NativeCanvas.isFullScreen()) {
            invalidate();
        } else {
            NativeCanvas.setCaption(caption);
        }
        // #sijapp cond.end#
    }

    public final String getCaption() {
        return caption;
    }
    
    public final void setTicker(String tickerString) {
        if ((null != tickerString) && tickerString.equals(ticker)) {
            return;
        }
        ticker = tickerString;
        if (NativeCanvas.isFullScreen()) {
            invalidate();
        }
    }
    
    protected int getCapHeight() {
        captionHeight = Math.max(captionHeight, GraphicsEx.calcCaptionHeight(capImages, caption));
        return captionHeight;
    }

    protected boolean isItemSelected(int index) {
        return (currItem == index);
    }

    protected void paint(GraphicsEx g) {
        int captionHeight = getCapHeight();
        g.drawCaption(capImages, (null == ticker) ? caption : ticker, messageIcon, captionHeight);
        int scrollPage = getVisCount();
        int scrollLenght = getSize();
        drawItems(g, captionHeight);
        g.setClip(getWidth() - scrollerWidth, captionHeight,
                scrollerWidth, getHeight() - captionHeight);
        g.drawVertScroll(getWidth() - scrollerWidth, captionHeight,
                scrollerWidth, getHeight() - captionHeight,
                topItem, scrollPage, scrollLenght,
                THEME_BACKGROUND, THEME_SCROLL_BACK);
    }

    private void drawItems(GraphicsEx g, int top_y) {
        int height = getHeight();
        int size = getSize();
        int itemWidth = getWidth() - scrollerWidth;
        
        // Fill background
        g.setThemeColor(THEME_BACKGROUND);
        g.fillRect(0, top_y, itemWidth, height - top_y);

        g.setClip(0, top_y, itemWidth, height - top_y);
        if (null != Scheme.backImage) {
            g.drawImage(Scheme.backImage, itemWidth / 2, (height + top_y) / 2,
                    Graphics.HCENTER | Graphics.VCENTER);
        }
        
        int grCursorY1 = -1;
        int grCursorY2 = -1; 
        // Draw cursor
        int y = top_y;
        for (int i = topItem; i < size; ++i) {
            int itemHeight = getItemHeight(i);
            if (isItemSelected(i)) {
                if (grCursorY1 == -1) grCursorY1 = y;
                grCursorY2 = y + itemHeight - 1;
            }
            y += itemHeight;
            if (y >= height) break;
        }
        
        if ((grCursorY1 != -1) && Scheme.fillCursor) {
            if ((topItem >= 1) && isItemSelected(topItem - 1)) {
                grCursorY1 -= 10;
            }
            g.setStrokeStyle(Graphics.SOLID);
            g.setThemeColor(THEME_SELECTION_BACK);
            g.fillRect(0, grCursorY1, itemWidth - 1, grCursorY2 - grCursorY1);
        }

        // Draw items
        y = top_y;
        for (int i = topItem; i < size; ++i) {
            int itemHeight = getItemHeight(i);
            g.setStrokeStyle(Graphics.SOLID);
            drawItemData(g, i, 2, y, itemWidth - 2, y + itemHeight);
            y += itemHeight;
            if (y >= height) break;
        }

        if (grCursorY1 != -1) {
            g.setThemeColor(THEME_SELECTION_RECT);
            g.setStrokeStyle(Scheme.fillCursor ? Graphics.SOLID : Graphics.DOTTED);
            if (!((topItem >= 1) && isItemSelected(topItem - 1))) {
                g.drawLine(1, grCursorY1, itemWidth - 2, grCursorY1);
            }
            g.drawLine(0, grCursorY1 + 1, 0, grCursorY2 - 1);
            g.drawLine(itemWidth - 1, grCursorY1 + 1, itemWidth - 1, grCursorY2 - 1);
            g.drawLine(1, grCursorY2, itemWidth-2, grCursorY2);
        }
    }

    protected abstract void drawItemData(GraphicsEx g, int index,
            int x1, int y1, int x2, int y2);
    
    protected int getHeight() {
        return NativeCanvas.getScreenHeight();
    }
    
    protected int getWidth() {
        return NativeCanvas.getScreenWidth();
    }
}