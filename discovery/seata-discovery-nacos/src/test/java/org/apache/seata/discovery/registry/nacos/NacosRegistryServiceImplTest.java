package org.apache.seata.discovery.registry.nacos;

import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The type Nacos registryService impl test
 */
public class NacosRegistryServiceImplTest {

    private static final RegistryService service = NacosRegistryServiceImpl.getInstance();

    @BeforeAll
    public static void init() {

    }

    @Test
    public void testGetInstance() {
        NacosRegistryServiceImpl instance = NacosRegistryServiceImpl.getInstance();
        assertInstanceOf(NacosRegistryServiceImpl.class, instance);
    }

    @Test
    public void testRegister() throws Exception {
        // normal condition
        service.register(new InetSocketAddress("127.0.0.1", 8080));

        // test register with invalid address
        assertThrows(IllegalArgumentException.class, () ->  service.register(new InetSocketAddress("127.0.0.1", 0)));
    }

}