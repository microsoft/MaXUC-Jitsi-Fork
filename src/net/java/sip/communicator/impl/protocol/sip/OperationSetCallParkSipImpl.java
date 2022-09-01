// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.beans.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.ResourceManagementService;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.plugin.provisioning.*;
import net.java.sip.communicator.service.diagnostics.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * SIP implementation of the CallPark operation set.
 */
public class OperationSetCallParkSipImpl implements OperationSetCallPark, StateDumper
{
    private static final Logger logger =
        Logger.getLogger(OperationSetCallParkSipImpl.class);

    // Constants used in config strings.
    private static final String NAME = "name";
    private static final String DEPARTMENT = "department";
    private static final String DEPARTMENT_SHORT = "departmentshort";
    private static final String ENABLED = "enabled";

    private final ConfigurationService configService
        = SipActivator.getConfigurationService();

    private OperationSetBasicTelephonySipImpl opSetTelephonySip;

    private OperationSetPresenceSipImpl opSetPersistentPresence;

    private final List<CallParkListener> listeners = new ArrayList<>();

    /**
     * <tt>true</tt> if the config contains any call park orbits.  If there are
     * none, the user cannot enable call park in Accession.
     */
    private boolean callParkAvailable;

    /**
     * Set to <tt>true</tt> when the user enables call park in Accession.  Can
     * only be true if {@link #callParkAvailable} is <tt>true</tt>.
     */
    private boolean callParkEnabled;

    /**
     * Mapping from the orbit code to the underlying CallParkOrbit object
     */
    private final HashMap<String, CallParkOrbitImpl> orbits = new HashMap<>();

    /**
     * Map containing all the groups used for call park.  Each department or
     * sub-department is a ContactGroup.  The key is the full department name.
     */
    private HashMap<String, ContactGroupSipImpl> departmentGroups = new HashMap<>();

    /**
     * Lock that must be held when modifying or iterating over the
     * {@link #departmentGroups} map.
     */
    private final Object departmentGroupsLock = new Object();

    public OperationSetCallParkSipImpl(
        OperationSetBasicTelephonySipImpl opSetBasicTelephonySipImpl,
        OperationSetPresenceSipImpl opSetPersPresence)
    {
        callParkEnabled = configService.user().getBoolean(
            CALLPARK_CONFIG_PREFIX + "." + ENABLED, false);

        opSetTelephonySip = opSetBasicTelephonySipImpl;
        opSetPersistentPresence = opSetPersPresence;

        // Ensure call park is available. If it isn't, 'enabled' will also be
        // changed to false.
        callParkAvailable = true;
        checkCallParkAvailability();

        if (callParkEnabled)
        {
            // Create the objects in a new thread, so as not to slow down plugin
            // creation.
            logger.info("Create call park objects");
            new Thread("Create call park objects") {
                public void run()
                {
                    logger.debug("Refreshing call park config on new thread");
                    refreshOrbits();
                }
            }.start();
        }

        // Listen out for if the call park config changes.
        configService.user().addPropertyChangeListener(
            ProvisioningServiceImpl.LAST_PROVISIONING_UPDATE_TIME,
            new PropertyChangeListener()
            {
                @Override
                public void propertyChange(PropertyChangeEvent evt)
                {
                    logger.debug("Property changed: " + evt);
                    if (callParkEnabled) refreshOrbits();
                }
            });

        // Listen for presence notifications and pass them on to the appropriate
        // orbit
        opSetPersistentPresence.addContactPresenceStatusListener(
            new ContactPresenceStatusListener(){
                @Override
                public void contactPresenceStatusChanged(
                    ContactPresenceStatusChangeEvent evt)
                {
                    String orbitCode = evt.getSourceContact().getDisplayName();
                    logger.debug("Notification for " + orbitCode + " in state " + evt.getNewStatus());

                    CallParkOrbit orbit = orbits.get(orbitCode);

                    if (orbit != null)
                    {
                        orbit.onPresenceStatusChanged(evt);
                    }
                }
            });

        DiagnosticsServiceRegistrar.registerStateDumper(this, SipActivator.getBundleContext());
    }

    @Override
    public boolean isCallParkAvailable()
    {
        return callParkAvailable;
    }

    @Override
    public void setEnabled(final boolean enabled)
    {
        // This method can take a while to run, if we are on the EDT, then move
        // to a new thread:
        if (SwingUtilities.isEventDispatchThread())
        {
            new Thread("CallParkEnabledSwitchThread")
            {
                @Override
                public void run()
                {
                    setEnabled(enabled);
                }
            }.start();

            return;
        }

        boolean broadcast = false;

        if (enabled && !callParkEnabled)
        {
            logger.info("User has enabled call park");
            callParkEnabled = true;
            configService.user().setProperty(CALLPARK_CONFIG_PREFIX + "." + ENABLED, true);
            refreshOrbits();
            broadcast = true;
        }
        else if (!enabled && callParkEnabled)
        {
            logger.info("User has disabled call park");
            callParkEnabled = false;
            configService.user().setProperty(CALLPARK_CONFIG_PREFIX + "." + ENABLED, false);
            destroyOrbits();
            broadcast = true;
        }

        if (broadcast)
        {
            for (CallParkListener listener : getIterableListeners())
            {
                logger.debug("Call park enabled? " + callParkEnabled + ". Inform listener: " + listener);
                listener.onCallParkEnabledChanged();
            }
        }
    }

    @Override
    public boolean isEnabled()
    {
        return callParkEnabled;
    }

    @Override
    public CallParkOrbit getOrbitForContact(Contact contact)
    {
        return orbits.get(contact.getDisplayName());
    }

    @Override
    public String getFriendlyNameFromOrbitCode(String orbitCode)
    {
        if (!callParkAvailable || !callParkEnabled)
            return orbitCode;

        CallParkOrbit orbit = orbits.get(orbitCode);
        if (orbit != null)
        {
            return orbit.getFriendlyName();
        }
        else
        {
            logger.warn("Couldn't find orbit for code: " + orbitCode);
            return orbitCode;
        }
    }

    @Override
    public void registerListener(CallParkListener listener)
    {
        logger.debug("Add listener: " + listener);
        synchronized (listeners)
        {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(CallParkListener listener)
    {
        logger.debug("Remove listener: " + listener);
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Inform any listeners that this orbit has changed state.
     * @param orbit The CallParkOrbit that has changed state.
     * @param oldState The previous state of the orbit.
     */
    public void callParkStateChanged(OperationSetCallPark.CallParkOrbit orbit, CallParkOrbitState oldState)
    {
        for (CallParkListener listener : getIterableListeners())
        {
            listener.onOrbitStateChanged(orbit, oldState);
        }
    }

    /**
     * Helper method for calculating what display name to use for
     * @param nameFromConfig - orbit name defined in config (null if there is none)
     * @param deptShort - dept. name defined in config (null if there is none)
     * @param orbit
     * @return display name
     */
    private static String getFriendlyName(String nameFromConfig, String deptShort, String orbit)
    {
        String friendlyName;

        // Figure out a friendly name if no name is configured.
        if ((nameFromConfig != null) && !"".equals(nameFromConfig))
        {
            friendlyName = nameFromConfig;
        }
        else
        {
            ResourceManagementService res = SipActivator.getResources();
            if ((deptShort != null) && !"".equals(deptShort))
            {
                // <department> orbit <orbit code>
                friendlyName = res.getI18NString(
                    "impl.protocol.sip.CALL_PARK_FRIENDLY_NAME_DEPT",
                    new String[]{deptShort, orbit});
            }
            else
            {
                // Orbit <orbit code>
                friendlyName = res.getI18NString(
                    "impl.protocol.sip.CALL_PARK_FRIENDLY_NAME_NO_DEPT",
                    new String[]{orbit});
            }
        }

        return friendlyName;
    }

    /**
     * Get the latest config from the config store and rebuild any new or
     * changed orbits.
     */
    @VisibleForTesting
    void refreshOrbits()
    {
        boolean hasChanged = false;

        // We may end up creating new orbits - all (and only) the new orbits
        // will go into this map.
        HashMap<String, CallParkOrbitImpl> newOrbits =
                new HashMap<>();
        List<String> unChangedOrbits = new ArrayList<>();

        // We also check the (department) contact groups.  By the end of this
        // method, the following set will contain all the department groups that
        // should exist (including any that already existed) - any others can
        // then be removed.
        HashMap<String, ContactGroupSipImpl> newDeptGroups =
                new HashMap<>();

        List<String> orbitCodeKeys = configService.user().getPropertyNamesByPrefix(
            CALLPARK_ORBIT_CONFIG_PREFIX, true);

        checkCallParkAvailability(orbitCodeKeys);

        for (String orbitCodeKey : orbitCodeKeys)
        {
            String orbitCode = configService.user().getString(orbitCodeKey);
            String nameFromConfig = configService.user().getString(orbitCodeKey + "." + NAME);
            String departmentShort = configService.user().getString(orbitCodeKey + "." + DEPARTMENT_SHORT);

            // Calculate the name in case we have none in the config
            String name = getFriendlyName(nameFromConfig, departmentShort, orbitCode);

            // Provide a default for the case where we're not returned a
            // department (running against a back-level CFS).
            String department = configService.user().getString(orbitCodeKey + "." + DEPARTMENT, CALL_PARK_GROUP);

            /*
             * We want a group hierarchy as follows:
             * RootGroup
             *  - CallParkGroup
             *      - orbits in root department
             *      - top-level department A
             *          - orbits in department A
             *      (etc)
             *
             * The CallParkGroup is needed as the call park groups must be non-
             * persistent (to stop them being stored in contactlist.xml), but we
             * don't want to set the real RootGroup as non-persistent.
             *
             * By default, SIP PS returns top-level orbits as being in a
             * top-level group "None", which then appears as a sibling to the
             * other top-level groups.  To fix that, move the other groups into
             * group "None".
             */
            if (!CALL_PARK_GROUP.equals(department))
            {
                department = CALL_PARK_GROUP + " / " + department;
            }

            ContactGroupSipImpl group = getOrCreateGroup(department, newDeptGroups);
            CallParkOrbitImpl oldOrbit = orbits.get(orbitCode);

            boolean createNew = false;

            if (oldOrbit == null)
            {
                // A completely new orbit
                logger.debug("New orbit: " + orbitCode);
                createNew = true;
            }
            else  if (!oldOrbit.matches(orbitCode, name, department, departmentShort, group))
            {
                // An existing orbit, where display details have changed
                logger.debug("Orbit details changed: (" +
                    oldOrbit.getOrbitCode() + ", '" +
                    oldOrbit.getFriendlyName() + "') -> (" + orbitCode +
                    ", '" + name + "')");
                createNew = true;
            }
            else
            {
                // Mainline - we have an existing unchanged orbit for this
                // orbit code so won't need to delete the old one.
                logger.debug("Unchanged orbit: " + orbitCode);
                unChangedOrbits.add(orbitCode);
            }

            if (createNew)
            {
                logger.debug("Create new orbit");
                hasChanged = true;
                CallParkOrbitImpl newOrbit = new CallParkOrbitImpl(
                                                        this,
                                                        opSetTelephonySip,
                                                        opSetPersistentPresence,
                                                        orbitCode,
                                                        name,
                                                        department,
                                                        departmentShort,
                                                        group);

                newOrbits.put(orbitCode, newOrbit);
            }
        }

        // Tear down any old orbits that are no longer required
        for (Iterator<Map.Entry<String, CallParkOrbitImpl>> it = orbits.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, CallParkOrbitImpl> entry = it.next();
            CallParkOrbitImpl oldOrbit = entry.getValue();

            if (!unChangedOrbits.contains(oldOrbit.getOrbitCode()))
            {
                logger.debug("Remove " + (newOrbits.containsKey(oldOrbit.getOrbitCode()) ? "changed" : "old") +
                    " orbit: " + oldOrbit.getOrbitCode());
                oldOrbit.destroyContact();
                it.remove();
                hasChanged = true;
            }
        }

        // Create the contacts for any new orbits.  We must do this after
        // tearing down the old ones to ensure we remain subscribed to LSM for
        // orbits where only display details have changed.
        for (CallParkOrbitImpl newOrbit : newOrbits.values())
        {
            logger.debug("Add new orbit: " + newOrbit.getOrbitCode());
            newOrbit.createContact();
            orbits.put(newOrbit.getOrbitCode(), newOrbit);
            hasChanged = true;
        }

        // Now we've created all the new orbits, we can tidy up any department
        // groups that are no longer in use.
        synchronized (departmentGroupsLock)
        {
            for (String oldGroupName : departmentGroups.keySet())
            {
                if (!newDeptGroups.containsKey(oldGroupName))
                {
                    // Group is not in the new groups set - it should be destroyed.
                    ContactGroupSipImpl oldGroup = departmentGroups.get(oldGroupName);
                    logger.debug("Remove old department group " + oldGroup);
                    ((ContactGroupSipImpl)oldGroup.getParentContactGroup()).removeSubGroup(oldGroup);
                }
            }

            departmentGroups = newDeptGroups;
        }

        if (hasChanged)
        {
            // Ensure we're subscribed to LSM for all the orbits, and then
            // inform the listeners.
            logger.debug("Config has changed - will update " + listeners.size() + " listeners");
            opSetPersistentPresence.refreshPresenceSubscriptions();

            for (CallParkListener listener : getIterableListeners())
            {
                listener.onOrbitListChanged();
            }
        }
    }

    /**
     * Find or create the contact group for this department (creating ancestor
     * departments first as necessary), and add the newly-created groups to the
     * passed-in set of groups.
     * @param fullName
     * @param newDeptGroups
     * @return The contact group for this department
     */
    private ContactGroupSipImpl getOrCreateGroup(String fullName,
        HashMap<String, ContactGroupSipImpl> newDeptGroups)
    {
        ContactGroupSipImpl group = newDeptGroups.get(fullName);

        if (group == null)
        {
            // We haven't seen this group yet (in this call to refreshConfig()),
            // but did it already exist?
            group = departmentGroups.get(fullName);

            if (group == null)
            {
                // We need to create this group, but we can only do that if its
                // parent already exists.  Check that and create the parent if
                // necessary.  The full name of a department (e.g. "A / B / C")
                // breaks down as "<full parent name> / <dept short name>".
                // If the parent is not a root, creating it will create its
                // ancestors too.
                int lastSlashIdx = fullName.lastIndexOf(" / ");

                if (lastSlashIdx != -1)
                {
                    // Create the parent if necessary
                    String parentName = fullName.substring(0, lastSlashIdx);
                    String shortName = fullName.substring(
                              lastSlashIdx + " / ".length(), fullName.length());

                    logger.debug("Create " + shortName + " as a child of " + parentName);
                    ContactGroupSipImpl parentGroup = getOrCreateGroup(parentName, newDeptGroups);
                    group = createNewContactGroup(shortName, parentGroup, opSetTelephonySip.getProtocolProvider());
                }
                else
                {
                    // This is the root department
                    logger.debug("Create root department group: " + fullName);
                    ContactGroupSipImpl parentGroup = (ContactGroupSipImpl)
                       opSetPersistentPresence.getServerStoredContactListRoot();
                    group = createNewContactGroup(
                                       fullName,
                                       parentGroup,
                                       opSetTelephonySip.getProtocolProvider());
                }
            }

            newDeptGroups.put(fullName, group);
        }

        return group;
    }

    /**
     * Create a new contact group for a (call park) department.
     * @param groupName
     * @param parentGroup
     * @param protocolProvider
     * @return The newly-created group
     */
    private ContactGroupSipImpl createNewContactGroup(
                                String groupName,
                                ContactGroupSipImpl parentGroup,
                                ProtocolProviderServiceSipImpl protocolProvider)
    {
        ContactGroupSipImpl group = new ContactGroupSipImpl(groupName, protocolProvider);

        // We don't want to store Call Park department groups (and their child contacts)
        group.setPersistent(false);

        parentGroup.addSubgroup(group);
        return group;
    }

    /**
     * Check whether Call Park is available, and update our flag accordingly.
     * Call Park is available if the config contains both the Park and Retrieve
     * access codes, as well as details of at least one park orbit.
     */
    private void checkCallParkAvailability()
    {
        List<String> orbitCodes = configService.user().getPropertyNamesByPrefix(
            CALLPARK_ORBIT_CONFIG_PREFIX, true);
        checkCallParkAvailability(orbitCodes);
    }

    /**
     * Check whether Call Park is available, and update our flag accordingly.
     * Call Park is available if the config contains both the Park and Retrieve
     * access codes, as well as details of at least one park orbit.
     * @param orbitCodes
     */
    private void checkCallParkAvailability(List<String> orbitCodes)
    {
        if (orbitCodes.size() == 0)
        {
            logger.info("No call park config - set call park to unavailable");
            setAvailable(false);
            return;
        }
        else
        {
            logger.debug("Orbits are available");

            String retrieveCode = (String) configService.user().getProperty(OperationSetCallPark.RETRIEVE_CODE_KEY);

            if (retrieveCode == null)
            {
                logger.info("Didn't get call park 'retrieve' access code.");
                setAvailable(false);
            }
            else
            {
                setAvailable(true);
            }
        }
    }

    /**
     * Called when the availability of call park changes.
     * @param available
     */
    private void setAvailable(boolean available)
    {
        if (!available && callParkEnabled)
        {
            logger.info("Disabling call park as it is no longer available");
            setEnabled(false);
        }

        if (callParkAvailable != available)
        {
            logger.info("Call Park availability changed to " + available);
            callParkAvailable = available;
            for (CallParkListener listener : getIterableListeners())
            {
                listener.onCallParkAvailabilityChanged();
            }
        }
    }

    /**
     * Unsubscribe from LSM for all contacts.
     */
    private void destroyOrbits()
    {
        logger.debug("Destroy all orbits");

        for (CallParkOrbitImpl orbit : orbits.values())
        {
            orbit.destroyContact();
        }

        orbits.clear();
    }

    /**
     * Get a copy of the listeners list that can be iterated over, without
     * risking a ConcurrentModificationException if the list is changed while
     * we're iterating over it.
     * @return List of the available <tt>CallParkListener</tt>s.
     */
    private List<CallParkListener> getIterableListeners()
    {
        synchronized (listeners)
        {
            return new ArrayList<>(listeners);
        }
    }

    @Override
    public String getStateDumpName()
    {
        return "Call Park";
    }

    @Override
    public String getState()
    {
        StringBuilder builder = new StringBuilder();

        builder.append("Call park available? " + callParkAvailable + "\n");
        builder.append("Call park enabled? " + callParkEnabled + "\n");
        builder.append("Listeners (" + listeners.size() + "):\n" + listeners + "\n");

        // Print two sets of details for departments - first a summary, and then
        // more details.
        synchronized (departmentGroupsLock)
        {
            builder.append("Department groups:\n");
            for (String groupName : departmentGroups.keySet())
            {
                // "  <name> (3 contacts, 0 subgroups)"
                ContactGroupSipImpl deptGroup = departmentGroups.get(groupName);
                builder.append("  ").append(groupName).append(" (");
                builder.append(deptGroup.countContacts()).append(" contacts, ");
                builder.append(deptGroup.countSubgroups()).append(" subgroups)\n");
            }

            builder.append("Department groups detail:\n");
            for (ContactGroupSipImpl deptGroup : departmentGroups.values())
            {
                // Full object detail (which includes contents of all groups and
                // subgroups).
                builder.append(deptGroup).append("\n");
            }
        }

        builder.append("Orbits:\n");
        for (CallParkOrbitImpl orbit : orbits.values())
        {
            // "  *23 'Orbit *23' in state BUSY"
            builder.append("  ").append(orbit.getOrbitCode()).append(" '");
            builder.append(orbit.getFriendlyName()).append("' in state ");
            builder.append(orbit.getState()).append("\n");
        }

        return builder.toString();
    }
}
