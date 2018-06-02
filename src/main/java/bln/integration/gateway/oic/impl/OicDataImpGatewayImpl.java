package bln.integration.gateway.oic.impl;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.InputMethodEnum;
import bln.integration.entity.enums.ProcessingStatusEnum;
import bln.integration.entity.enums.ReceivingMethodEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.oic.LogPointCfg;
import bln.integration.gateway.oic.OicDataImpGateway;
import bln.integration.gateway.oic.TelemetryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
public class OicDataImpGatewayImpl implements OicDataImpGateway {
    private static final Logger logger = LoggerFactory.getLogger(OicDataImpGatewayImpl.class);

    @Override
    public List<PeriodTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points, Integer interval)  {
        logger.info("request started");

        if (config == null) {
            logger.warn("Config is empty, request terminated");
            return emptyList();
        }

        if (points == null || points.isEmpty()) {
            logger.warn("List of points is empty, request terminated");
            return emptyList();
        }

        String arcType = "";
        if (interval == 3600)
            arcType = "MIN-60";
        else if (interval == 900)
            arcType = "MIN-15";
        else if (interval == 180)
            arcType = "MIN-3";

        List<LogPointCfg> logPoints = points.stream()
            .map(p -> {
                LogPointCfg lpc = new LogPointCfg();
                lpc.setLogPointId(Long.parseLong(p.getSourceMeteringPointCode()));
                lpc.setStart(p.getStartTime());
                lpc.setEnd(p.getEndTime());
                return lpc;
            })
            .collect(toList());

        List<PeriodTimeValueRaw> list;
        try {
            RestTemplate restTemplate = new RestTemplateBuilder().build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            String url = config.getUrl() + "/" + arcType;
            HttpEntity<List<LogPointCfg>> requestEntity = new HttpEntity<>(logPoints, headers);

            ResponseEntity<List<TelemetryDto>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<List<TelemetryDto>>() {}
            );

            list = mapToValue(responseEntity.getBody(), interval, points);
            logger.info("parseAnswer completed, count of rows: " + list.size());
            logger.info("request successfully completed");
        }

        catch (Exception e) {
            logger.error("request failed: " + e.toString());
            throw e;
        }

        return list;
    }

    private List<PeriodTimeValueRaw> mapToValue(List<TelemetryDto> telemetryList, Integer interval, List<MeteringPointCfg> points) {
        return telemetryList.stream()
            .map(t -> {
                PeriodTimeValueRaw pt = new PeriodTimeValueRaw();
                pt.setInterval(interval);
                pt.setSourceMeteringPointCode(t.getLogPointId().toString());
                pt.setMeteringDate(t.getDateTime());
                pt.setVal(t.getVal());
                pt.setSourceSystemCode(SourceSystemEnum.OIC);
                pt.setStatus(ProcessingStatusEnum.TMP);
                pt.setInputMethod(InputMethodEnum.AUTO);
                pt.setReceivingMethod(ReceivingMethodEnum.SERVICE);

                MeteringPointCfg pointCfg = points.stream()
                    .filter(p -> p.getSourceMeteringPointCode().equals(t.getLogPointId().toString()))
                    .findFirst()
                    .orElse(null);

                if (pointCfg!=null) {
                    pt.setSourceUnitCode(pointCfg.getSourceUnitCode());
                    pt.setSourceParamCode(pointCfg.getSourceParamCode());
                    pt.setMeteringPointId(pointCfg.getMeteringPointId());
                    pt.setParamId(pointCfg.getParamId());
                }

                return pt;
            })
            .collect(toList());
    }
}
