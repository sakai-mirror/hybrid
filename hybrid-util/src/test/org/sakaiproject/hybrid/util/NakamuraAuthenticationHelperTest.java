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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.sakaiproject.hybrid.test.TestHelper.disableLog4jDebug;
import static org.sakaiproject.hybrid.test.TestHelper.enableLog4jDebug;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.hybrid.util.NakamuraAuthenticationHelper.AuthInfo;
import org.sakaiproject.hybrid.util.NakamuraAuthenticationHelper.DefaultHttpClientProvider;
import org.sakaiproject.hybrid.util.NakamuraAuthenticationHelper.HttpClientProvider;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.tool.api.SessionManager;

@RunWith(MockitoJUnitRunner.class)
public class NakamuraAuthenticationHelperTest {
	private static final String MOCK_SHARED_SECRET = "e2KS54H35j6vS5Z38nK40";
	private static final String MOCK_HOSTNAME = "localhost";
	private static final String MOCK_VALIDATE_URL = "http://localhost/var/cluster/user.cookie.json?c=";
	private static final String MOCK_PRINCIPAL = "admin";
	/**
	 * @see XSakaiToken#getSharedSecret(String)
	 */
	private static final String MOCK_SAKAI_PROP_KEY = XSakaiToken.CONFIG_PREFIX
			+ "." + MOCK_HOSTNAME + "."
			+ XSakaiToken.CONFIG_SHARED_SECRET_SUFFIX;
	private static final String MOCK_JSON = "{"
			+ "\"server\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"user\": {" + "\"lastUpdate\": 1289584757709,"
			+ "\"homeServer\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"id\": \"admin\"," + "\"principal\": \"admin\","
			+ "\"properties\": {" + "\"sakai:search-exclude-tree\": \"true\","
			+ "\"firstName\": \"Admin\"," + "\"testproperty\": \"newvalue2\","
			+ "\"email\": \"admin@sakai.invalid\","
			+ "\"path\": \"/a/ad/admin\"," + "\"lastName\": \"User\"" + "},"
			+ "\"declaredMembership\": [" + "]," + "\"membership\": [" + "]"
			+ "}" + "}";
	/**
	 * HYB-70 net.sf.json.JSONException: JSONObject["firstName"] not found
	 */
	private static final String MOCK_JSON_NO_NAMES_NO_EMAIL = "{"
			+ "\"server\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"user\": {" + "\"lastUpdate\": 1289584757709,"
			+ "\"homeServer\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"id\": \"admin\"," + "\"principal\": \"admin\","
			+ "\"properties\": {" + "\"sakai:search-exclude-tree\": \"true\","
			+ "\"testproperty\": \"newvalue2\"," + "\"path\": \"/a/ad/admin\","
			+ "}," + "\"declaredMembership\": [" + "]," + "\"membership\": ["
			+ "]" + "}" + "}";
	private static final String MOCK_JSON_ANONYMOUS = "{"
			+ "\"server\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"user\": {" + "\"lastUpdate\": 1289584757709,"
			+ "\"homeServer\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"id\": \"anonymous\"," + "\"principal\": \"anonymous\","
			+ "\"properties\": {" + "\"sakai:search-exclude-tree\": \"true\","
			+ "\"testproperty\": \"newvalue2\","
			+ "\"path\": \"/a/an/anonymous\"," + "},"
			+ "\"declaredMembership\": [" + "]," + "\"membership\": [" + "]"
			+ "}" + "}";
	private static final String MOCK_JSON_EMPTY_PRINCIPAL = "{"
			+ "\"server\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"user\": {" + "\"lastUpdate\": 1289584757709,"
			+ "\"homeServer\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"id\": \"anonymous\"," + "\"principal\": \"\","
			+ "\"properties\": {" + "\"sakai:search-exclude-tree\": \"true\","
			+ "\"testproperty\": \"newvalue2\","
			+ "\"path\": \"/a/an/anonymous\"," + "},"
			+ "\"declaredMembership\": [" + "]," + "\"membership\": [" + "]"
			+ "}" + "}";
	/**
	 * HYB-70 net.sf.json.JSONException: JSONObject["firstName"] not found
	 */
	private static final String MOCK_JSON_NO_PRINCIPAL = "{"
			+ "\"server\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"user\": {" + "\"lastUpdate\": 1289584757709,"
			+ "\"homeServer\": \"10968-sakai3-nightly.foo.bar.edu\","
			+ "\"id\": \"admin\"," + "\"properties\": {"
			+ "\"sakai:search-exclude-tree\": \"true\","
			+ "\"testproperty\": \"newvalue2\"," + "\"path\": \"/a/ad/admin\","
			+ "}," + "\"declaredMembership\": [" + "]," + "\"membership\": ["
			+ "]" + "}" + "}";
	NakamuraAuthenticationHelper nakamuraAuthenticationHelper = null;
	@Mock
	ComponentManager componentManager;
	@Mock
	HttpServletRequest request;
	@Mock
	ServerConfigurationService serverConfigurationService;
	@Mock
	Cookie sakaiTrackingCookie;
	@Mock
	Cookie otherCookie;
	@Mock
	ThreadLocalManager threadLocalManager;
	@Mock
	SessionManager sessionManager;
	@Mock
	HttpClient httpClient;
	@Mock
	ClientConnectionManager clientConnectionManager;

	@BeforeClass
	public static void setupClass() {
		enableLog4jDebug();
	}

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		when(componentManager.get(ThreadLocalManager.class)).thenReturn(
				threadLocalManager);
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(serverConfigurationService);
		when(componentManager.get(SessionManager.class)).thenReturn(
				sessionManager);
		when(
				serverConfigurationService.getString(
						NakamuraAuthenticationHelper.CONFIG_ANONYMOUS,
						"anonymous")).thenReturn("anonymous");
		when(
				serverConfigurationService.getString(
						NakamuraAuthenticationHelper.CONFIG_COOKIE_NAME,
						"SAKAI-TRACKING")).thenReturn("SAKAI-TRACKING");
		// XSakaiToken
		when(serverConfigurationService.getString(MOCK_SAKAI_PROP_KEY, null))
				.thenReturn(MOCK_SHARED_SECRET);
		when(sakaiTrackingCookie.getName()).thenReturn("SAKAI-TRACKING");
		when(sakaiTrackingCookie.getValue()).thenReturn("theSecret");
		when(otherCookie.getName()).thenReturn("FOO");
		final Cookie[] cookies = { sakaiTrackingCookie, otherCookie };
		when(request.getCookies()).thenReturn(cookies);
		nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
				componentManager, MOCK_VALIDATE_URL, MOCK_PRINCIPAL,
				MOCK_HOSTNAME);
		nakamuraAuthenticationHelper.httpClientProvider = new MockHttpClientProvider();
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenReturn(MOCK_JSON);
		when(httpClient.getConnectionManager()).thenReturn(
				clientConnectionManager);
	}

	/**
	 * @see NakamuraAuthenticationHelper#NakamuraAuthenticationHelper(ThreadLocalManager,
	 *      ServerConfigurationService, String, String, String)
	 */
	@Test
	public void testConstructor() {
		try { // good parameters
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, MOCK_PRINCIPAL,
					MOCK_HOSTNAME);
		} catch (Throwable e) {
			fail("No exception should be thrown");
		}
		// bad parameters
		// /////////////////////////////////////////////////////////////////////////////////////
		try { // null componentManager
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					null, MOCK_VALIDATE_URL, MOCK_PRINCIPAL, MOCK_HOSTNAME);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}

		// /////////////////////////////////////////////////////////////////////////////////////
		try { // null validateUrl
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, null, MOCK_PRINCIPAL, MOCK_HOSTNAME);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}
		try { // empty validateUrl
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, "", MOCK_PRINCIPAL, MOCK_HOSTNAME);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}

		// /////////////////////////////////////////////////////////////////////////////////////
		try { // null principal
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, null, MOCK_HOSTNAME);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}
		try { // empty principal
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, "", MOCK_HOSTNAME);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}

		// /////////////////////////////////////////////////////////////////////////////////////
		try { // null hostname
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, MOCK_PRINCIPAL, null);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}
		try { // empty hostname
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, MOCK_PRINCIPAL, "");
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#NakamuraAuthenticationHelper(ComponentManager,
	 *      String, String, String)
	 */
	@Test
	public void testNullThreadLocalManager() {
		when(componentManager.get(ThreadLocalManager.class)).thenReturn(null);
		try { // null ThreadLocalManager
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, MOCK_PRINCIPAL,
					MOCK_HOSTNAME);
			fail("Should not be reached; IllegalStateException exception should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull("IllegalStateException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalStateException should be thrown");
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#NakamuraAuthenticationHelper(ComponentManager,
	 *      String, String, String)
	 */
	@Test
	public void testNullServerConfigurationService() {
		when(componentManager.get(ServerConfigurationService.class))
				.thenReturn(null);
		try { // null ServerConfigurationService
			nakamuraAuthenticationHelper = new NakamuraAuthenticationHelper(
					componentManager, MOCK_VALIDATE_URL, MOCK_PRINCIPAL,
					MOCK_HOSTNAME);
			fail("Should not be reached; IllegalStateException exception should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull("IllegalStateException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalStateException should be thrown");
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamura() throws Exception {
		// try bad parameters first
		try { // null request
			nakamuraAuthenticationHelper.getPrincipalLoggedIntoNakamura(null);
			fail("Should not be reached; IllegalArgumentException exception should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		}
		// /////////////////////////////////////////////////////////////////////////////////////
		try { // good request
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNotNull(authInfo);
			assertEquals(MOCK_PRINCIPAL, authInfo.getPrincipal());
			assertNotNull(authInfo.getFirstName());
			assertEquals("Admin", authInfo.getFirstName());
			assertNotNull(authInfo.getLastName());
			assertEquals("User", authInfo.getLastName());
			assertNotNull(authInfo.getEmailAddress());
			assertEquals("admin@sakai.invalid", authInfo.getEmailAddress());
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraLogDebugDisabled()
			throws Exception {
		disableLog4jDebug();
		try {
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNotNull(authInfo);
			assertEquals(MOCK_PRINCIPAL, authInfo.getPrincipal());
			assertNotNull(authInfo.getFirstName());
			assertEquals("Admin", authInfo.getFirstName());
			assertNotNull(authInfo.getLastName());
			assertEquals("User", authInfo.getLastName());
			assertNotNull(authInfo.getEmailAddress());
			assertEquals("admin@sakai.invalid", authInfo.getEmailAddress());
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
		enableLog4jDebug();
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraNoSecretFound()
			throws Exception {
		when(request.getCookies()).thenReturn(new Cookie[] { otherCookie });
		AuthInfo authInfo = null;
		try {
			authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
		assertNull(authInfo);
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraHyb70() throws Exception {
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenReturn(
				MOCK_JSON_NO_NAMES_NO_EMAIL);
		AuthInfo authInfo = null;
		try {
			authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
		assertNotNull(authInfo);
		assertEquals(MOCK_PRINCIPAL, authInfo.getPrincipal());
		assertEquals("", authInfo.getFirstName());
		assertEquals("", authInfo.getLastName());
		assertEquals("", authInfo.getEmailAddress());
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraHyb70A() throws Exception {
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenReturn(
				MOCK_JSON_NO_PRINCIPAL);
		try {
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNotNull(authInfo);
			assertNull(authInfo.getPrincipal());
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraAnonymous() throws Exception {
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenReturn(
				MOCK_JSON_ANONYMOUS);
		try {
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNotNull(authInfo);
			assertNull(authInfo.getPrincipal());
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraEmptyPrincipal()
			throws Exception {
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenReturn(
				MOCK_JSON_EMPTY_PRINCIPAL);
		try {
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNotNull(authInfo);
			assertNull(authInfo.getPrincipal());
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
	}

	/**
	 * With a good cache hit.
	 * 
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testGetPrincipalLoggedIntoNakamuraCacheHit() throws Exception {
		final AuthInfo mockAuthInfo = new AuthInfo(MOCK_JSON);
		when(
				threadLocalManager
						.get(NakamuraAuthenticationHelper.THREAD_LOCAL_CACHE_KEY))
				.thenReturn(mockAuthInfo);
		try {
			final AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNotNull(authInfo);
			assertEquals(MOCK_PRINCIPAL, authInfo.getPrincipal());
			assertTrue(mockAuthInfo == authInfo); // should be same object
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getSecret(HttpServletRequest)
	 */
	@Test
	public void testGetSecretNullRequest() {
		try {
			nakamuraAuthenticationHelper.getSecret(null);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalArgumentException should be thrown");
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getSecret(HttpServletRequest)
	 */
	@Test
	public void testGetSecretNullCookies() {
		when(request.getCookies()).thenReturn(null);
		try {
			final String secret = nakamuraAuthenticationHelper
					.getSecret(request);
			assertNull(secret);
		} catch (Throwable e) {
			fail("Throwable should not be thrown");
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testHttpResponseException() throws Exception {
		// HttpResponseException
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenThrow(
				new HttpResponseException(404,
						"could not find cookie / not valid"));
		try {
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNull(authInfo);
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testHttpResponseExceptionLogDebugDisabled() throws Exception {
		disableLog4jDebug();
		// HttpResponseException
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenThrow(
				new HttpResponseException(404,
						"could not find cookie / not valid"));
		try {
			AuthInfo authInfo = nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			assertNull(authInfo);
		} catch (Throwable e) {
			fail("Throwable should not be thrown: " + e);
		}
		enableLog4jDebug();
	}

	/**
	 * @see NakamuraAuthenticationHelper#getPrincipalLoggedIntoNakamura(HttpServletRequest)
	 */
	@Test
	public void testRuntimeException() throws Exception {
		// Throwable / RuntimeException
		when(
				httpClient.execute(any(HttpUriRequest.class),
						any(BasicResponseHandler.class))).thenThrow(
				new IllegalStateException());
		try {
			nakamuraAuthenticationHelper
					.getPrincipalLoggedIntoNakamura(request);
			fail("IllegalStateException should be thrown");
		} catch (IllegalStateException e) {
			assertNotNull("IllegalStateException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalStateException should be thrown");
		}
	}

	/**
	 * @see DefaultHttpClientProvider#DefaultHttpClientProvider()
	 */
	@Test
	public void testDefaultHttpClientProvider() {
		final DefaultHttpClientProvider defaultHttpClientProvider = new DefaultHttpClientProvider();
		assertNotNull(defaultHttpClientProvider);
		final HttpClient httpClient = defaultHttpClientProvider.getHttpClient();
		assertNotNull(httpClient);
	}

	/**
	 * Implementation is thread safe.
	 */
	private class MockHttpClientProvider implements HttpClientProvider {
		/**
		 * @see org.sakaiproject.hybrid.util.api.HttpClientProvider#getHttpGet()
		 */
		@Override
		public HttpClient getHttpClient() {
			return httpClient;
		}
	}
}
