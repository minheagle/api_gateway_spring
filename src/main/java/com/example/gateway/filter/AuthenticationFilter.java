package com.example.gateway.filter;

import com.example.gateway.dto.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {
    private final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    @Value("${services.logging.uri}")
    private String loggingUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");

        requestSendToLogService(exchange);

//        if (token == null || !isValidToken(token)) {
//            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
//            return exchange.getResponse().setComplete();
//        }

        return chain.filter(exchange);
    }

    private boolean isValidToken(String token) {
//        return token.startsWith("Bearer ");
        return true;
    }

    private void requestSendToLogService(ServerWebExchange exchange){
        String apiUrl = loggingUrl + "/logs";
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getURI().toString().replace("http://127.0.0.1:8080/gateway", "");
        Log log = new Log(method, path);

        String logMessage = null;
        try {
            logMessage = objectMapper.writeValueAsString(log);
        } catch (JsonProcessingException e) {
            String message = "Write value as string error : " + e.getMessage();
            logger.error(message);
        }

        HttpClient httpClient = HttpClients.createDefault();

        HttpPost httpPost = new HttpPost(apiUrl);
        httpPost.setHeader("Content-Type", "application/json");

        try {
            httpPost.setEntity(new StringEntity(logMessage));
        } catch (UnsupportedEncodingException e) {
            String message = "Unsupported Encoding Exception " + e.getMessage();
            logger.error(message);
        }

        try {
            HttpResponse response = httpClient.execute(httpPost);
        } catch (IOException e) {
            String message = "IO Exception : " + e.getMessage();
            logger.error(message);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
