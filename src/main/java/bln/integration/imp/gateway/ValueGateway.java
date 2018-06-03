package bln.integration.imp.gateway;

import bln.integration.entity.ConnectionConfig;

import java.util.List;

public interface ValueGateway<T> {
    List<T> request(ConnectionConfig config, List<MeteringPointCfg> points) throws Exception;
}
