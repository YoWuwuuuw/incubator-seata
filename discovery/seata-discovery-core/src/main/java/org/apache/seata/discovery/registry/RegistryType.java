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

import org.apache.seata.common.exception.NotSupportYetException;

/**
 * Registry type enumeration.
 * Defines supported registry center types for service discovery.
 */
public enum RegistryType {
    /**
     * File-based registry.
     */
    File,
    /**
     * Raft consensus registry.
     */
    Raft,
    /**
     * ZooKeeper registry.
     */
    ZK,
    /**
     * Redis registry.
     */
    Redis,
    /**
     * Nacos registry.
     */
    Nacos,
    /**
     * Eureka registry.
     */
    Eureka,
    /**
     * Consul registry.
     */
    Consul,
    /**
     * Etcd3 registry.
     */
    Etcd3,
    /**
     * SOFA registry.
     */
    Sofa,
    /**
     * Custom registry.
     */
    Custom,
    /**
     * Seata naming server registry.
     */
    Seata;

    /**
     * Gets registry type by name (case-insensitive).
     *
     * @param name the registry type name
     * @return the registry type
     * @throws NotSupportYetException if registry type is not supported
     */
    public static RegistryType getType(String name) {
        for (RegistryType registryType : RegistryType.values()) {
            if (registryType.name().equalsIgnoreCase(name)) {
                return registryType;
            }
        }
        throw new NotSupportYetException("not support registry type: " + name);
    }
}
