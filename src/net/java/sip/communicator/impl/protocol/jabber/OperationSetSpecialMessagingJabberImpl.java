// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.sip.communicator.service.diagnostics.DiagnosticsServiceRegistrar;
import net.java.sip.communicator.service.diagnostics.StateDumper;
import net.java.sip.communicator.service.protocol.ImMessage;
import net.java.sip.communicator.service.protocol.OperationSetBasicInstantMessaging;
import net.java.sip.communicator.service.protocol.OperationSetSpecialMessaging;
import net.java.sip.communicator.util.Logger;

/**
 * Jabber implementation of the Special Messaging operation set.
 */
public class OperationSetSpecialMessagingJabberImpl
    implements OperationSetSpecialMessaging, StateDumper
{
    private static final Logger logger =
        Logger.getLogger(OperationSetSpecialMessagingJabberImpl.class);

    /**
     * A map from type of special message to the handler for that type
     */
    private final Map<String, List<SpecialMessageHandler>> specialMessageHandlers =
            new HashMap<>();

    /**
     * The string that is common to all 'special' messages that are used to
     * pass on custom system messages and should not be displayed to the user.
     */
    protected static final String SPECIAL_MESSAGE_COMMON_TEXT =
                                                   ".accession.metaswitch.com:";

    /**
     * Creates an instance of this operation set.
     */
    OperationSetSpecialMessagingJabberImpl()
    {
        logger.info("Creating jabber special message op set");
        // Register as a state dumper so we can dump the state of the special
        // message handlers when sending and error report.
        DiagnosticsServiceRegistrar.registerStateDumper(
            this, JabberActivator.bundleContext);
    }

    @Override
    public ImMessage createSpecialMessage(String messageType, String messageText)
    {
        // messageText contains the chat address of the sender/receiver
        logger.debug("Creating special message of type " + messageType +
                     " with content " + sanitisePeerId(messageText));

        String content = messageType +
                         SPECIAL_MESSAGE_COMMON_TEXT +
                         messageText;

        return new MessageJabberImpl(content,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_TYPE,
            OperationSetBasicInstantMessaging.DEFAULT_MIME_ENCODING,
            null,
            false,
            false,
            false);
    }

    @Override
    public void registerSpecialMessageHandler(SpecialMessageHandler handler,
                                              String... types)
    {
        synchronized (specialMessageHandlers)
        {
            for (String type : types)
            {
                logger.debug("Adding special message handler " + handler +
                                                           " for type " + type);
                List<SpecialMessageHandler> currentHandlers = specialMessageHandlers.get(type);
                if (currentHandlers == null)
                {
                    currentHandlers = new ArrayList<>();
                }
                currentHandlers.add(handler);
                specialMessageHandlers.put(type, currentHandlers);
            }
        }
    }

    @Override
    public void removeSpecialMessageHandler(String... types)
    {
        synchronized (specialMessageHandlers)
        {
            for (String type : types)
            {
                logger.debug("Removing special message handler for type " + type);
                specialMessageHandlers.remove(type);
            }
        }
    }

    @Override
    public List<SpecialMessageHandler> getSpecialMessageHandlers(String type)
    {
        synchronized (specialMessageHandlers)
        {
            return specialMessageHandlers.get(type);
        }
    }

    @Override
    public String getStateDumpName()
    {
        return "OperationSetSpecialInstantMessagingJabberImpl";
    }

    @Override
    public String getState()
    {
        StringBuilder builder = new StringBuilder();

        if (specialMessageHandlers.isEmpty())
        {
            builder.append("No special message handlers");
        }
        else
        {
            builder.append("Special message handlers:\n");
            for (String handlerType : specialMessageHandlers.keySet())
            {
                builder.append(handlerType).append(": ").append(
                    specialMessageHandlers.get(handlerType)).append("\n");
            }
        }

        return builder.toString();
    }
}
