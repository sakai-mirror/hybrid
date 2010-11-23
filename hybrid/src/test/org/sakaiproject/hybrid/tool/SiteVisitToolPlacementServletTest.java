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

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.authz.api.GroupNotDefinedException;
import org.sakaiproject.authz.api.Role;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.Tool;

@RunWith(MockitoJUnitRunner.class)
public class SiteVisitToolPlacementServletTest {
	protected SiteVisitToolPlacementServlet siteVisitToolPlacementServlet;

	@Mock
	protected SessionManager sessionManager;
	@Mock
	protected SiteService siteService;
	@Mock
	protected EventTrackingService eventTrackingService;
	@Mock
	protected AuthzGroupService authzGroupService;
	@Mock
	protected ToolHelperImpl toolHelper;
	@Mock
	protected HttpServletRequest request;
	@Mock
	protected HttpServletResponse response;
	@Mock
	protected Session session;
	@Mock
	protected Site site;
	@Mock
	protected Role role;
	@Mock
	protected SitePage page;
	@Mock
	protected ToolConfiguration toolConfig;
	@Mock
	protected Tool tool;
	@Mock
	protected AuthzGroup group;
	@Mock
	protected PrintWriter writer;
	@Mock
	protected Event event;

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
		siteVisitToolPlacementServlet = new SiteVisitToolPlacementServlet();
		siteVisitToolPlacementServlet.sessionManager = sessionManager;
		siteVisitToolPlacementServlet.siteService = siteService;
		siteVisitToolPlacementServlet.eventTrackingService = eventTrackingService;
		siteVisitToolPlacementServlet.authzGroupService = authzGroupService;
		when(toolHelper.allowTool(any(Site.class), any(Placement.class)))
				.thenReturn(true);
		siteVisitToolPlacementServlet.toolHelper = toolHelper;
		siteVisitToolPlacementServlet.authzGroupService = authzGroupService;

		// pass siteId parameter
		when(request.getParameter("siteId")).thenReturn("!admin");

		when(sessionManager.getCurrentSession()).thenReturn(session);
		when(session.getUserEid()).thenReturn("admin");

		when(site.getTitle()).thenReturn("Administration Workspace");
		when(site.getId()).thenReturn("!admin");
		when(siteService.getSiteVisit("!admin")).thenReturn(site);

		when(role.getId()).thenReturn("admin");
		when(role.getDescription()).thenReturn(null);
		final Set<Role> roles = new HashSet<Role>();
		roles.add(role);

		when(page.getId()).thenReturn("!admin-100");
		when(page.getTitle()).thenReturn("Home");
		when(page.getLayout()).thenReturn(0);
		when(page.isPopUp()).thenReturn(false);
		List<ToolConfiguration> tools = new ArrayList<ToolConfiguration>();
		when(toolConfig.getToolId()).thenReturn("sakai.motd");
		when(toolConfig.getId()).thenReturn("!admin-110");
		when(tool.getId()).thenReturn("!admin-110");
		when(tool.getTitle()).thenReturn("Message of The Day");
		when(toolConfig.getTool()).thenReturn(tool);
		tools.add(toolConfig);
		when(page.getTools()).thenReturn(tools);

		List<SitePage> pages = new ArrayList<SitePage>();
		pages.add(page);
		when(site.getOrderedPages()).thenReturn(pages);

		when(group.getRoles()).thenReturn(roles);
		when(authzGroupService.getAuthzGroup(anyString())).thenReturn(group);

		when(response.getWriter()).thenReturn(writer);
	}

	@Test
	public void testNullSiteId() {
		try {
			when(request.getParameter("siteId")).thenReturn(null);
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

	@Test
	public void testNullSite() {
		try {
			when(siteService.getSiteVisit("!admin")).thenReturn(null);
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

	@Test
	public void testNormalBehavior() {
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	@Test
	public void testNormalBehaviorWriteEvent() {
		when(request.getParameter("writeEvent")).thenReturn("true");
		when(
				eventTrackingService.newEvent(anyString(), anyString(),
						anyBoolean())).thenReturn(event);
		siteVisitToolPlacementServlet.eventTrackingService = eventTrackingService;
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	@Test
	public void testIdUnusedException() {
		try {
			when(siteService.getSiteVisit("!admin")).thenThrow(
					new IdUnusedException(""));
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

	@Test
	public void testPermissionException() {
		try {
			when(siteService.getSiteVisit("!admin")).thenThrow(
					new PermissionException("w", "w", "w"));
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

	@Test
	public void testGroupNotDefinedException() {
		try {
			when(authzGroupService.getAuthzGroup(anyString())).thenThrow(
					new GroupNotDefinedException(""));
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

}
