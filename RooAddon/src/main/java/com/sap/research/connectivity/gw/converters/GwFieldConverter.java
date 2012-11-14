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

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.shell.Completion;
import org.springframework.roo.shell.Converter;
import org.springframework.roo.shell.MethodTarget;

import com.sap.research.connectivity.gw.GWOperationsUtils;
import com.sap.research.connectivity.gw.GwField;
import com.sap.research.connectivity.gw.GwUtils;
import com.sap.research.connectivity.gw.parsers.JavaSourceFileEditor;

@Component
@Service

public class GwFieldConverter extends GWOperationsUtils implements Converter<GwField> {

    private Logger log = Logger.getLogger(getClass().getName());
    
    public GwField convertFromText(final String value, final Class<?> requiredType, final String optionContext) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new GwField(value);
    }

    public boolean getAllPossibleValues(final List<Completion> completions, final Class<?> requiredType, final String existingData,
            final String optionContext, final MethodTarget target) {

    	//Parse string buffer and derive the class name
    	String entityClassName = null;
    	
    	if(target.getRemainingBuffer().contains("--entityClass"))
    	{
    		String[] splitCommand = target.getRemainingBuffer().split(" ");
    		
    		for(int i=0; i < splitCommand.length; i++){
    			if(splitCommand[i].equals("--entityClass"))
    			{
    			  entityClassName = splitCommand[i+1];
    			  break;
    			}
    		}
    	 } 
    	else {
    		log.severe("Please specify a class before choosing a field to import (use the option \"--class\").");
    		return true;
    	}
    	
    	//Derive used namespace from the Class
    	JavaSourceFileEditor entityClassFile = null;
    	try {
    		 	entityClassFile = getJavaFileEditor("domain", entityClassName, false);
    	} catch (Exception e) {
    		log.severe("The class \"" + entityClassName + "\" is not a valid class for connecting to a NW Gateway service. Please choose a valid class.");
    		return true;
    	}
    	
    	if (entityClassFile == null) {
    		log.severe("The class \"" + entityClassName + "\" is not a valid class for connecting to a NW Gateway service. Please choose a valid class.");
    		return true;
    	}
    	
    	String nameSpace = GwUtils.getNamespaceFromClass(entityClassFile);
    	
        //Derive already used fields in the Class    	
    	List<String> includedFieldsInClass = GwUtils.getFieldsIncludedInClass(entityClassFile);
    	
    	if (nameSpace.isEmpty()) {
    		log.severe("The class \"" + entityClassName + "\" is not a valid class for connecting to a NW Gateway service. Please choose a valid class.");
    		return true;
    	}
    	
    	//Retrieve the field list from metadata xml
    	Map<String, String> fields = getFieldsOfRemoteEntity(entityClassName, nameSpace);
		
		for(Map.Entry<String, String> field : fields.entrySet()){
			if(!includedFieldsInClass.contains(field.getKey()))
				completions.add(new Completion(field.getKey()));
		}
    	
		return false;
	}

    public boolean supports(final Class<?> requiredType, final String optionContext) {
        return GwField.class.isAssignableFrom(requiredType);
    }

}
