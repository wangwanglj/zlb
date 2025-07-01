package com.lzb.gateway.constants;

/**
 * 服务器类型
 * @author Lijin
 * @qq 1175572685
 * @time 2025/6/30 20:55
 */

public enum ServerType {

    /**
     * 算力服务器
     */
    SUNSHINE(1,"sunshine_server"),
    /**
     * 小型客户机
     */
    MOONLIGHT(2,"moonlight_server"),
    ;

    private int type;

    private String name;

    ServerType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static ServerType getServerType(int type) {
        for (ServerType serverType : ServerType.values()) {
            if (serverType.getType() == type) {
                return serverType;
            }
        }
        return SUNSHINE;
    }
}
