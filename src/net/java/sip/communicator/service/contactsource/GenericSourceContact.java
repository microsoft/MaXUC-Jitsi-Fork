/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.contactsource;

import static org.jitsi.util.Hasher.logHasher;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Implements a generic <tt>SourceContact</tt> for the purposes of the support
 * for the OS-specific Address Book.
 *
 * @author Lyubomir Marinov
 */
public class GenericSourceContact
    extends DataObject
    implements SourceContact
{
    /**
     * The <tt>ContactDetail</tt>s of this <tt>SourceContact</tt>.
     */
    protected final List<ContactDetail> contactDetails;

    /**
     * The <tt>ContactSourceService</tt> which has created this
     * <tt>SourceContact</tt>.
     */
    private final ContactSourceService contactSource;

    /**
     * The display name of this <tt>SourceContact</tt>.
     */
    private String displayName;

    /**
     * The display details of this contact.
     */
    private String displayDetails;

    /**
     * The presence status of this contact.
     */
    private PresenceStatus presenceStatus;

    /**
     * The image/avatar of this <tt>SourceContact</tt>
     */
    private BufferedImageFuture image;

    /**
     * Initializes a new <tt>AddrBookSourceContact</tt> instance.
     *
     * @param contactSource the <tt>ContactSourceService</tt> which is creating
     * the new instance
     * @param displayName the display name of the new instance
     * @param contactDetails the <tt>ContactDetail</tt>s of the new instance
     */
    public GenericSourceContact(
            ContactSourceService contactSource,
            String displayName,
            List<ContactDetail> contactDetails)
    {
        this.contactSource = contactSource;
        this.displayName = displayName;
        this.contactDetails = contactDetails;
    }

    /**
     * Gets the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt>.
     *
     * @return the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt>
     * @see SourceContact#getContactDetails()
     */
    public List<ContactDetail> getContactDetails()
    {
        return Collections.unmodifiableList(contactDetails);
    }

    /**
     * Gets the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt> which
     * support a specific <tt>OperationSet</tt>.
     *
     * @param operationSet the <tt>OperationSet</tt> the supporting
     * <tt>ContactDetail</tt>s of which are to be returned
     * @return the <tt>ContactDetail</tt>s of this <tt>SourceContact</tt> which
     * support the specified <tt>operationSet</tt>
     * @see SourceContact#getContactDetails(Class)
     */
    public List<ContactDetail> getContactDetails(
            Class<? extends OperationSet> operationSet)
    {
        List<ContactDetail> contactDetails = new LinkedList<>();

        for (ContactDetail contactDetail : getContactDetails())
        {
            List<Class<? extends OperationSet>> supportedOperationSets
                = contactDetail.getSupportedOperationSets();

            if ((supportedOperationSets != null)
                    && supportedOperationSets.contains(operationSet))
                contactDetails.add(contactDetail);
        }
        return contactDetails;
    }

    /**
     * Returns a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category.
     * @param category the <tt>OperationSet</tt> class we're looking for
     * @return a list of all <tt>ContactDetail</tt>s corresponding to the given
     * category
     */
    public List<ContactDetail> getContactDetails(
        ContactDetail.Category category)
    {
        List<ContactDetail> contactDetails = new LinkedList<>();

        for (ContactDetail contactDetail : getContactDetails())
        {
            if(contactDetail != null)
            {
                ContactDetail.Category detailCategory
                    = contactDetail.getCategory();
                if (detailCategory != null && detailCategory.equals(category))
                    contactDetails.add(contactDetail);
            }
        }
        return contactDetails;
    }

    /**
     * Gets the <tt>ContactSourceService</tt> which has created this
     * <tt>SourceContact</tt>.
     *
     * @return the <tt>ContactSourceService</tt> which has created this
     * <tt>SourceContact</tt>
     * @see SourceContact#getContactSource()
     */
    public ContactSourceService getContactSource()
    {
        return contactSource;
    }

    /**
     * Gets the display details of this <tt>SourceContact</tt>.
     *
     * @return the display details of this <tt>SourceContact</tt>
     * @see SourceContact#getDisplayDetails()
     */
    public String getDisplayDetails()
    {
        return displayDetails;
    }

    /**
     * Returns the details to be displayed in the tooltip of this
     * <tt>SourceContact</tt>.
     *
     * This method is required by <tt>CallHistorySourceContact</tt>, which
     * also implements <tt>SourceContact</tt>, as the details displayed in a
     * call history item's tooltip might not be the same as the normal display
     * details of the history item.  Here we just want to return the
     * displayDetails.
     *
     * @return the display details of this <tt>SourceContact</tt>
     * @see SourceContact#getTooltipDisplayDetails()
     */
    public String getTooltipDisplayDetails()
    {
        String toolTipDisplayDetails = "";
        if (!StringUtils.isNullOrEmpty(displayDetails))
        {
            toolTipDisplayDetails = "<p>" + displayDetails + "</p>";
        }
        return toolTipDisplayDetails;
    }

    /**
     * Sets the display details of this <tt>SourceContact</tt>.
     *
     * @param displayDetails the display details of this <tt>SourceContact</tt>
     */
    public String setDisplayDetails(String displayDetails)
    {
        return this.displayDetails = displayDetails;
    }

    /**
     * Gets the display name of this <tt>SourceContact</tt>.
     *
     * @return the display name of this <tt>SourceContact</tt>
     * @see SourceContact#getDisplayName()
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Sets the display name of this <tt>SourceContact</tt>.
     *
     * @param displayName The display name of this <tt>SourceContact</tt>
     */
    public void setDisplayName(String displayName)
    {
       this.displayName = displayName;
    }

    /**
     * Gets the image/avatar of this <tt>SourceContact</tt>.
     *
     * @return the image/avatar of this <tt>SourceContact</tt>
     * @see SourceContact#getImage()
     */
    public BufferedImageFuture getImage()
    {
        return image;
    }

    public String getImageDescription()
    {
        return null;
    }

    /**
     * Gets the preferred <tt>ContactDetail</tt> for a specific
     * <tt>OperationSet</tt>.
     *
     * @param operationSet the <tt>OperationSet</tt> to get the preferred
     * <tt>ContactDetail</tt> for
     * @return the preferred <tt>ContactDetail</tt> for the specified
     * <tt>operationSet</tt>
     * @see SourceContact#getPreferredContactDetail(Class)
     */
    public ContactDetail getPreferredContactDetail(
            Class<? extends OperationSet> operationSet)
    {
        List<ContactDetail> contactDetails = getContactDetails(operationSet);

        return contactDetails.isEmpty() ? null : contactDetails.get(0);
    }

    /**
     * Sets the image/avatar of this <tt>SourceContact</tt>.
     *
     * @param image the image/avatar to be set on this <tt>SourceContact</tt>
     */
    public void setImage(BufferedImageFuture image)
    {
        this.image = image;
    }

    /**
     * Returns the status of the source contact. And null if such information
     * is not available.
     * @return the PresenceStatus representing the state of this source contact.
     */
    public PresenceStatus getPresenceStatus()
    {
        return presenceStatus;
    }

    /**
     * Sets the status of the source contact.
     *
     * @param presenceStatus the status of this contact
     */
    public void setPresenceStatus(PresenceStatus presenceStatus)
    {
        this.presenceStatus = presenceStatus;
    }

    /**
     * Returns the index of this source contact in its parent.
     *
     * @return the index of this source contact in its parent
     */
    public int getIndex()
    {
        return -1;
    }

    public boolean isEnabled()
    {
        // Is always enabled
        return true;
    }

    public void setEnabled(boolean enabled)
    {
        // Does nothing
    }

    @Override
    public String getEmphasizedNumber()
    {
        return null;
    }

    @Override
    public Date getTimestamp()
    {
        return null;
    }

    @Override
    public boolean canBeMessaged()
    {
        return false;
    }

    /**
     * Returns a hash of the contact's display name so that we can log the
     * SourceContact without logging any PII.
     *
     * @return  the hashed display name of the SourceContact.
     */
    @Override
    public String toString()
    {
        return logHasher(getDisplayName());
    }
}
