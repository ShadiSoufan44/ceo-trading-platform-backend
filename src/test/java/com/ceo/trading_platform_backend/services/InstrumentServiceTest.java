package com.ceo.trading_platform_backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.repositories.InstrumentRepository;

public class InstrumentServiceTest {
    
    private InstrumentService instrumentService;
    
    @Mock
    private InstrumentRepository mockRepository;
    
    @Mock
    private MarketService mockMarketService;
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        instrumentService = new InstrumentService(mockRepository, mockMarketService);
    }
    
    /**
     * Tests that getInstrumentById retrieves an instrument from the repository by ID
     */
    @Test
    public void testGetInstrumentById() {
        // Arrange
        int instrumentId = 1;
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
        Instrument expectedInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");
        when(mockRepository.findBySymbol(symbol)).thenReturn(expectedInstrument);
        
        // Act
        Instrument result = instrumentService.getInstrumentBySymbol(symbol);
        
        // Print retrieved data
        System.out.println("\n[testGetInstrumentBySymbol] Retrieved: Symbol=" + result.getSymbol() + 
                           ", Type=" + result.getType() + 
                           ", Name=" + result.getFullName());
        
        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals(InstrumentType.EQUITY, result.getType());
        verify(mockRepository, times(1)).findBySymbol(symbol);
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
     * Tests that getOrInsertInstrumentBySymbol returns existing instrument without calling MarketService
     */
    @Test
    public void testGetOrInsertInstrumentBySymbolWhenExists() {
        // Arrange - instrument already exists
        String symbol = "AAPL";
        Instrument existingInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");
        when(mockRepository.findBySymbol(symbol)).thenReturn(existingInstrument);
        
        // Act
        Instrument result = instrumentService.getOrInsertInstrumentBySymbol(symbol);
        
        // Assert
        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        assertEquals("Apple Inc.", result.getFullName());
        
        // Verify MarketService was NOT called since instrument exists
        verify(mockMarketService, never()).getSymbolMetadata(symbol);
        verify(mockRepository, never()).save(any(Instrument.class));
    }
    
    /**
     * Tests that getOrInsertInstrumentBySymbol fetches from MarketService when instrument doesn't exist
     * and uses the real MarketService data to create a new instrument
     */
    @Test
    public void testGetOrInsertInstrumentBySymbolWhenNotExists_UsesRealMarketServiceData() {
        // Arrange - instrument doesn't exist, need to fetch from MarketService
        String symbol = "GOOGL";
        when(mockRepository.findBySymbol(symbol)).thenReturn(null);
        
        // Create mock metadata from MarketService
        MarketService.SymbolMetadataData mockMetadata = createMockMetadata("GOOGL", "EQUITY", "Alphabet Inc.");
        when(mockMarketService.getSymbolMetadata(symbol)).thenReturn(mockMetadata);
        
        // Mock repository save to return the created instrument
        when(mockRepository.save(any(Instrument.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        
        // Act
        Instrument result = instrumentService.getOrInsertInstrumentBySymbol(symbol);
        
        // Print data from MarketService integration
        System.out.println("\n[testGetOrInsertInstrumentBySymbolWhenNotExists_UsesRealMarketServiceData]");
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
        verify(mockMarketService, times(1)).getSymbolMetadata(symbol);
        
        // Verify repository.save was called with the correct data
        verify(mockRepository, times(1)).save(any(Instrument.class));
    }
    
    /**
     * Tests that parseInstrumentType defaults to EQUITY when MarketService returns an unknown type string
     */
    @Test
    public void testGetOrInsertInstrumentBySymbolParsesUnknownTypeAsEquity() {
        // Arrange - MarketService returns unknown instrument type
        String symbol = "CRYPTO";
        when(mockRepository.findBySymbol(symbol)).thenReturn(null);
        
        MarketService.SymbolMetadataData mockMetadata = createMockMetadata("CRYPTO", "UNKNOWN_TYPE", "Some Crypto");
        when(mockMarketService.getSymbolMetadata(symbol)).thenReturn(mockMetadata);
        
        when(mockRepository.save(any(Instrument.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        
        // Act
        Instrument result = instrumentService.getOrInsertInstrumentBySymbol(symbol);
        
        // Print type parsing result
        System.out.println("\n[testGetOrInsertInstrumentBySymbolParsesUnknownTypeAsEquity]");
        System.out.println("  Input type from MarketService: UNKNOWN_TYPE");
        System.out.println("  Parsed type: " + result.getType() + " (defaulted to EQUITY)");
        System.out.println("  Retrieved: Symbol=" + result.getSymbol() + ", Name=" + result.getFullName());
        
        // Assert - should default to EQUITY type
        assertNotNull(result);
        assertEquals(InstrumentType.EQUITY, result.getType());
        assertEquals("Some Crypto", result.getFullName());
    }
    
    /**
     * Tests that parseInstrumentType defaults to EQUITY when MarketService returns null for type
     */
    @Test
    public void testGetOrInsertInstrumentBySymbolParsesNullTypeAsEquity() {
        // Arrange - MarketService returns null type
        String symbol = "TEST";
        when(mockRepository.findBySymbol(symbol)).thenReturn(null);
        
        MarketService.SymbolMetadataData mockMetadata = createMockMetadata("TEST", null, "Test Instrument");
        when(mockMarketService.getSymbolMetadata(symbol)).thenReturn(mockMetadata);
        
        when(mockRepository.save(any(Instrument.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0);
        });
        
        // Act
        Instrument result = instrumentService.getOrInsertInstrumentBySymbol(symbol);
        
        // Print null type parsing result
        System.out.println("\n[testGetOrInsertInstrumentBySymbolParsesNullTypeAsEquity]");
        System.out.println("  Input type from MarketService: null");
        System.out.println("  Parsed type: " + result.getType() + " (defaulted to EQUITY)");
        System.out.println("  Retrieved: Symbol=" + result.getSymbol() + ", Name=" + result.getFullName());
        
        // Assert - should default to EQUITY type
        assertNotNull(result);
        assertEquals(InstrumentType.EQUITY, result.getType());
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
