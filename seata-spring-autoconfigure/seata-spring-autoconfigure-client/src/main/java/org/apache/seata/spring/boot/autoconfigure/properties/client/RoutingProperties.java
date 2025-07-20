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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.ROUTING_PREFIX;

@Component
@ConfigurationProperties(prefix = ROUTING_PREFIX)
public class RoutingProperties {
    /**
     * Whether enable routing feature
     */
    private boolean enabled = false;

    /**
     * Whether enable routing debug mode
     */
    private boolean debug = false;

    /**
     * Whether enable routing fallback strategy
     */
    private boolean fallback = true;

    /**
     * Router chain order, e.g. region-router,metadata-router-1,metadata-router-2
     */
    private String chainOrder = "";

    /**
     * Whether enable region router
     */
    private boolean regionRouterEnabled = true;

    /**
     * Top N servers for region router
     */
    private int regionRouterTopN = 5;

    /**
     * Dynamic metadata router configurations
     * Key: router name (e.g. metadata-router-1, metadata-router-2, custom-router)
     * Value: router configuration
     */
    private Map<String, MetadataRouterConfig> metadataRouters = new HashMap<>();

    /**
     * Primary backup router order
     */
    private String primaryBackupOrder = "region-router";

    /**
     * Whether enable primary backup router
     */
    private boolean primaryBackupEnabled = true;

    /**
     * Client location latitude, e.g. 39.9042
     */
    private String locationLat = "";

    /**
     * Client location longitude, e.g. 116.4074
     */
    private String locationLng = "";

    public boolean isEnabled() {
        return enabled;
    }

    public RoutingProperties setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public RoutingProperties setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isFallback() {
        return fallback;
    }

    public RoutingProperties setFallback(boolean fallback) {
        this.fallback = fallback;
        return this;
    }

    public String getChainOrder() {
        return chainOrder;
    }

    public RoutingProperties setChainOrder(String chainOrder) {
        this.chainOrder = chainOrder;
        return this;
    }

    public boolean isRegionRouterEnabled() {
        return regionRouterEnabled;
    }

    public RoutingProperties setRegionRouterEnabled(boolean regionRouterEnabled) {
        this.regionRouterEnabled = regionRouterEnabled;
        return this;
    }

    public int getRegionRouterTopN() {
        return regionRouterTopN;
    }

    public RoutingProperties setRegionRouterTopN(int regionRouterTopN) {
        this.regionRouterTopN = regionRouterTopN;
        return this;
    }

    public java.util.Map<String, MetadataRouterConfig> getMetadataRouters() {
        return metadataRouters;
    }

    public RoutingProperties setMetadataRouters(java.util.Map<String, MetadataRouterConfig> metadataRouters) {
        this.metadataRouters = metadataRouters;
        return this;
    }

    /**
     * Get metadata router configuration by name
     * @param routerName router name
     * @return metadata router configuration
     */
    public MetadataRouterConfig getMetadataRouter(String routerName) {
        return metadataRouters.get(routerName);
    }

    /**
     * Add metadata router configuration
     * @param routerName router name
     * @param config router configuration
     */
    public void addMetadataRouter(String routerName, MetadataRouterConfig config) {
        metadataRouters.put(routerName, config);
    }

    public String getPrimaryBackupOrder() {
        return primaryBackupOrder;
    }

    public RoutingProperties setPrimaryBackupOrder(String primaryBackupOrder) {
        this.primaryBackupOrder = primaryBackupOrder;
        return this;
    }

    public boolean isPrimaryBackupEnabled() {
        return primaryBackupEnabled;
    }

    public RoutingProperties setPrimaryBackupEnabled(boolean primaryBackupEnabled) {
        this.primaryBackupEnabled = primaryBackupEnabled;
        return this;
    }

    public String getLocationLat() {
        return locationLat;
    }

    public RoutingProperties setLocationLat(String locationLat) {
        this.locationLat = locationLat;
        return this;
    }

    public String getLocationLng() {
        return locationLng;
    }

    public RoutingProperties setLocationLng(String locationLng) {
        this.locationLng = locationLng;
        return this;
    }
}
