package io.quintkard.quintkardapp.message;

import org.springframework.data.jpa.domain.Specification;

public final class MessageSpecifications {

    private MessageSpecifications() {
    }

    public static Specification<Message> fromFilter(MessageFilter filter) {
        Specification<Message> specification = Specification.where(hasUserId(filter.userId()));
        specification = and(specification, hasStatus(filter.status()));
        specification = and(specification, hasSourceService(filter.sourceService()));
        specification = and(specification, hasMessageType(filter.messageType()));
        specification = and(specification, ingestedAfter(filter.ingestedAfter()));
        specification = and(specification, ingestedBefore(filter.ingestedBefore()));
        return specification;
    }

    private static Specification<Message> and(Specification<Message> left, Specification<Message> right) {
        return right == null ? left : left.and(right);
    }

    private static Specification<Message> hasUserId(String userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.join("user").get("userId"), userId);
    }

    private static Specification<Message> hasStatus(MessageStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    private static Specification<Message> hasSourceService(String sourceService) {
        if (sourceService == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("sourceService"), sourceService);
    }

    private static Specification<Message> hasMessageType(String messageType) {
        if (messageType == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("messageType"), messageType);
    }

    private static Specification<Message> ingestedAfter(java.time.Instant ingestedAfter) {
        if (ingestedAfter == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("ingestedAt"), ingestedAfter);
    }

    private static Specification<Message> ingestedBefore(java.time.Instant ingestedBefore) {
        if (ingestedBefore == null) {
            return null;
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("ingestedAt"), ingestedBefore);
    }
}
