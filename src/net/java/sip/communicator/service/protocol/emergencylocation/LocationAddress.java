// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.emergencylocation;

import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.json.simple.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LocationAddress
{
    final String number;
    final String numberSuffix;
    final String preDirectional;
    final String streetName;
    final String streetSuffix;
    final String postDirectional;
    final String addressLine2;
    final String city;
    final String stateOrProvince;
    final String postalCode;
    final String country;

    private String asString(Object possiblyNull)
    {
        return possiblyNull == null ? null : possiblyNull.toString();
    }

    public LocationAddress(JSONObject civicAddress)
    {
        number = asString(civicAddress.get("HNO"));
        numberSuffix = asString(civicAddress.get("HNS"));
        preDirectional = asString(civicAddress.get("PRD"));
        streetName = asString(civicAddress.get("RD"));
        streetSuffix = asString(civicAddress.get("STS"));
        postDirectional = asString(civicAddress.get("POD"));
        addressLine2 = asString(civicAddress.get("LOC"));
        city = asString(civicAddress.get("A3"));
        stateOrProvince = asString(civicAddress.get("A1"));
        postalCode = asString(civicAddress.get("PC"));
        country = asString(civicAddress.get("country"));
    }

    @Override
    public boolean equals(Object other)
    {
        final boolean equals;
        if (this == other)
        {
            equals = true;
        }
        else if (other == null || getClass() != other.getClass())
        {
            equals = false;
        }
        else
        {
            LocationAddress location = (LocationAddress)other;
            equals = Objects.equals(number, location.number) &&
                     Objects.equals(numberSuffix, location.numberSuffix) &&
                     Objects.equals(preDirectional, location.preDirectional) &&
                     Objects.equals(streetName, location.streetName) &&
                     Objects.equals(streetSuffix, location.streetSuffix) &&
                     Objects.equals(postDirectional, location.postDirectional) &&
                     Objects.equals(addressLine2, location.addressLine2) &&
                     Objects.equals(city, location.city) &&
                     Objects.equals(stateOrProvince, location.stateOrProvince) &&
                     Objects.equals(postalCode, location.postalCode) &&
                     Objects.equals(country, location.country);
        }

        return equals;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(number,
                            numberSuffix,
                            preDirectional,
                            streetName,
                            streetSuffix,
                            postDirectional,
                            addressLine2,
                            city,
                            stateOrProvince,
                            postalCode,
                            country);
    }

    private void addFieldIfPresent(Element civicAddress, String name, String value)
    {
        if (value != null)
        {
            Element e = civicAddress.getOwnerDocument().createElement("ca:" + name);
            e.setTextContent(value);
            civicAddress.appendChild(e);
        }
    }

    public Element getXmlElement(Document doc)
    {
        Element civicAddress = doc.createElement("ca:civicAddress");

        addFieldIfPresent(civicAddress, "HNO", number);
        addFieldIfPresent(civicAddress, "HNS", numberSuffix);
        addFieldIfPresent(civicAddress, "PRD", preDirectional);
        addFieldIfPresent(civicAddress, "RD", streetName);
        addFieldIfPresent(civicAddress, "STS", streetSuffix);
        addFieldIfPresent(civicAddress, "POD", postDirectional);
        addFieldIfPresent(civicAddress, "LOC", addressLine2);
        addFieldIfPresent(civicAddress, "A3", city);
        addFieldIfPresent(civicAddress, "A1", stateOrProvince);
        addFieldIfPresent(civicAddress, "PC", postalCode);
        addFieldIfPresent(civicAddress, "country", country);

        return civicAddress;
    }

    /**
     * @return a String representation of this address in the format:
     * [number=33,numberSuffix=,preDirectional=,streetName=Genotin,streetSuffix=Road,...]
     * This will contain PII and must be hashed before logging
     */
    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .append("number", number)
                .append("numberSuffix", numberSuffix)
                .append("preDirectional", preDirectional)
                .append("streetName", streetName)
                .append("streetSuffix", streetSuffix)
                .append("postDirectional", postDirectional)
                .append("addressLine2", addressLine2)
                .append("city", city)
                .append("stateOrProvince", stateOrProvince)
                .append("postalCode", postalCode)
                .append("country", country).toString();
    }
}
