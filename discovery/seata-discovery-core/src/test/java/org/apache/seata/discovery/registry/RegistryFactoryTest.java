/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.seata.discovery.registry.mock.MockNacosMetadataRegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The type Registry factory test.
 */
public class RegistryFactoryTest {

    private static final String REGISTRY_TYPE_KEY = ConfigurationKeys.FILE_ROOT_REGISTRY
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + ConfigurationKeys.FILE_ROOT_TYPE;

    @AfterEach
    public void tearDown() {
        System.clearProperty(REGISTRY_TYPE_KEY);
        System.setProperty(ConfigurationKeys.CLIENT_REGISTRY_ENABLEMETADATA, "false");
    }

    /**
     * Test getInstance with default config.
     */
    @Test
    public void testGetInstanceWithDefaultConfig() {
        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.File.name());

        BaseRegistryService<?, ?> instance = RegistryFactory.getInstance();
        assertEquals(FileRegistryServiceImpl.class, instance.getClass());
    }

    /**
     * Test buildRegistryService with blank registry type.
     */
    @Test
    public void testGetInstanceOfBlankRegistryType() throws Throwable {
        System.setProperty(REGISTRY_TYPE_KEY, "");

        BaseRegistryService<?, ?> instance = invokeBuildRegistryService();
        assertInstanceOf(FileRegistryServiceImpl.class, instance);
    }

    /**
     * Test buildRegistryService with invalid registry type.
     */
    @Test
    public void testGetInstanceOfInvalidRegistryType() {
        String invalidRegistryType = "InvalidRegistryType";
        System.setProperty(REGISTRY_TYPE_KEY, invalidRegistryType);

        assertThatThrownBy(RegistryFactoryTest::invokeBuildRegistryService)
                .isExactlyInstanceOf(NotSupportYetException.class)
                .hasMessage("not support registry type: " + invalidRegistryType);
    }

    /**
     * Test buildRegistryService with metadata enabled.
     */
    @Test
    public void testGetInstanceWithMetadataEnabled() throws Throwable {
        System.setProperty(ConfigurationKeys.CLIENT_REGISTRY_ENABLEMETADATA, "true");
        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.Nacos.name());

        BaseRegistryService<?, ?> instance = invokeBuildRegistryService();
        assertInstanceOf(MockNacosMetadataRegistryService.class, instance);
    }

    /**
     * Test buildRegistryService with metadata enabled but not support.
     */
    @Test
    public void testGetNotSupportInstancesWithMetadataEnabled() {
        System.setProperty(ConfigurationKeys.CLIENT_REGISTRY_ENABLEMETADATA, "true");

        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.File.name());
        assertThrows(NotSupportYetException.class, RegistryFactoryTest::invokeBuildRegistryService);

        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.Redis.name());
        assertThrows(NotSupportYetException.class, RegistryFactoryTest::invokeBuildRegistryService);
    }

    /**
     * Use reflection to call the buildRegistryService method
     */
    private static BaseRegistryService<?, ?> invokeBuildRegistryService() throws Throwable {
        Method buildMethod = RegistryFactory.class.getDeclaredMethod("buildRegistryService");

        buildMethod.setAccessible(true);
        try {
            return (BaseRegistryService<?, ?>) buildMethod.invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
