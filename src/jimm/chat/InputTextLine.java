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
 File: this
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Vladimir Kryukov
 *******************************************************************************/

package jimm.chat;
import DrawControls.text.*;
import javax.microedition.lcdui.*;
import jimm.comm.StringConvertor;
import jimm.modules.*;
import jimm.ui.*;
import jimm.ui.base.*;

/**
 *
 * @author Vladimir Krukov
 */
public class InputTextLine implements ActionListener {
    private boolean qwerty = false;
    private int height = 0;
    private int width = 0;
    private int y = 0;
    private int topLine = 0;
    private final int LINES = 2;
    private StringBuffer content = new StringBuffer();
    private FormatedText text = new FormatedText();
    private boolean visible = false;
    private boolean upperCase = false;

    public void setVisible(boolean v) {
        visible = v;
    }
    public boolean isVisible() {
        return visible;
    }

    // phone keyboard: keys
    // qwerty keyboard: shift en ru
    public InputTextLine() {
        // NokiaE63
        String platform = StringConvertor.notNull(jimm.Jimm.microeditionPlatform);
        qwerty = (-1 != platform.toLowerCase().indexOf("nokiae63"));
    }
    public int getHeight() {
        return visible ? height : 0;
    }

    public int getRealHeight() {
        Font font = GraphicsEx.chatFontSet[Font.STYLE_PLAIN];
        return font.getHeight() * LINES + 2;
    }
    public void setSize(int y, int width, int height) {
        this.y = y;
        this.width = width;
        this.height = getRealHeight();
    }

    public void paint(Graphics g) {
        if (!visible) {
            return;
        }
        Font font = GraphicsEx.chatFontSet[Font.STYLE_PLAIN];
        g.setStrokeStyle(Graphics.SOLID);
        g.setFont(font);

        final int modeWidth = font.charWidth('#') * 4;
        g.setColor(0xFFFFFF);
        g.setClip(width - modeWidth, y - font.getHeight(), modeWidth, font.getHeight());
        int d = Math.max(4, font.getHeight() * 30 / 100);
        g.fillRoundRect(width - modeWidth, y - font.getHeight(), modeWidth + d, font.getHeight() + d, d, d);
        g.setColor(0x000000);
        g.drawRoundRect(width - modeWidth, y - font.getHeight(), modeWidth + d, font.getHeight() + d, d, d);
        g.setColor(0x404040);
        g.drawString(modeStrings[mode], width - modeWidth + d, y - font.getHeight(), Graphics.TOP | Graphics.LEFT);

        g.setClip(0, y, width, height);
        g.setColor(0xFFFFFF);
        g.fillRect(0, y, width, height);
        g.setColor(0x000000);
        g.drawRect(0, y, width, height);
        g.setColor(0xD0D0D0);
        g.drawLine(3, y + font.getHeight() * 1 + 1, width - 6, y + font.getHeight() * 1 + 1);
        g.drawLine(3, y + font.getHeight() * 2 + 1, width - 6, y + font.getHeight() * 2 + 1);
        g.setColor(0x000000);

        final int OFFSET = 5;
        g.setColor(0x000000);
        boolean editableLetter = false && (System.currentTimeMillis() <= lastPressTime + KEY_TIMEOUT);
        for (int lineIndex = 0; lineIndex < LINES; ++lineIndex) {
            String line = "";
            if (topLine + lineIndex < text.getSize()) {
                line = " ";
                if (0 != text.getLine(topLine + lineIndex).items.size()) {
                    line = text.getLine(topLine + lineIndex).elementAt(0).text;
                }
            }
            int offset = OFFSET;
            int y = this.y + font.getHeight() * lineIndex;
            int stopPosition = line.length();
            int cursorPos = ((topLine + lineIndex == cursorLine) ? cursorChar : -1) - 1;
            int cursorOffset = offset;
            for (int i = 0; i < stopPosition; ++i) {
                char ch = line.charAt(i);
                int chWidth = font.charWidth(ch);
                if ((i == cursorPos) && editableLetter) {
                    g.setColor(0x0000FF);
                }
                g.drawChar(ch, offset, y + 1, Graphics.TOP | Graphics.LEFT);
                offset += chWidth;
                if (i == cursorPos) {
                    cursorOffset = offset;
                    g.setColor(0x000000);
                }

            }
            if (stopPosition <= cursorPos) {
                cursorOffset = offset + font.charWidth(' ');
            }
            if (-1 <= cursorPos) {
                g.setColor(0x0000FF);
                g.fillRect(cursorOffset + 1, y + 2, 2, font.getHeight() - 2);
                g.setColor(0x000000);
            }
        }
    }
    
    private int key = -1;

    private int pressCount = 0;
    private long lastPressTime = 0;
    private int cursor = 0;
    private int cursorLine = 0;
    private int cursorChar = 0;
    private void setCursor(int pos) {
        cursor = pos;
        cursorLine = 0;
        cursorChar = 0;
        int cursorPos = cursor;
        for (int lineIndex = 0; lineIndex < text.getSize(); ++lineIndex) {
            int len = 0;
            if (1 == text.getLine(lineIndex).items.size()) {
                len = text.getLine(lineIndex).elementAt(0).text.length();
            }
            if ('\0' != text.getLine(lineIndex).last_charaster) {
                len++;
            }
            if (cursorPos < len + 1) {
                cursorChar = cursorPos;
                if ((cursorPos == len) && ('\n' == text.getLine(lineIndex).last_charaster)) {
                    cursorLine++;
                    cursorChar = 0;
                }
                break;
            }
            cursorPos -= len;
            cursorLine++;
        }
        topLine = Math.max(topLine, Math.max(cursorLine - LINES + 1, 0));
        topLine = Math.min(topLine, Math.min(cursorLine,
                Math.max(0, text.getSize() - LINES)));
    }
    
    
    private static final int keySmile = Canvas.KEY_STAR;
    private static final int keyDelete = NativeCanvas.RIGHT_SOFT;
    
    // Russian Digit Latin
    private static final String[] numChars = {" 0\n", // 0
            ".,?!1@'\"-_():;", // 1
            "\u0430\u0431\u0432\u04332abc", // 2
            "\u0434\u0435\u0436\u04373def\u0451", // 3
            "\u0438\u0439\u043a\u043b4ghi", // 4
            "\u043c\u043d\u043e\u043f5jkl", // 5
            "\u0440\u0441\u0442\u04436mno", // 6
            "\u0444\u0445\u0446\u04477pqrs", // 7
            "\u0448\u0449\u044a\u044b8tuv", // 8
            "\u044c\u044d\u044e\u044f9wxyz" // 9
            };
    private static final byte MODE_L = 0; // abc
    private static final byte MODE_CAPS = 1; // Abc
    private byte mode = MODE_L;
    private int charNum = 0;
    private static final String[] modeStrings = {"abc", "Abc"};

    public void action(CanvasEx canvas, int cmd) {
        // #sijapp cond.if modules_SMILES is "true" #
        if (canvas instanceof Selector) {
            insert(" " + ((Selector)canvas).getSelectedCode() + " ");
        }
        // #sijapp cond.end#
    }

    private void resetCurrentChar() {
        lastPressTime = 0;
        key = -1;
        pressCount = 0;
    }
    private void insert(String text) {
        resetCurrentChar();
        if (cursor < content.length()) {
            content.insert(cursor, text);
        } else {
            setCursor(content.length());
            content.append(text);
        }
        updateUI();
        setCursor(cursor + text.length());
    }
    private void addChar(char ch) {
        if (cursor < content.length()) {
            content.insert(cursor, ch);
        } else {
            setCursor(content.length());
            content.append(ch);
        }
        updateUI();
        setCursor(cursor + 1);
    }
    private void updateUI() {
        text.clear();
        text.setWidth(width - 5 * 2);
        text.addBigText(GraphicsEx.chatFontSet, content.toString(), (byte)0, (byte)0, -1);
    }
    

    private char getCurrentChar() {
        char ch = numChars[key].charAt(pressCount);
        if (MODE_CAPS == mode) {
            ch = StringConvertor.toUpperCase(ch);
        }
        return ch;
    }
    private void setMode(byte mode) {
        this.mode = mode;
        charNum = 0;
    }
    private final static long KEY_TIMEOUT = 600;
    private void autoMode() {
        if (0 == content.length()) {
            setMode(MODE_CAPS);
            return;
        }
        if ((1 < cursor) && (-1 != " ".indexOf(content.charAt(cursor - 1)))
                && (-1 != ".!?".indexOf(content.charAt(cursor - 2)))) {
            setMode(MODE_CAPS);
            
        } else if (1 == charNum) {
            setMode(MODE_L);
        }
    }
    private boolean naviKeys(int actionCode) {
        if (NativeCanvas.NAVIKEY_LEFT == actionCode) {
            setCursor(cursor - 1);
            if (-1 == cursor) {
                setCursor(content.length());
            }
            resetCurrentChar();
            return true;
        }
        if (NativeCanvas.NAVIKEY_RIGHT == actionCode) {
            setCursor(cursor + 1);
            if (content.length() < cursor) {
                setCursor(0);
            }
            resetCurrentChar();
            return true;
        }
        return false;
    }
    
    private boolean qwertyKey(ChatTextList chat, int keyCode, int actionCode, int type) {
        if (sysKeys(chat, keyCode, actionCode, type)) {
            return true;
        }
        if ((NativeCanvas.ABC_KEY == keyCode) && (CanvasEx.KEY_REPEATED != type)) {// shift (-50)
            upperCase = (CanvasEx.KEY_PRESSED == type);
        }
        if (CanvasEx.KEY_PRESSED != type) {
            return false;
        }
        if (((32 <= keyCode) && (keyCode < 256))
                || (61441 == keyCode) || ('\n' == keyCode)) {
            char ch = (char)keyCode;
            if (upperCase) {
                ch = StringConvertor.toUpperCase(ch);
            }
            addChar(ch);
            return true;
        }
        // NokiaE63
        if (12 == keyCode) {// main menu
            setVisible(false);
            return true;
        }
        if (8 == keyCode) { // backspace
            if ((0 < content.length()) && (0 < cursor)) {
                setCursor(cursor - 1);
                content.deleteCharAt(cursor);
                updateUI();
            }
            autoMode();
        }
        return naviKeys(actionCode);
    }

    private boolean phoneKeyPressed(int keyCode, int actionCode) {
        final long currentTime = System.currentTimeMillis();
        boolean nextChar = (0 == lastPressTime) || (lastPressTime + KEY_TIMEOUT < currentTime);
        if ((Canvas.KEY_NUM0 <= keyCode) && (keyCode <= Canvas.KEY_NUM9)) {
            final int keyIndex = keyCode - Canvas.KEY_NUM0;
            if ((key != keyIndex) || nextChar) {
                resetCurrentChar();
                autoMode();
                charNum++;
                
                key = keyIndex;
                pressCount = 0;
                addChar(getCurrentChar());

            } else if (0 < cursor) {
                pressCount = (pressCount + 1) % numChars[key].length();
                content.setCharAt(cursor - 1, getCurrentChar());
                updateUI();
            }
            
            lastPressTime = currentTime;
            return true;
        }
        lastPressTime = 0;
        if (keySmile == keyCode) {
            // #sijapp cond.if modules_SMILES is "true" #
            Emotions.selectEmotion(this);
            // #sijapp cond.end #
            return true;
        }

        if (keyDelete == keyCode) {
            keyCode = NativeCanvas.CLEAR_KEY;
        }
        if (NativeCanvas.CLEAR_KEY == keyCode) {
            resetCurrentChar();
            if ((0 < content.length()) && (0 < cursor)) {
                setCursor(cursor - 1);
                content.deleteCharAt(cursor);
                updateUI();
            }
            autoMode();
            return true;
        }
        if (NativeCanvas.NAVIKEY_FIRE == actionCode) {
            return false;
        }
        if (!nextChar) {
            autoMode();
            return true;
        }
        return naviKeys(actionCode);
    }
    private boolean phoneKey(ChatTextList chat, int keyCode, int actionCode, int type) {
        if (Canvas.KEY_POUND == keyCode) {
            if (CanvasEx.KEY_PRESSED == type) {
                setMode((byte) ((mode + 1) % 3));
                chat.invalidate();
            }
            return true;
        }
        if ((CanvasEx.KEY_PRESSED == type) || (CanvasEx.KEY_REPEATED == type)) {
            if (phoneKeyPressed(keyCode, actionCode)) {
                chat.invalidate();
                return true;
            }
        }
        return sysKeys(chat, keyCode, actionCode, type);
    }
    
    
    private boolean sysKeys(ChatTextList chat, int keyCode, int actionCode, int type) {
        if (CanvasEx.KEY_PRESSED == type) {
            final String text = getString();
            final String address = ChatTextList.ADDRESS + " ";
            switch (keyCode) {
                case NativeCanvas.NAVIKEY_FIRE:
                    if (!text.endsWith(address)) {
                        chat.getContact().sendMessage(text, true);
                    }
                    setString("");
                    chat.invalidate();
                    setVisible(false);
                    return true;

                case NativeCanvas.CAMERA_KEY:
                case NativeCanvas.CALL_KEY:
                    chat.itemSelected();
                    return true;
            }
        }
        return false;
    }
    public boolean doKeyReaction(ChatTextList chat, int keyCode, int actionCode, int type) {
        if (!visible) {
            return false;
        }
        if (qwerty) {
            boolean result = qwertyKey(chat, keyCode, actionCode, type);
            if (result) {
                chat.invalidate();
            }
            return result;
        }
        return phoneKey(chat, keyCode, actionCode, type);
    }

    public String getString() {
        return content.toString();
    }
    public void setString(String str) {
        content = new StringBuffer(StringConvertor.notNull(str));
        autoMode();
        updateUI();
        setCursor(content.length());
    }
}
