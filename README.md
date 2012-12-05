# Cloud Foundry Login Server

It's grEAt!

Handles authentication on `cloudfoundry.com` and delegates all other
identity management tasks to the UAA.  Also provides OAuth2 endpoints
issuing tokens to client apps for `cloudfoundry.com` (the tokens come
from the UAA and no data are stored locally).

## Running and Testing the Login Server

The Login Server is a standard JEE servlet application, and you can
build a war file and deploy it to any container you like (`mvn
package` and look in the `target` directory).  For convenience there
is also a Maven profile that will run the Login Server, the UAA and
some sample apps all in the same container from the command line
(assuming you have the UAA and Login Server cloned in separate
directories with a common parent):

    $ (cd uaa; mvn clean install)
    $ cd login-server
    $ mvn clean install
    $ mvn tomcat:run -P integration

(Note that the `tomcat7` plugin at the moment does not support running
multiple apps in the same container - it's a bug that is fixed but not
released as of September 2012.)

You can run the Login Server integration tests using the command line
as well (as long as the UAA project is built and installed first as
above):

    $ mvn test -P integration
