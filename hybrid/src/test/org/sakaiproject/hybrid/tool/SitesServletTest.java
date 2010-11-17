/**
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.hybrid.tool;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;

public class SitesServletTest extends TestCase {
	protected SitesServlet sitesServlet;
	protected SessionManager sessionManager;
	protected SiteService siteService;
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected ComponentManager componentManager;
	protected ServerConfigurationService serverConfigurationService;
	protected ServletConfig config;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		Properties log4jProperties = new Properties();
		log4jProperties.put("log4j.rootLogger", "ALL, A1");
		log4jProperties.put("log4j.appender.A1",
				"org.apache.log4j.ConsoleAppender");
		log4jProperties.put("log4j.appender.A1.layout",
				"org.apache.log4j.PatternLayout");
		log4jProperties.put("log4j.appender.A1.layout.ConversionPattern",
				PatternLayout.TTCC_CONVERSION_PATTERN);
		log4jProperties.put("log4j.threshold", "ALL");
		PropertyConfigurator.configure(log4jProperties);
		serverConfigurationService = mock(ServerConfigurationService.class);
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		sessionManager = mock(SessionManager.class);
		final Session session = mock(Session.class);
		when(sessionManager.getCurrentSession()).thenReturn(session);
		when(session.getUserEid()).thenReturn("admin");
		siteService = mock(SiteService.class);
		Site site = mock(Site.class);
		when(site.getTitle()).thenReturn("Administration Workspace");
		when(site.getId()).thenReturn("!admin");
		when(site.getUrl())
				.thenReturn(
						"http://sakai3-nightly.uits.indiana.edu:8080/portal/site/!admin");
		when(site.getIconUrl()).thenReturn(null);
		User user = mock(User.class);
		when(user.getDisplayName()).thenReturn("Sakai Administrator");
		when(site.getCreatedBy()).thenReturn(user);
		@SuppressWarnings("unchecked")
		Set<Member> members = mock(Set.class);
		when(members.size()).thenReturn(1);
		when(site.getMembers()).thenReturn(members);
		when(site.getDescription()).thenReturn("Administration Workspace");
		when(site.getType()).thenReturn(null);
		when(site.getCreatedDate()).thenReturn(new Date());
		List<Site> siteList = new ArrayList<Site>();
		siteList.add(site);
		when(
				siteService
						.getSites(
								org.sakaiproject.site.api.SiteService.SelectionType.ACCESS,
								null,
								null,
								null,
								org.sakaiproject.site.api.SiteService.SortType.TITLE_ASC,
								null)).thenReturn(siteList);
		PrintWriter writer = mock(PrintWriter.class);
		when(response.getWriter()).thenReturn(writer);
		componentManager = mock(ComponentManager.class);
		when(componentManager.get(SessionManager.class)).thenReturn(
				sessionManager);
		when(componentManager.get(SiteService.class)).thenReturn(siteService);
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(serverConfigurationService);
		sitesServlet = new SitesServlet();
		sitesServlet.setupTestCase(componentManager);
		config = mock(ServletConfig.class);
		sitesServlet.init(config);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	public void testNormalBehavior() {
		try {
			sitesServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown: " + e, e);
		}
	}

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	public void testIOException() {
		try {
			when(response.getWriter()).thenThrow(new IOException());
			sitesServlet.doGet(request, response);
			fail("Should not be reached");
		} catch (ServletException e) {
			assertNull("ServletException should not be thrown: " + e, e);
		} catch (IOException e) {
			assertNotNull("IOException should be thrown", e);
		}
	}

	/**
	 * Tests {@link SitesServlet#init(ServletConfig)}
	 */
	public void testInitNullSessionManager() {
		when(componentManager.get(SessionManager.class)).thenReturn(null);
		try {
			sitesServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * Tests {@link SitesServlet#init(ServletConfig)}
	 */
	public void testInitNullSiteService() {
		when(componentManager.get(SiteService.class)).thenReturn(null);
		try {
			sitesServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * Tests {@link SitesServlet#init(ServletConfig)}
	 */
	public void testInitNullServerConfigurationService() {
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(null);
		try {
			sitesServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * Tests {@link SitesServlet#setupTestCase(ComponentManager)}
	 */
	public void testSetupTestCase() {
		try {
			sitesServlet.setupTestCase(null);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

}
