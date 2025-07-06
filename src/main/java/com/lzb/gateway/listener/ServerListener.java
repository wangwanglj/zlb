package com.lzb.gateway.listener;

import com.alibaba.nacos.api.annotation.NacosInjected;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.client.naming.listener.AbstractNamingChangeListener;
import com.alibaba.nacos.client.naming.listener.NamingChangeEvent;
import com.lzb.gateway.domain.ServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务监听器
 * @author Lijin
 * @qq 1175572685
 * @time 2025/7/3 20:22
 */
@Slf4j
@Configuration
public class ServerListener {

    @NacosInjected
    private NamingService namingService;

    @Value("${sunshine.name}")
    private String sunshineServerName;

    /**
     * 服务节点缓存
     * <服务类型，《服务id，服务信息》>
     */
    private final Map<String, Map<Integer, ServerInfo>> serviceNodesMap = new ConcurrentHashMap<>();

    public static ScheduledThreadPoolExecutor NACOS_SERVICE_CHANGE_THREAD_POOL = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {

        AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            int curCount = count.incrementAndGet();
            return new Thread(r, "nacos服务标动监听（线程池）" + curCount);
        }
    });

    @PostConstruct
    public void init() {
        AbstractNamingChangeListener listener = new AbstractNamingChangeListener() {
            @Override
            public Executor getExecutor() {
                return NACOS_SERVICE_CHANGE_THREAD_POOL;
            }

            @Override
            public void onChange(NamingChangeEvent event) {
                // 移除服务
                if (event.isRemoved()) {
                    List<Instance> removedInstances = event.getRemovedInstances();
                    removedInstances(removedInstances);
                    log.info("nacos service removed,serviceName={},instances={}", sunshineServerName, removedInstances);
                }
                // 新增服务
                if (event.isAdded()) {
                    List<Instance> addedInstances = event.getAddedInstances();
                    addedInstances(addedInstances);
                    log.info("nacos service added,serviceName={},instances={}", sunshineServerName, addedInstances);
                }
                // 修改服务
                if (event.isModified()) {
                    List<Instance> modifiedInstances = event.getModifiedInstances();
                    modifiedInstances(modifiedInstances);
                    log.info("nacos service modified,serviceName={},instances={}", sunshineServerName, modifiedInstances);
                }
            }
        };

        try {
            namingService.subscribe(sunshineServerName, listener);
        } catch (NacosException e) {
            log.error("subscribe nacos service error serviceName={}", sunshineServerName, e);
        }
    }


    public void removedInstances(List<Instance> instances) {
        Map<Integer, ServerInfo> changedServiceNodes = new HashMap<>();
        for (Instance instance : instances) {
            ServerInfo serviceNode = ServerInfo.fromInstance(instance);
            if (serviceNode == null) {
                continue;
            }
            Map<Integer, ServerInfo> serviceNodeMap = serviceNodesMap.computeIfAbsent(serviceNode.getServerName(), k -> new ConcurrentHashMap<>());
            serviceNodeMap.remove(serviceNode.getId());
            changedServiceNodes.put(serviceNode.getId(), serviceNode);
        }
    }

    public void removedInstances(List<Instance> instances) {
        Map<Integer,ServerInfo> changedServiceNodes = new HashMap<>();
        for (Instance instance : instances) {
            ServerInfo serviceNode = ServerInfo.instanceToServiceNode(instance);
            if (serviceNode == null) {
                continue;
            }
            Map<Integer, ServerInfo> serviceNodeMap = serviceNodesMap.computeIfAbsent(serviceNode.getServerName(), k -> new ConcurrentHashMap<>());
            serviceNodeMap.remove(serviceNode.getId());
            changedServiceNodes.put(serviceNode.getId(), serviceNode);
        }
    }

    public void modifiedInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            ServerInfo serviceNode = ServerInfo.instanceToServiceNode(instance);
            if (serviceNode == null) {
                continue;
            }
            Map<Integer, ServerInfo> serviceNodeMap = serviceNodesMap.computeIfAbsent(serviceNode.getServerName(), k -> new ConcurrentHashMap<>());
            serviceNodeMap.put(serviceNode.getId(), serviceNode);
        }
    }
}
