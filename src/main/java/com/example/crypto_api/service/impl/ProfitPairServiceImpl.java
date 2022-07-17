package com.example.crypto_api.service.impl;

import com.example.crypto_api.models.Currency;
import com.example.crypto_api.models.Exchange;
import com.example.crypto_api.models.ProfitPair;
import com.example.crypto_api.repository.CurrencyRepository;
import com.example.crypto_api.repository.ExchangeRepository;
import com.example.crypto_api.repository.ProfitPairRepository;
import com.example.crypto_api.service.ProfitPairService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class ProfitPairServiceImpl implements ProfitPairService {
    private final Logger logger = LoggerFactory.getLogger(ProfitPairServiceImpl.class);
    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ProfitPairRepository profitPairRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ExchangeRepository exchangeRepository;

    @Scheduled(fixedRate = 1000 * 5, initialDelay = 1000 * 3)
    @Async
    @Override
    public void scanPairs() {
        List<Currency> currencyList = currencyRepository.findAllByVolume24hGreaterThan(80000L);
        long start = System.currentTimeMillis();
        List<Exchange> exchanges = exchangeRepository.findAll();
        try {
            currencyList.stream().sorted().skip(100).forEach(currency -> {
                String responseEntity = restTemplate.getForObject("https://api.cryptorank.io/v0/coins/" + currency.getSlug() + "/tickers", String.class);
                try {
                    JsonNode root = objectMapper.readTree(responseEntity);

                    StreamSupport.stream(root.get("data").spliterator(), false).forEach(firstPair -> {
                        double firstPrice;
                        if (!exchanges.contains(Exchange.builder()
                                .exchangeName(firstPair.get("exchangeName").asText()).build())) {
                            return;
                        }
                        try {
                            firstPrice = firstPair.get("usdLast").asDouble();
                        } catch (NullPointerException e) {
                            return;
                        }
                        String toFirst = firstPair.get("from").asText();
                        StreamSupport.stream(root.get("data").spliterator(), false).forEach(secondPair -> {
                            double secondPrice;
                            if (!exchanges.contains(Exchange.builder()
                                    .exchangeName(secondPair.get("exchangeName").asText()).build())) {
                                return;
                            }
                            if (!toFirst.equals(secondPair.get("from").asText())) {
                                return;
                            }
                            try {
                                secondPrice = secondPair.get("usdLast").asDouble();
                            } catch (NullPointerException e) {
                                return;
                            }
                            double stepFirst = secondPrice - firstPrice;
                            double secondStep;
                            try {
                                secondStep = stepFirst * 100 / secondPrice;
                            } catch (ArithmeticException e) {
                                return;
                            }

                            if (secondStep > 2) {
                                Optional<ProfitPair> profitPair = profitPairRepository.findByFromExchangeAndFromExchangePairAndToExchangeAndToExchangePair(firstPair.get("exchangeName").asText(), firstPair.get("symbol").asText(), secondPair.get("exchangeName").asText(), secondPair.get("symbol").asText());
                                if (profitPair.isPresent() && firstPair.get("usdLast").asDouble() != profitPair.get().getFromExchangePairPrice() && secondPair.get("usdLast").asDouble() != profitPair.get().getToExchangePairPrice()) {
                                    profitPair.get().setFromExchangePairPrice(firstPair.get("usdLast").asDouble());
                                    profitPair.get().setToExchangePairPrice(secondPair.get("usdLast").asDouble());
                                    profitPair.get().setProfit("+" + secondStep + "%");
                                    profitPair.get().setLastUpdatedTime(LocalDateTime.now());
                                    profitPairRepository.save(profitPair.get());
                                } else if (profitPair.isEmpty()) {
                                    profitPairRepository.save(ProfitPair.builder()
                                            .fromExchange(firstPair.get("exchangeName").asText())
                                            .fromExchangePair(firstPair.get("symbol").asText())
                                            .fromExchangePairPrice(firstPair.get("usdLast").asDouble())
                                            .toExchange(secondPair.get("exchangeName").asText())
                                            .toExchangePair(secondPair.get("symbol").asText())
                                            .toExchangePairPrice(secondPair.get("usdLast").asDouble())
                                            .profit("+" + secondStep + "%")
                                            .createdTime(LocalDateTime.now())
                                            .lastUpdatedTime(LocalDateTime.now())
                                            .build());
                                }

                            }
                        });
                    });
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

            });
        } catch (HttpClientErrorException e) {
            logger.info(e.toString());
            System.out.println(429);
        }

        System.out.println("Done!");
        System.out.println("Time: " + (System.currentTimeMillis() - start));
    }

    @Override
    public List<ProfitPair> fetchData(Integer pageId) {
        return profitPairRepository.findAll(PageRequest.of(pageId, 500)).getContent();
    }

    @Async
    @Scheduled(fixedRate = 1000 * 60 * 5)
    @Override
    public void removeUnusedPairs() {
        List<ProfitPair> profitPairs = profitPairRepository.findAll();
        for (ProfitPair profitPair : profitPairs) {
            LocalDateTime now = LocalDateTime.now();
            long minutes = profitPair.getLastUpdatedTime().until(now, ChronoUnit.MINUTES);
            if (minutes <= -5) {
                profitPairRepository.delete(profitPair);
            }
        }
    }
}
