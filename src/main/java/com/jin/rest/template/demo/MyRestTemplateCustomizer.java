package com.jin.rest.template.demo;

import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * @author wu.jinqing
 * @date 2020年10月13日
 */
@Component
public class MyRestTemplateCustomizer implements RestTemplateCustomizer {
    @Override
    public void customize(RestTemplate restTemplate) {
        MyClientHttpRequestInterceptor myClientHttpRequestInterceptor = new MyClientHttpRequestInterceptor();

        restTemplate.getInterceptors().add(myClientHttpRequestInterceptor);
    }
}
