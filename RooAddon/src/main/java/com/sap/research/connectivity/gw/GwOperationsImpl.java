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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetailsBuilder;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.project.LogicalPath;
import org.springframework.roo.project.Path;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import com.sap.research.connectivity.gw.parsers.JavaSourceField;
import com.sap.research.connectivity.gw.parsers.JavaSourceFieldBuilder;
import com.sap.research.connectivity.gw.parsers.JavaSourceFileEditor;
import com.sap.research.connectivity.gw.parsers.JavaSourceMethod;
import com.sap.research.connectivity.gw.parsers.JavaSourceMethodBuilder;
import com.sap.research.connectivity.gw.parsers.MetadataXMLParser;

/**
 * Implementation of {@link GwOperations} interface.
 *
 * @since 1.1.1
 */
@Component
@Service
public class GwOperationsImpl extends GWOperationsUtils implements GwOperations {

    /* ------------------------------------------------------------------------------------------------------------------
     * SAP RESEARCH OWN CODE
     * 
     * 
     */
    
    //GW Setup command is available only if project and persistence are setup
	public boolean isCommandGWSetupAvailable() {
		return isInstalledInModule(projectOperations.getFocusedModuleName());
	}
	
    public boolean isInstalledInModule(final String moduleName) {
        final LogicalPath resourcesPath = LogicalPath.getInstance(
                Path.SRC_MAIN_RESOURCES, moduleName);
//        System.out.println(moduleName + resourcesPath.toString());
        return isProjectAvailable()
                && fileManager.exists(projectOperations.getPathResolver()
                        .getIdentifier(resourcesPath, PERSISTENCE_XML));
    }
    
    public boolean isProjectAvailable() {
        return projectOperations.isFocusedProjectAvailable();
    }    
    
    //Add ODATA Endpoint command is available only if the class for GW Connectivity is present
	public boolean isCommandODataEndpointAvailable() {
        return fileManager.exists(getSubPackagePath(oDataFolder) + SEPARATOR + "ODataConnectivity.java");
	}
    
	//Add GW Entity command is available only if an OData Endpoint class has been defined and thus metadata has been extracted
	public boolean isCommandGWEntityAvailable() {
		SortedSet<FileDetails> files = fileManager.findMatchingAntPath(getSubPackagePath(oDataFolder) + SEPARATOR + "*_metadata.xml");
		return !files.isEmpty();
	}

	//Add GW Field command is available only if the GW Entity command is available and a java class exists connecting to an ODATA Endpoint
	public boolean isCommandGWFieldAvailable() throws IOException {
		SortedSet<FileDetails> files = fileManager.findMatchingAntPath(getSubPackagePath(domain) + SEPARATOR + "*.java");
		for (FileDetails file : files) {
			InputStream inputStream = fileManager.getInputStream(file.getCanonicalPath());
			if (IOUtils.toString(inputStream).contains("ODataConnectivity " + GwUtils.GW_CONNECTION_FIELD_NAME)) {
				IOUtils.closeQuietly(inputStream);
				return true;
			}
			IOUtils.closeQuietly(inputStream);
		}
		
		return false;
	}
	
	public boolean isCommandGWMVCAdaptCommandAvailable() throws IOException {
		/*
		 * We check first to see if there is a gateway entity available. If so, we check if there are any generated controllers.
		 */
	    boolean returnResults = false;
		if (isCommandGWFieldAvailable() == true) {
			SortedSet<FileDetails> files = fileManager.findMatchingAntPath(getSubPackagePath(web) + SEPARATOR + "*Controller.java");
			if (!files.isEmpty())
				returnResults = true;
		}
		
	    return returnResults;
	}
    
    public void addODataConnectivity() {
   		addODataDependenciesToPom();
   		addOdataConnectivityClass();
   	}
	
	public void addNamespace(String nsName, String url, String user, String pass, String csrfMode, String host, String port, int timeout) throws Exception {
		
		String metadataString = "";
		
		metadataString = getMetadataString(url, user, pass, host, port, timeout);

		if (metadataString.isEmpty())
			throw new Exception("The specified URL did not return any valid data!");
		
		Map<String, String> replacements = new HashMap<String, String>();
		final String subPackagePath = getSubPackagePath(oDataFolder);
		final String topLevelPackageName = getTopLevelPackageName();
		
		replacements.put("<<PACKAGE>>", "package " + topLevelPackageName + "." + oDataFolder + ";\n");
		replacements.put("<<NSNAME>>", nsName);
		replacements.put("<<URL>>", url);
		replacements.put("<<USER>>", user);
		replacements.put("<<PASSWORD>>", pass);
		
		if (csrfMode.equals("standard")) {
			replacements.put("<<CSRF_MODE_GET>>", ".header(\"X-CSRF-Token\", \"Fetch\")");
			replacements.put("<<CSRF_MODE_SET>>", ".header(\"X-CSRF-Token\", this.xsrfTokenValue).header(\"Cookie\", xsrfCookieName + \"=\" + xsrfCookieValue)");
		}
		else {
			replacements.put("<<CSRF_MODE_GET>>", "");
			replacements.put("<<CSRF_MODE_SET>>", ".header(\"X-Requested-With\", \"XMLHttpRequest\")");
		}

		if(host == null && port == null){

			replacements.put("<<HOST>>", "");
			replacements.put("<<PORT>>", "");	
		
		} else {
			
			replacements.put("<<HOST>>", host);
			replacements.put("<<PORT>>", port);	
			
		}
	
		GwUtils.createClassFileFromTemplate(topLevelPackageName, 
											subPackagePath, 
											"ODataNS_template.java", 
											nsName + ".java", 
											replacements, 
											fileManager, 
											getClass());
		
		GwUtils.createFileFromString(subPackagePath, 
									nsName + "_metadata.xml", 
									metadataString, 
									fileManager);
	}

	
    public void createEntity(final String endpointName, final String remoteEntitySetName) throws Exception {

    	final String subPackagePath = getSubPackagePath(oDataFolder);
    
    	// Verify if the endpoint and remote entity set are valid
    	if (!fileManager.exists(subPackagePath + SEPARATOR + endpointName + "_metadata.xml")) {
    		throw new Exception("Namespace \"" + endpointName + "\" does not exist or is corrupted. Please specify a valid namespace.");
    	}
    	else {
    		InputStream metaDataIs = fileManager.getInputStream(subPackagePath + SEPARATOR + endpointName +"_metadata.xml");
    		Document doc = XmlUtils.getDocumentBuilder().parse(metaDataIs);
    		NodeList nodeList = doc.getElementsByTagName("entity");
    		boolean remoteEntityExists = false;
    		for (int i = 0; i < nodeList.getLength(); i++){
    			Attr attr = (Attr) nodeList.item(i).getAttributes().getNamedItem("name");
    			if (attr.getValue().toString().equals(remoteEntitySetName)) {
    				remoteEntityExists = true;
    				break;
    			}
    		}
    		
    		if (!remoteEntityExists) {
    			throw new Exception("Remote entity set \"" + remoteEntitySetName + "\" does not exist. Please specify a valid remote entity.");
    		}
    	}
    	
    //  Create entity class and add JPA Annotations.
    	int modifier = Modifier.PUBLIC;
    	
    	String localEntityPath = typeLocationService.getTopLevelPackageForModule(projectOperations.getFocusedModule()) + 
    			                 "." + domain +
    			                 "." + remoteEntitySetName; 
    	
        JavaType localEntity = new JavaType(localEntityPath);
    	
        final String declaredByMetadataId = PhysicalTypeIdentifier
                .createIdentifier(localEntity,
                        pathResolver.getFocusedPath(Path.SRC_MAIN_JAVA));    	
    	
        final ClassOrInterfaceTypeDetailsBuilder cidBuilder = new ClassOrInterfaceTypeDetailsBuilder(
                declaredByMetadataId, modifier, localEntity,
                PhysicalTypeCategory.CLASS);
        
        final List<AnnotationMetadataBuilder> annotationBuilder = new ArrayList<AnnotationMetadataBuilder>();
        
        annotationBuilder.add(ROO_JAVA_BEAN_BUILDER);
        
        annotationBuilder.add(ROO_TO_STRING_BUILDER);
        
        annotationBuilder.add(ROO_JPA_ACTIVE_RECORD_BUILDER);
        
        cidBuilder.setAnnotations(annotationBuilder);
        
        typeManagementService.createOrUpdateTypeOnDisk(cidBuilder.build());
    }
    
    public void addFieldsAndMethods(final String namespace, String remoteEntity, boolean importAll) throws Exception{
    	
    //   Extract fields and keys from Metadata XML
		 Map<String, String> fields = new HashMap<String, String>();
		 Map<String, String> keys = new HashMap<String, String>();
		 Map<String, String> allFields = new HashMap<String, String>();
		  
		 String metaDataPath = getSubPackagePath(oDataFolder);
		 String metaDataFile = metaDataPath + SEPARATOR + namespace +"_metadata.xml";
		  
		 InputStream metaDataIs = fileManager.getInputStream(metaDataFile);
		  
		 Document doc;
		  
		 try {
			  doc = XmlUtils.getDocumentBuilder().parse(metaDataIs);
		 } catch (Exception ex) {
			  throw new IllegalStateException(ex);
		 }         
         
		 MetadataXMLParser xmlParser = new MetadataXMLParser(doc, remoteEntity);
		 xmlParser.parse();
		 
		 if (importAll)
			 fields = xmlParser.getFields();
		 
		 keys = xmlParser.getKeys();
         
    //   Get handler for File Editor to edit the entity file  
         JavaSourceFileEditor entityClassFile = getJavaFileEditor(domain, remoteEntity); 
 		 
 	//   Add Imports 
 		 addImports(entityClassFile, namespace);
         
    //   Add Field declaration for OData Connector
         JavaSourceField odc = new JavaSourceFieldBuilder()
											.fieldPrefix("private final static")
											.fieldType("ODataConnectivity")
											.fieldName(GwUtils.GW_CONNECTION_FIELD_NAME)
											.fieldValue("new " + namespace + "()")
											.build();

         entityClassFile.addGlobalField(odc); 
         
    //   Add Fields and corresponding getter/setters methods to the Entity Class
         Map<String, String> keysIncludingId = new HashMap<String, String>();
         // Include id as a key(type String)
         keysIncludingId.put("Id", "String");
         keysIncludingId.putAll(keys);
         
         addGatewayFields(keysIncludingId, fields, entityClassFile); 

    //   Add Persistence Methods
    // Send all the fields (keys plus fields)
         allFields.putAll(keys);
         allFields.putAll(fields);
         addPersistenceMethods(allFields, entityClassFile, remoteEntity, keys);
         
         entityClassFile.makeFile();
         //throw new Exception(entityClassFile.getFileContent());
   }
    
   public void modifyController(final String remoteEntity) throws Exception{
	
	   //  Get handler for File Editor to edit the entity file  
	    String controllerEntityName = remoteEntity + "Controller";
       	JavaSourceFileEditor controllerClassFile = getJavaFileEditor(web, controllerEntityName); 
 
       	addControllerImports(controllerClassFile);
       	
	/*
	 *  We need to overwrite the show method of the .aj file, as we need to pass the itemId to the model as URLEncoded 
	 */
		JavaSourceMethod showMethod = new JavaSourceMethodBuilder()
											.methodName("show")
											.methodPrefix("public")
											.returnType("String")
											.annotations("@RequestMapping(value = \"/{Id}\", produces = \"text/html\")")
											.parameters(getControllerShowMethodParameters())
											.methodBody(getControllerShowMethodBody(remoteEntity))
											.build();
		controllerClassFile.addMethod(showMethod);
       	
       	controllerClassFile.makeFile();
   }
    
   public void addRemoteFieldInGWClass(String localClassName, String fieldName) throws Exception {
	   //   Get handler for File Editor to edit (and search) the entity file  
       JavaSourceFileEditor entityClassFile = getJavaFileEditor(domain, localClassName); 

	   Map.Entry<String, String> fieldObj = getValidatedField(localClassName, fieldName, entityClassFile);
       
       if (fieldObj == null)
    	   throw new Exception("The name \"" + fieldName + "\" is not a valid name. Please choose a name from the provided list.");

       addRemoteFieldInGWJavaFile(entityClassFile, fieldObj);
	   addRemoteFieldInPersistenceMethods(entityClassFile, fieldObj);
	   
       entityClassFile.makeFile();
   }

   
   public void addLocalFieldInGWClass(String localClassName, String fieldName, JavaType fieldType) throws Exception {
	//   Get handler for File Editor to edit (and search) the entity file  
       JavaSourceFileEditor entityClassFile = getJavaFileEditor(domain, localClassName); 

	   Map.Entry<String, String> fieldObj = getValidatedField(localClassName, fieldName, entityClassFile);
       
       if (fieldObj != null)
    	   throw new Exception("The name \"" + fieldName + "\" is a valid name for a remote field. In order to reduce confusions, " +
    	   		"please choose another name for your local field.");

       String processedTypeName = fieldType.getNameIncludingTypeParameters();
       processedTypeName = processedTypeName.substring(processedTypeName.lastIndexOf(".") + 1);
       
       addFieldInGWJavaFile(entityClassFile, fieldName, processedTypeName);
	   addLocalFieldInPersistenceMethods(entityClassFile, fieldName, processedTypeName);
       entityClassFile.makeFile();
   }

}