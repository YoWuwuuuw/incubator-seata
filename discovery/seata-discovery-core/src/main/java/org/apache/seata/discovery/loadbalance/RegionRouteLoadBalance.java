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
package org.apache.seata.discovery.loadbalance;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Region route load balancing strategy
 * Routes requests to instances in the same region as the client
 */
@LoadLevel(name = LoadBalanceFactory.REGION_ROUTE_LOAD_BALANCE)
@LoadBalanceMode(LoadBalanceModeEnum.METADATA)
public class RegionRouteLoadBalance implements LoadBalance {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegionRouteLoadBalance.class);
    
    private static final String REGION_KEY = "region";
    
    // Default region when client region is not configured
    private static final String DEFAULT_REGION = "default";

    @Override
    public <T> T select(List<T> invokers, String xid) throws Exception {
        if (invokers == null || invokers.isEmpty()) {
            return null;
        }

        List<ServiceInstance> serviceInstances = (List<ServiceInstance>) invokers;
        
        // Get client region from configuration
        String clientRegion = getClientRegion();
        
        // Find instances in the same region
        List<ServiceInstance> sameRegionInstances = new ArrayList<>();
        List<ServiceInstance> otherRegionInstances = new ArrayList<>();
        
        for (ServiceInstance instance : serviceInstances) {
            String instanceRegion = getRegion(instance);
            if (clientRegion.equals(instanceRegion)) {
                sameRegionInstances.add(instance);
            } else {
                otherRegionInstances.add(instance);
            }
        }
        
        // Priority: same region instances
        if (!sameRegionInstances.isEmpty()) {
            LOGGER.debug("Selected instance from same region: {}", clientRegion);
            return (T) sameRegionInstances.get(ThreadLocalRandom.current().nextInt(sameRegionInstances.size()));
        }
        
        // Fallback: other region instances
        if (!otherRegionInstances.isEmpty()) {
            LOGGER.debug("No instances in same region {}, falling back to other regions", clientRegion);
            return (T) otherRegionInstances.get(ThreadLocalRandom.current().nextInt(otherRegionInstances.size()));
        }
        
        // Last resort: any available instance
        LOGGER.debug("No region-based instances found, using any available instance");
        return (T) serviceInstances.get(ThreadLocalRandom.current().nextInt(serviceInstances.size()));
    }
    
    /**
     * Get client region from configuration
     */
    private String getClientRegion() {
        String region = ConfigurationFactory.getInstance().getConfig("client.loadBalance.region");
        return StringUtils.isNotBlank(region) ? region : DEFAULT_REGION;
    }
    
    /**
     * Get region from ServiceInstance metadata
     */
    private String getRegion(ServiceInstance instance) {
        if (instance == null || instance.getMetadata() == null) {
            return DEFAULT_REGION;
        }

        String region = instance.getMetadata().get(REGION_KEY);
        if (region != null) {
            return region;
        }
        return DEFAULT_REGION;
    }
} 