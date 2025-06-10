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

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.AbstractConfiguration;
import org.apache.seata.config.ConfigFuture;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Protocol;

/**
 * The type Redis configuration
 */
public class RedisConfiguration extends AbstractConfiguration {
    private static volatile RedisConfiguration instance;
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfiguration.class);

    private static final String DEFAULT_GROUP = "SEATA_GROUP";
    private static final String CONFIG_TYPE = "redis";
    private static final String REDIS_FILEKEY_PREFIX = "config.redis.";

    private final Configuration fileConfig = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    private static volatile Properties seataConfig = new Properties();
    private static volatile JedisPool jedisPool;

    private static final ConcurrentMap<String, Set<RedisListener>> CONFIG_LISTENERS_MAP = new ConcurrentHashMap<>();

    private final ExecutorService subscribeExecutor;

    /****
     * Returns the singleton instance of RedisConfiguration, initializing it if necessary.
     *
     * @return the RedisConfiguration singleton instance
     */
    public static RedisConfiguration getInstance() {
        if (instance == null) {
            synchronized (RedisConfiguration.class) {
                if (instance == null) {
                    instance = new RedisConfiguration();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes the RedisConfiguration instance by setting up the Redis connection pool, loading configuration data into the local cache, and preparing the thread pool for subscription tasks.
     *
     * Also registers a JVM shutdown hook to ensure proper cleanup of resources on application termination.
     */
    private RedisConfiguration() {
        initRedisPool();
        initSeataConfig();

        int threadPoolNum = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "thread-pool-num", 50);

        this.subscribeExecutor = new ThreadPoolExecutor(
                threadPoolNum,
                threadPoolNum,
                Integer.MAX_VALUE,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory("redis-subscribe-executor", threadPoolNum));

        // Register the JVM shutdown hook to release resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdownExecutorService(subscribeExecutor);

                if (jedisPool != null && !jedisPool.isClosed()) {
                    jedisPool.close();
                }
            } catch (Exception e) {
                LOGGER.error("Error while shutting down Redis configuration: {}", e.getMessage());
            }
        }));
    }

    /****
     * Retrieves the latest configuration value for the specified dataId, returning a default value if not found or on timeout.
     *
     * @param dataId the configuration key to retrieve
     * @param defaultValue the value to return if the configuration is not found or an error occurs
     * @param timeoutMills the maximum time in milliseconds to wait for the configuration retrieval
     * @return the configuration value for the given dataId, or defaultValue if not found or on error
     */
    @Override
    public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
        String value = seataConfig.getProperty(dataId);
        if (value != null) {
            return value;
        }

        // Time in ConfigFuture, if time out, get() would return the default value
        ConfigFuture configFuture =
                new ConfigFuture(dataId, defaultValue, ConfigFuture.ConfigOperation.GET, timeoutMills);

        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.hget(getConfigKey(), dataId);
            configFuture.setResult(result != null ? result : defaultValue);
        } catch (Exception e) {
            LOGGER.error("Failed to get config from Redis: {}", e.getMessage());
            configFuture.setResult(defaultValue);
        }

        return (String) configFuture.get();
    }

    /****
     * Stores or updates a configuration entry in Redis and notifies listeners of the change.
     *
     * Writes the specified configuration value for the given dataId to the Redis hash, publishes a change notification on the corresponding Redis channel, and updates the local cache. Returns true if the operation succeeds, false otherwise.
     *
     * @param dataId the configuration key to store or update
     * @param content the configuration value to set
     * @param timeoutMills the maximum time to wait for the operation to complete, in milliseconds
     * @return true if the configuration was successfully written to Redis; false otherwise
     */
    @Override
    public boolean putConfig(String dataId, String content, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUT, timeoutMills);

        try (Jedis jedis = jedisPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
            pipeline.hset(getConfigKey(), dataId, content);
            pipeline.publish(
                    getConfigChannelKey() + ":" + dataId,
                    buildConfigChangeMessage(ConfigOperation.PUT, dataId, content));
            pipeline.sync();

            seataConfig.setProperty(dataId, content);
            configFuture.setResult(Boolean.TRUE);
        } catch (Exception e) {
            LOGGER.error("Failed to put config to Redis: {}", e.getMessage());
            configFuture.setResult(Boolean.FALSE);
        }

        return (Boolean) configFuture.get();
    }

    /****
     * Writes a configuration entry only if it does not already exist in Redis.
     *
     * If the configuration for the specified dataId is absent, stores the provided content in Redis and updates the local cache. If the entry already exists, loads the existing value into the local cache without overwriting it. Publishes a change notification if a new entry is created.
     *
     * @param dataId the configuration key to write
     * @param content the value to store if absent
     * @param timeoutMills the maximum time to wait for the operation to complete, in milliseconds
     * @return true if the configuration exists after the operation (either pre-existing or newly written), false if the operation failed
     */
    @Override
    public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
        if (!seataConfig.isEmpty() && seataConfig.containsKey(dataId)) {
            return true;
        }

        ConfigFuture configFuture = new ConfigFuture(dataId, content, ConfigFuture.ConfigOperation.PUTIFABSENT, timeoutMills);

        try (Jedis jedis = jedisPool.getResource()) {
            String configKey = getConfigKey();
            Long result = jedis.hsetnx(configKey, dataId, content);

            if (result == 1) {
                jedis.publish(
                        getConfigChannelKey() + ":" + dataId,
                        buildConfigChangeMessage(ConfigOperation.PUT, dataId, content));
                seataConfig.setProperty(dataId, content);
                configFuture.setResult(Boolean.TRUE);
            } else {
                // If HSETNX returns 0, the key-field already exists in the Redis
                String existingContent = jedis.hget(configKey, dataId);
                seataConfig.setProperty(dataId, existingContent);
                configFuture.setResult(Boolean.TRUE);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to put config if absent to Redis: {}", e.getMessage());
            configFuture.setResult(Boolean.FALSE);
        }

        return (Boolean) configFuture.get();
    }

    /**
     * Removes a configuration entry for the specified dataId from Redis and the local cache, and publishes a removal notification.
     *
     * @param dataId the identifier of the configuration to remove
     * @param timeoutMills the maximum time to wait for the operation to complete, in milliseconds
     * @return true if the configuration was successfully removed; false otherwise
     */
    @Override
    public boolean removeConfig(String dataId, long timeoutMills) {
        ConfigFuture configFuture = new ConfigFuture(dataId, null, ConfigFuture.ConfigOperation.REMOVE, timeoutMills);

        try (Jedis jedis = jedisPool.getResource();
             Pipeline pipeline = jedis.pipelined()) {
            pipeline.hdel(getConfigKey(), dataId);
            pipeline.publish(
                    getConfigChannelKey() + ":" + dataId,
                    buildConfigChangeMessage(ConfigOperation.REMOVE, dataId, ""));
            pipeline.sync();

            seataConfig.remove(dataId);
            configFuture.setResult(Boolean.TRUE);
        } catch (Exception e) {
            LOGGER.error("Failed to remove config from Redis: {}", e.getMessage());
            configFuture.setResult(Boolean.FALSE);
        }

        return (Boolean) configFuture.get();
    }

    /**
     * Registers a listener to receive notifications for configuration changes on the specified dataId.
     *
     * The listener will be notified asynchronously via Redis pub/sub when the configuration for the given dataId is updated or removed.
     *
     * @param dataId the configuration key to listen for changes
     * @param listener the listener to notify of configuration changes
     */
    @Override
    public void addConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        try {
            RedisListener redisListener = new RedisListener(dataId, listener);

            subscribeExecutor.execute(() -> {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(redisListener, getConfigChannelKey() + ":" + dataId);
                } catch (Exception e) {
                    LOGGER.error("Failed to subscribe Redis channel: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to add Redis listener: {}", e.getMessage());
        }
    }

    /**
     * Unregisters a configuration change listener for the specified dataId.
     *
     * Removes the associated Redis subscription and deletes the listener from the internal map.
     * If no listeners remain for the dataId, cleans up the listener set.
     *
     * @param dataId the configuration key to stop listening for changes
     * @param listener the listener to remove
     */
    @Override
    public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {
        if (StringUtils.isBlank(dataId) || listener == null) {
            return;
        }

        Set<RedisListener> redisListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (redisListeners != null) {
            redisListeners.removeIf(redisListener -> {
                if (redisListener.getTargetListener().equals(listener)) {
                    redisListener.unsubscribe();
                    return true;
                }
                return false;
            });

            if (redisListeners.isEmpty()) {
                CONFIG_LISTENERS_MAP.remove(dataId);
            }
        }
    }

    /**
     * Retrieves the set of configuration change listeners registered for the specified dataId.
     *
     * @param dataId the identifier of the configuration item
     * @return a set of registered {@code ConfigurationChangeListener} instances for the dataId, or {@code null} if none are registered
     */
    @Override
    public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
        Set<RedisListener> redisListeners = CONFIG_LISTENERS_MAP.get(dataId);
        if (redisListeners != null) {
            return redisListeners.stream().map(RedisListener::getTargetListener).collect(Collectors.toSet());
        }

        return null;
    }

    /**
     * Returns the configuration type name for this implementation.
     *
     * @return the string "redis"
     */
    @Override
    public String getTypeName() {
        return CONFIG_TYPE;
    }

    /**
     * RedisListener, handles Redis publish events for configuration changes
     */
    public static class RedisListener extends JedisPubSub {
        private final String dataId;
        private final ConfigurationChangeListener listener;
        private volatile boolean running = true;

        /**
         * Constructs a RedisListener for a specific configuration dataId and listener.
         *
         * @param dataId the identifier of the configuration to listen for changes
         * @param listener the listener to notify when configuration changes occur
         */
        public RedisListener(String dataId, ConfigurationChangeListener listener) {
            this.dataId = dataId;
            this.listener = listener;
        }

        /**
         * Handles incoming Redis pub/sub messages for configuration changes, updates the local cache, and notifies the associated listener.
         *
         * @param channel the Redis channel on which the message was received
         * @param message the message containing the configuration operation, dataId, and content
         */
        @Override
        public void onMessage(String channel, String message) {
            if (!running) {
                return;
            }

            String[] parts = message.split("-", 3);
            if (parts.length >= 2) {
                try {
                    ConfigOperation operation = ConfigOperation.valueOf(parts[0].toUpperCase());
                    String dataId = parts[1];
                    String content = parts.length > 2 ? parts[2] : null;

                    switch (operation) {
                        case PUT:
                            if (content != null) {
                                seataConfig.setProperty(dataId, content);
                            }
                            break;
                        case REMOVE:
                            seataConfig.remove(dataId);
                            content = null;
                            break;
                        default:
                            break;
                    }

                    ConfigurationChangeEvent event = new ConfigurationChangeEvent()
                            .setDataId(dataId)
                            .setNewValue(content)
                            .setNamespace(DEFAULT_GROUP);
                    listener.onProcessEvent(event);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Invalid operation in Redis message: {}", parts[0]);
                }
            }
        }

        /****
         * Registers this listener instance in the global map for the specified dataId when a subscription to a Redis channel is established.
         *
         * @param channel the Redis channel to which the subscription was made
         * @param subscribedChannels the number of channels currently subscribed to
         */
        @Override
        public void onSubscribe(String channel, int subscribedChannels) {
            CONFIG_LISTENERS_MAP
                    .computeIfAbsent(dataId, key -> ConcurrentHashMap.newKeySet())
                    .add(this);
        }

        /**
         * Returns the underlying configuration change listener associated with this RedisListener.
         *
         * @return the target ConfigurationChangeListener instance
         */
        public ConfigurationChangeListener getTargetListener() {
            return this.listener;
        }

        /****
         * Stops listening for Redis pub/sub messages and unsubscribes from all channels.
         */
        public void unsubscribe() {
            running = false;
            super.unsubscribe();
        }
    }

    /****
     * Gracefully shuts down the given executor service, waiting up to 5 seconds for termination before forcing shutdown.
     *
     * If interrupted while waiting, the executor is forcibly shut down and the interrupt status is restored.
     */
    private void shutdownExecutorService(ExecutorService executorService) {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Initializes the Redis connection pool using configuration parameters from the file-based configuration.
     *
     * Reads connection and pool settings such as host, port, authentication, database index, and pooling options,
     * and creates a JedisPool instance for managing Redis connections.
     */
    private void initRedisPool() {
        GenericObjectPoolConfig redisConfig = new GenericObjectPoolConfig();
        redisConfig.setTestOnBorrow(fileConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-on-borrow", true));
        redisConfig.setTestOnReturn(fileConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-on-return", false));
        redisConfig.setTestWhileIdle(fileConfig.getBoolean(REDIS_FILEKEY_PREFIX + "test-while-idle", false));

        String serverAddr = fileConfig.getConfig(REDIS_FILEKEY_PREFIX + "server-addr");
        String[] serverArr = NetUtil.splitIPPortStr(serverAddr);
        String host = serverArr[0];
        int port = Integer.parseInt(serverArr[1]);

        String password = fileConfig.getConfig(REDIS_FILEKEY_PREFIX + "password");
        int database = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "db", 0);

        int maxIdle = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "max-idle", 0);
        if (maxIdle > 0) {
            redisConfig.setMaxIdle(maxIdle);
        }
        int minIdle = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "min-idle", 0);
        if (minIdle > 0) {
            redisConfig.setMinIdle(minIdle);
        }
        int maxActive = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "max-active", 0);
        if (maxActive > 0) {
            redisConfig.setMaxTotal(maxActive);
        }
        int maxTotal = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "max-total", 0);
        if (maxTotal > 0) {
            redisConfig.setMaxTotal(maxTotal);
        }
        int maxWait = fileConfig.getInt(
                REDIS_FILEKEY_PREFIX + "max-wait", fileConfig.getInt(REDIS_FILEKEY_PREFIX + "timeout", 0));
        if (maxWait > 0) {
            redisConfig.setMaxWaitMillis(maxWait);
        }
        int numTestsPerEvictionRun = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "num-tests-per-eviction-run", 0);
        if (numTestsPerEvictionRun > 0) {
            redisConfig.setNumTestsPerEvictionRun(numTestsPerEvictionRun);
        }
        int timeBetweenEvictionRunsMillis =
                fileConfig.getInt(REDIS_FILEKEY_PREFIX + "time-between-eviction-runs-millis", 0);
        if (timeBetweenEvictionRunsMillis > 0) {
            redisConfig.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
        }
        int minEvictableIdleTimeMillis = fileConfig.getInt(REDIS_FILEKEY_PREFIX + "min-evictable-idle-time-millis", 0);
        if (minEvictableIdleTimeMillis > 0) {
            redisConfig.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        }

        if (StringUtils.isNullOrEmpty(password)) {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, null, database);
        } else {
            jedisPool = new JedisPool(redisConfig, host, port, Protocol.DEFAULT_TIMEOUT, password, database);
        }
    }

    /****
     * Loads all configuration entries from Redis into the local cache.
     *
     * Retrieves all key-value pairs from the Redis configuration hash and populates the local `seataConfig` properties cache.
     * Logs an error if the initialization fails.
     */
    private void initSeataConfig() {
        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> configMap = jedis.hgetAll(getConfigKey());
            if (!configMap.isEmpty()) {
                configMap.forEach((key, value) -> seataConfig.setProperty(key, value));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to init Redis config: {}", e.getMessage());
        }
    }

    /**
     * Constructs a configuration change message for Redis pub/sub channels.
     *
     * The message format is "operation-dataId-content", where operation is the lowercase name of the configuration operation.
     *
     * @param operation the type of configuration operation (e.g., PUT, REMOVE)
     * @param dataId the identifier of the configuration entry
     * @param content the content associated with the configuration change
     * @return the formatted configuration change message
     */
    private String buildConfigChangeMessage(ConfigOperation operation, String dataId, String content) {
        // like: put-dataId-content
        return String.format("%s-%s-%s", operation.name().toLowerCase(), dataId, content);
    }

    /**
     * Returns the Redis hash key used for storing configuration entries.
     *
     * @return the Redis key in the format "seata:config:SEATA_GROUP"
     */
    private String getConfigKey() {
        // like: seata:config:SEATA_GROUP
        return "seata:config:" + DEFAULT_GROUP;
    }

    /**
     * Returns the Redis pub/sub channel key used for configuration change notifications.
     *
     * @return the Redis channel key in the format "seata:config:channel:SEATA_GROUP"
     */
    private String getConfigChannelKey() {
        // like: seata:config:channel:SEATA_GROUP
        return "seata:config:channel:" + DEFAULT_GROUP;
    }
}
