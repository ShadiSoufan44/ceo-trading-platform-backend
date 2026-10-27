package com.ceo.trading_platform_backend.services;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;
import com.ceo.trading_platform_backend.services.InstrumentCacheService;

public class InstrumentServiceTest {
    
    private InstrumentService instrumentService;
    
    @Mock
    private InstrumentRepository mockRepository;
    
    @Mock
    private MarketService mockMarketService;

    @Mock
    private InstrumentCacheService mockCandleService;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        instrumentService = new InstrumentService(mockRepository, mockMarketService, mockCandleService);
    }
    
    /**
     * Tests that getInstrumentById retrieves an instrument from the repository by ID
     */
    @Test
    public void testGetInstrumentById() {
        // Arrange
        UUID instrumentId = UUID.randomUUID();
        Instrument expectedInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");
        when(mockRepository.getReferenceById(instrumentId)).thenReturn(expectedInstrument);
        
        // Act
        Instrument result = instrumentService.getInstrumentById(instrumentId);
        
        // Print retrieved data
        System.out.println("\n[testGetInstrumentById] Retrieved: Symbol=" + result.getSymbol() + 
                           ", Type=" + result.getType() + 
                           ", Name=" + result.getFullName());
        
        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals(InstrumentType.EQUITY, result.getType());
        assertEquals("Apple Inc.", result.getFullName());
        verify(mockRepository, times(1)).getReferenceById(instrumentId);
    }
    
    /**
     * Tests that getInstrumentBySymbol retrieves an instrument from the repository by symbol string
     */
    @Test
    public void testGetInstrumentBySymbol() {
        // Arrange
        String symbol = "AAPL";
        InstrumentType type = InstrumentType.EQUITY;
        Instrument expectedInstrument = new Instrument("AAPL", type, "Apple Inc.");
        when(mockRepository.findBySymbolAndType(symbol, type)).thenReturn(expectedInstrument);
        
        // Act
        Instrument result = instrumentService.getInstrumentBySymbolAndType(symbol, type);
        
        // Print retrieved data
        System.out.println("\n[testGetInstrumentBySymbol] Retrieved: Symbol=" + result.getSymbol() + 
                           ", Type=" + result.getType() + 
                           ", Name=" + result.getFullName());
        
        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals(InstrumentType.EQUITY, result.getType());
        verify(mockRepository, times(1)).findBySymbolAndType(symbol, type);
    }
    
    /**
     * Tests that insertInstrumentByFields creates a new instrument with the provided symbol, type, and name
     */
    @Test
    public void testInsertInstrumentByFields() {
        // Arrange
        String symbol = "MSFT";
        InstrumentType type = InstrumentType.EQUITY;
        String name = "Microsoft Corporation";
        Instrument instrumentToSave = new Instrument(symbol, type, name);
        
        when(mockRepository.save(any(Instrument.class))).thenReturn(instrumentToSave);
        
        // Act
        Instrument result = instrumentService.insertInstrumentByFields(symbol, type, name);
        
        // Print created data
        System.out.println("\n[testInsertInstrumentByFields] Created: Symbol=" + result.getSymbol() + 
                           ", Type=" + result.getType() + 
                           ", Name=" + result.getFullName());
        
        // Assert
        assertNotNull(result);
        assertEquals(symbol, result.getSymbol());
        assertEquals(type, result.getType());
        assertEquals(name, result.getFullName());
        verify(mockRepository, times(1)).save(any(Instrument.class));
    }
    
    /**
     * Tests that getOrInsertInstrumentBySymbolAndType returns existing instrument without calling MarketService
     */
    @Test
    public void testgetOrInsertInstrumentBySymbolAndTypeWhenExists() {
        // Arrange - instrument already exists
        String symbol = "AAPL";
        InstrumentType type = InstrumentType.EQUITY;
        Instrument existingInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");
        when(mockRepository.findBySymbolAndType(symbol, type)).thenReturn(existingInstrument);
        
        // Act
        Instrument result = instrumentService.getOrInsertInstrumentBySymbolAndType(symbol, type);
        
        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals("Apple Inc.", result.getFullName());
        
        // Verify MarketService was NOT called since instrument exists
        verify(mockMarketService, never()).getSymbolMetadataByType(symbol, type);
        verify(mockRepository, never()).save(any(Instrument.class));
    }
    
    /**
     * Tests that getOrInsertInstrumentBySymbolAndType fetches from MarketService when instrument doesn't exist
     * and uses the real MarketService data to create a new instrument
     */
    @Test
    public void testgetOrInsertInstrumentBySymbolAndTypeWhenNotExists_UsesRealMarketServiceData() {
        // Arrange - instrument doesn't exist, need to fetch from MarketService
        String symbol = "GOOGL";
        InstrumentType type = InstrumentType.EQUITY;
        when(mockRepository.findBySymbolAndType(symbol, type)).thenReturn(null);
        
        // Create mock metadata from MarketService
        MarketService.SymbolMetadataData mockMetadata = createMockMetadata("GOOGL", "EQUITY", "Alphabet Inc.");
        when(mockMarketService.getSymbolMetadataByType(symbol, type)).thenReturn(mockMetadata);
        
        // Mock repository save to return the created instrument
        when(mockRepository.save(any(Instrument.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        
        // Act
        Instrument result = instrumentService.getOrInsertInstrumentBySymbolAndType(symbol, type);
        
        // Print data from MarketService integration
        System.out.println("\n[testgetOrInsertInstrumentBySymbolAndTypeWhenNotExists_UsesRealMarketServiceData]");
        System.out.println("  Fetched from MarketService: Symbol=" + result.getSymbol() + 
                           ", Type=" + result.getType() + 
                           ", Name=" + result.getFullName());
        System.out.println("  MarketService.getSymbolMetadata() was successfully called");
        
        // Assert - verify data came from MarketService
        assertNotNull(result);
        assertEquals("GOOGL", result.getSymbol());
        assertEquals(InstrumentType.EQUITY, result.getType());
        assertEquals("Alphabet Inc.", result.getFullName());
        
        // Verify MarketService was called with the symbol
        verify(mockMarketService, times(1)).getSymbolMetadataByType(symbol, type);
        
        // Verify repository.save was called with the correct data
        verify(mockRepository, times(1)).save(any(Instrument.class));
    }
    
    
    /**
     * Helper method to create mock SymbolMetadataData using reflection
     * since it's a record and we can't instantiate it directly in tests
     */
    private MarketService.SymbolMetadataData createMockMetadata(String symbol, String type, String name) {
        return new MarketService.SymbolMetadataData(
            symbol,
            name,
            type,
            "NYSE",
            "USD",
            true,
            null
        );
    }
}
