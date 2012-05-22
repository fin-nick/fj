/*
 * SocksSocket.java
 *
 * Created on 27 ������ 2007 �., 15:06
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

// #sijapp cond.if protocols_ICQ is "true" #
package protocol.net;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import jimm.comm.*;
import protocol.icq.*;
import protocol.icq.packet.*;
import jimm.modules.*;
import jimm.util.ResourceBundle;
import jimm.*;

// #sijapp cond.if modules_PROXY is "true"#

// SocksSocket
public class SocksSocket extends DirectSocket {
    
    private final byte[] SOCKS4_CMD_CONNECT =
    { (byte) 0x04, (byte) 0x01, (byte) 0x14, (byte) 0x46, // Port 5190
      (byte) 0x40, (byte) 0x0C, (byte) 0xA1, (byte) 0xB9,
      (byte) 0x00 // IP 64.12.161.185 (default login.icq.com)
    };
    
    private final byte[] SOCKS5_HELLO =
    { (byte) 0x05, (byte) 0x02, (byte) 0x00, (byte) 0x02};
    
    private final byte[] SOCKS5_CMD_CONNECT =
    { (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x03};
    
    
    private boolean is_connected = false;
    
    // Tries to resolve given host IP
    private String ResolveIP(String host, String port) {
        if (Util.isIP(host)) {
            return host;
        }
        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
        try {
            SocketConnection c = (SocketConnection) Connector.open("socket://" + host + ":" + port, Connector.READ_WRITE);
            String ip = c.getAddress();
            
            try {
                c.close();
            } catch (Exception e) {
            }
            c = null;
            return ip;
        } catch (Exception e) {
        }
        // #sijapp cond.end#
        return "0.0.0.0";
    }
    
    // Build socks4 CONNECT request
    private byte[] socks4_connect_request(String ip, String port) {
        byte[] buf = new byte[9];
        
        System.arraycopy(SOCKS4_CMD_CONNECT, 0, buf, 0, 9);
        Util.putWord(buf, 2, Integer.parseInt(port));
        byte[] bip = Util.ipToByteArray(ip);
        System.arraycopy(bip, 0, buf, 4, 4);
        
        return buf;
    }
    
    // Build socks5 AUTHORIZE request
    private byte[] socks5_authorize_request(String login, String pass) {
        byte[] buf = new byte[3 + login.length() + pass.length()];
        
        Util.putByte(buf, 0, 0x01);
        Util.putByte(buf, 1, login.length());
        Util.putByte(buf, login.length() + 2, pass.length());
        byte[] blogin = StringConvertor.stringToByteArray(login);
        byte[] bpass = StringConvertor.stringToByteArray(pass);
        System.arraycopy(blogin, 0, buf, 2, blogin.length);
        System.arraycopy(bpass, 0, buf, blogin.length + 3, bpass.length);
        
        return buf;
    }
    
    // Build socks5 CONNECT request
    private byte[] socks5_connect_request(String host, String port) {
        byte[] buf = new byte[7 + host.length()];
        
        System.arraycopy(SOCKS5_CMD_CONNECT, 0, buf, 0, 4);
        Util.putByte(buf, 4, host.length());
        byte[] bhost = StringConvertor.stringToByteArray(host);
        System.arraycopy(bhost, 0, buf, 5, bhost.length);
        Util.putWord(buf, 5 + bhost.length, Integer.parseInt(port));
        return buf;
    }
    
    // Opens a connection to the specified host and starts the receiver
    // thread
    public void connectTo(String url) throws JimmException {
        String hostAndPort = url.substring("socket://".length());

        int mode = Options.getInt(Options.OPTION_PRX_TYPE);
        is_connected = false;
        String host = "";
        String port = "";
        
        if (0 != mode) {
            int sep = hostAndPort.indexOf(':');
            host = hostAndPort.substring(0, sep);
            port = hostAndPort.substring(sep + 1);
        }
        switch (mode) {
            case 0:
                connect_socks4(host, port);
                break;
            case 1:
                connect_socks5(host, port);
                break;
            case 2:
                // Try better first
                try {
                    connect_socks5(host, port);
                } catch (Exception e) {
                    // Do nothing
                }
                // If not succeeded, then try socks4
                if (!is_connected) {
                    close();
                    try {
                        // Wait the given time
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                    connect_socks4(host, port);
                }
                break;
        }
    }
    
    // Attempts to connect through socks4
    private void connect_socks4(String host, String port) throws JimmException {
        String proxy_host = Options.getString(Options.OPTION_PRX_SERV);
        String proxy_port = Options.getString(Options.OPTION_PRX_PORT);
        int i = 0;
        byte[] buf;
        
        try {
            super.connectTo("socket://" + proxy_host + ":" + proxy_port);
            
            String ip = ResolveIP(host, port);
            
            write(socks4_connect_request(ip, port));
            
            // Wait for responce
            while (available() == 0 && i < 50) {
                try {
                    // Wait the given time
                    i++;
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
            // Read packet
            // If only got proxy responce packet, parse it
            if (available() == 8) {
                // Read reply
                buf = new byte[8];
                readFully(buf);
                
                int ver = Util.getByte(buf, 0);
                int meth = Util.getByte(buf, 1);
                // All we need
                if (ver == 0x00 && meth == 0x5A) {
                    is_connected = true;
                } else {
                    is_connected = false;
                    throw new JimmException(118, 2);
                }
            // If we got responce packet bigger than mere proxy responce,
            // we might got destination server responce in tail of proxy
            // responce
            } else if (available() > 8) {
                is_connected = true;
            } else {
                throw new JimmException(118, 2);
            }
        } catch (IllegalArgumentException e) {
            throw new JimmException(122, 0);
        }
    }
    
    private void waitResponse() throws JimmException, IOException {
        // Wait for responce
        for (int i = 0; i < 50; i++) {
            if (0 < available()) {
                return;
            }
            try {
                // Wait the given time
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
        if (available() == 0) {
            throw new JimmException(118, 2);
        }
    }

    // Attempts to connect through socks5
    private void connect_socks5(String host, String port) throws JimmException {
        String proxy_host = Options.getString(Options.OPTION_PRX_SERV);
        String proxy_port = Options.getString(Options.OPTION_PRX_PORT);
        byte[] buf;
        
        try {
            super.connectTo("socket://" + proxy_host + ":" + proxy_port);
            write(SOCKS5_HELLO);
            
            // Wait for responce
            waitResponse();
            
            // Read reply
            buf = new byte[available()];
            readFully(buf);
            
            int ver = Util.getByte(buf, 0);
            int meth = Util.getByte(buf, 1);
            
            // Plain text authorisation
            if (ver == 0x05 && meth == 0x02) {
                String proxy_login = Options.getString(Options.OPTION_PRX_NAME);
                String proxy_pass = Options.getString(Options.OPTION_PRX_PASS);
                write(socks5_authorize_request(proxy_login, proxy_pass));
                
                // Wait for responce
                waitResponse();
                
                // Read reply
                buf = new byte[available()];
                readFully(buf);
                
                meth = Util.getByte(buf, 1);
                
                if (meth == 0x00) {
                    is_connected = true;
                } else {
                    // Unknown error (bad login or pass)
                    throw new JimmException(118, 3);
                }

            // Proxy without authorisation
            } else  if (ver == 0x05 && meth == 0x00) {
                is_connected = true;
            } else {
                // Something bad happened :'(
                throw new JimmException(118, 2);
            }
            // If we got correct responce, send CONNECT
            if (is_connected == true) {
                write(socks5_connect_request(host, port));
            }

            // Wait for responce
            waitResponse();
            
            // Verify and strip out proxy responce
            // Check for socks5
            if (read() == 0x05) {
                // Strip only on first packet
                
                if (read() != 0x00) {
                    // Something went wrong :(
                    throw new JimmException(118, 1);
                }
                read();
                // Check ATYP and skip BND.ADDR
                switch (read()) {
                    case 0x01:
                        byte[] ip = new byte[4];
                        readFully(ip);
                        break;
                    case 0x03:
                        byte[] arr = new byte[read() + 1];
                        readFully(arr);
                        break;
                default:
                        byte[] ip2 = new byte[4];
                        readFully(ip2);
                        break;
                }
                read();
                read();
                is_connected = true;
            }
        } catch (ConnectionNotFoundException e) {
            throw new JimmException(121, 0);
        } catch (SecurityException e) {
            throw new JimmException(120, 9);
        } catch (IllegalArgumentException e) {
            throw new JimmException(122, 0);
        } catch (IOException e) {
            throw new JimmException(120, 0);
        }
    }
}
// #sijapp cond.end #
// #sijapp cond.end #

