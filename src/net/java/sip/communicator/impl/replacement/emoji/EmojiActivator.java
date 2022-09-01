/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.replacement.emoji;

import java.util.Hashtable;

import org.jitsi.service.resources.ResourceManagementService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import net.java.sip.communicator.service.replacement.ChatPartReplacementService;
import net.java.sip.communicator.service.replacement.TabbedImagesSelectorService;
import net.java.sip.communicator.util.Logger;

/**
 * Activator for the emoji source bundle.
 * @author Harsh Bakshi
 */
public class EmojiActivator
    implements BundleActivator
    {
        /**
         * The <tt>Logger</tt> used by the <tt>EmojiActivator</tt>
         * class.
         */
        private static final Logger logger =
            Logger.getLogger(EmojiActivator.class);

        /**
         * The currently valid bundle context.
         */
        private static BundleContext bundleContext = null;

        /**
         * The resources service
         */
        private static ResourceManagementService resourcesService;

        /**
         * The emoji service registration.
         */
        private ServiceRegistration<?> emojiServReg = null;

        /**
         *  The emoji inserter service
         */
        private static EmojiInserter emojiInserter = null;

        private static EmojiReplacementService emojiReplacer = null;

        /**
         * Starts the emoji replacement source bundle
         *
         * @param context the <tt>BundleContext</tt> as provided from the OSGi
         * framework
         */
        public void start(BundleContext context)
        {
            bundleContext = context;

            Hashtable<String, String> hashtable = new Hashtable<>();
            hashtable.put(ChatPartReplacementService.SOURCE_NAME,
                EmojiReplacementService.EMOJI_REPLACEMENT_SERVICE);

            emojiReplacer = new EmojiReplacementService();

            emojiServReg
                = context.registerService(ChatPartReplacementService.class.getName(),
                    emojiReplacer, hashtable);

            hashtable.put(TabbedImagesSelectorService.SOURCE_NAME,
                EmojiInserter.EMOJI_INSERTER_SERVICE);

            emojiInserter = new EmojiInserter(emojiReplacer);

            emojiServReg
            = context.registerService(TabbedImagesSelectorService.class.getName(),
                emojiInserter, hashtable);

            EmojiResources.loadEmojiImages();

            logger.info("Emoji source implementation [STARTED].");
        }

        /**
         * Unregisters the Emoji replacement service.
         *
         * @param context BundleContext
         */
        public void stop(BundleContext context)
        {
            emojiServReg.unregister();
            logger.info("Emoji source implementation [STOPPED].");
        }

        /**
         * Returns the <tt>ResourceManagementService</tt>, through which we will
         * access all resources.
         *
         * @return the <tt>ResourceManagementService</tt>, through which we will
         *         access all resources.
         */
        public static ResourceManagementService getResources()
        {
            if (resourcesService == null)
            {
                ServiceReference<?> serviceReference =
                    bundleContext
                        .getServiceReference(ResourceManagementService.class
                            .getName());

                if (serviceReference == null)
                    return null;

                resourcesService =
                    (ResourceManagementService) bundleContext
                        .getService(serviceReference);
            }
            return resourcesService;
        }
    }
