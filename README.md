# Cloud Foundry Login Server (Deprecated)

## Retirement / Deprecation
The Cloud Foundry Login Server project has been retired. The functionality of the Login Server have been merged into the
[Cloud Foundry UAA](https://github.com/cloudfoundry/uaa/)

## Login Server

[![Build Status](https://travis-ci.org/cloudfoundry/login-server.svg?branch=develop)](https://travis-ci.org/cloudfoundry/login-server)
[![Coverage Status](https://coveralls.io/repos/cloudfoundry/login-server/badge.png?branch=develop)](https://coveralls.io/r/cloudfoundry/login-server?branch=develop)

Handles authentication in Cloud Foundry and delegates all other
identity management tasks to the UAA.  Also provides OAuth2 endpoints
issuing tokens to client apps for Cloud Foundry (the tokens come
from the UAA and no data are stored locally).

## Co-ordinates

* Tokens: [A note on tokens, scopes and authorities](https://github.com/cloudfoundry/uaa/tree/master/docs/UAA-Tokens.md)
* Technical forum: [vcap-dev google group](https://groups.google.com/a/cloudfoundry.org/forum/?fromgroups#!forum/vcap-dev)
* Docs: [docs/](https://github.com/cloudfoundry/login-server/tree/master/docs)
* API Documentation: [Login-APIs.md](https://github.com/cloudfoundry/login-server/tree/master/docs/Login-APIs.md)
* Specification: [The Oauth 2 Authorization Framework](http://tools.ietf.org/html/rfc6749)
* SAML: [OpenAM-README.md](https://github.com/cloudfoundry/login-server/tree/master/docs/OpenAM-README.md),
        [Okta-README.md](https://github.com/cloudfoundry/login-server/tree/master/docs/Okta-README.md)

## Running and Testing the Login Server

The Login Server is a standard JEE servlet application, and you can
build a war file and deploy it to any container you like (`./gradlew
:war` and look in the `build/libs` directory).  For convenience there
is also a Gradle task that will run the Login Server, the UAA and
some sample apps all in the same container from the command line:

    $ git clone https://github.com/cloudfoundry/login-server.git
    $ cd login-server && ./update
    $ ./gradlew run

You can run the Login Server tests using the command line as
well. The integration tests require
[PhantomJS](http://phantomjs.org/download.html) to be installed.

    $ ./gradlew :test
    $ ./gradlew :integrationTest

You can run all tests for the login server and the uaa with

    $ ./gradlew test integrationTest

There are two documents that can help you configure the login server in your environment.
    
[Login Server Configuration in deployment manifest](docs/Login-Server-Configuration.md)

[OpenAM Configuration](docs/OpenAM-Configuration.md)

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
    UAA, or with other services (such as SAML).

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

## The Cloud Foundry SAML Login Server

The saml_login server supports two additional features on top of what you get from the regular [login-server] [1].
It adds authentication using an external SAML source. We have tested our authentication with [OpenAM] [2] and the vCenter SSO appliance.

  [1]: https://github.com/cloudfoundry/login-server/tree/master "login-server"
  [2]: https://github.com/cloudfoundry/login-server/tree/master/OpenAM-README.md "OpenAM Installation Instructions"

###Configuring cf-release for a saml_login deployment

The saml_login deploys the same way as the login-server, with additional configuration parameters.
Enabling saml is done using the `spring_profiles` configuration parameter. SAML can be used together, as two different profiles active
at the same time.

- Open your infrastructure manifest - for example cf-release/templates/cf-infrastructure-warden.yml

  Add your Tomcat JVM options as well as the intended protocol to use (http/https)
  <pre>
      saml_login:
        catalina_opts: -Xmx384m -XX:MaxPermSize=128m
        protocol: http

  </pre>
  Scroll down to your login job and change the template to saml_login, it will be found under
  <pre>
    jobs:
      - name: login_z1
        template: saml_login
  </pre>

- Open your cf-jobs.yml manifest and change the template for the login job

  <pre>
      - name: login_z1
        release: (( meta.release.name ))
        template: saml_login
  </pre>

- Open your cf-properties.yml manifest to configure saml_login properties

  Please note the spring_profiles setting
  - spring_profiles: saml (uses only  saml with an external SAML provider)

  <pre>
    saml_login:
      #standard login server configuration
      catalina_opts: (( merge ))
      uaa_certificate: ~
      protocol: https

      links:
        home: (( "https://console." domain ))
        passwd: (( "https://console." domain "/password_resets/new" ))
        signup: (( "https://console." domain "/register" ))

      #if you wish to use saml
      spring_profiles: saml

      #saml authentication information, only required if 'saml' is part of spring_profiles
      entityid: cloudfoundry-saml-login-server
      idpEntityAlias: vsphere-local
      idpMetadataURL: "https://win2012-sso2:7444/websso/SAML2/Metadata/vsphere.local"
      serviceProviderKeyPassword: password
      serviceProviderKey: |
        -----BEGIN RSA PRIVATE KEY-----
        Proc-Type: 4,ENCRYPTED
        DEK-Info: DES-EDE3-CBC,231BD428AF94D4C8
        0Nmo90pX8byVS7ZlakMIoXdJSLlxqzi1pN0g1ye2U+9HgTLTLuMwWaPknZ/2NFtK
        rO72ss8uc7xBAoMkOvcMTZCg5P4JDlmuQ31IabzRyOQcAxCPZedgarRnwxT6GUim
        JtkzNPmAAgf1bfUTu/LNt2o01dW+qq+2qiwUxgUBM2xLBmadIWqqTOZbkFc9Xjvl
        /IEnJgp/c49sNh68EpXPlsGJfW7jAh90nlA13H1fpvTsSg2/6wKbRsxxNkpVg0Nq
        bQURQIO6htOLZBPMMpoPILp/KtKkd1zpaZJnbZGDo9AdwfAh9dUbEw8ukJwRg3Xl
        lsptHoMGsGdvgViWZhCB/pAHYLh31G8oVMA/qPB9PNJYIK2aQZdm7yiAdf+m8Jxb
        Do2xBH6XUeHkg2F0LWnC/FjaMRpLuliI9vvJVB7YCQKkMdgNVV0SCx39IiX0rEm5
        8vuuoAH3b7b+maWp5+ffriNIcEFSlcmTPIgqZBboIORBNXZnHTUG7nGIML+nlOK9
        zdvF2vAxchqOKroc6+SGFLNvNQd9S/nLH3vP+aX9iStL55G11+p2tL+bIGMWZj0h
        Z+qqQoogtngRFbjcVoKYerFXbKG6xXzXUc4O3EbvAKvEi0HJodYccP3L7wIer1aY
        VaMF2M05g5KedHosEfvvhU17xS9L4u2SRMZIQ3K8iLNEhZ6bOw6EnzTaKWeffrYr
        UOjfMEgswcHfpxx1iD5T4RTwxuKOgtFhd1QM4enXPsU6uRU5PGSiB/0t6jal6ClF
        PhtIrTwhx0vBR4rySx4raXdLClxxt5vLe826C3uwo/6HTdUsnDvIXA==
        -----END RSA PRIVATE KEY-----
      serviceProviderKeyPassword: password
      serviceProviderCertificate: |
        -----BEGIN CERTIFICATE-----
        MIIBzzCCATgCCQDTMCX3wJYrVDANBgkqhkiG9w0BAQUFADAsMSowKAYDVQQDFCFz
        YW1sX2xvZ2luLE9VPXRlc3QsTz12bXdhcmUsTz1jb20wHhcNMTMwNzAyMjMzOTU4
        WhcNMTQwNzAyMjMzOTU4WjAsMSowKAYDVQQDFCFzYW1sX2xvZ2luLE9VPXRlc3Qs
        Tz12bXdhcmUsTz1jb20wgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBANYBRuep
        WaoNA7/RsOUVlmOzxbhtfW8AstGXjsWbAmmg8NSruRlNuMz1WdCeESM3zBqLSyp8
        Vf3j3ExzB2qquDbPXNA1k4EqgNya2E+6n3KsgVLCWQm4W46Pd7C6QswrR6JgUKaW
        6KI8BgyJQ9wjL/nR8uqZouJJyRSLuIaGXIuXAgMBAAEwDQYJKoZIhvcNAQEFBQAD
        gYEAXOojarkGv5nVZqTuY8BRM/TRt1oby3i0VRG70l0PcDlWft52aSvCd3t8ds2S
        h92cXLz8nvHEBaBTkxTLtf2/h5x2KQhXyHoU1UU+JjOegoF+LD6KdmaVk2l35Na5
        1V2AHsj+yDrJ9aKwt86jbBbcFkRphdkn5ivq71GCWRfcpZE=
        -----END CERTIFICATE-----
      nameidFormat: "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent"

  </pre>
