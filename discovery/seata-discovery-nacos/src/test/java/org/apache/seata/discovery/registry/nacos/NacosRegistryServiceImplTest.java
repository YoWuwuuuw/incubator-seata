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
package org.apache.seata.discovery.registry.nacos;

import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.apache.seata.common.util.ReflectionUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The type Nacos registryService impl test
 */
@EnabledOnOs(OS.LINUX)
public class NacosRegistryServiceImplTest {

    private static final String GROUP_NAME_KEY = "default_tx_group";
    private static final String GROUP_NAME = "default";
    private static final String CLUSTER_NAME = "default";

    private static final RegistryService service = NacosRegistryServiceImpl.getInstance();

    @AfterEach
    public void tearDown() throws Exception {
        List<InetSocketAddress> lookups = service.lookup(GROUP_NAME_KEY);

        if (lookups != null) {
            for (InetSocketAddress instance : lookups) {
                service.unregister(instance);
            }
        }
    }

    @Test
    public void testGetInstance() {
        NacosRegistryServiceImpl instance = NacosRegistryServiceImpl.getInstance();
        assertInstanceOf(NacosRegistryServiceImpl.class, instance);
    }

    @Test
    public void testAll() throws Exception {
        /*
            1.The first time lookup is called, if the cluster does not have listener, it will add listener
         */
        InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", 8080);
        service.register(inetSocketAddress);
        Thread.sleep(10000); // wait for Nacos loading
        assertEquals(inetSocketAddress, service.lookup(GROUP_NAME_KEY).get(0));
        assertEquals(1, getListenersMap().get(GROUP_NAME).size());

        /*
            2.When there is only one instance register(), and that instance unregister(),
            lookup will always return the previous cached list instead of updating the cache to empty
         */
        service.unregister(inetSocketAddress);
        Thread.sleep(10000);
        assertEquals(1, service.lookup(GROUP_NAME_KEY).size());

        /*
            3.If there is a new instance register, which triggers the listener onEvent(),
            then lookup () returns the actual instance
         */
        InetSocketAddress inetSocketAddress1 = new InetSocketAddress("127.0.0.1", 8081);
        service.register(inetSocketAddress1);
        Thread.sleep(10000);
        assertEquals(inetSocketAddress1, service.lookup(GROUP_NAME_KEY).get(0));
        assertEquals(1, service.lookup(GROUP_NAME_KEY).size());
        assertEquals(1, getListenersMap().get(GROUP_NAME).size());
    }

    /**
     * test register() and unregister() with invalid address
     */
    @Test
    public void testRegisterWithInvalidAddress() {
        assertThrows(IllegalArgumentException.class, () -> service.register(new InetSocketAddress("127.0.0.1", 0)));
        assertThrows(IllegalArgumentException.class, () -> service.unregister(new InetSocketAddress("127.0.0.1", 0)));
    }

    @Test
    public void testUnSubscribe() throws Exception {
        EventListener eventListener = new EventListener() {
            @Override
            public void onEvent(Event event) {

            }
        };
        service.subscribe(CLUSTER_NAME, eventListener);
        assertTrue(getListenersMap().get(GROUP_NAME).contains(eventListener));
        service.unsubscribe(CLUSTER_NAME, eventListener);
        assertFalse(getListenersMap().get(GROUP_NAME).contains(eventListener));
    }

    @Test
    public void testClose() throws Exception {
        NacosRegistryServiceImpl instance = NacosRegistryServiceImpl.getInstance();
        NacosRegistryServiceImpl.getNamingInstance();

        Field useSLBWayField = NacosRegistryServiceImpl.class.getDeclaredField("useSLBWay");
        useSLBWayField.setAccessible(true);
        useSLBWayField.set(instance, true);
        NacosRegistryServiceImpl.getNamingMaintainInstance();

        instance.close();

        Field namingField = NacosRegistryServiceImpl.class.getDeclaredField("naming");
        namingField.setAccessible(true);
        assertNull(namingField.get(null));

        Field namingMaintainField = NacosRegistryServiceImpl.class.getDeclaredField("namingMaintain");
        namingMaintainField.setAccessible(true);
        assertNull(namingMaintainField.get(null));
    }

    private ConcurrentMap<String, List<EventListener>> getListenersMap() throws Exception {
        Class<?> clazz = NacosRegistryServiceImpl.class;

        Field listenerServiceMapField = clazz.getDeclaredField("LISTENER_SERVICE_MAP");
        listenerServiceMapField.setAccessible(true);

        return (ConcurrentMap<String, List<EventListener>>) listenerServiceMapField.get(null);
    }
}
