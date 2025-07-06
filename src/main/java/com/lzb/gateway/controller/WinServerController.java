//package com.lzb.gateway.controller;
//
//
//import com.lzb.gateway.domain.ClientConfig;
//import com.lzb.gateway.listener.ClientConfigListener;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * windows 系统信息
// * @author Lijin
// * @qq 1175572685
// * @time 2025/3/24 21:04
// */
//@RestController
//@RequestMapping("/winServer")
//@Slf4j
//public class WinServerController {
//
//    @Autowired
//    private ClientConfigListener clientConfigListener;
//
//    @GetMapping("/winServerConfig")
//    public ClientConfig getWinServerConfig() {
//        return clientConfigListener.clientConfig();
//    }
//}
