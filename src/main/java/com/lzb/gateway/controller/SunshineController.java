package com.lzb.gateway.controller;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzb.gateway.constants.Result;
import com.lzb.gateway.service.SunshineService;
import com.lzb.gateway.utils.IpUtil;
import com.lzb.gateway.utils.SunshineUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * sunshine 协议
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/24 21:04
 */
@RestController
@RequestMapping("/sunshine")
@Slf4j
public class SunshineController {

    @Autowired
    private NacosDiscoveryProperties nacosDiscoveryProperties;

    @Value("${sunshine.name}")
    private String sunshineServerName;

    @Value("${sunshine.port}")
    private String sunshinePort;

    @Autowired
    private SunshineService sunshineService;

    @Resource
    private RestTemplate restTemplate;

    @GetMapping("/randomService")
    public Result<Instance> randomService() {
        NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();

        try {
            Instance instance = namingService.selectOneHealthyInstance(sunshineServerName);
            return Result.success(instance);
        } catch (NacosException e) {
            return Result.failure();
        }
    }

    @GetMapping("/getAllServices")
    public Result<List<Instance>> getAllSunshineServices(){
        NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();

        try {
            List<Instance> allInstances = namingService.getAllInstances(sunshineServerName);
            return Result.success(allInstances);
        } catch (NacosException e) {
            return Result.failure();
        }
    }

    @GetMapping("/pin")
    public Result<Instance> pin(HttpServletRequest request,@RequestParam String ip,@RequestParam String pinCode) {
        NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();

        try {
            List<Instance> allInstances = namingService.getAllInstances(sunshineServerName);
            for (Instance instance : allInstances) {
                // 寻找匹配的ip
                if (instance.getIp().equals(ip)) {
                    // 获取请求方ip
                    String ipAddress = IpUtil.getIpAddress(request);
                    // 请求地址
                    String pinUrl = SunshineUtil.getPinUrl(ip, sunshinePort);
                    // 请求头
                    HttpHeaders sunshineHeaders = sunshineService.getSunshineHeaders();
                    // 请求参数
                    Map<String, String> params = new HashMap<>();
                    params.put("pin", pinCode);
                    params.put("name", ipAddress);
                    HttpEntity<Object> requestEntity = new HttpEntity<>(params,sunshineHeaders);

                    ResponseEntity<String> response = restTemplate.exchange(pinUrl, HttpMethod.POST, requestEntity, String.class);
                    String body = response.getBody();
                    if (body != null && body.contains("true")) {
                        log.info("sunshine服务 连接成功 ipAddress: {} target:{}", ipAddress, ip);
                        // 更新服务状态
                        sunshineService.registerInstance(namingService, ip, Integer.parseInt(sunshinePort), false, Collections.singletonMap("ip", ipAddress));
                        return Result.success(instance);
                    }
                    log.info("sunshine服务 连接失败 ipAddress: {} target:{} result: {}", ipAddress, ip, body);
                    return Result.failure("连接失败，检查code");
                }
            }
        } catch (NacosException e) {
            return Result.failure();
        }
        return Result.failure("ip异常,未找到可以服务");
    }

    @GetMapping("/disconnect")
    public Result<String> disconnect(HttpServletRequest request, @RequestParam String ip) throws NacosException {
        NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();

        // 获取请求方ip
        String ipAddress = IpUtil.getIpAddress(request);
        // 取消所有匹配
        String unpairClientsUrl = SunshineUtil.getUnpairClientsUrl(ip, sunshinePort);
        // 请求头
        HttpHeaders sunshineHeaders = sunshineService.getSunshineHeaders();
        // 更新服务状态
        sunshineService.registerInstance(namingService, ip, Integer.parseInt(sunshinePort), true);

        // 断开连接
        String closeAppsURL = SunshineUtil.getCloseAppsURL(ip, sunshinePort);
        restTemplate.exchange(closeAppsURL, HttpMethod.POST, new HttpEntity<>(sunshineHeaders), String.class);

        // 取消所有匹配
        ResponseEntity<String> response = restTemplate.exchange(unpairClientsUrl, HttpMethod.POST, new HttpEntity<>(sunshineHeaders), String.class);
        String body = response.getBody();
        log.info("sunshine服务,断开连接 ipAddress:{} ip:{} result:{}", ipAddress, ip, body);

        return Result.success(body);
    }
}
