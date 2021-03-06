package de.rwth.idsg.steve.repository.dto;

import lombok.Builder;
import lombok.Getter;

/**
 *
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 *
 */
@Getter
@Builder
public final class Reservation {
    private final int id, ocppTagPk, chargeBoxPk;
    private final int connectorId;
    private final Integer transactionId;
    private final String ocppIdTag, chargeBoxId, startDatetime, expiryDatetime, status;
}
