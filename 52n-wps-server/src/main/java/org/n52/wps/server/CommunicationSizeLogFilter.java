/**
 * ﻿Copyright (C) 2007
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.wps.server;

// import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class measures the payload of the post data
 *
 * @author foerster
 *
 */
public final class CommunicationSizeLogFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(CommunicationSizeLogFilter.class);

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain) throws IOException,
                                                   ServletException {
        RequestSizeInfoWrapper wrappedRequest
                = new RequestSizeInfoWrapper(request);
        ResponseSizeInfoWrapper wrappedResponse
                = new ResponseSizeInfoWrapper(response);
        chain.doFilter(wrappedRequest, wrappedResponse);
        wrappedRequest.getInputStream().close();
        wrappedResponse.getOutputStream().close();
        long requestSize = wrappedRequest.getInputStream().getSize();
        long responseSize = wrappedResponse.getOutputStream().getSize();
        if (requestSize == 0) {
            return;
        }
        BigDecimal result = new BigDecimal((double) responseSize /
                                           (double) requestSize);
        result = result.setScale(4, BigDecimal.ROUND_HALF_UP).movePointRight(2);
        LOGGER.info("Simplification ratio: {}", result);
    }

    @Override
    public void destroy() {
    }

    private class ResponseSizeInfoStream extends ServletOutputStream {
        private final OutputStream intStream;
        private boolean closed = false;
        private long size = 0;

        ResponseSizeInfoStream(OutputStream outStream) {
            this.intStream = outStream;
        }

        @Override
        public void write(int i) throws IOException {
            this.size++;
            this.intStream.write(i);
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            this.intStream.close();
        }

        public long getSize() {
            return this.closed ? this.size : -1;
        }

    }

    private class ResponseSizeInfoWrapper extends HttpServletResponseWrapper {
        private final PrintWriter tpWriter;
        private final ResponseSizeInfoStream tpStream;

        ResponseSizeInfoWrapper(ServletResponse inResp) throws IOException {
            super((HttpServletResponse) inResp);
            tpStream = new ResponseSizeInfoStream(inResp.getOutputStream());
            tpWriter = new PrintWriter(tpStream);
        }

        @Override
        public ResponseSizeInfoStream getOutputStream() {
            return tpStream;
        }

        @Override
        public PrintWriter getWriter() {
            return tpWriter;
        }
    }

    private class RequestSizeInfoStream extends ServletInputStream {
        private boolean closed = false;
        private long size = 0;
        private final InputStream inputStream;

        RequestSizeInfoStream(InputStream inStream) {
            this.inputStream = inStream;
        }

        @Override
        public int read() throws IOException {
            int ret = this.inputStream.read();
            if (ret != -1) {
                this.size++;
            }
            return ret;
        }

        @Override
        public void close() throws IOException {
            this.closed = true;
            this.inputStream.close();
        }

        public long getSize() {
            return this.closed ? this.size : -1;
        }
    }

    private class RequestSizeInfoWrapper extends HttpServletRequestWrapper {
        private final BufferedReader tpReader;
        private final RequestSizeInfoStream tpStream;

        RequestSizeInfoWrapper(ServletRequest req) throws IOException {
            super((HttpServletRequest) req);
            this.tpStream = new RequestSizeInfoStream(req.getInputStream());
            this.tpReader
                    = new BufferedReader(new InputStreamReader(this.tpStream));
        }

        @Override
        public RequestSizeInfoStream getInputStream() {
            return this.tpStream;
        }

        @Override
        public BufferedReader getReader() {
            return this.tpReader;
        }
    }

}
