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

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.n52.wps.webapp.entities.ServiceIdentification;
import org.n52.wps.webapp.entities.ServiceProvider;
import org.n52.wps.webapp.util.JDomUtil;
import org.n52.wps.webapp.util.ResourcePathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * An implementation for the {@link CapabilitiesDAO} interface. This implementation uses {@code JDom} to parse the
 * {@code wpsCapabilitiesSkeleton.xml} file.
 */
@Repository("capabilitiesDAO")
public class XmlCapabilitiesDAO implements CapabilitiesDAO {

	public static final String FILE_NAME = "config/wpsCapabilitiesSkeleton.xml";

    public static final Namespace NS_OWS
            = Namespace.getNamespace("ows", "http://www.opengis.net/ows/1.1");
    public static final Namespace NS_XLINK
            = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

	private static final Logger LOGGER = LoggerFactory.getLogger(XmlCapabilitiesDAO.class);
    private static final String EN_KEYWORDS = "Keywords";
    private static final String EN_ACCESS_CONSTRAINTS = "AccessConstraints";
    private static final String EN_FEES = "Fees";
    private static final String EN_TITLE = "Title";
    private static final String EN_ABSTRACT = "Abstract";
    private static final String EN_SERVICE_TYPE_VERSION = "ServiceTypeVersion";
    private static final String EN_SERVICE_TYPE = "ServiceType";
    private static final String EN_SERVICE_IDENTIFICATION
            = "ServiceIdentification";
    private static final String EN_KEYWORD = "Keyword";
    private static final String EN_ELECTRONIC_MAIL_ADDRESS
            = "ElectronicMailAddress";
    private static final String EN_COUNTRY = "Country";
    private static final String EN_POSTAL_CODE = "PostalCode";
    private static final String EN_ADMINISTRATIVE_AREA = "AdministrativeArea";
    private static final String EN_CITY = "City";
    private static final String EN_DELIVERY_POINT = "DeliveryPoint";
    private static final String EN_ADDRESS = "Address";
    private static final String EN_FACSIMILE = "Facsimile";
    private static final String EN_VOICE = "Voice";
    private static final String EN_PHONE = "Phone";
    private static final String EN_CONTACT_INFO = "ContactInfo";
    private static final String EN_SERVICE_CONTACT = "serviceContact";
    private static final String EN_INDIVIDUAL_NAME = "IndividualName";
    private static final String EN_POSITION_NAME = "PositionName";
    private static final String EN_SERVICE_PROVIDER = "ServiceProvider";
    private static final String EN_PROVIDER_NAME = "ProviderName";
    private static final String EN_PROVIDER_SITE = "ProviderSite";
    private static final String AN_HREF = "href";

	@Autowired
	private JDomUtil jDomUtil;

	@Autowired
	private ResourcePathUtil resourcePathUtil;

    @Override
	public ServiceIdentification getServiceIdentification() {
        ServiceIdentification serviceIdentification = new ServiceIdentification();
        String absolutePath = resourcePathUtil.getWebAppResourcePath(FILE_NAME);
        Document document = jDomUtil.parse(absolutePath);
        Element root = document.getRootElement();
        Element serviceIdentificationElement = root
                .getChild(EN_SERVICE_IDENTIFICATION, NS_OWS);
        serviceIdentification.setTitle(getValue(serviceIdentificationElement, EN_TITLE));
        serviceIdentification.setServiceAbstract(getValue(serviceIdentificationElement, EN_ABSTRACT));
        serviceIdentification.setServiceType(getValue(serviceIdentificationElement, EN_SERVICE_TYPE));
        serviceIdentification.setServiceTypeVersion(getValue(serviceIdentificationElement, EN_SERVICE_TYPE_VERSION));
        serviceIdentification.setFees(getValue(serviceIdentificationElement, EN_FEES));
        serviceIdentification.setAccessConstraints(getValue(serviceIdentificationElement, EN_ACCESS_CONSTRAINTS));

        // keywords
        Element keywords = serviceIdentificationElement.getChild(EN_KEYWORDS, NS_OWS);
        if (keywords != null) {
            StringBuilder sb = new StringBuilder();
            for (Object keyword : keywords.getChildren()) {
                sb.append(((Element) keyword).getValue()).append("; ");
            }
            serviceIdentification.setKeywords(sb.toString());
        }
        LOGGER.info("'{}' is parsed and a ServiceIdentification object is returned", absolutePath);
        return serviceIdentification;
    }

    @Override
	public void saveServiceIdentification(ServiceIdentification serviceIdentification) {
        String absolutePath = resourcePathUtil.getWebAppResourcePath(FILE_NAME);
        Document document = jDomUtil.parse(absolutePath);

        Element root = document.getRootElement();
        Element serviceIdentificationElement = getElement(root, EN_SERVICE_IDENTIFICATION);
        setElement(getElement(serviceIdentificationElement, EN_TITLE), serviceIdentification.getTitle());
        setElement(getElement(serviceIdentificationElement, EN_ABSTRACT), serviceIdentification.getServiceAbstract());
        setElement(getElement(serviceIdentificationElement, EN_SERVICE_TYPE), serviceIdentification.getServiceType());
        setElement(getElement(serviceIdentificationElement, EN_SERVICE_TYPE_VERSION),
                   serviceIdentification.getServiceTypeVersion());
        setElement(getElement(serviceIdentificationElement, EN_FEES), serviceIdentification.getFees());
        setElement(getElement(serviceIdentificationElement, EN_ACCESS_CONSTRAINTS),
                   serviceIdentification.getAccessConstraints());

        Element keywords = getElement(serviceIdentificationElement, EN_KEYWORDS);
        if (keywords != null) {
            keywords.removeChildren(EN_KEYWORD, NS_OWS);
        }

        if (serviceIdentification.getKeywords() != null) {
            String[] keywordsArray = serviceIdentification.getKeywords().trim().split(";");
            for (String newKeyword : keywordsArray) {
                Element keyword = new Element(EN_KEYWORD, NS_OWS).setText(newKeyword);
                keywords.addContent(keyword);
            }
        }
        jDomUtil.write(document, absolutePath);
        LOGGER.info("ServiceIdentification values written to '{}'", absolutePath);
    }

    @Override
	public ServiceProvider getServiceProvider() {
        ServiceProvider serviceProvider = new ServiceProvider();

        String absolutePath = resourcePathUtil.getWebAppResourcePath(FILE_NAME);
        Document document = jDomUtil.parse(absolutePath);
        Element root = document.getRootElement();
        Element serviceProviderElement = getElement(root, EN_SERVICE_PROVIDER);

        serviceProvider.setProviderName(getValue(serviceProviderElement, EN_PROVIDER_NAME));

        // a special case, an attribute with a namespace
        serviceProvider.setProviderSite(serviceProviderElement
                .getChild(EN_PROVIDER_SITE, NS_OWS)
                .getAttributeValue(AN_HREF, NS_XLINK));

        // contact info
        Element serviceContact = getElement(serviceProviderElement, EN_SERVICE_CONTACT);
        serviceProvider.setIndividualName(getValue(serviceContact, EN_INDIVIDUAL_NAME));
        serviceProvider.setPosition(getValue(serviceContact, EN_POSITION_NAME));

        // phone
        Element contactInfo = getElement(serviceContact, EN_CONTACT_INFO);
        Element phone = getElement(contactInfo, EN_PHONE);
        serviceProvider.setPhone(getValue(phone, EN_VOICE));
        serviceProvider.setFacsimile(getValue(phone, EN_FACSIMILE));

        // address
        Element address = getElement(contactInfo, EN_ADDRESS);
        serviceProvider.setDeliveryPoint(getValue(address, EN_DELIVERY_POINT));
        serviceProvider.setCity(getValue(address, EN_CITY));
        serviceProvider.setAdministrativeArea(getValue(address, EN_ADMINISTRATIVE_AREA));
        serviceProvider.setPostalCode(getValue(address, EN_POSTAL_CODE));
        serviceProvider.setCountry(getValue(address, EN_COUNTRY));
        serviceProvider.setEmail(getValue(address, EN_ELECTRONIC_MAIL_ADDRESS));
        LOGGER.info("'{}' is parsed and a ServiceProvider object is returned", absolutePath);
        return serviceProvider;
    }

	@Override
	public void saveServiceProvider(ServiceProvider serviceProvider) {
		String absolutePath = resourcePathUtil.getWebAppResourcePath(FILE_NAME);
		Document document = jDomUtil.parse(absolutePath);
		Element root = document.getRootElement();
		Element serviceProviderElement = getElement(root, EN_SERVICE_PROVIDER);

		setElement(getElement(serviceProviderElement, EN_PROVIDER_NAME), serviceProvider.getProviderName());
		getElement(serviceProviderElement, EN_PROVIDER_SITE)
                .setAttribute(AN_HREF, serviceProvider.getProviderSite(), NS_XLINK);

		Element serviceContact = getElement(serviceProviderElement, EN_SERVICE_CONTACT);
		setElement(getElement(serviceContact, EN_INDIVIDUAL_NAME), serviceProvider.getIndividualName());
		setElement(getElement(serviceContact, EN_POSITION_NAME), serviceProvider.getPosition());

		Element contactInfo = getElement(serviceContact, EN_CONTACT_INFO);
		Element phone = getElement(contactInfo, EN_PHONE);
		setElement(getElement(phone, EN_VOICE), serviceProvider.getPhone());
		setElement(getElement(phone, EN_FACSIMILE), serviceProvider.getFacsimile());

		Element address = getElement(contactInfo, EN_ADDRESS);
		setElement(getElement(address, EN_DELIVERY_POINT), serviceProvider.getDeliveryPoint());
		setElement(getElement(address, EN_CITY), serviceProvider.getCity());
		setElement(getElement(address, EN_ADMINISTRATIVE_AREA), serviceProvider.getAdministrativeArea());
		setElement(getElement(address, EN_POSTAL_CODE), serviceProvider.getPostalCode());
		setElement(getElement(address, EN_COUNTRY), serviceProvider.getCountry());
		setElement(getElement(address, EN_ELECTRONIC_MAIL_ADDRESS), serviceProvider.getEmail());
		jDomUtil.write(document, absolutePath);
		LOGGER.info("ServiceProvider values written to '{}'", absolutePath);
	}

	private String getValue(Element element, String child) {
		if (element != null) {
			Element childElement = element.getChild(child, NS_OWS);
			if (childElement != null) {
				return childElement.getValue();
			}
		}
		return null;
	}

	private Element getElement(Element element, String child) {
		if (element != null) {
			return element.getChild(child, NS_OWS);
		}
		return null;
	}

	private void setElement(Element element, String value) {
		if (element != null) {
			element.setText(value);
		}
	}
}
