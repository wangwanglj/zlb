package com.lzb.gateway.domain;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.lzb.gateway.constants.GraphicsCardType;
import com.lzb.gateway.constants.ServerInfoConstants;
import com.lzb.gateway.constants.ServerType;
import lombok.Data;

/**
 * 服务信息
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/26 17:40
 */
@Data
public class ServerInfo {

    /**
     * 服务节点id
     */
    private String nodeId;

    /**
     * 服务类型 1：n100 moonlight端 2：sunshine客户端
     */
    private int type;

    /**
     * 服务节点ip地址
     */
    private String ip;


    /**
     * 服务节点mac地址
     */
    private String macAddress;

    /**
     * sunshine服务端口号
     */
    private String port;

    /**
     * 服务节点版本号
     */
    private String version;

    /**
     * sunshine服务状态 1：有连接 0：无连接
     */
    private int sunshineStatus;

    /**
     * 显卡类型 {@link GraphicsCardType}
     */
    private int cardType;

    /**
     * 构建nacos信息
     * @return
     */
    public Instance toInstance() {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(Integer.parseInt(port));
        ServerType serverType = ServerType.getServerType(type);
        instance.setServiceName(serverType.getName());
        instance.getMetadata().put(ServerInfoConstants.NODE_ID, nodeId);
        instance.getMetadata().put(ServerInfoConstants.SERVER_TYPE, String.valueOf(type));
        instance.getMetadata().put(ServerInfoConstants.MAC_ADDRESS, macAddress);
        instance.getMetadata().put(ServerInfoConstants.VERSION, version);
        instance.getMetadata().put(ServerInfoConstants.SUNSHINE_STATUS, String.valueOf(sunshineStatus));
        instance.getMetadata().put(ServerInfoConstants.CARD_TYPE, String.valueOf(cardType));
        // sunshine服务节点持久化
        if (serverType == ServerType.SUNSHINE) {
            instance.setEphemeral(false);
        }
        instance.setHealthy(sunshineStatus <= 0);
        instance.setEnabled(sunshineStatus <= 0);
        instance.setWeight(sunshineStatus <= 0 ? 1 : 0);
        return instance;
    }

    /**
     * 构建服务信息
     * @param instance
     * @return
     */
    public static ServerInfo fromInstance(Instance instance) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setNodeId(instance.getMetadata().get(ServerInfoConstants.NODE_ID));
        serverInfo.setType(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.SERVER_TYPE)));
        serverInfo.setIp(instance.getIp());
        serverInfo.setMacAddress(instance.getMetadata().get(ServerInfoConstants.MAC_ADDRESS));
        serverInfo.setPort(String.valueOf(instance.getPort()));
        serverInfo.setVersion(instance.getMetadata().get(ServerInfoConstants.VERSION));
        serverInfo.setSunshineStatus(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.SUNSHINE_STATUS)));
        serverInfo.setCardType(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.CARD_TYPE)));
        return serverInfo;
    }

}
