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

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Weighted random load balancing strategy.
 * Selects instances based on their configured weights using cumulative weight method.
 */
@LoadLevel(name = LoadBalanceFactory.WEIGHT_RANDOM_LOAD_BALANCE)
@LoadBalanceMode(LoadBalanceModeEnum.METADATA)
public class WeightRandomLoadBalance implements LoadBalance {

    private static final String WEIGHT_KEY = "weight";

    // this value is default 1
    private static final int DEFAULT_WEIGHT = 1;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T select(List<T> invokers, String xid) throws Exception {
        if (invokers == null || invokers.isEmpty()) {
            return null;
        }

        List<ServiceInstance> serviceInstances = (List<ServiceInstance>) invokers;

        int totalWeight = 0;
        int[] weights = new int[serviceInstances.size()];

        for (int i = 0; i < serviceInstances.size(); i++) {
            ServiceInstance instance = serviceInstances.get(i);
            int weight = getWeight(instance);
            weights[i] = weight;
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            // Degenerate to random selection if all weights are zero or negative
            return (T) serviceInstances.get(ThreadLocalRandom.current().nextInt(serviceInstances.size()));
        }

        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);

        // Cumulative weight selection
        for (int i = 0; i < serviceInstances.size(); i++) {
            randomWeight -= weights[i];
            if (randomWeight < 0) {
                return (T) serviceInstances.get(i);
            }
        }

        // Fallback to first instance
        return (T) serviceInstances.get(0);
    }

    /**
     * Gets weight from ServiceInstance metadata.
     *
     * @param instance the service instance
     * @return the weight value, defaults to 1 if not specified
     * @throws IllegalArgumentException if weight value is invalid
     */
    private int getWeight(ServiceInstance instance) {
        if (instance == null || instance.getMetadata() == null) {
            return DEFAULT_WEIGHT;
        }

        String weightStr = instance.getMetadata().get(WEIGHT_KEY);
        if (weightStr != null) {
            try {
                int weight = Integer.parseInt(weightStr);
                return Math.max(weight, 0);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid weight value: " + weightStr);
            }
        }
        return DEFAULT_WEIGHT;
    }
}
