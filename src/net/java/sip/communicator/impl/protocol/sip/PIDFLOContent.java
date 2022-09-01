// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;

import gov.nist.javax.sip.message.ContentImpl;

import net.java.sip.communicator.service.protocol.emergencylocation.EmergencyCallContext;
import net.java.sip.communicator.util.Logger;

public class PIDFLOContent extends ContentImpl
{
    public static final Logger logger = Logger.getLogger(PIDFLOContent.class);
    private final HeaderFactory headerFactory;
    private final EmergencyCallContext emergency;

    /**
     * Constructor
     *
     * @param emergency The context for the emergency call.  This includes
     * details of the user's location, so that we can render it into a PIDF-LO
     * message body part, for passing to the emergency services.
     * @param headerFactory The header factory to use when creating the headers
     * required for use with PIDF-LO.
     */
    public PIDFLOContent(EmergencyCallContext emergency, HeaderFactory headerFactory)
    {
        super(emergency.getPIDFLOBody());
        this.headerFactory = headerFactory;
        this.emergency = emergency;

        try
        {
            this.setContentTypeHeader(headerFactory.createContentTypeHeader(
                    "application",
                    "pidf+xml"));

            this.addExtensionHeader(headerFactory.createHeader(
                    "Content-ID",
                    "<" + emergency.getContentId() + ">"));
        }
        catch (ParseException e)
        {
            logger.error("Failed to create PIDF-LO content", e);
        }
    }

    /**
     * Get a list of additional headers that should be added to a SIP message
     * when a PIDF-LO body part has been added.
     *
     * @return A list of headers to add to the message.
     */
    public List<Header> getAdditionalHeaders()
    {
        List<Header> additionalHeaders = new ArrayList<>();

        try
        {
            additionalHeaders.add(headerFactory.createHeader(
                    "Geolocation",
                    "<cid:" + emergency.getContentId() + ">"));

            additionalHeaders.add(headerFactory.createHeader(
                    "Geolocation-Routing",
                    "yes"));

            // Surprisingly we don't set an accept header anywhere else, so
            // we can just set our value, rather than merging it
            additionalHeaders.add(headerFactory.createAcceptHeader(
                    "application",
                    "pidf+xml"));

            // We don't include a P-Asserted-Identity header, which matches the
            // behaviour of the mobile clients.
        }
        catch (ParseException e)
        {
            logger.error("Failed to create additional PIDF-LO headers", e);
        }

        return additionalHeaders;
    }
}
