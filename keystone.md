
Installing Keystone
==================================

    sudo apt-get install keystone python-keystone python-keystoneclient

Default password is ADMIN as defined in /etc/keystone/keystone.conf:admin_token


Creating a test user (no tenant)
==================================


    export SERVICE_ENDPOINT=http://localhost:35357/v2.0
    export SERVICE_TOKEN=ADMIN
    keystone user-create --name=marissa --pass=koala --email=marissa@test.org

    keystone service-create --name=keystoneV3 --type=identity --description="Keystone Identity Service V3"
    keystone endpoint-create  --service_id=<take ID output from above> --publicurl=http://localhost:5000/v3 --internalurl=http://localhost:5000/v3 --adminurl=http://localhost:35357/v3
    
Getting an user token (simple authentication)
==================================


    curl -v -d '{"auth":{"tenantName": "", "passwordCredentials": {"username": "marissa", "password": "koala"}}}' -H "Content-type: application/json" http://localhost:35357/v2.0/tokens    

Results in 


    {
        "access": {
            "token": {
                "expires": "2014-04-30T20:46:35Z", 
                "id": "f91a06a6d46b4f769dd2ca7992ed4c44"
            }, 
            "serviceCatalog": {}, 
            "user": {
                "username": "marissa", 
                "roles_links": [], 
                "id": "2e352099ab7a41b6beeb10d02f9cb082", 
                "roles": [], 
                "name": "marissa"
            }
        }
    }
    
