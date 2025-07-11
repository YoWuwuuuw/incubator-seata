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
package org.apache.seata.discovery.loadbalance;

import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.rpc.RpcStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class LoadBalanceTest {

    private static final String XID = "XID";

    /**
     * Test random load balance select.
     *
     * @param instances the instances
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testRandomLoadBalance_select(List<ServiceInstance> instances) throws Exception {
        int runs = 10000;
        Map<ServiceInstance, AtomicLong> counter = getSelectedCounter(runs, instances, new RandomLoadBalance());
        for (ServiceInstance instance : counter.keySet()) {
            Long count = counter.get(instance).get();
            Assertions.assertTrue(count > 0, "selecte one time at last");
        }
    }

    /**
     * Test round-robin load balance select.
     *
     * @param instances the instances
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testRoundRobinLoadBalance_select(List<ServiceInstance> instances) throws Exception {
        int runs = 10000;
        Map<ServiceInstance, AtomicLong> counter = getSelectedCounter(runs, instances, new RoundRobinLoadBalance());
        for (ServiceInstance instance : counter.keySet()) {
            Long count = counter.get(instance).get();
            Assertions.assertTrue(Math.abs(count - runs / (0f + instances.size())) < 1f, "abs diff shoud < 1");
        }
    }

    /**
     * Test xid load load balance select.
     *
     * @param instances the instances
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testXIDLoadBalance_select(List<ServiceInstance> instances) throws Exception {
        XIDLoadBalance loadBalance = new XIDLoadBalance();
        // ipv4
        ServiceInstance serviceInstance = loadBalance.select(instances, "127.0.0.1:8092:123456");
        Assertions.assertNotNull(serviceInstance);
        // ipv6
        serviceInstance = loadBalance.select(instances, "2000:0000:0000:0000:0001:2345:6789:abcd:8092:123456");
        Assertions.assertNotNull(serviceInstance);
        // test not found tc channel
        serviceInstance = loadBalance.select(instances, "127.0.0.1:8199:123456");
        Assertions.assertNotEquals(serviceInstance.getAddress().getPort(), 8199);
    }

    /**
     * Test consistent hash load balance select.
     *
     * @param instances the instances
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testConsistentHashLoadBalance_select(List<ServiceInstance> instances) throws Exception {
        int runs = 10000;
        int selected = 0;
        ConsistentHashLoadBalance loadBalance = new ConsistentHashLoadBalance();
        Map<ServiceInstance, AtomicLong> counter = getSelectedCounter(runs, instances, loadBalance);
        for (ServiceInstance instance : counter.keySet()) {
            if (counter.get(instance).get() > 0) {
                selected++;
            }
        }
        Assertions.assertEquals(1, selected, "selected must be equal to 1");
    }

    /**
     * Test cached consistent hash load balance select.
     *
     * @param instances the instances
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testCachedConsistentHashLoadBalance_select(List<ServiceInstance> instances) throws Exception {
        ConsistentHashLoadBalance loadBalance = new ConsistentHashLoadBalance();

        List<ServiceInstance> instances1 = new ArrayList<>(instances);
        loadBalance.select(instances1, XID);
        Object o1 = getConsistentHashSelectorByReflect(loadBalance);
        List<ServiceInstance> instances2 = new ArrayList<>(instances);
        loadBalance.select(instances2, XID);
        Object o2 = getConsistentHashSelectorByReflect(loadBalance);
        Assertions.assertEquals(o1, o2);

        List<ServiceInstance> instances3 = new ArrayList<>(instances);
        instances3.remove(ThreadLocalRandom.current().nextInt(instances.size()));
        loadBalance.select(instances3, XID);
        Object o3 = getConsistentHashSelectorByReflect(loadBalance);
        Assertions.assertNotEquals(o1, o3);
    }

    /**
     * Test least active load balance select.
     *
     * @param instances the instances
     */
    @ParameterizedTest
    @MethodSource("instanceProvider")
    public void testLeastActiveLoadBalance_select(List<ServiceInstance> instances) throws Exception {
        int runs = 10000;
        int size = instances.size();
        for (int i = 0; i < size - 1; i++) {
            RpcStatus.beginCount(instances.get(i).getAddress().toString());
        }
        ServiceInstance targetInstance = instances.get(size - 1);
        LoadBalance loadBalance = new LeastActiveLoadBalance();
        for (int i = 0; i < runs; i++) {
            ServiceInstance selectInstance = loadBalance.select(instances, XID);
            Assertions.assertEquals(selectInstance, targetInstance);
        }
        RpcStatus.beginCount(targetInstance.getAddress().toString());
        RpcStatus.beginCount(targetInstance.getAddress().toString());
        Map<ServiceInstance, AtomicLong> counter = getSelectedCounter(runs, instances, loadBalance);
        for (ServiceInstance instance : counter.keySet()) {
            Long count = counter.get(instance).get();
            if (instance == targetInstance) {
                Assertions.assertEquals(count, 0);
            } else {
                Assertions.assertTrue(count > 0);
            }
        }
    }

    /**
     * Get the selection count for each ServiceInstance after running the load balancing algorithm multiple times.
     *
     * @param runs        the number of times to perform selection
     * @param instances   the list of service instances to select from
     * @param loadBalance the load balancing strategy to use
     * @return a map where the key is the ServiceInstance and the value is the number of times it was selected
     */
    public Map<ServiceInstance, AtomicLong> getSelectedCounter(
            int runs, List<ServiceInstance> instances, LoadBalance loadBalance) throws Exception {
        Assertions.assertNotNull(loadBalance);
        Map<ServiceInstance, AtomicLong> counter = new ConcurrentHashMap<>();
        for (ServiceInstance instance : instances) {
            counter.put(instance, new AtomicLong(0));
        }

        for (int i = 0; i < runs; i++) {
            ServiceInstance selectInstance = loadBalance.select(instances, XID);
            counter.get(selectInstance).incrementAndGet();
        }

        return counter;
    }

    /**
     * Gets ConsistentHashSelector Instance By Reflect
     *
     * @param loadBalance the loadBalance
     * @return the ConsistentHashSelector
     */
    public Object getConsistentHashSelectorByReflect(ConsistentHashLoadBalance loadBalance) throws Exception {
        Field selectorWrapperField = ConsistentHashLoadBalance.class.getDeclaredField("selectorWrapper");
        selectorWrapperField.setAccessible(true);
        Object selectWrapper = selectorWrapperField.get(loadBalance);
        Assertions.assertNotNull(selectWrapper);
        Field selectorField = selectWrapper.getClass().getDeclaredField("selector");
        selectorField.setAccessible(true);
        return selectorField.get(selectWrapper);
    }

    /**
     * Provides a stream of service instance lists for parameterized tests.
     *
     * @return Stream<List < ServiceInstance>> service instance lists
     */
    static Stream<List<ServiceInstance>> instanceProvider() {
        return Stream.of(Arrays.asList(
                new ServiceInstance(new InetSocketAddress("127.0.0.1", 8091)),
                new ServiceInstance(new InetSocketAddress("127.0.0.1", 8092)),
                new ServiceInstance(new InetSocketAddress("127.0.0.1", 8093)),
                new ServiceInstance(new InetSocketAddress("127.0.0.1", 8094)),
                new ServiceInstance(new InetSocketAddress("127.0.0.1", 8095)),
                new ServiceInstance(new InetSocketAddress("2000:0000:0000:0000:0001:2345:6789:abcd", 8092))));
    }
}
