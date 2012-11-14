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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jvnet.inflector.Noun;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.support.util.FileUtils;

import com.sap.research.connectivity.gw.parsers.JavaSourceField;
import com.sap.research.connectivity.gw.parsers.JavaSourceFileEditor;

public class GwUtils {

	public static final char SEPARATOR = File.separatorChar;
	
	public static final String GW_CONNECTION_FIELD_NAME = "odc";
	
	public static void createClassFileFromTemplate(String packageName, String subFolder, String templateFileName, String targetFileName, 
			Map<String, String> replacements, FileManager fileManager, Class<?> loadingClass) {
		InputStream inputStream = null;
	    OutputStream outputStream = null;
	    String targetFile = subFolder + SEPARATOR + targetFileName;
        MutableFile mutableFile = fileManager.exists(targetFile) ? fileManager.updateFile(targetFile) : fileManager.createFile(targetFile);
        try {
            inputStream = FileUtils.getInputStream(loadingClass, templateFileName);
            outputStream =  mutableFile.getOutputStream();
            String inputStreamString = IOUtils.toString(inputStream);
            
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
            	inputStreamString = inputStreamString.replace(entry.getKey(), entry.getValue());
            }
            //System.out.println(inputStreamString);
            inputStream = IOUtils.toInputStream(inputStreamString);
            IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
	}
	
	public static void createFileFromString(String subFolder, String targetFileName, String content, FileManager fileManager) {
		InputStream inputStream = null;
	    OutputStream outputStream = null;
	    String targetFile = subFolder + SEPARATOR + targetFileName;
        MutableFile mutableFile = fileManager.exists(targetFile) ? fileManager.updateFile(targetFile) : fileManager.createFile(targetFile);
        try {
    		inputStream = IOUtils.toInputStream(content);
    		outputStream =  mutableFile.getOutputStream();
        	//System.out.println(inputStreamString);
    		IOUtils.copy(inputStream, outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(outputStream);
        }
	}
	
	public static String getInflectorPlural(final String term, final Locale locale) {
        try {
            return Noun.pluralOf(term, locale);
        }
        catch (final RuntimeException re) {
            return term;
        }
    } 
	
	public static String odataToJavaType(String oDataType) {
		
		if (oDataType.equals("SByte"))
			return("byte");
		if (oDataType.equals("Int64"))
			return("long");		
		if (oDataType.equals("Int32"))
			return("int");
		if (oDataType.equals("Int16"))
			return("short");		
		if(oDataType.equals("String"))
			return("String");
		if (oDataType.equals("Double"))
			return("double");
		if (oDataType.equals("Single"))
			return("float");
		if (oDataType.equals("Decimal"))
			return("float");
		if (oDataType.equals("Byte"))
			return("byte");
		if (oDataType.equals("Boolean"))
			return("boolean");
		if (oDataType.equals("Guid"))
			return("String");
		if (oDataType.equals("DateTime") || oDataType.equals("DateTimeOffset"))
			return("Date");
		if (oDataType.equals("Time"))
			return("String");	
		else
			return("String");
	
	}
	
	public static String generateReversedCast(String javaType) {
	
		if (javaType.toLowerCase().equals("byte"))
			return "byte_";
		if (javaType.toLowerCase().equals("short"))
			return "int16";
		if (javaType.toLowerCase().equals("long") || javaType.toLowerCase().equals("double"))
			return "decimal";
		if (javaType.toLowerCase().equals("float"))
			return "single";
		if (javaType.toLowerCase().equals("int"))
			return "int32";
		if (javaType.toLowerCase().equals("boolean"))
			return "boolean_";
		if (javaType.toLowerCase().equals("string") || javaType.toLowerCase().equals("char"))
			return "string";
		if (javaType.toLowerCase().equals("date"))
			return "datetime";
		if (javaType.toLowerCase().equals("time"))
			return "string";		
		
		return "string";
	}
	
	
	public static String generateCast(String javaType) {
		
		String dataTypes = "byte;short;long;float;double;boolean;Byte;Short;Long;Float;Double;Boolean;";
	
		String fieldTypeObjectName = "";
		
		if (dataTypes.contains(javaType)) {
			fieldTypeObjectName = StringUtils.capitalize(javaType);
			fieldTypeObjectName =  fieldTypeObjectName + ".parse" + fieldTypeObjectName + "(";
		}
	    
		else
			if (javaType.equals("int") || javaType.equals("Integer")) { 
				fieldTypeObjectName = "Integer" + ".parseInt(";
			}
		
		return fieldTypeObjectName;
	}
	
	public static List<String> getFieldsIncludedInClass(JavaSourceFileEditor entityClassFile) {

		List<String> fields = new ArrayList<String> ();

		ArrayList<JavaSourceField> globalFieldList = entityClassFile.getGlobalFieldList();

    	for(JavaSourceField globalField : globalFieldList){
    		fields.add(globalField.getFieldName());
    	}
		return fields;
	}	
	
	public static String getNamespaceFromClass(JavaSourceFileEditor entityClassFile) {
		String nameSpace = "";
		ArrayList<JavaSourceField> globalFieldList = entityClassFile.getGlobalFieldList();

    	for(JavaSourceField globalField : globalFieldList){
    		if(globalField.getFieldName().equals(GW_CONNECTION_FIELD_NAME)){
    			String nameSpaceValue = globalField.getFieldValue();
    			String[] splitNameSpaceValue = nameSpaceValue.split(" ");
    			int len = splitNameSpaceValue.length;
    			String nameSpaceInter = splitNameSpaceValue[len-1];
    			nameSpace = nameSpaceInter.substring(0, nameSpaceInter.length() - 2);  			
    			break;
    		}
    	}
		return nameSpace;
	}
	
	
}
