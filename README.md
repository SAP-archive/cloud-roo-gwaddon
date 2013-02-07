SAP NetWeaver Gateway Connectivity Addon for Spring Roo
=======================================================

Introduction
------------

### What is it about? ###

[Spring Roo](http://www.springsource.org/spring-roo) is a console-based Rapid Application Development (RAD) tool maintained by [SpringSource](http://www.springsource.org/) and made available to the public under the Apache License 2.0. Roo can be extended by addons, that provide additional functionality to the tool.

This project, the "SAP NetWeaver Gateway Connectivity Addon for Spring Roo" is an addon for Roo which adds commands to Roo that make it easy and comfortable to
connect your Roo-generated projects to "SAP Netweaver Gateway" systems. This addon is also provided under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

You can use Roo and the provided addon here to create in minutes a basic web application having (selected) entities connected to entities exposed by "SAP NetWeaver Gateway" systems. Roo generates 100% pure Java code, packaged as a standard Maven project, so it is easy to handle and extend. Import it in your favorite IDE like Eclipse and start extending it, or write a mobile app connecting to the REST interfaces to your data, that Roo can automatically create for you.

In addition, you may use the [SAP NetWeaver Cloud Addon for Spring Roo](http://sapnwcloudlabs.github.com/nwcloud-roo-addon/) to deploy your application to [SAP NetWeaver Cloud](http://scn.sap.com/community/developer-center/cloud-platform), which is a Java-based Platform-as-a-Service (PaaS) provided by [SAP AG](http://www.sap.com/). Currently SAP AG provides a [free trial](http://scn.sap.com/docs/DOC-28197) of SAP NetWeaver Cloud, so it's easy to give it a try.

Whether you intend to create a REST back-end application which aggregates information from multiple data sources, or simply need a fast and headache-free way of connecting your application to "SAP NetWeaver Gateway" endpoints, learn how this addon can help you connect to business systems in just a few steps.

As a final note, this addon is composed of two parts:

- A standalone java application, which is used only at design time for retrieving metadata information
- The Roo addon itself

Tutorial
--------

### How to get started? ###

We have prepared a tutorial that shows you how to install the addon and how to create a web application connected to a SAP NetWeaver Gateway service. The tutorial is available [here](http://sapnwcloudlabs.github.com/nwcloud-roo-gwaddon/tutorial.html).


Building, installing and using the addon
----------------------------------------

In the following this document explains how to

1. build
2. install
3. use

the addon.

If you don't want to build the addon by yourself, but just use it, then please refer to the tutorial (see above), where we have also included a link to a download package with pre-compiled binaries and automated installer.


### Prerequisites ###

Except for the [NWCloud-Maven-Plugin](https://github.com/sapnwcloudlabs/nwcloud-maven-plugin) (which is not mandatory), the prerequisites are similar to those of the [SAP NetWeaver Cloud Addon for Spring Roo](http://sapnwcloudlabs.github.com/nwcloud-roo-addon/). Please mind that the addon has been tested with Java versions 1.6 and 1.7.

1. Building

	You need to have a recent version of [Apache Maven](http://maven.apache.org/) installed and available on the path, so that you can issue the command `mvn` on the command prompt regardless of the directory you're currently in.

2. Installing

	For installing the addon to Roo, you need to have [Spring Roo](http://www.springsource.org/spring-roo) installed and available on the path, so that you can issue the command `roo` (or `roo.sh` if you are using Linux or Mac OS) on the command prompt regardless of the directory you're currently in. The addon is known to be compatible with Roo versions 1.2.1 and 1.2.2.

3. Using

	The additional commands provided by the addon are available after installation.


### Building the addon ###

First clone the addon project from the git repository to a directory of your choice. Two folders will be created, __ODataMetadataRetriever__ and __RooAddon__.

2. Go to the __ODataMetadataRetriever__ and create a runnable jar file. This can be done easily using the following _maven_ command from the command line, while in the aforementioned folder:

	mvn clean package

Alternatively, you could import the content of this folder as a java project in Eclipse and export it as a runnable jar file. 

3. Do a fresh build of the Roo addon. Go to the _RooAddon_ folder and issue the following command on the commandline:

	mvn clean package


### Installing the addon ###

After having built the OData Metadata Retriever application and the Roo addon, one must do the following:

1. Copy the runnable jar from the __ODataMetadataRetriever/target/__ folder to your user home directory. This directory varies on Windows and Linux, but it is essential that the jar will reside here. Be sure that you got the correct jar, which should be named _com.sap.research.connectivity.gw.metadataretriever-1.1.0.RELEASE-jar-with-dependencies.jar_. Rename the file to _appToRetrieveOdataMetadata.jar_.

   Please Note: _This java application is used only at design time, for retrieving metadata from the NetWeaver Gateway systems you are connecting to. It does not interact in any way with the running applications that you are going to generate using Roo and this addon. The access to this application was tested on Windows, Linux and Mac OS._

2. Go to the "target" subfolder of the __RooAddon__ folder, start a Roo shell and issue a command following this schema:


	roo> osgi start --url file:///&#60;path-to-project-dir&#62;/target/com.sap.research.connectivity.gw-1.1.0.RELEASE.jar

        Check the installed addon bundle using:

	roo> osgi ps

        You should see a line like the following appearing in the shown list:

	[  75] [Active     ] [    1] nwcloud-roo-gwaddon (1.1.0.RELEASE)

        If you want to uninstall the addon, then you could do this using the following command:

	roo> osgi uninstall --bundleSymbolicName com.sap.research.connectivity.gw


### Using the addon ###

Please refer to the tutorial we provide (see above for link) to learn on how to use the addon.


Provided Roo commands
---------------------

### Which commands will the addon add to Roo? ###

After installing the addon, the following additional commands will be available in the Roo shell, subject to certain constraints:

        gateway setup

Modifies the Roo project to embed the odata4j package as Maven dependency and creates a specific _connectivity_ class.
This command is available only after a project has been created and persistency has been setup (using the roo-specific _persistence setup_ command).

        gateway define odata_endpoint --Name           *<endpoint name>
                                      --URL            *<URL to the NetWeaver Gateway service>
                                      --USER           *<username to connect to the NetWeaver Gateway service>
                                      --PASSWORD       *<password to connect to the NetWeaver Gateway service>
                                      --CSRF_MODE      <CSRF mode - default is `standard`>
                                      --HTTP_PROXYHOST <proxy address>
                                      --HTTP_PROXYPORT <proxy port>
                                      --TIMEOUT_CALL   <timeout value for retrieving metadata - default is `30`>

Defines a connection endpoint to a specified NetWeaver Gateway service. Metadata is downloaded for later usage in code generation. The starred parameters are mandatory.
This command is available only after the _gateway setup_ command has been issued.


        gateway entity --namespace        *<endpoint name>
                       --remoteEntitySet  *<entity set name, from the ones exposed through the specified endpoint>
                       --import_all       <whether to import all the fields or not - default is `true`>

Imports in the project the specified remote entity set. No data will be downloaded (i.e. local persistency is not used), just the support for CRUD operations on the specified entity. If the *import_all* flag is set to __false__, then only the key fields will be imported to the project. 
This command is available only after at least one _odata endpoint_ has been defined.

        gateway field --entityClass  *<name of the entity where the field should be imported>
                      --fieldName    *<name of the field to be imported>

Imports a field in the specified entity. This command provides greater refinement used in conjunction with the
*import_all* flag set to __false__ on the previous command.
This command is available only after at least one NetWeaver Gateway entity has been imported.

        gateway local field --entityClass  *<name of the entity where the local field should be created>
                            --fieldName    *<name of the field to be created>
                            --fieldType    *<type of the field to be created>

Adds a field whose underlying store is local persistence. Even though it will appear as part of a linked Gateway entity, the values entered in this field will be kept only locally. This command is available only after at least one NetWeaver Gateway entity has been imported.

        gateway mvc adapt --entityClass    *<name of the entity to be adapted>

Adapts the mvc controller to handle properly local and remote fields. This command should be used only if the specified linked Gateway entity contains local fields. Otherwise it is not necessary. This command is available only after a *web scaffolding* command has been issued for at least one entity.


### What else do I need to know? ###

#### So, what is this addon actually doing? ####

In the current version, the addon gives the possibility of defining Roo entitities which use a NetWeaver Gateway entity set as persistency layer, rather than normal JPA. 

Ok, but then you might ask, why do I need to set up persistency first?

Well, since Spring Roo’s generated applications rely on JPA persistency (any entity created in Spring Roo is a JPA-entity), one should issue the persistency setup commands before running any NetWeaver Gateway connectivity commands. No data is actually persisted. This is just a workaround for making maximum use of Roo's capabilities.

#### Is there a list of features? ####

Let's try this one below, which is valid for the current version of the addon:

- Autocompletion. Most of the options' values are available through tab-completion, including namespaces, entity sets and fields.
- Multiple NetWeaver Gateway connections supported, through the definition of multiple OData Endpoints.
- We provide support for composed keys.
- When you define an entity, the keys are automatically imported, no need to specify them explicitly.
- All NetWeaver Gateway simple types are supported for fields.
- An imported NetWeaver Gateway entity can have both imported and independent (local) fields.
- [CSRF](http://en.wikipedia.org/wiki/Cross-site_request_forgery) support. 


Please keep in mind that the addon intends to provide a fast starting point for developing applications that connect to NetWeaver Gateway services. Any complex or specific behavior still requires manual coding.


#### Cool! Now which are the limitations of the addon? ####

In the current version, the most important limitations are:

- Due to a current limitation of NetWeaver Cloud, connections over HTTPS to external systems will fail if the generated application is deployed to NetWeaver Cloud. We hope to see an update on this issue very soon.
- We do not process complex field types (such as records). These will be rendered as strings when imported.
- Other field properties, such as length of visible name are not supported.
- Relations with other entities (either JPA or NetWeaver Gateway) are not supported. 
- No possibility of specifying read-only or hidden fields.

Additional information
----------------------

### License ###

This project is copyrighted by [SAP AG](http://www.sap.com/) and made available under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Please also confer to the text files "LICENSE" and "NOTICE" included with the project sources.


### Contributions ###

Contributions to this project are very welcome, but can only be accepted if the contributions themselves are given to the project under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html). Contributions other than those given under Apache License 2.0 will be rejected.


Version history
---------------

#### 1.1.0. RELEASE

- Added support for local field within Gateway-linked entities (`gateway local field` and `gateway mvc adapt` commands added)
- Bug Fixing

#### 1.0.0. RELEASE

- Initial release
