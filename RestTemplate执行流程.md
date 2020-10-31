## RestTemplate执行流程


``` 
@Override
	public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType, Object... uriVariables)
			throws RestClientException {

		RequestCallback requestCallback = acceptHeaderRequestCallback(responseType);
		// 响应结果提取器，通过MessageConverter将响应转换成对象
		ResponseExtractor<ResponseEntity<T>> responseExtractor = responseEntityExtractor(responseType);
		return nonNull(execute(url, HttpMethod.GET, requestCallback, responseExtractor, uriVariables));
	}
```


``` 
public ResponseEntityResponseExtractor(@Nullable Type responseType) {
			if (responseType != null && Void.class != responseType) {
				this.delegate = new HttpMessageConverterExtractor<>(responseType, getMessageConverters(), logger);
			}
			else {
				this.delegate = null;
			}
		}
```

``` 
@Override
	@Nullable
	public <T> T execute(String url, HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor, Object... uriVariables) throws RestClientException {
        // 构建URL，设置url参数，参数形式：{参数:默认值}，按参数出现顺序，一个一个赋值。
		URI expanded = getUriTemplateHandler().expand(url, uriVariables);
		return doExecute(expanded, method, requestCallback, responseExtractor);
	}
```


``` 
@Nullable
	protected <T> T doExecute(URI url, @Nullable HttpMethod method, @Nullable RequestCallback requestCallback,
			@Nullable ResponseExtractor<T> responseExtractor) throws RestClientException {

		Assert.notNull(url, "URI is required");
		Assert.notNull(method, "HttpMethod is required");
		ClientHttpResponse response = null;
		try {
		    // 如果有拦截器的话默认创建InterceptingClientHttpRequestFactory，否则默认SimpleClientHttpRequestFactory 创建请求
		    // SimpleClientHttpRequestFactory -> SimpleBufferingClientHttpRequest
		    // InterceptingClientHttpRequestFactory -> InterceptingClientHttpRequest
			ClientHttpRequest request = createRequest(url, method);
			if (requestCallback != null) {
			    // 将请求对象通过MessageConverter转换成字节数组
				requestCallback.doWithRequest(request);
			}
			response = request.execute();
			handleResponse(url, method, response);
			// 处理响应结果
			return (responseExtractor != null ? responseExtractor.extractData(response) : null);
		}
		catch (IOException ex) {
			String resource = url.toString();
			String query = url.getRawQuery();
			resource = (query != null ? resource.substring(0, resource.indexOf('?')) : resource);
			throw new ResourceAccessException("I/O error on " + method.name() +
					" request for \"" + resource + "\": " + ex.getMessage(), ex);
		}
		finally {
			if (response != null) {
				response.close();
			}
		}
	}
```


``` 
@Override
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
	
	
	
	
	
	// return super.getRequestFactory(); 代码如下
	public ClientHttpRequestFactory getRequestFactory() {
    		return this.requestFactory; // 默认值是SimpleClientHttpRequestFactory
    	}
```


#### SimpleClientHttpRequestFactory 设置超时时间

``` 
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
### 基于SimpleBufferingClientHttpRequest，特点：无拦截器
### RestTemplate.doExecute() -> AbstractClientHttpRequest.execute() -> AbstractBufferingClientHttpRequest.executeInternal()
### -> SimpleBufferingClientHttpRequest.executeInternal()。
``` 
@Override
	protected ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
		addHeaders(this.connection, headers);
		// JDK <1.8 doesn't support getOutputStream with HTTP DELETE
		if (getMethod() == HttpMethod.DELETE && bufferedOutput.length == 0) {
			this.connection.setDoOutput(false);
		}
		if (this.connection.getDoOutput() && this.outputStreaming) {
			this.connection.setFixedLengthStreamingMode(bufferedOutput.length);
		}
		// 建立网络连接
		this.connection.connect();
		if (this.connection.getDoOutput()) {
			FileCopyUtils.copy(bufferedOutput, this.connection.getOutputStream());
		}
		else {
			// Immediately trigger the request in a no-output scenario as well
			this.connection.getResponseCode();
		}
		return new SimpleClientHttpResponse(this.connection);
	}
```

### responseExtractor.extractData(response) 将响应转换成对象



## 有拦截器的执行流程 InterceptingClientHttpRequestFactory -> InterceptingClientHttpRequest, 最终还是委托给SimpleBufferingClientHttpRequest。

### RestTemplate.doExecute() -> AbstractClientHttpRequest.execute() -> AbstractBufferingClientHttpRequest.executeInternal()
### -> InterceptingClientHttpRequest.executeInternal() 隐式递归执行拦截器 -> SimpleBufferingClientHttpRequest.executeInternal()。


```  

// InterceptingClientHttpRequest.executeInternal()

@Override
	protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
	    // 封装了拦截器
		InterceptingRequestExecution requestExecution = new InterceptingRequestExecution();
		return requestExecution.execute(this, bufferedOutput);
	}

```


### InterceptingRequestExecution 隐式递归调用拦截器
``` 
@Override
		public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
			// 先调用拦截器
			if (this.iterator.hasNext()) {
				ClientHttpRequestInterceptor nextInterceptor = this.iterator.next();
				// 隐式递归调用拦截器, this=InterceptingRequestExecution
				return nextInterceptor.intercept(request, body, this);
			}
			else {// 拦截器全部执行完
				HttpMethod method = request.getMethod();
				Assert.state(method != null, "No standard HTTP method");
				// 默认将网络请求委托给SimpleBufferingClientHttpRequest
				ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), method);
				request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));
				if (body.length > 0) {
					if (delegate instanceof StreamingHttpOutputMessage) {
						StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) delegate;
						streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(body, outputStream));
					}
					else {
						StreamUtils.copy(body, delegate.getBody());
					}
				}
				
				// 默认将网络请求委托给SimpleBufferingClientHttpRequest
				return delegate.execute();
			}
		}
	}
```


### HttpEntityRequestCallback.doWithRequest()将请求对象通过HttpMessageConverter转换成字节数组
``` 
public void doWithRequest(ClientHttpRequest httpRequest) throws IOException {
			super.doWithRequest(httpRequest);
			Object requestBody = this.requestEntity.getBody();
			if (requestBody == null) {
				HttpHeaders httpHeaders = httpRequest.getHeaders();
				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
				if (!requestHeaders.isEmpty()) {
					requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
				}
				if (httpHeaders.getContentLength() < 0) {
					httpHeaders.setContentLength(0L);
				}
			}
			else {
				Class<?> requestBodyClass = requestBody.getClass();
				Type requestBodyType = (this.requestEntity instanceof RequestEntity ?
						((RequestEntity<?>)this.requestEntity).getType() : requestBodyClass);
				HttpHeaders httpHeaders = httpRequest.getHeaders();
				HttpHeaders requestHeaders = this.requestEntity.getHeaders();
				MediaType requestContentType = requestHeaders.getContentType();
				for (HttpMessageConverter<?> messageConverter : getMessageConverters()) {
					if (messageConverter instanceof GenericHttpMessageConverter) {
						GenericHttpMessageConverter<Object> genericConverter =
								(GenericHttpMessageConverter<Object>) messageConverter;
						if (genericConverter.canWrite(requestBodyType, requestBodyClass, requestContentType)) {
							if (!requestHeaders.isEmpty()) {
								requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
							}
							logBody(requestBody, requestContentType, genericConverter);
							genericConverter.write(requestBody, requestBodyType, requestContentType, httpRequest);
							return;
						}
					}
					else if (messageConverter.canWrite(requestBodyClass, requestContentType)) {
						if (!requestHeaders.isEmpty()) {
							requestHeaders.forEach((key, values) -> httpHeaders.put(key, new LinkedList<>(values)));
						}
						logBody(requestBody, requestContentType, messageConverter);
						((HttpMessageConverter<Object>) messageConverter).write(
								requestBody, requestContentType, httpRequest);
						return;
					}
				}
				String message = "No HttpMessageConverter for " + requestBodyClass.getName();
				if (requestContentType != null) {
					message += " and content type \"" + requestContentType + "\"";
				}
				throw new RestClientException(message);
			}
		}
```




















































































































































































































































































































































































































































































































































































































































































































































































































