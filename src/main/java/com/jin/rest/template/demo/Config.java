package com.jin.rest.template.demo;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wu.jinqing
 * @date 2020年10月12日
 */
@Configuration
public class Config {
    @Value("${rest.template.connect.timeout.millis}")
    private long restTemplateConnectTimeoutMillis;
    @Value("${rest.template.read.timeout.millis}")
    private long restTemplateReadTimeoutMillis;
    private final ObjectProvider<HttpMessageConverters> messageConverters;

    private final ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers;

    public Config(ObjectProvider<HttpMessageConverters> messageConverters, ObjectProvider<RestTemplateCustomizer> restTemplateCustomizers) {
        this.messageConverters = messageConverters;
        this.restTemplateCustomizers = restTemplateCustomizers;
    }

    @Bean
    public RestTemplate restTemplate()// 参考：org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration
    {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        HttpMessageConverters converters = this.messageConverters.getIfUnique();
        if (converters != null) {
            builder = builder.messageConverters(converters.getConverters());
        }

        List<RestTemplateCustomizer> customizers = this.restTemplateCustomizers
                .orderedStream().collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(customizers)) {
            builder = builder.customizers(customizers);
        }

        // 会重新生成一个新的RestTemplateBuilder对象
        builder = builder.setConnectTimeout(Duration.ofMillis(restTemplateConnectTimeoutMillis));
        builder = builder.setReadTimeout(Duration.ofMillis(restTemplateReadTimeoutMillis));

        RestTemplate restTemplate = builder.build();

        return restTemplate;
    }
}
