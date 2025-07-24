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

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.routing.RoutingContext;
import org.apache.seata.discovery.routing.config.RoutingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Automatically switches to backup chain when primary chain filtering result is empty
 */
public class PrimaryBackupRouterChain implements RouterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryBackupRouterChain.class);

    private final DefaultRouterChain primaryChain;
    private final DefaultRouterChain backupChain;

    private volatile boolean hasSwitchedToBackup = false; // Whether already switched to backup chain

    /**
     * Constructor
     */
    public PrimaryBackupRouterChain() {
        // Use clearer naming
        String primaryChainOrder = RoutingProperties.getPrimaryBackupOrder();
        String fallbackChainOrder = RoutingProperties.getRouterChainOrder();

        this.primaryChain = createRouterChain(primaryChainOrder, "primary");
        this.backupChain = createRouterChain(fallbackChainOrder, "backup");

        // If the primary chain is invalid, try to use the backup chain as the primary
        if (this.primaryChain == null && this.backupChain != null) {
            LoggerFactory.getLogger(PrimaryBackupRouterChain.class)
                    .warn("Primary chain is invalid, using backup chain as primary");
        }

        // Ensure both chains are not null
        if (this.primaryChain == null) {
            LoggerFactory.getLogger(PrimaryBackupRouterChain.class)
                    .warn("Primary and backup chain are invalid, using default chain");
        }
    }

    private DefaultRouterChain createRouterChain(String order, String chainType) {
        if (order == null || order.trim().isEmpty()) {
            LoggerFactory.getLogger(PrimaryBackupRouterChain.class).warn("{} chain order is not configured", chainType);
            return null;
        }
        if (!isValidRouterOrder(order)) {
            LoggerFactory.getLogger(PrimaryBackupRouterChain.class)
                    .warn("{} chain order configuration is invalid: {}", chainType, order);
            return null;
        }
        return new DefaultRouterChain(order);
    }

    /**
     * Execute routing filtering
     *
     * @param servers service instance list
     * @param ctx     routing context
     * @return filtered service instance list
     */
    @Override
    public List<ServiceInstance> filterAll(List<ServiceInstance> servers, RoutingContext ctx) {
        if (servers == null || servers.isEmpty()) {
            return servers;
        }

        // If already switched to backup chain, use backup chain directly
        if (hasSwitchedToBackup) {
            List<ServiceInstance> backupResult = backupChain.filterAll(servers, ctx);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Using backup chain (already switched), filtered {} servers from {}",
                        backupResult.size(),
                        servers.size());
            }
            return backupResult;
        }

        // Use primary chain
        List<ServiceInstance> primaryResult = primaryChain.filterAll(servers, ctx);

        // If primary chain result is not empty, return directly
        if (!primaryResult.isEmpty()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Primary chain filtered {} servers from {}", primaryResult.size(), servers.size());
            }
            return primaryResult;
        }

        // Primary chain result is empty
        if (!RoutingProperties.isPrimaryBackupEnabled()) {
            // Backup chain is disabled, return primary chain's empty result
            return primaryResult;
        }

        LOGGER.info("Primary chain produced empty result, switching to backup chain");
        hasSwitchedToBackup = true;

        List<ServiceInstance> backupResult = backupChain.filterAll(servers, ctx);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Backup chain filtered {} servers from {}", backupResult.size(), servers.size());
        }
        return backupResult;
    }

    /**
     * Validate if router order configuration is valid
     *
     * @param routerOrder router order string
     * @return whether valid
     */
    public static boolean isValidRouterOrder(String routerOrder) {
        if (routerOrder == null || routerOrder.trim().isEmpty()) {
            return false;
        }
        return Arrays.stream(routerOrder.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .allMatch(name -> {
                    if (!RouterChainUtils.isValidRouterName(name)) {
                        LOGGER.warn("Invalid router name in configuration: {}", name);
                        return false;
                    }
                    return true;
                });
    }
}
