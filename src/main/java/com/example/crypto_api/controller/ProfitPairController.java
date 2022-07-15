package com.example.crypto_api.controller;

import com.example.crypto_api.models.ProfitPair;
import com.example.crypto_api.service.ProfitPairService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping(value = "/api/v1/")
@RestController
public class ProfitPairController {
    @Autowired
    private ProfitPairService profitPairService;


    @GetMapping(value = "fetchAll")
    public ResponseEntity<List<ProfitPair>> fetchAll(@RequestParam(required = false,name = "pageNum",defaultValue = "1") Integer pageNum) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setAccessControlAllowOrigin("*");
        return ResponseEntity.ok().headers(httpHeaders).body(profitPairService.fetchData(pageNum));
    }
}
