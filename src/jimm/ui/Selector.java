package jimm.ui;

import DrawControls.icons.*;
import java.util.*;
import javax.microedition.lcdui.*;
import java.io.*;
import jimm.*;
import jimm.comm.*;
import jimm.ui.base.*;
import jimm.util.*;
import DrawControls.*;


public final class Selector extends VirtualList {
    
    private String[] names;
    private ImageList icons;
    private String[] codes;
    
    private ActionListener listener;
    private int cols;
    private int rows;
    private int itemHeight;
    private int curCol;
    
    public Selector(ImageList icons, String[] names, String[] codes) {
        super(null);
        this.icons = icons;
        this.names = names;
        this.codes = codes;
                
        int drawWidth = getWidth() - scrollerWidth - 2;
        // #sijapp cond.if modules_STYLUS is "true"#
        if (NativeCanvas.getInstance().hasPointerEvents()) {
            drawWidth = getWidth() - 2 * scrollerWidth;
        }
        // #sijapp cond.end#
        
        int heightSmall = Math.max(CanvasEx.minItemHeight, icons.getHeight() + 2);
        int moduloSmall = drawWidth % heightSmall;
        itemHeight = heightSmall + (moduloSmall * heightSmall / drawWidth);
        
        cols = drawWidth / itemHeight;
        rows = (names.length + cols - 1) / cols;
        setCurrentItem(0, 0);
    }
    public final void setSelectionListener(ActionListener listener) {
        this.listener = listener;
    }
    
    // #sijapp cond.if modules_STYLUS is "true"#
    protected final boolean pointerPressed(int item, int x, int y) {
        final int curCol = Math.min(Math.max(x / itemHeight, 0), cols - 1);
        final int curRow = item;
        if ((getCurrentCol() != curCol) || (getCurrentRow() != curRow)) {
            setCurrentItem(curCol, curRow);
            return true;
        }
        return false;
    }
    // #sijapp cond.end#
    
    protected final boolean isItemSelected(int index) {
        return false;
    }

//    public void restore() {
//        setCurrentItem(0, 0);
//        super.restore();
//    }
    
    protected void drawItemData(GraphicsEx g, int index,
            int x1, int y1, int x2, int y2) {
        int xa = x1;
        int xb;
        int startIdx = cols * index;
        int imagesCount = icons.size();
        boolean isSelected = (getCurrentRow() == index);
        for (int i = 0; i < cols; i++, startIdx++) {
            if (startIdx >= names.length) break;
            int smileIdx = startIdx;
            
            xb = xa + itemHeight;
            
            if (isSelected && (i == getCurrentCol())) {
                if (Scheme.fillCursor) {
                    g.setStrokeStyle(Graphics.SOLID);
                    g.setThemeColor(THEME_SELECTION_BACK);
                    g.getGraphics().fillRoundRect(xa, y1, itemHeight - 1, y2 - y1 - 1, 4, 4);
                } else {
                    int capBkCOlor = g.getThemeColor(THEME_CAP_BACKGROUND);
                    g.drawGradRect(capBkCOlor, g.transformColorLight(capBkCOlor, -32), xa, y1, itemHeight - 1, y2 - y1 - 1);
                }
            }
            
            if (smileIdx < imagesCount) {
                int centerX = xa + itemHeight / 2;
                int centerY = (y1 + y2) / 2;
                g.drawInCenter(icons.iconAt(smileIdx), centerX, centerY);
            }
            
            if (isSelected && (i == getCurrentCol())) {
                if (Scheme.fillCursor) {
                    g.setStrokeStyle(Graphics.SOLID);
                    g.setThemeColor(THEME_SELECTION_RECT);
                    g.getGraphics().drawRoundRect(xa, y1, itemHeight - 1, y2 - y1 - 1, 4, 4);
                } else {
                    g.setThemeColor(THEME_SELECTION_RECT);
                    g.setStrokeStyle(Graphics.DOTTED);
                    g.drawRect(xa, y1, itemHeight - 1, y2 - y1 - 1);
                }
            }
            xa = xb;
        }
    }
    
    public final int getSelectedIndex() {
        return getCurrentRow() * cols + getCurrentCol();
    }
    public final String getSelectedCode() {
        return codes[getSelectedIndex()];
    }

    private final int getCurrentRow() {
        return getCurrItem();
    }
    private final int getCurrentCol() {
        return curCol;
    }
    private final void setCurrentItem(int col, int row) {
        setCurrentItem(row);
        curCol = col;
        setCurrentItemToCaption();
        invalidate();
    }
    
    private final void setCurrentItemToCaption() {
        int selIdx = getCurrentRow() * cols + getCurrentCol();
        if (names.length <= selIdx) return;
        setCaption(names[selIdx]);
    }

    protected final int getItemHeight(int itemIndex) {
        return itemHeight;
    }
    
    protected final int getSize() {
        return rows;
    }
    
    protected void doKeyReaction(int keyCode, int actionCode, int type) {
        if (CanvasEx.KEY_RELEASED == type) {
            return;
        }
        switch (keyCode) {
            case NativeCanvas.LEFT_SOFT:
                select();
                return;
                
            case NativeCanvas.RIGHT_SOFT:
            case NativeCanvas.CLOSE_KEY:
                back();
                return;
        }
        int newRow = getCurrentRow();
        int newCol = getCurrentCol();
        final int rowCount = getSize();
        int index;
        switch (actionCode) {
            case NativeCanvas.NAVIKEY_FIRE:
                select();
                return;
                
            case NativeCanvas.NAVIKEY_DOWN:
                newRow++;
                index = newCol + newRow * cols;
                if (index >= names.length) {
                    newRow = 0;
                    newCol = (newCol < (cols - 1)) ? newCol + 1 : 0;
                }
                break;
                
            case NativeCanvas.NAVIKEY_UP:
                newRow--;
                if (newRow < 0) {
                    newRow = rowCount - 1;
                    newCol = ((newCol == 0) ? cols : newCol) - 1;
                }
                break;
                
            case NativeCanvas.NAVIKEY_LEFT:
                if (newCol != 0) {
                    newCol--;
                } else {
                    newCol = cols - 1;
                    newRow--;
                }
                if (newRow < 0) {
                    newCol = (names.length - 1) % cols;
                    newRow = rowCount;
                }
                break;
                
            case NativeCanvas.NAVIKEY_RIGHT:
                if (newCol < (cols - 1)) {
                    newCol++;
                } else {
                    newCol = 0;
                    newRow++;
                }
                index = newCol + newRow * cols;
                if (index >= names.length) {
                    newCol = 0;
                    newRow = 0;
                }
                break;
                
            default:
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
                        
                    default:
                        return;
                }
                newRow = getCurrentRow();
        }
        
        index = newCol + newRow * cols;
        if (names.length <= index) {
            newRow--;
        }
        setCurrentItem(newCol, newRow);
    }

    protected void itemSelected() {
        select();
    }
    private void select() {
        if (null != listener) {
    	    back();
    	    listener.action(this, 0);
        }
        listener = null;
    }
    
    protected void restoring() {
        NativeCanvas.setCommands("select", "cancel");
    }
}