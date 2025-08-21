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
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Automatically switches to backup chain when primary chain filtering result is empty
 */
public class PrimaryBackupRouterChain implements RouterChain {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrimaryBackupRouterChain.class);
    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;

    private final DefaultRouterChain primaryChain;
    private final DefaultRouterChain backupChain;

    private volatile boolean hasSwitchedToBackup = false;

    public PrimaryBackupRouterChain() {
        // Use clearer naming
        String primaryChainOrder = fileConfig.getConfig(ConfigurationKeys.CLIENT_PRIMARY_BACKUP_ORDER);
        String fallbackChainOrder = fileConfig.getConfig(ConfigurationKeys.CLIENT_ROUTER_CHAIN_ORDER);

        this.primaryChain = new DefaultRouterChain(primaryChainOrder);
        this.backupChain = new DefaultRouterChain(fallbackChainOrder);
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

        List<ServiceInstance> result;
        if (hasSwitchedToBackup) {
            // Currently on backup chain, try backup first
            result = backupChain.filterAll(servers, ctx);
            if (!result.isEmpty()) {
                return result;
            }
            // If backup is empty, try primary
            result = primaryChain.filterAll(servers, ctx);
            if (!result.isEmpty()) {
                hasSwitchedToBackup = false;
                return result;
            }
        } else {
            // Currently on primary chain, try primary first
            result = primaryChain.filterAll(servers, ctx);
            if (!result.isEmpty()) {
                return result;
            }
            // If primary is empty, try backup
            result = backupChain.filterAll(servers, ctx);
            if (!result.isEmpty()) {
                hasSwitchedToBackup = true;
                return result;
            }
        }

        LOGGER.error("Primary chain and backup chain both produced empty result");
        return new ArrayList<>();
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
