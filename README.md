## RestTemplate

### RestTemplate的属性
|属性名|类型|
|---|---|
|messageConverters|List\<HttpMessageConverter\<?>>|
|errorHandler|ResponseErrorHandler|
|uriTemplateHandler|UriTemplateHandler|
|headersExtractor|ResponseExtractor\<HttpHeaders>|
|interceptors|List\<ClientHttpRequestInterceptor>|
|interceptingRequestFactory|ClientHttpRequestFactory|
|requestFactory|ClientHttpRequestFactory|
|||


### 只能通过代码的方式配置超时时间
> restTemplateBuilder = restTemplateBuilder.setConnectTimeout(Duration.ofMillis(3000));

> restTemplateBuilder = restTemplateBuilder.setReadTimeout(Duration.ofMillis(3000));

### restTemplate发送请求是超时时间在RequestFactory里面设置
```

org.springframework.http.client.SimpleClientHttpRequestFactory.createRequest

@Override
	public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
		HttpURLConnection connection = openConnection(uri.toURL(), this.proxy);
		prepareConnection(connection, httpMethod.name());

		if (this.bufferRequestBody) {
			return new SimpleBufferingClientHttpRequest(connection, this.outputStreaming);
		}
		else {
			return new SimpleStreamingClientHttpRequest(connection, this.chunkSize, this.outputStreaming);
		}
	}

    protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (this.connectTimeout >= 0) {
			connection.setConnectTimeout(this.connectTimeout);
		}
		if (this.readTimeout >= 0) {
			connection.setReadTimeout(this.readTimeout);
		}

		connection.setDoInput(true);

		if ("GET".equals(httpMethod)) {
			connection.setInstanceFollowRedirects(true);
		}
		else {
			connection.setInstanceFollowRedirects(false);
		}

		if ("POST".equals(httpMethod) || "PUT".equals(httpMethod) ||
				"PATCH".equals(httpMethod) || "DELETE".equals(httpMethod)) {
			connection.setDoOutput(true);
		}
		else {
			connection.setDoOutput(false);
		}

		connection.setRequestMethod(httpMethod);
	}

	
```
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


### 通过反射设置超时时间

```
public <T extends RestTemplate> T configure(T restTemplate) {
        // 这里面通过反射设置SimpleClientHttpRequestFactory的readTimeout和connectTimeout
		configureRequestFactory(restTemplate);
		if (!CollectionUtils.isEmpty(this.messageConverters)) {
			restTemplate.setMessageConverters(new ArrayList<>(this.messageConverters));
		}
		if (this.uriTemplateHandler != null) {
			restTemplate.setUriTemplateHandler(this.uriTemplateHandler);
		}
		if (this.errorHandler != null) {
			restTemplate.setErrorHandler(this.errorHandler);
		}
		if (this.rootUri != null) {
			RootUriTemplateHandler.addTo(restTemplate, this.rootUri);
		}
		if (this.basicAuthentication != null) {
			restTemplate.getInterceptors().add(this.basicAuthentication);
		}
		restTemplate.getInterceptors().addAll(this.interceptors);
		if (!CollectionUtils.isEmpty(this.restTemplateCustomizers)) {
			for (RestTemplateCustomizer customizer : this.restTemplateCustomizers) {
				customizer.customize(restTemplate);
			}
		}
		return restTemplate;
	}
```

### 有拦截器使用InterceptingClientHttpRequestFactory，没有使用SimpleClientHttpRequestFactory。

```
org.springframework.http.client.support.InterceptingHttpAccessor.getRequestFactory

public ClientHttpRequestFactory getRequestFactory() {
		List<ClientHttpRequestInterceptor> interceptors = getInterceptors();
		if (!CollectionUtils.isEmpty(interceptors)) {
			ClientHttpRequestFactory factory = this.interceptingRequestFactory;
			if (factory == null) {
				factory = new InterceptingClientHttpRequestFactory(super.getRequestFactory(), interceptors);
				this.interceptingRequestFactory = factory;
			}
			return factory;
		}
		else {
			return super.getRequestFactory();
		}
	}
```














































































































































































































































































































































































































































































































































































































































