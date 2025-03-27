package com.lzb.gateway.configuration;

import com.lzb.gateway.utils.TrustSslUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * 请求模板配置
 * @author Lijin
 * @qq 1175572685
 * @time 2025/3/24 14:51
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public SimpleClientHttpRequestFactory httpRequestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(1000);
        return requestFactory;
    }

    @Bean
    public RestTemplate customRestTemplate() {
        TrustSslUtil.initDefaultSsl();
        return new RestTemplate(httpRequestFactory());
    }
}
