<<PACKAGE>>

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.odata4j.consumer.ODataClientRequest;
import org.odata4j.jersey.consumer.ODataJerseyConsumer;
import org.odata4j.jersey.consumer.behaviors.JerseyClientBehavior;
import org.odata4j.repack.org.apache.commons.codec.binary.Base64;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.Filterable;


public class <<NSNAME>> extends ODataConnectivity {

	public <<NSNAME>>() {
		super("<<URL>>", "<<USER>>", "<<PASSWORD>>", "<<HOST>>", "<<PORT>>");
		myJerseyBehavior jcb = new myJerseyBehavior(_USER, _PASSWORD);

		rooODataConsumer = ODataJerseyConsumer.newBuilder(_ODATA_END_POINT).setClientBehaviors(jcb).build();
	}
	
	private static class myJerseyBehavior implements JerseyClientBehavior {
	 	   
		private String xsrfCookieName;
		private String xsrfCookieValue;
		private String xsrfTokenValue;
		private String _USER;
		private String _PASSWORD;
		
		myJerseyBehavior(String _USER, String _PASSWORD) {
			this._USER = _USER;
			this._PASSWORD = _PASSWORD;
		}
 	   
		@Override
		public ODataClientRequest transform(ODataClientRequest request) {
         String userPassword = _USER + ":" + _PASSWORD;
         String encoded = Base64.encodeBase64String(userPassword.getBytes());
         encoded = encoded.replaceAll("\r\n?", "");
         
         if (request.getMethod().equals("GET")){
	            request = request
	            		<<CSRF_MODE_GET>>
	                   .header("Authorization", "Basic " + encoded);        
	                        
	            return request;
         }else {
         	request = request
		                   <<CSRF_MODE_SET>>
   		                   .header("Authorization", "Basic " + encoded);
		                             
		            return request;		            	
         }
		}
		
		@Override
		public void modifyWebResourceFilters(Filterable arg0) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public void modifyClientFilters(Filterable client) {
		       client.addFilter(new ClientFilter(){
		    	   
				@Override
				public ClientResponse handle(ClientRequest clientRequest) throws ClientHandlerException {
		             ClientResponse response = getNext().handle(clientRequest);
		             
		             List<NewCookie> cookies = response.getCookies();
		             
		             for (NewCookie cookie:cookies) {
		            	 if (cookie.getName().startsWith("sap-XSRF")) {
		            		 xsrfCookieName = cookie.getName();
		            		 xsrfCookieValue = cookie.getValue();
		            		 break;
		            	 }
		            		 
		            	// lastResponse += "\n" + cookie.getName() + " -----> " + cookie.getValue(); 
		             }
		             
		             MultivaluedMap<String, String> responseHeaders = response.getHeaders();
		             xsrfTokenValue = responseHeaders.getFirst("X-CSRF-Token");

		             return response;
		           }});
		       }
		
		@Override
		public void modify(ClientConfig arg0) {
			// TODO Auto-generated method stub
		}
	}
	
	
}
