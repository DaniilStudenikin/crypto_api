package com.example.crypto_api.service.impl;

import com.example.crypto_api.models.Currency;
import com.example.crypto_api.repository.CurrencyRepository;
import com.example.crypto_api.service.CurrencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class CurrencyServiceImpl implements CurrencyService {
    private final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Value("${cryptorank.api-key}")
    private String apiKey;

    /*
     * Код ошибки 429 Too Many Requests говорит о том,
     * что пользователь отправлял чересчур много запросов за единицу времени.
     * Возвращаемый сервером ответ содержит пояснение, а также может включать заголовок Retry-After.
     * Этот заголовок указывает на время, которое необходимо подождать, прежде чем повторять запрос.
     * */
    @Async
    @Scheduled(fixedDelay = 1000 * 60 * 60)
    @Override
    public void scanCurrency() {
        Set<Currency> currencies = new HashSet<>(currencyRepository.findAll());
        String responseEntity = restTemplate.getForObject("https://api.cryptorank.io/v1/currencies?limit=5586&api_key=" + apiKey, String.class);
        try {
            JsonNode root = mapper.readTree(responseEntity);
            mapper.writeValue(Paths.get("currency.json").toFile(), root);
            System.out.println("Started grabbing data");
            StreamSupport.stream(root.get("data").spliterator(), true).skip(100).forEach(currencyFromResponse -> {
                Currency currency = Currency.builder()
                        .id(currencyFromResponse.get("id").asLong())
                        .slug(currencyFromResponse.get("slug").asText())
                        .name(currencyFromResponse.get("name").asText())
                        .symbol(currencyFromResponse.get("symbol").asText())
                        .volume24h(currencyFromResponse.get("values").get("USD").get("volume24h").asLong())
                        .price(currencyFromResponse.get("values").get("USD").get("price").asLong())
                        .build();
                try {
                    currency.setRank(currencyFromResponse.get("rank").asLong());
                } catch (NullPointerException ignore) {
                    //Значит нет поля rank у коина
                }
                currencies.add(currency);
            });
            currencyRepository.saveAll(currencies);
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
    }

}
