## RestTemplate


### 初始化RestTemplate

``` 
@Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder)
    {
        // 会重新生成一个新的RestTemplateBuilder对象
        restTemplateBuilder = restTemplateBuilder.setConnectTimeout(Duration.ofMillis(3000));
        restTemplateBuilder = restTemplateBuilder.setReadTimeout(Duration.ofMillis(3000));
            
        RestTemplate restTemplate = restTemplateBuilder.build();

        return restTemplate;
    }
```

### 自定义配置RestTemplate

#### 配置自定义ClientHttpRequestInterceptor
``` 
@Component
public class MyRestTemplateCustomizer implements RestTemplateCustomizer {
    @Override
    public void customize(RestTemplate restTemplate) {
        MyClientHttpRequestInterceptor myClientHttpRequestInterceptor = new MyClientHttpRequestInterceptor();

        restTemplate.getInterceptors().add(myClientHttpRequestInterceptor);
    }
}
```



## 无拦截器
### 基于SimpleBufferingClientHttpRequest，特点：无拦截器
### RestTemplate.doExecute() -> AbstractClientHttpRequest.execute() -> AbstractBufferingClientHttpRequest.executeInternal()
### -> SimpleBufferingClientHttpRequest.executeInternal()。



## 有拦截器的执行流程 InterceptingClientHttpRequestFactory -> InterceptingClientHttpRequest, 最终还是委托给SimpleBufferingClientHttpRequest。

### RestTemplate.doExecute() -> AbstractClientHttpRequest.execute() -> AbstractBufferingClientHttpRequest.executeInternal()
### -> InterceptingClientHttpRequest.executeInternal() 隐式递归执行拦截器 -> 委托给SimpleBufferingClientHttpRequest.executeInternal()。


















































































































































































































































































































































































































































































































































































































































