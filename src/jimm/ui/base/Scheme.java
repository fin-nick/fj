/*
 * Scheme.java
 *
 * @author Vladimir Krukov
 */

package jimm.ui.base;

import DrawControls.*;
import DrawControls.icons.ImageList;
import java.io.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;

/**
 * 
 * Warning! This code used hack.
 * Current scheme not cloned (the reference to the base scheme is used),
 * but current scheme content will be rewritten, when current scheme is changed.
 *
 * @author Vladimir Krukov
 */
public class Scheme {
    
    /**
     * Creates a new instance of Scheme
     */
    private Scheme() {
    }
    
    static public final Image backImage = ImageList.loadImage("/back.png");
    static public final Image captionImage = ImageList.loadImage("/caption.png");
    static public final Image softbarImage = ImageList.loadImage("/softbar.png");

    static private int[] baseTheme = {
        0x000000, 0xFFFFFF, 0x1D1D1D, 0xFFFFFF, 0x00FFFF,  //it is changed
        0xFF0000, 0x00FFFF, 0x808080, 0xFFFFFF, 0x00FFFF,  //it is changed
        0xBFBFBF, 0x808080, 0x808080, 0xA0A000, 0xE0E0E0,  //it is changed
        0x000000, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,  //it is changed
        0x000000, 0xFFFFFF, 0x000000, 0x00FFFF, 0xFFFFFF,  //it is changed
        0xFF0000, 0x00FFFF, 0xFFFFFF, 0x606060, 0x202020,  //it is changed
        0xC0C0C0, 0xC0C0C0, 0x202020, 0xA0A000, 0xC0C0C0,  //it is changed
        0x606060, 0xC0C0C0, 0x202020, 0xC0C0C0, 0xFFFFFF,  //it is changed
        0xFFDDDD, 0x202020, 0x808080, 0xD8D8D8, 0x3F3F3F}; //it is changed
    static private int[] currentTheme = new int[baseTheme.length];
    static private int[][] themeColors;
    static private String[] themeNames;
    
    public static void load() {
        Vector themes = new Vector();
        try {
            String content = Config.loadResource("/themes.txt");
            Config.parseIniConfig(content, themes);
        } catch (Exception e) {
        }
        themeNames  = new String[themes.size() + 1];
        themeColors = new int[themes.size() + 1][];

        themeNames[0]  = "white on black"; //it is changed
        themeColors[0] = baseTheme;
        for (int i = 0; i < themes.size(); ++i) {
            Config config = (Config)themes.elementAt(i);
            themeNames[i + 1]  = config.getName();
            themeColors[i + 1] = configToTheme(config.getKeys(), config.getValues());
        }
    }
    
    private static int[] configToTheme(String[] keys, String[] values) {
        int[] theme = new int[baseTheme.length];
        System.arraycopy(baseTheme, 0, theme, 0, theme.length);
        try {
            for (int keyIndex = 0; keyIndex < keys.length; ++keyIndex) {
                int index = Util.strToIntDef(keys[keyIndex], -1);
                if ((0 <= index) && (index < theme.length)) {
                    theme[index] = Integer.parseInt(values[keyIndex].substring(2), 16);
                    if (1 == index) {
                        theme[41] = theme[40] = theme[39] = theme[1];
                    } else if (10 == index) {
                        theme[42] = theme[10];
                    } else if (2 == index) {
                        theme[44] = theme[2];
                    } else if (39 == index) {
                        theme[43] = theme[39];
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return theme;
    }
    
    /**
     * Retrieves color value from color scheme
     */
    static public int[] getScheme() {
        return currentTheme;
    }
    /**
     * Retrieves color value from color scheme
     */
    public static int getSchemeColor(int type) {
        return currentTheme[type];
    }
    
    /* Retrieves color value from color scheme */
    public static String[] getSchemeNames() {
        return themeNames;
    }
    
    public static boolean fillCursor;
    public static void setColorScheme(int schemeNum) {
        if (themeNames.length <= schemeNum) {
            schemeNum = 0;
        }
        Options.setInt(Options.OPTION_COLOR_SCHEME, schemeNum);
        System.arraycopy(themeColors[schemeNum], 0, currentTheme, 0 , currentTheme.length);
        fillCursor = (currentTheme[VirtualList.THEME_TEXT] != currentTheme[VirtualList.THEME_SELECTION_BACK]);
    }
}