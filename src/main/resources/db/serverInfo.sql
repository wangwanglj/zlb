CREATE TABLE `server_info` (
                          `node_id` varchar(255) NOT NULL,
                          `ip` varchar(255) DEFAULT NULL,
                          `mac_addr` varchar(255) DEFAULT NULL,
                          `port` varchar(255) DEFAULT NULL,
                          `version` varchar(255) DEFAULT NULL,
                          `sun_status` int DEFAULT NULL,
                          `type` int DEFAULT NULL,
                          `card_type` int DEFAULT NULL,
                          `serverName` varchar(255) DEFAULT NULL,
                          `status` int DEFAULT NULL,
                          PRIMARY KEY (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务信息';