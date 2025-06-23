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

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Registry heartbeat management utility.
 * Provides heartbeat functionality for service registration to maintain service availability.
 *
 * @since 2021/6/13 5:09 pm
 */
public class RegistryHeartBeats {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryHeartBeats.class);
    private static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String HEARTBEAT_KEY = "heartbeat";
    private static final String HEARTBEAT_PERIOD_KEY = "period";
    private static final String HEARTBEAT_ENABLED_KEY = "enabled";

    private static final long DEFAULT_HEARTBEAT_PERIOD = 60 * 1000;
    private static final boolean DEFAULT_HEARTBEAT_ENABLED = Boolean.TRUE;

    private static final ScheduledExecutorService HEARTBEAT_SCHEDULED =
            new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setName("seata-discovery-heartbeat");
                    return thread;
                }
            });

    /**
     * Adds heartbeat with default period for the registry type.
     *
     * @param registryType   the registry type
     * @param serverAddress  the server address
     * @param reRegister     the re-registration function
     */
    public static void addHeartBeat(String registryType, InetSocketAddress serverAddress, ReRegister reRegister) {
        addHeartBeat(registryType, serverAddress, getHeartbeatPeriod(registryType), reRegister);
    }

    /**
     * Adds heartbeat with custom period for the registry type.
     *
     * @param registryType   the registry type
     * @param serverAddress  the server address
     * @param period         the heartbeat period in milliseconds
     * @param reRegister     the re-registration function
     */
    public static void addHeartBeat(
            String registryType, InetSocketAddress serverAddress, long period, ReRegister reRegister) {
        if (!getHeartbeatEnabled(registryType)) {
            LOGGER.info("registry heartbeat disabled");
            return;
        }
        HEARTBEAT_SCHEDULED.scheduleAtFixedRate(
                () -> {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("seata heartbeat re-registry.");
                        }
                        reRegister.register(serverAddress);
                    } catch (Exception e) {
                        LOGGER.error("seata registry heartbeat failed!", e);
                    }
                },
                period,
                period,
                TimeUnit.MILLISECONDS);
    }

    /**
     * Closes heartbeat for the registry type.
     *
     * @param registryType the registry type
     */
    public static void close(String registryType) {
        if (getHeartbeatEnabled(registryType)) {
            HEARTBEAT_SCHEDULED.shutdown();
        }
    }

    /**
     * Gets heartbeat period from configuration.
     *
     * @param registryType the registry type
     * @return the heartbeat period in milliseconds
     */
    private static long getHeartbeatPeriod(String registryType) {
        String propertySuffix = String.join("-", HEARTBEAT_KEY, HEARTBEAT_PERIOD_KEY);
        return FILE_CONFIG.getLong(
                String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, registryType, propertySuffix),
                DEFAULT_HEARTBEAT_PERIOD);
    }

    /**
     * Gets heartbeat enabled status from configuration.
     *
     * @param registryType the registry type
     * @return true if heartbeat is enabled
     */
    private static boolean getHeartbeatEnabled(String registryType) {
        String propertySuffix = String.join("-", HEARTBEAT_KEY, HEARTBEAT_ENABLED_KEY);
        return FILE_CONFIG.getBoolean(
                String.join(FILE_CONFIG_SPLIT_CHAR, FILE_ROOT_REGISTRY, registryType, propertySuffix),
                DEFAULT_HEARTBEAT_ENABLED);
    }

    /**
     * Functional interface for re-registration operations.
     */
    @FunctionalInterface
    public interface ReRegister {

        /**
         * Performs re-registration of the server address.
         *
         * @param serverAddress the server address to re-register
         * @throws Exception if re-registration fails
         */
        void register(InetSocketAddress serverAddress) throws Exception;
    }
}
