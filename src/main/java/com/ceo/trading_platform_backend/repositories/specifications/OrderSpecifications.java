package com.ceo.trading_platform_backend.repositories.specifications;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.ceo.trading_platform_backend.enums.OrderStatus;
import com.ceo.trading_platform_backend.models.Instrument;
import com.ceo.trading_platform_backend.models.Order;
import com.ceo.trading_platform_backend.models.OrderStatusChange;
import com.ceo.trading_platform_backend.models.User;

import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/** Database-side admin filters. Null arguments mean no restriction, including for legacy orders. */
public final class OrderSpecifications {
    private OrderSpecifications() {}

    public static Specification<Order> hasClientId(UUID clientId) {
        return (order, query, cb) -> clientId == null
                ? cb.conjunction() : cb.equal(order.get("clientId"), clientId);
    }

    /** The service supplies a lowercase LIKE pattern with literal %, _ and ! escaped using !. */
    public static Specification<Order> clientNameMatches(String escapedNamePattern) {
        return (order, query, cb) -> {
            if (escapedNamePattern == null) {
                return cb.conjunction();
            }
            Subquery<Integer> matchingClient = query.subquery(Integer.class);
            Root<User> client = matchingClient.from(User.class);
            matchingClient.select(cb.literal(1)).where(
                    cb.equal(client.get("userId"), order.get("clientId")),
                    cb.like(cb.lower(client.get("fullName")), escapedNamePattern, '!'));
            return cb.exists(matchingClient);
        };
    }

    /** Match the latest event, not any historical occurrence of the requested status. */
    public static Specification<Order> hasCurrentStatus(OrderStatus status) {
        return (order, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            Subquery<Integer> matchingLatest = query.subquery(Integer.class);
            Root<OrderStatusChange> candidate = matchingLatest.from(OrderStatusChange.class);

            // A candidate is latest only if no event (of ANY status) sorts after it.
            Subquery<Integer> laterEvent = matchingLatest.subquery(Integer.class);
            Root<OrderStatusChange> other = laterEvent.from(OrderStatusChange.class);
            laterEvent.select(cb.literal(1)).where(
                    cb.equal(other.get("order"), candidate.get("order")),
                    sortsAfter(other, candidate, cb));

            matchingLatest.select(cb.literal(1)).where(
                    cb.equal(candidate.get("order").get("orderId"), order.get("orderId")),
                    cb.equal(candidate.get("status"), status),
                    cb.not(cb.exists(laterEvent)));
            return cb.exists(matchingLatest);
        };
    }

    /** Earliest non-null history timestamp, using [fromUtc, toExclusiveUtc). */
    public static Specification<Order> placedBetween(Instant fromUtc, Instant toExclusiveUtc) {
        return (order, query, cb) -> {
            if (fromUtc == null && toExclusiveUtc == null) {
                return cb.conjunction();
            }
            Subquery<Instant> placed = placementTime(order, query, cb);
            Predicate from = fromUtc == null ? cb.conjunction() : cb.greaterThanOrEqualTo(placed, fromUtc);
            Predicate to = toExclusiveUtc == null ? cb.conjunction() : cb.lessThan(placed, toExclusiveUtc);
            return cb.and(from, to);
        };
    }

    /** The service supplies an uppercase exact symbol; either side of the order may match. */
    public static Specification<Order> hasTicker(String ticker) {
        return (order, query, cb) -> {
            if (ticker == null) {
                return cb.conjunction();
            }
            Subquery<Integer> matchingInstrument = query.subquery(Integer.class);
            Root<Instrument> instrument = matchingInstrument.from(Instrument.class);
            matchingInstrument.select(cb.literal(1)).where(
                    cb.or(cb.equal(instrument.get("ID"), order.get("addInstrumentId")),
                            cb.equal(instrument.get("ID"), order.get("subtractInstrumentId"))),
                    cb.equal(cb.upper(instrument.get("symbol")), ticker));
            return cb.exists(matchingInstrument);
        };
    }

    /** Hibernate supports the correlated subquery sort; unknown dates stay last on every page. */
    public static Specification<Order> newestPlacementFirst() {
        return (order, query, cb) -> {
            // Counts need only predicates, never sorting or collection fetches.
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                Subquery<Instant> placed = placementTime(order, query, cb);
                query.orderBy(
                        cb.asc(cb.<Integer>selectCase().when(cb.isNull(placed), 1).otherwise(0)),
                        cb.desc(placed),
                        cb.asc(order.get("orderId")));
            }
            return cb.conjunction();
        };
    }

    private static Subquery<Instant> placementTime(Root<Order> order, CommonAbstractCriteria query,
            CriteriaBuilder cb) {
        Subquery<Instant> placed = query.subquery(Instant.class);
        Root<OrderStatusChange> history = placed.from(OrderStatusChange.class);
        placed.select(cb.least(history.<Instant>get("date"))).where(
                cb.equal(history.get("order").get("orderId"), order.get("orderId")));
        return placed;
    }

    /** Equivalent to change_date DESC NULLS LAST, order_status_change_id DESC. */
    private static Predicate sortsAfter(Root<OrderStatusChange> other, Root<OrderStatusChange> candidate,
            CriteriaBuilder cb) {
        Path<Instant> otherDate = other.get("date");
        Path<Instant> candidateDate = candidate.get("date");
        Predicate datedAfterUndated = cb.and(cb.isNotNull(otherDate), cb.isNull(candidateDate));
        Predicate laterDate = cb.greaterThan(otherDate, candidateDate);
        Predicate equalDates = cb.or(cb.equal(otherDate, candidateDate),
                cb.and(cb.isNull(otherDate), cb.isNull(candidateDate)));
        // Compare UUIDs in the database, preserving PostgreSQL's native unsigned UUID ordering.
        Predicate higherId = cb.greaterThan(other.<UUID>get("orderStatusChangeId"),
                candidate.<UUID>get("orderStatusChangeId"));
        return cb.or(datedAfterUndated, laterDate, cb.and(equalDates, higherId));
    }
}