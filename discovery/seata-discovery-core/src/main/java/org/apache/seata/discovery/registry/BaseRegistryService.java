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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.metadata.ServiceInstance;

/**
 * The base interface for all mode registry services.
 *
 * @param <T> the type parameter for listener
 * @param <I> the type parameter for instance
 */
public interface BaseRegistryService<T, I> {

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
    List<ServiceInstance> lookup(String key) throws Exception;

    /**
     * Get current service group name
     *
     * @param key service group
     * @return the service group name
     */
    String getServiceGroup(String key);

    /**
     * Get alive service instances
     *
     * @param transactionServiceGroup the transaction service group
     * @return the list of alive service instances
     */
    List<I> aliveLookup(String transactionServiceGroup);

    /**
     * Refresh alive service instances
     *
     * @param transactionServiceGroup the transaction service group
     * @param aliveAddress the list of alive service instances
     * @return the list of refreshed service instances
     */
    List<I> refreshAliveLookup(String transactionServiceGroup, List<I> aliveAddress);

    /**
     * Remove offline addresses if necessary.
     * Intersection of the old and new addresses
     *
     * @param transactionGroupService the transaction group service
     * @param clusterName the cluster name
     * @param newAddressed the new addressed collection
     */
    void removeOfflineAddressesIfNecessary(
            String transactionGroupService, String clusterName, Collection<I> newAddressed);
}
