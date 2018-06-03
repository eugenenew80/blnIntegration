package bln.integration.exp.gateway;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.ExportData;
import java.util.List;
import java.util.Map;

public interface FtpGateway {
    FtpGateway exportData(Map<String, List<ExportData>> exportData);
    FtpGateway path(String path);
    FtpGateway fileName(String fileName);
    FtpGateway config(ConnectionConfig config);
    void send() throws Exception;
}
