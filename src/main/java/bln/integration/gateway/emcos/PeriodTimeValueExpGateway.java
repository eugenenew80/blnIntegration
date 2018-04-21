package bln.integration.gateway.emcos;

import bln.integration.entity.ConnectionConfig;

import java.util.List;

public interface PeriodTimeValueExpGateway {
    PeriodTimeValueExpGateway config(ConnectionConfig config);
    PeriodTimeValueExpGateway points(List<MeteringPointCfg> points);
    void send() throws Exception;
}
