# Cloud Foundry Login Server

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
    $ mvn tomcat7:run -P integration

You can run the Login Server integration tests using the command line
as well (as long as the UAA project is built and installed first as
above):

    $ mvn test

Headless browser Selenium tests can be run from the login-server-integration-tests
directory. These tests fail if a Login Server and UAA have not been
started locally. They have been placed in their own module, so that they
need not be run when installing the login-server, and are *not* run when
performing a ````mvn install```` from the login-server directory.

These tests require [PhantomJS](http://phantomjs.org/download.html) to be installed.

    $ mvn tomcat7:run -P integration
    $ cd login-server-integration-tests
    $ mvn verify

There are two documents that can help you configure the login server in your environment.
    
[Login Server Configuration in deployment manifest](docs/Login-Server-Configuration.md)

[OpenAM Configuration](docs/OpenAM-Configuration.md)

# Contributing to the Login Server

Here are some ways for you to get involved in the community:

* Get involved with the Cloud Foundry community on the mailing lists.
  Please help out on the
  [mailing list](https://groups.google.com/a/cloudfoundry.org/forum/?fromgroups#!forum/vcap-dev)
  by responding to questions and joining the debate.
* Create [github](https://github.com/cloudfoundry/login-server/issues) tickets for bugs and new features and comment and
  vote on the ones that you are interested in.
* Github is for social coding: if you want to write code, we encourage
  contributions through pull requests from
  [forks of this repository](http://help.github.com/forking/).  If you
  want to contribute code this way, please reference an existing issue
  if there is one as well covering the specific issue you are
  addressing.  Always submit pull requests to the "develop" branch.
* Watch for upcoming articles on Cloud Foundry by
  [subscribing](http://blog.cloudfoundry.org) to the cloudfoundry.org
  blog
