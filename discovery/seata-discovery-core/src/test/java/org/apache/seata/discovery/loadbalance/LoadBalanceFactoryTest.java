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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The type Load balance factory test.
 */
public class LoadBalanceFactoryTest {

    @AfterEach
    public void tearDown() {
        System.clearProperty("client.registry.enable-metadata");
    }

    @Test
    public void testGetInstance() {
        System.setProperty("client.registry.enable-metadata", "false");
        LoadBalance loadBalance = LoadBalanceFactory.getInstance();
        assertNotNull(loadBalance);

        LoadBalanceModeEnum mode =
                loadBalance.getClass().getAnnotation(LoadBalanceMode.class).value();
        assertNotNull(mode);
    }

    @Test
    public void testValidateLoadBalanceMode() throws Exception {
        Method method = LoadBalanceFactory.class.getDeclaredMethod("validateLoadBalanceMode", LoadBalance.class);
        method.setAccessible(true);

        assertThrows(IllegalArgumentException.class, () -> method.invoke(new WeightRandomLoadBalance()));

        System.setProperty("client.registry.enable-metadata", "true");
        assertThrows(IllegalArgumentException.class, () -> method.invoke(new XIDLoadBalance()));
    }
}
