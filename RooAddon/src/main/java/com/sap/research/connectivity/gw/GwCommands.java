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
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.classpath.operations.FieldCommands;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;



/**
 * Example of a command class. The command class is registered by the Roo shell following an
 * automatic classpath scan. You can provide simple user presentation-related logic in this
 * class. You can return any objects from each method, or use the logger directly if you'd
 * like to emit messages of different severity (and therefore different colors on 
 * non-Windows systems).
 * 
 * @since 1.1.1
 */
@Component // Use these Apache Felix annotations to register your commands class in the Roo container
@Service
public class GwCommands implements CommandMarker { // All command types must implement the CommandMarker interface
    
    /**
     * Get hold of a JDK Logger
     */
    private Logger log = Logger.getLogger(getClass().getName());

    /**
     * Get a reference to the GwOperations from the underlying OSGi container
     */
    @Reference private GwOperations operations; 
    
    /**
     * Get a reference to the StaticFieldConverter from the underlying OSGi container;
     * this is useful for 'type save' command tab completions in the Roo shell
     */
    @Reference private StaticFieldConverter staticFieldConverter;

    /**
     * The activate method for this OSGi component, this will be called by the OSGi container upon bundle activation 
     * (result of the 'addon install' command) 
     * 
     * @param context the component context can be used to get access to the OSGi container (ie find out if certain bundles are active)
     */
    protected void activate(ComponentContext context) {
    }

    /**
     * The deactivate method for this OSGi component, this will be called by the OSGi container upon bundle deactivation 
     * (result of the 'addon remove' command) 
     * 
     * @param context the component context can be used to get access to the OSGi container (ie find out if certain bundles are active)
     */
    protected void deactivate(ComponentContext context) {
    }
    
    /*
     * SAP Research GW Connectivity Commands
     */
    
    /**
     * Check if connectivity setup command is available
     */
    @CliAvailabilityIndicator("gateway setup") 
    public boolean isAddODataConnectivityCommandAvailable() {
    	//Check that project is created and Persistence is set up
        return operations.isCommandGWSetupAvailable();
    }
    
    
    /**
     * Check if defining an odata endpoint command is available
     */
    @CliAvailabilityIndicator("gateway define odata_endpoint") 
    public boolean isAddODataEndpointCommandAvailable() {
    	//Check that project is created and Persistence is set up
        return operations.isCommandODataEndpointAvailable();
    }
    
    /**
     * Check if gateway entity command is available
     */
    @CliAvailabilityIndicator("gateway entity") 
    public boolean isAddGwEntityCommandAvailable() {
    	//Check that project is created and Persistence is set up
        return operations.isCommandGWEntityAvailable();
    }
    
    /**
     * Check if connectivity gateway field / gateway local field commands are available
     */
    @CliAvailabilityIndicator({"gateway field", "gateway local field"}) 
    public boolean isAddGwFieldCommandAvailable() {
    	//Check that there is a gateway entity available
        boolean returnResults = false;
        try {
			returnResults = operations.isCommandGWFieldAvailable();
		} catch (IOException e) {
			log.severe("The \"gateway field\" command is not available at the moment. Please check the attached error.");
			e.printStackTrace();
		}
        
        return returnResults;
    }
   
    /**
     * Check if gateway mvc adapt command is available
     */
    @CliAvailabilityIndicator("gateway mvc adapt") 
    public boolean isGWMVCAdaptCommandAvailable() {
    	boolean returnResults = false;
    	try {
    		returnResults = operations.isCommandGWMVCAdaptCommandAvailable();
    	} catch (IOException e) {
			log.severe("The \"gateway mvc adapt\" command is not available at the moment. Please check the attached error.");
			e.printStackTrace();
    	}
    	return returnResults;
    }
    
    /**
     * Add Gateway Connectivity to the project
     */
    @CliCommand(value = "gateway setup", help="Add a class for handling OData Gateway Connectivity.")
    public void addODataConnectivity() {
        operations.addODataConnectivity();
    }
 
    /**
     * Define connectivity namespace
     */
    @CliCommand(value = "gateway define odata_endpoint", help="Define a namespace for a connection.")
    public void addGWNamespace(
    		@CliOption(key = "Name", mandatory = true, help = "Connection Name") String nsName,
    		@CliOption(key = "URL", mandatory = true, help = "OData Endpoint URL") String url,
    		@CliOption(key = "USER", mandatory = true, help = "Username") String user,
    		@CliOption(key = "PASSWORD", mandatory = true, help = "Password") String pass,
    		@CliOption(key = "CSRF_MODE", mandatory = false, unspecifiedDefaultValue = "standard", specifiedDefaultValue = "standard", 
			help = "The Gateway Server Mode for CSRF. Default is standard. For compatibility mode, choose compatibility.") final String csrfMode,
    		@CliOption(key = "HTTP_PROXYHOST", mandatory = false, help = "http.proxyhost", unspecifiedDefaultValue = "") String http_proxyhost,
    		@CliOption(key = "HTTP_PROXYPORT", mandatory = false, help = "http.proxyport", unspecifiedDefaultValue = "") String http_proxyport,
    		@CliOption(key = "TIMEOUT_CALL", mandatory = false, unspecifiedDefaultValue = "30", specifiedDefaultValue = "30", 
    		help = "The timeout for retrieving the metadata in seconds. Default is 30.") int timeout
    		) {
 
    	//Prerequisite validation: Check if the metadata jar is placed at the right location.
    	String metadataExtractorPath = System.getProperty("user.home") + File.separatorChar + "appToRetrieveOdataMetadata.jar";
    	File metadataExtractorJar = new File(metadataExtractorPath);
    	if(!metadataExtractorJar.exists()){
    		log.severe("The metadata extractor JAR is missing in the user-home directory");
    		return;
    	}
    	
    	try {
    		operations.addNamespace(nsName, url, user, pass, csrfMode, http_proxyhost, http_proxyport, timeout);
    		log.info("An XML file containing metadata retrieved from the Gateway service has been generated.");
        	log.info("Please check the " + nsName + "_metadata.xml file from the connectivity package for available entities and fields.");
    	} catch (Exception e) {
    		log.severe(e.getMessage());
    	}
    	
    	//FieldCommands a;
    	//a.addFieldSetJpa(fieldName, fieldType, typeName, mappedBy, notNull, nullRequired, sizeMin, sizeMax, cardinality, fetch, comment, transientModifier, permitReservedWords);
    }
    
    /**
     * Create a gw connected entity
     */
    @CliCommand(value = "gateway entity", help="Creates a local class connected to the specified OData Service Provider.")
    public void addClassGwConnectivity(
    		@CliOption(key = "namespace", optionContext = "connectivity", mandatory = true, help = "OData Endpoint URL") final GwEndpoint endPointName,
    		@CliOption(key = "remoteEntitySet", mandatory = true, help = "OData Entity Set Name") final GwRemoteEntity remoteEntitySet,
    		@CliOption(key = "import_all", mandatory = false, unspecifiedDefaultValue = "true", specifiedDefaultValue = "true", 
    				help = "Whether to import all fields from the remote entity") final boolean importAll
    		) {
    	
    	// Create a class with JPA specific annotations
    	try {
			operations.createEntity(endPointName.getName(), remoteEntitySet.getName());
	    	// Add fields, getter/setters and persistence methods
	     	operations.addFieldsAndMethods(endPointName.getName(), remoteEntitySet.getName(), importAll);
		} catch (Exception e) {
			log.severe(e.getMessage());
		}
    }
    
    /**
     * Import a remote field from a gateway connected entity
     */
    @CliCommand(value = "gateway field", help="Imports a specific field from the specified remote gateway entity.")
    public void addGwField(
    		@CliOption(key = "entityClass", mandatory = true, help = "Name of the linked entity", optionContext = "domain") final GwEntityClass localClassName,
    		@CliOption(key = "fieldName", mandatory = false, help = "Field to be imported") final GwField fieldName
    		) {
    	try { 
    		operations.addRemoteFieldInGWClass(localClassName.getName(), fieldName.getName());
    	} catch (Exception e) {
    		log.severe(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    
    /**
     * Create a local field related to a gateway connected entity
     */
    @CliCommand(value = "gateway local field", help="Create a local field in an entity which is connected to the gateway. " +
    		"The created field will be only available locally.")
    public void addLocalGwField(
    		@CliOption(key = "entityClass", mandatory = true, help = "Name of the linked entity", optionContext = "domain") final GwEntityClass localClassName,
    		@CliOption(key = "fieldName", mandatory = true, help = "Field to be created") final String fieldName,
    		@CliOption(key = "fieldType", mandatory = true, optionContext = "java-lang,java-date", help = "The type of the entity") final JavaType fieldType
    		) {
    	try { 
    		operations.addLocalFieldInGWClass(localClassName.getName(), fieldName, fieldType);
    	} catch (Exception e) {
    		log.severe(e.getMessage());
    		e.printStackTrace();
    	}
    }
    
    /**
     * Adapt the controllers to gateway connected entity
     */
    @CliCommand(value = "gateway mvc adapt", help="It adapts the controllers of the GW connected entities in order to make all the CRUD operations work. " +
    		"This command is needed ONLY for the entities having local fields mingled with remote ones.")
    public void adaptMVC(
    		@CliOption(key = "entityClass", mandatory = true, help = "Name of the linked entity", optionContext = "domain") final GwEntityClass localClassName
    		) {
    	try { 
    		// Modify the controller (because we need to overwrite the show method)
	     	operations.modifyController(localClassName.getName());
    	} catch (Exception e) {
    		log.severe(e.getMessage());
    		e.printStackTrace();
    	}
    }
       
    
}