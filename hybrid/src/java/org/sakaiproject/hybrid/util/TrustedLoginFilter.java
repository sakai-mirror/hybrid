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

import java.io.IOException;
import java.security.SignatureException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * <pre>
 *  A filter to come after the standard sakai request filter to allow services
 *  to encode a token containing the user id accessing the service. 
 *  
 *  The filter must be configured with a shared secret and requests contain a 
 *  header "x-sakai-token". This token is used to validate the Request and
 *  associate a user with the request.
 *  
 *  The token contains:
 *  hash;user
 *  
 *  hash is a Base64 encoded HMAC hash, user is the username to associate with the request.
 *  
 *  The shared secret must be known by both ends of the conversation, and must not be distributed outside a trusted zone.
 *  
 *  To use this filter add it AFTER the Sakai Request Filter in you web.xml like
 *  
 *  
 *  	&lt;!-- 
 * 	The Sakai Request Hander 
 * 	--&gt;
 * 	&lt;filter&gt;
 * 		&lt;filter-name&gt;sakai.request&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;org.sakaiproject.util.RequestFilter&lt;/filter-class&gt;
 * 	&lt;/filter&gt;
 * 	&lt;filter&gt;
 * 		&lt;filter-name&gt;sakai.trusted&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;org.sakaiproject.hybrid.util.TrustedLoginFilter&lt;/filter-class&gt;
 *       &lt;init-param&gt;
 *       	&lt;param-name&gt;shared.secret&lt;/param-name&gt;
 *           &lt;param-value&gt;The Snow on the Volga falls only under the bridges&lt;/param-value&gt;
 *       &lt;/init-param&gt;
 * 	&lt;/filter&gt;
 * 	
 * 	&lt;!--
 * 	Mapped onto Handler
 * 	--&gt;
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;sakai.request&lt;/filter-name&gt;
 * 		&lt;servlet-name&gt;sakai.mytoolservlet&lt;/servlet-name&gt;
 * 		&lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;INCLUDE&lt;/dispatcher&gt;
 * 	&lt;/filter-mapping&gt; 
 * 
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;sakai.trusted&lt;/filter-name&gt;
 * 		&lt;servlet-name&gt;sakai.mytoolservlet&lt;/servlet-name&gt;
 * 		&lt;dispatcher&gt;REQUEST&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;FORWARD&lt;/dispatcher&gt;
 * 		&lt;dispatcher&gt;INCLUDE&lt;/dispatcher&gt;
 * 	&lt;/filter-mapping&gt;
 * 
 * </pre>
 * 
 */
@SuppressWarnings("PMD.LongVariable")
public class TrustedLoginFilter implements Filter {
	private final static Log LOG = LogFactory.getLog(TrustedLoginFilter.class);
	private static final String ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SHARED_SECRET = "org.sakaiproject.hybrid.util.TrustedLoginFilter.sharedSecret";
	private static final String ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_ENABLED = "org.sakaiproject.hybrid.util.TrustedLoginFilter.enabled";
	private static final String ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SAFE_HOSTS = "org.sakaiproject.hybrid.util.TrustedLoginFilter.safeHosts";
	private static final String TOKEN_SEPARATOR = ";";

	protected SessionManager sessionManager;
	protected UserDirectoryService userDirectoryService;

	/**
	 * Property to contain the shared secret used by all trusted servers. The
	 * shared secret used for server to server trusted tokens.
	 */
	protected String sharedSecret = null;
	/**
	 * True if server tokens are enabled. If true, trusted tokens from servers
	 * are accepted considered.
	 */
	protected boolean enabled = true;
	/**
	 * A list of all the known safe hosts to trust as servers. A ; separated
	 * list of hosts that this instance trusts to make server connections.
	 */
	protected String safeHosts = ";localhost;";

	/**
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		if (enabled && req instanceof HttpServletRequest) {
			HttpServletRequest hreq = (HttpServletRequest) req;
			final String host = req.getRemoteHost();
			if (safeHosts.indexOf(host) < 0) {
				LOG.warn("Ignoring Trusted Token request from: " + host);
				chain.doFilter(req, resp);
				return;
			} else {
				final String token = hreq.getHeader("x-sakai-token");
				Session currentSession = null;
				Session requestSession = null;
				if (token != null) {
					final String trustedUserName = decodeToken(token);
					if (trustedUserName != null) {
						currentSession = sessionManager.getCurrentSession();
						if (!trustedUserName
								.equals(currentSession.getUserEid())) {
							org.sakaiproject.user.api.User user = null;
							try {
								user = userDirectoryService
										.getUserByEid(trustedUserName);
							} catch (UserNotDefinedException e) {
								LOG.warn(trustedUserName + " not found!");
							}
							if (user != null) {
								requestSession = sessionManager.startSession();
								requestSession.setUserEid(user.getEid());
								requestSession.setUserId(user.getId());
								requestSession.setActive();
								sessionManager
										.setCurrentSession(requestSession);
								// wrap the request so that we can get the user
								// via getRemoteUser() in other places.
								if (!(hreq instanceof ToolRequestWrapper)) {
									hreq = new ToolRequestWrapper(hreq,
											trustedUserName);
								}
							}
						}
					}
				}
				try {
					chain.doFilter(hreq, resp);
				} finally {
					if (requestSession != null) {
						requestSession.invalidate();
					}
					if (currentSession != null) {
						sessionManager.setCurrentSession(currentSession);
					}
				}
			}
		} else {
			chain.doFilter(req, resp);
			return;
		}
	}

	/**
	 * @param token
	 * @return
	 */
	protected String decodeToken(String token) {
		String userId = null;
		final String[] parts = token.split(TOKEN_SEPARATOR);
		if (parts.length == 3) {
			try {
				final String hash = parts[0];
				final String user = parts[1];
				final String timestamp = parts[2];
				final String message = user + TOKEN_SEPARATOR + timestamp;
				final String hmac = Signature.calculateRFC2104HMAC(message,
						sharedSecret);
				if (hmac.equals(hash)) {
					// the user is Ok, we will trust it.
					userId = user;
				}
			} catch (SignatureException e) {
				LOG.error("Failed to validate server token: " + token, e);
			}
		} else {
			LOG.error("Illegal number of elements in trusted server token: "
					+ token);
		}
		return userId;
	}

	/**
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig config) throws ServletException {
		sessionManager = (SessionManager) ComponentManager
				.get(org.sakaiproject.tool.api.SessionManager.class);
		if (sessionManager == null) {
			throw new IllegalStateException("SessionManager == null");
		}
		userDirectoryService = (UserDirectoryService) ComponentManager
				.get(org.sakaiproject.user.api.UserDirectoryService.class);
		if (userDirectoryService == null) {
			throw new IllegalStateException("UserDirectoryService == null");
		}
		// default to true - enabled
		enabled = ServerConfigurationService.getBoolean(
				ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_ENABLED, enabled);
		sharedSecret = ServerConfigurationService.getString(
				ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SHARED_SECRET,
				sharedSecret);
		// default to localhost
		safeHosts = ServerConfigurationService.getString(
				ORG_SAKAIPROJECT_UTIL_TRUSTED_LOGIN_FILTER_SAFE_HOSTS,
				safeHosts);
	}

	/**
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
		// nothing to do here
	}

}
