package com.aiplatform.ai.client;

import java.time.Duration;
import java.util.Map;
import javax.net.ssl.SSLException;
import com.aiplatform.ai.config.AgentProperties;
import com.aiplatform.common.exception.ServiceException;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiClient {

    @Resource
    private AgentProperties agentProperties;

    private WebClient buildClient() {
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> {
                    try {
                        spec.sslContext(SslContextBuilder.forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                .build());
                    } catch (SSLException e) {
                        throw new ServiceException("SSL 配置失败: " + e.getMessage());
                    }
                })
                .responseTimeout(Duration.ofSeconds(agentProperties.getReadTimeout()))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(agentProperties.getReadTimeout()))
                                .addHandlerLast(new WriteTimeoutHandler(agentProperties.getConnectTimeout())));

        return WebClient.builder()
                .baseUrl(agentProperties.getBaseUrl() + agentProperties.getApiPrefix())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public String post(String path, Map<String, Object> body) {
        long start = System.currentTimeMillis();
        try {
            String result = buildClient().post()
                    .uri(path)
                    .bodyValue(body != null ? body : Map.of())
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(errBody -> Mono.error(new ServiceException("Agent 返回错误: " + errBody))))
                    .bodyToMono(String.class)
                    .block();
            log.info("FastApiClient POST {} 耗时 {}ms", path, System.currentTimeMillis() - start);
            return result;
        } catch (WebClientRequestException e) {
            log.error("FastApiClient POST {} 请求失败, 耗时 {}ms", path, System.currentTimeMillis() - start, e);
            throw new ServiceException("Agent 服务不可达: " + agentProperties.getBaseUrl());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("FastApiClient POST {} 调用失败, 耗时 {}ms", path, System.currentTimeMillis() - start, e);
            throw new ServiceException("Agent 调用失败: " + e.getMessage());
        }
    }

    public String get(String path) {
        long start = System.currentTimeMillis();
        try {
            String result = buildClient().get()
                    .uri(path)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(errBody -> Mono.error(new ServiceException("Agent 返回错误: " + errBody))))
                    .bodyToMono(String.class)
                    .block();
            log.info("FastApiClient GET {} 耗时 {}ms", path, System.currentTimeMillis() - start);
            return result;
        } catch (WebClientRequestException e) {
            log.error("FastApiClient GET {} 请求失败, 耗时 {}ms", path, System.currentTimeMillis() - start, e);
            throw new ServiceException("Agent 服务不可达: " + agentProperties.getBaseUrl());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("FastApiClient GET {} 调用失败, 耗时 {}ms", path, System.currentTimeMillis() - start, e);
            throw new ServiceException("Agent 调用失败: " + e.getMessage());
        }
    }

    public Flux<String> streamPost(String path, Map<String, Object> body) {
        long start = System.currentTimeMillis();
        return buildClient().post()
                .uri(path)
                .bodyValue(body != null ? body : Map.of())
                .accept(org.springframework.http.MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnComplete(() -> log.info("FastApiClient SSE POST {} 完成, 耗时 {}ms", path, System.currentTimeMillis() - start))
                .doOnError(e -> log.error("FastApiClient SSE POST {} 失败, 耗时 {}ms", path, System.currentTimeMillis() - start, e));
    }

}