/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.coheigea.cxf.jaxrs.jwt.authorization;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.coheigea.cxf.jaxrs.json.common.Number;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jaxrs.JwtAuthenticationClientFilter;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtConstants;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test JAX-RS JWT authorization using a "role" claim embedded in the token. The service is configured
 * with the SimpleAuthorizingInterceptor, which requires a role of "boss" to invoke on the "doubleIt"
 * operation.
 */
public class JWTAuthorizationRoleTest extends AbstractBusClientServerTestBase {

    private static final String PORT = allocatePort(Server.class);

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
            );
    }

    @org.junit.Test
    public void testAuthorizedRequest() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setProperty("role", "boss");
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 50);
    }
    
    @org.junit.Test
    public void testNoRole() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testWrongRole() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleit/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setProperty("role", "employee");
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Number numberToDouble = new Number();
        numberToDouble.setDescription("This is the number to double");
        numberToDouble.setNumber(25);

        Response response = client.post(numberToDouble);
        assertNotEquals(response.getStatus(), 200);
    }

    @org.junit.Test
    public void testAuthorizedRequestForRoleAnnotation() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleitrole/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setProperty("role", "boss");
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.get();
        assertEquals(response.getStatus(), 200);
        assertEquals(response.readEntity(Number.class).getNumber(), 8);
    }
    
    @org.junit.Test
    public void testNoRoleAnnotation() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleitrole/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.get();
        assertNotEquals(response.getStatus(), 200);
    }
    
    @org.junit.Test
    public void testWrongRoleAnnotation() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleitrole/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setProperty("role", "employee");
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.get();
        assertNotEquals(response.getStatus(), 200);
    }
    
    // Check we still get an error on the role using HEAD instead of GET
    @org.junit.Test
    public void testWrongRoleAnnotationHEAD() throws Exception {

        URL busFile = JWTAuthorizationRoleTest.class.getResource("cxf-client.xml");

        List<Object> providers = new ArrayList<Object>();
        providers.add(new JacksonJsonProvider());
        providers.add(new JwtAuthenticationClientFilter());
        
        String address = "http://localhost:" + PORT + "/doubleitrole/services";
        WebClient client = 
            WebClient.create(address, providers, busFile.toString());
        client.type("application/json").accept("application/json");
        
        // Create the JWT Token
        JwtClaims claims = new JwtClaims();
        claims.setSubject("alice");
        claims.setIssuer("DoubleItSTSIssuer");
        claims.setIssuedAt(new Date().getTime() / 1000L);
        claims.setProperty("role", "employee");
        claims.setAudiences(Collections.singletonList(address));
        
        JwtToken token = new JwtToken(claims);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("rs.security.keystore.type", "jks");
        properties.put("rs.security.keystore.password", "cspass");
        properties.put("rs.security.keystore.alias", "myclientkey");
        properties.put("rs.security.keystore.file", "clientstore.jks");
        properties.put("rs.security.key.password", "ckpass");
        properties.put("rs.security.signature.algorithm", "RS256");
        properties.put(JwtConstants.JWT_TOKEN, token);
        
        WebClient.getConfig(client).getRequestContext().putAll(properties);

        Response response = client.head();
        assertNotEquals(response.getStatus(), 200);
    }
    
}