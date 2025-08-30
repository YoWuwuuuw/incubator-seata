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
package org.apache.seata.discovery.routing.chain;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.RouterSnapshotNode;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.StateRouter;
import org.apache.seata.discovery.routing.router.MetadataRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses chain of responsibility pattern to encapsulate multiple routers
 */
public class DefaultRouterChain implements RouterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRouterChain.class);
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private final List<StateRouter<ServiceInstance>> routers = new ArrayList<>();

    private final boolean debugEnabled;

    /**
     * Default constructor
     */
    public DefaultRouterChain() {
        this.debugEnabled = fileConfig.getBoolean(ConfigurationKeys.CLIENT_ROUTING_DEBUG, false);
        loadRouters();
    }

    /**
     * Execute routing filter
     *
     * @param servers service instances list
     * @param ctx     routing context
     * @return filtered service instances list
     */
    @Override
    public List<ServiceInstance> filterAll(List<ServiceInstance> servers, RoutingContext ctx) {
        return route(servers, ctx);
    }

    /**
     * Execute routing filter (internal method)
     *
     * @param servers service instances list
     * @param ctx     routing context
     * @return filtered service instances list
     */
    private List<ServiceInstance> route(List<ServiceInstance> servers, RoutingContext ctx) {
        List<ServiceInstance> result = servers;
        List<RouterSnapshotNode<ServiceInstance>> snapshots = debugEnabled ? new ArrayList<>() : null;

        for (StateRouter<ServiceInstance> router : routers) {
            result = router.route(result, ctx, snapshots);
        }

        // If debug mode is enabled, print complete snapshot information
        if (debugEnabled && !snapshots.isEmpty()) {
            buildRouterChainSnapshot(snapshots, servers.size(), result.size());
        }

        return result;
    }

    /**
     * Print router chain debug information
     *
     * @param snapshots   snapshot list
     * @param initialSize initial server count
     * @param finalSize   final server count
     */
    private void buildRouterChainSnapshot(
            List<RouterSnapshotNode<ServiceInstance>> snapshots, int initialSize, int finalSize) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Router Chain Debug ===\n");
        sb.append(String.format("Initial servers: %d, Final servers: %d\n", initialSize, finalSize));
        sb.append("Router execution details:\n");

        for (int i = 0; i < snapshots.size(); i++) {
            RouterSnapshotNode<ServiceInstance> snapshot = snapshots.get(i);
            sb.append(String.format(
                    "  [%d] %s: %d -> %d servers\n",
                    i + 1, snapshot.getRouterName(), snapshot.getInputSize(), snapshot.getOutputSize()));

            // If output count is small, show selected servers
            if (snapshot.getOutputSize() <= 5 && snapshot.getOutputSize() > 0) {
                sb.append("    Selected: ")
                        .append(snapshot.getSelectedServers())
                        .append("\n");
            }
        }

        sb.append("=== End Debug ===\n");
        LOGGER.info(sb.toString());
    }

    /**
     * Load routers - simplified to only load metadata routers
     */
    private void loadRouters() {
        // Load default metadata router if enabled
        if (isRouterEnabled("metadata-router")) {
            StateRouter<ServiceInstance> defaultRouter = new MetadataRouter();
            routers.add(defaultRouter);
        }

        // Load additional metadata routers (metadata-router-1, metadata-router-2, etc.)
        // Limit to a reasonable number to avoid infinite loops
        int maxRouters = 10;
        int index = 1;
        while (index <= maxRouters) {
            String routerName = "metadata-router-" + index;
            if (isRouterEnabled(routerName)) {
                StateRouter<ServiceInstance> router = new MetadataRouter(routerName);
                routers.add(router);
                index++;
            } else {
                break;
            }
        }

        // Load custom routers via SPI
        try {
            List<StateRouter> customRouters = EnhancedServiceLoader.loadAll(StateRouter.class);
            for (StateRouter customRouter : customRouters) {
                if (customRouter != null) {
                    routers.add((StateRouter<ServiceInstance>) customRouter);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load custom routers via SPI", e);
        }
    }

    /**
     * Check if router is enabled
     *
     * @param routerName router name
     * @return whether enabled
     */
    private boolean isRouterEnabled(String routerName) {
        if (routerName.startsWith("metadata-router-")) {
            String configKey = "client.routing." + routerName + ".enabled";
            return fileConfig.getBoolean(configKey, true);
        }

        if ("metadata-router".equals(routerName)) {
            return fileConfig.getBoolean(ConfigurationKeys.CLIENT_METADATA_ROUTER_ENABLED, true);
        }

        return true; // Custom routers are enabled by default
    }
}
