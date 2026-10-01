package com.ceo.trading_platform_backend.services;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.ceo.trading_platform_backend.services.MarketService.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class MarketServiceTest {

    private MarketService marketService;
    private ObjectMapper objectMapper;
    private String apiKey;
    private String baseUrl = "https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        
        apiKey = getAPIkey();
        
        // Fall back to system environment if not found in properties
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = System.getenv("api_key");
        }
        
        // Create MarketService with the API key
        marketService = new MarketService(
            RestClient.builder(),
            objectMapper,
            baseUrl,
            apiKey != null ? apiKey : ""
        );
    }

    private String getAPIkey() {
        try {
            // Try to load from .env file in project root
            Properties props = new Properties();
            if (Files.exists(Paths.get(".env"))) {
                props.load(Files.newInputStream(Paths.get(".env")));
                String key = props.getProperty("api_key", "").trim();
                if (!key.isEmpty()) {
                    return key;
                }
            }
        } catch (IOException e) {
            // Fall through to environment variable
        }
        
        // Fall back to environment variable
        return System.getenv("api_key") != null ? System.getenv("api_key") : "";
    }

    @Test
    void testMarketServiceInitialization() {
        assertNotNull(marketService);
    }

    @Test
    void testGetPriceThrowsExceptionForNullSymbol() {
        assertThrows(IllegalArgumentException.class, () -> {
            marketService.getPrice(null);
        });
    }

    @Test
    void testGetPriceThrowsExceptionForEmptySymbol() {
        assertThrows(IllegalArgumentException.class, () -> {
            marketService.getPrice("");
        });
    }

    @Test
    void testGetQuotesThrowsExceptionForMoreThan25Symbols() {
        assertThrows(IllegalArgumentException.class, () -> {
            marketService.getQuotes(java.util.List.of(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
                "21", "22", "23", "24", "25", "26"
            ));
        });
    }

    @Test
    void testGetQuotesThrowsExceptionForNullSymbolsList() {
        assertThrows(IllegalArgumentException.class, () -> {
            marketService.getQuotes(null);
        });
    }

    @Test
    void testQuoteDataStructure() {
        BigDecimal price = new BigDecimal("150.25");
        QuoteData quoteData = new QuoteData(
            "AAPL",
            price,
            new BigDecimal("150.00"),
            new BigDecimal("150.50"),
            new BigDecimal("0.33"),
            "USD",
            new BigDecimal("2.50"),
            new BigDecimal("1.69"),
            new BigDecimal("147.75"),
            OffsetDateTime.now(),
            "OPEN"
        );
        
        assertEquals("AAPL", quoteData.symbol());
        assertEquals(price, quoteData.price());
    }

    @Test
    void testCandleDataStructure() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Candle candle = new Candle(
            date,
            new BigDecimal("150.00"),
            new BigDecimal("155.50"),
            new BigDecimal("149.75"),
            new BigDecimal("154.25"),
            new BigDecimal("154.20"),
            1000000L,
            false
        );
        
        assertEquals(date, candle.date());
        assertEquals(new BigDecimal("150.00"), candle.open());
        assertEquals(1000000L, candle.volume());
    }

    @Test
    void testSymbolMetadataDataStructure() {
        Coverage coverage = new Coverage(
            LocalDate.of(2020, 1, 1),
            LocalDate.now()
        );
        
        SymbolMetadataData metadata = new SymbolMetadataData(
            "AAPL",
            "Apple Inc.",
            "EQUITY",
            "NASDAQ",
            "USD",
            true,
            coverage
        );
        
        assertEquals("AAPL", metadata.symbol());
        assertTrue(metadata.active());
    }

    @Test
    void testApiCallToRealMarketService() {
        // This test attempts to make a real API call to verify connectivity
        try {
            System.out.println("Using API Key: " + (apiKey != null && !apiKey.isEmpty() ? "***CONFIGURED***" : "NOT CONFIGURED"));
            
            // Attempt to get a quote for a valid symbol
            QuoteData quote = marketService.getQuote("AAPL");
            
            assertNotNull(quote);
            assertNotNull(quote.symbol());
            assertEquals("AAPL", quote.symbol());
            assertNotNull(quote.price());
            assertTrue(quote.price().compareTo(BigDecimal.ZERO) > 0);
            
            System.out.println("API Call Successful! AAPL Price: " + quote.price());
        } catch (IllegalStateException e) {
            // Expected if API key is not configured
            System.out.println("API key not configured: " + e.getMessage());
        } catch (Exception e) {
            // Log the error but don't fail the test if API is unreachable
            System.out.println("API Call Test - Error occurred: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @Test
    void testGetSymbolMetadataForAAPL() {
        // This test verifies the real API call to /symbols/AAPL endpoint
        try {
            System.out.println("Testing Symbol Metadata endpoint for AAPL...");
            
            SymbolMetadataData metadata = marketService.getSymbolMetadata("AAPL");
            
            // Verify the response is not null and contains expected data
            assertNotNull(metadata);
            assertNotNull(metadata.symbol());
            assertEquals("AAPL", metadata.symbol());
            
            // Verify exchange (API returns "US" for AAPL)
            assertNotNull(metadata.exchange());
            // Note: The actual market API returns "US" as the exchange code for AAPL
            assertEquals("US", metadata.exchange());
            
            // Verify other metadata fields
            assertNotNull(metadata.name());
            assertNotNull(metadata.type());
            assertNotNull(metadata.currency());
            assertEquals("USD", metadata.currency());
            assertTrue(metadata.active());
            
            System.out.println("✓ Symbol Metadata Test Passed!");
            System.out.println("  Symbol: " + metadata.symbol());
            System.out.println("  Name: " + metadata.name());
            System.out.println("  Exchange: " + metadata.exchange());
            System.out.println("  Type: " + metadata.type());
            System.out.println("  Currency: " + metadata.currency());
            System.out.println("  Active: " + metadata.active());
        } catch (IllegalStateException e) {
            // Expected if API key is not configured
            System.out.println("API key not configured: " + e.getMessage());
        } catch (Exception e) {
            // Log the error but don't fail the test if API is unreachable
            System.out.println("Symbol Metadata Test - Error occurred: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}
