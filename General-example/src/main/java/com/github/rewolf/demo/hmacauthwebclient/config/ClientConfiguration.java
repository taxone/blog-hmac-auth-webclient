package com.github.rewolf.demo.hmacauthwebclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rewolf.demo.hmacauthwebclient.client.Environment;
import com.github.rewolf.demo.hmacauthwebclient.client.MessageCapturingHttpConnector;
import com.github.rewolf.demo.hmacauthwebclient.client.RequestSigningJsonEncoder;
import com.github.rewolf.demo.hmacauthwebclient.client.SignatureProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.function.client.ExchangeFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfiguration {

    @Bean
    public Environment clientEnvironment() {
        return Environment.builder()
                          .protocol("http")
                          .host("requestbin.net")
                          .basePath("/r")
                          .build();
    }

    @Bean
    public WebClient webclient(final Environment environment,
                               @Value("${client.id}") final String clientId,
                               @Value("${client.secret}") final String secret) throws Exception {

        final SignatureProvider signatureProvider = new SignatureProvider(clientId, secret);
        final MessageCapturingHttpConnector httpConnector = new MessageCapturingHttpConnector(signatureProvider);
        final RequestSigningJsonEncoder requestSigningJsonEncoder = new RequestSigningJsonEncoder(httpConnector::signWithBody);

        return WebClient.builder()
                        .exchangeFunction(ExchangeFunctions.create(
                                httpConnector,
                                ExchangeStrategies
                                        .builder()
                                        .codecs(clientDefaultCodecsConfigurer -> {
                                            clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(requestSigningJsonEncoder);
                                            clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(new ObjectMapper(), MediaType.APPLICATION_JSON));
                                        })
                                        .build()
                        ))
                        .baseUrl(String.format("%s://%s/%s", environment.getProtocol(), environment.getHost(), environment.getBasePath()))
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build();
    }
}
