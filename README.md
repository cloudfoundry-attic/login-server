# Cloud Foundry Login Server

[![Build Status](https://travis-ci.org/cloudfoundry/login-server.svg?branch=develop)](https://travis-ci.org/cloudfoundry/login-server)
[![Coverage Status](https://coveralls.io/repos/cloudfoundry/login-server/badge.png?branch=develop)](https://coveralls.io/r/cloudfoundry/login-server?branch=develop)

Handles authentication on `run.pivotal.io` and delegates all other
identity management tasks to the UAA.  Also provides OAuth2 endpoints
issuing tokens to client apps for `run.pivotal.io` (the tokens come
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
    $ unset GEM_PATH (see https://www.pivotaltracker.com/story/show/70172968)
    $ mvn clean install
    $ mvn tomcat7:run -P integration

The unit tests will have been run as part of `mvn install`, or can be
run on their own with `mvn test`.

You can run the Login Server integration tests using the command line as
well. These integration tests will be skipped automatically if a Login
Server and UAA have not been started locally. These tests require
[PhantomJS](http://phantomjs.org/download.html) to be installed.

    $ mvn verify

There are two documents that can help you configure the login server in your environment.
    
[Login Server Configuration in deployment manifest](docs/Login-Server-Configuration.md)

[OpenAM Configuration](docs/OpenAM-Configuration.md)

## Stylesheets

The Login Server uses [SASS](http://sass-lang.com/) to preprocess CSS stylesheets.
These get compiled in the generate-resources phase of the Maven build.
To watch the SCSS files and auto-recompile them during development, use:

    $ mvn -pl login-server sass:watch

## The Login Application

The UAA can authenticate user accounts, but only if it manages them
itself, and it only provides a basic UI.  The Login app can be branded
and customized for non-native authentication and for more complicated
UI flows, like user registration and password reset.

The login application is actually itself an OAuth2 endpoint provider,
but delegates those features to the UAA server.  Configuration for the
login application therefore consists of locating the UAA through its
OAuth2 endpoint URLs, and registering the login application itself as
a client of the UAA.  There is a `login.yml` for the UAA locations,
e.g. for a local `vcap` instance:

    uaa:
      url: http://uaa.vcap.me
      token:
        url: http://uaa.vcap.me/oauth/token
      login:
        url: http://uaa.vcap.me/login.do

and there is an environment variable (or Java System property),
`LOGIN_SECRET` for the client secret that the app uses when it
authenticates itself with the UAA.  The Login app is registered by
default in the UAA only if there are no active Spring profiles (so not
at all in `vcap`).  In the UAA you can find the registration in the
`oauth-clients.xml` config file.  Here's a summary:

    id: login
    secret: loginsecret
    authorized-grant-types: client_credentials
    authorities: ROLE_LOGIN
    resource-ids: oauth

### Use Cases

1. Authenticate

        GET /login

    The Login Server presents a form login interface for the backend
    UAA, or with other services (such as LDAP or SAML).

2. Approve OAuth2 token grant

        GET /oauth/authorize?client_id=app&response_type=code...

    Standard OAuth2 Authorization Endpoint.  Client credentials and
    all other features are handled by the UAA in the back end, and the
    login server is used to render the UI (see
    `access_confirmation.html`).

3. Obtain access token

        POST /oauth/token

    Standard OAuth2 Authorization Endpoint passed through to the UAA.

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
