package DrawControls.text;

import DrawControls.*;
import DrawControls.icons.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.modules.*;
import jimm.ui.base.*;
/*
 * FormatedText.java
 *
 * Created on 25 Октябрь 2008 г., 16:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

/**
 *
 * @author Vladimir Krukov
 */
public final class FormatedText {
	// Vector of lines. Each line contains cols. Col can be text or image
	private Vector lines = new Vector();

    private int width;
    public void setWidth(int w) {
        width = w;
    }
    private int getWidth() {
        return width;
    }
    public int getSize() {
        return lines.size();
	}

    public TextLine getLine(int index) {
		return (TextLine)lines.elementAt(index);
	}
	public void clear() {
		lines.removeAllElements();
	}
    public int getTextIndex(int lineIndex) {
		if ((getSize() <= lineIndex) || (lineIndex < 0)) {
            return -1;
        }
		return getLine(lineIndex).bigTextIndex;
    }
    public void remove(int textIndex) {
        for (int i = lines.size() - 1; i >= 0; --i) {
            TextLine line = (TextLine)lines.elementAt(i);
            if (textIndex == line.bigTextIndex) {
                lines.removeElementAt(i);
            }
        }
    }
    public void removeFirstText() {
        if (lines.isEmpty()) {
            return;
        }
        final int textIndex = getLine(0).bigTextIndex;
        while (!lines.isEmpty() && (textIndex == getLine(0).bigTextIndex)) {
            lines.removeElementAt(0);
        }
    }

	private void internAdd(Font font, String  text, byte colorType, byte fontStyle,
            int textIndex, boolean doCRLF, char last_charaster) {
		ListItem newItem = new ListItem(text, colorType, fontStyle);

		if (lines.isEmpty()) {
            lines.addElement(new TextLine());
        }
		TextLine textLine = (TextLine)lines.lastElement();
		textLine.addElement(newItem, font);
		textLine.bigTextIndex = textIndex;
		if (doCRLF) {
			textLine.last_charaster = last_charaster;
			TextLine newLine = new TextLine();
			newLine.bigTextIndex = textIndex;
			lines.addElement(newLine);
		}
	}
    private void internNewLine(int textIndex) {
        TextLine newLine = new TextLine();
        newLine.bigTextIndex = textIndex;
        lines.addElement(newLine);
    }

    public void doCRLF(int blockTextIndex) {
		if (lines.size() != 0) {
            ((TextLine)lines.lastElement()).last_charaster = '\n';
        }
		TextLine newLine = new TextLine();
		newLine.bigTextIndex = blockTextIndex; 
		lines.addElement(newLine);
	}

	public ListItem addImage(Icon image, int blockTextIndex) {
        return addImage(image, null, image.getWidth() + 2, image.getHeight(), blockTextIndex);
    }
	public ListItem addImage(Icon image, String altarnateText, int imageWidth, int imageHeight, int blockTextIndex) {
		if (lines.isEmpty()) lines.addElement(new TextLine());
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.bigTextIndex = blockTextIndex; 
		
		if ((textLine.getWidth() + imageWidth) > getWidth()) {
			doCRLF(blockTextIndex);
			textLine = (TextLine) lines.lastElement();
		}
		ListItem img = new ListItem(image, altarnateText, imageWidth, imageHeight);
		textLine.addElement(img, null);
        return img;
	}

    private void addImage(ListItem imageItem, int blockTextIndex) {
		if (lines.isEmpty()) {
            lines.addElement(new TextLine());
        }
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.bigTextIndex = blockTextIndex; 
		
		if ((textLine.getWidth() + imageItem.getWidth()) > getWidth()) {
			doCRLF(blockTextIndex);
			textLine = (TextLine) lines.lastElement();
		}
		
		textLine.addElement(imageItem, null);
	}
    private void internAdd(ListItem imageItem, int blockTextIndex) {
		if (lines.isEmpty()) {
            lines.addElement(new TextLine());
        }
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.bigTextIndex = blockTextIndex; 
		textLine.addElement(imageItem, null);
	}

    /**
     * Add big multiline text. 
     *
	 * Text visial width can be larger then screen width.
     * Method addBigText automatically divides text to short lines
     * and adds lines to text list
     */
	public void addBigText(Font[] fontSet, String text, byte colorType,
            byte fontStyle, int textIndex, boolean withEmotions) {
        text = StringConvertor.removeCr(text);

		Font font = fontSet[fontStyle];
		
		// Width of free space in last line 
        final int fullWidth = getWidth();
		int width = fullWidth;
        if (!lines.isEmpty()) {
            width -= ((TextLine)lines.lastElement()).getWidth();
        }
		int lastWordEnd = -1;
        
        // #sijapp cond.if modules_SMILES is "true" #
        int smileCount = 100;
        // #sijapp cond.end #
        int lineStart = 0;
        int wordStart = 0;
        int wordWidth = 0;
		int textLen = text.length();
        // #sijapp cond.if modules_SMILES is "true" #
        Emotions smiles = Emotions.instance;
        // #sijapp cond.end #
        for (int i = 0; i < textLen; ++i) {
            char ch = text.charAt(i);
            if ('\n' == ch) {
                String substr = text.substring(lineStart, i);
                internAdd(font, substr, colorType, fontStyle, textIndex, true, '\n');
                lineStart = i + 1;
                width = fullWidth;
                wordStart = lineStart;
                wordWidth = 0;
                continue;
            }
            
            // #sijapp cond.if modules_SMILES is "true" #
            int smileIndex = withEmotions ? smiles.getSmile(text, i) : -1;
            if (-1 != smileIndex) {
                wordStart = i;
                if (lineStart < wordStart) {
                    String substr = text.substring(lineStart, wordStart);
                    internAdd(font, substr, colorType, fontStyle, textIndex, (width <= 0), '\0');
                    if (width <= 0) {
                        width = fullWidth;
                    }
                }

                ListItem smileItem = smiles.getSmileItem(smileIndex);
                width -= smileItem.getWidth();
                if (width <= 0) {
                    internNewLine(textIndex);
                    width = fullWidth - smileItem.getWidth();
                }
                internAdd(smileItem, textIndex);

                i += smileItem.text.length() - 1;
                lineStart = i + 1;
                wordStart = lineStart;
                wordWidth = 0;
                
                smileCount--;
                if (0 == smileCount) {
                    withEmotions = false;
                }
                continue;
            }
            // #sijapp cond.end #
            
            int charWidth = font.charWidth(ch);

            wordWidth += charWidth;
            width -= charWidth;
            if (' ' == ch) {
                wordStart = i + 1;
                wordWidth = 0;
                continue;
            }

            if (width <= 0) {
                if (lineStart < wordStart) {
                    String substr = text.substring(lineStart, wordStart);
                    internAdd(font, substr, colorType, fontStyle, textIndex, true, '\0');
                    lineStart = wordStart;
                    width = fullWidth - wordWidth;
                    continue;

                } else if (wordWidth < fullWidth) {
                    internNewLine(textIndex);
                    width = fullWidth - wordWidth;
                    continue;

                } else {
                    String substr = text.substring(lineStart, i);
                    internAdd(font, substr, colorType, fontStyle, textIndex, true, '\0');
                    lineStart = i;
                    width = fullWidth - charWidth;
                    wordStart = i;
                    wordWidth = 0;
                    continue;
                }
            }
		}
        String substr = text.substring(lineStart);
        if (0 < substr.length()) {
            internAdd(font, substr, colorType, fontStyle, textIndex, false, '\0');
        }
	}

    public int getHeight() {
        int textHeight = 0;
		int linesCount = getSize();
		for (int line = 0; line < linesCount; ++line) {
            textHeight += getLine(line).getHeight();
        }
        return textHeight;
	}

	public void addBigText(Font[] fontSet, String text, byte colorType,
            byte fontStyle, int textIndex) {
        addBigText(fontSet, text, colorType, fontStyle, textIndex, false);
    }

    public void addTextWithEmotions(Font[] fontSet, String text,
            byte colorType, byte fontStyle, int textIndex) {
        // #sijapp cond.if modules_SMILES is "true" #
        if (jimm.Options.getBoolean(jimm.Options.OPTION_USE_SMILES)             //add
                && jimm.modules.Emotions.isSupported()) {                       //add
            addBigText(fontSet, text, colorType, fontStyle, textIndex, true);
            return;
        }
        // #sijapp cond.end #
        addBigText(fontSet, text, colorType, fontStyle, textIndex, false);
    }

    public void paint(Font[] fontSet, GraphicsEx g, int x, int y, int width, int height) {
		final int linesCount = getSize();
		int currentY = y;
		for (int lineIndex = 0; lineIndex < linesCount; lineIndex++) {
            TextLine line = getLine(lineIndex);
            
			line.paint(fontSet, x, currentY, g);
			currentY += line.getHeight();
		}
	}

    public void paint(Font[] fontSet, GraphicsEx g, int x, int y, int width, int height, int skipHeight) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.clipRect(x, y, width, height);

        final int linesCount = getSize();
        int lineIndex = 0;
		for (; (0 < skipHeight) && (lineIndex < linesCount); ++lineIndex) {
            skipHeight -= getLine(lineIndex).getHeight();
        }
		int currentY = y;
        if (0 != skipHeight) {
            lineIndex--;
            currentY -= skipHeight + getLine(lineIndex).getHeight();
        }

		for (; (0 < height) && (lineIndex < linesCount); ++lineIndex) {
            TextLine line = getLine(lineIndex);
            int lineHeight = line.getHeight();
            line.paint(fontSet, x, currentY, g);
            currentY += lineHeight;
            height -= lineHeight;
		}
        
        g.setClip(clipX, clipY, clipWidth, clipHeight);
	}
    
    public void paint(Font[] fontSet, GraphicsEx g, int x, int y, int width, int height, int skipHeight, int selectedIndex) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.clipRect(x, y, width, height);

        final int lineCount = getSize();
        int lineIndex = 0;
		for (; (0 < skipHeight) && (lineIndex < lineCount); ++lineIndex) {
            skipHeight -= getLine(lineIndex).getHeight();
        }
		int currentY = y;
        if (0 != skipHeight) {
            lineIndex--;
            currentY -= skipHeight + getLine(lineIndex).getHeight();
        }

        int grCursorY1 = -1;
        int grCursorY2 = -1;
        final int topIndex = lineIndex;
        // Draw cursor
        if (-1 != selectedIndex) {
            int yCursor = currentY;
            int endY = yCursor + height;
            for (int i = topIndex; i < lineCount; ++i) {
                TextLine line = getLine(i);
                int lineHeight = line.getHeight();
                if (line.bigTextIndex == selectedIndex) {
                    if (grCursorY1 == -1) grCursorY1 = yCursor;
                    grCursorY2 = yCursor + lineHeight - 1;
                }
                yCursor += lineHeight;
                if (yCursor >= endY) break;
            }
        }
        
        if ((-1 != grCursorY1) && Scheme.fillCursor) {
            if ((1 <= topIndex) && (getLine(topIndex - 1).bigTextIndex == selectedIndex)) {
                grCursorY1 -= 10;
            }
            g.setStrokeStyle(Graphics.SOLID);
            g.setThemeColor(CanvasEx.THEME_SELECTION_BACK);
            g.getGraphics().fillRoundRect(0, grCursorY1, width - 1, grCursorY2 - grCursorY1, 4, 4);
            g.setThemeColor(CanvasEx.THEME_SELECTION_RECT);
            g.getGraphics().drawRoundRect(0, grCursorY1, width - 1, grCursorY2 - grCursorY1, 4, 4);
        }

        for (; (0 < height) && (lineIndex < lineCount); ++lineIndex) {
            TextLine line = getLine(lineIndex);
            int lineHeight = line.getHeight();
            line.paint(fontSet, x, currentY, g);
            currentY += lineHeight;
            height -= lineHeight;
		}

        if ((-1 != grCursorY1) && !Scheme.fillCursor) {
            g.setStrokeStyle(Graphics.DOTTED);
            g.setThemeColor(CanvasEx.THEME_SELECTION_RECT);
            if (!((1 <= topIndex) && (getLine(topIndex - 1).bigTextIndex == selectedIndex))) {
                g.drawLine(1, grCursorY1, width - 2, grCursorY1);
            }
            g.drawLine(0, grCursorY1 + 1, 0, grCursorY2 - 1);
            g.drawLine(width - 1, grCursorY1 + 1, width - 1, grCursorY2 - 1);
            g.drawLine(1, grCursorY2, width-2, grCursorY2);
        }
        g.setClip(clipX, clipY, clipWidth, clipHeight);
	}

	// Returns lines of text which were added by 
	// methon addBigText in current selection
	public String getText(int textIndex, int offset, boolean wholeText) {
		int offsetCounter = 0;
		StringBuffer result = new StringBuffer();
		int currTextIndex = textIndex;

        // Fills the lines
		int size = getSize();
		for (int i = 0; i < size; i++) {
			TextLine line = getLine(i);
			if (wholeText || (line.bigTextIndex == currTextIndex)) {
				if (offset != offsetCounter) {
					offsetCounter++;
					continue;
				}
				int count = line.items.size();
				for (int k = 0; k < count; k++) {
                    String str = line.elementAt(k).text;
                    if (null != str) {
                        result.append(str);
                    }
				}
				if (line.last_charaster != '\0') {
					if (line.last_charaster == '\n') {
                        result.append("\n");
                    } else {
                        result.append(line.last_charaster);
                    }
				}
			}
		}
		String retval = result.toString().trim();
		return (retval.length() == 0) ? null : retval;
	}
}
