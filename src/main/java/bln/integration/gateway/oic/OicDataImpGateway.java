package bln.integration.gateway.oic;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.gateway.emcos.MeteringPointCfg;
import java.util.List;

public interface OicDataImpGateway {
    List<PeriodTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points, Integer interval);
}
