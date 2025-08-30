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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.discovery.registry.RegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.discovery.routing.RoutingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for AbstractNettyRemotingClient routing filter functionality
 */
@ExtendWith(MockitoExtension.class)
public class AbstractNettyRemotingClientTest {

    @Mock
    private RegistryService registryService;

    @Mock
    private RoutingManager routingManager;

    private TestNettyRemotingClient client;

    /**
     * Concrete implementation of AbstractNettyRemotingClient for testing
     */
    private static class TestNettyRemotingClient extends AbstractNettyRemotingClient {

        public TestNettyRemotingClient() {
            super(mock(NettyClientConfig.class), mock(ThreadPoolExecutor.class), NettyPoolKey.TransactionRole.TMROLE);
        }

        @Override
        protected java.util.function.Function<String, NettyPoolKey> getPoolKeyFunction() {
            return key -> new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, key);
        }

        @Override
        protected String getTransactionServiceGroup() {
            return "test-group";
        }

        @Override
        protected boolean isEnableClientBatchSendRequest() {
            return false;
        }

        @Override
        protected long getRpcRequestTimeout() {
            return 30000L;
        }

        // Expose loadBalance method for testing
        public String testLoadBalance(String transactionServiceGroup, Object msg) {
            return loadBalance(transactionServiceGroup, msg);
        }

        @Override
        public void onRegisterMsgSuccess(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}

        @Override
        public void onRegisterMsgFail(
                String serverAddress, Channel channel, Object response, AbstractMessage requestMessage) {}
    }

    @BeforeEach
    public void setUp() {
        client = new TestNettyRemotingClient();
    }

    @AfterEach
    public void tearDown() {
        // Clean up system properties
        System.clearProperty("client.routing.enabled");
        System.clearProperty("client.routing.metadata-routers.metadata-router-1.enabled");
        System.clearProperty("client.routing.metadata-routers.metadata-router-1.expression");
    }

    /**
     * Test routing filter with metadata-based routing
     * Test scenario: Production environment should route to production servers
     */
    @Test
    public void testRoutingFilterWithMetadataRouting() {
        // Configure routing settings
        System.setProperty("client.routing.enabled", "true");
        System.setProperty("client.routing.metadata-routers.metadata-router-1.enabled", "true");
        System.setProperty("client.routing.metadata-routers.metadata-router-1.expression", "env == 'prod'");

        // Create service instances with metadata
        List<ServiceInstance> serviceInstances = createServiceInstances(
                new ServerConfig("127.0.0.1", 8091, new HashMap<String, Object>() {
                    {
                        put("env", "prod");
                        put("version", "1.0.0");
                        put("zone", "zone-a");
                    }
                }),
                new ServerConfig("127.0.0.2", 8092, new HashMap<String, Object>() {
                    {
                        put("env", "staging");
                        put("version", "1.0.0");
                        put("zone", "zone-b");
                    }
                }),
                new ServerConfig("127.0.0.3", 8093, new HashMap<String, Object>() {
                    {
                        put("env", "dev");
                        put("version", "1.0.0");
                        put("zone", "zone-c");
                    }
                }));

        try (MockedStatic<RegistryFactory> mockedRegistryFactory = mockStatic(RegistryFactory.class);
                MockedStatic<RoutingManager> mockedRoutingManager = mockStatic(RoutingManager.class)) {

            // Mock RegistryFactory
            mockedRegistryFactory.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.aliveLookup("test-group")).thenReturn(serviceInstances);

            // Mock RoutingManager
            mockedRoutingManager.when(RoutingManager::getInstance).thenReturn(routingManager);
            when(routingManager.filter(serviceInstances))
                    .thenReturn(serviceInstances.subList(0, 1)); // Return only production server

            // Test with BranchRegisterRequest
            BranchRegisterRequest request = new BranchRegisterRequest();
            request.setXid("test-xid-456");

            String result = client.testLoadBalance("test-group", request);

            assertNotNull(result);
            // Should route to production server (127.0.0.1)
            assertEquals("127.0.0.1:8091", result);
        }
    }

    /**
     * Test routing filter with no routing configuration
     * Test scenario: No routing rules configured, should return original instances
     */
    @Test
    public void testRoutingFilterWithNoConfiguration() {
        // Disable routing
        System.setProperty("client.routing.enabled", "false");

        // Create basic service instances without special metadata
        List<ServiceInstance> serviceInstances = createServiceInstances(
                new ServerConfig("127.0.0.1", 8091, new HashMap<>()),
                new ServerConfig("127.0.0.2", 8092, new HashMap<>()));

        try (MockedStatic<RegistryFactory> mockedRegistryFactory = mockStatic(RegistryFactory.class);
                MockedStatic<RoutingManager> mockedRoutingManager = mockStatic(RoutingManager.class)) {

            // Mock RegistryFactory
            mockedRegistryFactory.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.aliveLookup("test-group")).thenReturn(serviceInstances);

            // Mock RoutingManager - should return original list when routing is disabled
            mockedRoutingManager.when(RoutingManager::getInstance).thenReturn(routingManager);
            when(routingManager.filter(serviceInstances)).thenReturn(serviceInstances);

            // Test with BranchRegisterRequest
            BranchRegisterRequest request = new BranchRegisterRequest();
            request.setXid("test-xid-no-config");

            String result = client.testLoadBalance("test-group", request);

            assertNotNull(result);
            // Should return one of the original instances
            assertTrue(result.equals("127.0.0.1:8091") || result.equals("127.0.0.2:8092"));
        }
    }

    /**
     * Test routing filter with complex metadata routing
     * Test scenario: Multiple metadata conditions
     */
    @Test
    public void testRoutingFilterWithComplexMetadataRouting() {
        // Configure routing settings
        System.setProperty("client.routing.enabled", "true");
        System.setProperty("client.routing.metadata-routers.metadata-router-1.enabled", "true");
        System.setProperty(
                "client.routing.metadata-routers.metadata-router-1.expression", "env == 'prod' && zone == 'zone-a'");

        // Create service instances with complex metadata
        List<ServiceInstance> serviceInstances = createServiceInstances(
                new ServerConfig("127.0.0.1", 8091, new HashMap<String, Object>() {
                    {
                        put("env", "prod");
                        put("version", "1.0.0");
                        put("zone", "zone-a");
                    }
                }),
                new ServerConfig("127.0.0.2", 8092, new HashMap<String, Object>() {
                    {
                        put("env", "prod");
                        put("version", "1.0.0");
                        put("zone", "zone-b");
                    }
                }),
                new ServerConfig("127.0.0.3", 8093, new HashMap<String, Object>() {
                    {
                        put("env", "staging");
                        put("version", "1.0.0");
                        put("zone", "zone-a");
                    }
                }));

        try (MockedStatic<RegistryFactory> mockedRegistryFactory = mockStatic(RegistryFactory.class);
                MockedStatic<RoutingManager> mockedRoutingManager = mockStatic(RoutingManager.class)) {

            // Mock RegistryFactory
            mockedRegistryFactory.when(RegistryFactory::getInstance).thenReturn(registryService);
            when(registryService.aliveLookup("test-group")).thenReturn(serviceInstances);

            // Mock RoutingManager - return production zone-a server
            mockedRoutingManager.when(RoutingManager::getInstance).thenReturn(routingManager);
            when(routingManager.filter(serviceInstances))
                    .thenReturn(serviceInstances.subList(0, 1)); // Return production zone-a server

            // Test with BranchRegisterRequest
            BranchRegisterRequest request = new BranchRegisterRequest();
            request.setXid("test-xid-complex");

            String result = client.testLoadBalance("test-group", request);

            assertNotNull(result);
            // Should route to production zone-a server (127.0.0.1)
            assertEquals("127.0.0.1:8091", result);
        }
    }

    /**
     * Server configuration for creating service instances
     */
    private static class ServerConfig {
        private final String host;
        private final int port;
        private final Map<String, Object> metadata;

        public ServerConfig(String host, int port, Map<String, Object> metadata) {
            this.host = host;
            this.port = port;
            this.metadata = metadata;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Create service instances with given configurations
     * @param configs server configurations
     * @return list of service instances
     */
    private List<ServiceInstance> createServiceInstances(ServerConfig... configs) {
        List<ServiceInstance> serviceInstances = new ArrayList<>();

        for (ServerConfig config : configs) {
            ServiceInstance instance = mock(ServiceInstance.class);
            lenient().when(instance.getAddress()).thenReturn(new InetSocketAddress(config.getHost(), config.getPort()));
            lenient().when(instance.getMetadata()).thenReturn(config.getMetadata());
            serviceInstances.add(instance);
        }

        return serviceInstances;
    }
}
