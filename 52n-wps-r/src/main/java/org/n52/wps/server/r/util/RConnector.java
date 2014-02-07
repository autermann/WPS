/**
 * Copyright (C) 2010 - 2014 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *       * Apache License, version 2.0
 *       * Apache Software License, version 1.0
 *       * GNU Lesser General Public License, version 3
 *       * Mozilla Public License, versions 1.0, 1.1 and 2.0
 *       * Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.server.r.util;

import java.io.IOException;
import java.util.Arrays;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RConnector {

    private static final long START_ATTEMPT_SLEEP = 1000l;

    private static final int START_ATTEMP_COUNT = 5;

    private static Logger log = LoggerFactory.getLogger(RConnector.class);

    public RConnection getNewConnection(boolean enableBatchStart,
            String host,
            int port,
            String user,
            String password) throws RserveException
    {
        RConnection con = null;
        log.debug("Creating new RConnection");
        con = getNewConnection(enableBatchStart, host, port);

        // Login MUSST be the next request after connection
        // otherwise the connection is broken even if login is requested later
        if (con != null && con.needLogin())
            con.login(user, password);

        RLogger.log(con, "New connection from WPS4R");
        REXP info = con.eval("capture.output(sessionInfo())");
        try {
            log.debug("NEW CONNECTION >>> sessionInfo:\n" + Arrays.deepToString(info.asStrings()));
        } catch (REXPMismatchException e) {
            log.warn("Error creating session info.", e);
        }

        return con;
    }

    private RConnection getNewConnection(boolean enableBatchStart,
            String host,
            int port) throws RserveException
    {
        log.debug("New connection using batch " + enableBatchStart + " at host:port" + host + ":" + port);

        RConnection con = null;
        try {
            con = new RConnection(host, port);
        } catch (RserveException rse) {
            log.error("Could not connect to RServe.", rse);

            if (rse.getMessage().startsWith("Cannot connect") && enableBatchStart) {
                log.info("Attempting to start RServe.");

                try {
                    con = attemptStarts(host, port);
                } catch (Exception e) {
                    log.error("Attempted to start Rserve and establish a connection failed", e);

                    // Throwable#addSuppressed() only supported by Java 1.7+
                    // rse.addSuppressed(e);
                }
            } else
                throw rse;
        }
        // prevent from occasional NullPointerExceptions
        if (con == null)
            throw new RserveException(null, "Cannot connect with Rserve, connection is null");

        return con;
    }

    private RConnection attemptStarts(String host,
            int port) throws InterruptedException, IOException, RserveException
    {
        startRserve();

        int attempt = 1;
        RConnection con = null;
        while (attempt <= START_ATTEMP_COUNT) {
            try {
                Thread.sleep(START_ATTEMPT_SLEEP); // wait for R to startup,
                                                   // then establish connection
                con = new RConnection(host, port);
                break;
            } catch (RserveException rse) {
                if (attempt >= 5) {
                    throw rse;
                }

                attempt++;
            }
        }
        return con;
    }

    private static void startRServeOnLinux() throws InterruptedException, IOException
    {
        String rserveStartCMD = "R CMD Rserve --vanilla --slave";
        Runtime.getRuntime().exec(rserveStartCMD).waitFor();
    }

    private static void startRServeOnWindows() throws IOException
    {
        String rserveStartCMD = "cmd /c start R -e library(Rserve);Rserve() --vanilla --slave";
        Runtime.getRuntime().exec(rserveStartCMD);
    }

    public void startRserve() throws InterruptedException, IOException
    {
        log.debug("Starting Rserve locally...");

        if (System.getProperty("os.name").toLowerCase().indexOf("linux") > -1) {
            startRServeOnLinux();
        } else if (System.getProperty("os.name").toLowerCase().indexOf("windows") > -1) {
            startRServeOnWindows();
        }

        log.debug("Started RServe.");
    }

}
