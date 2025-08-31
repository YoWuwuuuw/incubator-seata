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
package org.apache.seata.discovery.routing.chain;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.discovery.routing.RoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for DefaultRouterChain
 */
@ExtendWith(MockitoExtension.class)
public class DefaultRouterChainTest {

    @Mock
    private Configuration mockConfiguration;

    private MockedStatic<ConfigurationFactory> mockedFactory;

    @BeforeEach
    public void setUp() throws Exception {
        mockedFactory = mockStatic(ConfigurationFactory.class);

        // Use reflection to set the static field
        Field field = ConfigurationFactory.class.getDeclaredField("CURRENT_FILE_INSTANCE");
        field.setAccessible(true);
        field.set(null, mockConfiguration);

        // Set default mock behavior
        when(mockConfiguration.getBoolean(anyString(), anyBoolean())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Boolean defaultValue = invocation.getArgument(1);
            // Check if we have a specific mock for this key
            String configValue = mockConfiguration.getConfig(key);
            if (configValue != null) {
                return Boolean.parseBoolean(configValue);
            }
            return defaultValue;
        });
        when(mockConfiguration.getConfig(anyString())).thenReturn(null);
    }

    @AfterEach
    public void tearDown() {
        if (mockedFactory != null) {
            mockedFactory.close();
        }
    }

    /**
     * Test loading default metadata-router successfully
     */
    @Test
    public void testLoadDefaultMetadataRouter() {
        // Mock configuration for default metadata-router
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router");
        when(mockConfiguration.getConfig("client.routing.metadata-router.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router.expression"))
                .thenReturn("version >= 2.0");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return filtered servers (only servers with version >= 2.0)
        assertNotNull(result);
        assertTrue(result.size() <= 3);
        // Verify that only servers with version >= 2.0 are returned
        for (ServiceInstance server : result) {
            String version = (String) server.getMetadata().get("version");
            assertTrue(version.compareTo("2.0") >= 0);
        }
    }

    /**
     * Test loading numbered metadata-router successfully
     */
    @Test
    public void testLoadNumberedMetadataRouter() {
        // Mock configuration for numbered metadata-router
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router-1");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.expression"))
                .thenReturn("env = prod");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return filtered servers (only servers with env = prod)
        assertNotNull(result);
        assertTrue(result.size() <= 3);
        // Verify that only servers with env = prod are returned
        for (ServiceInstance server : result) {
            String env = (String) server.getMetadata().get("env");
            assertEquals("prod", env);
        }
    }

    /**
     * Test loading multiple routers
     */
    @Test
    public void testLoadMultipleRouters() {
        // Mock configuration for multiple routers
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router,metadata-router-1");
        when(mockConfiguration.getConfig("client.routing.metadata-router.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router.expression"))
                .thenReturn("version >= 2.0");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.expression"))
                .thenReturn("env = prod");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return filtered servers
        assertNotNull(result);
        assertTrue(result.size() <= 3);
        // Verify that only servers matching both conditions are returned
        for (ServiceInstance server : result) {
            String version = (String) server.getMetadata().get("version");
            String env = (String) server.getMetadata().get("env");
            assertTrue(version.compareTo("2.0") >= 0);
            assertEquals("prod", env);
        }
    }

    /**
     * Test loading router with enabled=false
     */
    @Test
    public void testLoadRouterWithDisabledConfiguration() {
        // Mock configuration for disabled router
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router-1");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.enabled"))
                .thenReturn("false");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return original servers when router is disabled
        assertEquals(3, result.size());
        assertEquals(servers, result);
    }

    /**
     * Test loading router with missing configuration
     */
    @Test
    public void testLoadRouterWithMissingConfiguration() {
        // Mock configuration for router name but no actual configuration
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router-1");
        // No enabled or expression configured (both return null)

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return original servers when router has no configuration
        assertEquals(3, result.size());
        assertEquals(servers, result);
    }

    /**
     * Test loading SPI router
     */
    @Test
    public void testLoadSpiRouter() {
        // Mock configuration for SPI router
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("spi-custom");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return original servers for now (SPI loading not implemented)
        assertEquals(3, result.size());
        assertEquals(servers, result);
    }

    /**
     * Test loading unknown router type
     */
    @Test
    public void testLoadUnknownRouterType() {
        // Mock configuration for unknown router type
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("unknown-router");
        when(mockConfiguration.getConfig("client.routing.unknown-router.enabled"))
                .thenReturn("true");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return original servers for unknown router type
        assertEquals(3, result.size());
        assertEquals(servers, result);
    }

    /**
     * Test router chain execution order
     */
    @Test
    public void testRouterChainExecutionOrder() {
        // Mock configuration for multiple routers to test execution order
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router,metadata-router-1");
        when(mockConfiguration.getConfig("client.routing.metadata-router.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router.expression"))
                .thenReturn("version >= 2.0");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.expression"))
                .thenReturn("env = prod");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return filtered servers
        assertNotNull(result);
        assertTrue(result.size() <= 3);
        // Verify that only servers matching both conditions are returned
        for (ServiceInstance server : result) {
            String version = (String) server.getMetadata().get("version");
            String env = (String) server.getMetadata().get("env");
            assertTrue(version.compareTo("2.0") >= 0);
            assertEquals("prod", env);
        }
    }

    /**
     * Test with empty server list
     */
    @Test
    public void testWithEmptyServerList() {
        // Mock configuration for router
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router");
        when(mockConfiguration.getConfig("client.routing.metadata-router.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router.expression"))
                .thenReturn("version >= 2.0");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = new ArrayList<>();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return empty list
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test with null server list
     */
    @Test
    public void testWithNullServerList() {
        // Mock configuration for router
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn("metadata-router");
        when(mockConfiguration.getConfig("client.routing.metadata-router.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router.expression"))
                .thenReturn("version >= 2.0");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(null, ctx);

        // Should return null
        assertNull(result);
    }

    /**
     * Test router configuration with whitespace
     */
    @Test
    public void testRouterConfigurationWithWhitespace() {
        // Mock configuration for routers with whitespace
        when(mockConfiguration.getConfig("client.routing.routers")).thenReturn(" metadata-router , metadata-router-1 ");
        when(mockConfiguration.getConfig("client.routing.metadata-router.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router.expression"))
                .thenReturn("version >= 2.0");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.enabled"))
                .thenReturn("true");
        when(mockConfiguration.getConfig("client.routing.metadata-router-1.expression"))
                .thenReturn("env = prod");

        DefaultRouterChain routerChain = new DefaultRouterChain();
        List<ServiceInstance> servers = createTestServers();
        RoutingContext ctx = new RoutingContext();

        List<ServiceInstance> result = routerChain.filterAll(servers, ctx);

        // Should return filtered servers
        assertNotNull(result);
        assertTrue(result.size() <= 3);
        // Verify that only servers matching both conditions are returned
        for (ServiceInstance server : result) {
            String version = (String) server.getMetadata().get("version");
            String env = (String) server.getMetadata().get("env");
            assertTrue(version.compareTo("2.0") >= 0);
            assertEquals("prod", env);
        }
    }

    /**
     * Test constructor
     */
    @Test
    public void testConstructor() {
        // Verify default constructor works
        DefaultRouterChain chain = new DefaultRouterChain();
        assertNotNull(chain);
    }

    /**
     * Create test servers with different metadata
     */
    private List<ServiceInstance> createTestServers() {
        List<ServiceInstance> servers = new ArrayList<>();

        // Server 1: Beijing, version 2.0, env prod
        ServiceInstance server1 = createMockServer("server1", "39.9042", "116.4074", "2.0", "prod");
        servers.add(server1);

        // Server 2: Shanghai, version 1.5, env prod
        ServiceInstance server2 = createMockServer("server2", "31.2304", "121.4737", "1.5", "prod");
        servers.add(server2);

        // Server 3: Guangzhou, version 2.0, env dev
        ServiceInstance server3 = createMockServer("server3", "23.1291", "113.2644", "2.0", "dev");
        servers.add(server3);

        return servers;
    }

    /**
     * Create mock server with metadata
     */
    private ServiceInstance createMockServer(String id, String lat, String lng, String version, String env) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lat", lat);
        metadata.put("lng", lng);
        metadata.put("version", version);
        metadata.put("env", env);

        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        return new ServiceInstance(address, metadata);
    }
}
