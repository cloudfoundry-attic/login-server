## Setting up OpenAM

- Follow this guide to set it up on tomcat http://openam.forgerock.org/openam-documentation/openam-doc-source/doc/install-guide/

- Set the tomcat port to 8081 so that it is usable along with the uaa and login server listening on 8080

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

- Export the IDP metadata using a URL similar to `http://openam.example.org:8081/openam/saml2/jsp/exportmetadata.jsp?entityid=http://openam.example.org:8081/openam`

- Configure a Remote Service Provider using the metadata located at `http://localhost:8080/login/saml/metadata`

- Create a user in OpenAM. Set the user's email address

- Go to Federation -> Click the link matching your IDP Entity Provider -> NameID Value Map
  - Add urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified=mail

- Test the configuration by going to http://localhost:8080/app

