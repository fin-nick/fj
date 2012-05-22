/*
 * GraphicsEx.java
 *
 * Created on 15 Ноябрь 2007 г., 0:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import DrawControls.icons.*;
import javax.microedition.lcdui.*;
import jimm.Options;
import jimm.ui.*;
import jimm.comm.Util; //add for message icon

/**
 *
 * @author vladimir
 */
public final class GraphicsEx {
    private Graphics g;

    /** Creates a new instance of GraphicsEx */
    private final int softbarOffset;
    public GraphicsEx() {
        if (null != Scheme.softbarImage) {
            softbarOffset = Scheme.softbarImage.getHeight() / 3;
        } else {
            softbarOffset = 1;
        }
    }
    public void setGraphics(Graphics graphics) {
        g = graphics;
    }

    
    static private int[] theme = Scheme.getScheme();
    public void setThemeColor(byte object) {
        g.setColor(theme[object]);
    }
    public int getThemeColor(byte object) {
        return theme[object];
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    public  static Font[] chatFontSet;
    public  static Font[] contactListFontSet;
    public  static Font[] popupFontSet;
    public  static Font menuFont;
    public  static Font statusLineFont;
    private static Font captionFont;
    private static Font softBarFont;
    
    private static Font[] createFontSet(int fontSize) {
        Font[] fontSet = new Font[2];
        fontSet[Font.STYLE_PLAIN] = createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,  fontSize); 
        fontSet[Font.STYLE_BOLD]  = createFont(Font.FACE_SYSTEM, Font.STYLE_BOLD,   fontSize);
        return fontSet;
    }

    private static final Font createPlainFont(int size) {
        return createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, size);
    }
    private static final Font createFont(int face, int style, int size) {
        return Font.getFont(face, style, size);
    }
    
    private static int size2font(int size) {
        switch (Math.max(0, Math.min(size, 3))) {
            case 0: return Font.SIZE_SMALL;
            case 1: return Font.SIZE_MEDIUM;
            case 2: return Font.SIZE_LARGE;
        }
        return Font.SIZE_SMALL;
    }
    private static int num2size(int num, boolean chat) {
        switch (num) {
            case 0: return 0;
            case 1: return chat ? 0 : 1;
            case 2: return 1;
            case 3: return 1;
            case 4: return 2;
        }
        return 0;
    }
    public static void setFontScheme(int num) {
        int[] sizes = new int[7];
        sizes[0] = size2font(num2size(num, true));
        sizes[1] = size2font(num2size(num, false));
        sizes[2] = size2font(num2size(num, true));
        sizes[3] = size2font(num2size(num, false) - 1);
        int systemSize = (num < 3) ? 0 : 1;
        sizes[4] = size2font(systemSize);
        sizes[5] = size2font(systemSize);
        sizes[6] = size2font(systemSize);

        chatFontSet        = createFontSet(sizes[0]);
        contactListFontSet = createFontSet(sizes[1]);
        popupFontSet       = createFontSet(sizes[2]);

        statusLineFont = createPlainFont(sizes[3]);
        menuFont       = createPlainFont(sizes[4]);
        captionFont    = createFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, sizes[5]);
        softBarFont    = createFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, sizes[6]); //font softbar
    }
    ////////////////////////////////////////////////////////////////////////////

    
    // change light of color
    public int transformColorLight(int color, int light) {
        int r = (color & 0xFF) + light;
        int g = ((color & 0xFF00) >> 8) + light;
        int b = ((color & 0xFF0000) >> 16) + light;
        r = Math.min(Math.max(r, 0), 255);
        g = Math.min(Math.max(g, 0), 255);
        b = Math.min(Math.max(b, 0), 255);
        return r | (g << 8) | (b << 16);
    }
    
    public void drawString(String str, int x, int y, int width, int height) {
        if (null == str) {
            return;
        }
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);
        g.drawString(str, x, y + height, Graphics.BOTTOM + Graphics.LEFT);
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    public void drawBorderedString(String str, int x, int y, int width, int height) {
        final int color = g.getColor();
        final int cr = 255 - (color >> 16) & 0xFF;
        final int cg = 255 - (color >> 8) & 0xFF;
        final int cb = 255 - (color >> 0) & 0xFF;
        final int inversedColor = (cr << 16) | (cg << 8) | (cb << 0);
        g.setColor(inversedColor);
        drawString(str, x - 1, y, width, height);
        drawString(str, x + 1, y, width, height);
        drawString(str, x, y - 1, width, height);
        drawString(str, x, y + 1, width, height);
        g.setColor(color);
        drawString(str, x, y, width, height);
    }
    public void drawImage(Image image, int x, int y, int width, int height) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);
        if (width < image.getWidth()) {
            g.drawImage(image, x + (width - image.getWidth()) / 2, y, Graphics.TOP | Graphics.LEFT);
        } else {
            for (int offsetX = 0; offsetX < width; offsetX += image.getWidth()) {
                g.drawImage(image, x + offsetX, y, Graphics.TOP | Graphics.LEFT);
            }
        }
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    public void drawString(Icon[] lIcons, String str, Icon[] rIcons, int x, int y, int width, int height) {
        if (null == str) {
            return;
        }

        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);

        int lWidth = drawImages(lIcons, x, y, height);
        if (lWidth > 0) {
            lWidth++;
        }
        int rWidth = getImagesWidth(rIcons);
        if (rWidth > 0) {
            rWidth++;
        }
        drawImages(rIcons, x + width - rWidth, y, height);
        g.setClip(x + lWidth, y, width - lWidth - rWidth, height);
        g.drawString(str, x + lWidth, y + (height - g.getFont().getHeight()) / 2,
                Graphics.LEFT + Graphics.TOP);

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    
    public int drawImages(Icon[] icons, int x, int y, int height) {
        int width = 0;
		if (null != icons) {
            for (int i = 0; i < icons.length; i++) {
                if (null != icons[i]) {
                    icons[i].drawByLeftTop(g, x + width, y + (height - icons[i].getHeight()) / 2);
                    width += icons[i].getWidth() + 1;
                }
            }
		}
        return width - 1;
    }
    
    public int getImagesWidth(Icon[] icons) {
        int width = 0;
        int correction = 0;
		if (null != icons) {
            for (int i = 0; i < icons.length; i++) {
                if (null != icons[i]) {
                    width += icons[i].getWidth() + 1;
                    correction = -1;
                }
            }
		}
        return width + correction;
    }
    public static int getMaxImagesHeight(Icon[] images) {
        if (null == images) {
            return 0;
        }
        int height = 0;
        for (int i = 0; i < images.length; i++) {
            if (null != images[i]) {
                height = Math.max(height, images[i].getHeight());
            }
        }
        return height;
    }
    
    public void drawGradRect(int color1, int color2, int x, int y, int width, int height) {
        int r1 = ((color1 & 0xFF0000) >> 16);
        int g1 = ((color1 & 0x00FF00) >> 8);
        int b1 =  (color1 & 0x0000FF);
        int r2 = ((color2 & 0xFF0000) >> 16);
        int g2 = ((color2 & 0x00FF00) >> 8);
        int b2 =  (color2 & 0x0000FF);
        
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);

        int step = Math.max((height + 8) / 9, 1);
        for (int i = 0; i < 9; i++) {
            g.setColor(i * (r2 - r1) / 8 + r1, i * (g2 - g1) / 8 + g1, i * (b2 - b1) / 8 + b1);
            g.fillRect(x, i * step + y, width, step);
        }

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    // #sijapp cond.if target="MIDP2" | target="MOTOROLA" | target="SIEMENS2"#
    public int getSoftBarSize(String left, String middle, String right) {
        if (NativeCanvas.isFullScreen() && Options.getBoolean(Options.OPTION_SHOW_SOFTBAR)) { //add
        if ((null == left) && (null == right)) {
            return 0;
        }
        if (NativeCanvas.isFullScreen()) {
            if (null != Scheme.softbarImage) {
                return Scheme.softbarImage.getHeight();
            }                                                                                  //add
            }
            return Math.max(CanvasEx.minItemHeight, softBarFont.getHeight() + 2);
        }
        return 0;
    }
    public void drawSoftBar(String left, String middle, String right, int height) {
        int w = getWidth();
        int x = 0;
        int y = getHeight();
        int h = height;
                
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, w, h);

        if (null == Scheme.softbarImage) {
            int capBkColor = getThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            drawGradRect(capBkColor, transformColorLight(capBkColor, -32), x, y, w, h);
            g.setColor(transformColorLight(capBkColor, -128));
            g.drawLine(x, y, x + w, y);
        } else {
            drawImage(Scheme.softbarImage, x, y, w, h);
        }
        int halfSoftWidth = w / 2 - (2 + 2 * softbarOffset);
        h -= 2;
        y++;
        setThemeColor(CanvasEx.THEME_CAP_TEXT);
        g.setFont(softBarFont);

        int leftWidth = 0;
        h -= (h - softBarFont.getHeight()) / 2;
        if (null != left) {
            leftWidth = Math.min(softBarFont.stringWidth(left),  halfSoftWidth);
            drawString(left,  x + softbarOffset, y, leftWidth,  h);
        }
        
        int rightWidth = 0;
        if (null != right) {
            rightWidth = Math.min(softBarFont.stringWidth(right), halfSoftWidth);
            drawString(right, x + w - rightWidth - softbarOffset, y, rightWidth, h);
        }
        
        int criticalWidth = halfSoftWidth - 5;
        if ((rightWidth < criticalWidth) && (leftWidth < criticalWidth)) {
            int middleWidth = softBarFont.stringWidth(middle) + 2;
            int start = (w - middleWidth) / 2;
            if ((leftWidth < start) && (rightWidth < start)) {
                drawString(middle, x + start, y, middleWidth, h);
            }
            
        }
        
        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    // #sijapp cond.end #

//    public void drawGlassRect(int color1, int color2, int x, int y, int width, int height) {
//        int clipX = g.getClipX();
//        int clipY = g.getClipY();
//        int clipHeight = g.getClipHeight();
//        int clipWidth = g.getClipWidth();
//        g.setClip(x, y, width, height);
//        g.setColor(color1);
//        g.fillRect(x, y, width, height / 2);
//        g.setColor(color2);
//        g.fillRect(x, y + height / 2, width, (height + 1) / 2);
//        g.setClip(clipX, clipY, clipWidth, clipHeight);
//    }
//    
    // #sijapp cond.if modules_STYLUS is "true"#
    public static int[] getVertScrollMetrix(int height, int pos, int len, int total) {
        height++;
        boolean haveToShowScroller = ((total > len) && (total > 0));
        if (!haveToShowScroller) {
            return null;
        }
        pos = Math.min(total - len, pos);
        int srcollerY1 = (pos * height) / total;
        int srcollerY2 = ((pos + len) * height) / total;
        int sliderSize = srcollerY2 - srcollerY1;
        if (sliderSize < 7) {
            sliderSize = 7;
            srcollerY2 = Math.min(height, srcollerY1 + sliderSize);
            srcollerY1 = Math.max(0, srcollerY2 - sliderSize);
        }
        return new int[]{srcollerY1, srcollerY2};
    }
    // #sijapp cond.end#

private void corner(int x, int y1, int y2, int radius) { //scroll
        if (radius < 8) {
            y2 += radius;
            y1 -= radius + 1;
            radius = (radius + 1) / 2;
            x += radius;
            for (int dx = 0; dx < radius; ++dx) {
                g.drawLine(x + dx, y2 - dx, x + dx, y2);
                g.drawLine(x + dx, y1 + dx, x + dx, y1);
            }
            return;
        }
        int dy = radius;
        int sum = radius;
        g.setStrokeStyle(Graphics.SOLID);
        y1 -= 1;
        for (int dx = radius / 2; dx < radius; ++dx) {
            sum -= dx;
            while (sum < 0) sum += dy--;
            g.drawLine(x + dx, y2 + dy, x + dx, y2 + radius);
            g.drawLine(x + dx, y1 - dy, x + dx, y1 - radius);
        }
        //g.drawLine(x + radius, y2 + 0, x + radius, y2 + radius);
        //g.drawLine(x + radius, y1 - 0, x + radius, y1 - radius);
    }
    public void drawVertScroll(int x, int y, int width, int height, int pos, int len, int total, byte fore, byte back) {
        boolean haveToShowScroller = ((total > len) && (total > 0));
        pos = Math.min(total - len, pos);
        if (haveToShowScroller) {
            int clipX = g.getClipX();
            int clipY = g.getClipY();
            int clipHeight = g.getClipHeight();
            int clipWidth = g.getClipWidth();
            g.setClip(x, y, width, height);

            int srcollerY1 = (pos * height) / total;
            int srcollerY2 = ((pos + len) * height) / total;
            int sliderSize = srcollerY2 - srcollerY1;
            if (sliderSize < 7) {
                sliderSize = 7;
                srcollerY2 = Math.min(height, srcollerY1 + sliderSize);
                srcollerY1 = Math.max(0, srcollerY2 - sliderSize);
            }
            srcollerY1 += y;
            srcollerY2 += y;
            g.setStrokeStyle(Graphics.SOLID);
            setThemeColor(back);
            g.fillRect(x, y, width, height);
            setThemeColor(fore);
            //g.fillRoundRect(x - width - 1, srcollerY1, width * 2, sliderSize, width, width);
            g.fillRect(x, srcollerY1, width - 1, sliderSize);
            setThemeColor(back);
            corner(x, srcollerY1 + width - 1, srcollerY2 - width + 1, width - 1);

            g.setClip(clipX, clipY, clipWidth, clipHeight);
        } else {
            g.setStrokeStyle(Graphics.SOLID);
            setThemeColor(fore);
            g.fillRect(x, y, width, height);
        }
    }

    public void fillRect(int x, int y, int width, int height, byte sback) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x, y, width, height);

        setThemeColor(sback);
        g.fillRect(x, y, width, height);

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }
    private static final int SHADOW_SIZE = 2;
    public void drawDoubleBorder(int x, int y, int width, int height, byte sborder) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(x - SHADOW_SIZE, y - SHADOW_SIZE, width + SHADOW_SIZE * 2, height + SHADOW_SIZE * 2);

        setThemeColor(sborder);
        g.drawLine(x + width, y - 1, x + width, y + height);
        g.drawLine(x - 1, y - 1, x - 1, y + height);
        g.fillRect(x - SHADOW_SIZE, y, SHADOW_SIZE, height);
        g.fillRect(x + width, y, SHADOW_SIZE, height);
        g.fillRect(x, y - SHADOW_SIZE, width, SHADOW_SIZE);
        g.fillRect(x, y + height, width, SHADOW_SIZE);

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public final void setStrokeStyle(int style) {
        g.setStrokeStyle(style);
    }

    public final void fillRect(int x, int y, int width, int height) {
        g.fillRect(x, y, width, height);
    }
    public final void fillRect(int left, int top, int width, int height, Image img) {
        int clipX = g.getClipX();
        int clipY = g.getClipY();
        int clipHeight = g.getClipHeight();
        int clipWidth = g.getClipWidth();
        g.setClip(left, top, width, height);

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        for (int x = 0; x < width; x += imgWidth) {
            for (int y = 0; y < height; y += imgHeight) {
                g.drawImage(img, x + left, y + top, Graphics.TOP | Graphics.LEFT);
            }
        }

        g.setClip(clipX, clipY, clipWidth, clipHeight);
    }

    public final void drawImage(Image image, int x, int y, int anchor) {
        g.drawImage(image, x, y, anchor);
    }
    public final void setColor(int color) {
        g.setColor(color);
    }

    public final void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    public final int getColor() {
        return g.getColor();
    }

    public final void drawRect(int x, int y, int width, int height) {
        g.drawRect(x, y, width, height);
    }

    public final void drawString(String str, int x, int y, int anchor) {
        g.drawString(str, x, y, anchor);
    }

    public final void drawByLeftTop(Icon icon, int x, int y) {
        if (null != icon) {
            icon.drawByLeftTop(g, x, y);
        }
    }

    public final void drawInCenter(Icon icon, int x, int y) {
        if (null != icon) {
            icon.drawInCenter(g, x, y);
        }
    }


    public final int getClipY() {
        return g.getClipY();
    }

    public final int getClipX() {
        return g.getClipX();
    }

    public final int getClipWidth() {
        return g.getClipWidth();
    }

    public final int getClipHeight() {
        return g.getClipHeight();
    }

    public final void setClip(int x, int y, int width, int height) {
        g.setClip(x, y, width, height);
    }
    public final void clipRect(int x, int y, int width, int height) {
        g.clipRect(x, y, width, height);
    }

    public final Graphics getGraphics() {
        return g;
    }

    public final void setFont(Font font) {
        g.setFont(font);
    }

    // #sijapp cond.if target is "MIDP2"#
    public static final int captionOffset;
    public static final int captionWidthFix;
    static {
        if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_NOKIA_N80)) {
            captionOffset = 30;
        } else if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_NOKIA)) {
            captionOffset = 20;
        } else {
            captionOffset = 0;
        }
        if (jimm.Jimm.isPhone(jimm.Jimm.PHONE_SE) || jimm.Jimm.isPhone(jimm.Jimm.PHONE_SAMSUNG)) {
            captionWidthFix = (Util.strToIntDef(Options.getString(Options.OPTION_CAPTION_ICON), 0)); //message icon
        } else {
            captionWidthFix = 0;
        }
    }
    // #sijapp cond.end#
    public static int calcCaptionHeight(Icon[] icons, String text) {
        if (!NativeCanvas.isFullScreen()) return 0;

        if (null != Scheme.captionImage) {
            return Scheme.captionImage.getHeight();
        }

        int captionHeight = captionFont.getHeight();
        if (null != icons) {
            for (int i = 0; i < icons.length; i++) {
                if (null != icons[i]) {
                    captionHeight = Math.max(captionHeight, icons[i].getHeight());
                }
            }
        }
        return Math.max(CanvasEx.minItemHeight, captionHeight + 2 + 1);
    }
    public void drawCaption(Icon[] icons, String text, Icon leftIcon, int height) {
        if (height <= 0) return;
        
        int width = getWidth();
        if (null == Scheme.captionImage) {
            int capBkCOlor = getThemeColor(CanvasEx.THEME_CAP_BACKGROUND);
            drawGradRect(capBkCOlor, transformColorLight(capBkCOlor, -32), 0, 0, width, height);
            setColor(transformColorLight(capBkCOlor, -128));
            drawLine(0, height - 1, width, height - 1);
        } else {
            drawImage(Scheme.captionImage, 0, 0, width, height);
        }
        if (null != leftIcon) {
            int h = Math.max(0, (height - leftIcon.getHeight()) / 2);
            // #sijapp cond.if target is "MIDP2"#
            width -= (0 == captionWidthFix) ? h : captionWidthFix;
            // #sijapp cond.else#
            width -= h;
            // #sijapp cond.end#
            
            width -= leftIcon.getWidth();
            leftIcon.drawByLeftTop(g, width, h);
            width -= 1;
        }
        int x = 2;
        // #sijapp cond.if target is "MIDP2"#
        x += captionOffset;
        // #sijapp cond.end#
        g.setFont(captionFont);
        setThemeColor(CanvasEx.THEME_CAP_TEXT);
        drawString(icons, text, null, x, 1, width - x, height - 2);
    }
    public int getWidth() {
        return NativeCanvas.getScreenWidth();
    }
    public int getHeight() {
        return NativeCanvas.getScreenHeight();
    }
    public void reset() {
        g = null;
    }

}