package com.lzb.gateway.configuration;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.annotation.NacosProperties;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.client.naming.NacosNamingService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class NacosConfiguration {

    @Value("${nacos.server-addr}")
    private String serverAddr;

    @Value("${nacos.namespace}")
    private String nameSpace;

    @Bean
    public NamingService namingService() throws NacosException {
        Properties properties = new Properties();
        properties.put("nacos.naming.distro.enabled", "true");
        properties.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
        if (StringUtils.isNotEmpty(nameSpace)) {
            properties.put(PropertyKeyConst.NAMESPACE, nameSpace);
        }
        return NacosFactory.createNamingService(properties);
    }
}

