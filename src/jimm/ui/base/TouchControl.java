/*
 * TouchControl.java
 *
 * Created on 18 Январь 2010 г., 18:42
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package jimm.ui.base;

import java.util.Timer;
import java.util.TimerTask;
import jimm.Options;
import jimm.modules.*;

/**
 *
 * @author Vladimir Kryukov
 */
// #sijapp cond.if modules_STYLUS is "true"#
public class TouchControl {
    private int pressY;
    private int pressX;
    private int curY;
    private int curX;
    private long pressTime;
    private boolean isDraged;

    public boolean scrollingOn;
    public boolean tappingOn;
    public boolean kineticOn;

    public int prevHeight;
    public int prevTopY;
    public int prevTopItem;
    public boolean isSecondTap;
    private KineticScrolling kinetic = null;

    private CanvasEx canvas = null;
    private int viewHeight = 0;
    private int viewWidth = 0;
    public void setCanvas(CanvasEx c) {
        canvas = c;
        stopKinetic();
    }
    public void setView(int width, int height) {
        viewWidth = width;
        viewHeight = height;
    }
    private void stopKinetic() {
        if (null != kinetic) {
            kinetic.stop();
            kinetic = null;
        }
    }
    public int calcScrollPosition(int size) {
        int top = (prevTopY + curY - pressY) * size / prevHeight;
        return Math.max(0, Math.min(top, size));
    }

    public void pointerReleased(int x, int y) {
        isDraged |= Math.abs(x - pressX) > 10;
        isDraged |= Math.abs(y - pressY) > 10;
        if (!(isDraged ? scrollingOn : tappingOn)) {
            return;
        }
        
        curX = x;
        curY = y;
        if (viewHeight <= y) {
            if (isDraged) {
                return;
            }
            int w = viewWidth;
            int lsoftWidth = w / 2 - (w * 10 / 100);
            int rsoftWidth = w - lsoftWidth;
            int lSoft = NativeCanvas.LEFT_SOFT;
            int rSoft = NativeCanvas.RIGHT_SOFT;
            if (Options.getBoolean(Options.OPTION_SWAP_SOFT_KEY)) {
                rSoft = NativeCanvas.LEFT_SOFT;
                lSoft = NativeCanvas.RIGHT_SOFT;
            }
            NativeCanvas nat = NativeCanvas.getInstance();
            if (x < lsoftWidth) {
                nat.emulateKey(lSoft);
                
            } else if (rsoftWidth < x) {
                nat.emulateKey(rSoft);
                
            } else {
                nat.emulateKey(NativeCanvas.NAVIKEY_FIRE);
            }
            pressTime = 0;
            return;
        }
        CanvasEx c = canvas;
        if (null != c) {
            if (isDraged) {
                try {
                    c.stylusMoved(pressX, pressY, x, y);
                    if (kineticOn) {
                        long deltaT = System.currentTimeMillis() - kineticStartTime;
                        kinetic = startKinetic(kineticFromY, y, (int)deltaT);
                    }
                } catch (Exception e) {
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    jimm.modules.DebugLog.panic("stylusMoved");
                    // #sijapp cond.end #
                }
            } else {
                try {
                    c.stylusTap(x, y, (System.currentTimeMillis() - pressTime) > 700);
                } catch (Exception e) {
                    // #sijapp cond.if modules_DEBUGLOG is "true" #
                    jimm.modules.DebugLog.panic("stylusTap");
                    // #sijapp cond.end #
                }
            }
        }
        pressTime = 0;
    }

    public void pointerPressed(int x, int y) {
        kineticOn = true;
        scrollingOn = true;
        tappingOn = true;

        isDraged = false;
        pressX = x;
        pressY = y;
        curX = x;
        curY = y;
        pressTime = System.currentTimeMillis();
        stopKinetic();
        kineticStartTime = pressTime;
        kineticFromY = curY;

        if (viewHeight <= y) {
            return;
        }
        CanvasEx c = canvas;
        if (null != c) {
            try {
                c.stylusPressed(x, y);
            } catch (Exception e) {
                kineticOn = false;
                scrollingOn = false;
                tappingOn = false;
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("stylusPressed");
                // #sijapp cond.end #
            }
        }
    }

    private int kineticFromY;
    private long kineticStartTime;
    private long kineticLastTime;
    public void pointerDragged(int x, int y) {
        if (!scrollingOn) {
            return;
        }
        boolean moved = (10 < Math.abs(x - curX))
                || (10 < Math.abs(y - curY));
        if (!moved) {
            return;
        }
        isDraged |= (10 < Math.abs(x - pressX));
        isDraged |= (10 < Math.abs(y - pressY));

        CanvasEx c = canvas;
        if (scrollingOn && isDraged
                && (0 < pressTime) && (null != c)) {
            if (kineticOn) {
                kineticStartTime = kineticLastTime;
                kineticFromY = curY;
                kineticLastTime = System.currentTimeMillis();
            }
            curX = x;
            curY = y;
            try {
                c.stylusMoving(pressX, pressY, x, y);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("stylusMoving");
                // #sijapp cond.end #
            }
        }
    }

    void kineticMoving(int y) {
        CanvasEx c = canvas;
        if (kineticOn && (null != c)) {
            try {
                c.stylusKineticMoving(pressX, pressY, pressX, y);
            } catch (Exception e) {
                // #sijapp cond.if modules_DEBUGLOG is "true" #
                jimm.modules.DebugLog.panic("stylusKineticMoving");
                // #sijapp cond.end #
            }
        }
    }

    private static final int discreteness = 50;
    private final int calcV(int way, int time) {
        // v = 2 * s / t - v0
        // v0 = 0
        return 2 * way * discreteness / time;
    }
    private final int calcAbsA(int v, int time) {
        // a = (v1 - v0) / t
        // v0 = 0
        return Math.abs(v * discreteness / time);
    }
    private KineticScrolling startKinetic(int fromY, int toY, int time) {
        final int way = toY - fromY;
        //DebugLog.println("way " + way);
        //DebugLog.println("time " + time);
        if ((Math.abs(way) < 3) || (time < 2)) {
            return null;
        }
        final int v = calcV(way, time) * 4 / 5;
        //DebugLog.println("velosity " + v);
        if (Math.abs(v) <= 2) {
            return null;
        }
        int a = Math.max(1, Math.max(calcAbsA(v, time) / 3, calcAbsA(v, 3000)));
        //DebugLog.println("acceleration " + a);
        KineticScrolling it = new KineticScrolling(this, toY, v, ((way < 0) ? +a : -a));
        it.start(discreteness);
        return it;
    }
}
class KineticScrolling extends TimerTask {
    private int y;
    private int a;
    private int v;
    private Timer timer;
    private TouchControl touch;
    
    public KineticScrolling(TouchControl touch, int y, int velosity, int acceleration) {
        this.y = y;
        this.a = acceleration;
        this.v = velosity;
        this.touch = touch;
        timer = new Timer();
    }
    public void start(int interval) {
        timer.schedule(this, interval, interval);
    }

    public void run() {
        y += v;
        //DebugLog.println("going " + y + " " + v);
        TouchControl t = touch;
        if (null != t) {
            t.kineticMoving(y);
        }
        int prevV = v;
        v += a;
        if (Math.abs(v) <= Math.min(5, CanvasEx.minItemHeight / 4)) {
            stop();
        }
        if (v * prevV < 0) {
            stop();
        }
    }
    public void stop() {
        Timer t = timer;
        timer = null;
        touch = null;
        if (null != t) {
            t.cancel();
        }
    }
}
// #sijapp cond.end#