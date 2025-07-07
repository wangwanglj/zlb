package com.lzb.gateway.dto.cache;

import com.alibaba.nacos.common.utils.MapUtil;
import com.lzb.gateway.constants.ServerType;
import com.lzb.gateway.dto.entity.ServerInfo;
import com.lzb.gateway.dto.mapper.ServerInfoMapper;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServerInfoCache {

    /**
     * 服务信息缓存
     * <服务id，服务信息>
     */
    private Map<String, ServerInfo> serverInfoMap;

    /**
     * 服务节点缓存
     * <服务类型，《服务id，服务信息》>
     */
    private Map<Integer, Map<String, ServerInfo>> serviceNodesMap = new ConcurrentHashMap<>();


    @Autowired
    private ServerInfoMapper serverInfoMapper;

    @PostConstruct
    public void loadUserData() {
        serverInfoMap = new HashMap<>();
        List<ServerInfo> users = serverInfoMapper.selectList(null);
        for (ServerInfo serverInfo : users) {
            serverInfoMap.put(serverInfo.getNode_id(), serverInfo);
            Map<String, ServerInfo> serverNodeMap = serviceNodesMap.computeIfAbsent(serverInfo.getType(), k -> new HashMap<>());
            serverNodeMap.put(serverInfo.getNode_id(), serverInfo);
        }
    }


    /**
     * 添加服务信息
     *
     * @param serverInfo
     */
    public void addServerInfo(ServerInfo serverInfo) {
        if (serverInfoMap.containsKey(serverInfo.getNode_id())) {
            serverInfoMap.put(serverInfo.getNode_id(), serverInfo);
            serverInfoMapper.updateById(serverInfo);
        } else {
            serverInfoMapper.insert(serverInfo);
            serverInfoMap.put(serverInfo.getNode_id(), serverInfo);
        }
        Map<String, ServerInfo> serverNodeMap = serviceNodesMap.computeIfAbsent(serverInfo.getType(), k -> new HashMap<>());
        serverNodeMap.put(serverInfo.getNode_id(), serverInfo);
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