/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lzb.gateway.listener;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;

import java.util.Date;

import javax.annotation.PostConstruct;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.lzb.gateway.coverter.PojoNacosConfigConverter;
import com.lzb.gateway.domain.ClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Timeout {@link NacosConfigListener}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 0.1.0
 */
@Configuration
public class ClientConfigListener {

	private static final Logger logger = LoggerFactory
			.getLogger(ClientConfigListener.class);

	private ClientConfig clientConfig = new ClientConfig();

	@Autowired
	private PojoNacosConfigConverter pojoNacosConfigConverter;


	@Autowired
	private NacosConfigManager nacosConfigManager;

	@PostConstruct
	public void init() throws Exception {
		ConfigService configService = nacosConfigManager.getConfigService();
		String config = configService.getConfig(ClientConfig.DATA_ID, DEFAULT_GROUP, 1000);
		if(config == null){
			// Initialize
			clientConfig.setNasApiUrl("http://192.168.9.141/api/v2.0/");
			clientConfig.setNasToken("1-WvOjR7HFrYFDkd4O29x32O3C7c88lPo9YvRScYhlrOCGPcVRXecToN47hnOec3XZ");
			clientConfig.setMainWinVersion("win10/4060-worker@playnite");
			clientConfig.setMainGameVersion("game/game@playnite");
			clientConfig.setMainPortalId(1);
			clientConfig.setCreated(new Date());
			// Serialization
			ObjectMapper objectMapper = new ObjectMapper();
			String content = objectMapper.writeValueAsString(clientConfig);
			// Publish
			configService.publishConfig(ClientConfig.DATA_ID, DEFAULT_GROUP, content);
		}else {
			clientConfig =pojoNacosConfigConverter.convert(config);
		}

		// 添加监听
		configService.addListener(ClientConfig.DATA_ID, DEFAULT_GROUP, new AbstractListener() {
			@Override
			public void receiveConfigInfo(String configInfo) {
				clientConfig =pojoNacosConfigConverter.convert(configInfo);
				logger.info("onReceived(Pojo) : {}", configInfo);
			}
		});
	}
	public ClientConfig clientConfig(){
		return clientConfig;
	}
}
