package bln.integration.gateway.emcos;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.AtTimeValueRaw;

import java.util.List;

public interface AtTimeValueGateway {
    AtTimeValueGateway config(ConnectionConfig config);

    AtTimeValueGateway points(List<MeteringPointCfg> points);

    List<AtTimeValueRaw> request() throws Exception;
}
