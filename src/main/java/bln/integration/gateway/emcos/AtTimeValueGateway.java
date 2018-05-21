package bln.integration.gateway.emcos;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.AtTimeValueRaw;

import java.util.List;

public interface AtTimeValueGateway {
    List<AtTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points) throws Exception;
}
