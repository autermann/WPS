/**
 * Copyright (C) 2013
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

package org.n52.wps.webapp.dao;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jdom.Document;
import org.jdom.Element;
import org.n52.wps.webapp.entities.LogConfigurations;
import org.n52.wps.webapp.util.JDomUtil;
import org.n52.wps.webapp.util.ResourcePathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.google.common.annotations.VisibleForTesting;

/**
 * An implementation for the {@link LogConfigurationsDAO} interface. This implementation uses {@code JDom} to parse the
 * {@code lobback.xml} file.
 */
@Repository
public class XmlLogConfigurationsDAO implements LogConfigurationsDAO {
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlLogConfigurationsDAO.class);
    @VisibleForTesting
    public static final String FILE_NAME = "logback.xml";
    private static final String CONSOLE_APPENDER_NAME = "wpsconsole";
    private static final String FILE_APPENDER_NAME = "wpsfile";
    private static final String EN_ROOT = "root";
    private static final String EN_APPENDER_REF = "appender-ref";
    private static final String AN_REF = "ref";
    private static final String AN_LEVEL = "level";
    private static final String AN_NAME = "name";
    private static final String EN_LOGGER = "logger";
    private static final String EN_APPENDER = "appender";
    private static final String EN_FILE_NAME_PATTERN = "fileNamePattern";
    private static final String EN_MAX_HISTORY = "maxHistory";
    private static final String EN_PATTERN = "pattern";
    private static final String EN_ENCODER = "encoder";
    private static final String EN_ROLLING_POLICY = "rollingPolicy";

	@Autowired
	private JDomUtil jDomUtil;

	@Autowired
	private ResourcePathUtil resourcePathUtil;

	@Override
	public LogConfigurations getLogConfigurations() {
        LogConfigurations logConfigurations = new LogConfigurations();
        String absolutePath = resourcePathUtil.getClassPathResourcePath(FILE_NAME);
        Document document = jDomUtil.parse(absolutePath);
        Element root = document.getRootElement();

        @SuppressWarnings("unchecked")
        List<Element> appenders = root.getChildren(EN_APPENDER);

        Element fileAppenderFileNamePatternElement = appenders.get(0).getChild(EN_ROLLING_POLICY)
                .getChild(EN_FILE_NAME_PATTERN);
        logConfigurations.setWpsfileAppenderFileNamePattern(getValue(fileAppenderFileNamePatternElement));
        
        Element fileAppenderMaxHistoryElement = appenders.get(0).getChild(EN_ROLLING_POLICY).getChild(EN_MAX_HISTORY);
        logConfigurations.setWpsfileAppenderMaxHistory(Integer.parseInt(getValue(fileAppenderMaxHistoryElement)));
        
        Element fileAppenderEncoderPatternElement = appenders.get(0).getChild(EN_ENCODER).getChild(EN_PATTERN);
        logConfigurations.setWpsfileAppenderEncoderPattern(getValue(fileAppenderEncoderPatternElement));
        
        Element consoleAppenderEncoderPatternElement = appenders.get(1).getChild(EN_ENCODER).getChild(EN_PATTERN);
        logConfigurations.setWpsconsoleEncoderPattern(getValue(consoleAppenderEncoderPatternElement));

        @SuppressWarnings("unchecked")
        List<Element> loggersElements = root.getChildren(EN_LOGGER);
        SortedMap<String, String> loggersMap = new TreeMap<>();

        for (Element element : loggersElements) {
            loggersMap.put(element.getAttributeValue(AN_NAME), element.getAttributeValue(AN_LEVEL));
        }
        logConfigurations.setLoggers(loggersMap);
        
        Element rootLevelElement = root.getChild(EN_ROOT);
        logConfigurations.setRootLevel(rootLevelElement.getAttributeValue(AN_LEVEL));

        @SuppressWarnings("unchecked")
        List<Element> rootAppenderRefsElements = rootLevelElement.getChildren(EN_APPENDER_REF);
        for (Element element : rootAppenderRefsElements) {
            switch (element.getAttributeValue(AN_REF)) {
                case FILE_APPENDER_NAME:
                    logConfigurations.setFileAppenderEnabled(true);
                    break;
                case CONSOLE_APPENDER_NAME:
                    logConfigurations.setConsoleAppenderEnabled(true);
                    break;
            }
        }
        LOGGER.info("'{}' is parsed and a LogConfigurations object is returned", absolutePath);
        return logConfigurations;
    }

    @Override
	public void saveLogConfigurations(LogConfigurations logConfigurations) {
        String absolutePath = resourcePathUtil.getClassPathResourcePath(FILE_NAME);
        Document document = jDomUtil.parse(absolutePath);

        Element root = document.getRootElement();

        @SuppressWarnings("unchecked")
        List<Element> appenders = root.getChildren(EN_APPENDER);

        Element fileAppenderFileNamePatternElement = appenders.get(0).getChild(EN_ROLLING_POLICY)
                .getChild(EN_FILE_NAME_PATTERN);
        setElement(fileAppenderFileNamePatternElement, logConfigurations.getWpsfileAppenderFileNamePattern());

        Element fileAppenderMaxHistoryElement = appenders.get(0).getChild(EN_ROLLING_POLICY).getChild(EN_MAX_HISTORY);
        setElement(fileAppenderMaxHistoryElement, String.valueOf(logConfigurations.getWpsfileAppenderMaxHistory()));

        Element fileAppenderEncoderPatternElement = appenders.get(0).getChild(EN_ENCODER).getChild(EN_PATTERN);
        setElement(fileAppenderEncoderPatternElement, logConfigurations.getWpsfileAppenderEncoderPattern());

        Element consoleAppenderEncoderPatternElement = appenders.get(1).getChild(EN_ENCODER).getChild(EN_PATTERN);
        setElement(consoleAppenderEncoderPatternElement, logConfigurations.getWpsconsoleEncoderPattern());

        root.removeChildren(EN_LOGGER);
        SortedMap<String, String> loggersMap = logConfigurations.getLoggers();

        if (loggersMap != null) {
            for (Map.Entry<String, String> entry : loggersMap.entrySet()) {
                Element element = new Element(EN_LOGGER);
                element.setAttribute(AN_NAME, entry.getKey());
                element.setAttribute(AN_LEVEL, entry.getValue());
                root.addContent(element);
            }
        }

        Element rootLevelElement = root.getChild(EN_ROOT);
        rootLevelElement.setAttribute(AN_LEVEL, logConfigurations.getRootLevel());

        rootLevelElement.removeChildren(EN_APPENDER_REF);
        if (logConfigurations.isFileAppenderEnabled()) {
            setAppender(rootLevelElement, FILE_APPENDER_NAME);
        }

        if (logConfigurations.isConsoleAppenderEnabled()) {
            setAppender(rootLevelElement, CONSOLE_APPENDER_NAME);
        }
        jDomUtil.write(document, absolutePath);
        LOGGER.info("LogConfigurations values written to '{}'", absolutePath);
    }

	private String getValue(Element element) {
        if (element != null) {
            return element.getValue();
        }
        return null;
    }

	private void setElement(Element element, String value) {
        if (element != null) {
            element.setText(value);
        }
    }
	
	private void setAppender(Element rootLevelElement, String appender) {
		Element appenderElement = new Element(EN_APPENDER_REF);
		appenderElement.setAttribute(AN_REF, appender);
		rootLevelElement.addContent(appenderElement);
	}

}
