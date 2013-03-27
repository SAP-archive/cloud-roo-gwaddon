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
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.TypeManagementService;
import org.springframework.roo.classpath.converters.JavaTypeConverter;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.classpath.operations.Cardinality;
import org.springframework.roo.classpath.operations.Fetch;
import org.springframework.roo.classpath.operations.FieldCommands;
import org.springframework.roo.file.monitor.event.FileDetails;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
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

    /**
     * Get hold of a JDK Logger
     */
    protected Logger log = Logger.getLogger(getClass().getName());
    
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
    
    public static final String web = "web";
    
    protected static final AnnotationMetadataBuilder ROO_JAVA_BEAN_BUILDER = new AnnotationMetadataBuilder(ROO_JAVA_BEAN);
    
    protected static final AnnotationMetadataBuilder ROO_TO_STRING_BUILDER = new AnnotationMetadataBuilder(ROO_TO_STRING);
    
    protected static final AnnotationMetadataBuilder ROO_JPA_ACTIVE_RECORD_BUILDER = new AnnotationMetadataBuilder(ROO_JPA_ACTIVE_RECORD);	
    
    protected static final String PERSISTENCE_XML = "META-INF/persistence.xml";
    
    private static final String ENCODED_KEY = "encodedKey";
    private static final String DECODED_KEY = "decodedKey";
	private static final String ODATA_KEY = "ODataKey";
	 
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


	public Map<String[], String> getFieldsOfRemoteEntity(String entityClassName, String nameSpace) {
		Map<String[], String> fields = new HashMap<String[], String>();
		String metaDataPath = getSubPackagePath(oDataFolder);
    	String metaDataFile = metaDataPath + SEPARATOR + nameSpace +"_metadata.xml";    
    	
    	InputStream metaDataIs = fileManager.getInputStream(metaDataFile);
    	
    	Document doc;
    	MetadataXMLParser xmlParser;
    	
		try {
			doc = XmlUtils.getDocumentBuilder().parse(metaDataIs);
			xmlParser = new MetadataXMLParser(doc, entityClassName);
			xmlParser.parse();
		} catch (Exception ex) {
			  throw new IllegalStateException(ex);
		}     	
		
		fields = xmlParser.getFields();
		return fields;
	}
	
	public void addRemoteFieldInPersistenceMethods(JavaSourceFileEditor entityClassFile, Map.Entry<String[], String> fieldObj) {
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
	
	public void addLocalFieldInPersistenceMethods(JavaSourceFileEditor entityClassFile, String fieldName, String fieldType) {
		ArrayList<JavaSourceMethod> globalMethodList = entityClassFile.getGlobalMethodList();
		String pluralRemoteEntity = GwUtils.getInflectorPlural(entityClassFile.CLASS_NAME, Locale.ENGLISH);
		String smallRemoteEntity = StringUtils.uncapitalize(entityClassFile.CLASS_NAME);
		
		for (JavaSourceMethod method : globalMethodList) {
			String methodName = method.getMethodName();
			
			/*
			 * We insert the new field in the findAll and find<Entity>Entries methods
			 */
			if (methodName.endsWith("findAll" + pluralRemoteEntity) || methodName.endsWith("find" + entityClassFile.CLASS_NAME + "Entries")) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.indexOf("virtual" + entityClassFile.CLASS_NAME + "List.add"), 
						makeLocalShowFieldCode("\t\t", smallRemoteEntity + "Instance", entityClassFile.CLASS_NAME, fieldName));
				method.setMethodBody(methodBody.toString());
			}
			
			/* NO NEED TO INSERT IN THE FIND METHOD ANYMORE AS ONCE FOUND IN THE LOCAL DB, THE LOCAL FIELDS ARE AUTOMATICALLY POPULATED
			 * We insert the new field in the find<Entity> method
			 *
			else if (methodName.endsWith("find" + entityClassFile.CLASS_NAME)) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.indexOf("return "), 
						makeLocalShowFieldCode("\t", "virtual" + entityClassFile.CLASS_NAME, entityClassFile.CLASS_NAME, fieldName));
				method.setMethodBody(methodBody.toString());
			}*/
		}
	}

	
	public void addPersistenceMethods(Map<String[], String> fields, JavaSourceFileEditor entityClassFile, String remoteEntity, Map<String[], String> keys) {
	
	    //  Persist
		JavaSourceMethod persistMethod = new JavaSourceMethodBuilder()
											.methodName("persist")
											.methodPrefix("public")
											.returnType("void")
											.annotations("@Transactional")
											.methodBody(getPersistMethodBody(fields, keys, remoteEntity))
											.build();
		entityClassFile.addMethod(persistMethod);
		
		/*
		 * localPersist - needed to store at least ID's on the local persistence container, as currently needed by local fields, references to other entities, etc.
		 * TODO: keep only the id and local fields in the persisted entity, the rest should be cleared
		 */
		JavaSourceMethod localPersistMethod = new JavaSourceMethodBuilder()
											.methodName("localPersist")
											.methodPrefix("private")
											.returnType("void")
											.annotations("@Transactional")
											.methodBody(getLocalPersistMethodBody())
											.build();
		entityClassFile.addMethod(localPersistMethod);
		

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
											.methodBody(getCountMethodBody(remoteEntity))
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
		
		/*
		 * localMerge - needed by local fields, references to other entities, etc.
		 */
		JavaSourceMethod localMerge = new JavaSourceMethodBuilder()
									.methodName("localMerge")
									.methodPrefix("private")
									.returnType(remoteEntity)
									.annotations("@Transactional")
									.methodBody(getLocalMergeMethodBody(remoteEntity))
									.build();
		entityClassFile.addMethod(localMerge);
		
	    //  remove
		JavaSourceMethod remove = new JavaSourceMethodBuilder()
											.methodName("remove")
											.methodPrefix("public")
											.returnType("void")
											.annotations("@Transactional")
											.methodBody(getRemoveMethodBody(remoteEntity))
											.build();
		entityClassFile.addMethod(remove);		
		
		/*
		 * localMerge - needed by local fields, references to other entities, etc.
		 */
		JavaSourceMethod localRemove = new JavaSourceMethodBuilder()
								.methodName("localRemove")
								.methodPrefix("public")
								.returnType("void")
								.annotations("@Transactional")
								.methodBody(getLocalRemoveMethodBody(remoteEntity))
								.build();
		entityClassFile.addMethod(localRemove);		
		
		JavaSourceMethod getRemoteEntity = new JavaSourceMethodBuilder()
								.methodName("getRemote" + remoteEntity)
								.methodPrefix("public static")
								.returnType("OEntity")
								.parameters(getFindMethodParameters())
								.methodBody(getRemoteEntityMethodBody(remoteEntity))
								.build();
		entityClassFile.addMethod(getRemoteEntity);		

	}
   
	private String getRemoteEntityMethodBody(String remoteEntity) {

		String returnString =  "\t\tOEntityKey " + ODATA_KEY + " = OEntityKey.parse( " + GwUtils.GW_CONNECTION_FIELD_NAME + ".getDecodedRemoteKey(Id));\n" +
				"\t\treturn " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntity(\"" + remoteEntity + "\", " + ODATA_KEY + ").execute();\n";
		
		return returnString;
	}

	public String getRemoveMethodBody(String remoteEntity) {
		
		String returnString = "\t\tOEntityKey " + ODATA_KEY + " = OEntityKey.parse(" + GwUtils.GW_CONNECTION_FIELD_NAME + ".getDecodedRemoteKey(Id));\n";
		
		returnString += "\t\t" + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.deleteEntity(\"" + remoteEntity +	"\", ODataKey).execute();\n" +
				"\t\tlocalRemove();\n";		

		return returnString;
	}
	
	public String getLocalRemoveMethodBody(String remoteEntity) {
		
		String returnString = "\t\tif (this.entityManager == null) this.entityManager = entityManager();\n" +
				"\t\tif (this.entityManager.contains(this)) {\n" +
				"\t\t\tthis.entityManager.remove(this);\n" +
				"\t\t} else {\n" +
				"\t\t\t" + remoteEntity + " local" + remoteEntity + " = entityManager().find(" + remoteEntity + ".class, Id);\n" +
				"\t\t\tthis.entityManager.remove(local" + remoteEntity + ");\n" +
				"\t\t}\n";		

		return returnString;
	}

	public String getMergeMethodBody(Map<String[], String> fields, String remoteEntity, Map<String[], String> keys) {
		
		String returnString = "\t\tOEntity remote" + remoteEntity + " = getRemote" + remoteEntity + "(Id);\n";
		
		returnString += "\t\tOModifyRequest<OEntity> modifyEntityRequest = " + GwUtils.GW_CONNECTION_FIELD_NAME + 
				".rooODataConsumer.updateEntity(remote" + remoteEntity +");\n";
		returnString += "\t\tboolean modifyRequest = modifyEntityRequest\n";
		
		for (Map.Entry<String[], String> field: fields.entrySet()){
			/*
			 * We do not update the key fields
			 */
			if (!keys.containsKey(field.getKey())) {
				returnString += makeGWPersistFieldCode("\t\t\t", field);
			}
		}
		
		returnString += "\t\t\t.execute();\n";

		returnString += "\t\t" + remoteEntity + " localMerged = localMerge();\n";

		returnString += "\n";
		returnString += "\t\treturn localMerged;\n";

		return returnString;
	}
	
	public String getLocalMergeMethodBody(String remoteEntity) {
		
		String returnString = "\t\tif (this.entityManager == null) this.entityManager = entityManager();\n" +
				"\t\t" + remoteEntity + " merged = this.entityManager.merge(this);\n" +
				"\t\tthis.entityManager.flush();\n" +
				"\t\treturn merged;\n";	

		return returnString;
	}

	public String getCountMethodBody(String remoteEntity) {
		
		String smallRemoteEntity = StringUtils.uncapitalize(remoteEntity);
		
		String returnString = "\t\tOQueryRequest<OEntity> " + smallRemoteEntity + "List = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntities(\"" + remoteEntity + "\");\n";
		returnString += "\t\tint i = 0;\n";
		
		returnString += "\t\tfor (OEntity " + smallRemoteEntity + "Item : " + smallRemoteEntity + "List) {\n";
		returnString += "\t\t\ti++;\n";
		returnString += "\t\t}\n";
		
		returnString += "\t\treturn i;\n";
	
		return returnString;
	}

	public String getFindMethodBody(Map<String[], String> fields, String remoteEntity) {
		
		String smallRemoteEntity = StringUtils.uncapitalize(remoteEntity);
		
		String returnString = "\t\tOEntity " + smallRemoteEntity + " = getRemote" + remoteEntity + "(Id);\n";

		returnString += "\t\t" + remoteEntity + " virtual" + remoteEntity + " = entityManager().find(" + remoteEntity + ".class, " + 
				GwUtils.GW_CONNECTION_FIELD_NAME + ".getDecodedRemoteKey(Id));\n";
						
		returnString += "\t\tif (virtual" + remoteEntity + " == null)\n" +
						"\t\t\tvirtual" + remoteEntity + " = new " + remoteEntity + "();\n";
		
		returnString += "\t\tDateTimeFormatter DTformatter = ISODateTimeFormat.dateHourMinuteSecondFraction();\n";
		returnString += "\t\tDateTimeFormatter DTOformatter = ISODateTimeFormat.dateTime();\n";		
		
		for (Map.Entry<String[], String> field: fields.entrySet()){
			returnString += makeGWShowFieldCode("\t\t", "virtual" + remoteEntity, smallRemoteEntity, field);
		}
		
		//returnString += "\t\t" + "virtual" + remoteEntity + ".setId(" + DECODED_KEY + ");\n";
		//returnString += "\t\t" + remoteEntity + " local" + remoteEntity + " = entityManager().find(" + remoteEntity + ".class, " + DECODED_KEY + ");\n";

		returnString += "\t\ttry {\n" +
				"\t\t\t\n" +
				"\t\t} catch (Exception relationshipsException) {\n" +
				"\t\t\trelationshipsException.printStackTrace();\n" +
				"\t\t};\n";
		returnString += "\t\treturn " + "virtual" + remoteEntity + ";\n";
		
		return returnString;
	}
	
	public String getControllerShowMethodBody(String remoteEntity) {
		
		String smallRemoteEntity = StringUtils.lowerCase(remoteEntity);
		String pluralRemoteEntity = GwUtils.getInflectorPlural(remoteEntity, Locale.ENGLISH);
		
		String returnString = "";
		
		if (existsDateFieldInController(remoteEntity)) {
			returnString += "\t\taddDateTimeFormatPatterns(uiModel);\n";
		}
		returnString += "\t\tuiModel.addAttribute(\"" + smallRemoteEntity + "\", " + remoteEntity + ".find" + remoteEntity + "(Id));\n";
		returnString += generateURLEncodingCode("Id", "\t\t");
		returnString += "\t\tuiModel.addAttribute(\"itemId\", " + ENCODED_KEY + ");\n";
		returnString += "\t\treturn \"" + StringUtils.lowerCase(pluralRemoteEntity) + "/show\";\n";
		
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
	
	public ArrayList<String> getControllerShowMethodParameters() {

		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add("@PathVariable(\"Id\") String Id");
		parameters.add("Model uiModel");
		return parameters;
	}

	public String getfindAllMethodBody(Map<String[], String> fields, Map<String[], String> keys, String remoteEntity) {
		
		String smallRemoteEntity = StringUtils.uncapitalize(remoteEntity);
		
		String returnString = "\t\tOQueryRequest<OEntity> " + smallRemoteEntity + "List = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntities(\"" + remoteEntity + "\");\n";	

		returnString += "\t\tList<" + remoteEntity + "> virtual" + remoteEntity + "List = " + "new ArrayList<" + remoteEntity + ">();\n";	
		
		returnString += "\t\tfor (OEntity " + smallRemoteEntity + "Item : " + smallRemoteEntity + "List) {\n";
		returnString += "\t\t\t" + remoteEntity + " " + smallRemoteEntity + "Instance = new " + remoteEntity + "();\n";
		
		returnString += "\t\t\tDateTimeFormatter DTformatter = ISODateTimeFormat.dateHourMinuteSecondFraction();\n";
		returnString += "\t\t\tDateTimeFormatter DTOformatter = ISODateTimeFormat.dateTime();\n";
		
		for (Map.Entry<String[], String> field: fields.entrySet()) {
				returnString += makeGWShowFieldCode("\t\t\t", smallRemoteEntity + "Instance", smallRemoteEntity + "Item", field);
		}
		
		String instance = smallRemoteEntity + "Instance";
		returnString += generateEncodedKey(keys, 3, instance);
		returnString += "\t\t\tString " + DECODED_KEY + " = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".getDecodedRemoteKey(" + ODATA_KEY + ".toKeyString());\n";
		
		/*
		 * Here we deal with the local part (e.g. fields, if any)
		 * (the idea is that if we browse through a remote list of entities, then we might encounter some that have not been created using 
		 * the generated backend. So then we must first persist them (at least the id's)
		 */
		returnString += "\t\t\t" + remoteEntity + " local" + remoteEntity + " = entityManager().find(" + remoteEntity + ".class, " + DECODED_KEY + ");\n";
		returnString += "\t\t\tif (local" + remoteEntity + " == null) {\n" +
				"\t\t\t\t" + remoteEntity + " tempLocal" + remoteEntity + " = new " + remoteEntity + "();\n" +
				"\t\t\t\ttempLocal" + remoteEntity + ".entityManager = entityManager();\n" +
				"\t\t\t\ttempLocal" + remoteEntity + ".setId("+ DECODED_KEY +");\n" +
				"\t\t\t\ttempLocal" + remoteEntity + ".localPersist();\n" +
				"\t\t\t\tlocal" + remoteEntity + " = entityManager().find(" + remoteEntity + ".class, " + DECODED_KEY + ");\n" +
				"\t\t\t}\n";
				
		returnString += "\t\t\ttry {\n" +
				"\t\t\t\t\n" +
				"\t\t\t} catch (Exception relationshipsException) {\n" +
				"\t\t\t\trelationshipsException.printStackTrace();\n" +
				"\t\t\t};\n";
		
		returnString += "\t\t\tvirtual" + remoteEntity + "List.add(" + smallRemoteEntity + "Instance);\n";
		returnString += "\t\t}\n";
		returnString += "\n";
		
		returnString += "\t\treturn " + "virtual" + remoteEntity + "List;\n";
		
		return returnString;
	}

   public String getPersistMethodBody(Map<String[], String> fields, Map<String[], String> keys, String remoteEntity) {
	   
		String returnString = "\t\tOEntity newEntity;\n";
		returnString += "\n";
		returnString += "\t\tOCreateRequest<OEntity> newEntityRequest = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.createEntity(\"" + 
							remoteEntity +"\");\n";
		returnString += "\n";
		returnString += "\t\tnewEntity = newEntityRequest\n";
		
		for (Map.Entry<String[], String> field: fields.entrySet()){
			returnString += makeGWPersistFieldCode("\t\t\t", field);
		}
		
		returnString += "\t\t\t.execute();\n";
		
		/*
		 * Commented, as when writing to the local persistence db we don't need urlencoding
		 */
		//returnString += generateEncodedKey(keys, 2, "");
		
		for (Map.Entry<String[], String> keyField: keys.entrySet()){
			returnString += makeGWShowFieldCode("\t\t","this", "newEntity", keyField);
		}
		
		returnString += generateDBKeyFromODataKeys(keys, "\t\t", "");
		returnString += "\t\tsetId(" + ODATA_KEY + ".toKeyString());\n";
		returnString += "\t\tlocalPersist();\n";
		return returnString;
   }
   
   public String getLocalPersistMethodBody() {
	    String returnString = "\t\tif (this.entityManager == null) this.entityManager = entityManager();\n" +
				"\t\tthis.entityManager.persist(this);\n";
		return returnString;
  }
   
   public String generateEncodedKey(Map<String[], String> keys, int numberOfTabs, String instance) {
	   
	   String returnString = "\n";
	   String tabs = "";
	   String instanceMethod = "";
	   
	   
	   for(int i=0; i < numberOfTabs; i++){
		   tabs = tabs + "\t";
	   }
	   
	   if(!instance.equals(""))
		   instanceMethod = instance + ".";
	   
	   returnString += generateDBKeyFromODataKeys(keys, tabs, instanceMethod);
	   
	   returnString += tabs + instanceMethod + "setId(" + GwUtils.GW_CONNECTION_FIELD_NAME + ".getEncodedRemoteKey(" + ODATA_KEY +".toKeyString()));\n";
	   
	   return returnString;
   }

   public String generateURLEncodingCode(String variableToEncode, String tabs) {
	   String returnString = tabs + "String " + ENCODED_KEY + " = null;\n";
	   returnString += tabs + "try {\n";
	   returnString += tabs + "\t" + ENCODED_KEY + " = URLEncoder.encode(" + variableToEncode + ",\"UTF-8\");\n";
	   returnString += tabs + "} catch (UnsupportedEncodingException e) {\n";
	   returnString += tabs + "\te.printStackTrace();\n";
	   returnString += tabs + "}\n";
	   return returnString;
   }
   
 
   private String generateDBKeyFromODataKeys(Map<String[], String> keys, String tabs, String instanceMethod) {
	String returnString;
	String keyBuilderString = "";
	String separator = "";
	for(Map.Entry<String[], String> key: keys.entrySet()){
		String remoteFieldName = key.getKey()[0];
		String localFieldName = key.getKey()[1];
		keyBuilderString += separator + "\"" + remoteFieldName + "\", ";
		keyBuilderString += instanceMethod + "get" + StringUtils.capitalize(localFieldName) + "()";
		separator = ",";
	}
   
	returnString = tabs + "OEntityKey " + ODATA_KEY + " = OEntityKey.create(" + keyBuilderString +");\n";
	return returnString;
   }
   
   public String generateDecodedKey(int numberOfTabs, String keyNameToDecode) {
	   
	   String returnString = "\n";
	   String tabs = "";
   	   for(int i=0; i < numberOfTabs; i++){
		   tabs = tabs + "\t";
	   }
	   
	   returnString += tabs + "String " + DECODED_KEY + " = \"\";\n";
	   returnString += tabs + "try{\n";
	   returnString += tabs + "\t" + DECODED_KEY + " = URLDecoder.decode(" + keyNameToDecode + ",\"UTF-8\");\n";
	   returnString += tabs + "}catch(UnsupportedEncodingException e){\n";
	   returnString += tabs + "}\n";
	   
	   return returnString;
   }   

   private String makeGWShowFieldCode(String tabLevels, String entityName, String remoteEntity, Map.Entry<String[], String> field) {
		String startCast = "", endCast = "", returnString = "";
		String remoteFieldName = field.getKey()[0];
		String localFieldName = field.getKey()[1];
		
		if (field.getValue().equals("DateTime")) {
			returnString = tabLevels + "DateTime " + localFieldName.toLowerCase() + "DT = DTformatter.parseDateTime(" + remoteEntity + 
					".getProperty(\"" + remoteFieldName  + "\").getValue().toString());\n";	
			returnString += tabLevels + "Date " + localFieldName.toLowerCase() + "ConvertedDate = " +  localFieldName.toLowerCase() + "DT.toDate();\n";
			returnString += tabLevels + entityName + ".set" + StringUtils.capitalize(localFieldName) + "(" + localFieldName.toLowerCase() + "ConvertedDate);\n";			

		} else if(field.getValue().equals("DateTimeOffset")) {
			returnString = tabLevels + "DateTime " + localFieldName.toLowerCase() + "DT = DTOformatter.parseDateTime(" + remoteEntity + 
					".getProperty(\"" + remoteFieldName  + "\").getValue().toString());\n";	
			returnString += tabLevels + "Date " + localFieldName.toLowerCase() + "ConvertedDate = " +  localFieldName.toLowerCase() + "DT.toDate();\n";
			returnString += tabLevels + entityName + ".set" + StringUtils.capitalize(localFieldName) + "(" + localFieldName.toLowerCase() + "ConvertedDate);\n";
			
		} else {
			String javaType = GwUtils.odataToJavaType(field.getValue());			
			startCast = GwUtils.generateCast(javaType);
			if (!startCast.isEmpty())
				endCast = ")";
			
			returnString =  tabLevels + entityName + ".set" + StringUtils.capitalize(localFieldName) + "(" + startCast + remoteEntity + ".getProperty(\""
				+ remoteFieldName + "\").getValue().toString()" + endCast + ");\n";
		}
		
		return returnString;
	}

   private String makeLocalShowFieldCode(String tabLevels, String entityName, String remoteEntity, String fieldName) {

		String returnString =  entityName + ".set" + StringUtils.capitalize(fieldName) + "(local" + remoteEntity + "== null ? null : local" + remoteEntity 
				+ ".get" + StringUtils.capitalize(fieldName) + "());\n" + tabLevels;
		
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
	   connectivityImports.add("java.util.Calendar");
	   connectivityImports.add("org.odata4j.core.OQueryRequest");
	   connectivityImports.add("org.odata4j.core.OCreateRequest");
	   connectivityImports.add("org.odata4j.core.OModifyRequest");
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
   
   public void addControllerImports(JavaSourceFileEditor entityClassFile){

	   ArrayList<String> connectivityImports = new ArrayList<String>();

	   connectivityImports.add("org.springframework.web.bind.annotation.PathVariable");
	   connectivityImports.add("org.springframework.ui.Model");
	   connectivityImports.add("java.net.URLEncoder");
	   connectivityImports.add("java.io.UnsupportedEncodingException");
	   
	   entityClassFile.addImports(connectivityImports);	   
   }

	public void addGatewayFields(Map<String[], String> keys, Map<String[], String> fields, JavaSourceFileEditor entityClassFile) throws Exception{

		// Add Keys 
        for (Map.Entry<String[], String> key: keys.entrySet()) {
            addKeyInGWJavaFile(keys, entityClassFile, key);		
        }		
 		 
        // Add Fields
        if (!fields.isEmpty()) {
	        for (Map.Entry<String[], String> field: fields.entrySet()) {
	        	 addRemoteFieldInGWJavaFile(entityClassFile, field);	
	     	     addRemoteFieldInPersistenceMethods(entityClassFile, field);
	          }
        }
	}

	public void addKeyInGWJavaFile(Map<String[], String> keys, JavaSourceFileEditor entityClassFile, Map.Entry<String[], String> key) {
		//Map ODataFieldTypes to JavaTypes 
		String oDataType = key.getValue();
		String javaType = GwUtils.odataToJavaType(oDataType);
		
		String localFieldName = key.getKey()[1];
		JavaSourceFieldBuilder fieldBuilder = new JavaSourceFieldBuilder()
													.fieldPrefix("private")
													.fieldType(javaType)
													.fieldName(localFieldName)
													.fieldValue("");
		
		if (localFieldName.equals("Id"))
			fieldBuilder = fieldBuilder.fieldAnnotations("@Id\n\t@Column(name = \"id\")");
		// \t@GeneratedValue(strategy = GenerationType.AUTO)\n
		
		if (key.getValue().equals("DateTime"))
			fieldBuilder = fieldBuilder.fieldAnnotations("@Temporal(TemporalType.TIMESTAMP)\n\t@DateTimeFormat(style=\"M-\")");		
		
		JavaSourceField fieldDeclaration = fieldBuilder.build();		

		entityClassFile.addGlobalField(fieldDeclaration);
		
		if (!localFieldName.equals("Id")) {
			JavaSourceMethod getMethod = new JavaSourceMethodBuilder()
												.methodName("get"+StringUtils.capitalize(localFieldName))
												.methodPrefix("public")
												.returnType(javaType)
												.methodBody("\t\t" + "return" + " " + "this." + localFieldName + ";\n")
												.build(); 
			entityClassFile.addMethod(getMethod);
			
			ArrayList<String> parameters = new ArrayList<String>();
			parameters.add(javaType + " " + localFieldName);
			
			JavaSourceMethod setMethod = new JavaSourceMethodBuilder()
												.methodName("set"+StringUtils.capitalize(localFieldName))
												.methodPrefix("public")
												.returnType("void")
											    .parameters(parameters)												
												.methodBody("\t\t" + "this." + localFieldName + " = " + localFieldName + ";\n")
												.build(); 
			
			entityClassFile.addMethod(setMethod);
		}
	}
	
	public void addRemoteFieldInGWJavaFile(JavaSourceFileEditor entityClassFile, Map.Entry<String[], String> field) throws Exception{
		
		String fieldName = field.getKey()[1];
		//Map ODataFieldTypes to JavaTypes 
		String oDataType = field.getValue();
		String javaType = GwUtils.odataToJavaType(oDataType);
		
		addFieldInGWJavaFile(entityClassFile, fieldName, javaType);
	}

	
	public void addFieldInGWJavaFile(JavaSourceFileEditor entityClassFile, String fieldName, String javaType) throws Exception {
		addFieldInGWJavaFile(entityClassFile, fieldName, "", javaType, "");
	}
	
	private void addFieldInGWJavaFile(JavaSourceFileEditor entityClassFile, String fieldName, String fieldValue, String javaType, String annotations) throws Exception {
		JavaSourceFieldBuilder fieldBuilder = new JavaSourceFieldBuilder()
									    		.fieldPrefix("private")
									    		.fieldType(javaType)
									    		.fieldName(fieldName)
									    		.fieldValue(fieldValue);
		
		if (javaType.toLowerCase().contains("date") || javaType.toLowerCase().contains("calendar"))
			annotations += "\n\t" + "@Temporal(TemporalType.TIMESTAMP)\n\t@DateTimeFormat(style=\"M-\")";
		
		fieldBuilder = fieldBuilder.fieldAnnotations(annotations);
		JavaSourceField fieldDeclaration = fieldBuilder.build();
		
		if (entityClassFile.fieldExists(fieldDeclaration.getFieldName()))
			throw new Exception("Field \"" + fieldDeclaration.getFieldName() + "\" already exists in java class file " + entityClassFile.CLASS_NAME);
     	
		entityClassFile.addGlobalField(fieldDeclaration);
      
		JavaSourceMethod getMethod = new JavaSourceMethodBuilder()
											.methodName("get"+StringUtils.capitalize(fieldName))
											.methodPrefix("public")
											.returnType(javaType)
											.methodBody("\t\t" + "return" + " " + "this." + fieldName + ";\n")
											.build(); 
		entityClassFile.addMethod(getMethod);
		
		ArrayList<String> parameters = new ArrayList<String>();
		parameters.add(javaType + " " + fieldName);
		
		JavaSourceMethod setMethod = new JavaSourceMethodBuilder()
											.methodName("set"+StringUtils.capitalize(fieldName))
											.methodPrefix("public")
											.returnType("void")
										    .parameters(parameters)												
											.methodBody("\t\t" + "this." + fieldName + " = " + fieldName + ";\n")
											.build(); 
		
		entityClassFile.addMethod(setMethod);
	}

	private String makeGWPersistFieldCode(String tabLevels, Map.Entry<String[], String> fieldObj) {
		String reversedCast = "", dateTimeOffsetCastStart = "", dateTimeOffsetCastEnd = "";
		String remoteFieldName = fieldObj.getKey()[0];
		String localFieldName = fieldObj.getKey()[1];
		if (fieldObj.getValue().equals("DateTimeOffset")) {
			reversedCast = "datetimeOffset";
			dateTimeOffsetCastStart = "new DateTime(";
			dateTimeOffsetCastEnd = ")";
		}
		else {
			reversedCast = GwUtils.generateReversedCast(GwUtils.odataToJavaType(fieldObj.getValue()));
		}
		
		return tabLevels + ".properties(OProperties." + reversedCast + "(\"" + remoteFieldName + "\", " + dateTimeOffsetCastStart + "get" 
				+ StringUtils.capitalize(localFieldName) + "()" + dateTimeOffsetCastEnd + "))\n";
	}
	
	public Map.Entry<String[], String> getValidatedField(String localClassName, String fieldName, JavaSourceFileEditor entityClassFile) throws Exception {
		Map.Entry<String[], String> fieldObj = null;
		if (!entityClassFile.fieldExists(fieldName)) {
    	   String nameSpace = GwUtils.getNamespaceFromClass(entityClassFile);
    	   Map<String[], String> fields = getFieldsOfRemoteEntity(localClassName, nameSpace);
		
    	   for(Map.Entry<String[], String> field : fields.entrySet()){
    		   if (field.getKey()[0].equals(fieldName)) {
    			   fieldObj = field;
    			   break;
    		   }
    	   }
       }
       else {
    	   throw new Exception("Field \"" + fieldName + "\" already exists in java class file " + localClassName);
       }
	   return fieldObj;
	}
	
	private boolean existsDateFieldInController(String remoteEntity) {
		SortedSet<FileDetails> files = fileManager.findMatchingAntPath(getSubPackagePath(web) + SEPARATOR + remoteEntity +"Controller_Roo_Controller*.aj");
		for (FileDetails file : files) {
			InputStream inputStream = fileManager.getInputStream(file.getCanonicalPath());
			try {
				if (IOUtils.toString(inputStream).contains(remoteEntity + "Controller.addDateTimeFormatPatterns(")) {
					IOUtils.closeQuietly(inputStream);
					return true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			IOUtils.closeQuietly(inputStream);
		}
		
		return false;
	}
	
	public void addRelationships(Map<String, String[]> relationships, String remoteEntityName, JavaSourceFileEditor entityClassFile) throws Exception {
		//TODO: still to add many to many relationships in persistence methods !!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		if (relationships.isEmpty())
			return;
		
		entityClassFile.addImport("javax.persistence.CascadeType");
		entityClassFile.addImport("org.odata4j.core.OLink");
		entityClassFile.addImport("org.odata4j.core.OLinks");
		
		for (Map.Entry<String, String[]> relation : relationships.entrySet()) {
			int currentEntityIndex = relation.getValue()[0].equals(remoteEntityName) ? 0 : 2;
			
			String fieldName = relation.getKey();
			String fieldValue = "";
			String javaType = relation.getValue()[2 - currentEntityIndex];
			
			if (getJavaFileEditor(domain, javaType, false) != null) {
			/*
			 * We process the types of association (many to many, many to one, one to one, one to many)
			 */
				String associationType = "";
				String multiplicityEnd1 = relation.getValue()[currentEntityIndex].contains("1") ? "One" : "Many";
				String multiplicityEnd2 = relation.getValue()[3 - currentEntityIndex].contains("1") ? "One" : "Many";
				associationType = multiplicityEnd1 + "To" + multiplicityEnd2;

				addRelationshipInPersistenceMethods(entityClassFile, fieldName, javaType, associationType);
				
				if (multiplicityEnd2.equals("Many")) {
					entityClassFile.addImport("java.util.HashSet");
					entityClassFile.addImport("java.util.Set");
					javaType = "Set<"+javaType+">";
					fieldValue = "new Hash"+javaType+"()";
				}
			
				entityClassFile.addImport("javax.persistence." + associationType);
				addFieldInGWJavaFile(entityClassFile, fieldName, fieldValue, javaType, "@" + associationType);
			}
		}
		
	}
	
	private void addRelationshipInPersistenceMethods(JavaSourceFileEditor entityClassFile, String nav, String javaType, String associationType) {
		// TODO Auto-generated method stub
		ArrayList<JavaSourceMethod> globalMethodList = entityClassFile.getGlobalMethodList();
		String pluralRemoteEntity = GwUtils.getInflectorPlural(entityClassFile.CLASS_NAME, Locale.ENGLISH);
		String smallRemoteEntity = StringUtils.uncapitalize(entityClassFile.CLASS_NAME);
		
		for (JavaSourceMethod method : globalMethodList) {
			String methodName = method.getMethodName();
			/*
			 * We insert the relation in the persist and merge methods
			 */
			if (methodName.endsWith("persist")) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.lastIndexOf("newEntity = newEntityRequest"), 
						makeGWPersistRelationshipCode(nav, javaType, associationType, "\t\t"));
				method.setMethodBody(methodBody.toString());
			}
			else if (methodName.endsWith("merge")) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.lastIndexOf("boolean modifyRequest = modifyEntityRequest"), 
						makeGWMergeRelationshipCode(nav, javaType, associationType, "\t\t"));
				method.setMethodBody(methodBody.toString());
			}
			
			/*
			 * We insert the relation in the findAll and find<Entity>Entries methods
			 */
			else if (methodName.endsWith("findAll" + pluralRemoteEntity) || methodName.endsWith("find" + entityClassFile.CLASS_NAME + "Entries")) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.indexOf("} catch (Exception relationshipsException)"), 
						makeGWShowRelationshipCode(entityClassFile.CLASS_NAME, smallRemoteEntity + "Instance", smallRemoteEntity + "Item", ODATA_KEY, nav, javaType, associationType, "\t\t"));
				method.setMethodBody(methodBody.toString());
			}
			/*
			 * We insert the relation in the find<Entity> method
			 */
			else if (methodName.endsWith("find" + entityClassFile.CLASS_NAME)) {
				StringBuffer methodBody = new StringBuffer(method.getMethodBody());
				methodBody.insert(methodBody.indexOf("} catch (Exception relationshipsException)"), 
						makeGWShowRelationshipCode(entityClassFile.CLASS_NAME, "virtual" + entityClassFile.CLASS_NAME, smallRemoteEntity,
								"OEntityKey.parse(" + GwUtils.GW_CONNECTION_FIELD_NAME + ".getDecodedRemoteKey(Id))", nav, javaType, associationType, "\t\t\t"));
				method.setMethodBody(methodBody.toString());
			}
		}
	}

	private String makeGWMergeRelationshipCode(String nav, String javaType, String associationType, String tabs) {
		String returnString = "";
		if ("ManyToOne OneToOne".contains(associationType)) {
			returnString += "OEntity linked" + javaType + " = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntity(\"" + 
						javaType + "\", " + javaType + ".getRemote" + javaType + "(" + nav + ".getId())).execute();\n";
			returnString += tabs + "modifyEntityRequest = modifyEntityRequest.link(\"" + nav + "\", linked" + javaType + ");\n" + tabs;
		}
		
		return returnString;
	}
	
	private String makeGWPersistRelationshipCode(String nav, String javaType, String associationType, String tabs) {
		String returnString = "";
		if ("ManyToOne OneToOne".contains(associationType)) {
			returnString += "OEntity linked" + javaType + " = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntity(\"" + 
						javaType + "\", " + javaType + ".getRemote" + javaType + "(" + nav + ".getId())).execute();\n";
			returnString += tabs + "newEntityRequest = newEntityRequest.link(\"" + nav + "\", linked" + javaType + ");\n" + tabs;
		}
		
		return returnString;
	}

	private String makeGWShowRelationshipCode(String className, String virtualLocalEntityInstance, String remoteEntity, String remoteId, String nav, String javaType, 
			String associationType, String tabs) {
		String smallClassName = StringUtils.uncapitalize(className);
		String capitalNav = StringUtils.capitalize(nav);
		String returnString = "";
		if ("ManyToOne OneToOne".contains(associationType)) {
			returnString += "OEntity remote" + javaType + " = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntity(\"" +
						className + "\", " + remoteId + ").nav(\"" + nav + 
						"\").execute();\n";
			returnString += tabs + javaType + " virtual" + javaType + " = " + javaType + ".find" + javaType + "(remote" + javaType + 
						".getEntityKey().toKeyString());\n" ;
			returnString += tabs + virtualLocalEntityInstance + ".set" + javaType + "(virtual" + javaType + ");\n" + tabs;
		} else {
			returnString += "List<OLink> " + smallClassName + "Links = " + remoteEntity + ".getLinks();\n" +
					tabs + "\tfor(OLink " + smallClassName + "Link : " + smallClassName + "Links) {\n" +
					tabs + "\t\tif (" + smallClassName + "Link.isCollection())\n";
			
			returnString +=	tabs + "\t\t\tif (" + smallClassName + "Link.getTitle().equals(\"" + nav + "\")) {\n" +
					tabs + "\t\t\t\tSet<" + javaType + "> virtual" + javaType + "List = new HashSet<" + javaType + ">();\n" +
					tabs + "\t\t\t\tList<OEntity> remote" + javaType + "List = " + GwUtils.GW_CONNECTION_FIELD_NAME + ".rooODataConsumer.getEntities(" +
							"OLinks.relatedEntities(" + smallClassName + "Link.getRelation(), " + smallClassName + "Link.getTitle(), " + 
							smallClassName + "Link.getHref()))\n" +
					tabs + "\t\t\t\t.execute().toList();\n" +
					tabs + "\t\t\t\tfor (OEntity remote" + javaType + " : remote" + javaType + "List) {\n" +
					tabs + "\t\t\t\t\t" + javaType + " virtual" + javaType + " = " + javaType + ".find" + javaType + "(remote" + javaType + 
							".getEntityKey().toKeyString());\n" +
					tabs + "\t\t\t\t\tvirtual" + javaType + "List.add(virtual" + javaType + ");\n" +
					tabs + "\t\t\t\t}\n" +
					tabs + "\t\t\t\t" + virtualLocalEntityInstance + ".set" + capitalNav + "(virtual" + javaType + "List);\n" +
					tabs + "\t\t\t}\n" +
					tabs + "}\n" + tabs;
		}
		
		return returnString;
	}
}
