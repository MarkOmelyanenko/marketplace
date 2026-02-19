package com.example.paymentsservice.service;

import com.example.paymentsservice.entity.Wallet;
import com.example.paymentsservice.entity.WalletTransaction;
import com.example.paymentsservice.repository.WalletRepository;
import com.example.paymentsservice.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final String OWNER_TYPE = "BUYER";
    private static final String OWNER_ID = "buyer-1";

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;

    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletService = new WalletService(walletRepository, walletTransactionRepository);
    }

    @Test
    void getBalanceCents_returnsZeroWhenNoWallet() {
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.empty());

        long balance = walletService.getBalanceCents(OWNER_TYPE, OWNER_ID);

        assertThat(balance).isZero();
    }

    @Test
    void getBalanceCents_returnsWalletBalanceWhenPresent() {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setBalanceCents(5000L);
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.of(wallet));

        long balance = walletService.getBalanceCents(OWNER_TYPE, OWNER_ID);

        assertThat(balance).isEqualTo(5000L);
    }

    @Test
    void deposit_createsWalletWhenMissingAndAddsBalance() {
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> {
            Wallet w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        long newBalance = walletService.deposit(OWNER_TYPE, OWNER_ID, 1000L, "USD");

        assertThat(newBalance).isEqualTo(1000L);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, atLeastOnce()).save(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getBalanceCents()).isEqualTo(1000L);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void deposit_addsToExistingBalance() {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setBalanceCents(500L);
        wallet.setCurrency("USD");
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        long newBalance = walletService.deposit(OWNER_TYPE, OWNER_ID, 500L, "USD");

        assertThat(newBalance).isEqualTo(1000L);
        assertThat(wallet.getBalanceCents()).isEqualTo(1000L);
    }

    @Test
    void debit_returnsFalseWhenInsufficientBalance() {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setBalanceCents(100L);
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.of(wallet));

        boolean result = walletService.debit(OWNER_TYPE, OWNER_ID, 500L, "ORDER_PURCHASE", UUID.randomUUID());

        assertThat(result).isFalse();
        verify(walletRepository, never()).save(any());
    }

    @Test
    void debit_returnsFalseWhenNoWallet() {
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.empty());

        boolean result = walletService.debit(OWNER_TYPE, OWNER_ID, 100L, "ORDER_PURCHASE", UUID.randomUUID());

        assertThat(result).isFalse();
    }

    @Test
    void debit_deductsBalanceAndReturnsTrueWhenSufficient() {
        UUID refId = UUID.randomUUID();
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setBalanceCents(1000L);
        when(walletRepository.findByOwnerTypeAndOwnerId(OWNER_TYPE, OWNER_ID)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(walletTransactionRepository.save(any(WalletTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        boolean result = walletService.debit(OWNER_TYPE, OWNER_ID, 300L, "ORDER_PURCHASE", refId);

        assertThat(result).isTrue();
        assertThat(wallet.getBalanceCents()).isEqualTo(700L);
        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getAmountCents()).isEqualTo(-300L);
        assertThat(txCaptor.getValue().getType()).isEqualTo("WITHDRAWAL");
    }
}
