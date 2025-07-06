package com.lzb.gateway.constants;

/**
 * 服务器信息常量
 * @author Lijin
 * @qq 1175572685
 * @time 2025/6/30 20:16
 */

public interface ServerInfoConstants {

    /**
     * 服务器信息唯一id
     */
    String NODE_ID = "node_id";

    /**
     * 服务器类型 1：n100 moonlight端 2：sunshine客户端
     */
    String SERVER_TYPE = "type";

    /**
     * 服务器mac地址
     */
    String MAC_ADDRESS = "mac_addr";

    /**
     * sunshine端口
     */
    String SUNSHINE_PORT = "port";

    /**
     * 服务器版本
     */
    String VERSION = "version";

    /**
     * sunshine状态
     * 1：有连接 0：无连接
     */
    String SUNSHINE_STATUS = "sun_status";

    /**
     * 服务状态
     * 1：可用 0：已关机
     */
    String SERVER_STATUS = "status";

    /**
     * 显卡类型 {@link GraphicsCardType}
     */
    String CARD_TYPE = "card_type";
}
