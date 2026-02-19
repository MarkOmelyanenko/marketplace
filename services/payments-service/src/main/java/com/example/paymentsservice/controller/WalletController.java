package com.example.paymentsservice.controller;

import com.example.paymentsservice.dto.DepositRequest;
import com.example.paymentsservice.dto.WalletBalanceResponse;
import com.example.paymentsservice.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    @Operation(
        summary = "Get wallet balance",
        description = "Returns the current balance for the authenticated partner or buyer.",
        security = @SecurityRequirement(name = "Partner-Id or Buyer-Id")
    )
    public ResponseEntity<WalletBalanceResponse> getBalance(HttpServletRequest request) {
        String ownerType = (String) request.getAttribute("ownerType");
        String ownerId = (String) request.getAttribute("ownerId");
        long balanceCents = walletService.getBalanceCents(ownerType, ownerId);
        String currency = "USD";
        return ResponseEntity.ok(new WalletBalanceResponse(balanceCents, currency));
    }

    @PostMapping("/deposit")
    @Operation(
        summary = "Deposit money",
        description = "Simulated deposit to increase wallet balance. Partner or buyer only.",
        security = @SecurityRequirement(name = "Partner-Id or Buyer-Id")
    )
    public ResponseEntity<WalletBalanceResponse> deposit(
            @Valid @RequestBody DepositRequest body,
            HttpServletRequest request) {
        String ownerType = (String) request.getAttribute("ownerType");
        String ownerId = (String) request.getAttribute("ownerId");
        String currency = body.getCurrency() != null && !body.getCurrency().isBlank() ? body.getCurrency() : "USD";
        long newBalance = walletService.deposit(ownerType, ownerId, body.getAmountCents(), currency);
        return ResponseEntity.ok(new WalletBalanceResponse(newBalance, currency));
    }
}
