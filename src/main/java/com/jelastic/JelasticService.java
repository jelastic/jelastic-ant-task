/*
 * Copyright 2012 Jelastic.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jelastic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jelastic.model.AuthenticationResponse;
import com.jelastic.model.CreateObjectResponse;
import com.jelastic.model.DeployResponse;
import com.jelastic.model.UploadResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.tools.ant.Project;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

public class JelasticService {
    private String shema = "https";
    private int port = -1;
    private Double version = 1.0;
    private long totalSize;
    private int numSt;
    private CookieStore cookieStore = null;
    private String urlAuthentication = "/" + version + "/users/authentication/rest/signin";
    private String urlUploader = "/" + version + "/storage/uploader/rest/upload";
    private String urlCreateObject = "/deploy/createobject";
    private String urlDeploy = "/deploy/DeployArchive";
    private Project project;
    private String filename;
    private String dir;
    private String apiHoster;
    private String environment;
    private String context;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getApiHoster() {
        return apiHoster;
    }

    public void setApiHoster(String apiHoster) {
        this.apiHoster = apiHoster;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public JelasticService(Project project) {
        this.project = project;
    }

    public int getPort() {
        return port;
    }

    public String getShema() {
        return shema;
    }

    public void setCookieStore(CookieStore cookieStore) {
        this.cookieStore = cookieStore;
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public String getUrlAuthentication() {
        return urlAuthentication;
    }

    public String getUrlUploader() {
        return urlUploader;
    }

    public String getUrlCreateObject() {
        return urlCreateObject;
    }

    public String getUrlDeploy() {
        return urlDeploy;
    }

    public AuthenticationResponse authentication(String email, String password) {
        AuthenticationResponse authenticationResponse = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient = wrapClient(httpclient);
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("login", email));
            qparams.add(new BasicNameValuePair("password", password));
            URI uri = URIUtils.createURI(getShema(), getApiHoster(), getPort(), getUrlAuthentication(), URLEncodedUtils.format(qparams, "UTF-8"), null);
            project.log("Authentication url : " + uri.toString(), Project.MSG_DEBUG);
            HttpGet httpGet = new HttpGet(uri);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpGet, responseHandler);
            setCookieStore(httpclient.getCookieStore());
            project.log("Authentication response : " + responseBody, Project.MSG_DEBUG);
            Gson gson = new GsonBuilder().setVersion(version).create();
            authenticationResponse = gson.fromJson(responseBody, AuthenticationResponse.class);
        } catch (URISyntaxException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (ClientProtocolException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (IOException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        }
        return authenticationResponse;
    }

    public UploadResponse upload(AuthenticationResponse authenticationResponse) {
        UploadResponse uploadResponse = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient = wrapClient(httpclient);
            httpclient.setCookieStore(getCookieStore());

            final File file = new File(getDir() + getFilename());
            if (!file.exists()) {
                throw new IllegalArgumentException("First build artifact and try again. Artifact not found .. ");
            }

            CustomMultiPartEntity multipartEntity = new CustomMultiPartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, new CustomMultiPartEntity.ProgressListener() {
                public void transferred(long num) {
                    if (((int) ((num / (float) totalSize) * 100)) != numSt) {
                        System.out.println("File Uploading : [" + (int) ((num / (float) totalSize) * 100) + "%]");
                        numSt = ((int) ((num / (float) totalSize) * 100));
                    }
                }
            });

            multipartEntity.addPart("fid", new StringBody("123456"));
            multipartEntity.addPart("session", new StringBody(authenticationResponse.getSession()));
            multipartEntity.addPart("file", new FileBody(file));
            totalSize = multipartEntity.getContentLength();

            URI uri = URIUtils.createURI(getShema(), getApiHoster(), getPort(), getUrlUploader(), null, null);
            project.log("Upload url : " + uri.toString(), Project.MSG_DEBUG);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(multipartEntity);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpPost, responseHandler);
            project.log("Upload response : " + responseBody, Project.MSG_DEBUG);
            Gson gson = new GsonBuilder().setVersion(version).create();
            uploadResponse = gson.fromJson(responseBody, UploadResponse.class);
        } catch (URISyntaxException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (ClientProtocolException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (IOException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        }
        return uploadResponse;
    }

    public CreateObjectResponse createObject(UploadResponse upLoader, AuthenticationResponse authentication) {
        CreateObjectResponse createObjectResponse = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient = wrapClient(httpclient);
            httpclient.setCookieStore(getCookieStore());
            List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
            nameValuePairList.add(new BasicNameValuePair("charset", "UTF-8"));
            nameValuePairList.add(new BasicNameValuePair("session", authentication.getSession()));
            nameValuePairList.add(new BasicNameValuePair("type", "JDeploy"));
            nameValuePairList.add(new BasicNameValuePair("data", "{'name':'" + upLoader.getName() + "', 'archive':'" + upLoader.getFile() + "', 'link':0, 'size':" + upLoader.getSize() + ", 'comment':'" + upLoader.getName() + "'}"));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nameValuePairList, "UTF-8");


            for (NameValuePair nameValuePair : nameValuePairList) {
                project.log(nameValuePair.getName() + " : " + nameValuePair.getValue(), Project.MSG_DEBUG);
            }

            URI uri = URIUtils.createURI(getShema(), getApiHoster(), getPort(), getUrlCreateObject(), null, null);
            project.log("CreateObject url : " + uri.toString(), Project.MSG_DEBUG);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(entity);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpPost, responseHandler);
            project.log("CreateObject response : " + responseBody, Project.MSG_DEBUG);
            Gson gson = new GsonBuilder().setVersion(version).create();
            createObjectResponse = gson.fromJson(responseBody, CreateObjectResponse.class);
        } catch (URISyntaxException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (ClientProtocolException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (IOException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        }
        return createObjectResponse;
    }

    public DeployResponse deploy(AuthenticationResponse authentication, UploadResponse upLoader) {
        DeployResponse deployResponse = null;
        try {
            DefaultHttpClient httpclient = new DefaultHttpClient();
            httpclient = wrapClient(httpclient);
            httpclient.setCookieStore(getCookieStore());
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("charset", "UTF-8"));
            qparams.add(new BasicNameValuePair("session", authentication.getSession()));
            qparams.add(new BasicNameValuePair("archiveUri", upLoader.getFile()));
            qparams.add(new BasicNameValuePair("archiveName", upLoader.getName()));
            qparams.add(new BasicNameValuePair("newContext", getContext()));
            qparams.add(new BasicNameValuePair("domain", getEnvironment()));

            for (NameValuePair nameValuePair : qparams) {
                project.log(nameValuePair.getName() + " : " + nameValuePair.getValue(), Project.MSG_DEBUG);
            }

            URI uri = URIUtils.createURI(getShema(), getApiHoster(), getPort(), getUrlDeploy(), URLEncodedUtils.format(qparams, "UTF-8"), null);
            project.log("Deploy url : " + uri.toString(), Project.MSG_DEBUG);
            HttpGet httpPost = new HttpGet(uri);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpPost, responseHandler);
            project.log("Deploy response : " + responseBody, Project.MSG_DEBUG);
            Gson gson = new GsonBuilder().setVersion(version).create();
            deployResponse = gson.fromJson(responseBody, DeployResponse.class);
        } catch (URISyntaxException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (ClientProtocolException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        } catch (IOException e) {
            project.log(e.getMessage(), Project.MSG_ERR);
        }
        return deployResponse;
    }


    public static DefaultHttpClient wrapClient(DefaultHttpClient base) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = base.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", ssf, 443));
            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception e) {
            return null;
        }
    }
}
