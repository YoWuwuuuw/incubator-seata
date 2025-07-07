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

import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import org.apache.seata.common.metadata.ServiceInstance;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.seata.discovery.registry.metadata.MetadataRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Nacos registry service implementation for metadata mode.
 */
public class NacosMetadataRegistryServiceImpl extends AbstractNacosRegistryServiceImpl
        implements MetadataRegistryService<EventListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosMetadataRegistryServiceImpl.class);

    private static final ConcurrentMap<String, List<EventListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<ServiceInstance>> CLUSTER_INSTANCE_MAP = new ConcurrentHashMap<>();

    private static volatile NacosMetadataRegistryServiceImpl instance;

    private NacosMetadataRegistryServiceImpl() {
        initUseSLBWay();
    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    static NacosMetadataRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (NacosMetadataRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new NacosMetadataRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getNamingInstance().registerInstance(getServiceName(), getServiceGroup(), getMetadataNacosInstance(address));
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getNamingInstance().deregisterInstance(getServiceName(), getServiceGroup(), getMetadataNacosInstance(address));
    }

    @Override
    public void subscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>()).add(listener);
        getNamingInstance().subscribe(getServiceName(), getServiceGroup(), clusters, listener);
    }

    @Override
    public void unsubscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        List<EventListener> subscribeList = LISTENER_SERVICE_MAP.get(cluster);
        if (subscribeList != null) {
            List<EventListener> newSubscribeList = subscribeList.stream()
                    .filter(eventListener -> !eventListener.equals(listener))
                    .collect(Collectors.toList());
            LISTENER_SERVICE_MAP.put(cluster, newSubscribeList);
        }
        getNamingInstance().unsubscribe(getServiceName(), getServiceGroup(), clusters, listener);
    }

    @Override
    public void close() throws Exception {
        if (naming != null) {
            try {
                naming.shutDown();
            } catch (Exception e) {
                LOGGER.warn("Error while shutting down Nacos NamingService", e);
            } finally {
                naming = null;
            }
        }

        if (useSLBWay && namingMaintain != null) {
            try {
                namingMaintain.shutDown();
            } catch (Exception e) {
                LOGGER.warn("Error while shutting down Nacos NamingMaintainService", e);
            } finally {
                namingMaintain = null;
            }
        }
    }

    @Override
    public List<ServiceInstance> lookup(String key) throws Exception {
        transactionServiceGroup = key;
        String clusterName = getServiceGroup(key);
        if (clusterName == null) {
            String missingDataId = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
            throw new ConfigNotFoundException("%s configuration item is required", missingDataId);
        }

        if (useSLBWay) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("look up service address of SLB by nacos");
            }
            if (!CLUSTER_INSTANCE_MAP.containsKey(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName)) {
                Service service = getNamingMaintainInstance().queryService(DEFAULT_APPLICATION, clusterName);
                String pubnetIp = service.getMetadata().get(PUBLIC_NAMING_SERVICE_META_IP_KEY);
                String pubnetPort = service.getMetadata().get(PUBLIC_NAMING_SERVICE_META_PORT_KEY);
                if (StringUtils.isBlank(pubnetIp) || StringUtils.isBlank(pubnetPort)) {
                    throw new Exception("cannot find service address from nacos naming mata-data");
                }
                InetSocketAddress publicAddress = new InetSocketAddress(pubnetIp, Integer.valueOf(pubnetPort));
                ServiceInstance serviceInstance = new ServiceInstance(publicAddress, null);
                List<ServiceInstance> publicAddressList = Arrays.asList(serviceInstance);
                CLUSTER_INSTANCE_MAP.put(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName, publicAddressList);
                return publicAddressList;
            }
            return CLUSTER_INSTANCE_MAP.get(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName);
        }

        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            synchronized (LOCK_OBJ) {
                if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
                    List<String> clusters = new ArrayList<>();
                    clusters.add(clusterName);
                    List<Instance> firstAllInstances =
                            getNamingInstance().getAllInstances(getServiceName(), getServiceGroup(), clusters);
                    if (null != firstAllInstances) {
                        CLUSTER_INSTANCE_MAP.put(clusterName, convertToServiceInstances(firstAllInstances));
                    }
                    subscribe(clusterName, event -> {
                        List<Instance> instances = ((NamingEvent) event).getInstances();
                        if (CollectionUtils.isEmpty(instances) && null != CLUSTER_INSTANCE_MAP.get(clusterName)) {
                            LOGGER.info("receive empty server list,cluster:{}", clusterName);
                        } else {
                            List<ServiceInstance> newInstanceList = convertToServiceInstances(instances);
                            CLUSTER_INSTANCE_MAP.put(clusterName, newInstanceList);
                            if (StringUtils.isNotEmpty(transactionServiceGroup)) {
                                removeOfflineAddressesIfNecessary(
                                        transactionServiceGroup, clusterName, newInstanceList);
                            }
                        }
                    });
                }
            }
        }
        return CLUSTER_INSTANCE_MAP.get(clusterName);
    }

    /**
     * Converts a list of Nacos Instance objects to a list of Seata ServiceInstance objects
     *
     * @param instances the list of Nacos Instance objects
     * @return the list of converted ServiceInstance objects
     */
    private List<ServiceInstance> convertToServiceInstances(List<Instance> instances) {
        return instances.stream()
                .filter(eachInstance -> eachInstance.isEnabled() && eachInstance.isHealthy())
                .map(eachInstance -> {
                    InetSocketAddress address = new InetSocketAddress(eachInstance.getIp(), eachInstance.getPort());
                    Map<String, String> metadata = eachInstance.getMetadata();
                    return new ServiceInstance(address, metadata);
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates a Nacos instance with metadata for registration.
     *
     * @param address the service address
     * @return the Nacos instance with metadata
     */
    private Instance getMetadataNacosInstance(InetSocketAddress address) {
        Instance instance = new Instance();
        instance.setIp(address.getAddress().getHostAddress());
        instance.setPort(address.getPort());
        instance.setClusterName(getClusterName());

        Map<String, String> metadata = loadServerMetadata(FILE_CONFIG);
        if (metadata == null) {
            LOGGER.warn("The metadata schema has been started, but the metadata configuration is empty");
        }

        instance.setMetadata(metadata);
        return instance;
    }
}
