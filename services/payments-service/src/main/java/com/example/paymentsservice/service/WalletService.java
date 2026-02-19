package com.example.paymentsservice.service;

import com.example.paymentsservice.entity.Wallet;
import com.example.paymentsservice.entity.WalletTransaction;
import com.example.paymentsservice.repository.WalletRepository;
import com.example.paymentsservice.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Wallet balance and transactions. Creates wallet on first deposit/credit if missing.
 * All mutation methods are transactional and perform DB writes.
 */
@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private static final String TYPE_DEPOSIT = "DEPOSIT";
    private static final String TYPE_WITHDRAWAL = "WITHDRAWAL";
    private static final String TYPE_REFUND = "REFUND";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(WalletRepository walletRepository,
                        WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    protected Wallet createWallet(String ownerType, String ownerId) {
        Wallet w = new Wallet();
        w.setOwnerType(ownerType);
        w.setOwnerId(ownerId);
        w.setBalanceCents(0);
        w.setCurrency("USD");
        return walletRepository.save(w);
    }

    @Transactional(readOnly = true)
    public long getBalanceCents(String ownerType, String ownerId) {
        Optional<Wallet> w = walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
        return w.map(Wallet::getBalanceCents).orElse(0L);
    }

    /**
     * Deposits amount into wallet. Creates wallet if missing. Transactional.
     *
     * @param ownerType  e.g. PARTNER or BUYER
     * @param ownerId    owner identifier
     * @param amountCents amount to add (positive)
     * @param currency   optional; updates wallet currency if non-blank
     * @return new balance in cents
     */
    @Transactional
    public long deposit(String ownerType, String ownerId, long amountCents, String currency) {
        Wallet wallet = walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
            .orElseGet(() -> createWallet(ownerType, ownerId));
        if (currency != null && !currency.isBlank()) {
            wallet.setCurrency(currency);
        }
        wallet.setBalanceCents(wallet.getBalanceCents() + amountCents);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setAmountCents(amountCents);
        tx.setType(TYPE_DEPOSIT);
        walletTransactionRepository.save(tx);

        logger.info("Deposit: ownerType={}, ownerId={}, amountCents={}, newBalanceCents={}",
            ownerType, ownerId, amountCents, wallet.getBalanceCents());
        return wallet.getBalanceCents();
    }

    /**
     * Debits amount from wallet (for payment). Caller should run in same transaction as payment create/update.
     *
     * @param ownerType    e.g. PARTNER or BUYER
     * @param ownerId      owner identifier
     * @param amountCents  amount to deduct (positive)
     * @param referenceType e.g. OFFER_LISTING, ORDER_PURCHASE
     * @param referenceId  related entity id
     * @return true if debited, false if insufficient balance
     */
    @Transactional
    public boolean debit(String ownerType, String ownerId, long amountCents, String referenceType, UUID referenceId) {
        Wallet wallet = walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId).orElse(null);
        if (wallet == null || wallet.getBalanceCents() < amountCents) {
            logger.warn("Insufficient balance: ownerType={}, ownerId={}, required={}, actual={}",
                ownerType, ownerId, amountCents, wallet != null ? wallet.getBalanceCents() : 0);
            return false;
        }
        wallet.setBalanceCents(wallet.getBalanceCents() - amountCents);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setAmountCents(-amountCents);
        tx.setType(TYPE_WITHDRAWAL);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        walletTransactionRepository.save(tx);

        logger.info("Debit: ownerType={}, ownerId={}, amountCents={}, referenceType={}, referenceId={}, newBalanceCents={}",
            ownerType, ownerId, amountCents, referenceType, referenceId, wallet.getBalanceCents());
        return true;
    }

    /**
     * Credits amount to wallet (e.g. refund). Creates wallet if missing. Records REFUND transaction. Transactional.
     *
     * @param ownerType     e.g. PARTNER or BUYER
     * @param ownerId       owner identifier
     * @param amountCents   amount to add (positive)
     * @param referenceType e.g. ORDER_REFUND
     * @param referenceId   related entity id (e.g. payment id)
     * @return new balance in cents
     */
    @Transactional
    public long credit(String ownerType, String ownerId, long amountCents, String referenceType, UUID referenceId) {
        Wallet wallet = walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
            .orElseGet(() -> createWallet(ownerType, ownerId));
        wallet.setBalanceCents(wallet.getBalanceCents() + amountCents);
        walletRepository.save(wallet);

        WalletTransaction tx = new WalletTransaction();
        tx.setWalletId(wallet.getId());
        tx.setAmountCents(amountCents);
        tx.setType(TYPE_REFUND);
        tx.setReferenceType(referenceType);
        tx.setReferenceId(referenceId);
        walletTransactionRepository.save(tx);

        logger.info("Refund credit: ownerType={}, ownerId={}, amountCents={}, referenceType={}, referenceId={}, newBalanceCents={}",
            ownerType, ownerId, amountCents, referenceType, referenceId, wallet.getBalanceCents());
        return wallet.getBalanceCents();
    }
}
