// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.emergencylocation;

import static net.java.sip.communicator.impl.protocol.sip.OperationSetPresenceSipImpl.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.java.sip.communicator.util.Logger;
import org.jitsi.util.xml.XMLUtils;

/**
 * Class encapsulating the information relating to an emergency call.
 */
public class EmergencyCallContext
{
    private static final Logger logger = Logger.getLogger(EmergencyCallContext.class);

    static final String DM_GP_ELEMENT = "xmlns:gp";
    static final String DM_GP_VALUE = "urn:ietf:params:xml:ns:pidf:geopriv10";

    static final String DM_GBP_ELEMENT = "xmlns:gbp";
    static final String DM_GBP_VALUE = DM_GP_VALUE + ":basicPolicy";

    static final String DM_CA_ELEMENT = "xmlns:ca";
    static final String DM_CA_VALUE = DM_GP_VALUE + ":civicAddr";

    private final String contentId;
    private final String pidfloBody;

    public EmergencyCallContext(LocationAddress address, String contentId)
    {
        this.contentId = contentId;

        String body = null;
        try
        {
            Document doc = XMLUtils.createDocument();

            final String entity = "pres:" + contentId;
            final String targetId = contentId;

            // <presence>
            Element presence = doc.createElement(PRESENCE_ELEMENT);
            presence.setAttribute(NS_ELEMENT, PIDF_NS_VALUE);
            presence.setAttribute(DM_NS_ELEMENT, DM_NS_VALUE);
            presence.setAttribute(DM_GP_ELEMENT, DM_GP_VALUE);
            presence.setAttribute(DM_GBP_ELEMENT, DM_GBP_VALUE);
            presence.setAttribute(DM_CA_ELEMENT, DM_CA_VALUE);
            presence.setAttribute(ENTITY_ATTRIBUTE, entity);
            doc.appendChild(presence);

            Element device = doc.createElement("dm:device");
            device.setAttribute(ID_ATTRIBUTE, targetId);
            presence.appendChild(device);

            Element geopriv = doc.createElement("gp:geopriv");
            device.appendChild(geopriv);

            Element locationInfo = doc.createElement("gp:location-info");
            geopriv.appendChild(locationInfo);

            locationInfo.appendChild(address.getXmlElement(doc));

            Element usageRules = doc.createElement("gp:usage-rules");
            geopriv.appendChild(usageRules);

            Element retransmissionAllowed = doc.createElement("gbp:retransmission-allowed");
            retransmissionAllowed.setTextContent("true");
            usageRules.appendChild(retransmissionAllowed);

            Element method = doc.createElement("gp:method");
            method.setTextContent("802.11");
            geopriv.appendChild(method);

            body = XMLUtils.createXml(doc);

            // We don't trace out the body we have constructed - it contains the location which is PII
        }
        catch (Exception e)
        {
            logger.error("Failed to create xml body", e);
        }

        pidfloBody = body;
    }

    public String getPIDFLOBody()
    {
        return pidfloBody;
    }

    public String getContentId()
    {
        return contentId;
    }
}
