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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.Service;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.exception.ConfigNotFoundException;
import org.apache.seata.discovery.registry.RegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The type Nacos registry service.
 */
public class NacosRegistryServiceImpl extends AbstractNacosRegistryServiceImpl implements RegistryService<EventListener> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosRegistryServiceImpl.class);

    private static final ConcurrentMap<String, List<EventListener>> LISTENER_SERVICE_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, List<InetSocketAddress>> CLUSTER_ADDRESS_MAP = new ConcurrentHashMap<>();

    private static volatile NacosRegistryServiceImpl instance;

    private NacosRegistryServiceImpl() {
        initUseSLBWay();
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    static NacosRegistryServiceImpl getInstance() {
        if (instance == null) {
            synchronized (NacosRegistryServiceImpl.class) {
                if (instance == null) {
                    instance = new NacosRegistryServiceImpl();
                }
            }
        }
        return instance;
    }

    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getNamingInstance().registerInstance(getServiceName(), getServiceGroup(), address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        getNamingInstance().deregisterInstance(getServiceName(), getServiceGroup(), address.getAddress().getHostAddress(), address.getPort(), getClusterName());
    }

    @Override
    public void subscribe(String cluster, EventListener listener) throws Exception {
        List<String> clusters = new ArrayList<>();
        clusters.add(cluster);
        LISTENER_SERVICE_MAP.computeIfAbsent(cluster, key -> new ArrayList<>())
                .add(listener);
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
    public List<InetSocketAddress> lookup(String key) throws Exception {
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
            if (!CLUSTER_ADDRESS_MAP.containsKey(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName)) {
                Service service = getNamingMaintainInstance().queryService(DEFAULT_APPLICATION, clusterName);
                String pubnetIp = service.getMetadata().get(PUBLIC_NAMING_SERVICE_META_IP_KEY);
                String pubnetPort = service.getMetadata().get(PUBLIC_NAMING_SERVICE_META_PORT_KEY);
                if (StringUtils.isBlank(pubnetIp) || StringUtils.isBlank(pubnetPort)) {
                    throw new Exception("cannot find service address from nacos naming mata-data");
                }
                InetSocketAddress publicAddress = new InetSocketAddress(pubnetIp,
                        Integer.valueOf(pubnetPort));
                List<InetSocketAddress> publicAddressList = Arrays.asList(publicAddress);
                CLUSTER_ADDRESS_MAP.put(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName, publicAddressList);
                return publicAddressList;
            }
            return CLUSTER_ADDRESS_MAP.get(PUBLIC_NAMING_ADDRESS_PREFIX + clusterName);
        }
        if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
            synchronized (LOCK_OBJ) {
                if (!LISTENER_SERVICE_MAP.containsKey(clusterName)) {
                    List<String> clusters = new ArrayList<>();
                    clusters.add(clusterName);
                    List<Instance> firstAllInstances = getNamingInstance().getAllInstances(getServiceName(), getServiceGroup(), clusters);
                    if (null != firstAllInstances) {
                        List<InetSocketAddress> newAddressList = firstAllInstances.stream()
                                .filter(eachInstance -> eachInstance.isEnabled() && eachInstance.isHealthy())
                                .map(eachInstance -> new InetSocketAddress(eachInstance.getIp(), eachInstance.getPort()))
                                .collect(Collectors.toList());
                        CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                    }
                    subscribe(clusterName, event -> {
                        List<Instance> instances = ((NamingEvent) event).getInstances();
                        if (CollectionUtils.isEmpty(instances) && null != CLUSTER_ADDRESS_MAP.get(clusterName)) {
                            // TODO(www)：这里的info级别是否太低？
                            LOGGER.info("receive empty server list,cluster:{}", clusterName);
                        } else {
                            List<InetSocketAddress> newAddressList = instances.stream()
                                    .filter(eachInstance -> eachInstance.isEnabled() && eachInstance.isHealthy())
                                    .map(eachInstance -> new InetSocketAddress(eachInstance.getIp(), eachInstance.getPort()))
                                    .collect(Collectors.toList());
                            CLUSTER_ADDRESS_MAP.put(clusterName, newAddressList);
                            if (StringUtils.isNotEmpty(transactionServiceGroup)) {
                                removeOfflineAddressesIfNecessary(transactionServiceGroup, clusterName, newAddressList);
                            }
                        }
                    });
                }
            }
        }
        return CLUSTER_ADDRESS_MAP.get(clusterName);
    }

    @Override
    public void close() throws Exception {

    }
}
