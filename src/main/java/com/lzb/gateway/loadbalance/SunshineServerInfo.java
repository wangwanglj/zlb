package com.lzb.gateway.loadbalance;

import com.alibaba.nacos.api.naming.pojo.Instance;
import lombok.Data;

/**
 * sunshine服务信息
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/26 17:40
 */
@Data
public class SunshineServerInfo extends Instance {

    /**
     * 服务是否可用
     */
    private boolean enabled;
}
