package com.lzb.gateway.listener;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.AbstractEventListener;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
//import com.alibaba.nacos.client.naming.listener.AbstractNamingChangeListener;
//import com.alibaba.nacos.client.naming.listener.NamingChangeEvent;
import com.alibaba.nacos.common.utils.MapUtil;
import com.lzb.gateway.constants.ServerType;
import com.lzb.gateway.dto.entity.ServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务配置管理
 * @author Lijin
 * @qq 1175572685
 * @time 2025/7/3 20:22
 */
@Slf4j
@Configuration
public class ServerConfiguration {

    @Autowired
    private NamingService namingService;

    @Value("${sunshine.name}")
    private String sunshineServerName;

    /**
     * 服务节点缓存
     * <服务类型，《服务id，服务信息》>
     */
    private Map<Integer, Map<String, ServerInfo>> serviceNodesMap = new ConcurrentHashMap<>();

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
        AbstractEventListener listener = new AbstractEventListener() {
            @Override
            public void onEvent(Event event) {
                if(event instanceof NamingEvent){
                    NamingEvent namingEvent = (NamingEvent) event;

                    Map<Integer, Map<String, ServerInfo>> nodeMap = new ConcurrentHashMap<>();
                    List<Instance> instances = namingEvent.getInstances();
                    for (Instance instance : instances) {
                        ServerInfo serviceNode = ServerInfo.fromInstance(instance);
                        Map<String, ServerInfo> serviceNodeMap = nodeMap.computeIfAbsent(serviceNode.getType(), k -> new ConcurrentHashMap<>());
                        serviceNodeMap.put(serviceNode.getNode_id(), serviceNode);
                    }
                    serviceNodesMap =  nodeMap;
                }
            }

            @Override
            public Executor getExecutor() {
                return NACOS_SERVICE_CHANGE_THREAD_POOL;
            }

//            @Override
//            public void onChange(NamingChangeEvent event) {
//                // 移除服务
//                if (event.isRemoved()) {
//                    List<Instance> removedInstances = event.getRemovedInstances();
//                    removedInstances(removedInstances);
//                    log.info("nacos service removed,serviceName={},instances={}", sunshineServerName, removedInstances);
//                }
//                // 新增服务
//                if (event.isAdded()) {
//                    List<Instance> addedInstances = event.getAddedInstances();
//                    addedInstances(addedInstances);
//                    log.info("nacos service added,serviceName={},instances={}", sunshineServerName, addedInstances);
//                }
//                // 修改服务
//                if (event.isModified()) {
//                    List<Instance> modifiedInstances = event.getModifiedInstances();
//                    modifiedInstances(modifiedInstances);
//                    log.info("nacos service modified,serviceName={},instances={}", sunshineServerName, modifiedInstances);
//                }
//            }
        };

        try {
            namingService.subscribe(sunshineServerName, listener);
        } catch (NacosException e) {
            log.error("subscribe nacos service error serviceName={}", sunshineServerName, e);
        }
    }


    /**
     * 移除服务
     * @param instances
     */
    public void removedInstances(List<Instance> instances) {
        Map<String, ServerInfo> changedServiceNodes = new HashMap<>();
        for (Instance instance : instances) {
            ServerInfo serviceNode = ServerInfo.fromInstance(instance);
            if (serviceNode == null) {
                continue;
            }
            Map<String, ServerInfo> serviceNodeMap = serviceNodesMap.computeIfAbsent(serviceNode.getType(), k -> new ConcurrentHashMap<>());
            serviceNodeMap.remove(serviceNode.getNode_id());
            changedServiceNodes.put(serviceNode.getNode_id(), serviceNode);
        }
    }

    /**
     * 修改服务
     * @param instances
     */
    public void modifiedInstances(List<Instance> instances) {
        for (Instance instance : instances) {
            ServerInfo serviceNode = ServerInfo.fromInstance(instance);
            if (serviceNode == null) {
                continue;
            }
            Map<String, ServerInfo> serviceNodeMap = serviceNodesMap.computeIfAbsent(serviceNode.getType(), k -> new ConcurrentHashMap<>());
            serviceNodeMap.put(serviceNode.getNode_id(), serviceNode);
        }
    }

    /**
     * 添加服务
     * @param instances
     */
    public void addedInstances(List<Instance> instances) {
        Map<String, ServerInfo> changedServiceNodes = new HashMap<>();
        for (Instance instance : instances) {
            ServerInfo serviceNode = ServerInfo.fromInstance(instance);
            if (serviceNode == null) {
                continue;
            }
            Map<String, ServerInfo> serviceNodeMap = serviceNodesMap.computeIfAbsent(serviceNode.getType(), k -> new ConcurrentHashMap<>());
            serviceNodeMap.put(serviceNode.getNode_id(), serviceNode);
            changedServiceNodes.put(serviceNode.getNode_id(), serviceNode);
        }
    }

    /**
     * 获取服务列表
     * @param serverType
     * @return
     */
    public Collection<ServerInfo> getServiceNodes(ServerType serverType) {
        Map<String, ServerInfo> serviceNodeMap = serviceNodesMap.get(serverType.getType());
        if (MapUtil.isEmpty(serviceNodeMap)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(serviceNodeMap.values());
    }

    /**
     * 随机可以用服务
     * @param serverType
     * @return
     */
    public ServerInfo randomServerInfo(ServerType serverType) {
        Collection<ServerInfo> serverInfos = getServiceNodes(serverType);
        if (CollectionUtils.isEmpty(serverInfos)) {
            return null;
        }
        List<ServerInfo> serverInfoList = new ArrayList<>();
        for (ServerInfo serverInfo : serverInfos) {
            // 服务是否可用
            if(serverInfo.availableServer()){
                serverInfoList.add(serverInfo);
            }
        }
        if(CollectionUtils.isEmpty(serverInfoList)){
            return null;
        }
        int size = serverInfoList.size();
        int random = RandomUtils.nextInt(0, size);
        return serverInfoList.get(random);
    }

    /**
     * 获取所有可用服务信息
     *
     * @param serverType
     * @return
     */
    public List<ServerInfo> getAvailableInstances(ServerType serverType) {
        List<ServerInfo> availableInstances = new ArrayList<>();
        Collection<ServerInfo> serviceNodes = getServiceNodes(serverType);
        for (ServerInfo serviceNode : serviceNodes) {
            if (serviceNode.availableServer()) {
                availableInstances.add(serviceNode);
            }
        }
        return availableInstances;
    }

    /**
     * 获取不可用服务
     *
     * @param serverType
     * @return
     */
    public List<ServerInfo> getUnavailableInstances(ServerType serverType) {
        List<ServerInfo> unavailableInstances = new ArrayList<>();
        Collection<ServerInfo> serviceNodes = getServiceNodes(serverType);
        for (ServerInfo serviceNode : serviceNodes) {
            if (serviceNode.getStatus() <= 0) {
                unavailableInstances.add(serviceNode);
            }
        }
        return unavailableInstances;
    }

}
