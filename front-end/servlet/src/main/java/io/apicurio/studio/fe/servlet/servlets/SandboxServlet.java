/*
 * Copyright 2019 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.studio.fe.servlet.servlets;

import io.apicurio.studio.fe.servlet.config.StudioUiConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.json.*;
import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A preview servlet for apicurio.  This servlet is responsible for serving up a RedDoc page used to
 * preview documentation for an API.  The user must have access to the API, and the API must exist.
 * The servlet requires the ID of the API to display.
 * @author eric.wittmann@gmail.com
 */
public class SandboxServlet extends HttpServlet {

    private static final long serialVersionUID = 7680564778236525468L;
    private static Logger logger = LoggerFactory.getLogger(SandboxServlet.class);

    @Inject
    private StudioUiConfiguration uiConfig;
    private CloseableHttpClient httpClient;

    @PostConstruct
    protected void postConstruct() {
        try {
            if (uiConfig.isDisableHubApiTrustManager()) {
                SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
                    @Override
                    public boolean isTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        return true;
                    }
                }).build();
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
                httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            } else {
                httpClient = HttpClients.createSystem();
            }
        } catch (Exception e) {
            logger.error("Error creating HTTP client.", e);
            throw new RuntimeException(e);
        }
    }

    public String createSandbox(String account, String application, String version, String swaggerURL, String url) {
        try {
            JsonObjectBuilder sandboxObjectBuilder = Json.createObjectBuilder();
            sandboxObjectBuilder.add("account", account);
            sandboxObjectBuilder.add("application", application);
            sandboxObjectBuilder.add("version", version);
            sandboxObjectBuilder.add("url", swaggerURL);

            JsonObject sandboxObj = sandboxObjectBuilder.build();

            StringWriter stringWriter = new StringWriter();
            JsonWriter writer = Json.createWriter(stringWriter);
            writer.writeObject(sandboxObj);
            writer.close();
            String sandboxObjJSON = stringWriter.getBuffer().toString();

            System.out.println((sandboxObjJSON));

            StringEntity entity = new StringEntity(sandboxObjJSON);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            try (CloseableHttpResponse apiResponse = httpClient.execute(httpPost)) {

                HttpEntity apiRespEntity = apiResponse.getEntity();
//                String responseString = EntityUtils.toString(apiRespEntity, "UTF-8");
//                System.out.println(url);
//                System.out.println(responseString);

                InputStream stream = apiRespEntity.getContent();

                JsonReader reader = Json.createReader(stream);

                JsonObject sandboxDetails = reader.readObject();

                String uuid = sandboxDetails.getString("uuid", null);

                return uuid;
            }
        } catch (IOException | IllegalStateException e) {
            logger.error("Error proxying URL: " + url, e);
        }

        return null;
    }
    
    private static String TEMPLATE_REDOC = null;
    {
        URL templateURL = SandboxServlet.class.getResource("preview_redoc.template");
        try {
            TEMPLATE_REDOC = IOUtils.toString(templateURL, Charset.forName("UTF-8"));
        } catch (Exception e) {
            logger.error("Failed to load previe template resource: preview_redoc.template", e);
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String apiId = req.getParameter("aid");
        String token = req.getParameter("session");
        String app = req.getParameter("application");
        
        logger.debug("Rendering document preview for API: {}", apiId);

        String url = this.uiConfig.getUiUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        String specURL = url + "/download?type=api&format=json&dereference=true&id=" + apiId+"&session="+token;
        logger.debug("Spec URL: {}", specURL);

        String uuid = createSandbox("dac", app, "3", specURL, this.uiConfig.getSandboxApiUrl()+"openapi-service/upload/url");

        String SANDBOX_URL = this.uiConfig.getSandboxApiUrl()+ "openapi-service/download-openapi?uuid="+uuid;
        String content= TEMPLATE_REDOC.replace("SPEC_URL", SANDBOX_URL);

        resp.setStatus(200);
        resp.setContentLength(content.length());
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();
        writer.print(content);
        writer.flush();
    }

}
