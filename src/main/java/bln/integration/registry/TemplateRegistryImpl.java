package bln.integration.registry;

import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
public class TemplateRegistryImpl implements TemplateRegistry {
	private Map<String, String> templates = new HashMap<>();
	
	@PostConstruct
	public void init() {
        
		//REQML
		
		String template = ""
                + "<?xml version=\"1.0\" standalone=\"yes\"?>"
                + "<DATAPACKET Version=\"2.0\">"
					+ "<METADATA>"
						+ "<FIELDS>"
							+ "<FIELD attrname=\"PPOINT_CODE\" fieldtype=\"string\" required=\"true\" WIDTH=\"50\" />"
							+ "<FIELD attrname=\"PML_ID\" fieldtype=\"fixed\" required=\"true\" WIDTH=\"6\" />"
							+ "<FIELD attrname=\"PBT\" fieldtype=\"SQLdateTime\" />"
							+ "<FIELD attrname=\"PET\" fieldtype=\"SQLdateTime\" />"
						+ "</FIELDS>"
						+ "<PARAMS LCID=\"0\" />"
					+ "</METADATA>"
					+ "<ROWDATA>"
						+ "#points#"
					+ "</ROWDATA>"
                + "</DATAPACKET>";		
        registerTemplate("EMCOS_REQML_DATA", template);
        
        
        template = ""
                + "<UserId>#user#</UserId>"
                + "<aPacked>#isPacked#</aPacked>"
                + "<Func>#func#</Func>"
                + "<Reserved></Reserved>"
                + "<AttType>#attType#</AttType>";
        registerTemplate("EMCOS_REQML_PROPERTY", template);        
        
        
        template = ""
                + "<?xml version=\"1.0\"?>"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<SOAP-ENV:Body>"
						+ "<TransferEMCOSData xmlns=\"http://www.sigmatelas.lt/webservices\">"
							+ "<parameters>"
								+ "<aDProperty>"
									+ "#property#"
								+ "</aDProperty>"
								+ "<aData>"
									+ "#data#"
								+ "</aData>"
							+ "</parameters>"
						+ "</TransferEMCOSData>"
					+ "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";
        registerTemplate("EMCOS_REQML_BODY", template);

        template = ""
                + "<?xml version=\"1.0\"?>"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<SOAP-ENV:Body>"
						+ "<TransferEMCOSData xmlns=\"http://www.sigmatelas.lt/webservices\">"
							+ "<parameters>"
								+ "<aDProperty>"
									+ "#property#"
								+ "</aDProperty>"
								+ "<aData>";
        registerTemplate("EMCOS_REQML_BODY_1", template);

        template = ""
								+ "</aData>"
							+ "</parameters>"
						+ "</TransferEMCOSData>"
					+ "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";
        registerTemplate("EMCOS_REQML_BODY_2", template);



        //REQCFG
        
        template = ""
    	 		+ "<?xml version=\"1.0\" encoding=\"windows-1251\"?>"
    	 		+ "<DATAPACKET Version=\"2.0\">"
					+ "<METADATA>"
						+ "<FIELDS>"
							+ "<FIELD attrname=\"CFG\" fieldtype=\"fixed\" WIDTH=\"6\" />"
						+ "</FIELDS>"
						+ "<PARAMS LCID=\"0\" />"
					+ "</METADATA>"
					+ "<ROWDATA>"
					+ "</ROWDATA>"
    	 		+ "</DATAPACKET>";			
        registerTemplate("EMCOS_REQCFG_DATA", template);    
        
        template = ""
                + "<UserId>#user#</UserId>"
                + "<aPacked>#isPacked#</aPacked>"
                + "<Func>#func#</Func>"
                + "<Reserved></Reserved>"
                + "<AttType>#attType#</AttType>";        		
        registerTemplate("EMCOS_REQCFG_PROPERTY", template);    
        
        template = ""
                + "<?xml version=\"1.0\"?>"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<SOAP-ENV:Body>"
						+ "<TransferEMCOSData xmlns=\"http://www.sigmatelas.lt/webservices\">"
							+ "<parameters>"
								+ "<aDProperty>"
									+ "#property#"
								+ "</aDProperty>"
								+ "<aData>"
									+ "#media#"
								+ "</aData>"
							+ "</parameters>"
						+ "</TransferEMCOSData>"
					+ "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";                
        registerTemplate("EMCOS_REQCFG_BODY", template);



        //EEML

		template = ""
				+ "<?xml version=\"1.0\" encoding=\"windows-1251\"?>"
				+ "<DATAPACKET Version=\"2.0\">"
					+ "<METADATA>"
						+ "<FIELDS>"
							+ "<FIELD attrname=\"PDA\" 	fieldtype=\"dateTime\" />"
							+ "<FIELD attrname=\"PBT\" fieldtype=\"dateTime\" />"
							+ "<FIELD attrname=\"PET\" fieldtype=\"dateTime\" />"
							+ "<FIELD attrname=\"PCODE\" fieldtype=\"string\" WIDTH=\"80\" />"
							+ "<FIELD attrname=\"PML_ID\" fieldtype=\"i4\"  />"
							+ "<FIELD attrname=\"PVAL\" fieldtype=\"r8\" DECIMALS=\"8\" WIDTH=\"32\" />"
						+ "</FIELDS>"
						+ "<PARAMS LCID=\"0\" />"
					+ "</METADATA>"
					+ "<ROWDATA>"
						+ "#points#"
					+ "</ROWDATA>"
				+ "</DATAPACKET>";
		registerTemplate("EMCOS_EEML_DATA", template);


		template = ""
				+ "<NS:TEMCOSSetStruct xsi:type=\"NS:TEMCOSSetStruct\" xmlns:NS=\"urn:ST_ExchWebServiceIntf\">"
					+ "<UserId>#user#</UserId>"
					+ "<aPacked>#isPacked#</aPacked>"
					+ "<Func>#func#</Func>"
					+ "<Reserved></Reserved>"
					+ "<AttType>#attType#</AttType>"
				+ "</NS:TEMCOSSetStruct>"
		;
		registerTemplate("EMCOS_EEML_PROPERTY", template);


		template = ""
				+ "<?xml version=\"1.0\"?>"
				+ "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
					+ "<SOAP-ENV:Body>"
						+ "<TransferEMCOSData xmlns=\"http://www.sigmatelas.lt/webservices\">"
							+ "<aDProperty>"
								+ "#property#"
							+ "</aDProperty>"
							+ "<aData>"
								+ "#media#"
							+ "</aData>"
						+ "</TransferEMCOSData>"
					+ "</SOAP-ENV:Body>"
				+ "</SOAP-ENV:Envelope>";
		registerTemplate("EMCOS_EEML_BODY", template);
	}
	
	@Override
	public void registerTemplate(String key, String template) {
		templates.put(key, template);
	}

	@Override
	public String getTemplate(String key) {
		return templates.get(key);
	}
}
