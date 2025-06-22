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
package org.apache.seata.discovery.registry;

import org.apache.seata.common.metadata.Instance;
import org.apache.seata.config.ConfigurationFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The base interface for all mode registry services.
 *
 * @param <T> the type parameter for listener
 * @param <I> the type parameter for instance
 */
public interface BaseRegistryService<T, I> {

    /**
     * The constant PREFIX_SERVICE_MAPPING.
     */
    String PREFIX_SERVICE_MAPPING = "vgroupMapping.";

    /**
     * The constant PREFIX_SERVICE_ROOT.
     */
    String PREFIX_SERVICE_ROOT = "service";

    /**
     * The constant CONFIG_SPLIT_CHAR.
     */
    String CONFIG_SPLIT_CHAR = ".";

    Set<String> SERVICE_GROUP_NAME = new HashSet<>();

    /**
     * Register.
     *
     * @param address the address
     * @throws Exception the exception
     */
    void register(InetSocketAddress address) throws Exception;

    default void register(Instance instance) throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(
                instance.getTransaction().getHost(), instance.getTransaction().getPort());
        register(inetSocketAddress);
    }

    /**
     * Unregister.
     *
     * @param address the address
     * @throws Exception the exception
     */
    void unregister(InetSocketAddress address) throws Exception;

    default void unregister(Instance instance) throws Exception {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(
                instance.getTransaction().getHost(), instance.getTransaction().getPort());
        unregister(inetSocketAddress);
    }

    /**
     * Subscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void subscribe(String cluster, T listener) throws Exception;

    /**
     * Unsubscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void unsubscribe(String cluster, T listener) throws Exception;

    /**
     * Close.
     *
     * @throws Exception the exception
     */
    void close() throws Exception;

    /**
     * Lookup instance list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    List<I> lookup(String key) throws Exception;

    /**
     * get the list of available service instances(local cache)
     *
     * @param transactionServiceGroup the transaction service group
     * @return available service instances
     */
    List<I> aliveLookup(String transactionServiceGroup);

    /**
     * Refresh the list of available service instances and update the local cache
     *
     * @param transactionServiceGroup the transaction service group
     * @param aliveAddress            the list of alive service addresses / instances
     * @return Old available service instances list
     */
    List<I> refreshAliveLookup(String transactionServiceGroup, List<I> aliveAddress);

    /**
     * Remove service instances that have been taken offline
     *
     * @param transactionGroupService the transaction group service
     * @param clusterName             the cluster name
     * @param newAddressed            the new addresses / instances collection
     */
    void removeOfflineAddressesIfNecessary(
            String transactionGroupService, String clusterName, Collection<I> newAddressed);

    /**
     * Get current service group name
     *
     * @param key service group
     * @return the service group name
     */
    default String getServiceGroup(String key) {
        key = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        if (!SERVICE_GROUP_NAME.contains(key)) {
            SERVICE_GROUP_NAME.add(key);
        }
        return ConfigurationFactory.getInstance().getConfig(key);
    }
}
