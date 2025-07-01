package com.lzb.gateway.constants;

/**
 * 显卡类型
 * @author Lijin
 * @qq 1175572685
 * @time 2025/7/1 10:03
 */

public enum GraphicsCardType {

    RTX_4060(1, "rtx_4060"),

    RTX_4070(2, "rtx_4070"),

    RTX_4080(3, "rtx_4080"),

    RTX_4090(4, "rtx_4090"),

    RTX_5060(5, "rtx_5060"),

    RTX_5070(6, "rtx_5070"),

    RTX_5080(7, "rtx_5080"),

    RTX_5090(8, "rtx_5090"),
    ;
    /**
     * 显卡类型
     */
    private int type;
    /**
     * 显卡名称
     */
    private String name;
    GraphicsCardType(int type, String name) {
        this.type = type;
        this.name = name;
    }
    public int getType() {
        return type;
    }
    public String getName() {
        return name;
    }
}
