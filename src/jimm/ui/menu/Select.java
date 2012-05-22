/*
 * Select.java
 *
 * Created on 22 Июнь 2007 г., 15:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.menu;

import DrawControls.*;
import DrawControls.icons.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.modules.*;
import jimm.search.UserInfo;
import jimm.ui.*;
import jimm.ui.base.*;
import jimm.ui.timers.*;
import jimm.util.ResourceBundle;

/**
 * @author vladimir
 */
public final class Select extends CanvasEx {
    private static final int BORDER_Y = 1;
    private static final int BORDER_X = 2;
    private static final int ICON_INTERVAL = 2;
    private static final int WIDTH_SPACE = 6;

    private static final int SLEEP_BEFOR = 2000 / NativeCanvas.UIUPDATE_TIME;
    private static final int SLEEP_AFTER = 1000 / NativeCanvas.UIUPDATE_TIME;
    private static final int STEP = 2;
    private static final int EMPTY_WIDTH = 5 * STEP;
        
    private MenuModel items;
    private int topItem;
    private int selectedItemIndex;
    
    private int selectedItemPosX;
    private int sleep;
    
    private int left;
    private int top;
    private int width;
    private int height;

    private int itemHeight;
    private int itemWidth;
    private int textWidth;
    private int iconWidth;

    private int calcItemPerPage() {
        return Math.min(height / itemHeight, items.count());
    }

    // #sijapp cond.if modules_STYLUS is "true"#
    private int getItemStartY(int relativeY) {
        return topItem * itemHeight;
    }
    private int getItemByCoord(int relativeY) {
        final int size = items.count();
        for (int i = topItem; i < size; ++i) {
            if (relativeY < itemHeight) {
                return i;
            }
            relativeY -= itemHeight;
        }
        return -1;
    }

    private boolean checkRegion(int fromX, int fromY) {
        int relativeX = fromX - left;
        int relativeY = fromY - top;
        int itemPerPage = calcItemPerPage();
        int curHeight = itemPerPage * itemHeight;
        int curWidth = calcMenuWidth();
        return (relativeX >= 0) && (relativeX < curWidth)
                && (relativeY >= 0) && (relativeY < curHeight);
    }
    private boolean isItemsRegion(int absoluteX) {
        return (absoluteX - left) < itemWidth;
    }

    protected void stylusPressed(int toX, int toY) {
        if (!checkRegion(toX, toY)) {
            return;
        }
        int posX = toX - left;
        int posY = toY - top;
        TouchControl nat = NativeCanvas.getInstance().touchControl;

        if (isItemsRegion(toX)) { // menu items
            nat.prevTopY = getItemStartY(posY);
            nat.prevHeight = itemHeight;
            int cur = getItemByCoord(posY);
            if (-1 != cur) {
                setSelectedItemCode(items.getItemCodeByIndex(getIndex(cur, 0)));
            }
            return;
        }

        // scroll bar
        int visItemCount = calcItemPerPage();
        int height = visItemCount * itemHeight;
        int[] metrix = GraphicsEx.getVertScrollMetrix(height,
                topItem, visItemCount, items.count());
        if (null == metrix || metrix[1] < posY || posY < metrix[0]) {
            nat.scrollingOn = false;
            nat.tappingOn = false;
        } else {
            nat.tappingOn = false;
            nat.kineticOn = false;
            nat.prevTopY = metrix[0];
            nat.prevHeight = height - (metrix[1] - metrix[0]);
        }
    }
    private void setTop(int pos) {
        int top = Math.max(0, Math.min(pos, items.count() - calcItemPerPage()));

        boolean update = (top != topItem);
        topItem = top;
        if (update) {
            invalidate();
        }
    }

    protected void stylusGeneralMoved(int fromX, int fromY, int toX, int toY) {
        if (checkRegion(fromX, fromY)) {
            TouchControl nat = NativeCanvas.getInstance().touchControl;
            if (isItemsRegion(fromX)) {
                setTop((nat.prevTopY - toY + fromY + nat.prevHeight / 2) / nat.prevHeight);
            } else {
                setTop(nat.calcScrollPosition(items.count()));
            }
        }
    }
    
    protected void stylusTap(int x, int y, boolean longTap) {
        if (checkRegion(x, y)) {
            if (isItemsRegion(x)) {
                int posY = y - this.top;
                int cur = getItemByCoord(posY);
                if (-1 != cur) {
                    setSelectedItemCode(items.getItemCodeByIndex(getIndex(cur, 0)));
                    go(items.itemAt(cur).code);
                }
            }
        } else {
            back();
        }
    }
    // #sijapp cond.end#

    public Select(MenuModel menu) {
        setModel(menu);
    }

    protected void restoring() {
        NativeCanvas.setCommands("select", "back");
    }
    
    private void calcMetrix() {
        Font menuFont = GraphicsEx.menuFont;
        itemHeight = menuFont.getHeight();
        textWidth = 0;
        iconWidth = 0;

        final int size = items.count();
        for (int i = 0; i < size; ++i) {
            MenuItem item = items.itemAt(i);
            if (null == item.text) continue;
            Icon icon = item.icon;
            if (null != icon) {
                itemHeight = Math.max(itemHeight, icon.getHeight());
                iconWidth  = Math.max(iconWidth,  icon.getWidth());
            }
            textWidth = Math.max(textWidth, menuFont.stringWidth(item.text));
        }
        textWidth = Math.max(NativeCanvas.getScreenWidth() * 2 / 5, textWidth);
        itemHeight = Math.max(itemHeight + BORDER_Y, CanvasEx.minItemHeight);
        
        if (0 < iconWidth) {
            iconWidth = Math.max(iconWidth, itemHeight);
        }

        int _itemWidth = textWidth + iconWidth + ICON_INTERVAL + 2 * BORDER_X;
        itemWidth = between(_itemWidth,
                CanvasEx.minItemWidth,
                NativeCanvas.getScreenWidth() - (WIDTH_SPACE + scrollerWidth + 2 * BORDER_X));
    }
    private int calcMenuWidth() {
        final int size = items.count();
        int itemPerPage = calcItemPerPage();
        if (size > itemPerPage) {
            return itemWidth + scrollerWidth;
        }
        return itemWidth;
    }

    protected void showing() {
        selectedItemIndex = items.getDefaultItemIndex();
        selectedItemPosX = 0;
        sleep = 0;
    }
    private int between(int x, int min, int max) {
        return Math.max(min, Math.min(x, max));
    }
    protected void beforShow() {
        selectedItemPosX = 0;
        sleep = 0;
        calcMetrix();
        int screenHeight = NativeCanvas.getScreenHeight();
        int screenWidth = NativeCanvas.getScreenWidth();

        height = Math.min(screenHeight - 10, itemHeight * items.count());
        width  = between(calcMenuWidth(), screenWidth / 3, screenWidth - 2 * BORDER_X - 10);
        left = (screenWidth - width) / 2;
        top = (screenHeight - height) / 3;
        setSelectedItem(selectedItemIndex);
    }

    public void update() {
        beforShow();
        invalidate();
    }

    public final void setModel(MenuModel model) {
        items = model;
    }
    public int getSelectedItemCode() {
        return items.getItemCodeByIndex(selectedItemIndex);
    }
    private final void setSelectedItemCode(int itemCode) {
        if (getSelectedItemCode() != itemCode) {
            selectedItemIndex = items.getIndexByItemCode(itemCode);
            selectedItemPosX = 0;
            sleep = 0;
            invalidate();
        }
    }
        
    
    protected void paint(GraphicsEx g) {
        final int size = items.count();
        final int itemPerPage = calcItemPerPage();
        final boolean hasScroll = (size > itemPerPage);

        final int itemWidth = this.itemWidth;

        // get top item
        int curWidth = this.width;
        int curHeight = itemHeight * itemPerPage + BORDER_Y;
        int currentIndex = selectedItemIndex;
        int top = topItem;
        
        int y = this.top;
        int x = this.left;
        paintBack(g, prevDisplay);
        g.setStrokeStyle(Graphics.SOLID);
        g.fillRect(x, y, curWidth, curHeight, THEME_MENU_BACK);
        g.drawDoubleBorder(x, y, curWidth, curHeight, THEME_MENU_BORDER);
        g.setClip(x, y, curWidth + 1, curHeight + 1);
        if (hasScroll) {
            g.drawVertScroll(x + itemWidth, y, scrollerWidth + 1, curHeight,
                    top, itemPerPage, size, THEME_MENU_BACK, THEME_MENU_BORDER);
        }
        paintItems(g, x, y, itemPerPage, currentIndex);
    }
    
    private void paintItems(GraphicsEx g, int baseX, int baseY, int count, int currentIndex) {
        Font menuFont = GraphicsEx.menuFont;
        final int itemWidth = this.itemWidth;
        final int textWidth = itemWidth - (iconWidth + ICON_INTERVAL + 2 * BORDER_X);

        int iconX  = BORDER_X + baseX + iconWidth / 2;
        int iconY  = BORDER_Y + baseY + (itemHeight - BORDER_Y) / 2;
        int promtX = BORDER_X + baseX + iconWidth + ICON_INTERVAL;
        int promtY = BORDER_Y + baseY + (itemHeight - BORDER_Y) / 2 - menuFont.getHeight() / 2;

        g.setFont(menuFont);
        for (int i = topItem; count > 0; ++i, --count) {
            g.setClip(baseX, baseY, itemWidth + 1, itemHeight + 1);
            if (currentIndex == i) {
                g.setThemeColor(THEME_MENU_SEL_BACK);
                int capBkCOlor = g.getThemeColor(THEME_MENU_SEL_BACK);
                g.drawGradRect(capBkCOlor, g.transformColorLight(capBkCOlor, -32),
                        baseX, baseY, itemWidth - 1, itemHeight);
                g.setThemeColor(THEME_MENU_SEL_BORDER);
                g.drawRect(baseX, baseY, itemWidth - 1, itemHeight);
            }
            MenuItem item = items.itemAt(i);
            g.drawInCenter(item.icon, iconX, iconY);
            g.setClip(promtX, BORDER_Y + baseY - 1, textWidth, itemHeight - BORDER_Y + 2);
            if (null == item.text) {
                int posY = baseY + itemHeight / 2;
                int posX = baseX + itemWidth;
                g.setThemeColor(THEME_MENU_BORDER);
                g.drawLine(baseX + 2, posY, baseX + itemWidth - 4, posY);
                g.drawLine(baseX + 3, posY + 1, baseX + itemWidth - 3, posY + 1);

            } else if (currentIndex == i) {
                g.setThemeColor(THEME_MENU_SEL_TEXT);
                g.drawString(item.text, promtX - selectedItemPosX, promtY, Graphics.TOP | Graphics.LEFT);

            } else {
                g.setThemeColor(THEME_MENU_TEXT);
                g.drawString(item.text, promtX, promtY, Graphics.TOP | Graphics.LEFT);
            }
            baseY += itemHeight;
            iconY += itemHeight;
            promtY += itemHeight;
        }
    }
    private int getIndex(int currentIndex, int moveTo) {
        return Math.max(Math.min(currentIndex + moveTo, items.count() - 1), 0);
    }
    private void setSelectedItem(int index) {
        setSelectedItemCode(items.getItemCodeByIndex(getIndex(index, 0)));

        final int size = items.count();
        final int itemPerPage = calcItemPerPage();
        final boolean hasScroll = (size > itemPerPage);
        if (hasScroll) {
            int top = topItem;
            top = Math.max(top, getIndex(selectedItemIndex, 1 + 1 - itemPerPage));
            top = Math.min(top, getIndex(size, -itemPerPage));
            top = Math.min(top, getIndex(selectedItemIndex, -1));
            topItem = top;
        } else {
            topItem = 0;
        }

        invalidate();
    }
    private void nextPrevItem(boolean next) {
        final int size = items.count();
        setSelectedItem((selectedItemIndex + (next ? 1 : size - 1)) % size);
    }


    protected void doKeyReaction(int keyCode, int gameAct, int type) {
        if (KEY_RELEASED == type) return;
        switch (gameAct) {
            case NativeCanvas.NAVIKEY_DOWN:
            case NativeCanvas.NAVIKEY_UP:
                nextPrevItem(NativeCanvas.NAVIKEY_DOWN == gameAct);
                return;
        }
        if (KEY_PRESSED != type) return;
        switch (keyCode) {
            case NativeCanvas.LEFT_SOFT:
                go(getSelectedItemCode());
                return;
            case NativeCanvas.RIGHT_SOFT:
            case NativeCanvas.CLOSE_KEY:
            case NativeCanvas.CLEAR_KEY:
                back();
                return;
            case NativeCanvas.KEY_NUM1:
                setSelectedItem(0);
                return;
            case NativeCanvas.KEY_NUM7:
                setSelectedItem(items.count() - 1);
                return;
            case NativeCanvas.KEY_NUM3:
            case NativeCanvas.KEY_NUM9:
                int item = calcItemPerPage();
                if (NativeCanvas.KEY_NUM3 == keyCode) {
                    item = -item;
                }
                setSelectedItem(selectedItemIndex + item);
                return;
        }
		switch (gameAct) {
            case NativeCanvas.NAVIKEY_RIGHT: // ????
            case NativeCanvas.NAVIKEY_FIRE:
                go(getSelectedItemCode());
                break;
            case NativeCanvas.NAVIKEY_LEFT:
                back();
                break;
        }
	}

    protected void updateTask() {
        sleep++;
        if ((0 == selectedItemPosX) && (sleep < SLEEP_BEFOR)) return;
        Font menuFont = GraphicsEx.menuFont;
        int width = menuFont.stringWidth(items.itemAt(selectedItemIndex).text);
        int textWidth = itemWidth - (iconWidth + ICON_INTERVAL + 2 * BORDER_X);
        if (width <= textWidth) return;

        if (selectedItemPosX + textWidth > (width + EMPTY_WIDTH)) {
            if (sleep < SLEEP_AFTER) return;
            selectedItemPosX = 0;
        } else {
            selectedItemPosX += STEP;
        }
        sleep = 0;
        invalidate();
    }

    private void go(int code) {
        items.exec(this, code);
    }
}