package com.jin.rest.template.demo;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * @author wu.jinqing
 * @date 2020年10月12日
 */
@Configuration
public class Config {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder)
    {
        // 会重新生成一个新的RestTemplateBuilder对象
        restTemplateBuilder = restTemplateBuilder.setConnectTimeout(Duration.ofMillis(3000));
        restTemplateBuilder = restTemplateBuilder.setReadTimeout(Duration.ofMillis(3000));

        RestTemplate restTemplate = restTemplateBuilder.build();

        return restTemplate;
    }
}
