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

package com.sap.research.connectivity.gw.converters;

import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sap.research.connectivity.gw.GWOperationsUtils;
import com.sap.research.connectivity.gw.GwRemoteEntity;


@Component
@Service

public class GwRemoteEntityConverter extends GWOperationsUtils implements Converter<GwRemoteEntity> {
	
    private Logger log = Logger.getLogger(getClass().getName());
    
    public GwRemoteEntity convertFromText(final String value,
            final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new GwRemoteEntity(value);
    }

    public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {
    	
//      Parse, and obtain the namespace value
    	String namespaceValue = " ";

    	if(target.getRemainingBuffer().contains("--namespace"))
    	{
    		String[] splitCommand = target.getRemainingBuffer().split(" ");
    		
    		for(int i=0; i < splitCommand.length; i++){
    			if(splitCommand[i].equals("--namespace"))
    			{
    			  namespaceValue = splitCommand[i+1];
    			  break;
    			}
    			
    		}
    	 }
    	else {
    		log.severe("Please specify a namespace before choosing the remote entity set (use the option \"--namespace\").");
    		return true;
    	}
    	
//      Obtain the available entity sets from the gateway name space
    	String metaDataPath = getSubPackagePath(oDataFolder);
    	String metaDataFile = metaDataPath + SEPARATOR + namespaceValue +"_metadata.xml";
    	
		Document doc;
		  
		try{
			InputStream metaDataIs = fileManager.getInputStream(metaDataFile);
			doc = XmlUtils.getDocumentBuilder().parse(metaDataIs);
		} catch (Exception ex){
			  //throw new IllegalStateException(ex);
			log.severe("Namespace \"" + namespaceValue + "\" does not exist or is corrupted. Please specify a valid namespace.");
    		return true;
		}        
		 
		NodeList nodeList = doc.getElementsByTagName("entity"); 
		 
	    for (int i = 0; i < nodeList.getLength(); i++){
			  
		  Node node = nodeList.item(i);
		  Attr attr = (Attr) node.getAttributes().getNamedItem("name");
		  
		  completions.add(new Completion(attr.getValue().toString()));
		
	    }		 
    	
    	return false;
    	
    }
   

    public boolean supports(final Class<?> requiredType, final String optionContext) {
        return GwRemoteEntity.class.isAssignableFrom(requiredType);
    }	

}

