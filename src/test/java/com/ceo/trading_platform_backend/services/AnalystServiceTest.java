package com.ceo.trading_platform_backend.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalystService Tests")
class AnalystServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AnalystService analystService;

    @Nested
    @DisplayName("generateVolumeReport Tests")
    class GenerateVolumeReportTests {

        @Test
        @DisplayName("Should return monthly volumes from database")
        void testGenerateVolumeReportReturnsData() {
            // Arrange
            List<Map<String, Object>> mockData = new ArrayList<>();
            
            Map<String, Object> record1 = new HashMap<>();
            record1.put("month_year", "2026-10-01");
            record1.put("total_orders", 8);
            record1.put("total_volume", 245);
            record1.put("symbol", "AAPL");
            mockData.add(record1);
            
            Map<String, Object> record2 = new HashMap<>();
            record2.put("month_year", "2026-10-01");
            record2.put("total_orders", 6);
            record2.put("total_volume", 178);
            record2.put("symbol", "GOOGL");
            mockData.add(record2);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(mockData);

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(2, result.size(), "Should return 2 volume records");
            assertEquals("AAPL", result.get(0).get("symbol"), "First symbol should be AAPL");
            assertEquals(245, result.get(0).get("total_volume"), "First volume should be 245");
            
            verify(jdbcTemplate, times(1)).queryForList(anyString());
        }

        @Test
        @DisplayName("Should return empty list when no volumes found")
        void testGenerateVolumeReportReturnsEmpty() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isEmpty(), "Result should be empty");
            
            verify(jdbcTemplate, times(1)).queryForList(anyString());
        }

        @Test
        @DisplayName("Should call JdbcTemplate with SQL query containing FULFILLED status")
        void testGenerateVolumeReportQueriesFulfilledOrders() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

            // Act
            analystService.generateVolumeReport();

            // Assert - Verify the SQL contains key filtering criteria
            verify(jdbcTemplate).queryForList(argThat(sql -> 
                sql.contains("FULFILLED") && 
                sql.contains("DATE_TRUNC") &&
                sql.contains("GROUP BY")
            ));
        }
    }

    @Nested
    @DisplayName("generateActivityReport Tests")
    class GenerateActivityReportTests {

        @Test
        @DisplayName("Should return client activity from database")
        void testGenerateActivityReportReturnsData() {
            // Arrange
            List<Map<String, Object>> mockData = new ArrayList<>();
            
            Map<String, Object> record1 = new HashMap<>();
            record1.put("user_id", 1);
            record1.put("full_name", "Alice Johnson");
            record1.put("month_year", "2026-10-01");
            record1.put("total_trades", 8);
            record1.put("total_volume_value", 12450.50);
            mockData.add(record1);
            
            Map<String, Object> record2 = new HashMap<>();
            record2.put("user_id", 2);
            record2.put("full_name", "Bob Smith");
            record2.put("month_year", "2026-10-01");
            record2.put("total_trades", 5);
            record2.put("total_volume_value", 8925.75);
            mockData.add(record2);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(mockData);

            // Act
            List<Map<String, Object>> result = analystService.generateActivityReport();

            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals(2, result.size(), "Should return 2 activity records");
            assertEquals("Alice Johnson", result.get(0).get("full_name"), "First client should be Alice");
            assertEquals(8, result.get(0).get("total_trades"), "Alice should have 8 trades");
            assertEquals(12450.50, result.get(0).get("total_volume_value"), "Alice volume should be 12450.50");
            
            verify(jdbcTemplate, times(1)).queryForList(anyString());
        }

        @Test
        @DisplayName("Should return empty list when no activity found")
        void testGenerateActivityReportReturnsEmpty() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

            // Act
            List<Map<String, Object>> result = analystService.generateActivityReport();

            // Assert
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isEmpty(), "Result should be empty");
            
            verify(jdbcTemplate, times(1)).queryForList(anyString());
        }

        @Test
        @DisplayName("Should filter by CLIENT role and FULFILLED status")
        void testGenerateActivityReportFiltersCorrectly() {
            // Arrange
            when(jdbcTemplate.queryForList(anyString())).thenReturn(new ArrayList<>());

            // Act
            analystService.generateActivityReport();

            // Assert - Verify SQL contains role and status filtering
            verify(jdbcTemplate).queryForList(argThat(sql -> 
                sql.contains("FULFILLED") && 
                sql.contains("role = 'CLIENT'") &&
                sql.contains("DATE_TRUNC") &&
                sql.contains("GROUP BY")
            ));
        }
    }

    @Nested
    @DisplayName("Edge Cases & Boundary Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle null data from database")
        void testHandleNullData() {
            // Arrange - Simulate database returning null
            when(jdbcTemplate.queryForList(anyString())).thenReturn(null);

            // Act & Assert - Should not throw NullPointerException
            assertNotNull(analystService.generateVolumeReport());
        }

        @Test
        @DisplayName("Should handle massive dataset (10K+ records)")
        void testHandleMassiveDataset() {
            // Arrange - Create 10,000 records
            List<Map<String, Object>> largeDataset = new ArrayList<>();
            for (int i = 0; i < 10000; i++) {
                Map<String, Object> record = new HashMap<>();
                record.put("month_year", "2026-10-01");
                record.put("total_orders", 100 + i);
                record.put("total_volume", 5000 + i);
                record.put("symbol", "STOCK" + (i % 100));
                largeDataset.add(record);
            }

            when(jdbcTemplate.queryForList(anyString())).thenReturn(largeDataset);

            // Act
            long startTime = System.currentTimeMillis();
            List<Map<String, Object>> result = analystService.generateVolumeReport();
            long duration = System.currentTimeMillis() - startTime;

            // Assert - Should complete in reasonable time (< 1 second)
            assertEquals(10000, result.size());
            assertTrue(duration < 1000, "Performance: Should complete in < 1 second, took " + duration + "ms");
            
            verify(jdbcTemplate, times(1)).queryForList(anyString());
        }

        @Test
        @DisplayName("Should handle data with null/missing volume values")
        void testHandleNullVolumes() {
            // Arrange - Some records have null volume
            List<Map<String, Object>> mixedData = new ArrayList<>();
            
            Map<String, Object> validRecord = new HashMap<>();
            validRecord.put("month_year", "2026-10-01");
            validRecord.put("total_orders", 10);
            validRecord.put("total_volume", 250);
            validRecord.put("symbol", "AAPL");
            mixedData.add(validRecord);
            
            Map<String, Object> nullRecord = new HashMap<>();
            nullRecord.put("month_year", "2026-10-01");
            nullRecord.put("total_orders", 5);
            nullRecord.put("total_volume", null);  // ← Null value
            nullRecord.put("symbol", "GOOGL");
            mixedData.add(nullRecord);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(mixedData);

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert - Should not throw, handles gracefully
            assertEquals(2, result.size());
            assertNull(result.get(1).get("total_volume"));
            
            verify(jdbcTemplate, times(1)).queryForList(anyString());
        }

        @Test
        @DisplayName("Should handle edge case: zero values")
        void testHandleZeroValues() {
            // Arrange
            List<Map<String, Object>> zeroData = new ArrayList<>();
            
            Map<String, Object> record = new HashMap<>();
            record.put("month_year", "2026-10-01");
            record.put("total_orders", 0);  // ← Zero orders
            record.put("total_volume", 0);  // ← Zero volume
            record.put("symbol", "ZZZZ");
            zeroData.add(record);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(zeroData);

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert
            assertEquals(1, result.size());
            assertEquals(0, result.get(0).get("total_orders"));
            assertEquals(0, result.get(0).get("total_volume"));
        }

        @Test
        @DisplayName("Should handle very large numbers (edge case: max long values)")
        void testHandleLargeNumbers() {
            // Arrange
            List<Map<String, Object>> largeNumbers = new ArrayList<>();
            
            Map<String, Object> record = new HashMap<>();
            record.put("month_year", "2026-10-01");
            record.put("total_orders", 999999999);  // Large number
            record.put("total_volume", 1000000000L);  // Very large volume
            record.put("symbol", "MEGA");
            largeNumbers.add(record);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(largeNumbers);

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert
            assertEquals(1, result.size());
            assertEquals(999999999, result.get(0).get("total_orders"));
            assertEquals(1000000000L, result.get(0).get("total_volume"));
        }

        @Test
        @DisplayName("Should handle special characters in client names")
        void testHandleSpecialCharactersInNames() {
            // Arrange
            List<Map<String, Object>> specialData = new ArrayList<>();
            
            Map<String, Object> record = new HashMap<>();
            record.put("user_id", 1);
            record.put("full_name", "O'Brien-Smith & Co.");  // Special chars
            record.put("month_year", "2026-10-01");
            record.put("total_trades", 5);
            record.put("total_volume_value", 5000.50);
            specialData.add(record);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(specialData);

            // Act
            List<Map<String, Object>> result = analystService.generateActivityReport();

            // Assert
            assertEquals(1, result.size());
            assertEquals("O'Brien-Smith & Co.", result.get(0).get("full_name"));
        }

        @Test
        @DisplayName("Should handle duplicate records in same month")
        void testHandleDuplicateRecords() {
            // Arrange - Same instrument, same month, multiple records
            List<Map<String, Object>> duplicateData = new ArrayList<>();
            
            for (int i = 0; i < 3; i++) {
                Map<String, Object> record = new HashMap<>();
                record.put("month_year", "2026-10-01");
                record.put("total_orders", 10);
                record.put("total_volume", 100);
                record.put("symbol", "AAPL");  // Same symbol, same month
                duplicateData.add(record);
            }

            when(jdbcTemplate.queryForList(anyString())).thenReturn(duplicateData);

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert - Should return all records (database query already aggregates)
            assertEquals(3, result.size());
        }

        @Test
        @DisplayName("Should handle database returning exactly 1 record")
        void testHandleSingleRecord() {
            // Arrange
            List<Map<String, Object>> singleRecord = new ArrayList<>();
            Map<String, Object> record = new HashMap<>();
            record.put("month_year", "2026-10-01");
            record.put("total_orders", 1);
            record.put("total_volume", 50);
            record.put("symbol", "X");
            singleRecord.add(record);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(singleRecord);

            // Act
            List<Map<String, Object>> result = analystService.generateVolumeReport();

            // Assert
            assertEquals(1, result.size());
            assertEquals(50, result.get(0).get("total_volume"));
        }

        @Test
        @DisplayName("Should handle fractional trading values (decimals)")
        void testHandleFractionalValues() {
            // Arrange
            List<Map<String, Object>> fractionalData = new ArrayList<>();
            
            Map<String, Object> record = new HashMap<>();
            record.put("user_id", 1);
            record.put("full_name", "Penny Trader");
            record.put("month_year", "2026-10-01");
            record.put("total_trades", 3);
            record.put("total_volume_value", 1234.567);  // Fractional
            fractionalData.add(record);

            when(jdbcTemplate.queryForList(anyString())).thenReturn(fractionalData);

            // Act
            List<Map<String, Object>> result = analystService.generateActivityReport();

            // Assert
            assertEquals(1, result.size());
            assertEquals(1234.567, result.get(0).get("total_volume_value"));
        }
    }
}
