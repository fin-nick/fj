/*
 * Socket.java
 *
 * Created on 4 Февраль 2009 г., 15:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.jabber;

import com.jcraft.jzlib.*;
import java.io.*;
import javax.microedition.io.*;
import jimm.JimmException;
import jimm.modules.*;
import jimm.modules.traffic.*;

/**
 *
 * @author Vladimir Krukov
 */
class Socket {
    private StreamConnection sc;
    private InputStream is;
    private OutputStream os;
    private boolean connected;
    
    /**
     * Creates a new instance of Socket
     */
    public Socket() {
    }
    // #sijapp cond.if modules_ZLIB is "true" #
    private ZInputStream zin;
    private ZOutputStream zout;
    private boolean compressed;
    public void activateStreamCompression() {
        zin = new ZInputStream(is);
        zout = new ZOutputStream(os, JZlib.Z_DEFAULT_COMPRESSION);
        zout.setFlushMode(JZlib.Z_SYNC_FLUSH);
        compressed = true;
        // #sijapp cond.if modules_DEGUGLOG is "true" #
        DebugLog.println("zlib is working");
        // #sijapp cond.end #
    }
    
    private boolean isCompressed() {
        return compressed;
    }
    // #sijapp cond.end #
    public boolean isConnected() {
        return connected;
    }
    
    public void connectTo(String url) throws JimmException {
        try {
            System.out.print("url: " + url);
            sc = (StreamConnection)Connector.open(url, Connector.READ_WRITE);
            os = sc.openOutputStream();
            is = sc.openInputStream();
            connected = true;
        } catch (ConnectionNotFoundException e) {
            throw new JimmException(121, 0);
        } catch (IllegalArgumentException e) {
            throw new JimmException(122, 0);
        } catch (SecurityException e) {
            throw new JimmException(120, 9);
        } catch (IOException e) {
            throw new JimmException(120, 0);
        } catch (Exception e) {
            throw new JimmException(120, 10);
        }
    }

    private int read(byte[] data) throws JimmException {
        try {
            // #sijapp cond.if modules_ZLIB is "true" #
            if (isCompressed()) {
                int bRead = zin.read(data);
                if (-1 == bRead) {
                    throw new IOException("EOF");
                }
                return bRead;
            }
            // #sijapp cond.end #
            int length = Math.min(data.length, is.available());
            if (0 == length) {
                return 0;
            }
            int bRead = is.read(data, 0, length);
            if (-1 == bRead) {
                throw new IOException("EOF");
            }
            // #sijapp cond.if modules_TRAFFIC is "true" #
            Traffic.getInstance().addInTraffic(bRead * 3 / 2);
            // #sijapp cond.end#
            return bRead;

        } catch (IOException e) {
            close();
            throw new JimmException(120, 1);
        }
    }

    public void write(byte[] data) throws JimmException {
        try {
            // #sijapp cond.if modules_ZLIB is "true" #
            if (isCompressed()) {
                zout.write(data);
                zout.flush();
                return;
            }
            // #sijapp cond.end #
            os.write(data);
            os.flush();
        } catch (IOException e) {
            close();
            throw new JimmException(120, 2);
        }
        // #sijapp cond.if modules_TRAFFIC is "true" #
        Traffic.getInstance().addOutTraffic(data.length * 3 / 2);
        // #sijapp cond.end#
    }
    public void close() {
        connected = false;
        // #sijapp cond.if modules_ZLIB is "true" #
        try {
            zin.close();
            zout.close();
        } catch (Exception ex) {
        }
        // #sijapp cond.end #
        try {
            is.close();
            os.close();
        } catch (Exception ex) {
        }
        try {
            sc.close();
        } catch (Exception ex) {
        }
        inputBufferLength = 0;
        inputBufferIndex = 0;
    }

    private final void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
    
    private byte[] inputBuffer = new byte[1024];
    private int inputBufferLength = 0;
    public int inputBufferIndex = 0;
    public byte readByte() throws JimmException {
        if (inputBufferIndex >= inputBufferLength) {
            inputBufferIndex = 0;
            inputBufferLength = read(inputBuffer);
            while (0 == inputBufferLength) {
                sleep(100);
                inputBufferLength = read(inputBuffer);
            }
        }
        return inputBuffer[inputBufferIndex++];
    }
    public int available() throws JimmException {
        if (inputBufferIndex < inputBufferLength) {
            return (inputBufferLength - inputBufferIndex);
        }
        try {
            return is.available();
        } catch (IOException ex) {
            throw new JimmException(120, 3);
        }
    }
}
