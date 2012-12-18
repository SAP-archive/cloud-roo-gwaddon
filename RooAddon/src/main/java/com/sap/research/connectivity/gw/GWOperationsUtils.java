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

import static org.springframework.roo.model.RooJavaType.ROO_JAVA_BEAN;
import static org.springframework.roo.model.RooJavaType.ROO_JPA_ACTIVE_RECORD;
import static org.springframework.roo.model.RooJavaType.ROO_TO_STRING;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sap.research.connectivity.gw.parsers.JavaSourceField;
import com.sap.research.connectivity.gw.parsers.JavaSourceFieldBuilder;
import com.sap.research.connectivity.gw.parsers.JavaSourceFileEditor;
import com.sap.research.connectivity.gw.parsers.JavaSourceMethod;
import com.sap.research.connectivity.gw.parsers.JavaSourceMethodBuilder;
import com.sap.research.connectivity.gw.parsers.MetadataXMLParser;

@Component
public class GWOperationsUtils {

	protected static final char SEPARATOR = GwUtils.SEPARATOR;

    /**
     * Get a reference to the FileManager from the underlying OSGi container. Make sure you
     * are referencing the Roo bundle which contains this service in your add-on pom.xml.
     * 
     * Using the Roo file manager instead if java.io.File gives you automatic rollback in case
     * an Exception is thrown.
     */
    @Reference protected FileManager fileManager;
    
    /**
     * Get a reference to the ProjectOperations from the underlying OSGi container. Make sure you
     * are referencing the Roo bundle which contains this service in your add-on pom.xml.
     */
    @Reference protected ProjectOperations projectOperations;

    /**
     * Use TypeLocationService to find types which are annotated with a given annotation in the project
     */
    @Reference protected TypeLocationService typeLocationService;
    
    @Reference protected PathResolver pathResolver;
    
    @Reference protected TypeManagementService typeManagementService;    
    
    
    /* ------------------------------------------------------------------------------------------------------------------
     * SAP RESEARCH OWN CODE
     * 
     * 
     */
    
    /*
     * Used for sub-package path of the connectivity classes
     */
    public static final String oDataFolder = "connectivity";
	
    public static final String domain = "domain";
    
    protected static final AnnotationMetadataBuilder ROO_JAVA_BEAN_BUILDER = new AnnotationMetadataBuilder(ROO_JAVA_BEAN);
    
    protected static final AnnotationMetadataBuilder ROO_TO_STRING_BUILDER = new AnnotationMetadataBuilder(ROO_TO_STRING);
    
    protected static final AnnotationMetadataBuilder ROO_JPA_ACTIVE_RECORD_BUILDER = new AnnotationMetadataBuilder(ROO_JPA_ACTIVE_RECORD);	
    
    protected static final String PERSISTENCE_XML = "META-INF/persistence.xml";
	 
	public void addODataDependenciesToPom() {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		
		// Install dependencies defined in external XML file
        for (Element dependencyElement : XmlUtils.findElements("/configuration/batch/dependencies/dependency", XmlUtils.getConfiguration(getClass()))) {
            dependencies.add(new Dependency(dependencyElement));
        }

        // Add all new dependencies to pom.xml
        projectOperations.addDependencies("", dependencies);	
	}

	public void addOdataConnectivityClass() {
		Map<String, String> replacements = new HashMap<String, String>();
		replacements.put("<<PACKAGE>>", "package " + getTopLevelPackageName() + "." + oDataFolder + ";\n");
		
		GwUtils.createClassFileFromTemplate(getTopLevelPackageName(), getSubPackagePath(oDataFolder), "ODataConnectivity_template.java", 
				"ODataConnectivity.java", replacements, fileManager, getClass());
	}

	/*
	 * This method should dump all the metadata existing at the specified url.
	 * It calls an external standalone java app, which should be present in user home (regardless of OS)
	 */
	public String getMetadataString(String url, String user, String pass, String host, String port, int timeOut) throws Exception {
	      String returnString = "";
	 
	      try {
	    	  String execArgs[] = new String[] {"java", "-jar", 
	    			  System.getProperty("user.home") + SEPARATOR + "appToRetrieveOdataMetadata.jar",
	    			  url, user, pass, host, port}; 
	    	  
	    	  final Process theProcess = Runtime.getRuntime().exec(execArgs);
			
	    	  Callable<String> call = new Callable<String>() {
                public String call() throws Exception {
                	String returnString = "";
                	try {
                		BufferedReader inStream = new BufferedReader(new InputStreamReader( theProcess.getInputStream() ));
            			returnString = IOUtils.toString(inStream);
            			IOUtils.closeQuietly(inStream);
            			//if (theProcess.exitValue() != 0)
            				theProcess.waitFor();
                	} catch (InterruptedException e) {
                		throw new TimeoutException();
                		//log.severe("The call to the Gateway Service was interrupted.");
                	} 
               		return returnString;
                }
            };

            final ExecutorService theExecutor = Executors.newSingleThreadExecutor();
			Future<String> futureResultOfCall = theExecutor.submit(call);
            try
            {
            	returnString = futureResultOfCall.get(timeOut, TimeUnit.SECONDS);
            }
            catch (TimeoutException ex)
            {
            	throw new TimeoutException("The Gateway Service call timed out. Please try again or check your settings.");
            }
            catch (ExecutionException ex)
            {
            	throw new RuntimeException("The Gateway Service call did not complete due to an execution error. " + ex.getCause().getLocalizedMessage());
            }
            finally {
            	theExecutor.shutdownNow();
            }
	      } catch (InterruptedException ex) {
	         throw new InterruptedException("The Gateway Service call did not complete due to an unexpected interruption.");
	      } catch(IOException e) {
	         throw new IOException("Error when retrieving metadata from the Gateway Service.");
	      }
	      
	      return returnString;
	}

    public String getTopLevelPackageName() {
    	return projectOperations.getFocusedTopLevelPackage().getFullyQualifiedPackageName();
    }

	public String getSubPackagePath(String subPackageName) {
		String packagePath = getTopLevelPackageName().replace('.', SEPARATOR);
   		PathResolver pathResolver = projectOperations.getPathResolver();
   		String path = pathResolver.getFocusedIdentifier(Path.SRC_MAIN_JAVA, packagePath + SEPARATOR + subPackageName);
		return path;
	}
   
	public JavaSourceFileEditor getJavaFileEditor(final String subPackagePathName, final String className) {
		return getJavaFileEditor(subPackagePathName, className, true);
	}
	
	public JavaSourceFileEditor getJavaFileEditor(final String subPackagePathName, final String className, final boolean createIfNotExists) {
		String entityClassPath = getSubPackagePath(subPackagePathName) + SEPARATOR + className + ".java";
		MutableFile sourceEntityFile = null;
		JavaSourceFileEditor entityClassFile = null;
		
		if (fileManager.exists(entityClassPath)) {
			sourceEntityFile = fileManager.updateFile(entityClassPath);
			entityClassFile = new JavaSourceFileEditor(sourceEntityFile);
		}
		else if (createIfNotExists) {
			sourceEntityFile = fileManager.createFile(entityClassPath);
	 		entityClassFile = new JavaSourceFileEditor(sourceEntityFile);
		}
		return entityClassFile;
	}


	public Map<String, String> getFieldsOfRemoteEntity(String entityClassName, String nameSpace) {
		Map<String, String> fields = new HashMap<String, String>();
		String metaDataPath = getSubPackagePath(oDataFolder);
    	String metaDataFile = metaDataPath + SEPARATOR + nameSpace +"_metadata.xml";    
    	
    	InputStream metaDataIs = fileManager.getInputStream(metaDataFile);
    	
    	Document doc;
    	
		try {
			doc = XmlUtils.getDocumentBuilder().parse(metaDataIs);
		} catch (Exception ex) {
			  throw new IllegalStateException(ex);
		}      	
    	
		MetadataXMLParser xmlParser = new MetadataXMLParser(doc, entityClassName);
		xmlParser.parse();
		fields = xmlParser.getFields();
		return fields;
	}
	
	public void addFieldInPersistenceMethods(JavaSourceFileEditor entityClassFile, Map.Entry<String, String> fieldObj) {
		ArrayList<JavaSourceMethod> globalMethodList = entityClassFile.getGlobalMethodList();
		String pluralRemoteEntity = GwUtils.getInflectorPlural(entityClassFile.CLASS_NAME, Locale.ENGLISH);
		String smallRemoteEntity = StringUtils.uncapitalize(entityClassFile.CLASS_NAME);
		
		for (JavaSourceMethod method : globalMethodList) {
			String methodName = method.getMethodName();
			/*
			 * We insert the new field in the persist and merge methods
			 */
			if (methodName.endsWith("persist") || methodName.endsWith("merge")) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.lastIndexOf(".execute()"), makeGWPersistFieldCode("", fieldObj));
				method.setMethodBody(methodBody.toString());
			}
			/*
			 * We insert the new field in the findAll and find<Entity>Entries methods
			 */
			else if (methodName.endsWith("findAll" + pluralRemoteEntity) || methodName.endsWith("find" + entityClassFile.CLASS_NAME + "Entries")) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.indexOf("virtual" + entityClassFile.CLASS_NAME + "List.add"), 
						makeGWShowFieldCode("", smallRemoteEntity + "Instance", smallRemoteEntity + "Item", fieldObj));
				method.setMethodBody(methodBody.toString());
			}
			/*
			 * We insert the new field in the find<Entity> method
			 */
			else if (methodName.endsWith("find" + entityClassFile.CLASS_NAME)) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.indexOf("return "), 
						makeGWShowFieldCode("", "virtual" + entityClassFile.CLASS_NAME, smallRemoteEntity, fieldObj));
				method.setMethodBody(methodBody.toString());
			}
		}
	}

	
	public void addPersistenceMethods(Map<String, String> fields, JavaSourceFileEditor entityClassFile, String remoteEntity, Map<String, String> keys) {
		
	    //  Persist
		JavaSourceMethod persistMethod = new JavaSourceMethodBuilder()
											.methodName("persist")
											.methodPrefix("public")
											.returnType("void")
											.annotations("@Transactional")
											.methodBody(getPersistMethodBody(fields, keys, remoteEntity))
											.build();

		entityClassFile.addMethod(persistMethod);

		//  findAll
		String pluralRemoteEntity = GwUtils.getInflectorPlural(remoteEntity, Locale.ENGLISH);
		JavaSourceMethod findAllMethod = new JavaSourceMethodBuilder()
											.methodName("findAll" + pluralRemoteEntity)
											.methodPrefix("public static")
											.returnType("List" + "<" + remoteEntity + ">")
											.methodBody(getfindAllMethodBody(fields, keys, remoteEntity))
											.build();

		entityClassFile.addMethod(findAllMethod);	
		
		//  findEntries
		JavaSourceMethod findEntries = new JavaSourceMethodBuilder()
											.methodName("find" + remoteEntity + "Entries")
											.methodPrefix("public static")
											.returnType("List" + "<" + remoteEntity + ">")
											.parameters(getFindEntriesMethodParameters())
											.methodBody(getfindAllMethodBody(fields, keys, remoteEntity))
											.build();	
		
		entityClassFile.addMethod(findEntries);

		//  find
		JavaSourceMethod find = new JavaSourceMethodBuilder()
											.methodName("find" + remoteEntity)
											.methodPrefix("public static")
											.returnType(remoteEntity)
											.parameters(getFindMethodParameters())
											.methodBody(getFindMethodBody(fields, remoteEntity))
											.build();	
		
		entityClassFile.addMethod(find);

		//  count
		JavaSourceMethod count = new JavaSourceMethodBuilder()
											.methodName("count" + pluralRemoteEntity)
											.methodPrefix("public static")
											.returnType("long")
											.methodBody(getCountMethodBody(fields, remoteEntity))
											.build();	
		
		entityClassFile.addMethod(count);
		
	    //  merge
		JavaSourceMethod merge = new JavaSourceMethodBuilder()
											.methodName("merge")
											.methodPrefix("public")
											.returnType(remoteEntity)
											.annotations("@Transactional")
											.methodBody(getMergeMethodBody(fields, remoteEntity, keys))
											.build();

		entityClassFile.addMethod(merge);
		
	    //  remove
		JavaSourceMethod remove = new JavaSourceMethodBuilder()
											.methodName("remove")
											.methodPrefix("public")
											.returnType("void")
											.annotations("@Transactional")
											.methodBody(getRemoveMethodBody(fields, remoteEntity, keys))
											.build();

		entityClassFile.addMethod(remove);		
		
	}
   
	public String getRemoveMethodBody(Map<String, String> fields, String remoteEntity, Map<String, String> keys) {
		
		String returnString = "\n";
		
		returnString += generateDecodedKey(2);

		returnString += "\t\t" + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.deleteEntity(\"" + remoteEntity +	"\", ODataKey).execute();\n";		

		return returnString;
		
	}

	public String getMergeMethodBody(Map<String, String> fields, String remoteEntity, Map<String, String> keys) {
		
		String returnString = "\n";
		
		returnString += generateDecodedKey(2);
		
		returnString += "\t\tOEntity remote" + remoteEntity + " = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntity(\"" + remoteEntity + 
				"\", ODataKey).execute();\n";	
		
		returnString += "\t\tboolean modifyRequest = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.updateEntity(remote" + remoteEntity +")\n";
		
		for (Map.Entry<String, String> field: fields.entrySet()){
			/*
			 * We do not update the key fields
			 */
			if (!keys.containsKey(field.getKey())) {
				returnString += makeGWPersistFieldCode("\t\t\t", field);
			}
		}
		
		returnString += "\t\t\t.execute();\n";
		
		returnString += "\n";
		returnString += "\t\treturn this;\n";

		return returnString;

	}

	public String getCountMethodBody(Map<String, String> fields, String remoteEntity) {
		
		String returnString = "\n";
		String smallRemoteEntity = StringUtils.uncapitalize(remoteEntity);
		
		returnString += "\t\tOQueryRequest<OEntity> " + smallRemoteEntity + "List = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntities(\"" + remoteEntity + "\");\n";
		returnString += "\t\tint i = 0;\n";
		
		returnString += "\t\tfor (OEntity " + smallRemoteEntity + "Item : " + smallRemoteEntity + "List) {\n";
		returnString += "\t\t\ti++;\n";
		returnString += "\t\t}\n";
		
		returnString += "\t\treturn i;\n";
	
		return returnString;

	}

	public String getFindMethodBody(Map<String, String> fields, String remoteEntity) {
		
		String returnString = "\n";
		
		String smallRemoteEntity = StringUtils.uncapitalize(remoteEntity);
		
		returnString += generateDecodedKey(2);
		
		returnString += "\t\tOEntity " + smallRemoteEntity + " = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntity(\"" + remoteEntity + "\", ODataKey).execute();\n";

		returnString += "\t\t" + remoteEntity + " virtual" + remoteEntity + " = new " + remoteEntity + "();\n";
		
		returnString += "\t\tDateTimeFormatter DTformatter = ISODateTimeFormat.dateHourMinuteSecondFraction();\n";
		returnString += "\t\tDateTimeFormatter DTOformatter = ISODateTimeFormat.dateTime();\n";		
		
		for (Map.Entry<String, String> field: fields.entrySet()){
			returnString += makeGWShowFieldCode("\t\t", "virtual" + remoteEntity, smallRemoteEntity, field);
		}
		
		returnString += "\t\t" + "virtual" + remoteEntity + ".setId(Id);\n";
		
		returnString += "\t\treturn " + "virtual" + remoteEntity + ";\n";
		
		return returnString;

	}

	public ArrayList<String> getFindMethodParameters() {

		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("String Id");
		return parameters;
		
	}

	public ArrayList<String> getFindEntriesMethodParameters() {

		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("int firstResult");
		parameters.add("int maxResults");
		return parameters;

	}

	public String getfindAllMethodBody(Map<String, String> fields, Map<String, String> keys, String remoteEntity) {
		
		String returnString = "\n";
		String smallRemoteEntity = StringUtils.uncapitalize(remoteEntity);
		
		returnString += "\t\tOQueryRequest<OEntity> " + smallRemoteEntity + "List = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntities(\"" + remoteEntity + "\");\n";	
		
		returnString += "\t\tList<" + remoteEntity + "> virtual" + remoteEntity + "List = " + "new ArrayList<" + remoteEntity + ">();\n";	
		
		returnString += "\t\tfor (OEntity " + smallRemoteEntity + "Item : " + smallRemoteEntity + "List) {\n";
		returnString += "\t\t\t" + remoteEntity + " " + smallRemoteEntity + "Instance = new " + remoteEntity + "();\n";
		
		returnString += "\t\t\tDateTimeFormatter DTformatter = ISODateTimeFormat.dateHourMinuteSecondFraction();\n";
		returnString += "\t\t\tDateTimeFormatter DTOformatter = ISODateTimeFormat.dateTime();\n";
		
		for (Map.Entry<String, String> field: fields.entrySet()) {
				returnString += makeGWShowFieldCode("\t\t\t", smallRemoteEntity + "Instance", smallRemoteEntity + "Item", field);
		}

		String instance = smallRemoteEntity + "Instance";
		returnString += generateEncodedKey(keys, 3, instance);
		
		returnString += "\t\t\tvirtual" + remoteEntity + "List.add(" + smallRemoteEntity + "Instance);\n";
		returnString += "\t\t}\n";
		returnString += "\n";
		
		returnString += "\t\treturn " + "virtual" + remoteEntity + "List;\n";
		
		return returnString;
		
	}

   public String getPersistMethodBody(Map<String, String> fields, Map<String, String> keys, String remoteEntity) {
	   
	    String returnString = "\n";
	   
		returnString += "\t\tOEntity newEntity;\n";
		returnString += "\n";
		returnString += "\t\tnewEntity = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.createEntity(\"" + remoteEntity +"\")\n";
		
		for (Map.Entry<String, String> field: fields.entrySet()){
			returnString += makeGWPersistFieldCode("\t\t\t", field);
		}
		
		returnString += "\t\t\t.execute();\n";
		
		returnString += generateEncodedKey(keys, 2, "");
		
		return returnString;
   }
   
   public String generateEncodedKey(Map<String, String> keys, int numberOfTabs, String instance) {
	   
	   String returnString = "\n";
	   String tabs = "";
	   String keyBuilderString = "";
	   String instanceMethod = "";
	   String seperator = "";
	   
	   for(int i=0; i < numberOfTabs; i++){
		   tabs = tabs + "\t";
	   }
	   
	   if(!instance.equals(""))
		   instanceMethod = instance + ".";
	   
	   for(Map.Entry<String, String> key: keys.entrySet()){
		   
		   keyBuilderString += seperator + "\"" + key.getKey() + "\", ";
		   keyBuilderString += instanceMethod + "get" + key.getKey() + "().toString()";
		   
		   seperator = ",";
		   
	   }
	   
	   returnString += tabs + "OEntityKey ODataKey = OEntityKey.create(" + keyBuilderString +");\n";
	   
	   returnString += tabs + "String encodeKey = null;\n";
	   returnString += tabs + "try {\n";
	   returnString += tabs + "\tencodeKey = URLEncoder.encode(ODataKey.toKeyString(),\"UTF-8\");\n";
	   returnString += tabs + "} catch (UnsupportedEncodingException e) {\n";
	   returnString += tabs + "}\n";
	   
	   returnString += tabs + instanceMethod + "setId(encodeKey);\n";
	   
	   return returnString;
   }
   
   public String generateDecodedKey(int numberOfTabs) {
	   
	   String returnString = "\n";
	   String tabs = "";
   	   for(int i=0; i < numberOfTabs; i++){
		   tabs = tabs + "\t";
	   }
	   
	   returnString += tabs + "String decodeKey = \"\";\n";
	   returnString += tabs + "try{\n";
	   returnString += tabs + "\tdecodeKey = URLDecoder.decode(Id,\"UTF-8\");\n";
	   returnString += tabs + "}catch(UnsupportedEncodingException e){\n";
	   returnString += tabs + "}\n";
	   
	   returnString += tabs + "OEntityKey ODataKey = OEntityKey.parse(decodeKey);\n";
	   
	   return returnString;
   }   

   private String makeGWShowFieldCode(String tabLevels, String entityName, String remoteEntity, Map.Entry<String, String> field) {
		String startCast = "", endCast = "", returnString = "";
		String fieldName = field.getKey();
		
		if (field.getValue().equals("DateTime")) {
			returnString = tabLevels + "DateTime " + fieldName.toLowerCase() + "DT = DTformatter.parseDateTime(" + remoteEntity + 
					".getProperty(\"" + fieldName  + "\").getValue().toString());\n";	
			returnString += tabLevels + "Date " + fieldName.toLowerCase() + "ConvertedDate = " +  fieldName.toLowerCase() + "DT.toDate();\n";
			returnString += tabLevels + entityName + ".set" + fieldName + "(" + fieldName.toLowerCase() + "ConvertedDate);\n";			

		} else if(field.getValue().equals("DateTimeOffset")) {
			returnString = tabLevels + "DateTime " + fieldName.toLowerCase() + "DT = DTOformatter.parseDateTime(" + remoteEntity + 
					".getProperty(\"" + fieldName  + "\").getValue().toString());\n";	
			returnString += tabLevels + "Date " + fieldName.toLowerCase() + "ConvertedDate = " +  fieldName.toLowerCase() + "DT.toDate();\n";
			returnString += tabLevels + entityName + ".set" + fieldName + "(" + fieldName.toLowerCase() + "ConvertedDate);\n";
			
		} else {
			String javaType = GwUtils.odataToJavaType(field.getValue());			
			startCast = GwUtils.generateCast(javaType);
			if (!startCast.isEmpty())
				endCast = ")";
			
			returnString =  tabLevels + entityName + ".set" + fieldName + "(" + startCast + remoteEntity + ".getProperty(\""
				+ fieldName + "\").getValue().toString()" + endCast + ");\n";
		}
		
		return returnString;
	}

   public void addImports(JavaSourceFileEditor entityClassFile, String namespace){

	   ArrayList<String> connectivityImports = new ArrayList<String>();

	   connectivityImports.add(getTopLevelPackageName() + ".connectivity." + namespace);
	   connectivityImports.add(getTopLevelPackageName() + ".connectivity.ODataConnectivity");
	   
	   connectivityImports.add("org.odata4j.core.OEntity");
	   connectivityImports.add("org.odata4j.core.OProperties");
	   
	   connectivityImports.add("org.springframework.transaction.annotation.Transactional");
	   
	   connectivityImports.add("java.util.List");
	   connectivityImports.add("java.util.ArrayList");
	   connectivityImports.add("java.util.Date");
	   connectivityImports.add("org.odata4j.core.OQueryRequest");
	   
	   connectivityImports.add("org.odata4j.core.OEntityKey");
	   
	   connectivityImports.add("javax.persistence.Temporal");
	   connectivityImports.add("javax.persistence.TemporalType");

	   connectivityImports.add("org.springframework.format.annotation.DateTimeFormat");
	   
	   connectivityImports.add("org.joda.time.DateTime");
	   connectivityImports.add("org.joda.time.format.DateTimeFormatter");
	   connectivityImports.add("org.joda.time.format.ISODateTimeFormat");	
	   
	   connectivityImports.add("org.odata4j.core.OEntityKey");

	   connectivityImports.add("javax.persistence.Column");			   
	   connectivityImports.add("javax.persistence.GenerationType");
	   connectivityImports.add("javax.persistence.GeneratedValue");
	   
	   connectivityImports.add("java.net.URLDecoder");			   
	   connectivityImports.add("java.net.URLEncoder");
	   connectivityImports.add("java.io.UnsupportedEncodingException");	   
	   connectivityImports.add("javax.persistence.Id");
	   
	   entityClassFile.addImports(connectivityImports);	   
	   
   }

	public void addGatewayFields(Map<String, String> keys, Map<String, String> fields, JavaSourceFileEditor entityClassFile) {

		// Add Keys 
        for (Map.Entry<String, String> key: keys.entrySet()) {
            addKeyInGWJavaFile(keys, entityClassFile, key);		
        }		
 		 
        // Add Fields
        if (!fields.isEmpty()) {
	        for (Map.Entry<String, String> field: fields.entrySet()) {
	        	 addFieldInGWJavaFile(entityClassFile, field);	
	     	     addFieldInPersistenceMethods(entityClassFile, field);
	          }
        }
	}

	public void addKeyInGWJavaFile(Map<String, String> keys, JavaSourceFileEditor entityClassFile, Map.Entry<String, String> key) {
		//Map ODataFieldTypes to JavaTypes 
		String oDataType = key.getValue();
		String javaType = GwUtils.odataToJavaType(oDataType);
		
		JavaSourceFieldBuilder fieldBuilder = new JavaSourceFieldBuilder()
													.fieldPrefix("private")
													.fieldType(javaType)
													.fieldName(key.getKey())
													.fieldValue("");
		
		if (key.getKey().equals("Id"))
			fieldBuilder = fieldBuilder.fieldAnnotations("@Id\n\t@Column(name = \"id\")");
		// \t@GeneratedValue(strategy = GenerationType.AUTO)\n
		
		if (key.getValue().equals("DateTime"))
			fieldBuilder = fieldBuilder.fieldAnnotations("@Temporal(TemporalType.TIMESTAMP)\n\t@DateTimeFormat(style=\"M-\")");		
		
		JavaSourceField fieldDeclaration = fieldBuilder.build();		

		entityClassFile.addGlobalField(fieldDeclaration);
      
		if (!key.getKey().equals("Id")) {
			JavaSourceMethod getMethod = new JavaSourceMethodBuilder()
												.methodName("get"+key.getKey())
												.methodPrefix("public")
												.returnType(javaType)
												.methodBody("\t\t" + "return" + " " + "this." + key.getKey() + ";\n")
												.build(); 
			entityClassFile.addMethod(getMethod);
			
			ArrayList<String> parameters = new ArrayList<String>();
			parameters.add(javaType + " " + key.getKey());
			
			JavaSourceMethod setMethod = new JavaSourceMethodBuilder()
												.methodName("set"+key.getKey())
												.methodPrefix("public")
												.returnType("void")
											    .parameters(parameters)												
												.methodBody("\t\t" + "this." + key.getKey() + " = " + key.getKey() + ";\n")
												.build(); 
			
			entityClassFile.addMethod(setMethod);
		}
	}
	
	public void addFieldInGWJavaFile(JavaSourceFileEditor entityClassFile, Map.Entry<String, String> field) throws Error{
		
		//Map ODataFieldTypes to JavaTypes 
		String oDataType = field.getValue();
		String javaType = GwUtils.odataToJavaType(oDataType);
		
		JavaSourceFieldBuilder fieldBuilder = new JavaSourceFieldBuilder()
									    		.fieldPrefix("private")
									    		.fieldType(javaType)
									    		.fieldName(field.getKey())
									    		.fieldValue("");
		
		if (field.getValue().equals("DateTime") || field.getValue().equals("DateTimeOffset"))
			fieldBuilder = fieldBuilder.fieldAnnotations("@Temporal(TemporalType.TIMESTAMP)\n\t@DateTimeFormat(style=\"M-\")");
		
		JavaSourceField fieldDeclaration = fieldBuilder.build();
		
		if (entityClassFile.fieldExists(fieldDeclaration.getFieldName()))
			throw new Error("Field \"" + fieldDeclaration.getFieldName() + "\" already exists in java class file " + entityClassFile.CLASS_NAME);
     	
		entityClassFile.addGlobalField(fieldDeclaration);
      
		JavaSourceMethod getMethod = new JavaSourceMethodBuilder()
											.methodName("get"+field.getKey())
											.methodPrefix("public")
											.returnType(javaType)
											.methodBody("\t\t" + "return" + " " + "this." + field.getKey() + ";\n")
											.build(); 
		entityClassFile.addMethod(getMethod);
		
		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add(javaType + " " + field.getKey());
		
		JavaSourceMethod setMethod = new JavaSourceMethodBuilder()
											.methodName("set"+field.getKey())
											.methodPrefix("public")
											.returnType("void")
										    .parameters(parameters)												
											.methodBody("\t\t" + "this." + field.getKey() + " = " + field.getKey() + ";\n")
											.build(); 
		
		entityClassFile.addMethod(setMethod);
	}

	private String makeGWPersistFieldCode(String tabLevels, Map.Entry<String, String> fieldObj) {
		String reversedCast = "", dateTimeOffsetCastStart = "", dateTimeOffsetCastEnd = "";
		if (fieldObj.getValue().equals("DateTimeOffset")) {
			reversedCast = "datetimeOffset";
			dateTimeOffsetCastStart = "new DateTime(";
			dateTimeOffsetCastEnd = ")";
		}
		else {
			reversedCast = GwUtils.generateReversedCast(GwUtils.odataToJavaType(fieldObj.getValue()));
		}
		
		return tabLevels + ".properties(OProperties." + reversedCast + "(\"" + fieldObj.getKey() + "\", " + dateTimeOffsetCastStart + "get" 
				+ StringUtils.capitalize(fieldObj.getKey()) + "()" + dateTimeOffsetCastEnd + "))\n";
	}
	
	
}
