package com.lzb.gateway.controller;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.naming.NamingService;
import com.lzb.gateway.service.SunshineService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


/**
 * Created by lijin on 2019/11/4.
 */
@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @Autowired
    private RestTemplate restTemplate;


    @NacosInjected
    private NamingService namingService;

    @Value("${server.port}")
    private String serverPort;

    @Autowired
    private SunshineService sunshineService;

    @GetMapping("/hello")
    public String  fallback(){
        String plainCredentials = "wangwang:m5888406.";
        String base64Credentials = new String(Base64.encodeBase64(plainCredentials.getBytes()));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Credentials);
//
//        String url = "https://10.40.7.89:47990/api/clients/list";
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        int sunshineConnectCount = sunshineService.getSunshineConnectCount("192.168.9.172");
//        log.info("hello:{}",response);
//        nacosNamingService.selectOneHealthyInstance()
        return "hello:"+sunshineConnectCount;
    }
}
