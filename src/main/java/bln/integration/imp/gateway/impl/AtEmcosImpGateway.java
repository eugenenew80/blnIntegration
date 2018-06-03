package bln.integration.imp.gateway.impl;

import bln.integration.entity.*;
import bln.integration.entity.enums.*;
import bln.integration.http.HttpGatewayImpl;
import bln.integration.imp.gateway.MeteringPointCfg;
import bln.integration.imp.gateway.ValueGateway;
import bln.integration.registry.*;
import lombok.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import java.net.URL;
import static java.util.Collections.emptyList;

@Service
@RequiredArgsConstructor
public class AtEmcosImpGateway implements ValueGateway<AtTimeValueRaw> {
    private final TemplateRegistry templateRegistry;
    private static final Logger logger = LoggerFactory.getLogger(AtEmcosImpGateway.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:'00000'");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<AtTimeValueRaw> request(ConnectionConfig config, List<MeteringPointCfg> points) throws Exception {
        logger.info("request started");

        List<AtTimeValueRaw> list;
        try {
            byte[] body = buildBody(config, points);
            if (body==null || body.length==0) {
                logger.info("Request body is empty, request terminated");
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
                    l.setParamId(point.getParamId());
                }
            });

            logger.info("request successfully completed");
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
            .map( p-> buildNode(p))
            .filter(p -> StringUtils.isNotEmpty(p))
            .collect(Collectors.joining());
        logger.trace("points: " + strPoints);

        if (StringUtils.isEmpty(strPoints)) {
            logger.debug("List of points is empty, AtTimeValueGatewayImpl.buildBody stopped");
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

        logger.trace("body part 1 for request metering aData: " + body1);
        logger.trace("body part 2 for request metering aData: " + body2);

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

    private String buildNode(MeteringPointCfg point) {
        return ""
            + "<ROW PPOINT_CODE=\"" + point.getSourceMeteringPointCode() + "\" "
            + "PML_ID=\"" + point.getSourceParamCode() + "\" "
            + "PBT=\"" + point.getStartTime().format(timeFormatter) + "\" "
            + "PET=\"" + point.getEndTime().format(timeFormatter) + "\" />";
    }

    private List<AtTimeValueRaw> parseAnswer(String answer) throws Exception {
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

        List<AtTimeValueRaw> list = new ArrayList<>();
        for(int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName() == "ROWDATA") {
                NodeList rowData = nodes.item(i).getChildNodes();
                for(int j = 0; j < rowData.getLength(); j++) {
                    if (rowData.item(j).getNodeName() == "ROW") {
                    	logger.debug("row: " + (j+1));
                        AtTimeValueRaw node = parseNode(rowData.item(j));
                        if (node!=null)
                            list.add(node);
                    }
                }
            }
        }
        logger.debug("convert xml to list completed");

        logger.info("parseAnswer completed, count of rows: " + list.size());
        return list;
    }

    private AtTimeValueRaw parseNode(Node node) {
        NamedNodeMap attributes = node.getAttributes();

        String externalCode = attributes
            .getNamedItem("PPOINT_CODE")
            .getNodeValue() ;

        String sourceParamCode = attributes
            .getNamedItem("PML_ID")
            .getNodeValue() ;
        
        LocalDate date = null;
        String dateStr = attributes
            .getNamedItem("PBT")
            .getNodeValue();
        dateStr = dateStr.substring(0, 8);

        try {
            if (dateStr != null)
                date = LocalDate.parse(dateStr, dateFormatter);
        }
        catch (Exception e) {
            logger.error("parse date error :  " + e.getMessage());
            logger.error("dateStr = " + dateStr);
            logger.error("sourceParamCode = " + sourceParamCode);
            logger.error("externalCode = " + externalCode);
        }
        if (date==null)
            return null;

        Double val = null;
        String valStr = attributes
            .getNamedItem("PVAL")
            .getNodeValue();

        if (valStr!=null)
            val = Double.parseDouble(valStr);

        AtTimeValueRaw value = new AtTimeValueRaw();
        value.setSourceMeteringPointCode(externalCode);
        value.setMeteringDate(date.atStartOfDay());
        value.setSourceSystemCode(SourceSystemEnum.EMCOS);
        value.setStatus(ProcessingStatusEnum.TMP);
        value.setInputMethod(InputMethodEnum.AUTO);
        value.setReceivingMethod(ReceivingMethodEnum.SERVICE);
        value.setSourceParamCode(sourceParamCode);
        value.setVal(val);

        return value;
    }
}
