package io.quintkard.quintkardapp.message;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class MessageSpecificationsTest {

    @Test
    void fromFilterAppliesAllConfiguredPredicates() {
        @SuppressWarnings("unchecked")
        Root<Message> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        Join<Object, Object> userJoin = mock(Join.class);
        @SuppressWarnings("unchecked")
        Path<Object> userIdPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> statusPath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> sourceServicePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> messageTypePath = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> ingestedAtPath = mock(Path.class);

        Predicate userPredicate = mock(Predicate.class);
        Predicate statusPredicate = mock(Predicate.class);
        Predicate sourceServicePredicate = mock(Predicate.class);
        Predicate messageTypePredicate = mock(Predicate.class);
        Predicate ingestedAfterPredicate = mock(Predicate.class);
        Predicate ingestedBeforePredicate = mock(Predicate.class);
        when(root.join("user")).thenReturn(userJoin);
        when(userJoin.get("userId")).thenReturn(userIdPath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("sourceService")).thenReturn(sourceServicePath);
        when(root.get("messageType")).thenReturn(messageTypePath);
        when(root.get("ingestedAt")).thenReturn(ingestedAtPath);

        when(criteriaBuilder.equal(userIdPath, "admin")).thenReturn(userPredicate);
        when(criteriaBuilder.equal(statusPath, MessageStatus.PENDING)).thenReturn(statusPredicate);
        when(criteriaBuilder.equal(sourceServicePath, "gmail")).thenReturn(sourceServicePredicate);
        when(criteriaBuilder.equal(messageTypePath, "EMAIL")).thenReturn(messageTypePredicate);
        when(criteriaBuilder.greaterThanOrEqualTo((Path) ingestedAtPath, Instant.parse("2026-04-05T00:00:00Z")))
                .thenReturn(ingestedAfterPredicate);
        when(criteriaBuilder.lessThanOrEqualTo((Path) ingestedAtPath, Instant.parse("2026-04-06T00:00:00Z")))
                .thenReturn(ingestedBeforePredicate);

        Specification<Message> specification = MessageSpecifications.fromFilter(
                new MessageFilter(
                        "admin",
                        "invoice",
                        MessageStatus.PENDING,
                        "gmail",
                        "EMAIL",
                        Instant.parse("2026-04-05T00:00:00Z"),
                        Instant.parse("2026-04-06T00:00:00Z")
                )
        );

        specification.toPredicate(root, query, criteriaBuilder);

        verify(criteriaBuilder).equal(userIdPath, "admin");
        verify(criteriaBuilder).equal(statusPath, MessageStatus.PENDING);
        verify(criteriaBuilder).equal(sourceServicePath, "gmail");
        verify(criteriaBuilder).equal(messageTypePath, "EMAIL");
        verify(criteriaBuilder).greaterThanOrEqualTo((Path) ingestedAtPath, Instant.parse("2026-04-05T00:00:00Z"));
        verify(criteriaBuilder).lessThanOrEqualTo((Path) ingestedAtPath, Instant.parse("2026-04-06T00:00:00Z"));
    }

    @Test
    void fromFilterWithOnlyRequiredUserPredicateSkipsOptionalFilters() {
        @SuppressWarnings("unchecked")
        Root<Message> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        @SuppressWarnings("unchecked")
        Join<Object, Object> userJoin = mock(Join.class);
        @SuppressWarnings("unchecked")
        Path<Object> userIdPath = mock(Path.class);
        Predicate userPredicate = mock(Predicate.class);

        when(root.join("user")).thenReturn(userJoin);
        when(userJoin.get("userId")).thenReturn(userIdPath);
        when(criteriaBuilder.equal(userIdPath, "admin")).thenReturn(userPredicate);

        Specification<Message> specification = MessageSpecifications.fromFilter(
                new MessageFilter("admin", null, null, null, null, null, null)
        );

        Predicate predicate = specification.toPredicate(root, query, criteriaBuilder);

        assertSame(userPredicate, predicate);
        verify(root).join("user");
    }
}
