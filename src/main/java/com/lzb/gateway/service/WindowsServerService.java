package com.lzb.gateway.service;

import com.lzb.gateway.domain.WindowsServerConfig;
import com.lzb.gateway.domain.WindowsServerInfo;
import com.lzb.gateway.listener.WindowsServerConfigListener;
import com.lzb.gateway.utils.IpUtil;
import com.lzb.gateway.utils.MagicPackageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * windows 服务管理
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/24 11:44
 */
@Slf4j
@Service
public class WindowsServerService {

    @Autowired
    private WindowsServerConfigListener windowsServerConfigListener;

    /**
     * 有效服務檢測，服務不足自啓動
     */
    @Scheduled(cron = "*/5 * * * * ?")
    public void availableServerScan() throws SocketException {
        WindowsServerConfig windowsServerInfo = windowsServerConfigListener.getWindowsServerInfo();

        // 获取在綫迷你主機
        List<WindowsServerInfo> availableMinList = new ArrayList<>();
        List<WindowsServerInfo> minList = windowsServerInfo.getMinList();
        for (WindowsServerInfo serverInfo : minList) {
            String ip = serverInfo.getIp();
            if (IpUtil.checkIpReachable(ip)) {
                availableMinList.add(serverInfo);
            }
        }

        // 获取在线不在服务主机
        List<WindowsServerInfo> availableServerList = new ArrayList<>();
        List<WindowsServerInfo> offServerList = new ArrayList<>();
        List<WindowsServerInfo> serverList = windowsServerInfo.getServerList();
        for (WindowsServerInfo serverInfo : serverList) {
            String ip = serverInfo.getIp();
            if (IpUtil.checkIpReachable(ip)) {
                availableServerList.add(serverInfo);
            }else {
                offServerList.add(serverInfo);
            }
        }

        // 在线迷你主机 大于服务主机 随机唤醒服务主机
        if(availableMinList.size()>availableServerList.size()){
            if(CollectionUtils.isEmpty(offServerList)){
                log.error("擴容主機，無空閑主機可用 availableMinList：{} serverList：{}",availableMinList,serverList);
                return;
            }

            WindowsServerInfo needStartServer = offServerList.get(0);
            String mac = needStartServer.getMac();
            MagicPackageUtils.sendMagicPackage(mac);
            log.info("扩容主机 needStartServer：{}",needStartServer);
        }
    }
}
