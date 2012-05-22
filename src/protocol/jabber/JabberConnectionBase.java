/*
 * JabberConnection.java
 *
 * Created on 12 Июль 2008 г., 19:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
// #sijapp cond.if protocols_JABBER is "true" #
package protocol.jabber;

import java.io.*;
import java.util.Vector;
import javax.microedition.io.*;
import jimm.JimmException;
import jimm.Options;
import jimm.comm.StringConvertor;
import jimm.comm.Util;
import jimm.modules.*;
import protocol.Protocol;

/**
 *
 * @author Vladimir Krukov
 */
abstract class JabberConnectionBase implements Runnable {
    protected Socket socket;
    protected Protocol protocol;
    private boolean connect;
    
    /** Creates a new instance of JabberConnection */
    public JabberConnectionBase(Protocol protocol) {
        this.protocol = protocol;
    }
    public final void start() {
        connect = true;
        new Thread(this).start();
    }
    

    protected void write(byte[] data) throws JimmException {
        socket.write(data);
    }

    protected void connectTo(String url) throws JimmException {
        try {
            socket = new Socket();
            socket.connectTo(url);
        } catch (JimmException e) {
            socket.close();
            socket = null;
            throw e;
        }
    }

    final boolean isConnected() {
        return connect;
    }
    public final void disconnect() {
        connect = false;
        protocol = null;
    }

    protected final int available() throws JimmException {
        return socket.available();
    }

    /////////////////////////////////////////////////////    
    protected abstract void connect() throws JimmException;
    protected abstract void processPacket() throws JimmException;
    protected abstract void sendPingPacket() throws JimmException;
    private long prevPingTime = 0;
    private long keepAliveInterv = 0;
    private void ping() throws JimmException {
        if (Options.getBoolean(Options.OPTION_KEEP_CONN_ALIVE)) {
            long time = System.currentTimeMillis();
            if (time > (prevPingTime + keepAliveInterv)) {
                prevPingTime = time;
                sendPingPacket();
            }
        }
    }

    private final Vector packets = new Vector();
    protected void putPacketIntoQueue(Object packet) {
        synchronized (packets) {
            packets.addElement(packet);
        }
    }
    private boolean hasOutPackets() {
        synchronized (packets) {
            return !packets.isEmpty();
        }
    }
    protected abstract void writePacket(String packet) throws JimmException;
    private void sendPacket() throws JimmException {
        String packet = null;
        synchronized (packets) {
            packet = (String)packets.elementAt(0);
            packets.removeElementAt(0);
        }
        writePacket(packet);
    }
    /////////////////////////////////////////////////////

    protected final void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
    
    protected abstract void sendClosePacket();
    public final void run() {
        long pingTime = Util.strToIntDef(Options.getString(Options.OPTION_CONN_ALIVE_INVTERV), 120);
        pingTime = Math.max(pingTime, 1) * 1000;
        keepAliveInterv = pingTime;

        try {
            connect();
            while (null != protocol && socket.isConnected()) {
                if (hasOutPackets()) {
                    sendPacket();

                } else if (0 < available()) {
                    processPacket();

                } else {
                    sleep(200);
                    ping();
                }
            }
        } catch (JimmException e) {
            Protocol p = protocol;
            if (null != p) {
                p.processException(e);
            }
        } catch (Exception e) {
        }
        if (null != socket) {
            disconnect();
            if (socket.isConnected()) {
                sendClosePacket();
            }
            socket.close();
            socket = null;
        }
    }
}
// #sijapp cond.end #
