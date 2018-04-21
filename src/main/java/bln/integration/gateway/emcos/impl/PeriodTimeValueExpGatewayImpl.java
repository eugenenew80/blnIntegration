package bln.integration.gateway.emcos.impl;

import bln.integration.entity.ConnectionConfig;
import bln.integration.gateway.emcos.MeteringPointCfg;
import bln.integration.gateway.emcos.PeriodTimeValueExpGateway;
import bln.integration.registry.TemplateRegistry;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PeriodTimeValueExpGatewayImpl implements PeriodTimeValueExpGateway {
    private static final Logger logger = LoggerFactory.getLogger(PeriodTimeValueExpGatewayImpl.class);
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:'00000'");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private List<MeteringPointCfg> points;
    private ConnectionConfig config;

    public PeriodTimeValueExpGateway points(List<MeteringPointCfg> points) {
        this.points = points;
        return this;
    }

    public PeriodTimeValueExpGateway config(ConnectionConfig config) {
        this.config = config;
        return this;
    }

    public void send() throws Exception {
        logger.info("send started");

        if (config ==null) {
            logger.warn("Config is empty, send stopped");
            return;
        }

        if (points ==null || points.isEmpty()) {
            logger.warn("List of points is empty, send stopped");
            return;
        }

        try {
            logger.info("Send http request for metering media...");
            byte[] body = buildBody();
            if (body==null || body.length==0) {
            	logger.info("Request body is empty, send stopped");
                return;
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


            try (PrintWriter out = new PrintWriter("answer.xml")) {
                out.println(answer);
            }
            catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            logger.info("send competed");
        }

        catch (Exception e) {
            logger.error("send failed: " + e.toString());
            throw e;
        }
    }

    private byte[] buildBody() {
    	logger.debug("buildBody started");

    	String strPoints = points.stream()
            .map( p-> buildPoint(p))
            .filter(p -> StringUtils.isNotEmpty(p))
            .collect(Collectors.joining());
        logger.trace("points: " + strPoints);

        if (StringUtils.isEmpty(strPoints)) {
        	logger.debug("List of points is empty, buildBody stopped");
            return new byte[0];
        }

        String data = templateRegistry.getTemplate("EMCOS_EEML_DATA")
        	.replace("#points#", strPoints);
        logger.trace("media: " + data);

        String property = templateRegistry.getTemplate("EMCOS_EEML_PROPERTY")
        	.replace("#user#", config.getUserName())
        	.replace("#isPacked#", "false")
        	.replace("#func#", "EEML")
        	.replace("#attType#", "1");
        logger.trace("property: " + property);

        String body = templateRegistry.getTemplate("EMCOS_EEML_BODY")
        	.replace("#property#", property)
        	.replace("#media#", Base64.encodeBase64String(data.getBytes()));
        logger.trace("body for request metering media: " + body);


        try (PrintWriter out = new PrintWriter("body.xml")) {
            out.println(body);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try (PrintWriter out = new PrintWriter("data.xml")) {
            out.println(data);
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        logger.debug("buildBody completed");
        return body.getBytes();
    }

    private String buildPoint(MeteringPointCfg point) {
        logger.info("buildPoint started");

        String str = ""
                + "<ROW PDA=\"" + point.getStartTime().toLocalDate().format(dateFormatter) + "\" "
                + "PBT=\"" + point.getStartTime().format(timeFormatter) + "\" "
                + "PET=\"" + point.getEndTime().format(timeFormatter) + "\" "
                + "PCODE=\"" + point.getSourceMeteringPointCode() + "\" "
                + "PML_ID=\"" + point.getSourceParamCode() + "\" "
                + "PVAL=\"" +  point.getVal() + "\" />";

        return str;
    }


    @Autowired
    private TemplateRegistry templateRegistry;
}
