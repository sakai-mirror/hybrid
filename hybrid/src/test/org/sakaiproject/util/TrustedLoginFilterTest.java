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
package org.sakaiproject.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

@RunWith(MockitoJUnitRunner.class)
public class TrustedLoginFilterTest extends TestCase {
	TrustedLoginFilter trustedLoginFilter;
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

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		trustedLoginFilter = new TrustedLoginFilter();
		when(request.getRemoteHost()).thenReturn("localhost");
		when(request.getHeader("x-sakai-token")).thenReturn(
				"sw9TTTqlEbGQkELqQuQPq92ydr4=;username;nonce");
		trustedLoginFilter.sharedSecret = "e2KS54H35j6vS5Z38nK40";
		trustedLoginFilter.sessionManager = sessionManager;
		// default to non-existing session to exercise more code
		when(existingSession.getUserEid()).thenReturn(null);
		when(sessionManager.getCurrentSession()).thenReturn(existingSession);
		trustedLoginFilter.userDirectoryService = userDirectoryService;
		when(sessionManager.startSession()).thenReturn(newSession);
		when(user.getEid()).thenReturn("username");
		when(user.getId()).thenReturn("uuid1234567890");
		when(userDirectoryService.getUserByEid("username")).thenReturn(user);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterDefaultBehaviorNewSession() {
		try {
			trustedLoginFilter.doFilter(request, response, chain);
			verify(sessionManager).startSession();
			verify(sessionManager).setCurrentSession(newSession);
			verify(sessionManager).setCurrentSession(existingSession);
			verify(sessionManager, times(2)).setCurrentSession(
					any(Session.class));
			verify(newSession).setActive();
			verify(chain).doFilter(any(ToolRequestWrapper.class), eq(response));
			verify(chain, never()).doFilter(request, response);
			verify(newSession).invalidate();
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterDefaultBehaviorExistingSession() {
		// eid of existing session should match; i.e. reuse existing session.
		when(existingSession.getUserEid()).thenReturn("username");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
			verify(sessionManager, never()).startSession();
			verify(sessionManager, never()).setCurrentSession(newSession);
			verify(sessionManager).setCurrentSession(existingSession);
			verify(chain).doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterUnsafeHost() {
		when(request.getRemoteHost()).thenReturn("big.bad.hacker.com");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterNullToken() {
		when(request.getHeader("x-sakai-token")).thenReturn(null);
		try {
			trustedLoginFilter.doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterBadHmac() {
		when(request.getHeader("x-sakai-token")).thenReturn(
				"badhash;username;nonce");
		try {
			trustedLoginFilter.doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterUserNotDefinedException() {
		try {
			when(userDirectoryService.getUserByEid("username")).thenThrow(
					new UserNotDefinedException("username"));
			trustedLoginFilter.doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	@Test
	public void testDoFilterDisabled() {
		trustedLoginFilter.enabled = false;
		try {
			trustedLoginFilter.doFilter(request, response, chain);
			verify(chain).doFilter(request, response);
		} catch (Exception e) {
			e.printStackTrace();
			assertNull(e);
		}
	}

}
