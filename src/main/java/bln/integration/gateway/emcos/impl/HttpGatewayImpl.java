package bln.integration.gateway.emcos.impl;

import bln.integration.gateway.emcos.HttpGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpGatewayImpl implements HttpGateway {
	private static Logger logger = LoggerFactory.getLogger(HttpGatewayImpl.class);
	private final URL url;
	private final String method;
	private final byte[] body;
	
	private HttpGatewayImpl(Builder builder) {
		this.url = builder.url;
		this.method = builder.method;
		this.body = builder.body;
	}
	
    public byte[] doRequest() throws IOException {
		logger.info("doRequest started");
		logger.info("url: " + url);
		logger.info("method: " + method);

		StringBuffer response = new StringBuffer();
		HttpURLConnection con = null;
		try {
			con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod(method);
			con.setUseCaches(false);
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setConnectTimeout(99999999);
			con.setReadTimeout(99999999);

			try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
				wr.write(body);
				wr.flush();
			}

			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String output;
				while ((output = in.readLine()) != null) {
					response.append(output);
				}
			}
			logger.info("doRequest successfully completed");
		}

		catch (IOException e) {
			logger.error("doRequest failed: " + e);
			throw e;
		}

		finally {
			if (con!=null) con.disconnect();
		}

		return response.toString().getBytes();

		/*
		String rawData = response.toString();
		logger.info(rawData);

		String answerData;
		int n1 = rawData.indexOf("<AnswerData>");
		int n2 = rawData.indexOf("</AnswerData>");
		if (n2>n1)
			answerData = rawData.substring(n1+12, n2);
		else {
			n1 = rawData.indexOf("<?xml version=\"1.0\"?>");
			n2 = rawData.indexOf("</SOAP-ENV:Envelope>");
			if (n2>n1)
				answerData = rawData.substring(n1, n2 + 20);
			else
				answerData = rawData;
		}

        return answerData;
        */
	}

    public static class Builder {
    	private URL url;
    	private String method;
    	private byte[] body;
    	
    	public Builder url(final URL url) {
    		this.url = url;
    		return this;
    	}
    	    	
    	public Builder method(final String method) {
    		this.method = method;
    		return this;
    	}    	

    	public Builder body(final byte[] body) {
    		this.body = body;
    		return this;
    	}        
    	
    	public HttpGateway build() {
    		return new HttpGatewayImpl(this); 
    	}
    }
}
