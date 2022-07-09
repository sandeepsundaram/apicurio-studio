/*
 * Copyright 2020 JBoss Inc
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

package io.apicurio.hub.api.security;


import io.apicurio.studio.shared.beans.User;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.DefaultJwtSignatureValidator;

import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;

import static io.jsonwebtoken.SignatureAlgorithm.HS256;

/**
 * This is a simple filter that extracts authentication information from the
 * security context
 *
 * @author carnalca@redhat.com
 */
public class QuarkusAuthenticationFilter implements Filter {

    @Inject
    private ISecurityContext security;

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override public void init(FilterConfig filterConfig) throws ServletException {

    }

    /**
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        String authorization = httpReq.getHeader("Authorization");

        if (authorization != null) {
            String token = authorization.split(" ")[1];
            SignatureAlgorithm sa = HS256;
            SecretKeySpec secretKeySpec = new SecretKeySpec("secretKey".getBytes(), sa.getJcaName());

            String[] chunks = token.split("\\.");

            String tokenWithoutSignature = chunks[0] + "." + chunks[1];
            String signature = chunks[2];

            DefaultJwtSignatureValidator validator = new DefaultJwtSignatureValidator(sa, secretKeySpec);

            if (!validator.isValid(tokenWithoutSignature, signature)) {
                throw new ServletException("Could not verify JWT token integrity!");
            }

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));

            JsonObject jObj = Json.createReader(new StringReader(payload)).readObject();

            User user = new User();
            user.setEmail(jObj.getString("email"));
            user.setLogin(jObj.getString("login"));
            user.setName(jObj.getString("name"));
            user.setId(jObj.getInt("id"));

            ((SecurityContext) security).setUser(user);
            ((SecurityContext) security).setToken(token);

            chain.doFilter(request, response);
        } else {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid access");
        }

    }

    @Override public void destroy() { }
}
