<<PACKAGE>>

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import org.odata4j.consumer.behaviors.OClientBehaviors;
import org.odata4j.jersey.consumer.ODataJerseyConsumer;


public class ODataConnectivity {

	public String _ODATA_END_POINT;
	public String _USER;
	public String _PASSWORD;
	public String _HOST;
	public String _PORT;
	
	public ODataJerseyConsumer rooODataConsumer;
	
	public ODataConnectivity(String _ODATA_END_POINT, String _USER, String _PASSWORD, String _HOST, String _PORT)
	{
		this._ODATA_END_POINT = _ODATA_END_POINT;
		this._USER = _USER;
		this._PASSWORD = _PASSWORD;
		
		System.setProperty("https.proxyHost", _HOST);
		System.setProperty("https.proxyPort", _PORT);		
	}
	
	public String getDecodedRemoteKey(String Id) {
		String decodedKey = "";
		try{
			decodedKey = URLDecoder.decode(Id,"UTF-8");
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		return decodedKey;
	}
	
	public String getEncodedRemoteKey(String Id) {
		String encodedKey = "";
		try{
			encodedKey = URLEncoder.encode(Id,"UTF-8");
		}catch(UnsupportedEncodingException e){
			e.printStackTrace();
		}
		return encodedKey;
	}
}