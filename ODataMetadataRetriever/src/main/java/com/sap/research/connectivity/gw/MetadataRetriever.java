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

package com.sap.research.connectivity.gw;

import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.odata4j.consumer.behaviors.OClientBehaviors;
import org.odata4j.edm.EdmAssociationEnd;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.edm.EdmProperty;
import org.odata4j.jersey.consumer.ODataJerseyConsumer;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MetadataRetriever {
	
	public static void main(String[] args) {
		
		String url = args[0];
		String user = args[1];
		String pass = args[2];
		
		String http_proxy_host = null;
		String http_proxy_port = null;
		
		int noOfArguments = args.length;
		
		if (noOfArguments == 3){
			http_proxy_host = "";
			http_proxy_port = "";				
		}else if (noOfArguments == 5){
			http_proxy_host = args[3];
			http_proxy_port = args[4];			
		}
		
		System.setProperty("https.proxyHost", http_proxy_host);
		System.setProperty("https.proxyPort", http_proxy_port);			
		
	    ODataJerseyConsumer ODataConsumer = ODataJerseyConsumer.newBuilder(url).setClientBehaviors(OClientBehaviors.basicAuth(user,pass)).build();
	    
		  try {
			    
			    EdmDataServices metad = ODataConsumer.getMetadata();
			    
			    
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				
				Document doc = docBuilder.newDocument();
				Element rootElement = doc.createElement("entities");
				doc.appendChild(rootElement);
				
				for(EdmEntitySet es: metad.getEntitySets()){
					
					Element entity = doc.createElement("entity");
					rootElement.appendChild(entity);
					
					
					Attr nameAttr = doc.createAttribute("name");
					nameAttr.setValue(es.getName());
					entity.setAttributeNode(nameAttr);	
					
					List<String> keys = es.getType().getKeys();
					
		    		for (EdmProperty ep : es.getType().getProperties())
		    		{
		    			Element entityField = doc.createElement("entityfield");
		    			entity.appendChild(entityField);
		    			
		    			Element fieldName = doc.createElement("fieldname");
		    			fieldName.appendChild(doc.createTextNode(ep.getName()));
		    			entityField.appendChild(fieldName);
		    			
		    			Element fieldType = doc.createElement("fieldtype");
		    			String remoteFieldType = ep.getType().getFullyQualifiedTypeName();
		    			remoteFieldType = (remoteFieldType.toLowerCase().startsWith("edm.")) ? remoteFieldType.substring(4) : remoteFieldType;
						fieldType.appendChild(doc.createTextNode(remoteFieldType));
		    			entityField.appendChild(fieldType);	

		    			Element key = doc.createElement("key");
		    			String text = null;
		    			if (keys.contains(ep.getName())){
		    			  text = "true";
		    			} else {
		    			  text = "false";
		    			}
		    			key.appendChild(doc.createTextNode(text));
		    			entityField.appendChild(key);		
		    		}					
					
		    		for (EdmNavigationProperty navProp : es.getType().getNavigationProperties()) {
		    			Element navPropertyXMLNode = doc.createElement("navproperty");
		    			entity.appendChild(navPropertyXMLNode);
		    			
		    			Element relationship = doc.createElement("relationship_id");
		    			relationship.appendChild(doc.createTextNode(navProp.getRelationship().getName()));
		    			navPropertyXMLNode.appendChild(relationship);
		    			
		    			Element navName = doc.createElement("navpath");
		    			navName.appendChild(doc.createTextNode(navProp.getName()));
		    			navPropertyXMLNode.appendChild(navName);
		    			
		    			Element fromNode = doc.createElement("end1");
		    			EdmAssociationEnd end1 = navProp.getRelationship().getEnd1();
						fromNode.appendChild(doc.createTextNode(end1.getType().getName()));
		    			navPropertyXMLNode.appendChild(fromNode);
		    			
		    			Element toNode = doc.createElement("end2");
		    			EdmAssociationEnd end2 = navProp.getRelationship().getEnd2();
						toNode.appendChild(doc.createTextNode(end2.getType().getName()));
		    			navPropertyXMLNode.appendChild(toNode);
		    			
						Attr multiplicity1 = doc.createAttribute("multiplicity");
						multiplicity1.setValue(end1.getMultiplicity().getSymbolString());
						fromNode.setAttributeNode(multiplicity1);	
		    			
						Attr multiplicity2 = doc.createAttribute("multiplicity");
		    			multiplicity2.setValue(end2.getMultiplicity().getSymbolString());
						toNode.setAttributeNode(multiplicity2);	
		    		}
				}
				

				StringWriter out = new StringWriter();				
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			    transformer.transform(new DOMSource(doc), new StreamResult(out));	
			    
				System.out.print(out.toString());
				System.out.flush();
		 
			  } catch (ParserConfigurationException pce) {
				  pce.printStackTrace(System.out);
			  } catch (TransformerException tfe) {
				  tfe.printStackTrace(System.out);				
			  }	catch (Exception e) {
				  e.printStackTrace(System.out);
			  }
	}

}
