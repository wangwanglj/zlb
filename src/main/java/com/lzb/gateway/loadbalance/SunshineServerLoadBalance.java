//package com.lzb.gateway.loadbalance;
//
//import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
//import com.alibaba.nacos.api.config.listener.AbstractListener;
//import com.alibaba.nacos.api.exception.NacosException;
//import com.alibaba.nacos.api.naming.NamingService;
//import com.alibaba.nacos.api.naming.pojo.Instance;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.PostConstruct;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.Executor;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import java.util.concurrent.ThreadFactory;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * sunshine负载均衡算法
// * @author Lijin
// * @qq 1175572685
// * @time 2025/3/26 17:39
// */
//@Slf4j
//@Component
//public class SunshineServerLoadBalance {
//
//    /**
//     * 服务节点缓存
//     * <服务id，服务信息>
//     */
//    private final Map<String, SunshineServerInfo> serviceNodesMap = new ConcurrentHashMap<>();
//
//    /**
//     * 可用服务节点缓存
//     */
//    private final Map<String, SunshineServerInfo> ennableServiceNodesMap = new ConcurrentHashMap<>();
//
//    @Autowired
//    private NacosDiscoveryProperties nacosDiscoveryProperties;
//
//    public static ScheduledThreadPoolExecutor NACOS_SERVICE_CHANGE_THREAD_POOL = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
//
//        AtomicInteger count = new AtomicInteger(0);
//
//        @Override
//        public Thread newThread(Runnable r) {
//            int curCount = count.incrementAndGet();
//            return new Thread(r, "nacos服务标动监听（线程池）" + curCount);
//        }
//    });
//
//    @PostConstruct
//    public void init(){
//        NamingService namingService = nacosDiscoveryProperties.namingServiceInstance();
//        AbstractListener listener = new AbstractListener() {
//            @Override
//            public Executor getExecutor() {
//                return NACOS_SERVICE_CHANGE_THREAD_POOL;
//            }
//
//            @Override
//            public void onChange(NamingChangeEvent event) {
//                // 移除服务
//                if (event.isRemoved()) {
//                    List<Instance> removedInstances = event.getRemovedInstances();
//                    removedInstances(removedInstances);
//                    log.info("nacos service removed,serviceName:{},instances:{}", serverType, removedInstances);
//                }
//                // 新增服务
//                if (event.isAdded()) {
//                    List<Instance> addedInstances = event.getAddedInstances();
//                    addedInstances(addedInstances);
//                    log.info("nacos service added,serviceName:{},instances:{}", serverType, addedInstances);
//                }
//                // 修改服务
//                if (event.isModified()) {
//                    List<Instance> modifiedInstances = event.getModifiedInstances();
//                    modifiedInstances(modifiedInstances);
//                    log.info("nacos service modified,serviceName:{},instances:{}", serverType, modifiedInstances);
//                }
//            }
//        };
//
//        try {
//            namingService.subscribe(serverType.name(), listener);
//        } catch (NacosException e) {
//            log.error("subscribe nacos service error serviceName:{}", serverType, e);
//        }
//    }
//}
