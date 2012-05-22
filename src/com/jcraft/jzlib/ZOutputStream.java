/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2001 Lapo Luchini.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 
  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.
 
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the distribution.
 
  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

// #sijapp cond.if modules_ZLIB is "true" #
import java.io.*;

public class ZOutputStream {
    
    protected ZStream z = new ZStream();
    protected final int bufsize = 512;
    protected int flush = JZlib.Z_NO_FLUSH;
    protected byte[] buf = new byte[bufsize];
    protected OutputStream out;
    
    public ZOutputStream(OutputStream out, int level) {
        super();
        this.out = out;
        z.deflateInit(level, Deflate.MAX_WBITS, false);
    }
    
    public void write(byte b[]) throws IOException {
        int off = 0;
        int len = b.length;
        if (0 == len) {
            return;
        }
        int err;
        z.next_in = b;
        z.next_in_index = off;
        z.avail_in = len;
        do {
            z.next_out = buf;
            z.next_out_index = 0;
            z.avail_out = bufsize;
            err = z.deflate(flush);
            if (JZlib.Z_OK != err) {
                throw new IOException("deflating error");
            }
            out.write(buf, 0, bufsize - z.avail_out);
            // #sijapp cond.if modules_TRAFFIC is "true" #
            jimm.modules.traffic.Traffic.getInstance().addOutTraffic((bufsize-z.avail_out) * 2);
            // #sijapp cond.end#
        } while (z.avail_in>0 || z.avail_out==0);
    }
    
    public int getFlushMode() {
        return(flush);
    }
    
    public void setFlushMode(int flush) {
        this.flush=flush;
    }
    
    public void finish() throws IOException {
        int err;
        do{
            z.next_out = buf;
            z.next_out_index = 0;
            z.avail_out = bufsize;
            err = z.deflate(JZlib.Z_FINISH);
            if (err != JZlib.Z_STREAM_END && err != JZlib.Z_OK) {
                throw new IOException("deflating error");
            }
            if (0 < bufsize - z.avail_out) {
                out.write(buf, 0, bufsize - z.avail_out);
            }
        }
        while((0 < z.avail_in) || (0 == z.avail_out));
        flush();
    }
    public void end() {
        if (null == z) {
            return;
        }
        z.deflateEnd();
        z.free();
        z = null;
    }
    public void close() throws IOException {
        try{
            finish();
        } catch (IOException ignored) {
        }
        end();
        out.close();
        out=null;
    }
    
    public void flush() throws IOException {
        out.flush();
    }
    
}
// #sijapp cond.end #
