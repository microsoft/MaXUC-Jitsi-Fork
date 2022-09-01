// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.dns;

import java.util.Arrays;
import java.util.List;

import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.config.FallbackPropertyResolverConfigProvider;
import org.xbill.DNS.config.PropertyResolverConfigProvider;
import org.xbill.DNS.config.ResolvConfResolverConfigProvider;
import org.xbill.DNS.config.ResolverConfigProvider;
import org.xbill.DNS.config.SunJvmResolverConfigProvider;
import org.xbill.DNS.config.WindowsResolverConfigProvider;

/**
 * Utility methods for working with the DNSJava library.
 */
public final class DNSJavaUtils
{
    /**
     * Not to be instantiated.
     */
    private DNSJavaUtils()
    {
    }

    /**
     * Refresh DNSJava's ResolverConfig, including a workaround for https://github.com/dnsjava/dnsjava/issues/226.
     * @deprecated This workaround can probably be removed following DNSJava upgrade to v3.4.3 or later.
     */
    @Deprecated
    public static void refreshResolverConfig()
    {
        // Workaround for https://github.com/dnsjava/dnsjava/issues/226
        // Reinitialize DNSJava's ResolverConfigProvider objects, otherwise they retain the previous
        // DNS servers, meaning that outdated DNS servers (e.g. for a now-disconnected VPN) stay in the list.
        // This code is based on `checkInitialized` from https://github.com/dnsjava/dnsjava/blob/v3.4.2/src/main/java/org/xbill/DNS/ResolverConfig.java
        // but excluding resolvers we don't use: AndroidResolverConfigProvider and JndiContextResolverConfigProvider.
        final List<ResolverConfigProvider> configProviders = Arrays.asList(
            new PropertyResolverConfigProvider(),
            new ResolvConfResolverConfigProvider(),
            new WindowsResolverConfigProvider(),
            new SunJvmResolverConfigProvider(),
            new FallbackPropertyResolverConfigProvider()
        );
        ResolverConfig.setConfigProviders(configProviders);
        ResolverConfig.refresh();
    }
}
