package com.example.crypto_api.repository;

import com.example.crypto_api.models.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {
    //На вход принимает объем за 24 по коину в $ e.x: 80000(Вернет все коины у которых объем за 24 больше 80к$
    List<Currency> findAllByVolume24hGreaterThan(Long val);
}
