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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.hybrid.test.TestHelper.disableLog4jDebug;
import static org.sakaiproject.hybrid.test.TestHelper.enableLog4jDebug;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.hybrid.tool.SiteVisitToolPlacementServlet.ResponseCommittedException;
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
	protected transient ComponentManager componentManager;
	@Mock
	protected transient SecurityService securityService;
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
	@Mock
	protected ServletConfig config;

	@BeforeClass
	public static void beforeClass() {
		enableLog4jDebug();
	}

	@Before
	public void setUp() throws Exception {
		when(componentManager.get(SecurityService.class)).thenReturn(
				securityService);
		when(componentManager.get(SessionManager.class)).thenReturn(
				sessionManager);
		when(componentManager.get(SiteService.class)).thenReturn(siteService);
		when(componentManager.get(EventTrackingService.class)).thenReturn(
				eventTrackingService);
		when(componentManager.get(AuthzGroupService.class)).thenReturn(
				authzGroupService);

		when(toolHelper.allowTool(any(Site.class), any(Placement.class)))
				.thenReturn(true);

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

		siteVisitToolPlacementServlet = new SiteVisitToolPlacementServlet();
		siteVisitToolPlacementServlet.setupTestCase(componentManager);
		siteVisitToolPlacementServlet.init(config);
		siteVisitToolPlacementServlet.toolHelper = toolHelper;
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
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

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
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

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
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

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorAllowToolFalse() {
		when(toolHelper.allowTool(any(Site.class), any(Placement.class)))
				.thenReturn(false);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorLogDebugDisabled() {
		disableLog4jDebug();
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
		enableLog4jDebug();
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorNullTool() {
		when(toolConfig.getTool()).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorNullToolId() {
		when(tool.getId()).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorNullOrderedPages() {
		when(site.getOrderedPages()).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorNullTools() {
		when(page.getTools()).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorEmptyTools() {
		final List<ToolConfiguration> list = Collections.emptyList();
		when(page.getTools()).thenReturn(list);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorWriteEvent() {
		when(request.getParameter("writeEvent")).thenReturn("true");
		when(
				eventTrackingService.newEvent(anyString(), anyString(),
						anyBoolean())).thenReturn(event);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).setStatus(HttpServletResponse.SC_OK);
		} catch (Throwable e) {
			e.printStackTrace();
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testIdUnusedException() {
		try {
			when(siteService.getSiteVisit("!admin")).thenThrow(
					new IdUnusedException("!admin"));
			siteVisitToolPlacementServlet.doGet(request, response);
			verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
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

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
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

	/**
	 * @see ResponseCommittedException#ResponseCommittedException(String)
	 */
	@Test
	public void testResponseCommittedException() {
		try {
			final ResponseCommittedException responseCommittedException = new ResponseCommittedException(
					"message");
			assertNotNull(responseCommittedException);
		} catch (Throwable e) {
			assertNull("Exception should not be thrown", e);
		}
	}

	/**
	 * @throws ServletException
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testInitConfig() throws ServletException {
		final SiteVisitToolPlacementServlet siteVisitToolPlacementServlet = new SiteVisitToolPlacementServlet();
		siteVisitToolPlacementServlet.componentManager = componentManager;
		siteVisitToolPlacementServlet.init(config);
	}

	/**
	 * @throws ServletException
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test(expected = IllegalStateException.class)
	public void testInitConfigIllegalStateException() throws ServletException {
		final SiteVisitToolPlacementServlet siteVisitToolPlacementServlet = new SiteVisitToolPlacementServlet();
		siteVisitToolPlacementServlet.init(config);
	}

	/**
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testInitNullSessionManager() {
		when(componentManager.get(SessionManager.class)).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testInitNullSiteService() {
		when(componentManager.get(SiteService.class)).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testInitNullEventTrackingService() {
		when(componentManager.get(EventTrackingService.class)).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testInitNullAuthzGroupService() {
		when(componentManager.get(AuthzGroupService.class)).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testInitNullSecurityService() {
		when(componentManager.get(SecurityService.class)).thenReturn(null);
		try {
			siteVisitToolPlacementServlet.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#init(ServletConfig)
	 */
	@Test
	public void testSetupTestCase() {
		try {
			siteVisitToolPlacementServlet.setupTestCase(null);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#sendError(HttpServletResponse, int,
	 *      String)
	 */
	@Test
	public void testSendError() {
		try {
			siteVisitToolPlacementServlet.sendError(response,
					HttpServletResponse.SC_BAD_REQUEST, "message");
			verify(response, times(1)).sendError(
					HttpServletResponse.SC_BAD_REQUEST);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#sendError(HttpServletResponse, int,
	 *      String)
	 */
	@Test
	public void testSendErrorWhenResponseCommitted() {
		when(response.isCommitted()).thenReturn(true);
		try {
			siteVisitToolPlacementServlet.sendError(response,
					HttpServletResponse.SC_BAD_REQUEST, "message");
			fail("ResponseCommittedException should be thrown");
		} catch (ResponseCommittedException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#canAccessAtLeastOneTool(Site)
	 */
	@Test
	public void testCanAccessAtLeastOneToolNoPages() {
		when(site.getOrderedPages()).thenReturn(null);
		assertFalse(siteVisitToolPlacementServlet.canAccessAtLeastOneTool(site,
				null));
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorNullSiteIdCommittedResponse() {
		when(request.getParameter("siteId")).thenReturn(null);
		when(response.isCommitted()).thenReturn(true);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			fail("IllegalAccessError should be thrown");
		} catch (IllegalAccessError e) {
			assertNotNull("IllegalAccessError should be thrown", e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see SiteVisitToolPlacementServlet#doGet(HttpServletRequest,
	 *      HttpServletResponse)
	 */
	@Test
	public void testNormalBehaviorEmptySiteIdCommittedResponse() {
		when(request.getParameter("siteId")).thenReturn("");
		when(response.isCommitted()).thenReturn(true);
		try {
			siteVisitToolPlacementServlet.doGet(request, response);
			fail("IllegalAccessError should be thrown");
		} catch (IllegalAccessError e) {
			assertNotNull("IllegalAccessError should be thrown", e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

}
