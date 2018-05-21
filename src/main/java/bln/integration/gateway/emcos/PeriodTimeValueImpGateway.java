package bln.integration.gateway.emcos;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import java.util.List;

public interface PeriodTimeValueImpGateway {
    List<PeriodTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points) throws Exception;
}
