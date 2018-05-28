package bln.integration.gateway.oic;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import java.util.List;

public interface OicDataImpGateway {
    List<PeriodTimeValueRaw> request(ConnectionConfig config, List<LogPointCfg> points, String arcType) throws Exception;
}
