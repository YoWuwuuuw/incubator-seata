package org.apache.seata.discovery.registry.nacos;

import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The type Nacos registryService impl test
 */
public class NacosRegistryServiceImplTest {

    private static final String GROUP_NAME = "default_tx_group";

    private static final RegistryService service = NacosRegistryServiceImpl.getInstance();
    private static final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 8080);
    private static final InetSocketAddress address1 = new InetSocketAddress("127.0.0.1", 8081);

    @BeforeAll
    public static void init() {

    }

    @AfterEach
    public void tearDown() throws Exception {
        List<InetSocketAddress> lookup = service.lookup(GROUP_NAME);
        for (InetSocketAddress instance : lookup) {
            service.unregister(instance);
        }
    }

    @Test
    public void testGetInstance() {
        NacosRegistryServiceImpl instance = NacosRegistryServiceImpl.getInstance();
        assertInstanceOf(NacosRegistryServiceImpl.class, instance);
    }

    @Order(1)
    @Test
    public void testAll() throws Exception {
        /* 1.When there is only one instance register(), and that instance unregister(),
             lookup will always return the previous cached list instead of updating the cache to empty */
        service.register(address);
        Thread.sleep(1000); // wait for Nacos loading
        assertEquals(address, service.lookup(GROUP_NAME).get(0));

        service.unregister(address);
        Thread.sleep(1000); // wait for Nacos loading
        assertEquals(1, service.lookup(GROUP_NAME).size());

        // 2.test with invalid address
        assertThrows(IllegalArgumentException.class, () -> service.register(new InetSocketAddress("127.0.0.1", 0)));
        assertThrows(IllegalArgumentException.class, () -> service.unregister(new InetSocketAddress("127.0.0.1", 0)));

        /* 3.When there are multiple instances register (),
             the return value of lookup is the actual value when the instance goes online or offline */
        service.register(address);
        service.register(address1);
        Thread.sleep(10000); // wait for Nacos loading
        assertEquals(2, service.lookup("default_tx_group").size());

        service.unregister(address);
        Thread.sleep(10000); // wait for Nacos loading
        assertEquals(1, service.lookup("default_tx_group").size());
    }

}