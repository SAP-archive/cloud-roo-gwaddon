/*
 * Copyright 2012 SAP AG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sap.research.connectivity.gw.parsers;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MetadataXMLParser {
	
	Document doc;
	String remoteEntity;
	
	Map<String, String> fields = new HashMap<String, String>();
	Map<String, String> keys = new HashMap<String, String>();	
	

	public MetadataXMLParser(Document document, String remoteEntity){

		this.doc = document;
		this.remoteEntity = remoteEntity;
	
	}
	
	public void parse(){
		
	  NodeList nodeList = doc.getElementsByTagName("entity");
	  Element remoteEntityEle = null;
	  
	  for (int i = 0; i < nodeList.getLength(); i++){
		  
		  Node node = nodeList.item(i);
		  Attr attr = (Attr) node.getAttributes().getNamedItem("name");
		  
		  if(remoteEntity.equals(attr.getValue().toString())){
			   remoteEntityEle = (Element)node;
			   break;
			  }
	  }
	  
	  NodeList fieldList = remoteEntityEle.getElementsByTagName("entityfield");
	    
	  for (int i = 0; i < fieldList.getLength(); i++){
		  
		  Element field = (Element)fieldList.item(i);
		  
		  String fieldName = getTextValue(field,"fieldname");
		  String fieldType = getTextValue(field,"fieldtype");
		  String key       = getTextValue(field, "key");
		  
		  if (key.equals("true")){
			  keys.put(fieldName, fieldType);
		  }else {
			  fields.put(fieldName, fieldType);  
		  }
	  }		

	}
	
	public Map<String, String> getFields() {
		return fields;
	}

	public Map<String, String> getKeys() {
		return keys;
	}	
	
	
	private String getTextValue(Element ele, String tagName) {
		String textVal = null;
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			Element el = (Element)nl.item(0);
			textVal = el.getFirstChild().getNodeValue();
		}
		return textVal;
	}	

}
