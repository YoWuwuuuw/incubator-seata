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
package org.apache.seata.discovery.routing.config;

import org.apache.seata.config.ConfigurationFactory;

/**
 * Manage routing-related configuration items
 */
public class RoutingProperties {

    /**
     * Routing feature switch
     */
    public static final String ROUTING_ENABLED = "client.routing.enabled";

    /**
     * Routing debug mode
     */
    public static final String ROUTING_DEBUG = "client.routing.debug";

    /**
     * Routing fallback strategy
     */
    public static final String ROUTING_FALLBACK = "client.routing.fallback";

    /**
     * Client routing location latitude
     */
    public static final String CLIENT_ROUTING_LOCATION_LAT = "client.routing.location.lat";

    /**
     * Client routing location longitude
     */
    public static final String CLIENT_ROUTING_LOCATION_LNG = "client.routing.location.lng";

    /**
     * Region router configuration
     */
    public static final String REGION_ROUTER_ENABLED = "client.routing.region-router.enabled";

    public static final String REGION_ROUTER_TOP_N = "client.routing.region-router.topN";

    /**
     * Metadata router configuration
     */
    public static final String METADATA_ROUTER_ENABLED = "client.routing.metadata-router.enabled";

    public static final String METADATA_ROUTER_EXPRESSION = "client.routing.metadata-router.expression";

    /**
     * Router chain order configuration
     */
    public static final String ROUTER_CHAIN_ORDER = "client.routing.chain.order";

    /**
     * Primary backup chain configuration
     */
    public static final String PRIMARY_BACKUP_ENABLED = "client.routing.primary-backup.enabled";

    public static final String PRIMARY_BACKUP_ORDER = "client.routing.primary-backup.order";

    /**
     * Check if routing feature is enabled
     * @return whether enabled
     */
    public static boolean isRoutingEnabled() {
        return ConfigurationFactory.getInstance().getBoolean(ROUTING_ENABLED, false);
    }

    /**
     * Check if routing debug mode is enabled
     * @return whether enabled
     */
    public static boolean isRoutingDebugEnabled() {
        return ConfigurationFactory.getInstance().getBoolean(ROUTING_DEBUG, false);
    }

    /**
     * Check if routing fallback strategy is enabled
     * @return whether enabled
     */
    public static boolean isRoutingFallbackEnabled() {
        return ConfigurationFactory.getInstance().getBoolean(ROUTING_FALLBACK, true);
    }

    /**
     * Get client routing location latitude
     * @return latitude
     */
    public static double getClientRoutingLocationLat() {
        return Double.parseDouble(ConfigurationFactory.getInstance().getConfig(CLIENT_ROUTING_LOCATION_LAT, "0.0"));
    }

    /**
     * Get client routing location longitude
     * @return longitude
     */
    public static double getClientRoutingLocationLng() {
        return Double.parseDouble(ConfigurationFactory.getInstance().getConfig(CLIENT_ROUTING_LOCATION_LNG, "0.0"));
    }

    /**
     * Check if region router is enabled
     * @return whether enabled
     */
    public static boolean isRegionRouterEnabled() {
        return ConfigurationFactory.getInstance().getBoolean(REGION_ROUTER_ENABLED, false);
    }

    /**
     * Get region router TopN configuration
     * @return TopN value
     */
    public static int getRegionRouterTopN() {
        return ConfigurationFactory.getInstance().getInt(REGION_ROUTER_TOP_N, 5);
    }

    /**
     * Check if metadata router is enabled
     * @return whether enabled
     */
    public static boolean isMetadataRouterEnabled() {
        return ConfigurationFactory.getInstance().getBoolean(METADATA_ROUTER_ENABLED, false);
    }

    /**
     * Get metadata router expression
     * @return expression
     */
    public static String getMetadataRouterExpression() {
        return ConfigurationFactory.getInstance().getConfig(METADATA_ROUTER_EXPRESSION, "");
    }

    /**
     * Get router chain order
     * @return chain order string
     */
    public static String getRouterChainOrder() {
        return ConfigurationFactory.getInstance().getConfig(ROUTER_CHAIN_ORDER);
    }

    /**
     * Check if primary backup chain is enabled
     * @return whether enabled
     */
    public static boolean isPrimaryBackupEnabled() {
        return ConfigurationFactory.getInstance().getBoolean(PRIMARY_BACKUP_ENABLED, false);
    }

    /**
     * Get primary backup chain order
     * @return primary backup chain order string
     */
    public static String getPrimaryBackupOrder() {
        return ConfigurationFactory.getInstance().getConfig(PRIMARY_BACKUP_ORDER);
    }

    /**
     * Check if specific metadata router is enabled
     * @param routerName router name
     * @return whether enabled
     */
    public static boolean isMetadataRouterEnabled(String routerName) {
        if ("metadata-router".equals(routerName)) {
            return isMetadataRouterEnabled();
        }

        // For metadata-router-1, metadata-router-2, etc.
        if (routerName.startsWith("metadata-router-")) {
            String configKey = "client.routing." + routerName + ".enabled";
            return ConfigurationFactory.getInstance().getBoolean(configKey, true);
        }

        return false;
    }

    /**
     * Get specific metadata router expression
     * @param routerName router name
     * @return expression
     */
    public static String getMetadataRouterExpression(String routerName) {
        if ("metadata-router".equals(routerName)) {
            return getMetadataRouterExpression();
        }

        // For metadata-router-1, metadata-router-2, etc.
        if (routerName.startsWith("metadata-router-")) {
            String configKey = "client.routing." + routerName + ".expression";
            return ConfigurationFactory.getInstance().getConfig(configKey, "");
        }

        return "";
    }
}
