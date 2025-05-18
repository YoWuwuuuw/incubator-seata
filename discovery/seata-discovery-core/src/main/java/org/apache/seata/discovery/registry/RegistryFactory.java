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

import java.util.Objects;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.registry.metadata.MetadataRegistryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Registry factory.
 */
public class RegistryFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryFactory.class);

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static BaseRegistryService<?, ?> getInstance() {
        return RegistryFactoryHolder.INSTANCE;
    }

    private static BaseRegistryService<?, ?> buildRegistryService() {
        boolean enableMetadata =
                ConfigurationFactory.CURRENT_FILE_INSTANCE.getBoolean(ConfigurationKeys.CLIENT_REGISTRY_ENABLEMETADATA);

        RegistryType registryType;
        String registryTypeName = ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_REGISTRY
                + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE);
        LOGGER.info("use registry center type: {}", registryTypeName);
        try {
            registryType = RegistryType.getType(registryTypeName);
        } catch (Exception exx) {
            throw new NotSupportYetException("not support registry type: " + registryTypeName);
        }

        if (!enableMetadata) {
            return EnhancedServiceLoader.load(RegistryProvider.class, Objects.requireNonNull(registryType).name()).provide();
        }

        // TODO(www):校验无法开启元数据模式的注册中心类型, NotSupportYetException
        return EnhancedServiceLoader.load(MetadataRegistryProvider.class, Objects.requireNonNull(registryType).name()).provide();
    }

    private static class RegistryFactoryHolder {
        private static final BaseRegistryService<?, ?> INSTANCE = buildRegistryService();
    }
}
