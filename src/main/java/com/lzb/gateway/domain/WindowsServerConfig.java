package com.lzb.gateway.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * window 服务信息
 */
@Data
public class WindowsServerConfig {


    public static final String DATA_ID = "win-server-config";

    /**
     * 迷你主机mac列表
     */
    private List<WindowsServerInfo> minList=new ArrayList<>();

    /**
     * 服务器主机mac列表
     */
    private List<WindowsServerInfo> serverList = new ArrayList<>();
}
