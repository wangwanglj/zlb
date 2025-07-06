package com.lzb.gateway.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzb.gateway.utils.SunshineUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * sunshine 服务管理
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/24 11:44
 */
@Slf4j
@Service
public class SunshineService {

    /** 定时任务执行器 */
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    /**
     * 扫描网段
     */
    private static final Queue<String> ipQueue = new ConcurrentLinkedQueue<>();

    public static final int TIMEOUT_MS = 10;      // 超时时间（ms）

    private static final int THREAD_POOL_SIZE = 1000;  // 控制线程池大小

    /**
     * sunshine 请求头
     */
    private HttpHeaders sunshineHeaders;

    @Resource
    private RestTemplate restTemplate;

    @Value("${sunshine.authorization}")
    private String sunshineAuthorization;

    @Value("${sunshine.port}")
    private String sunshinePort;

    @Value("${sunshine.name}")
    private String sunshineServerName;

    @NacosInjected
    private NamingService namingService;

    /**
     * 已注册的 sunshine 客户端
     */
    private static final String SUNSHINE_NAMED_CERTS = "named_certs";

    private final ExecutorService sunshineScanExecutorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new ThreadFactory() {
        final AtomicInteger index = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("sunshine-scan-thread-" + index.incrementAndGet());
            return thread;
        }
    });

    /**
     * 初始化
     */
    @PostConstruct
    public void init(){
        String base64Credentials = new String(Base64.encodeBase64(sunshineAuthorization.getBytes()));

        sunshineHeaders = new HttpHeaders();
        sunshineHeaders.add("Authorization", "Basic " + base64Credentials);
        // 启动统计可用sunshine服务
        executorService.scheduleWithFixedDelay(()->{
            try {
                setIpQueue();
            } catch (Exception e) {
                log.error("scheduleWithFixedDelay.withFixedReportLoginStatisticData:", e);
            }
        },5,15, TimeUnit.SECONDS);
    }


    /**
     * 设置ip队列
     * @throws SocketException
     */
    public void setIpQueue() throws SocketException, ExecutionException, InterruptedException {
        ipQueue.clear();
        List<String[]> allIpRanges = new ArrayList<>();

        // 获取所有可用网段
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue; // 过滤回环、虚拟和未启用的网卡
            }

            // 过滤VMware
            if(networkInterface.getDisplayName().contains("VMware")){
                continue;
            }

            for (InterfaceAddress address : networkInterface.getInterfaceAddresses()) {
                InetAddress inetAddress = address.getAddress();
                if (inetAddress instanceof Inet4Address) {
                    String localIp = inetAddress.getHostAddress();
                    short prefixLength = address.getNetworkPrefixLength();
                    int subnetMask = getSubnetMask(prefixLength);

                    String[] ipRange = getFullIpRange(localIp, subnetMask);
                    log.info("检测到网段: " + ipRange[0] + " - " + ipRange[1]);
                    allIpRanges.add(ipRange);
                }
            }
        }
        // 添加服务器测试网段
//        String[] ipRange = new String[2];
//        ipRange[0]="10.88.88.0";
//        ipRange[1]="10.88.88.255";
//        allIpRanges.add(ipRange);
        // 将所有 IP 加入队列
        for (String[] range : allIpRanges) {
            int start = ipToInt(range[0]);
            int end = ipToInt(range[1]);
            for (int ip = start + 1; ip < end; ip++) {
                ipQueue.add(intToIp(ip));
            }
        }
        log.info("ipQueue.size: " + ipQueue.size());

        long currentTimeMillis = System.currentTimeMillis();

        List<Future<?>> futures = new ArrayList<>();
        // 创建工作线程，让线程池重复利用
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            futures.add(sunshineScanExecutorService.submit(() -> {
                while (!ipQueue.isEmpty()) {
                    String targetIp = ipQueue.poll();
                    if (targetIp != null) {
                        scanIp(targetIp);
                    }
                }
            }));
        }
        // 等待所有任务完成
        for (Future<?> future : futures) {
            future.get();
        }

        log.info("扫描sunshine服务耗时: {} ms",(System.currentTimeMillis() - currentTimeMillis));
    }

    // 扫描 IP
    private void scanIp(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address.isReachable(TIMEOUT_MS)||ip.contains("10.88.88.")) {
                // 获取注册信息
                int connectCount = getSunshineConnectCount(ip);
                registerInstance(namingService, ip, Integer.parseInt(sunshinePort), connectCount==0);
                log.info("sunshine服务地址: {} connectCount:{} ", ip, connectCount);
            }
        } catch (Exception ignored) {}
    }

    // 计算子网掩码
    private int getSubnetMask(short prefixLength) {
        return (int) ((-1L << (32 - prefixLength)) & 0xFFFFFFFF);
    }


    // 计算 IP 范围
    private String[] getFullIpRange(String ip, int subnetMask) {
        int ipInt = ipToInt(ip);
        int networkAddress = ipInt & subnetMask;
        int broadcastAddress = networkAddress | (~subnetMask & 0xFFFFFFFF);

        return new String[]{intToIp(networkAddress), intToIp(broadcastAddress)};
    }

    // IP 转整数
    private int ipToInt(String ip) {
        String[] parts = ip.split("\\.");
        return (Integer.parseInt(parts[0]) << 24) |
                (Integer.parseInt(parts[1]) << 16) |
                (Integer.parseInt(parts[2]) << 8) |
                Integer.parseInt(parts[3]);
    }

    // 整数转 IP
    private String intToIp(int ip) {
        return ((ip >> 24) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                (ip & 0xFF);
    }

    /**
     * 获取sunshine服务地址
     * @param ip
     * @return
     */
    private String getSunshineUrl(String ip){
        return "https://" + ip + ":" + sunshinePort + "/api/clients/list";
    }

    /**
     * 注册实例
     * @param namingService
     * @param ip
     * @param port
     * @throws NacosException
     */
    public void registerInstance(NamingService namingService, String ip, int port, boolean healthy) throws NacosException {
        Instance target = new Instance();
        target.setIp(ip);
        target.setPort(port);
        // 临时节点，断开自动清理
        target.setEphemeral(true);
        // 服务名称
        target.setServiceName("sunshine service");
        target.setHealthy(healthy);
        target.setEnabled(healthy);
        target.setWeight(healthy ? 1 : 0);
        namingService.registerInstance(sunshineServerName, target);
    }

    /**
     * 注册实例
     *
     * @param namingService
     * @param ip
     * @param port
     * @throws NacosException
     */
    public void registerInstance(NamingService namingService, String ip, int port, boolean healthy, Map<String, String> metadata) throws NacosException {
        Instance target = new Instance();
        target.setIp(ip);
        target.setPort(port);
        // 临时节点，断开自动清理
        target.setEphemeral(true);
        // 服务名称
        target.setServiceName("sunshine service");
        target.setHealthy(healthy);
        target.setEnabled(healthy);
        target.setWeight(healthy ? 1 : 0);
        target.setMetadata(metadata);
        namingService.registerInstance(sunshineServerName, target);
    }

    /**
     * 获取sunshine服务头信息
     * @return
     */
    public HttpHeaders getSunshineHeaders() {
        return sunshineHeaders;
    }


    /**
     * 获取当前sunshine服务被串流机器数
     * @param ip
     * @return
     */
    public int getSunshineConnectCount(String ip){
//        String sunshineUrl = getSunshineUrl(ip);
        String sunshineUrl = SunshineUtil.getSessionCount(ip,sunshinePort);
        ResponseEntity<String> response = restTemplate.exchange(sunshineUrl, HttpMethod.GET, new HttpEntity<>(sunshineHeaders), String.class);
        String body = response.getBody();
        JSONObject jsonObject = JSONObject.parseObject(body);
        Object count = jsonObject.get("count");
        if(count==null){
            return 999;
        }
        // 获取注册信息
        return jsonObject.getIntValue("count");
    }
}
