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
package org.apache.seata.discovery.registry.sofa;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.ReflectionUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simplified tests for SofaRegistryServiceImpl: focus on basic functionality without e2e registry dependency.
 */
public class SofaRegistryServiceImplTest {

    @AfterEach
    public void tearDown() {
        // clear any properties we might set in tests
        System.clearProperty("registry.sofa.region");
        System.clearProperty("registry.sofa.datacenter");
        System.clearProperty("registry.sofa.group");
        System.clearProperty("registry.sofa.cluster");
        System.clearProperty("registry.sofa.addressWaitTime");
        System.clearProperty("registry.sofa.serverAddr");
        System.clearProperty("registry.sofa.application");
    }

    @Test
    public void testGetNamingProperties_defaults_and_overrides() throws Exception {
        Method method = ReflectionUtil.getMethod(SofaRegistryServiceImpl.class, "getNamingProperties");
        Properties props = (Properties) ReflectionUtil.invokeMethod(null, method);

        // minimal defaults check (avoid asserting environment-specific values)
        Assertions.assertThat(props.getProperty("region")).isEqualTo("DEFAULT_ZONE");
        Assertions.assertThat(props.getProperty("group")).isEqualTo("SEATA_GROUP");

        // set overrides via system properties (verify they take effect deterministically)
        System.setProperty("registry.sofa.region", "REGION_X");
        System.setProperty("registry.sofa.addressWaitTime", "1500");
        System.setProperty("registry.sofa.serverAddr", "10.21.76.32:9600");

        props = (Properties) ReflectionUtil.invokeMethod(null, method);
        Assertions.assertThat(props.getProperty("region")).isEqualTo("REGION_X");
        Assertions.assertThat(props.getProperty("addressWaitTime")).isEqualTo("1500");
        Assertions.assertThat(props.getProperty("serverAddr")).isEqualTo("10.21.76.32:9600");
    }

    @Test
    public void testBuildServiceData_and_flatData_with_metadata() throws Exception {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 8091);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("k1", "v1");
        metadata.put("k 2", "v&2=2?"); // include special chars to verify URL encoding/decoding

        assertRegisterAndDiscoverWithMetadata(addr, metadata);
    }

    @Test
    public void testFlatData_without_metadata() throws Exception {
        SofaRegistryServiceImpl sofa = SofaRegistryServiceImpl.getInstance();
        String raw = "127.0.0.1:8091"; // legacy format without meta
        Map<String, List<String>> zoneMap = new HashMap<>();
        zoneMap.put("DEFAULT_ZONE", Collections.singletonList(raw));

        Method flatData = ReflectionUtil.getMethod(SofaRegistryServiceImpl.class, "flatData", Map.class);
        @SuppressWarnings("unchecked")
        List<ServiceInstance> list = (List<ServiceInstance>) ReflectionUtil.invokeMethod(sofa, flatData, zoneMap);

        assertEquals(1, list.size());
        ServiceInstance si = list.get(0);
        assertEquals(new InetSocketAddress("127.0.0.1", 8091), si.getAddress());
        Assertions.assertThat(si.getMetadata()).isNull();
    }

    @Test
    public void testRegisterAndLookupIntegration() throws Exception {
        SofaRegistryServiceImpl sofa = SofaRegistryServiceImpl.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 10080);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("testKey", "testVal");
        ServiceInstance instance = new ServiceInstance(address, metadata);


        sofa.register(instance);
        sleep(3000);

        List<ServiceInstance> found = sofa.lookup("default_tx_group");
        Assertions.assertThat(found).isNotEmpty();
        boolean matched = found.stream()
                .anyMatch(si -> si.getAddress().equals(address)
                        && "testVal".equals(si.getMetadata().get("testKey")));
        assertTrue(matched);
    }

    @Test
    public void testUnregisterIntegration() throws Exception {
        SofaRegistryServiceImpl sofa = SofaRegistryServiceImpl.getInstance();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 10081);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("unreg", "xxx");
        ServiceInstance instance = new ServiceInstance(address, metadata);

        sofa.register(instance);
        sofa.unregister(instance);
        List<ServiceInstance> found = sofa.lookup("default_tx_group");
        boolean present =
                found != null && found.stream().anyMatch(si -> si.getAddress().equals(address));
        assertFalse(present);
    }

    // assert registration(buildServiceData) and discovery(flatData) with metadata
    private void assertRegisterAndDiscoverWithMetadata(InetSocketAddress addr, Map<String, Object> metadata)
            throws Exception {
        SofaRegistryServiceImpl sofa = SofaRegistryServiceImpl.getInstance();

        Method buildServiceData = ReflectionUtil.getMethod(
                SofaRegistryServiceImpl.class, "buildServiceData", InetSocketAddress.class, Map.class);
        String serviceData = (String) ReflectionUtil.invokeMethod(sofa, buildServiceData, addr, metadata);
        assertNotNull(serviceData);
        Assertions.assertThat(serviceData).startsWith(addr.getAddress().getHostAddress() + ":" + addr.getPort());

        Map<String, List<String>> zoneMap = new HashMap<>();
        zoneMap.put("DEFAULT_ZONE", Collections.singletonList(serviceData));

        Method flatData = ReflectionUtil.getMethod(SofaRegistryServiceImpl.class, "flatData", Map.class);
        @SuppressWarnings("unchecked")
        List<ServiceInstance> list = (List<ServiceInstance>) ReflectionUtil.invokeMethod(sofa, flatData, zoneMap);

        assertEquals(1, list.size());
        ServiceInstance si = list.get(0);
        assertEquals(addr, si.getAddress());
        Assertions.assertThat(si.getMetadata()).isNotNull();
        for (Map.Entry<String, Object> e : metadata.entrySet()) {
            Assertions.assertThat(si.getMetadata()).containsEntry(e.getKey(), String.valueOf(e.getValue()));
        }
    }
}
