/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.gitkit.samples;

import com.google.common.collect.Lists;
import com.google.identitytoolkit.GitkitClient;
import com.google.identitytoolkit.GitkitClientException;
import com.google.identitytoolkit.GitkitUser;
import org.json.JSONException;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.SessionHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Scanner;

public class GitkitExample {

    GitkitExample() {
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(4567);
        ServletHandler servletHandler = new ServletHandler();
        SessionHandler sessionHandler = new SessionHandler();
        sessionHandler.setHandler(servletHandler);
        server.setHandler(sessionHandler);
        servletHandler.addServletWithMapping(LoginServlet.class, "/login");
        servletHandler.addServletWithMapping(WidgetServlet.class, "/gitkit");
        servletHandler.addServletWithMapping(GitKitUserInfoServlet.class, "/info");
        servletHandler.addServletWithMapping(GitKitUploadUsersServlet.class, "/upload");
        servletHandler.addServletWithMapping(LoginServlet.class, "/");
        server.start();
        server.join();
    }

    public static class LoginServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            // This check prevents the "/" handler from handling all requests by default
            if (!"/".equals(request.getServletPath())) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try {
                GitkitUser gitkitUser = null;
                GitkitClient gitkitClient = GitkitClient.createFromJson("/apps/apps-config/gitkit-server-config.json");

                gitkitUser = gitkitClient.validateTokenInRequest(request);
                String userInfo = null;
                if (gitkitUser != null) {
                    userInfo = "Welcome back!<br><br> Email: " + gitkitUser.getEmail() + "<br> Id: "
                            + gitkitUser.getLocalId() + "<br> Provider: " + gitkitUser.getCurrentProvider();
                }

                response.getWriter().print(new Scanner(new File("templates/index.html"), "UTF-8")
                        .useDelimiter("\\A").next()
                        .replaceAll("WELCOME_MESSAGE", userInfo != null ? userInfo : "You are not logged in")
                        .toString());
                response.setStatus(HttpServletResponse.SC_OK);
            } catch (FileNotFoundException | GitkitClientException | JSONException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print(e.toString());
            }
        }
    }

    public static class GitKitUploadUsersServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.setContentType("text/html");

            System.out.println("GitKitUploadUsersServlet::doGet()");

            GitkitClient gitkitClient = GitkitClient.createFromJson("/apps/apps-config/gitkit-server-config.json");

            try {
                String hashKey = "hash-key";
                String hashAlgorithm = "SHA1";
                String userPass = "trythis";

                MessageDigest cript = MessageDigest.getInstance("SHA-1");
                cript.reset();
                cript.update(userPass.getBytes("utf8"));
                GitkitUser user = new GitkitUser()
                        .setLocalId("1111")
                        .setEmail("new.user@relaydomain.com")
                        .setHash(cript.digest());

                gitkitClient.uploadUsers(hashAlgorithm, hashKey.getBytes(), Lists.newArrayList(user));
            } catch (Exception e) {
                e.printStackTrace();
            }

            StringBuilder builder = new StringBuilder();
            String line;
            try {
                while ((line = request.getReader().readLine()) != null) {
                    builder.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            String postBody = URLEncoder.encode(builder.toString(), "UTF-8");

            try {
                response.getWriter().print(new Scanner(new File("templates/gitkit-widget.html"), "UTF-8")
                        .useDelimiter("\\A").next()
                        .replaceAll("JAVASCRIPT_ESCAPED_POST_BODY", postBody)
                        .toString());
                response.setStatus(HttpServletResponse.SC_OK);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print(e.toString());
            }
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            doGet(request, response);
        }
    }

    public static class GitKitUserInfoServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.setContentType("text/html");

            System.out.println("GitKiUserInfoServlet::doGet()");

            GitkitClient gitkitClient = GitkitClient.createFromJson("/apps/apps-config/gitkit-server-config.json");

            try {
                // Verifies a GitkitToken
                GitkitUser gitkitUser = gitkitClient.validateTokenInRequest(request);
                System.out.println("GitKiUserInfoServlet::doGet() logged in user info is " + gitkitUser.getEmail() + ", " + gitkitUser.getName());

                // Download all accounts from Google Identity Toolkit
                Iterator<GitkitUser> userIterator = gitkitClient.getAllUsers();
                while (userIterator.hasNext()) {
                    // individual user info is returned in userIterator.next()
                    GitkitUser user = userIterator.next();
                    System.out.println("GitKiUserInfoServlet::doGet() user info is " + user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            StringBuilder builder = new StringBuilder();
            String line;
            try {
                while ((line = request.getReader().readLine()) != null) {
                    builder.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            String postBody = URLEncoder.encode(builder.toString(), "UTF-8");

            try {
                response.getWriter().print(new Scanner(new File("templates/gitkit-widget.html"), "UTF-8")
                        .useDelimiter("\\A").next()
                        .replaceAll("JAVASCRIPT_ESCAPED_POST_BODY", postBody)
                        .toString());
                response.setStatus(HttpServletResponse.SC_OK);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print(e.toString());
            }
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            doGet(request, response);
        }
    }

    public static class WidgetServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            response.setContentType("text/html");

            StringBuilder builder = new StringBuilder();
            String line;
            try {
                while ((line = request.getReader().readLine()) != null) {
                    builder.append(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String postBody = URLEncoder.encode(builder.toString(), "UTF-8");

            try {
                response.getWriter().print(new Scanner(new File("templates/gitkit-widget.html"), "UTF-8")
                        .useDelimiter("\\A").next()
                        .replaceAll("JAVASCRIPT_ESCAPED_POST_BODY", postBody)
                        .toString());
                response.setStatus(HttpServletResponse.SC_OK);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().print(e.toString());
            }
        }

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            doGet(request, response);
        }
    }
}
