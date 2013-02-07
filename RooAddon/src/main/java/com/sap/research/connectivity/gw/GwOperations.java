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

import org.springframework.roo.model.JavaType;

/**
 * Interface of commands that are available via the Roo shell.
 *
 * @since 1.1.1
 */
public interface GwOperations {

	boolean isCommandGWSetupAvailable();

	boolean isCommandODataEndpointAvailable();

	boolean isCommandGWEntityAvailable();
	
	boolean isCommandGWFieldAvailable() throws IOException;
	
	boolean isCommandGWMVCAdaptCommandAvailable() throws IOException;
	
	void addODataConnectivity();

	void addNamespace(String nsName, String url, String user, String pass, String csrfMode, String host, String port, int timeout) throws Exception;
	
	void createEntity(String endpointName, String remoteEntitySetName) throws Exception;
	
	void addFieldsAndMethods(String namespace, String remoteEntity, boolean importAll) throws Exception;
	
	void modifyController(final String remoteEntity) throws Exception;

	void addRemoteFieldInGWClass(String localClassName, String fieldName) throws Exception;

	void addLocalFieldInGWClass(String localClassName, String fieldName, JavaType fieldType) throws Exception;

}