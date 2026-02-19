package com.example.offerservice.controller;

import com.example.offerservice.dto.ApplyAiRequest;
import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for offers: create, list, get, update, apply AI, publish. Expects Partner-Id
 * from interceptor (set on request attributes). Catalog endpoints are unauthenticated.
 */
@RestController
@RequestMapping("/v1/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    @Operation(
        summary = "Create a new offer",
        description = "Creates a new offer in ENRICHING status and publishes offer.created event. Supports idempotency via Idempotency-Key header. Valid statuses: DRAFT, ENRICHING, READY.",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    @ApiResponse(
        responseCode = "201",
        description = "Offer created successfully",
        content = @Content(schema = @Schema(implementation = OfferResponse.class))
    )
    @ApiResponse(
        responseCode = "200",
        description = "Idempotent request - existing offer returned",
        content = @Content(schema = @Schema(implementation = OfferResponse.class))
    )
    public ResponseEntity<OfferResponse> createOffer(
            @Valid @RequestBody CreateOfferRequest request,
            @Parameter(hidden = true) @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existingKey = offerService.findIdempotencyKey(idempotencyKey, partnerId);
            if (existingKey.isPresent()) {
                OfferResponse existingOffer = offerService.getOfferById(existingKey.get().getOfferId(), partnerId);
                return ResponseEntity.status(HttpStatus.OK).body(existingOffer);
            }
        }
        
        OfferResponse response = offerService.createOffer(request, partnerId, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping
    @Operation(
        summary = "List offers",
        description = "Retrieves all offers for the authenticated partner",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    public ResponseEntity<List<OfferResponse>> getOffers(
            @RequestParam(value = "mine", required = false, defaultValue = "true") boolean mine,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        List<OfferResponse> offers = offerService.getOffersByPartner(partnerId);
        return ResponseEntity.ok(offers);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get offer by ID",
        description = "Retrieves a specific offer by ID (scoped to partner)",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    public ResponseEntity<OfferResponse> getOffer(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        OfferResponse offer = offerService.getOfferById(id, partnerId);
        return ResponseEntity.ok(offer);
    }
    
    @PatchMapping("/{id}")
    @Operation(
        summary = "Update offer",
        description = "Partially updates an offer (title and/or description)",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    public ResponseEntity<OfferResponse> updateOffer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOfferRequest request,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        OfferResponse offer = offerService.updateOffer(id, request, partnerId);
        return ResponseEntity.ok(offer);
    }
    
    @PostMapping("/{id}/apply-ai")
    @Operation(
        summary = "Apply AI suggestions",
        description = "Applies AI-generated title and/or description to the offer. Status must be READY with AI fields populated.",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    @ApiResponse(
        responseCode = "200",
        description = "AI suggestions applied successfully",
        content = @Content(schema = @Schema(implementation = OfferResponse.class))
    )
    public ResponseEntity<OfferResponse> applyAi(
            @PathVariable UUID id,
            @Valid @RequestBody ApplyAiRequest request,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        OfferResponse offer = offerService.applyAi(id, request, partnerId);
        return ResponseEntity.ok(offer);
    }
    
    @PostMapping("/{id}/publish")
    @Operation(
        summary = "Publish offer",
        description = "Publishes an offer by creating a listing fee payment. Offer status changes to PUBLISH_PENDING, then PUBLISHED when payment is captured.",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    @ApiResponse(
        responseCode = "200",
        description = "Publish initiated successfully",
        content = @Content(schema = @Schema(implementation = OfferResponse.class))
    )
    public ResponseEntity<OfferResponse> publishOffer(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        offerService.publishOffer(id, partnerId);
        OfferResponse offer = offerService.getOfferById(id, partnerId);
        return ResponseEntity.ok(offer);
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete offer",
        description = "Deletes an offer. Allowed for any status including PUBLISHED. Only the owning partner can delete.",
        security = @SecurityRequirement(name = "Partner-Id")
    )
    @ApiResponse(responseCode = "204", description = "Offer deleted successfully")
    public ResponseEntity<Void> deleteOffer(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        
        String partnerId = (String) httpRequest.getAttribute("partnerId");
        offerService.deleteOffer(id, partnerId);
        return ResponseEntity.noContent().build();
    }
}
