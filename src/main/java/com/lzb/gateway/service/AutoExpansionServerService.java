package com.lzb.gateway.service;

import com.lzb.gateway.constants.ServerType;
import com.lzb.gateway.dto.entity.ServerInfo;
import com.lzb.gateway.listener.ServerConfiguration;
import com.lzb.gateway.utils.MagicPackageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.net.SocketException;
import java.util.Collection;
import java.util.List;

/**
 * 自动扩容服务
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/24 11:44
 */
@Slf4j
@Service
public class AutoExpansionServerService {

    @Autowired
    private ServerConfiguration serverConfiguration;

    @Value("${sunshine.auto.expansion.num:2}")
    private int sunshineAutoExpansionNum;

    /**
     * 有效服務檢測，服務不足自啓動
     */
    @Scheduled(cron = "*/5 * * * * ?")
    public void availableServerScan() throws SocketException {
        // 服务端
        Collection<ServerInfo> serviceNodes = serverConfiguration.getAvailableInstances(ServerType.SUNSHINE);
        if(CollectionUtils.isEmpty(serviceNodes)){
            return;
        }

        // 客户端
        Collection<ServerInfo> clientServers = serverConfiguration.getAvailableInstances(ServerType.MOONLIGHT);

        // 已有足够的服务
        int serverNum = serviceNodes.size();
        int clientNum = clientServers.size();
        if (serverNum - clientNum > sunshineAutoExpansionNum) {
            return;
        }

        // 在线迷你主机 大于服务主机 随机唤醒服务主机
        List<ServerInfo> unavailableInstances = serverConfiguration.getUnavailableInstances(ServerType.SUNSHINE);

        if (CollectionUtils.isEmpty(unavailableInstances)) {
            log.warn("扩容 无机器可用！！ serverNum:{} clientNum:{}", serverNum, clientNum);
            return;
        }
        int size = unavailableInstances.size();
        int random = RandomUtils.nextInt(0, size);
        ServerInfo needStartServer = unavailableInstances.get(random);
        String mac = needStartServer.getMac_addr();
        MagicPackageUtils.sendMagicPackage(mac);
        log.info("扩容主机 needStartServer：{}", needStartServer);
    }
}
