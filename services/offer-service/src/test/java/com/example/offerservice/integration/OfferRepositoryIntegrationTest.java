package com.example.offerservice.integration;

import com.example.offerservice.entity.Offer;
import com.example.offerservice.repository.OfferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OfferRepositoryIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("test_offer_db")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private OfferRepository offerRepository;
    
    @Test
    void shouldSaveAndRetrieveOffer() {
        // given
        Offer offer = new Offer();
        offer.setId(UUID.randomUUID());
        offer.setPartnerId("partner-123");
        offer.setStatus("ENRICHING");
        offer.setTitle("Test Offer");
        offer.setDescription("Test Description");
        offer.setCreatedAt(OffsetDateTime.now());
        offer.setUpdatedAt(OffsetDateTime.now());
        
        // when
        Offer saved = offerRepository.save(offer);
        Offer found = offerRepository.findById(saved.getId()).orElse(null);
        
        // then
        assertThat(found).isNotNull();
        assertThat(found.getPartnerId()).isEqualTo("partner-123");
        assertThat(found.getTitle()).isEqualTo("Test Offer");
        assertThat(found.getStatus()).isEqualTo("ENRICHING");
    }
    
    @Test
    void shouldFindOffersByPartnerId() {
        // given
        Offer offer1 = new Offer();
        offer1.setId(UUID.randomUUID());
        offer1.setPartnerId("partner-123");
        offer1.setStatus("ENRICHING");
        offer1.setTitle("Offer 1");
        offer1.setDescription("Description 1");
        offer1.setCreatedAt(OffsetDateTime.now());
        offer1.setUpdatedAt(OffsetDateTime.now());
        
        Offer offer2 = new Offer();
        offer2.setId(UUID.randomUUID());
        offer2.setPartnerId("partner-123");
        offer2.setStatus("READY");
        offer2.setTitle("Offer 2");
        offer2.setDescription("Description 2");
        offer2.setCreatedAt(OffsetDateTime.now());
        offer2.setUpdatedAt(OffsetDateTime.now());
        
        offerRepository.save(offer1);
        offerRepository.save(offer2);
        
        // when
        var offers = offerRepository.findByPartnerIdOrderByCreatedAtDesc("partner-123");
        
        // then
        assertThat(offers).hasSize(2);
        assertThat(offers.get(0).getTitle()).isEqualTo("Offer 2"); // most recent first
    }
}
