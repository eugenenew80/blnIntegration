package bln.integration.http;

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Builder
public class HttpGatewayImpl implements HttpGateway {
	private static Logger logger = LoggerFactory.getLogger(HttpGatewayImpl.class);
	private final URL url;
	private final String method;
	private final byte[] body;

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
	}
}
