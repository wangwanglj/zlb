package com.lzb.gateway.dto.entity;

import com.alibaba.nacos.api.naming.pojo.Instance;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lzb.gateway.constants.GraphicsCardType;
import com.lzb.gateway.constants.ServerInfoConstants;
import com.lzb.gateway.constants.ServerType;
import lombok.Data;

/**
 * 服务信息
 *
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/26 17:40
 */
@Data
@TableName("server_info")  // 数据库表名
public class ServerInfo {

    /**
     * 服务节点id
     */
    @TableId
    private String node_id;

    /**
     * 服务节点ip地址
     */
    private String ip;

    /**
     * 服务节点mac地址
     */
    private String mac_addr;
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
    private int sun_status;
    /**
     * 服务类型 1：n100 moonlight端 2：sunshine客户端
     */
    private int type;
    /**
     * 显卡类型 {@link GraphicsCardType}
     */
    private int card_type;

    /**
     * 服务名称
     */
    private String serverName;


    /**
     * 服务是否可用 1可用 0已关机
     */
    private int status;


    /**
     * 构建nacos信息
     *
     * @return
     */
    public Instance toInstance() {
        Instance instance = new Instance();
        instance.setIp(ip);
        instance.setPort(Integer.parseInt(port));
        ServerType serverType = ServerType.getServerType(type);
        instance.setServiceName(serverType.getName());
        instance.getMetadata().put(ServerInfoConstants.NODE_ID, node_id);
        instance.getMetadata().put(ServerInfoConstants.SERVER_TYPE, String.valueOf(type));
        instance.getMetadata().put(ServerInfoConstants.MAC_ADDRESS, mac_addr);
        instance.getMetadata().put(ServerInfoConstants.VERSION, version);
        instance.getMetadata().put(ServerInfoConstants.SUNSHINE_STATUS, String.valueOf(sun_status));
        instance.getMetadata().put(ServerInfoConstants.SERVER_STATUS, String.valueOf(status));
        instance.getMetadata().put(ServerInfoConstants.CARD_TYPE, String.valueOf(card_type));
        // 7服务节点持久化
//        instance.setEphemeral(false);
        instance.setHealthy(sun_status <= 0);
        instance.setEnabled(status > 0);
        instance.setWeight(sun_status <= 0 ? 1 : 0);
        return instance;
    }

    /**
     * 构建服务信息
     *
     * @param instance
     * @return
     */
    public static ServerInfo fromInstance(Instance instance) {
        ServerInfo serverInfo = new ServerInfo();
        serverInfo.setNode_id(instance.getMetadata().get(ServerInfoConstants.NODE_ID));
        serverInfo.setType(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.SERVER_TYPE)));
        ServerType serverType = ServerType.getServerType(serverInfo.getType());
        serverInfo.setServerName(serverType.getName());
        serverInfo.setIp(instance.getIp());
        serverInfo.setMac_addr(instance.getMetadata().get(ServerInfoConstants.MAC_ADDRESS));
        serverInfo.setPort(String.valueOf(instance.getPort()));
        serverInfo.setVersion(instance.getMetadata().get(ServerInfoConstants.VERSION));
        serverInfo.setSun_status(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.SUNSHINE_STATUS)));
        serverInfo.setStatus(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.SERVER_STATUS)));
        serverInfo.setCard_type(Integer.parseInt(instance.getMetadata().get(ServerInfoConstants.CARD_TYPE)));
        return serverInfo;
    }

    /**
     * 服务是否可用
     *
     * @return
     */
    public boolean availableServer() {
        return sun_status <= 0 && status > 0;
    }

}
