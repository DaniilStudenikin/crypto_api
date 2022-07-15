package com.example.crypto_api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class ProfitPair {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromExchange;

    private String fromExchangePair;

    private Double fromExchangePairPrice;

    private String toExchange;

    private String toExchangePair;

    private Double toExchangePairPrice;

    private String profit;
}
