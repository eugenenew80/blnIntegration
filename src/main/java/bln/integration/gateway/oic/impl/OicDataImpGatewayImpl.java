package bln.integration.gateway.oic.impl;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.TelemetryRaw;
import bln.integration.entity.enums.InputMethodEnum;
import bln.integration.entity.enums.ProcessingStatusEnum;
import bln.integration.entity.enums.ReceivingMethodEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.oic.OicDataImpGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class OicDataImpGatewayImpl implements OicDataImpGateway {
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Logger logger = LoggerFactory.getLogger(OicDataImpGatewayImpl.class);

    @Override
    public List<PeriodTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points, String arcType) throws Exception {
        Client client = ClientBuilder.newClient();
        WebTarget webTarget = client.target(config.getUrl());
        WebTarget telemetryWebTarget = webTarget.path("/" + arcType);

        List<TelemetryRaw> response = telemetryWebTarget.request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(points, MediaType.APPLICATION_JSON), new GenericType<List<TelemetryRaw>>(){});

        return mapToValue(response, arcType);
    }

    private List<PeriodTimeValueRaw> mapToValue(List<TelemetryRaw> telemetryList, String arcType) {
        Integer interval = null;
        if (arcType.equals("MIN-60"))
            interval=3600;
        else if (arcType.equals("MIN-15"))
            interval = 900;
        else if (arcType.equals("MIN-3"))
            interval = 180;

        Integer finalInterval = interval;
        return telemetryList.stream()
            .map(t -> {
                PeriodTimeValueRaw pt = new PeriodTimeValueRaw();
                pt.setInterval(finalInterval);
                pt.setSourceParamCode(t.getParamCode());
                pt.setSourceMeteringPointCode(t.getLogPoint().toString());
                pt.setSourceUnitCode(t.getUnitCode());
                pt.setMeteringDate(LocalDateTime.parse(t.getDateTime(), timeFormatter));
                pt.setVal(t.getVal());
                pt.setSourceSystemCode(SourceSystemEnum.OIC);
                pt.setStatus(ProcessingStatusEnum.TMP);
                pt.setInputMethod(InputMethodEnum.AUTO);
                pt.setReceivingMethod(ReceivingMethodEnum.SERVICE);
                return pt;
            })
            .collect(toList());
    }
}
