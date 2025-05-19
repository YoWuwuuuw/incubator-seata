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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.naming.NamingMaintainService;
import com.alibaba.nacos.api.naming.NamingService;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.regex.Pattern;

/**
 * The abstract base class for Nacos registry service implementations.
 */
public abstract class AbstractNacosRegistryServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNacosRegistryServiceImpl.class);

    protected static final String DEFAULT_NAMESPACE = "";
    protected static final String DEFAULT_CLUSTER = "default";
    protected static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    protected static final String DEFAULT_APPLICATION = "seata-server";
    protected static final String PRO_SERVER_ADDR_KEY = "serverAddr";
    protected static final String PRO_NAMESPACE_KEY = "namespace";
    protected static final String REGISTRY_TYPE = "nacos";
    protected static final String REGISTRY_CLUSTER = "cluster";
    protected static final String PRO_APPLICATION_KEY = "application";
    protected static final String PRO_GROUP_KEY = "group";
    protected static final String USER_NAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String ACCESS_KEY = "accessKey";
    protected static final String SECRET_KEY = "secretKey";
    protected static final String RAM_ROLE_NAME_KEY = "ramRoleName";
    protected static final String SLB_PATTERN = "slbPattern";
    protected static final String CONTEXT_PATH = "contextPath";
    protected static final String USE_PARSE_RULE = "false";
    protected static final String PUBLIC_NAMING_ADDRESS_PREFIX = "public_";
    protected static final String PUBLIC_NAMING_SERVICE_META_IP_KEY = "publicIp";
    protected static final String PUBLIC_NAMING_SERVICE_META_PORT_KEY = "publicPort";

    protected static final Configuration FILE_CONFIG = ConfigurationFactory.CURRENT_FILE_INSTANCE;
    protected static volatile NamingService naming;
    protected static volatile NamingMaintainService namingMaintain;
    protected static final Object LOCK_OBJ = new Object();
    protected static final Pattern DEFAULT_SLB_REGISTRY_PATTERN = Pattern.compile("(?!.*internal)(?=.*seata).*mse.aliyuncs.com");
    protected static volatile Boolean useSLBWay;

    protected String transactionServiceGroup;

    protected void initUseSLBWay() {
        String configForNacosSLB = FILE_CONFIG.getConfig(
                String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, SLB_PATTERN));

        Pattern patternOfNacosRegistryForSLB = StringUtils.isBlank(configForNacosSLB) ? DEFAULT_SLB_REGISTRY_PATTERN : Pattern.compile(configForNacosSLB);

        useSLBWay = patternOfNacosRegistryForSLB.matcher(getNamingProperties().getProperty(PRO_SERVER_ADDR_KEY)).matches();
    }

    /**
     * Gets naming instance.
     *
     * @return the naming instance
     * @throws Exception the exception
     */
    protected static NamingService getNamingInstance() throws Exception {
        if (naming == null) {
            synchronized (AbstractNacosRegistryServiceImpl.class) {
                if (naming == null) {
                    naming = NacosFactory.createNamingService(getNamingProperties());
                }
            }
        }
        return naming;
    }

    /**
     * Gets naming maintain instance.
     *
     * @return the naming maintain instance
     * @throws Exception the exception
     */
    protected static NamingMaintainService getNamingMaintainInstance() throws Exception {
        if (namingMaintain == null) {
            synchronized (AbstractNacosRegistryServiceImpl.class) {
                if (namingMaintain == null) {
                    namingMaintain = NacosFactory.createMaintainService(getNamingProperties());
                }
            }
        }
        return namingMaintain;
    }

    /**
     * Gets naming properties.
     *
     * @return the naming properties
     */
    protected static Properties getNamingProperties() {
        Properties properties = new Properties();
        properties.setProperty(ConfigurationKeys.IS_USE_CLOUD_NAMESPACE_PARSING, USE_PARSE_RULE);
        properties.setProperty(ConfigurationKeys.IS_USE_ENDPOINT_PARSING_RULE, USE_PARSE_RULE);

        if (System.getProperty(PRO_SERVER_ADDR_KEY) != null) {
            properties.setProperty(PRO_SERVER_ADDR_KEY, System.getProperty(PRO_SERVER_ADDR_KEY));
        } else {
            String address = FILE_CONFIG.getConfig(
                    String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_SERVER_ADDR_KEY));
            if (address != null) {
                properties.setProperty(PRO_SERVER_ADDR_KEY, address);
            }
        }

        if (System.getProperty(PRO_NAMESPACE_KEY) != null) {
            properties.setProperty(PRO_NAMESPACE_KEY, System.getProperty(PRO_NAMESPACE_KEY));
        } else {
            String namespace = FILE_CONFIG.getConfig(
                    String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_NAMESPACE_KEY));

            if (namespace == null) {
                namespace = DEFAULT_NAMESPACE;
            }
            properties.setProperty(PRO_NAMESPACE_KEY, namespace);
        }

        if (!initNacosAuthProperties(properties)) {
            LOGGER.info("Nacos naming auth properties empty.");
        }

        String contextPath = StringUtils.isNotBlank(System.getProperty(CONTEXT_PATH)) ? System.getProperty(CONTEXT_PATH) : FILE_CONFIG.getConfig(
                String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, CONTEXT_PATH));

        if (StringUtils.isNotBlank(contextPath)) {
            properties.setProperty(CONTEXT_PATH, contextPath);
        }

        return properties;
    }

    /**
     * Init nacos auth properties.
     *
     * @param sourceProperties the source properties
     * @return whether auth properties are set
     */
    protected static boolean initNacosAuthProperties(Properties sourceProperties) {
        String userName = StringUtils.isNotBlank(System.getProperty(USER_NAME)) ? System.getProperty(USER_NAME) : FILE_CONFIG.getConfig(
                String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, USER_NAME));

        if (StringUtils.isNotBlank(userName)) {
            String password = StringUtils.isNotBlank(System.getProperty(PASSWORD)) ? System.getProperty(PASSWORD) : FILE_CONFIG.getConfig(
                    String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PASSWORD));

            if (StringUtils.isNotBlank(password)) {
                sourceProperties.setProperty(USER_NAME, userName);
                sourceProperties.setProperty(PASSWORD, password);

                LOGGER.info("Nacos check auth with userName/password.");
                return true;
            }
        } else {
            String accessKey = StringUtils.isNotBlank(System.getProperty(ACCESS_KEY)) ? System.getProperty(ACCESS_KEY) : FILE_CONFIG.getConfig(
                    String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, ACCESS_KEY));
            String ramRoleName = StringUtils.isNotBlank(System.getProperty(RAM_ROLE_NAME_KEY)) ? System.getProperty(RAM_ROLE_NAME_KEY) : FILE_CONFIG.getConfig(
                    String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_CONFIG, REGISTRY_TYPE, RAM_ROLE_NAME_KEY));

            if (StringUtils.isNotBlank(accessKey)) {
                String secretKey = StringUtils.isNotBlank(System.getProperty(SECRET_KEY)) ? System.getProperty(SECRET_KEY) : FILE_CONFIG.getConfig(
                        String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, SECRET_KEY));

                if (StringUtils.isNotBlank(secretKey)) {
                    sourceProperties.put(ACCESS_KEY, accessKey);
                    sourceProperties.put(SECRET_KEY, secretKey);
                    LOGGER.info("Nacos check auth with ak/sk.");
                    return true;
                }
            } else if (StringUtils.isNotBlank(ramRoleName)) {
                sourceProperties.put(RAM_ROLE_NAME_KEY, ramRoleName);
                LOGGER.info("Nacos check auth with ram role.");
                return true;
            }
        }

        return false;
    }

    protected static String getClusterName() {
        return FILE_CONFIG.getConfig(
                String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, REGISTRY_CLUSTER), DEFAULT_CLUSTER);
    }

    protected static String getServiceName() {
        return FILE_CONFIG.getConfig(
                String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_APPLICATION_KEY), DEFAULT_APPLICATION);
    }

    protected static String getServiceGroup() {
        return FILE_CONFIG.getConfig(
                String.join(ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR, ConfigurationKeys.FILE_ROOT_REGISTRY, REGISTRY_TYPE, PRO_GROUP_KEY), DEFAULT_GROUP);
    }
}