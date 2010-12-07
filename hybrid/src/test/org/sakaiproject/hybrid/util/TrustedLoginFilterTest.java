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
package org.sakaiproject.hybrid.util;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

@RunWith(MockitoJUnitRunner.class)
public class TrustedLoginFilterTest extends TestCase {
	TrustedLoginFilter trustedLoginFilter = null;
	@Mock
	protected transient ComponentManager componentManager;
	@Mock
	protected ServerConfigurationService serverConfigurationService;
	@Mock
	HttpServletRequest request;
	@Mock
	HttpServletResponse response;
	@Mock
	FilterChain chain;
	@Mock
	SessionManager sessionManager;
	@Mock
	UserDirectoryService userDirectoryService;
	@Mock
	Session existingSession;
	@Mock
	Session newSession;
	@Mock
	User user;
	@Mock
	FilterConfig config;
	@Mock
	ServletRequest servletRequest;

	@BeforeClass
	public static void setupClass() {
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

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		trustedLoginFilter = new TrustedLoginFilter();
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(serverConfigurationService);
		when(componentManager.get(SessionManager.class)).thenReturn(
				sessionManager);
		when(componentManager.get(UserDirectoryService.class)).thenReturn(
				userDirectoryService);
		when(
				serverConfigurationService
						.getBoolean(
								TrustedLoginFilter.ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_ENABLED,
								true)).thenReturn(true);
		when(
				serverConfigurationService
						.getString(
								TrustedLoginFilter.ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SHARED_SECRET,
								null)).thenReturn("e2KS54H35j6vS5Z38nK40");
		when(
				serverConfigurationService
						.getString(
								TrustedLoginFilter.ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SAFE_HOSTS,
								trustedLoginFilter.safeHosts)).thenReturn(
				trustedLoginFilter.safeHosts);
		when(request.getRemoteHost()).thenReturn("localhost");
		when(request.getHeader("x-sakai-token")).thenReturn(
				"sw9TTTqlEbGQkELqQuQPq92ydr4=;username;nonce");
		// default to non-existing session to exercise more code
		when(existingSession.getUserEid()).thenReturn(null);
		when(sessionManager.getCurrentSession()).thenReturn(existingSession);
		when(sessionManager.startSession()).thenReturn(newSession);
		when(user.getEid()).thenReturn("username");
		when(user.getId()).thenReturn("uuid1234567890");
		when(userDirectoryService.getUserByEid("username")).thenReturn(user);

		trustedLoginFilter.setupTestCase(componentManager);
		trustedLoginFilter.init(config);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterDefaultBehaviorNewSession() throws IOException,
			ServletException {
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager).startSession();
		verify(sessionManager, times(1)).setCurrentSession(newSession);
		verify(sessionManager, times(1)).setCurrentSession(existingSession);
		verify(sessionManager, times(2)).setCurrentSession(isA(Session.class));
		verify(newSession).setActive();
		verify(chain).doFilter(isA(ToolRequestWrapper.class), eq(response));
		verify(chain, never()).doFilter(request, response);
		verify(newSession).invalidate();
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterDefaultBehaviorExistingSession()
			throws IOException, ServletException {
		// eid of existing session should match; i.e. reuse existing session.
		when(existingSession.getUserEid()).thenReturn("username");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager).setCurrentSession(existingSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterUnsafeHost() throws IOException, ServletException {
		when(request.getRemoteHost()).thenReturn("big.bad.hacker.com");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager, never()).setCurrentSession(existingSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterNullToken() throws IOException, ServletException {
		when(request.getHeader("x-sakai-token")).thenReturn(null);
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager, never()).setCurrentSession(existingSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterIllegalToken() throws IOException, ServletException {
		// missing nonce (i.e. not three part token)
		when(request.getHeader("x-sakai-token")).thenReturn(
				"sw9TTTqlEbGQkELqQuQPq92ydr4=;username");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager, never()).setCurrentSession(existingSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterBadHmac() throws IOException, ServletException {
		when(request.getHeader("x-sakai-token")).thenReturn(
				"badhash;username;nonce");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager, never()).setCurrentSession(existingSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterUserNotDefinedException() throws IOException,
			ServletException {
		try {
			when(userDirectoryService.getUserByEid("username")).thenThrow(
					new UserNotDefinedException("username"));
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterDisabled() throws IOException, ServletException {
		trustedLoginFilter.enabled = false;
		try {
			trustedLoginFilter.doFilter(request, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager, never()).setCurrentSession(existingSession);
		verify(chain).doFilter(request, response);
	}

	/**
	 * Test with a regular ServletRequest instead of a HttpServletRequest.
	 * Increases cobertura coverage. Test method for
	 * {@link org.sakaiproject.hybrid.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 * 
	 * @throws ServletException
	 * @throws IOException
	 */
	@Test
	public void testDoFilterServletRequest() throws IOException,
			ServletException {
		try {
			trustedLoginFilter.doFilter(servletRequest, response, chain);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
		verify(sessionManager, never()).startSession();
		verify(sessionManager, never()).setCurrentSession(newSession);
		verify(sessionManager, never()).setCurrentSession(existingSession);
		verify(chain).doFilter(servletRequest, response);
	}

	/**
	 * @see TrustedLoginFilter#init(FilterConfig)
	 */
	@Test
	public void testNullServerConfigurationService() {
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(null);
		try {
			trustedLoginFilter.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull(e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}

	}

	/**
	 * @see TrustedLoginFilter#init(FilterConfig)
	 */
	@Test
	public void testNullSessionManager() {
		when(componentManager.get(SessionManager.class)).thenReturn(null);
		try {
			trustedLoginFilter.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull("IllegalStateException should be thrown", e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}

	}

	/**
	 * @see TrustedLoginFilter#init(FilterConfig)
	 */
	@Test
	public void testNullUserDirectoryService() {
		when(componentManager.get(UserDirectoryService.class)).thenReturn(null);
		try {
			trustedLoginFilter.init(config);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull("IllegalStateException should be thrown", e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}

	}

	/**
	 * @see TrustedLoginFilter#setupTestCase(ComponentManager)
	 */
	@Test
	public void testSetupTestCase() {
		try {
			trustedLoginFilter.setupTestCase(null);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalStateException should be thrown", e);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see TrustedLoginFilter#setupTestCase(ComponentManager)
	 */
	@Test
	public void testDestroy() {
		try {
			trustedLoginFilter.destroy();
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

}
