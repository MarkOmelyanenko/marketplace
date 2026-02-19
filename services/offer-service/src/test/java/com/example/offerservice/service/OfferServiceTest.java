package com.example.offerservice.service;

import com.example.offerservice.dto.CatalogOfferResponse;
import com.example.offerservice.dto.CreateOfferRequest;
import com.example.offerservice.dto.OfferResponse;
import com.example.offerservice.dto.UpdateOfferRequest;
import com.example.offerservice.entity.Offer;
import com.example.offerservice.metrics.OfferMetrics;
import com.example.offerservice.repository.IdempotencyKeyRepository;
import com.example.offerservice.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    private static final String PARTNER_ID = "partner-1";
    private static final UUID OFFER_ID = UUID.randomUUID();

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private com.example.offerservice.client.PaymentServiceClient paymentServiceClient;
    @Mock
    private OutboxService outboxService;
    @Mock
    private OfferMetrics offerMetrics;

    private OfferService offerService;

    @BeforeEach
    void setUp() {
        offerService = new OfferService(
                offerRepository,
                idempotencyKeyRepository,
                null,
                new com.fasterxml.jackson.databind.ObjectMapper(),
                paymentServiceClient,
                outboxService,
                offerMetrics
        );
    }

    @Test
    void createOffer_savesWithCorrectFields() {
        CreateOfferRequest request = new CreateOfferRequest();
        request.setTitle("Test title");
        request.setDescription("Test description");
        request.setPriceCents(999);
        request.setCurrency("USD");

        Offer saved = new Offer();
        saved.setId(OFFER_ID);
        saved.setPartnerId(PARTNER_ID);
        saved.setStatus("ENRICHING");
        saved.setTitle(request.getTitle());
        saved.setDescription(request.getDescription());
        saved.setPriceCents(request.getPriceCents());
        saved.setCurrency(request.getCurrency());
        saved.setCreatedAt(OffsetDateTime.now());
        saved.setUpdatedAt(OffsetDateTime.now());

        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            o.setId(OFFER_ID);
            o.setCreatedAt(OffsetDateTime.now());
            o.setUpdatedAt(OffsetDateTime.now());
            return o;
        });

        OfferResponse response = offerService.createOffer(request, PARTNER_ID, null);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(response.getStatus()).isEqualTo("ENRICHING");
        assertThat(response.getTitle()).isEqualTo("Test title");
        assertThat(response.getDescription()).isEqualTo("Test description");
        assertThat(response.getPriceCents()).isEqualTo(999);
        assertThat(response.getCurrency()).isEqualTo("USD");

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
        verify(offerRepository).save(captor.capture());
        Offer captured = captor.getValue();
        assertThat(captured.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(captured.getStatus()).isEqualTo("ENRICHING");
        assertThat(captured.getTitle()).isEqualTo("Test title");
    }

    @Test
    void createOffer_usesDefaultPriceAndCurrencyWhenNull() {
        CreateOfferRequest request = new CreateOfferRequest();
        request.setTitle("Title");
        request.setDescription("Desc");
        request.setPriceCents(null);
        request.setCurrency(null);

        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            o.setId(OFFER_ID);
            o.setCreatedAt(OffsetDateTime.now());
            o.setUpdatedAt(OffsetDateTime.now());
            return o;
        });

        OfferResponse response = offerService.createOffer(request, PARTNER_ID, null);

        assertThat(response.getPriceCents()).isEqualTo(499);
        assertThat(response.getCurrency()).isEqualTo("USD");
    }

    @Test
    void getOfferById_returnsOfferWhenFound() {
        Offer offer = new Offer();
        offer.setId(OFFER_ID);
        offer.setPartnerId(PARTNER_ID);
        offer.setStatus("READY");
        offer.setTitle("Title");
        offer.setDescription("Desc");
        offer.setCreatedAt(OffsetDateTime.now());
        offer.setUpdatedAt(OffsetDateTime.now());

        when(offerRepository.findByIdAndPartnerId(OFFER_ID, PARTNER_ID)).thenReturn(Optional.of(offer));

        OfferResponse response = offerService.getOfferById(OFFER_ID, PARTNER_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(OFFER_ID);
        assertThat(response.getTitle()).isEqualTo("Title");
    }

    @Test
    void getOfferById_throwsWhenNotFound() {
        when(offerRepository.findByIdAndPartnerId(OFFER_ID, PARTNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerService.getOfferById(OFFER_ID, PARTNER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Offer not found");
    }

    @Test
    void getOffersByPartner_returnsListFromRepository() {
        Offer offer = new Offer();
        offer.setId(OFFER_ID);
        offer.setPartnerId(PARTNER_ID);
        offer.setStatus("READY");
        offer.setTitle("Title");
        offer.setDescription("Desc");
        offer.setCreatedAt(OffsetDateTime.now());
        offer.setUpdatedAt(OffsetDateTime.now());

        when(offerRepository.findByPartnerIdOrderByCreatedAtDesc(PARTNER_ID)).thenReturn(List.of(offer));

        List<OfferResponse> list = offerService.getOffersByPartner(PARTNER_ID);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Title");
    }

    @Test
    void updateOffer_updatesTitleAndDescription() {
        Offer offer = new Offer();
        offer.setId(OFFER_ID);
        offer.setPartnerId(PARTNER_ID);
        offer.setStatus("READY");
        offer.setTitle("Old");
        offer.setDescription("Old desc");
        offer.setCreatedAt(OffsetDateTime.now());
        offer.setUpdatedAt(OffsetDateTime.now());

        when(offerRepository.findByIdAndPartnerId(OFFER_ID, PARTNER_ID)).thenReturn(Optional.of(offer));
        when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateOfferRequest update = new UpdateOfferRequest();
        update.setTitle("New title");
        update.setDescription("New desc");

        OfferResponse response = offerService.updateOffer(OFFER_ID, update, PARTNER_ID);

        assertThat(response.getTitle()).isEqualTo("New title");
        assertThat(response.getDescription()).isEqualTo("New desc");
        verify(offerRepository).save(offer);
    }
}
