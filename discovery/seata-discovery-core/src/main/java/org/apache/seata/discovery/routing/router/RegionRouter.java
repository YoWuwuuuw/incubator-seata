/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.discovery.routing.router;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.BitList;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.region.GeoLocation;
import org.apache.seata.discovery.routing.region.ServerWithDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Region router
 * Routes based on geographic region or distance
 */
public class RegionRouter extends AbstractStateRouter<ServiceInstance> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionRouter.class);
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private final int regionTopN;

    /**
     * Default constructor
     */
    public RegionRouter() {
        super("RegionRouter", true);
        this.regionTopN = fileConfig.getInt(ConfigurationKeys.CLIENT_REGION_ROUTER_TOP_N, 5);
    }

    @Override
    protected BitList<ServiceInstance> doRoute(BitList<ServiceInstance> servers, RoutingContext ctx) {
        // Get client location information
        GeoLocation geoLocation = getClientLocation(ctx);

        // If client location cannot be obtained, skip this router
        if (geoLocation == null) {
            LOGGER.error("Failed to get client location, skipping region router");
            return servers; // Return original server list, do not filter
        }

        // Calculate distance and sort
        List<ServerWithDistance<ServiceInstance>> sorted = servers.toList().stream()
                .map(server -> new ServerWithDistance<>(server, calculateDistance(geoLocation, server)))
                .sorted(Comparator.comparingDouble(ServerWithDistance::getDistance))
                .limit(regionTopN)
                .collect(Collectors.toList());

        // Convert to BitList
        List<ServiceInstance> selectedServers =
                sorted.stream().map(ServerWithDistance::getServer).collect(Collectors.toList());

        return BitList.fromList(selectedServers);
    }

    /**
     * Get client location
     * @param ctx routing context
     * @return client location, or null if not available
     */
    private GeoLocation getClientLocation(RoutingContext ctx) {
        // Get client location information from context
        Object clientLat = ctx.getAttribute("clientLat");
        Object clientLng = ctx.getAttribute("clientLng");

        if (clientLat != null && clientLng != null) {
            try {
                return new GeoLocation(
                        Double.parseDouble(clientLat.toString()), Double.parseDouble(clientLng.toString()));
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid client location format: lat={}, lng={}", clientLat, clientLng, e);
                return null;
            }
        }

        // Cannot get location information, return null
        LOGGER.warn("Client location not found in routing context");
        return null;
    }

    /**
     * Calculate distance
     * @param geoLocation client location
     * @param server server instance
     * @return distance
     */
    private double calculateDistance(GeoLocation geoLocation, ServiceInstance server) {
        // Get location information from server metadata
        Object serverLat = server.getMetadata().get("lat");
        Object serverLng = server.getMetadata().get("lng");

        if (serverLat != null && serverLng != null) {
            try {
                GeoLocation serverLocation = new GeoLocation(
                        Double.parseDouble(serverLat.toString()), Double.parseDouble(serverLng.toString()));
                return calculateHaversineDistance(geoLocation, serverLocation);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid server location format: lat={}, lng={}", serverLat, serverLng);
                return Double.MAX_VALUE;
            }
        }

        // If no location information, return max distance
        LOGGER.debug("No location metadata found for server");
        return Double.MAX_VALUE;
    }

    /**
     * Calculate Haversine distance between two points
     * @param loc1 location 1
     * @param loc2 location 2
     * @return distance (km)
     */
    private double calculateHaversineDistance(GeoLocation loc1, GeoLocation loc2) {
        final double r = 6371; // Earth radius (km)

        double latDistance = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double lngDistance = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(loc1.getLatitude()))
                        * Math.cos(Math.toRadians(loc2.getLatitude()))
                        * Math.sin(lngDistance / 2)
                        * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return r * c;
    }

    @Override
    public String buildSnapshot() {
        return String.format("RegionRouter: regionTopN=%d", regionTopN);
    }
}
