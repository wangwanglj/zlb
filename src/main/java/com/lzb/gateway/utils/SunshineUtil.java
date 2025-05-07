package com.lzb.gateway.utils;

/**
 * sunshine 工具类
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/27 11:12
 */
public class SunshineUtil {

    /**
     * 获取匹配的url
     *
     * @param ip
     * @return
     */
    public static String getPairedClientsUrl(String ip, String sunshinePort) {
        return "https://" + ip + ":" + sunshinePort + "/api/clients/list";
    }

    /**
     * 获取匹配的url
     * @param ip
     * @param sunshinePort
     * @return
     */
    public static String getPinUrl(String ip, String sunshinePort) {
        return "https://" + ip + ":" + sunshinePort + "/api/pin";
    }

    /**
     * 取消所有匹配
     * @param ip
     * @param sunshinePort
     * @return
     */
    public static String getUnpairClientsUrl(String ip,String sunshinePort) {
        return "https://" + ip + ":" + sunshinePort + "/api/clients/unpair-all";
    }

    /**
     * 关闭所有应用
     * @param ip
     * @param sunshinePort
     * @return
     */
    public static String getCloseAppsURL(String ip,String sunshinePort){
        return "https://" + ip + ":" + sunshinePort + "/api/apps/close";
    }

    /**
     * 重置
     * @param ip
     * @param sunshinePort
     * @return
     */
    public static String getRestDisplayUrl(String ip,String sunshinePort){
        return "https://" + ip + ":" + sunshinePort + "/api/reset-display-device-persistence";
    }

    /**
     * 获取连接数
     * @param ip
     * @param sunshinePort
     * @return
     */
    public static String getSessionCount(String ip,String sunshinePort){
        return "https://" + ip + ":" + sunshinePort + "/api/sessionCount";
    }
}
