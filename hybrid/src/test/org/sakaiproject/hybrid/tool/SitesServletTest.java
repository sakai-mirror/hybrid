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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.api.app.messageforums.SynopticMsgcntrItem;
import org.sakaiproject.api.app.messageforums.SynopticMsgcntrManager;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.EntityPropertyNotDefinedException;
import org.sakaiproject.entity.api.EntityPropertyTypeException;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.User;

@RunWith(MockitoJUnitRunner.class)
public class SitesServletTest {
	private static final String UID = "admin";
	private static final String EID = UID;

	@Mock
	protected SitesServlet sitesServlet;
	@Mock
	protected SessionManager sessionManager;
	@Mock
	protected SiteService siteService;
	@Mock
	protected SynopticMsgcntrManager synopticMsgcntrManager;
	@Mock
	protected HttpServletRequest request;
	@Mock
	protected HttpServletResponse response;
	@Mock
	protected ComponentManager componentManager;
	@Mock
	protected ServerConfigurationService serverConfigurationService;
	@Mock
	protected transient PreferencesService preferencesService;
	@Mock
	protected ServletConfig config;
	@Mock
	protected Session session;
	@Mock(name = "!admin")
	protected Site site;
	@Mock(name = "My Workspace")
	protected Site myWorkSpace;
	@Mock
	protected User user;
	@Mock
	protected Set<Member> members;
	@Mock
	protected PrintWriter writer;
	@Mock
	protected Preferences preferences;
	@Mock
	protected ResourceProperties resourceProperties;
	@Mock
	SynopticMsgcntrItem synopticMsgcntrItem1;

	@BeforeClass
	public static void beforeClass() {
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
	}

	@Before
	public void setUp() throws Exception {
		when(sessionManager.getCurrentSession()).thenReturn(session);
		when(session.getUserEid()).thenReturn(EID);
		when(sessionManager.getCurrentSessionUserId()).thenReturn(UID);
		when(site.getTitle()).thenReturn("Administration Workspace");
		when(site.getId()).thenReturn("!admin");
		when(site.getUrl())
				.thenReturn(
						"http://sakai3-nightly.uits.indiana.edu:8080/portal/site/!admin");
		when(site.getIconUrl()).thenReturn(null);
		when(user.getDisplayName()).thenReturn("Sakai Administrator");
		when(site.getCreatedBy()).thenReturn(user);
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
		when(siteService.getUserSiteId(UID)).thenReturn("~admin");
		when(siteService.getSite("~admin")).thenReturn(myWorkSpace);
		when(myWorkSpace.getId()).thenReturn("~admin");
		when(preferencesService.getPreferences(UID)).thenReturn(preferences);
		when(preferences.getProperties("sakai:portal:sitenav")).thenReturn(
				resourceProperties);
		when(resourceProperties.getLongProperty("tabs")).thenReturn(11L);
		when(response.getWriter()).thenReturn(writer);
		when(componentManager.get(SessionManager.class)).thenReturn(
				sessionManager);
		when(componentManager.get(SiteService.class)).thenReturn(siteService);
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(serverConfigurationService);
		when(componentManager.get(SynopticMsgcntrManager.class)).thenReturn(
				synopticMsgcntrManager);
		when(componentManager.get(PreferencesService.class)).thenReturn(
				preferencesService);
		when(request.getParameter(SitesServlet.CATEGORIZED)).thenReturn("true");
		when(request.getParameter(SitesServlet.UNREAD)).thenReturn("true");
		when(request.getParameter(SitesServlet.LOCALE)).thenReturn("en_US");
		when(request.getLocale()).thenReturn(Locale.getDefault());
		when(synopticMsgcntrItem1.getSiteId()).thenReturn("!admin");
		when(synopticMsgcntrItem1.getNewForumCount()).thenReturn(7);
		when(synopticMsgcntrItem1.getNewMessagesCount()).thenReturn(13);
		final List<SynopticMsgcntrItem> synopticMsgcntrItems = new ArrayList<SynopticMsgcntrItem>();
		synopticMsgcntrItems.add(synopticMsgcntrItem1);
		when(synopticMsgcntrManager.getWorkspaceSynopticMsgcntrItems(UID))
				.thenReturn(synopticMsgcntrItems);
		sitesServlet = new SitesServlet();
		sitesServlet.setupTestCase(componentManager);
		sitesServlet.init(config);
	}

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	@Test
	public void testDoGetNormalBehavior() {
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
	@Test
	public void testDoGetNullEid() {
		when(session.getUserEid()).thenReturn(null);
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
	@Test
	public void testDoGetEmptyEid() {
		when(session.getUserEid()).thenReturn("");
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
	@Test
	public void testDoGetLocaleLanguage() {
		when(request.getParameter(SitesServlet.LOCALE)).thenReturn("es");
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
	@Test
	public void testDoGetLocaleWithVariant() {
		when(request.getParameter(SitesServlet.LOCALE)).thenReturn(
				"es_ES_Traditional");
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
	@Test
	public void testDoGetLocaleWithVariant2() {
		/*
		 * While multiple variants is not currently supported in the get
		 * parameter parser, they should not break anything.
		 */
		when(request.getParameter(SitesServlet.LOCALE)).thenReturn(
				"es_ES_Traditional_MAC");
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
	@Test
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
	@Test
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
	@Test
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
	@Test
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
	 * Tests {@link SitesServlet#init(ServletConfig)}
	 */
	@Test
	public void testInitNullSynopticMsgcntrManager() {
		when(componentManager.get(SynopticMsgcntrManager.class)).thenReturn(
				null);
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
	@Test
	public void testInitNullPreferencesService() {
		when(componentManager.get(PreferencesService.class)).thenReturn(null);
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
	@Test
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

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	@Test
	public void testEntityPropertyNotDefinedException() {
		try {
			when(resourceProperties.getLongProperty("tabs")).thenThrow(
					new EntityPropertyNotDefinedException());
			sitesServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown: " + e, e);
		}
	}

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	@Test
	public void testEntityPropertyTypeException() {
		try {
			when(resourceProperties.getLongProperty("tabs")).thenThrow(
					new EntityPropertyTypeException("message"));
			sitesServlet.doGet(request, response);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull("IllegalStateException should be thrown: " + e, e);
		} catch (Throwable e) {
			fail("IllegalStateException should be thrown");
		}
	}

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	@Test
	public void testNullPreferences() {
		when(preferencesService.getPreferences(UID)).thenReturn(null);
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
	@Test
	public void testIdUnusedException() {
		try {
			when(siteService.getSite("~admin")).thenThrow(
					new IdUnusedException("message"));
			sitesServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown: " + e, e);
		}
	}

	/**
	 * Tests {@link SitesServlet#doGet(HttpServletRequest, HttpServletResponse)}
	 */
	@Test
	public void testDoGetNotCategorized() {
		when(request.getParameter(SitesServlet.CATEGORIZED))
				.thenReturn("false");
		try {
			sitesServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown: " + e, e);
		}
	}

}
