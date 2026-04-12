package io.quintkard.quintkardapp.card;

import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> fromFilter(CardFilter filter) {
        Specification<Card> specification = Specification.where(hasUserId(filter.userId()));
        specification = and(specification, hasStatus(filter.status()));
        specification = and(specification, hasCardType(filter.cardType()));
        specification = and(specification, updatedAfter(filter.updatedAfter()));
        specification = and(specification, updatedBefore(filter.updatedBefore()));
        return specification;
    }

    private static Specification<Card> and(Specification<Card> left, Specification<Card> right) {
        return right == null ? left : left.and(right);
    }

    private static Specification<Card> hasUserId(String userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.join("user").get("userId"), userId);
    }

    private static Specification<Card> hasStatus(CardStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<Card> hasCardType(CardType cardType) {
        if (cardType == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("cardType"), cardType);
    }

    private static Specification<Card> updatedAfter(java.time.Instant updatedAfter) {
        if (updatedAfter == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), updatedAfter);
    }

    private static Specification<Card> updatedBefore(java.time.Instant updatedBefore) {
        if (updatedBefore == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), updatedBefore);
    }
}
