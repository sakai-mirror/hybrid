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

import java.net.URI;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.sakaiproject.thread_local.api.ThreadLocalManager;

/**
 * Useful helper for interacting with Nakamura's authentication REST end-points.
 * Note: thread safe.
 */
public class NakamuraAuthenticationHelper {
	private static final Log LOG = LogFactory
			.getLog(NakamuraAuthenticationHelper.class);

	/**
	 * The name of the cookie that is set by nakamura.
	 */
	private static final String COOKIE_NAME = "SAKAI-TRACKING";
	/**
	 * The anonymous nakamura principal name.
	 */
	private static final String ANONYMOUS = "anonymous";
	/**
	 * The key that will be used to cache AuthInfo hits in ThreadLocal. This
	 * will handle cases where AuthInfo is requested more than once per request.
	 */
	private static final String THREAD_LOCAL_CACHE_KEY = NakamuraAuthenticationHelper.class
			.getName() + ".AuthInfo.cache";

	/**
	 * The Nakamura RESTful service to validate authenticated users
	 */
	protected String validateUrl;

	/**
	 * The nakamura user that has permissions to GET
	 * /var/cluster/user.cookie.json.
	 */
	protected String principal;

	/**
	 * The hostname we will use to lookup the sharedSecret for access to
	 * validateUrl.
	 */
	protected String hostname;

	// dependencies
	private ThreadLocalManager threadLocalManager;

	/**
	 * Class is immutable and thread safe.
	 * 
	 * @param threadLocalManager
	 *            Required. Used to get a reference to
	 *            {@link HttpServletRequest} from {@link ThreadLocalManager}
	 * @param validateUrl
	 *            The Nakamura REST end-point we will use to validate the
	 *            cookie.
	 * @param principal
	 *            The principal that will be used when connecting to Nakamura
	 *            REST end-point. Must have permissions to read
	 *            /var/cluster/user.cookie.json.
	 * @param hostname
	 *            The hostname we will use to lookup the sharedSecret for access
	 *            to validateUrl
	 * @throws IllegalArgumentException
	 */
	public NakamuraAuthenticationHelper(ThreadLocalManager threadLocalManager,
			String validateUrl, String principal, String hostname) {
		if (threadLocalManager == null) {
			throw new IllegalArgumentException("threadLocalManager == null");
		}
		if (validateUrl == null || "".equals(validateUrl)) {
			throw new IllegalArgumentException("validateUrl == null OR empty");
		}
		if (principal == null || "".equals(principal)) {
			throw new IllegalArgumentException("principal == null OR empty");
		}
		if (hostname == null || "".equals(hostname)) {
			throw new IllegalArgumentException("hostname == null OR empty");
		}
		this.threadLocalManager = threadLocalManager;
		this.validateUrl = validateUrl;
		this.principal = principal;
		this.hostname = hostname;
	}

	/**
	 * Calls Nakamura to determine the identity of the current user.
	 * 
	 * @param request
	 * @return null if user cannot be authenticated.
	 * @throws IllegalArgumentException
	 */
	public AuthInfo getPrincipalLoggedIntoNakamura(HttpServletRequest request) {
		LOG.debug("getPrincipalLoggedIntoNakamura(HttpServletRequest request)");
		if (request == null) {
			throw new IllegalArgumentException("HttpServletRequest == null");
		}
		final Object cache = threadLocalManager.get(THREAD_LOCAL_CACHE_KEY);
		if (cache != null && cache instanceof AuthInfo) {
			LOG.debug("cache hit!");
			return (AuthInfo) cache;
		}
		AuthInfo authInfo = null;
		final String secret = getSecret(request);
		if (secret != null) {
			final DefaultHttpClient http = new DefaultHttpClient();
			try {
				final URI uri = new URI(validateUrl + secret);
				final HttpGet httpget = new HttpGet(uri);
				// authenticate to Nakamura using x-sakai-token mechanism
				final String token = XSakaiToken.createToken(hostname,
						principal);
				httpget.addHeader(XSakaiToken.X_SAKAI_TOKEN_HEADER, token);
				//
				final ResponseHandler<String> responseHandler = new BasicResponseHandler();
				final String responseBody = http.execute(httpget,
						responseHandler);
				authInfo = new AuthInfo(responseBody);
			} catch (HttpResponseException e) {
				// usually a 404 error - could not find cookie / not valid
				if (LOG.isDebugEnabled()) {
					LOG.debug("HttpResponseException: " + e.getMessage() + ": "
							+ e.getStatusCode() + ": " + validateUrl + secret);
				}
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
				throw new Error(e);
			} finally {
				http.getConnectionManager().shutdown();
			}
		}

		// cache results in thread local
		threadLocalManager.set(THREAD_LOCAL_CACHE_KEY, authInfo);

		return authInfo;
	}

	/**
	 * Gets the authentication key from SAKAI-TRACKING cookie.
	 * 
	 * @param req
	 * @return null if no secret can be found.
	 */
	private String getSecret(HttpServletRequest req) {
		LOG.debug("getSecret(HttpServletRequest req)");
		if (req == null) {
			throw new IllegalArgumentException("HttpServletRequest == null");
		}
		String secret = null;
		final Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (COOKIE_NAME.equals(cookie.getName())) {
					secret = cookie.getValue();
				}
			}
		}
		return secret;
	}

	/**
	 * Static final class for storing cached results from Nakamura lookup.
	 * Generally the caller should expect raw results from the JSON parsing
	 * (e.g. principal could in theory be null).
	 */
	public static final class AuthInfo {
		private static final Log AILOG = LogFactory.getLog(AuthInfo.class);

		private String principal;
		private String firstName;
		private String lastName;
		private String emailAddress;

		/**
		 * 
		 * @param json
		 *            The JSON returned from nakamura.
		 */
		private AuthInfo(final String json) {
			if (AILOG.isDebugEnabled()) {
				AILOG.debug("new AuthInfo(String " + json + ")");
			}
			final JSONObject user = JSONObject.fromObject(json).getJSONObject(
					"user");
			final String p = user.getString("principal");
			if (p != null && !"".equals(p) && !ANONYMOUS.equals(p)) {
				principal = p;
			}

			final JSONObject properties = user.getJSONObject("properties");
			firstName = properties.getString("firstName");
			lastName = properties.getString("lastName");
			emailAddress = properties.getString("email");
		}

		/**
		 * @return the givenName
		 */
		public String getFirstName() {
			return firstName;
		}

		/**
		 * @return the familyName
		 */
		public String getLastName() {
			return lastName;
		}

		/**
		 * @return the emailAddress
		 */
		public String getEmailAddress() {
			return emailAddress;
		}

		/**
		 * @return the principal
		 */
		public String getPrincipal() {
			return principal;
		}
	}

}
