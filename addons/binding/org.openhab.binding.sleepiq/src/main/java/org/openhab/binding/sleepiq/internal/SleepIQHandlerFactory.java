/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sleepiq.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.sleepiq.internal.discovery.SleepIQBedDiscoveryService;
import org.openhab.binding.sleepiq.internal.handler.SleepIQCloudHandler;
import org.openhab.binding.sleepiq.internal.handler.SleepIQDualBedHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link SleepIQHandlerFactory} is responsible for creating thing handlers.
 *
 * @author Gregory Moyer - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.sleepiq")
public class SleepIQHandlerFactory extends BaseThingHandlerFactory {
    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPE_UIDS = Sets
            .union(SleepIQCloudHandler.SUPPORTED_THING_TYPE_UIDS, SleepIQDualBedHandler.SUPPORTED_THING_TYPE_UIDS);

    private final Logger logger = LoggerFactory.getLogger(SleepIQHandlerFactory.class);

    private final Map<ThingUID, ServiceRegistration<?>> discoveryServiceReg = new HashMap<>();

    @Override
    public boolean supportsThingType(final ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(final Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SleepIQCloudHandler.SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            logger.debug("Creating SleepIQ cloud thing handler");
            SleepIQCloudHandler cloudHandler = new SleepIQCloudHandler((Bridge) thing);
            registerBedDiscoveryService(cloudHandler);
            return cloudHandler;
        } else if (SleepIQDualBedHandler.SUPPORTED_THING_TYPE_UIDS.contains(thingTypeUID)) {
            logger.debug("Creating SleepIQ dual bed thing handler");
            return new SleepIQDualBedHandler(thing);
        }

        return null;
    }

    @Override
    protected void removeHandler(final ThingHandler thingHandler) {
        logger.debug("Removing SleepIQ thing handler");

        if (thingHandler instanceof SleepIQCloudHandler) {
            unregisterBedDiscoveryService((SleepIQCloudHandler) thingHandler);
        }
    }

    /**
     * Register the given cloud handler to participate in discovery of new beds.
     *
     * @param cloudHandler the cloud handler to register (must not be <code>null</code>)
     */
    private synchronized void registerBedDiscoveryService(final SleepIQCloudHandler cloudHandler) {
        logger.debug("Registering bed discovery service");
        SleepIQBedDiscoveryService discoveryService = new SleepIQBedDiscoveryService(cloudHandler);
        discoveryServiceReg.put(cloudHandler.getThing().getUID(),
                bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>()));
    }

    /**
     * Unregister the given cloud handler from participating in discovery of new beds.
     *
     * @param cloudHandler the cloud handler to unregister (must not be <code>null</code>)
     */
    private synchronized void unregisterBedDiscoveryService(final SleepIQCloudHandler cloudHandler) {
        ThingUID thingUID = cloudHandler.getThing().getUID();
        ServiceRegistration<?> serviceReg = discoveryServiceReg.remove(thingUID);
        if (serviceReg != null) {
            logger.debug("Unregistering bed discovery service");
            serviceReg.unregister();
        }
    }
}
