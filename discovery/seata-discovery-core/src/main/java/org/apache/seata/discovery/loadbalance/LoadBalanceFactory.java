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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;

import static org.apache.seata.common.DefaultValues.DEFAULT_LOAD_BALANCE;

/**
 * The type Load balance factory.
 *
 */
public class LoadBalanceFactory {

    private static final String CLIENT_PREFIX = "client.";
    /**
     * The constant LOAD_BALANCE_PREFIX.
     */
    public static final String LOAD_BALANCE_PREFIX = CLIENT_PREFIX + "loadBalance.";

    public static final String LOAD_BALANCE_TYPE = LOAD_BALANCE_PREFIX + "type";

    public static final String RANDOM_LOAD_BALANCE = "RandomLoadBalance";

    public static final String XID_LOAD_BALANCE = "XID";

    public static final String ROUND_ROBIN_LOAD_BALANCE = "RoundRobinLoadBalance";

    public static final String CONSISTENT_HASH_LOAD_BALANCE = "ConsistentHashLoadBalance";

    public static final String LEAST_ACTIVE_LOAD_BALANCE = "LeastActiveLoadBalance";

    public static final String WEIGHTED_RANDOM_LOAD_BALANCE = "WeightedRandomLoadBalance";

    /**
     * Get instance.
     *
     * @return the instance
     */
    public static LoadBalance getInstance() {
        String config = ConfigurationFactory.getInstance().getConfig(LOAD_BALANCE_TYPE, DEFAULT_LOAD_BALANCE);
        LoadBalance loadBalance = EnhancedServiceLoader.load(LoadBalance.class, config);
        validateLoadBalanceMode(loadBalance);
        return loadBalance;
    }

    private static void validateLoadBalanceMode(LoadBalance loadBalance) {
        LoadBalanceMode modeAnnotation = loadBalance.getClass().getAnnotation(LoadBalanceMode.class);
        if (modeAnnotation == null) { // TODO：修改逻辑
            throw new IllegalStateException("LoadBalance implementation must be annotated with @LoadBalanceMode: " +
                    loadBalance.getClass().getName());
        }
        String strategyMode = modeAnnotation.value();

        // 因为现在只有两模式，所以根据元数据模式是否启动作为校验依据
        boolean enableMetadata = ConfigurationFactory.CURRENT_FILE_INSTANCE.getBoolean(
                ConfigurationKeys.CLIENT_REGISTRY_ENABLE_METADATA, false);

        // 校验逻辑
        if (enableMetadata && !LoadBalanceModeConstants.METADATA_MODE.equals(strategyMode)) {
            throw new IllegalStateException("LoadBalance mode mismatch: expected METADATA mode, but found " + strategyMode);
        }
        if (!enableMetadata && !LoadBalanceModeConstants.ORIGINAL_MODE.equals(strategyMode)) {
            throw new IllegalStateException("LoadBalance mode mismatch: expected ORIGINAL mode, but found " + strategyMode);
        }
    }
}
