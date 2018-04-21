package bln.integration.gateway.emcos;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import java.util.List;

public interface PeriodTimeValueImpGateway {
    PeriodTimeValueImpGateway config(ConnectionConfig config);

    PeriodTimeValueImpGateway points(List<MeteringPointCfg> points);

    List<PeriodTimeValueRaw> request() throws Exception;
}
