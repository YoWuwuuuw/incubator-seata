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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.BitList;
import org.apache.seata.discovery.routing.RouterSnapshotNode;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.StateRouter;
import org.apache.seata.discovery.routing.config.RoutingProperties;
import org.apache.seata.discovery.routing.router.MetadataRouter;
import org.apache.seata.discovery.routing.router.RegionRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses chain of responsibility pattern to encapsulate multiple routers
 */
public class DefaultRouterChain implements RouterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRouterChain.class);

    private final List<StateRouter<ServiceInstance>> routers = new ArrayList<>();

    private final boolean fallbackToAny;
    private final boolean debugEnabled;

    /**
     * Default constructor
     */
    public DefaultRouterChain() {
        this.fallbackToAny = RoutingProperties.isRoutingFallbackEnabled();
        this.debugEnabled = RoutingProperties.isRoutingDebugEnabled();
        loadRouters(RoutingProperties.getRouterChainOrder());
    }

    /**
     * Constructor with specified router order
     *
     * @param routerOrder router order string
     */
    public DefaultRouterChain(String routerOrder) {
        this.fallbackToAny = RoutingProperties.isRoutingFallbackEnabled();
        this.debugEnabled = RoutingProperties.isRoutingDebugEnabled();
        loadRouters(routerOrder);
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
        if (servers == null || servers.isEmpty()) {
            return servers;
        }

        BitList<ServiceInstance> bitList = BitList.fromList(servers);
        BitList<ServiceInstance> result = route(bitList, ctx);
        return result.toList();
    }

    /**
     * Execute routing filter (internal method)
     *
     * @param servers service instances list
     * @param ctx     routing context
     * @return filtered service instances list
     */
    private BitList<ServiceInstance> route(BitList<ServiceInstance> servers, RoutingContext ctx) {
        BitList<ServiceInstance> result = servers;
        List<RouterSnapshotNode<ServiceInstance>> snapshots = debugEnabled ? new ArrayList<>() : null;

        for (StateRouter<ServiceInstance> router : routers) {
            result = router.route(result, ctx, debugEnabled, snapshots);

            if (result.isEmpty()) {
                if (fallbackToAny) {
                    LOGGER.warn("Router chain produced empty result, falling back to all servers");
                    return servers;
                } else {
                    LOGGER.warn("Router chain produced empty result, no fallback allowed");
                    return result;
                }
            }
        }

        // If debug mode is enabled, print complete snapshot information
        if (debugEnabled && snapshots != null && !snapshots.isEmpty()) {
            logRouterChainSnapshot(snapshots, servers.size(), result.size());
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
    private void logRouterChainSnapshot(
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

            // Show snapshot details
            if (snapshot.getSnapshot() != null && !snapshot.getSnapshot().isEmpty()) {
                sb.append("    Details: ").append(snapshot.getSnapshot()).append("\n");
            }
        }

        sb.append("=== End Debug ===\n");
        LOGGER.info(sb.toString());
    }

    /**
     * Load routers
     *
     * @param routerOrder router order string
     */
    private void loadRouters(String routerOrder) {
        // Parse router order
        List<String> chainOrder = parseRouterOrder(routerOrder);

        // Load routers according to configuration order
        for (String routerName : chainOrder) {
            if (isRouterEnabled(routerName)) {
                List<StateRouter<ServiceInstance>> routerInstances = createRoutersByName(routerName);
                routers.addAll(routerInstances);
            }
        }

        // Build responsibility chain
        for (int i = 0; i < routers.size() - 1; i++) {
            routers.get(i).setNext(routers.get(i + 1));
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
            // Check if specific metadata-router is enabled
            return RoutingProperties.isMetadataRouterEnabled(routerName);
        }

        switch (routerName) {
            case "region-router":
                return RoutingProperties.isRegionRouterEnabled();
            case "metadata-router":
                return RoutingProperties.isMetadataRouterEnabled();
            default:
                return true; // SPI routers are enabled by default
        }
    }

    /**
     * Create routers by name
     *
     * @param routerName router name
     * @return router instances list
     */
    @SuppressWarnings("unchecked")
    private List<StateRouter<ServiceInstance>> createRoutersByName(String routerName) {
        List<StateRouter<ServiceInstance>> routerInstances = new ArrayList<>();

        switch (routerName) {
            case "region-router":
                routerInstances.add(new RegionRouter());
                break;
            case "metadata-router":
                routerInstances.add(new MetadataRouter());
                break;
            default:
                if (routerName.startsWith("metadata-router-")) {
                    // Create specific metadata-router instance
                    MetadataRouter router = new MetadataRouter(routerName);
                    routerInstances.add(router);
                } else {
                    // Try to load custom router via SPI
                    try {
                        StateRouter<ServiceInstance> customRouter =
                                EnhancedServiceLoader.load(StateRouter.class, routerName);
                        if (customRouter != null) {
                            routerInstances.add(customRouter);
                        } else {
                            LOGGER.warn(
                                    "Custom router '{}' specified in configuration but not found via SPI", routerName);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load custom router: {}", routerName, e);
                    }
                }
                break;
        }

        return routerInstances;
    }

    /**
     * Parse router order
     *
     * @param routerOrder router order string
     * @return router names list
     */
    public static List<String> parseRouterOrder(String routerOrder) {
        List<String> order = new ArrayList<>();

        if (routerOrder != null && !routerOrder.trim().isEmpty()) {
            String[] orderNames = routerOrder.split(",");
            for (String orderName : orderNames) {
                String trimmedName = orderName.trim();
                if (!trimmedName.isEmpty()) {
                    order.add(trimmedName);
                }
            }
        }

        // Validate configuration validity
        if (order.isEmpty()) {
            LOGGER.warn("Router order configuration is invalid or empty, using fallback strategy");
            order.add("region-router");
            order.add("metadata-router");
        } else {
            // Validate router name validity
            List<String> validRouters = new ArrayList<>();
            for (String routerName : order) {
                if (RouterChainUtils.isValidRouterName(routerName)) {
                    validRouters.add(routerName);
                } else {
                    LOGGER.warn("Invalid router name in configuration: {}, skipping", routerName);
                }
            }

            if (validRouters.isEmpty()) {
                LOGGER.warn("No valid routers found in configuration, using fallback strategy");
                validRouters.add("region-router");
                validRouters.add("metadata-router");
            }

            order = validRouters;
        }

        return order;
    }
}
