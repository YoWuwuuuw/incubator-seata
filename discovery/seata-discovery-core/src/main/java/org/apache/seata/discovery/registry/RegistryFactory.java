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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.registry.metadata.MetadataRegistryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Factory for creating registry service instances.
 */
public class RegistryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);

    /**
     * Gets the registry service instance.
     *
     * @return the registry service instance
     */
    public static BaseRegistryService<?, ?> getInstance() {
        return RegistryFactoryHolder.INSTANCE;
    }

    /**
     * Builds the registry service based on configuration.
     *
     * @return the configured registry service
     * @throws NotSupportYetException if metadata mode is not supported for the registry type
     */
    private static BaseRegistryService<?, ?> buildRegistryService() {
        RegistryType registryType;
        String registryTypeName =
                ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_REGISTRY
                        + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                        + ConfigurationKeys.FILE_ROOT_TYPE);

        // Use default configuration if blank
        if (StringUtils.isBlank(registryTypeName)) {
            registryTypeName = RegistryType.File.name();
        }

        LOGGER.info("use registry center type: {}", registryTypeName);

        registryType = RegistryType.getType(registryTypeName);

        boolean enableMetadata =
                ConfigurationFactory.CURRENT_FILE_INSTANCE.getBoolean(ConfigurationKeys.CLIENT_REGISTRY_ENABLEMETADATA);
        if (enableMetadata) {
            if (registryType.equals(RegistryType.File) || registryType.equals(RegistryType.Redis)) {
                throw new NotSupportYetException("metadata mode not support registry type: " + registryType);
            }
            return EnhancedServiceLoader.load(
                            MetadataRegistryProvider.class,
                            Objects.requireNonNull(registryType).name())
                    .provide();
        }

        return EnhancedServiceLoader.load(
                        RegistryProvider.class,
                        Objects.requireNonNull(registryType).name())
                .provide();
    }

    /**
     * Holder for lazy initialization of registry service.
     */
    private static class RegistryFactoryHolder {
        private static final BaseRegistryService<?, ?> INSTANCE = buildRegistryService();
    }
}
