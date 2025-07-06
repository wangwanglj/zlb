//package com.lzb.gateway.listener;
//
//
//import com.alibaba.cloud.nacos.NacosConfigManager;
//import com.alibaba.fastjson.JSON;
//import com.alibaba.nacos.api.config.ConfigService;
//import com.alibaba.nacos.api.config.listener.AbstractListener;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.lzb.gateway.domain.WindowsServerConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Configuration;
//
//import javax.annotation.PostConstruct;
//
//import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
//
//@Slf4j
//@Configuration
//public class WindowsServerConfigListener {
//
//    private WindowsServerConfig windowsServerConfig = new WindowsServerConfig();
//
//    @Autowired
//    private NacosConfigManager nacosConfigManager;
//
//    @PostConstruct
//    public void init() throws Exception {
//        ConfigService configService = nacosConfigManager.getConfigService();
//        String config = configService.getConfig(WindowsServerConfig.DATA_ID, DEFAULT_GROUP, 1000);
//        if(config == null){
//            // Serialization
//            ObjectMapper objectMapper = new ObjectMapper();
//            String content = objectMapper.writeValueAsString(windowsServerConfig);
//            // Publish
//            configService.publishConfig(WindowsServerConfig.DATA_ID, DEFAULT_GROUP, content);
//        }else {
//            windowsServerConfig = JSON.parseObject(config, WindowsServerConfig.class);
//        }
//
//        // 添加监听
//        configService.addListener(WindowsServerConfig.DATA_ID, DEFAULT_GROUP, new AbstractListener() {
//            @Override
//            public void receiveConfigInfo(String configInfo) {
//                windowsServerConfig = JSON.parseObject(configInfo, WindowsServerConfig.class);
//                log.info("接受配置变化 onReceived : {}", configInfo);
//            }
//        });
//    }
//
//    public WindowsServerConfig getWindowsServerInfo() {
//        return windowsServerConfig;
//    }
//}
