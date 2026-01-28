package io.github.ddmfuhrmann.ledgerpoc.application.event;

public enum OutboxEventType {

    CASH_IN_REQUESTED(AggregateType.PAYMENT),
    CASH_IN_CONFIRMED(AggregateType.PAYMENT),
    CASH_IN_FAILED(AggregateType.PAYMENT),

    CASH_OUT_REQUESTED(AggregateType.PAYMENT),
    CASH_OUT_CONFIRMED(AggregateType.PAYMENT),
    CASH_OUT_FAILED(AggregateType.PAYMENT);

    private final AggregateType aggregateType;

    OutboxEventType(AggregateType aggregateType) {
        this.aggregateType = aggregateType;
    }

    public AggregateType aggregateType() {
        return aggregateType;
    }

    public OutboxEvent toEvent(Long aggregateId, String payloadJson) {
        return new OutboxEvent(
                aggregateType.name(),
                aggregateId,
                name(),
                payloadJson
        );
    }
}