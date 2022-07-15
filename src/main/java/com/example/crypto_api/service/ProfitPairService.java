package com.example.crypto_api.service;

import com.example.crypto_api.models.ProfitPair;

import java.util.List;

public interface ProfitPairService {
    void scanPairs();


    List<ProfitPair> fetchData(Integer pageId);
}
