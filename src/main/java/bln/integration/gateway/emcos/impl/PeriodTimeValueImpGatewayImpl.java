package bln.integration.gateway.emcos.impl;

import bln.integration.entity.ConnectionConfig;
import bln.integration.entity.PeriodTimeValueRaw;
import bln.integration.entity.enums.InputMethodEnum;
import bln.integration.entity.enums.ProcessingStatusEnum;
import bln.integration.entity.enums.ReceivingMethodEnum;
import bln.integration.entity.enums.SourceSystemEnum;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueImpGateway;
import bln.integration.registry.TemplateRegistry;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;

@Service
public class PeriodTimeValueImpGatewayImpl implements PeriodTimeValueImpGateway {
    private static final Logger logger = LoggerFactory.getLogger(PeriodTimeValueImpGatewayImpl.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:'00000'");

    @Override
    public List<PeriodTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points) throws Exception {
        logger.info("request started");

        if (config == null) {
            logger.warn("Config is empty, request stopped");
            return emptyList();
        }

        if (points == null || points.isEmpty()) {
            logger.warn("List of points is empty, request stopped");
            return emptyList();
        }

        List<PeriodTimeValueRaw> list;
        try {
            logger.info("Send http request for metering media...");
            byte[] body = buildBody(config, points);
            if (body==null || body.length==0) {
            	logger.info("Request body is empty, request stopped");
                return emptyList();
            }

            byte[] byteAnswer = new HttpGatewayImpl.Builder()
                .url(new URL(config.getUrl()))
                .method("POST")
                .body(body)
                .build()
                .doRequest();

            String answer = new String(byteAnswer, "UTF-8");
            int n1 = answer.indexOf("<AnswerData>");
            int n2 = answer.indexOf("</AnswerData>");
            if (n2>n1)
                answer = answer.substring(n1+12, n2);

            list = parseAnswer(answer);
            list.stream().forEach(l -> {
                MeteringPointCfg point = points.stream()
                    .filter(p -> p.getSourceMeteringPointCode().equals(l.getSourceMeteringPointCode()))
                    .filter(p -> p.getSourceParamCode().equals(l.getSourceParamCode()))
                    .findFirst()
                    .orElse(null);

                if (point!=null) {
                    l.setMeteringPointId(point.getMeteringPointId());
                    l.setSourceUnitCode(point.getSourceUnitCode());
                    l.setInterval(point.getInterval());
                }
            });

            logger.info("request competed");
        }

        catch (Exception e) {
            logger.error("request failed: " + e.toString());
            throw e;
        }

        return list;
    }

    private byte[] buildBody(ConnectionConfig config, List<MeteringPointCfg> points) {
    	logger.debug("buildBody started");

    	String strPoints = points.stream()
            .filter(p -> !p.getStartTime().isAfter(p.getEndTime()))
            .map( p-> buildPoint(p))
            .filter(p -> StringUtils.isNotEmpty(p))
            .collect(Collectors.joining());
        logger.trace("points: " + strPoints);

        if (StringUtils.isEmpty(strPoints)) {
        	logger.debug("List of points is empty, buildBody stopped");
            return new byte[0];
        }

        String aData = templateRegistry.getTemplate("EMCOS_REQML_DATA")
        	.replace("#points#", strPoints);
        logger.trace("media: " + aData);

        String property = templateRegistry.getTemplate("EMCOS_REQML_PROPERTY")
        	.replace("#user#", config.getUserName())
        	.replace("#isPacked#", "false")
        	.replace("#func#", "REQML")
        	.replace("#attType#", "1");
        logger.trace("property: " + property);

        String body1 = templateRegistry.getTemplate("EMCOS_REQML_BODY_1")
        	.replace("#property#", property);

        String body2 = templateRegistry.getTemplate("EMCOS_REQML_BODY_2")
        	.replace("#property#", property);

        logger.trace("body part 1 for request metering data: " + body1);
        logger.trace("body part 2 for request metering data: " + body2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //noinspection Duplicates
        try {
            baos.write(body1.getBytes());
            baos.write(Base64.encodeBase64(aData.getBytes()));
            baos.write(body2.getBytes());
            baos.flush();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            try { baos.close(); }
            catch (IOException e) { }
        }

        logger.debug("buildBody completed");
        return baos.toByteArray();
    }

    private String buildPoint(MeteringPointCfg point) {
        return ""
            + "<ROW PPOINT_CODE=\"" + point.getSourceMeteringPointCode() + "\" "
            + "PML_ID=\"" + point.getSourceParamCode() + "\" "
            + "PBT=\"" + point.getStartTime().format(timeFormatter) + "\" "
            + "PET=\"" + point.getEndTime().format(timeFormatter) + "\" />";
}

    private List<PeriodTimeValueRaw> parseAnswer(String answer) throws Exception {
    	logger.info("parseAnswer started");
        logger.trace("answer: " + new String(Base64.decodeBase64(answer), "Cp1251"));

        logger.debug("parsing xml started");
        Document doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new InputSource(new StringReader( new String(Base64.decodeBase64(answer), "Cp1251") )));
        logger.debug("parsing xml completed");
        
        
        logger.debug("convert xml to list started");
        NodeList nodes =  doc.getDocumentElement().getParentNode()
            .getFirstChild()
            .getChildNodes();

        List<PeriodTimeValueRaw> list = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName() == "ROWDATA") {
                NodeList rowData = nodes.item(i).getChildNodes();
                for(int j = 0; j < rowData.getLength(); j++) {
                    if (rowData.item(j).getNodeName() == "ROW") {
                    	logger.debug("row: " + (j+1));
                        list.add(parseNode(rowData.item(j)));
                    }
                }
            }
        }
        logger.debug("convert xml to list completed");

        logger.info("parseAnswer completed, count of rows: " + list.size());
        return list;
    }

    private PeriodTimeValueRaw parseNode(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        String externalCode = attributes
            .getNamedItem("PPOINT_CODE")
            .getNodeValue() ;

        String sourceParamCode = attributes
            .getNamedItem("PML_ID")
            .getNodeValue() ;
        
        LocalDateTime time = null;
        String timeStr = attributes
            .getNamedItem("PBT")
            .getNodeValue() ;

        if (timeStr!=null) {
            if (timeStr.indexOf("T")<0) timeStr = timeStr+"T00:00:00000";
            time = LocalDateTime.parse(timeStr, timeFormatter);
        }

        Double val = null;
        String valStr = attributes
            .getNamedItem("PVAL")
            .getNodeValue();

        if (valStr!=null)
            val = Double.parseDouble(valStr);

        PeriodTimeValueRaw value = new PeriodTimeValueRaw();
        value.setSourceMeteringPointCode(externalCode);
        value.setMeteringDate(time);
        value.setSourceSystemCode(SourceSystemEnum.EMCOS);
        value.setStatus(ProcessingStatusEnum.TMP);
        value.setInputMethod(InputMethodEnum.AUTO);
        value.setReceivingMethod(ReceivingMethodEnum.SERVICE);
        value.setSourceParamCode(sourceParamCode);
        value.setVal(val);
        
        return value;
    }

    @Autowired
    private TemplateRegistry templateRegistry;
}
