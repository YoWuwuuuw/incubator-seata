package org.apache.seata.discovery.loadbalance.metadata;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.discovery.loadbalance.LoadBalance;
import org.apache.seata.discovery.loadbalance.LoadBalanceFactory;
import org.apache.seata.discovery.loadbalance.LoadBalanceMode;
import org.apache.seata.discovery.loadbalance.LoadBalanceModeConstants;
import org.apache.seata.discovery.registry.metadata.ServiceInstance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@LoadBalanceMode(LoadBalanceModeConstants.METADATA_MODE)
@LoadLevel(name = LoadBalanceFactory.WEIGHTED_RANDOM_LOAD_BALANCE)
public class WeightedRandomLoadBalance implements LoadBalance {

    @Override
    public <T> T select(List<T> invokers, String xid) {
        if (CollectionUtils.isEmpty(invokers)) {
            return null;
        }

        // 计算总权重
        int totalWeight = 0;
        for (T invoker : invokers) {
            if (invoker instanceof ServiceInstance) {
                ServiceInstance instance = (ServiceInstance) invoker;
                Map<String, String> metadata = instance.getMetadata();
                int weight = Integer.parseInt(metadata.getOrDefault("weight", "1")); // 默认权重为 1
                totalWeight += weight;
            }
        }

        // 随机选择一个权重值
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        int currentWeight = 0;

        // 根据权重选择实例
        for (T invoker : invokers) {
            if (invoker instanceof ServiceInstance) {
                ServiceInstance instance = (ServiceInstance) invoker;
                Map<String, String> metadata = instance.getMetadata();
                int weight = Integer.parseInt(metadata.getOrDefault("weight", "1")); // 默认权重为 1
                currentWeight += weight;

                // 如果随机值落在当前权重区间，则选择该实例
                if (randomWeight < currentWeight) {
                    return invoker;
                }
            }
        }

        // 如果未选择到实例，返回第一个实例
        return invokers.get(0);
    }
}