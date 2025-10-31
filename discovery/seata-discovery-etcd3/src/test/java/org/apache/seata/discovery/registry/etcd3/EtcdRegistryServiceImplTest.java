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
package org.apache.seata.discovery.registry.etcd3;

import io.etcd.jetcd.Watch;
import io.etcd.jetcd.watch.WatchResponse;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

public class EtcdRegistryServiceImplTest {
    // not used directly in tests anymore
    private static final String CLUSTER_NAME = "default";
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8091;

    @BeforeAll
    public static void beforeClass() throws Exception {
        // 设置事务组映射，便于 lookup 按默认集群解析
        System.setProperty("service.vgroupMapping.default", CLUSTER_NAME);
    }

    @AfterAll
    public static void afterClass() throws Exception {
        System.clearProperty("service.vgroupMapping.default");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegister() throws Exception {
        RegistryService<Watch.Listener> registryService =
                (RegistryService<Watch.Listener>) new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(new ServiceInstance(inetSocketAddress));
        // 2.lookup should see 1 instance
        List<ServiceInstance> serviceInstances = registryService.lookup(DEFAULT_TX_GROUP);
        assertThat(serviceInstances).size().isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnregister() throws Exception {
        RegistryService<Watch.Listener> registryService =
                (RegistryService<Watch.Listener>) new EtcdRegistryProvider().provide();
        ServiceInstance serviceInstance = new ServiceInstance(new InetSocketAddress(HOST, PORT));
        // 1.register
        registryService.register(serviceInstance);
        // 2.lookup should see 1 instance
        List<ServiceInstance> list = registryService.lookup(DEFAULT_TX_GROUP);
        assertThat(list).size().isEqualTo(1);
        // 3.unregister
        registryService.unregister(serviceInstance);
        // 4.lookup should see 0 instance
        list = registryService.lookup(DEFAULT_TX_GROUP);
        assertThat(list).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRegisterWithMetadataAndLookup() throws Exception {
        RegistryService<Watch.Listener> registryService =
                (RegistryService<Watch.Listener>) new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("zone", "az1");
        metadata.put("version", "1.0.0");
        ServiceInstance instanceWithMeta = new ServiceInstance(inetSocketAddress, metadata);

        // 1.register with metadata
        registryService.register(instanceWithMeta);

        // 2.lookup and assert metadata propagated
        List<ServiceInstance> serviceInstances = registryService.lookup(DEFAULT_TX_GROUP);
        assertThat(serviceInstances).isNotEmpty();
        ServiceInstance found = serviceInstances.get(0);
        assertThat(found.getMetadata()).isNotNull();
        assertThat(found.getMetadata().get("zone")).isEqualTo("az1");
        assertThat(found.getMetadata().get("version")).isEqualTo("1.0.0");

        // 4.cleanup
        registryService.unregister(instanceWithMeta);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSubscribe() throws Exception {
        RegistryService<Watch.Listener> registryService =
                (RegistryService<Watch.Listener>) new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(new ServiceInstance(inetSocketAddress));
        // 2.subscribe
        EtcdListener etcdListener = new EtcdListener();
        registryService.subscribe(CLUSTER_NAME, etcdListener);
        // 3.unregister instance to trigger event
        registryService.unregister(new ServiceInstance(inetSocketAddress));
        assertThat(etcdListener.isNotified()).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUnsubscribe() throws Exception {
        RegistryService<Watch.Listener> registryService =
                (RegistryService<Watch.Listener>) new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(new ServiceInstance(inetSocketAddress));
        // 2.subscribe
        EtcdListener etcdListener = new EtcdListener();
        registryService.subscribe(CLUSTER_NAME, etcdListener);
        // 3.unregister instance to trigger event
        registryService.unregister(new ServiceInstance(inetSocketAddress));
        assertThat(etcdListener.isNotified()).isTrue();
        // 4.unsubscribe
        registryService.unsubscribe(CLUSTER_NAME, etcdListener);
        // 5.reset
        etcdListener.reset();
        // 6.register again, the listener should not be notified now
        registryService.register(new ServiceInstance(inetSocketAddress));
        assertThat(etcdListener.isNotified()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLookup() throws Exception {
        RegistryService<Watch.Listener> registryService =
                (RegistryService<Watch.Listener>) new EtcdRegistryProvider().provide();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(HOST, PORT);
        // 1.register
        registryService.register(new ServiceInstance(inetSocketAddress));
        // 2.lookup
        List<ServiceInstance> serviceInstances = registryService.lookup(DEFAULT_TX_GROUP);
        assertThat(serviceInstances).size().isEqualTo(1);
    }

    // helper methods no longer needed as we operate via RegistryService

    /**
     * etcd listener
     */
    private static class EtcdListener implements Watch.Listener {
        private boolean notified = false;

        @Override
        public void onNext(WatchResponse response) {
            notified = true;
        }

        @Override
        public void onError(Throwable throwable) {}

        @Override
        public void onCompleted() {}

        /**
         * @return
         */
        public boolean isNotified() throws InterruptedException {
            TimeUnit.SECONDS.sleep(3);
            return notified;
        }

        /**
         * reset
         */
        private void reset() {
            this.notified = false;
        }
    }
}
