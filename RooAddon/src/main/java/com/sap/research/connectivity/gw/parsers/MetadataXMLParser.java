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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.roo.model.ReservedWords;

public class MetadataXMLParser {
	
    /**
     * Get hold of a JDK Logger
     */
	Document doc;
	String remoteEntity;
	
	/*
	 * We use array in the value, to keep the original name (from the remote system), because we might change it locally 
	 * (for cases like "id", "user", etc.) 
	 */
	Map<String[], String> fields = new HashMap<String[], String>();
	Map<String[], String> keys = new HashMap<String[], String>();
	
	/*
	 * The relation properties.
	 * Key = field name on which to make relationship (navbar is the property exposed by OData)
	 * Value = array of strings in this order: from_entity, from_entity_multiplicity, to_entity, to_entity_multiplicity
	 */
	Map<String, String[]> relationships = new HashMap<String, String[]>();
	

	public MetadataXMLParser(Document document, String remoteEntity){

		this.doc = document;
		this.remoteEntity = remoteEntity;
	
	}
	
	public void parse() throws Exception{
		
	  NodeList nodeList = doc.getElementsByTagName("entity");
	  Element remoteEntityEle = getRemoteEntityNode(nodeList);
	  
	  if (remoteEntityEle == null)
		  throw new Exception("There is no entity with name "+ remoteEntity);
	  
	  getFieldsOfEntityNode(remoteEntityEle);
	  getRelationsOfEntityNode(remoteEntityEle);
	}

	private void getRelationsOfEntityNode(Element remoteEntityEle) throws Exception {
		  NodeList relationNodes = remoteEntityEle.getElementsByTagName("navproperty");
		  for (int i = 0; i < relationNodes.getLength(); i++){
			  Element relationNode = (Element)relationNodes.item(i);
			  
			  //String relationshipId = getTextValue(relationNode,"relationship_id");
			  String relationField = getTextValue(relationNode,"navpath");
			  
			  Element relationMultiplicityEnd1Node = getSubNodeByName(relationNode, "end1");
			  String relationMultiplicityEnd1 = relationMultiplicityEnd1Node.getFirstChild().getNodeValue();
			  String relationMultiplicityEnd1Attr = getNodeAttributeValue(relationMultiplicityEnd1Node, "multiplicity");
			  
			  Element relationMultiplicityEnd2Node = getSubNodeByName(relationNode, "end2");
			  String relationMultiplicityEnd2 = relationMultiplicityEnd2Node.getFirstChild().getNodeValue();
			  String relationMultiplicityEnd2Attr = getNodeAttributeValue(relationMultiplicityEnd2Node, "multiplicity");
			  
			  String[] relationProperties = {relationMultiplicityEnd1, relationMultiplicityEnd1Attr, relationMultiplicityEnd2, relationMultiplicityEnd2Attr}; 
			  relationships.put(relationField, relationProperties);
		  }
	}

	private void getFieldsOfEntityNode(Element remoteEntityEle) {
		NodeList fieldList = remoteEntityEle.getElementsByTagName("entityfield");
		    
		  for (int i = 0; i < fieldList.getLength(); i++){
			  
			  Element field = (Element)fieldList.item(i);
			  
			  String fieldName = getTextValue(field,"fieldname");
			  String fieldType = getTextValue(field,"fieldtype");
			  String key       = getTextValue(field, "key");
			  
			  if (key.equals("true")){
				  keys.put(processRemoteField(fieldName), fieldType);
			  }else {
				  fields.put(processRemoteField(fieldName), fieldType);  
			  }
		  }
	}

	private Element getRemoteEntityNode(NodeList nodeList) {
		for (int i = 0; i < nodeList.getLength(); i++){
			  
			  Node node = nodeList.item(i);
			  Attr attr = (Attr) node.getAttributes().getNamedItem("name");
			  
			  if(remoteEntity.equals(attr.getValue().toString()))
				   return (Element)node;
		  }
		return null;
	}
	
	public Map<String[], String> getFields() {
		return fields;
	}

	public Map<String[], String> getKeys() {
		return keys;
	}	
	
	public Map<String, String[]> getRelationships() {
		return relationships;
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
	
	private Element getSubNodeByName(Element ele, String tagName) {
		NodeList nl = ele.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			return (Element)nl.item(0);
		}
		return null;
	}
	
	private String getNodeAttributeValue(Element ele, String attributeName) throws Exception {
		return ((Attr) ele.getAttributes().getNamedItem(attributeName)).getValue().toString();
	}
	
	private String[] processRemoteField(String remoteFieldName) {
		String[] returnArray = new String[2];
		returnArray[0] = remoteFieldName;
		returnArray[1] = remoteFieldName;
		try {
			ReservedWords.verifyReservedWordsNotPresent(remoteFieldName);
			
			if (Arrays.asList(Constants.RESERVED_FIELD_WORDS_ARRAY).contains(remoteFieldName.toUpperCase())) {
				throw new IllegalStateException("Found a word which has potential dangerous implications ");
			}
		} catch (IllegalStateException e) {
			returnArray[1] = "remote_" + remoteFieldName;
		}
		
		return returnArray;
	}

}
