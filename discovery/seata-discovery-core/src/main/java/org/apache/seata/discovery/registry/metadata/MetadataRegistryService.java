package org.apache.seata.discovery.registry.metadata;

import org.apache.seata.common.metadata.Instance;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.config.ConfigurationFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The interface Registry service for metadata mode
 *
 * @param <T> the type parameter
 */
public interface MetadataRegistryService<T> {
    /**
     * The constant PREFIX_SERVICE_MAPPING.
     */
    String PREFIX_SERVICE_MAPPING = "vgroupMapping.";
    /**
     * The constant PREFIX_SERVICE_ROOT.
     */
    String PREFIX_SERVICE_ROOT = "service";
    /**
     * The constant CONFIG_SPLIT_CHAR.
     */
    String CONFIG_SPLIT_CHAR = ".";

    Set<String> SERVICE_GROUP_NAME = new HashSet<>();

    /**
     * Service cache for metadata mode
     */
    Map<String, Map<String, List<ServiceInstance>>> CURRENT_INSTANCE_MAP = new ConcurrentHashMap<>();

    /**
     * Register.
     *
     * @param address the address
     * @throws Exception the exception
     */
    @Deprecated
    void register(InetSocketAddress address) throws Exception;

    /**
     * Register.
     *
     * @param instance the address
     * @throws Exception the exception
     */
    default void register(Instance instance) throws Exception {
        InetSocketAddress inetSocketAddress =
                new InetSocketAddress(instance.getTransaction().getHost(), instance.getTransaction().getPort());
        register(inetSocketAddress);
    }

    /**
     * Unregister.
     *
     * @param address the address
     * @throws Exception the exception
     */
    @Deprecated
    void unregister(InetSocketAddress address) throws Exception;

    /**
     * Unregister.
     *
     * @param instance the instance
     * @throws Exception the exception
     */
    default void unregister(Instance instance) throws Exception {
        InetSocketAddress inetSocketAddress =
                new InetSocketAddress(instance.getTransaction().getHost(), instance.getTransaction().getPort());
        unregister(inetSocketAddress);
    }

    /**
     * Subscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void subscribe(String cluster, T listener) throws Exception;

    /**
     * Unsubscribe.
     *
     * @param cluster  the cluster
     * @param listener the listener
     * @throws Exception the exception
     */
    void unsubscribe(String cluster, T listener) throws Exception;

    /**
     * Lookup list.
     *
     * @param key the key
     * @return the list
     * @throws Exception the exception
     */
    List<InetSocketAddress> lookup(String key) throws Exception;

    /**
     * Close.
     * @throws Exception the exception
     */
    void close() throws Exception;

    /**
     * Get current service group name
     *
     * @param key service group
     * @return the service group name
     */
    default String getServiceGroup(String key) {
        key = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        if (!SERVICE_GROUP_NAME.contains(key)) {
            SERVICE_GROUP_NAME.add(key);
        }
        return ConfigurationFactory.getInstance().getConfig(key);
    }

    default List<ServiceInstance> aliveLookup(String transactionServiceGroup) {
        Map<String, List<ServiceInstance>> clusterAddressMap = CURRENT_INSTANCE_MAP.computeIfAbsent(transactionServiceGroup,
                k -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);
        List<ServiceInstance> inetSocketAddresses = clusterAddressMap.get(clusterName);
        if (CollectionUtils.isNotEmpty(inetSocketAddresses)) {
            return inetSocketAddresses;
        }

        // fall back to addresses of any cluster
        return clusterAddressMap.values().stream().filter(CollectionUtils::isNotEmpty)
                .findAny().orElse(Collections.emptyList());
    }

    default List<ServiceInstance> refreshAliveLookup(String transactionServiceGroup,
                                                       List<ServiceInstance> aliveAddressInstances) {

        Map<String, List<ServiceInstance>> clusterAddressMap = CURRENT_INSTANCE_MAP.computeIfAbsent(transactionServiceGroup,
                key -> new ConcurrentHashMap<>());

        String clusterName = getServiceGroup(transactionServiceGroup);

        return clusterAddressMap.put(clusterName, aliveAddressInstances);
    }


    /**
     *
     * remove offline addresses if necessary.
     *
     * Intersection of the old and new addresses
     *
     * @param clusterName
     * @param newAddressed
     */
    default void removeOfflineAddressesIfNecessary(String transactionGroupService, String clusterName, Collection<InetSocketAddress> newAddressed) {

        Map<String, List<ServiceInstance>> clusterAddressMap = CURRENT_INSTANCE_MAP.computeIfAbsent(transactionGroupService,
                key -> new ConcurrentHashMap<>());

        List<ServiceInstance> currentInstances = clusterAddressMap.getOrDefault(clusterName, Collections.emptyList());

        List<ServiceInstance> inetSocketAddressInstances = currentInstances.stream()
                .filter(instance -> newAddressed.contains(instance.getAddress()))
                .collect(Collectors.toList());

        // prevent empty update
        if (CollectionUtils.isNotEmpty(inetSocketAddressInstances)) {
            clusterAddressMap.put(clusterName, inetSocketAddressInstances);
        }
    }

}

