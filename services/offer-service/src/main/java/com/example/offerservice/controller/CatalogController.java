package com.example.offerservice.controller;

import com.example.offerservice.dto.CatalogOfferResponse;
import com.example.offerservice.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/catalog")
public class CatalogController {
    
    private final OfferService offerService;
    
    public CatalogController(OfferService offerService) {
        this.offerService = offerService;
    }
    
    @GetMapping("/offers")
    @Operation(
        summary = "Get published offers catalog",
        description = "Returns a list of published offers (public endpoint, no authentication required)"
    )
    public ResponseEntity<List<CatalogOfferResponse>> getCatalogOffers() {
        List<CatalogOfferResponse> offers = offerService.getPublishedOffers();
        return ResponseEntity.ok(offers);
    }
}
