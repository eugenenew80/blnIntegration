package bln.integration.gateway.oic;

import bln.integration.entity.PeriodTimeValueRaw;

import java.time.LocalDateTime;
import java.util.List;

public interface OicDataImpGateway {
    OicDataImpGateway points(List<String> points);

    OicDataImpGateway startDateTime(LocalDateTime startDateTime);

    OicDataImpGateway endDateTime(LocalDateTime endDateTime);

    OicDataImpGateway arcType(String arcType);

    List<PeriodTimeValueRaw> request() throws Exception;
}
