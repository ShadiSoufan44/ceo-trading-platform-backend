package com.ceo.trading_platform_backend.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.ceo.trading_platform_backend.enums.InstrumentType;
import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.enums.Side;
import com.ceo.trading_platform_backend.enums.UserRole;
import com.ceo.trading_platform_backend.models.Holding;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.models.Portfolio;
import com.ceo.trading_platform_backend.models.User;

/** Executes actual Specifications, pagination and count queries on an isolated, in-memory database.
 * No Spring application startup, Flyway, Kafka, or configured/shared database is used.
 * H2 is not a substitute for final PostgreSQL integration verification.
 */
class OrderRepositorySpecificationsTests {
    private static final Instant FIRST = Instant.parse("2026-10-25T00:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-10-26T00:00:00Z");
    private static final Instant THIRD = Instant.parse("2026-10-27T00:00:00Z");
    private static SessionFactory factory;
    private Session session;
    private OrderRepository repository;
    private User john;
    private User jane;
    private Instrument apple;
    private Instrument usd;

    @BeforeAll
    static void buildIsolatedDatabase() {
        factory = new Configuration()
                .addAnnotatedClass(Order.class).addAnnotatedClass(OrderStatusChange.class)
                .addAnnotatedClass(User.class).addAnnotatedClass(Portfolio.class)
                .addAnnotatedClass(Instrument.class).addAnnotatedClass(Holding.class)
                .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
                .setProperty("hibernate.connection.url", "jdbc:h2:mem:admin-specifications;MODE=PostgreSQL")
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .setProperty("hibernate.show_sql", "false")
                .buildSessionFactory();
    }

    @AfterAll
    static void closeDatabase() {
        if (factory != null) {
            factory.close();
        }
    }

    @BeforeEach
    void setUp() {
        session = factory.openSession();
        session.beginTransaction();
        repository = new JpaRepositoryFactory(session).getRepository(OrderRepository.class);
        john = new User("John Doe", "john@example.test", "unused", FIRST, UserRole.CLIENT);
        jane = new User("Jane Smith", "jane@example.test", "unused", FIRST, UserRole.CLIENT);
        apple = new Instrument("aApL", InstrumentType.EQUITY, "Apple");
        usd = new Instrument("USD", InstrumentType.CASH, "Dollar");
        session.persist(john);
        session.persist(jane);
        session.persist(apple);
        session.persist(usd);
    }

    @AfterEach
    void tearDown() {
        session.getTransaction().rollback();
        session.close();
    }

    @Test
    void allOrdersArePagedByPlacementWithUnknownDatesLastAndCorrectCounts() {
        Order older = order(john, apple, usd);
        event(older, OrderStatus.PENDING, FIRST);
        Order newer = order(jane, apple, usd);
        event(newer, OrderStatus.FULFILLED, SECOND);
        Order unknown = order(john, null, null);
        reload();

        Page<Order> first = search(null, null, null, null, null, null, 0, 1);
        assertIds(first, newer);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(3);
        assertIds(search(null, null, null, null, null, null, 1, 1), older);
        assertIds(search(null, null, null, null, null, null, 2, 1), unknown);
        Page<Order> beyond = search(null, null, null, null, null, null, 3, 1);
        assertThat(beyond).isEmpty();
        assertThat(beyond.getTotalElements()).isEqualTo(3);
    }

    @Test
    void combinesAllFiltersBeforePaginationAndCounting() {
        Order first = order(john, apple, usd);
        event(first, OrderStatus.PENDING, FIRST);
        event(first, OrderStatus.FULFILLED, SECOND);
        Order second = order(john, usd, apple);
        event(second, OrderStatus.FULFILLED, SECOND);
        Order wrongUser = order(jane, apple, usd);
        event(wrongUser, OrderStatus.FULFILLED, FIRST);
        Order wrongStatus = order(john, apple, usd);
        event(wrongStatus, OrderStatus.ACCEPTED, FIRST);
        Order wrongTicker = order(john, usd, null);
        event(wrongTicker, OrderStatus.FULFILLED, FIRST);
        Order tooLate = order(john, apple, usd);
        event(tooLate, OrderStatus.FULFILLED, THIRD);
        reload();

        Page<Order> page = search(john.getUserId(), "%john%", "FULFILLED", FIRST, THIRD, "AAPL", 0, 1);
        assertIds(page, second);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertIds(search(john.getUserId(), "%john%", "FULFILLED", FIRST, THIRD, "AAPL", 1, 1), first);
        assertThat(search(john.getUserId(), "%jane%", null, null, null, null, 0, 20)).isEmpty();
    }

    @Test
    void currentStatusDoesNotMatchAnOlderEvent() {
        Order order = order(john, apple, usd);
        event(order, OrderStatus.FULFILLED, SECOND);
        event(order, OrderStatus.PENDING, FIRST); // Persisted out of chronological order.
        reload();

        assertThat(search(null, null, "PENDING", null, null, null, 0, 20)).isEmpty();
        assertIds(search(null, null, "FULFILLED", null, null, null, 0, 20), order);
    }

    @Test
    void nullDatesAndUuidTiesSelectTheSameLatestStatusAsBefore() {
        Order tied = order(john, apple, usd);
        eventWithId(tied, OrderStatus.ACCEPTED, SECOND, "00000000-0000-0000-0000-000000000001");
        eventWithId(tied, OrderStatus.CANCELLED, SECOND, "80000000-0000-0000-0000-000000000001");
        event(tied, OrderStatus.REJECTED, null); // Must not replace a dated status.
        Order undated = order(john, null, null);
        eventWithId(undated, OrderStatus.PENDING, null, "00000000-0000-0000-0000-000000000002");
        eventWithId(undated, OrderStatus.ACCEPTED, null, "80000000-0000-0000-0000-000000000002");
        Order noHistory = order(john, null, null);
        reload();

        assertIds(search(null, null, "CANCELLED", null, null, null, 0, 20), tied);
        assertIds(search(null, null, "ACCEPTED", null, null, null, 0, 20), undated);
        assertThat(search(null, null, "PENDING", null, null, null, 0, 20)).isEmpty();
        assertThat(search(null, null, "REJECTED", null, null, null, 0, 20)).isEmpty();
        assertThat(search(null, null, null, null, null, null, 0, 20).getContent())
                .extracting(Order::getOrderId).contains(noHistory.getOrderId());
    }

    @Test
    void dateBoundsUseEarliestNonNullEventAndAllowOpenEnds() {
        Order first = order(john, apple, usd);
        event(first, OrderStatus.ACCEPTED, FIRST);
        event(first, OrderStatus.FULFILLED, THIRD);
        event(first, OrderStatus.PENDING, null);
        Order second = order(john, apple, usd);
        event(second, OrderStatus.PENDING, SECOND);
        Order third = order(john, apple, usd);
        event(third, OrderStatus.PENDING, THIRD);
        order(john, null, null);
        reload();

        assertIds(search(null, null, null, FIRST, SECOND, null, 0, 20), first);
        assertIds(search(null, null, null, null, SECOND, null, 0, 20), first);
        assertIds(search(null, null, null, SECOND, null, null, 0, 20), third, second);
        assertThat(search(null, null, null, SECOND, SECOND, null, 0, 20)).isEmpty();
    }

    @Test
    void escapedNamesAreLiteralAndTickerMatchesEitherSideExactly() {
        john.setFullName("Jo_%! Doe");
        jane.setFullName("JoABC Doe");
        Order buy = order(john, apple, usd);
        Order sell = order(john, usd, apple);
        Order deposit = order(john, usd, null);
        order(jane, apple, usd);
        reload();

        assertThat(search(null, "%jo!_!%!!%", null, null, null, null, 0, 20).getContent())
                .extracting(Order::getOrderId)
                .containsExactlyInAnyOrder(buy.getOrderId(), sell.getOrderId(), deposit.getOrderId());
        assertThat(search(john.getUserId(), null, null, null, null, "AAPL", 0, 20).getContent())
                .extracting(Order::getOrderId).containsExactlyInAnyOrder(buy.getOrderId(), sell.getOrderId());
        assertThat(search(null, null, null, null, null, "AAP", 0, 20)).isEmpty();
    }

    @Test
    void placementTiesAreSortedByOrderIdAndBulkHistoryFetchStillWorks() {
        Order first = order(john, apple, usd);
        Order second = order(john, apple, usd);
        event(first, OrderStatus.PENDING, FIRST);
        event(first, OrderStatus.FULFILLED, SECOND);
        event(second, OrderStatus.ACCEPTED, FIRST);
        reload();

        List<UUID> expected = List.of(first.getOrderId(), second.getOrderId()).stream()
                .sorted(java.util.Comparator.comparing(UUID::toString)).toList();
        assertThat(search(null, null, null, null, null, null, 0, 20).getContent())
                .extracting(Order::getOrderId).containsExactlyElementsOf(expected);
        Order hydrated = repository.findWithHistoryByOrderIdIn(List.of(first.getOrderId())).getFirst();
        assertThat(hydrated.getOrderHistory()).hasSize(2);
    }

    @Test
    void clientHistoryIncludesAllEventsButNeverAnotherClientsOrders() {
        Order first = order(john, apple, usd);
        event(first, OrderStatus.PENDING, FIRST);
        event(first, OrderStatus.PENDING, SECOND);
        Order second = order(john, usd, apple);
        event(second, OrderStatus.FULFILLED, THIRD);
        Order empty = order(john, null, null);
        Order unrelated = order(jane, apple, usd);
        event(unrelated, OrderStatus.ACCEPTED, FIRST);
        reload();

        List<Order> orders = repository.findWithHistoryByClientId(john.getUserId());
        session.clear(); // History must already be loaded, not depend on an open persistence context.

        assertThat(orders).extracting(Order::getOrderId)
                .containsExactlyInAnyOrder(first.getOrderId(), second.getOrderId(), empty.getOrderId());
        assertThat(orders.stream().flatMap(o -> o.getOrderHistory().stream()).toList()).hasSize(3);
        assertThat(repository.findWithHistoryByClientId(UUID.randomUUID())).isEmpty();
    }

    private Page<Order> search(UUID userId, String name, String status, Instant from, Instant to,
            String ticker, int page, int size) {
        return repository.searchAdminOrders(userId, name, status, from, to, ticker, PageRequest.of(page, size));
    }

    private Order order(User client, Instrument added, Instrument subtracted) {
        Order order = new Order(added == null ? null : added.getID(), subtracted == null ? null : subtracted.getID(),
                Side.BUY, null, client.getUserId(), BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, "UTC");
        session.persist(order);
        return order;
    }

    private OrderStatusChange event(Order order, OrderStatus status, Instant date) {
        OrderStatusChange event = new OrderStatusChange(status, "test event", date);
        ReflectionTestUtils.setField(event, "order", order);
        session.persist(event);
        return event;
    }

    private void eventWithId(Order order, OrderStatus status, Instant date, String id) {
        OrderStatusChange event = event(order, status, date);
        session.flush();
        // Fixed fixture UUIDs exercise the unsigned boundary; application entities generate random IDs.
        session.createNativeMutationQuery("UPDATE OrderStatusChange SET order_status_change_id = :id "
                + "WHERE order_status_change_id = :oldId")
                .setParameter("id", UUID.fromString(id)).setParameter("oldId", event.getOrderStatusChangeId())
                .executeUpdate();
        session.detach(event);
    }

    private void reload() {
        session.flush();
        session.clear();
    }

    private void assertIds(Page<Order> page, Order... expected) {
        assertThat(page.getContent()).extracting(Order::getOrderId)
                .containsExactly(java.util.Arrays.stream(expected).map(Order::getOrderId).toArray(UUID[]::new));
    }
}