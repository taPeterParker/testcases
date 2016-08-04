/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.coheigea.bigdata.kms.ranger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.apache.hadoop.crypto.key.kms.server.KMS.KMSOp;
import org.apache.hadoop.crypto.key.kms.server.KMSACLsType.Type;
import org.apache.hadoop.crypto.key.kms.server.KMSConfiguration;
import org.apache.hadoop.crypto.key.kms.server.KMSWebApp;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.AuthorizationException;
import org.easymock.EasyMock;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Policies available from admin via:
 * 
 * http://localhost:6080/service/plugins/policies/download/KMSTest
 * 
 * The user "bob" can do anything. The group "IT" can only call the "get" methods
 */
public class RangerKmsAuthorizerTest {
    
    private static Connection conn;
    private static KMSWebApp kmsWebapp;
    
    @BeforeClass
    public static void startServers() throws Exception {
        // Start Apache Derby
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver").newInstance();
        
        Properties props = new Properties();
        Connection conn = DriverManager.getConnection("jdbc:derby:memory:derbyDB;create=true", props);
        
        Statement statement = conn.createStatement();
        statement.execute("CREATE SCHEMA KMSADMIN");
        
        statement.execute("SET SCHEMA KMSADMIN");
        
        // Create masterkey table
        statement.execute("CREATE SEQUENCE RANGER_MASTERKEY_SEQ START WITH 1 INCREMENT BY 1");
        String tableCreationString = "CREATE TABLE ranger_masterkey (id VARCHAR(20) NOT NULL PRIMARY KEY, create_time DATE,"
            + "update_time DATE, added_by_id VARCHAR(20), upd_by_id VARCHAR(20),"
            + "cipher VARCHAR(255), bitlength VARCHAR(11), masterkey VARCHAR(2048))";
        statement.execute(tableCreationString);
        
        // Create keys table
        statement.execute("CREATE SEQUENCE RANGER_KEYSTORE_SEQ START WITH 1 INCREMENT BY 1");
        statement.execute("CREATE TABLE ranger_keystore(id VARCHAR(20) NOT NULL PRIMARY KEY, create_time DATE,"
            + "update_time DATE, added_by_id VARCHAR(20), upd_by_id VARCHAR(20),"
            + "kms_alias VARCHAR(255) NOT NULL, kms_createdDate VARCHAR(20), kms_cipher VARCHAR(255),"
            + "kms_bitLength VARCHAR(20), kms_description VARCHAR(512), kms_version VARCHAR(20),"
            + "kms_attributes VARCHAR(1024), kms_encoded VARCHAR(2048))");
        
        Path configDir = Paths.get("src/test/resources/kms");
        System.setProperty(KMSConfiguration.KMS_CONFIG_DIR, configDir.toFile().getAbsolutePath());
        
        // Start KMSWebApp
        ServletContextEvent servletContextEvent = EasyMock.createMock(ServletContextEvent.class);
        ServletContext servletContext = EasyMock.createMock(ServletContext.class);
        EasyMock.expect(servletContextEvent.getServletContext()).andReturn(servletContext).anyTimes();
        EasyMock.replay(servletContextEvent);
        
        kmsWebapp = new KMSWebApp();
        kmsWebapp.contextInitialized(servletContextEvent);
    }
    
    @AfterClass
    public static void stopServers() throws Exception {
        // Shut Derby down
        if (conn != null) {
            conn.close();
        }
        try {
            DriverManager.getConnection("jdbc:derby:memory:derbyDB;drop=true");
        } catch (SQLException ex) {
            // expected
        }
    }
    
    @org.junit.Test
    public void testCreateKeys() throws Throwable {
        
        // bob should have permission to create
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.CREATE, ugi, KMSOp.CREATE_KEY, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to create
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.CREATE, ugi2, KMSOp.CREATE_KEY, "newkey2", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should not have permission to create
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.CREATE, ugi3, KMSOp.CREATE_KEY, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
    }
    
    @org.junit.Test
    public void testDeleteKeys() throws Throwable {
        
        // bob should have permission to delete
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.DELETE, ugi, KMSOp.DELETE_KEY, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to delete
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.DELETE, ugi2, KMSOp.DELETE_KEY, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should not have permission to delete
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.DELETE, ugi3, KMSOp.DELETE_KEY, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
    }
    
    @org.junit.Test
    public void testRollover() throws Throwable {
        
        // bob should have permission to rollover
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.ROLLOVER, ugi, KMSOp.ROLL_NEW_VERSION, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to rollover
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.ROLLOVER, ugi2, KMSOp.ROLL_NEW_VERSION, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should not have permission to rollover
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.ROLLOVER, ugi3, KMSOp.ROLL_NEW_VERSION, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
    }
    
    @org.junit.Test
    public void testGetKeys() throws Throwable {
        
        // bob should have permission to get keys
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.GET_KEYS, ugi, KMSOp.GET_KEYS, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to get keys
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.GET_KEYS, ugi2, KMSOp.GET_KEYS, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should have permission to get keys
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.GET_KEYS, ugi3, KMSOp.GET_KEYS, "newkey1", "127.0.0.1");
                return null;
            }
        });
    }
    
    @org.junit.Test
    public void testGetMetadata() throws Throwable {
        
        // bob should have permission to get the metadata
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.GET_METADATA, ugi, KMSOp.GET_METADATA, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to get the metadata
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.GET_METADATA, ugi2, KMSOp.GET_METADATA, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should have permission to get the metadata
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.GET_METADATA, ugi3, KMSOp.GET_METADATA, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
    }
  
    @org.junit.Test
    public void testGenerateEEK() throws Throwable {
        
        // bob should have permission to generate EEK
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.GENERATE_EEK, ugi, KMSOp.GENERATE_EEK, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to generate EEK
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.GENERATE_EEK, ugi2, KMSOp.GENERATE_EEK, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should not have permission to generate EEK
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.GENERATE_EEK, ugi3, KMSOp.GENERATE_EEK, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
    }
    
    @org.junit.Test
    public void testDecryptEEK() throws Throwable {
        
        // bob should have permission to generate EEK
        final UserGroupInformation ugi = UserGroupInformation.createRemoteUser("bob");
        ugi.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                KMSWebApp.getACLs().assertAccess(Type.DECRYPT_EEK, ugi, KMSOp.DECRYPT_EEK, "newkey1", "127.0.0.1");
                return null;
            }
        });
        
        // "eve" should not have permission to decrypt EEK
        final UserGroupInformation ugi2 = UserGroupInformation.createRemoteUser("eve");
        ugi2.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.DECRYPT_EEK, ugi2, KMSOp.DECRYPT_EEK, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
        // the IT group should not have permission to decrypt EEK
        final UserGroupInformation ugi3 = UserGroupInformation.createUserForTesting("alice", new String[]{"IT"});
        ugi3.doAs(new PrivilegedExceptionAction<Void>() {

            public Void run() throws Exception {
                try {
                    KMSWebApp.getACLs().assertAccess(Type.DECRYPT_EEK, ugi3, KMSOp.DECRYPT_EEK, "newkey1", "127.0.0.1");
                    Assert.fail("Failure expected");
                } catch (AuthorizationException ex) {
                    // expected
                }
                return null;
            }
        });
        
    }

}