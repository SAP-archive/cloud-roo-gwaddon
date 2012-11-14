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

import japa.parser.ast.body.FieldDeclaration;

public class JavaSourceField {

	public String FIELD_STRING;
	private String fieldPrefix;
	private String fieldType;
	private String fieldName;
	private String fieldValue;
	private String fieldAnnotations;
	
	JavaSourceField(String fieldPrefix, String fieldType, String fieldName, String fieldValue, String fieldAnnotations) {
		this.fieldPrefix = fieldPrefix;
		this.fieldType = fieldType;
		this.fieldName = fieldName;
		this.fieldValue = fieldValue;
		this.fieldAnnotations = fieldAnnotations;
		
		try {
			FIELD_STRING = makeField();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public JavaSourceField (FieldDeclaration field) {
		
		this(JavaSourceParserUtils.translateModifiers(field.getModifiers()),
			field.getType().toString(),
			JavaSourceParserUtils.translateFieldName(field.getVariables()),
			JavaSourceParserUtils.translateFieldValue(field.getVariables()),
			JavaSourceParserUtils.translateAnnotations(field.getAnnotations()));
	}
	
	
	public void setFieldPrefix(String fieldPrefix) {
		this.fieldPrefix = fieldPrefix;
	}
	
	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}
	
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	
	public void setFieldValue(String fieldValue) {
		this.fieldValue = fieldValue;
	}
	
	public String getFieldPrefix() {
		return fieldPrefix;
	}
	
	public String getFieldType() {
		return fieldType;
	}
	
	public String getFieldName() {
		return fieldName;
	}
	
	public String getFieldValue() {
		return fieldValue;
	}
	
	public void setAnnotations(String annotations) {
		this.fieldAnnotations = annotations;
	}	
	
	public String makeField() throws Exception {
		if (!fieldPrefix.isEmpty())
			fieldPrefix += " ";
		
		if (fieldType.isEmpty())
			throw new Exception("Field Type cannot be empty!");
		
		if (fieldName.isEmpty())
			throw new Exception("Field Name cannot be empty!");
				
		if (!fieldValue.isEmpty())
		{
			if (fieldType.equals("String"))
				fieldValue = "\"" + fieldValue + "\"";
			
			fieldValue = " = " + fieldValue;
		}
		
		if (!fieldAnnotations.isEmpty())
			fieldAnnotations += "\n";
		
		String returnString = "";
		
		if (!fieldAnnotations.isEmpty()){
			returnString = "\t" + fieldAnnotations + "\t" + fieldPrefix + fieldType + " " + fieldName + fieldValue + ";";
		} else {
			returnString = "\t" + fieldPrefix + fieldType + " " + fieldName + fieldValue + ";";			
		}		
		
		return returnString;
	}
}
