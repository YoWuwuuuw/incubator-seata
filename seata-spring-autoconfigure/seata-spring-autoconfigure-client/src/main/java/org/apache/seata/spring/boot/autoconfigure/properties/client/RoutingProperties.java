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
     * Dynamic metadata router configurations
     * Key: router name (e.g. metadata-router-1, metadata-router-2, custom-router)
     * Value: router configuration
     */
    private Map<String, MetadataRouterConfig> metadataRouters = new HashMap<>();

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
}
