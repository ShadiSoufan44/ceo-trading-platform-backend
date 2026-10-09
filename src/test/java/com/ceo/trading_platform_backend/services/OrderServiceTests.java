package com.ceo.trading_platform_backend.services;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.repositories.OrderRepository;
import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.PortfolioType;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.exception.ResourceNotFoundException;


@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Tests")
public class OrderServiceTests {
    
    @Mock // mock repository stubbing
    private OrderRepository orderRepository;
    @Mock
    private MarketService marketService;
    @Mock 
    private InstrumentService instrumentService;
    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private OrderService orderService;

    
    private final UUID uuid1 = UUID.randomUUID();
    private final UUID uuid2 = UUID.randomUUID();
    private final UUID uuid3 = UUID.randomUUID();

    private Instrument stockInstrument;

    @BeforeEach 
    void setUp() {
        stockInstrument = new Instrument("AAPL", InstrumentType.EQUITY, "Apple Inc.");

    }

    // methods to set up testing values
    private Order createTestOrder(UUID orderId) {
        Order order = new Order(
            null, 
            null, 
            null, 
            null, 
            new BigDecimal(2), 
            new BigDecimal(0.1), 
            new BigDecimal(5)
        );
        System.out.println(order.getQuote().toString());
        return order;
    }

    private Order createWorkingSellOrder(UUID orderId) {
        Order order = new Order(
            null, 
            Side.SELL, 
            null, 
            null, 
            BigDecimal.ONE, 
            new BigDecimal(0.1), 
            BigDecimal.ONE
        );
        return order;
    }

    private Holding createStockHolding(Portfolio portfolio) {
        return new Holding(
            Instant.now(),
            BigDecimal.ONE,
            BigDecimal.ONE,
            stockInstrument,
            createTestOrder(uuid1),
            portfolio
        );
    }
    
    private Portfolio createTestPortfolio(UUID id, PortfolioType type) {
        Portfolio portfolio = new Portfolio(type);
        return portfolio;
    }

    // classes for tests for each method
    @Nested
    @DisplayName("Test getOrderById()")
    class GetOrderByIdTests {

        @Test 
        @DisplayName("Should return correct order if it exists")
        void shouldReturnOrderWhenExists() {
            @Nullable Optional<Order> expectedOrder = Optional.ofNullable(createTestOrder(uuid1));
            when(orderRepository.findById(uuid1)).thenReturn(expectedOrder);

            Optional<Order> actualOrder = orderService.getOrderById(uuid1);

            assertThat(actualOrder).isNotNull().isEqualTo(expectedOrder);
            verify(orderRepository).findById(uuid1);

        }

        @Test
        @DisplayName("Should return ResourceNotFoundExceptions")
        void shouldThrowExceptionWhenNotExists() {
            Optional<Order> emptyOrder = Optional.empty();
            when(orderRepository.findById(uuid2)).thenReturn(emptyOrder);
            Optional<Order> actualOrder = orderService.getOrderById(uuid2);

            assertThat(actualOrder).isNotNull().isEqualTo(emptyOrder);
            verify(orderRepository).findById(uuid2);
        }

    }

    @Nested
    @DisplayName("Test getAllOrders()") 
    class GetAllOrders {

        @Test
        @DisplayName("Should return all orders made")
        void testGetAllOrders() {
            @Nullable Optional<Order> expectedOrder1 = Optional.ofNullable(createTestOrder(uuid1));
            @Nullable Optional<Order> expectedOrder2 = Optional.ofNullable(createTestOrder(uuid2));
            List<Order> expectedOrders = new ArrayList<Order>();
            expectedOrders.add(expectedOrder1.orElse(null));
            expectedOrders.add(expectedOrder2.orElse(null));
            when(orderRepository.findAll()).thenReturn(expectedOrders);

            List<Order> actualOrders = orderService.getAllOrders();
            assertThat(actualOrders).isNotNull().isEqualTo(expectedOrders);
            verify(orderRepository).findAll();
        }

        @Test
        @DisplayName("Should return an empty list")
        void testGetAllOrdersWhenEmpty() {
            List<Order> expectedOrders = new ArrayList<Order>();
            when(orderRepository.findAll()).thenReturn(expectedOrders);

            List<Order> actualOrders = orderService.getAllOrders();
            assertThat(actualOrders).isNotNull().isEqualTo(expectedOrders);
            verify(orderRepository).findAll();
        }

    }

    @Nested
    @DisplayName("Test getClientOrders()")
    class GetClientOrders {

        @Test
        @DisplayName("Should return all orders by this client")
        void testGetClientOrders() {
            @Nullable Optional<Order> expectedOrder1 = Optional.ofNullable(createTestOrder(uuid1));
            @Nullable Optional<Order> expectedOrder2 = Optional.ofNullable(createTestOrder(uuid2));
            List<Order> expectedOrders = new ArrayList<Order>();
            expectedOrders.add(expectedOrder1.orElse(null));
            expectedOrders.add(expectedOrder2.orElse(null));

            when(orderRepository.findByClientId(uuid3)).thenReturn(expectedOrders);

            List<Order> actualOrders = orderService.getClientOrders(uuid3);

            assertThat(actualOrders).isNotNull().isEqualTo(expectedOrders);
            verify(orderRepository).findByClientId(uuid3);
        }

        @Test
        @DisplayName("Should return empty list when client id not found")
        void testGetClientOrdersWhenNotExist() {
            List<Order> expectedOrders = new ArrayList<Order>();

            when(orderRepository.findByClientId(uuid3)).thenReturn(expectedOrders);
            List<Order> actualOrders = orderService.getClientOrders(uuid3);

            assertThat(actualOrders).isNotNull().isEqualTo(expectedOrders);
            verify(orderRepository).findByClientId(uuid3);
        }
    }

    @Nested
    @DisplayName("Test validate()")
    class Validate {
        
        @Test 
        @DisplayName("Should reject order with untradable instrument")
        void testUntradableInstrumentValidation() {
            // test that an order with an untradable instrument is rejected
            Order badOrder = createTestOrder(uuid1); // try to validate this
            badOrder.setInstrumentId(uuid2);

            // make time in ours ot isolate increase threshold
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            badOrder.addOrderStatusChange(initialStatusChange);

            // stub instrumentSerrvice to return set up instrument
            // stub marketService to say instrument untradable
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(marketService.isActiveSymbol(stockInstrument.getSymbol())).thenReturn(false);
            
            Order resultOrder = orderService.validate(badOrder);
 
            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.REJECTED);
        }

        @Test
        @DisplayName("Should reject sell orders that do not have sufficient holdings")
        void testInsufficientHoldingsValidation() {
            // test that a sell order is rejected if the user does not have enough holdings
            Order badOrder = createTestOrder(uuid1);
            badOrder.setInstrumentId(uuid2);
            badOrder.setSide(Side.SELL);
            badOrder.setPortfolioId(uuid3);

            // make time in ours ot isolate increase threshold
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            badOrder.addOrderStatusChange(initialStatusChange);

            List<Holding> holdingsList = new ArrayList<>();
            holdingsList.add(createStockHolding(createTestPortfolio(uuid3, PortfolioType.BROKERAGE)));

            // stub portfolioService to return holdings list
            when(portfolioService.getHoldingsByPortfolioId(badOrder.getPortfolioId())).thenReturn(holdingsList);
            when(instrumentService.getInstrumentById(badOrder.getInstrumentId())).thenReturn(stockInstrument);
            when(marketService.isActiveSymbol(stockInstrument.getSymbol())).thenReturn(true);

            Order resultOrder = orderService.validate(badOrder);

            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.REJECTED);
        }

        @Test
        @DisplayName("Should pass buy order validation")
        void testWorkingBuyValidation() {
            Order workingOrder = createTestOrder(uuid1);
            workingOrder.setInstrumentId(uuid2);

            // make time in ours ot isolate increase threshold
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            workingOrder.addOrderStatusChange(initialStatusChange);

            // stub instrumentSerrvice to return set up instrument
            // stub marketService to say instrument untradable
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(marketService.isActiveSymbol(stockInstrument.getSymbol())).thenReturn(true);

            Order resultOrder = orderService.validate(workingOrder);

            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.ACCEPTED);

        }

        @Test
        @DisplayName("Should pass sell order validation")
        void testWorkingSellValidation() {
            Order workingOrder = createWorkingSellOrder(uuid1);
            workingOrder.setInstrumentId(uuid2);
            workingOrder.setPortfolioId(uuid3);

            // make time in ours ot isolate increase threshold
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            workingOrder.addOrderStatusChange(initialStatusChange);

            List<Holding> holdingsList = new ArrayList<>();
            holdingsList.add(createStockHolding(createTestPortfolio(uuid3, PortfolioType.BROKERAGE)));

            // stub portfolioService to return holdings list
            when(portfolioService.getHoldingsByPortfolioId(workingOrder.getPortfolioId())).thenReturn(holdingsList);
            when(instrumentService.getInstrumentById(workingOrder.getInstrumentId())).thenReturn(stockInstrument);
            when(marketService.isActiveSymbol(stockInstrument.getSymbol())).thenReturn(true);

            Order resultOrder = orderService.validate(workingOrder);

            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }
    }

    @Nested 
    @DisplayName("Test execute()")
    class Execute {

        // @Test  I DONT KNOW HOW TO STUB INSTANT.MNOW
        @DisplayName("Should reject out of hours order")
        void testOutOfHoursOrder() {


            Instant specificTime = Instant.parse("2026-10-09T19:00:00Z"); // execution chekcs time NOW so need to stub the insant.now to be out ofhours
            // try (MockedStatic<Instant> mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {
            //     mockedInstant.when(Instant::now).thenReturn(specificTime);
                var clock = Clock.fixed(specificTime, ZoneOffset.UTC);
                var mockedInstant = Instant.now(clock);
                MockedStatic<Instant> mockedStatic = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS);
                mockedStatic.when(Instant::now).thenReturn(mockedInstant);
                OrderService orderService = new OrderService(orderRepository, portfolioService, instrumentService, marketService);
                Order badOrder = createWorkingSellOrder(uuid1);
                badOrder.setInstrumentId(uuid2);
                LocalTime time = LocalTime.of(19, 0); // 7pm
                ZoneId easternZone = ZoneId.of("America/New_York");
                LocalDate date = LocalDate.now();
                ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
                Instant instantUTC = easternDateTime.toInstant();

                OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
                badOrder.addOrderStatusChange(initialStatusChange);

                assertEquals(specificTime, Instant.now());

                when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument); 
                when(marketService.getPrice(stockInstrument.getSymbol())).thenReturn(BigDecimal.ONE);

                Order resultOrder = orderService.execute(badOrder);
        
                assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.REJECTED);
            // }
 
        }

        @Test
        @DisplayName("Should reject orders when price change exceeds increase threshold")
        void testExceededIncreaseThreshold() {
            Order badOrder = createTestOrder(uuid1);
            badOrder.setInstrumentId(uuid2);

            // make time in ours ot isolate increase threshold
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            badOrder.addOrderStatusChange(initialStatusChange);

            when(instrumentService.getInstrumentById(badOrder.getInstrumentId())).thenReturn(stockInstrument);
            when(marketService.getPrice(stockInstrument.getSymbol())).thenReturn(new BigDecimal(10));
            
            Order resultOrder = orderService.execute(badOrder);
    
            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.REJECTED);
        }

        @Test 
        @DisplayName("Should reject buy order with insufficient funds")
        void testBuyWithInsufficientFunds() {
            Order badOrder = createTestOrder(uuid1);
            badOrder.setInstrumentId(uuid2);
            badOrder.setSide(Side.BUY);
            badOrder.setPortfolioId(uuid3);
            badOrder.setIncreaseThreshold(new BigDecimal(100)); // within threshold to isolate funds
            System.out.println(badOrder.getQuote().toString());
            // make time in ours ot isolate insufficient funds
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            badOrder.addOrderStatusChange(initialStatusChange);

            when(instrumentService.getInstrumentById(badOrder.getInstrumentId())).thenReturn(stockInstrument);
            when(marketService.getPrice(stockInstrument.getSymbol())).thenReturn(new BigDecimal(10));
            when(portfolioService.getBuyingPowerByPortfolioId(badOrder.getPortfolioId())).thenReturn(BigDecimal.ONE);

            Order resultOrder = orderService.execute(badOrder);
    
            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.REJECTED);

        }

        @Test 
        @DisplayName("Should execute a proper buy order")
        void testWorkingBuyExecution() {

            Order workingOrder = createTestOrder(uuid1);
            workingOrder.setInstrumentId(uuid2);
            workingOrder.setIncreaseThreshold(new BigDecimal(100)); // set increase threshold to pass

            // make time in hour
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            workingOrder.addOrderStatusChange(initialStatusChange);

            // stub instrumentSerrvice to return set up instrument
            // stub marketService to say instrument untradable
            when(instrumentService.getInstrumentById(uuid2)).thenReturn(stockInstrument);
            when(marketService.getPrice(stockInstrument.getSymbol())).thenReturn(new BigDecimal(1));
            // when(portfolioService.getBuyingPowerByPortfolioId(workingOrder.getPortfolioId())).thenReturn(new BigDecimal(100));

            // when(marketService.isActiveSymbol(stockInstrument.getSymbol())).thenReturn(true);

            Order resultOrder = orderService.execute(workingOrder);

            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.FULFILLED);
        }

    
        @Test @DisplayName("Should execute proper sell order")
        void testWorkingSellExecution() {
            Order workingOrder = createWorkingSellOrder(uuid1);
            workingOrder.setInstrumentId(uuid2);
            workingOrder.setPortfolioId(uuid3);

            // make time in hours
            LocalTime time = LocalTime.of(12, 0); // 7pm
            ZoneId easternZone = ZoneId.of("America/New_York");
            LocalDate date = LocalDate.now();
            ZonedDateTime easternDateTime = ZonedDateTime.of(date, time, easternZone);
            Instant instantUTC = easternDateTime.toInstant();

            OrderStatusChange initialStatusChange = new OrderStatusChange(OrderStatus.PENDING, "order submitted", instantUTC);
            workingOrder.addOrderStatusChange(initialStatusChange);

            List<Holding> holdingsList = new ArrayList<>();
            holdingsList.add(createStockHolding(createTestPortfolio(uuid3, PortfolioType.BROKERAGE)));

            // stub portfolioService to return holdings list
            when(instrumentService.getInstrumentById(workingOrder.getInstrumentId())).thenReturn(stockInstrument);
            // when(marketService.isActiveSymbol(stockInstrument.getSymbol())).thenReturn(true);
            when(marketService.getPrice(stockInstrument.getSymbol())).thenReturn(new BigDecimal(1));


            Order resultOrder = orderService.execute(workingOrder);

            assertThat(resultOrder.getCurrentOrderStatus().getStatus()).isEqualTo(OrderStatus.FULFILLED);
        }

    }

}
