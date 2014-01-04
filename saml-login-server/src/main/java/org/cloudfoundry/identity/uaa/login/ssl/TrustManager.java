package org.cloudfoundry.identity.uaa.login.ssl;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;


public class TrustManager {

    
    final String[] subjectsToTrust = new String[] { "C=US, CN=\"ssoserver,dc=vsphere,dc=local\"",
            "C=US, CN=\"CA, CN=WIN-B7H4VETU0UM, dc=vsphere,dc=local\"" };

    public void init() throws Exception {
        System.err.println("FILIP - INIT");
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());  
        trustManagerFactory.init((KeyStore)null);  
        final javax.net.ssl.TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();  
        
        
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                ArrayList<X509Certificate> certs = new ArrayList<X509Certificate>();
                for (javax.net.ssl.TrustManager tm : trustManagers) {
                    
                    if (tm instanceof X509TrustManager) {
                        certs.addAll(Arrays.asList(((X509TrustManager)tm).getAcceptedIssuers()));
                    }
                }
                return (java.security.cert.X509Certificate[])certs.toArray(new X509Certificate[0]);
                
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs,String authType) throws CertificateException {
                for (java.security.cert.X509Certificate cert : certs) {
                    for (String dn : subjectsToTrust) {
                        if (dn.equals(cert.getSubjectDN())) {
                            return;
                        }
                    }
                }
                for (java.security.cert.X509Certificate cert : certs) {
                    for (javax.net.ssl.TrustManager tm : trustManagers) {
                        if (tm instanceof X509TrustManager) {
                            ((X509TrustManager)tm).checkClientTrusted(certs, authType);
                        }
                    }
                }
                    
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException {
                System.err.println("FILIP - CHECK SERVER\n:"+Arrays.toString(certs));
                for (java.security.cert.X509Certificate cert : certs) {
                    for (String dn : subjectsToTrust) {
                        if (dn.equals(cert.getSubjectDN())) {
                            return;
                        }
                    }
                }
                for (java.security.cert.X509Certificate cert : certs) {
                    for (javax.net.ssl.TrustManager tm : trustManagers) {
                        if (tm instanceof X509TrustManager) {
                            ((X509TrustManager)tm).checkServerTrusted(certs, authType);
                        }
                    }
                }
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            Protocol.registerProtocol("https", new Protocol("https", new EasySSLProtocolSocketFactory(),7444));
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new TrustManager();
        try {
            URL url = new URL("https://172.16.216.132:7444/websso/SAML2/Metadata/vsphere.local");
            url.openStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

}
