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
package org.apache.seata.discovery.registry.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.discovery.registry.BaseRegistryService;

/**
 * The interface Registry service for metadata.
 *
 * @param <T> the type parameter
 */
public interface MetadataRegistryService<T> extends BaseRegistryService<T, ServiceInstance> {

    /**
     * Service cache for metadata mode
     */
    Map<String, Map<String, List<ServiceInstance>>> CURRENT_INSTANT_MAP = new ConcurrentHashMap<>();

    default List<ServiceInstance> aliveLookup(String transactionServiceGroup) {
        Map<String, List<ServiceInstance>> clusterInstancesMap =
                CURRENT_INSTANT_MAP.computeIfAbsent(transactionServiceGroup, k -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);
        List<ServiceInstance> serviceInstances = clusterInstancesMap.get(clusterName);
        if (CollectionUtils.isNotEmpty(serviceInstances)) {
            return serviceInstances;
        }

        return clusterInstancesMap.values().stream()
                .filter(CollectionUtils::isNotEmpty)
                .findAny()
                .orElse(Collections.emptyList());
    }

    default List<ServiceInstance> refreshAliveLookup(
            String transactionServiceGroup, List<ServiceInstance> aliveInstances) {
        Map<String, List<ServiceInstance>> clusterInstancesMap =
                CURRENT_INSTANT_MAP.computeIfAbsent(transactionServiceGroup, key -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);

        return clusterInstancesMap.put(clusterName, aliveInstances);
    }

    default void removeOfflineAddressesIfNecessary(
            String transactionGroupService, String clusterName, Collection<ServiceInstance> newInstances) {
        Map<String, List<ServiceInstance>> clusterInstancesMap =
                CURRENT_INSTANT_MAP.computeIfAbsent(transactionGroupService, key -> new ConcurrentHashMap<>());

        List<ServiceInstance> currentInstances = clusterInstancesMap.getOrDefault(clusterName, Collections.emptyList());
        List<ServiceInstance> instances =
                currentInstances.stream().filter(newInstances::contains).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(instances)) {
            clusterInstancesMap.put(clusterName, instances);
        }
    }
}
