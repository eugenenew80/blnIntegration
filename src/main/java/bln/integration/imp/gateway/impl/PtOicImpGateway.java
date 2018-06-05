package bln.integration.imp.gateway.impl;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.InputMethodEnum;
import bln.integration.entity.enums.ProcessingStatusEnum;
import bln.integration.entity.enums.ReceivingMethodEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.imp.gateway.LogPointCfg;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.imp.gateway.TelemetryDto;
import bln.integration.imp.gateway.ValueGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.List;
import static java.util.stream.Collectors.toList;

@Service
public class PtOicImpGateway implements ValueGateway<PeriodTimeValueRaw> {
    private static final Logger logger = LoggerFactory.getLogger(PtOicImpGateway.class);

    @Override
    public List<PeriodTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points)  {
        logger.info("request started");

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

            String url = config.getUrl();
            HttpEntity<List<LogPointCfg>> requestEntity = new HttpEntity<>(logPoints, headers);

            ResponseEntity<List<TelemetryDto>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<List<TelemetryDto>>() {}
            );

            list = mapToValue(responseEntity.getBody(), points, config);
            logger.info("parseAnswer completed, count of rows: " + list.size());
            logger.info("request successfully completed");
        }

        catch (Exception e) {
            logger.error("request failed: " + e.toString());
            throw e;
        }

        return list;
    }

    private List<PeriodTimeValueRaw> mapToValue(List<TelemetryDto> telemetryList, List<MeteringPointCfg> points, ConnectionConfig config) {
        int offset = tzOffset(config.getTimeZone())-1;

        return telemetryList.stream()
            .map(t -> {
                PeriodTimeValueRaw pt = new PeriodTimeValueRaw();
                pt.setSourceMeteringPointCode(t.getLogPointId().toString());
                pt.setMeteringDate(t.getDateTime().plusHours(offset));
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
                    pt.setInterval(pointCfg.getInterval());
                }

                return pt;
            })
            .collect(toList());
    }

    public int tzOffset(String timezone) {
        if (timezone.startsWith("UTC+1"))
            return 0;

        if (timezone.startsWith("UTC+2"))
            return -1;

        if (timezone.startsWith("UTC+3"))
            return -2;

        if (timezone.startsWith("UTC+4"))
            return -3;

        if (timezone.startsWith("UTC+5"))
            return -4;

        if (timezone.startsWith("UTC+6"))
            return -5;

        if (timezone.startsWith("UTC+7"))
            return -6;

        return 0;
    }
}
