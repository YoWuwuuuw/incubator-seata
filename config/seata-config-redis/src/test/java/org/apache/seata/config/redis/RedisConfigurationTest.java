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
package org.apache.seata.config.redis;

import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class RedisConfigurationTest {

    private static final String TEST_DATA_ID = "testDataId";
    private static final String TEST_CONTENT = "testContent";
    private static final String CONFIGKEY = "seata:config:SEATA_GROUP";

    private static final long TIMEOUT_MILLIS = 5000;

    private static Configuration configuration;
    private static JedisPool jedisPool;

    @BeforeAll
    public static void setUp() {
        System.setProperty("config.type", "redis");
        System.setProperty("config.redis.server-addr", "127.0.0.1:6379");
        configuration = RedisConfiguration.getInstance();

        jedisPool = new JedisPool("127.0.0.1", 6379);

        // refresh data
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(CONFIGKEY);
        }
    }

    @AfterAll
    public static void tearDown() {
        // clean up all test data
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(CONFIGKEY);
        }
        jedisPool.close();
    }

    @Test
    public void testPutConfig() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConfigurationChangeListener listener = addPublishListener(TEST_DATA_ID, TEST_CONTENT, latch);

        boolean putResult = configuration.putConfig(TEST_DATA_ID, TEST_CONTENT, TIMEOUT_MILLIS);
        Assertions.assertTrue(putResult);

        boolean isMessageReceived = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(isMessageReceived, "onMessage() method is not triggered");

        cleanupAfterTest(TEST_DATA_ID, listener);
    }

    @Test
    public void testGetLatestConfig() throws InterruptedException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(CONFIGKEY, TEST_DATA_ID, TEST_CONTENT);
        }

        String result = configuration.getLatestConfig(TEST_DATA_ID, null, TIMEOUT_MILLIS);
        Assertions.assertEquals(TEST_CONTENT, result);

        cleanupAfterTest(TEST_DATA_ID, null);
    }

    @Test
    public void testRemoveConfig() throws InterruptedException {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(CONFIGKEY, TEST_DATA_ID, TEST_CONTENT);
        }

        CountDownLatch latch = new CountDownLatch(1);
        ConfigurationChangeListener listener = addPublishListener(TEST_DATA_ID, null, latch);

        boolean success = configuration.removeConfig(TEST_DATA_ID, TIMEOUT_MILLIS);
        Assertions.assertTrue(success);

        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.hget(CONFIGKEY, TEST_DATA_ID);
            Assertions.assertNull(value);
        }

        boolean isMessageReceived = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(isMessageReceived, "onMessage() method is not triggered");

        cleanupAfterTest(TEST_DATA_ID, listener);
    }

    @Test
    public void testPutConfigIfAbsent() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ConfigurationChangeListener listener = addPublishListener(TEST_DATA_ID, TEST_CONTENT, latch);

        // When seataConfig is empty, execute redis transactions -> set and publish
        boolean success = configuration.putConfigIfAbsent(TEST_DATA_ID, TEST_CONTENT, TIMEOUT_MILLIS);
        Assertions.assertTrue(success);

        // When local cache is available, return true directly
        boolean secondTry = configuration.putConfigIfAbsent(TEST_DATA_ID, TEST_CONTENT, TIMEOUT_MILLIS);
        Assertions.assertTrue(secondTry);

        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.hget(CONFIGKEY, TEST_DATA_ID);
            Assertions.assertEquals(TEST_CONTENT, value);
        }

        // When dataId is new and seataConfig is not empty, -> .putConfig()
        String newDataId = "newTestDataId";
        boolean thirdTry = configuration.putConfigIfAbsent(newDataId, "newTestContent", TIMEOUT_MILLIS);
        Assertions.assertTrue(thirdTry);

        boolean isMessageReceived = latch.await(5, TimeUnit.SECONDS);
        Assertions.assertTrue(isMessageReceived, "onMessage() method is not triggered");

        cleanupAfterTest(TEST_DATA_ID, listener);
        cleanupAfterTest(newDataId, null);
    }

    @Test
    public void testGetConfigListeners() throws InterruptedException {
        Set<ConfigurationChangeListener> initialListeners = configuration.getConfigListeners(TEST_DATA_ID);
        Assertions.assertNull(initialListeners);

        CountDownLatch latch1 = new CountDownLatch(1);
        ConfigurationChangeListener listener1 = addPublishListener(TEST_DATA_ID, TEST_CONTENT, latch1);

        CountDownLatch latch2 = new CountDownLatch(1);
        ConfigurationChangeListener listener2 = addPublishListener(TEST_DATA_ID, TEST_CONTENT, latch2);

        Set<ConfigurationChangeListener> listeners = configuration.getConfigListeners(TEST_DATA_ID);
        Assertions.assertNotNull(listeners);
        Assertions.assertEquals(2, listeners.size());
        Assertions.assertTrue(listeners.contains(listener1));
        Assertions.assertTrue(listeners.contains(listener2));

        configuration.removeConfigListener(TEST_DATA_ID, listener1);
        listeners = configuration.getConfigListeners(TEST_DATA_ID);
        Assertions.assertNotNull(listeners);
        Assertions.assertEquals(1, listeners.size());
        Assertions.assertTrue(listeners.contains(listener2));

        Set<ConfigurationChangeListener> nonExistentListeners = configuration.getConfigListeners("nonExistentDataId");
        Assertions.assertNull(nonExistentListeners);
    }

    /**
     * Create and add a configuration listener, check if publish notifications are successful
     */
    private ConfigurationChangeListener addPublishListener(String dataId, String expectedContent, CountDownLatch latch)
            throws InterruptedException {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                if (event.getDataId().equals(dataId)) {
                    String newValue = event.getNewValue();
                    if ((expectedContent == null && newValue == null)
                            || (expectedContent != null && expectedContent.equals(newValue))) {
                        latch.countDown();
                    }
                }
            }
        };

        configuration.addConfigListener(dataId, listener);

        // add sleep to ensure listener added successfully
        Thread.sleep(100);
        return listener;
    }

    /**
     * Clean up resources after each test
     */
    private void cleanupAfterTest(String dataId, ConfigurationChangeListener listener) throws InterruptedException {
        // remove the listener
        if (listener != null) {
            configuration.removeConfigListener(dataId, listener);
        }

        // clean redis data
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hdel(CONFIGKEY, dataId);
        }

        // clean up the cache in seataConfig
        try {
            Field seataConfigField = RedisConfiguration.class.getDeclaredField("seataConfig");
            seataConfigField.setAccessible(true);
            Properties seataConfig = (Properties) seataConfigField.get(null);
            seataConfig.remove(dataId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean seataConfig cache", e);
        }

        Thread.sleep(100);
    }
}
