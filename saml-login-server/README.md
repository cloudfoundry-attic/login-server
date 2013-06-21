## Setting up OpenAM with the SAML Login server

- Follow this guide to set up OpenAM on tomcat http://openam.forgerock.org/openam-documentation/openam-doc-source/doc/install-guide/

- Set the OpenAM tomcat port to 8081 so that it is usable along with the uaa and login server listening on 8080

- Create the openam script per the suggestions in the openam install guide.
  - For a mac, JDK_HOME is "/System/Library/Frameworks/JavaVM.framework/Home"

- Install the saml login server certificate in the jdk trust store at "/System/Library/Frameworks/JavaVM.framework/Home/lib/security/cacerts"
  - `keytool -importcert -file login-server/saml-login-server/src/main/resources/security/cert.pem -alias samlcert -storepass changeit -trustcacerts -keystore cacerts`

- Install the saml login server certificate in the OpenAM trust store
  - `keytool -importcert -file login-server/saml-login-server/src/main/resources/security/cert.pem -alias samlcert -storepass changeit -trustcacerts -keystore keystore.jks`

- Start up the login server, uaa and sample apps
  - `export MAVEN_OPTS="-Xmx768m -XX:MaxPermSize=256m"`
  - `cd login-server/saml-login-server; mvn tomcat:run -P integration`

- Create an OpenAM Hosted Identity provider and a circle of trust

- Configure a Remote Service Provider using the metadata located at `http://localhost:8080/login/saml/metadata`

- Create a user in OpenAM. Set the user's email address

- Go to Federation -> Click the link matching your IDP Entity Provider -> NameID Value Map
  - Add urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified=mail

There are two ways to configure the SAML Login server

- Set the configuration to have the SAML login server get the IDP metadata

  <pre>
    login:
      idpMetadataURL: http://openam.example.com:8181/openam/saml2/jsp/exportmetadata.jsp?entityid=http://openam.example.com:8181/openam
  </pre>

- Export the IDP metadata from a URL similar to `http://openam.example.org:8081/openam/saml2/jsp/exportmetadata.jsp?entityid=http://openam.example.org:8081/openam` to a file
  - Set the configuration to point to that file
  
  <pre>
    login:
      idpMetadataFile: /path/to/idpMetadata.xml
  </pre>

  - To use this option, run the SAML metadata with both the fileMetadata and default profiles. `mvn tomcat:run -Dspring.profiles.active=fileMetadata,default`

- Test the configuration by going to http://localhost:8080/app

