package nz.co.vista.services.wsvistawebclient_servicecontracts._1;

import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import nz.co.vista.services.wsvistawebclient_datatypes._1.DataResponse;
import nz.co.vista.services.wsvistawebclient_datatypes._1.GetSessionInfoRequest;
import oracle.webservices.ClientConstants;
import weblogic.wsee.jws.jaxws.owsm.SecurityPoliciesFeature;

// !THE CHANGES MADE TO THIS FILE WILL BE DESTROYED IF REGENERATED!
// This source file is generated by Oracle tools
// Contents may be subject to change
// For reporting problems, use the following
// Version = Oracle WebServices (11.1.1.0.0, build 130224.1947.04102)

public class DataServiceClient
{
  @WebServiceRef
  private static DataService_Service dataService_Service;

  public static void main(String [] args)
  {
    dataService_Service = new DataService_Service();
        SecurityPoliciesFeature securityFeatures =
            new SecurityPoliciesFeature(new String[] { "oracle/wss11_message_protection_client_policy" });
        DataService port = dataService_Service.getDataService(securityFeatures);
        // Add your code to call the desired methods.
        
        BindingProvider wsbp = (BindingProvider) port;
                    
        Map<String, Object> reqContext = wsbp.getRequestContext();        
        reqContext.put(ClientConstants.WSSEC_KEYSTORE_TYPE, "JKS");  
        reqContext.put(ClientConstants.WSSEC_KEYSTORE_LOCATION, "cinepolis.jks");  
        reqContext.put(ClientConstants.WSSEC_KEYSTORE_PASSWORD, "cinepolis1");  
  
        reqContext.put(ClientConstants.WSSEC_ENC_KEY_ALIAS, "1");  
        reqContext.put(ClientConstants.WSSEC_ENC_KEY_PASSWORD, "cinepolis1");  
        reqContext.put(ClientConstants.WSSEC_RECIPIENT_KEY_ALIAS, "1");  

        System.out.println(securityFeatures.getPolicies());
        
        GetSessionInfoRequest request = new GetSessionInfoRequest();
        request.setCinemaId("305");
        request.setSessionId("61080");
        DataResponse response = port.getSessionInfo(request);

        System.out.println(response.getDatasetXML());

  }
}
