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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.mockito.InOrder;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

public class TrustedLoginFilterTest extends TestCase {
	TrustedLoginFilter trustedLoginFilter;
	HttpServletRequest request;
	HttpServletResponse response;
	FilterChain chain;
	SessionManager sessionManager;
	UserDirectoryService userDirectoryService;
	Session existingSession;
	Session newSession;

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		trustedLoginFilter = new TrustedLoginFilter();
		request = mock(HttpServletRequest.class);
		response = mock(HttpServletResponse.class);
		when(request.getRemoteHost()).thenReturn("localhost");
		chain = mock(FilterChain.class);
		when(request.getHeader("x-sakai-token")).thenReturn(
				"sw9TTTqlEbGQkELqQuQPq92ydr4=;username;nonce");
		trustedLoginFilter.sharedSecret = "e2KS54H35j6vS5Z38nK40";
		sessionManager = mock(SessionManager.class);
		trustedLoginFilter.sessionManager = sessionManager;
		existingSession = mock(Session.class, "existing Session");
		// default to non-existing session to exercise more code
		when(existingSession.getUserEid()).thenReturn(null);
		when(sessionManager.getCurrentSession()).thenReturn(existingSession);
		userDirectoryService = mock(UserDirectoryService.class);
		trustedLoginFilter.userDirectoryService = userDirectoryService;
		newSession = mock(Session.class, "new Session");
		when(sessionManager.startSession()).thenReturn(newSession);
		User user = mock(User.class);
		when(user.getEid()).thenReturn("username");
		when(user.getId()).thenReturn("uuid1234567890");
		when(userDirectoryService.getUserByEid("username")).thenReturn(user);
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.TrustedLoginFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)}
	 * .
	 */
	public void testDoFilterDefaultBehaviorNewSession() {
		try {
			InOrder inOrder = inOrder(sessionManager);
			trustedLoginFilter.doFilter(request, response, chain);
			verify(sessionManager).startSession();
			inOrder.verify(sessionManager).setCurrentSession(newSession);
			inOrder.verify(sessionManager).setCurrentSession(existingSession);
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
