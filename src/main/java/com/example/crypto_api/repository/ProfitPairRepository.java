package com.example.crypto_api.repository;

import com.example.crypto_api.models.ProfitPair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfitPairRepository extends JpaRepository<ProfitPair, Long> {
    Optional<ProfitPair> findByFromExchangeAndFromExchangePairAndToExchangeAndToExchangePair(String fromExchange, String fromExchangePair, String toExchange, String toExchangePair);
}
