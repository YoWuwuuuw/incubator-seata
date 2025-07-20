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

import java.util.List;

/**
 * Automatically switches to backup chain when primary chain filtering result is empty
 */
public class PrimaryBackupRouterChain implements RouterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryBackupRouterChain.class);

    private final DefaultRouterChain primaryChain;
    private final DefaultRouterChain backupChain;

    private final String primaryOrder;
    private final String backupOrder;

    private boolean hasSwitchedToBackup = false; // Whether already switched to backup chain

    /**
     * Constructor
     */
    public PrimaryBackupRouterChain() {
        this.primaryOrder = RoutingProperties.getPrimaryBackupOrder();
        this.backupOrder = RoutingProperties.getRouterChainOrder();

        // Validate primary chain configuration
        if (primaryOrder == null || primaryOrder.trim().isEmpty()) {
            // Primary chain not configured, check backup chain
            if (backupOrder == null || backupOrder.trim().isEmpty()) {
                LOGGER.warn("Both primary and backup chain orders are not configured, using fallback strategy");
                this.primaryChain = new DefaultRouterChain();
                this.backupChain = new DefaultRouterChain();
            } else {
                LOGGER.warn("Primary backup order is not configured, using backup chain as primary");
                this.primaryChain = new DefaultRouterChain(backupOrder);
                this.backupChain = new DefaultRouterChain();
            }
        } else {
            // Validate primary chain configuration validity
            if (isValidRouterOrder(primaryOrder)) {
                this.primaryChain = new DefaultRouterChain(primaryOrder);
                this.backupChain = new DefaultRouterChain();
            } else {
                LOGGER.warn("Primary backup order configuration is invalid, using fallback strategy");
                this.primaryChain = new DefaultRouterChain();
                this.backupChain = new DefaultRouterChain();
            }
        }
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

        // Primary chain result is empty, switch to backup chain
        if (RoutingProperties.isPrimaryBackupEnabled()) {
            LOGGER.info("Primary chain produced empty result, switching to backup chain");
            hasSwitchedToBackup = true;

            List<ServiceInstance> backupResult = backupChain.filterAll(servers, ctx);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Backup chain filtered {} servers from {}", backupResult.size(), servers.size());
            }
            return backupResult;
        }

        // If backup chain is also disabled, return original list
        return servers;
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

        String[] orderNames = routerOrder.split(",");
        for (String orderName : orderNames) {
            String trimmedName = orderName.trim();
            if (!trimmedName.isEmpty() && !RouterChainUtils.isValidRouterName(trimmedName)) {
                LOGGER.warn("Invalid router name in configuration: {}", trimmedName);
                return false;
            }
        }

        return true;
    }
}
