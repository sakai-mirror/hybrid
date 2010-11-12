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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.SignatureException;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link Signature} class.
 */
public class SignatureTest {
	private static final String MOCK_DATA = "data";
	private static final String MOCK_KEY = "key";
	/**
	 * Used to test return vales for {@link #MOCK_DATA} and {@link #MOCK_KEY}
	 */
	private static final String TEST_HMAC = "EEFSxb/coHvGM+69RhmfAlXJ9J0=";
	/**
	 * Used to test return vales for {@link #MOCK_DATA} and {@link #MOCK_KEY}
	 */
	private static final String TEST_HMAC_URLSAFE = "EEFSxb_coHvGM-69RhmfAlXJ9J0";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.Signature#calculateRFC2104HMAC(java.lang.String, java.lang.String)}
	 * .
	 */
	@Test
	public void testCalculateRFC2104HMAC() {
		// good parameters
		try {
			final String hmac = Signature.calculateRFC2104HMAC(MOCK_DATA,
					MOCK_KEY);
			assertNotNull(hmac);
			assertEquals(TEST_HMAC, hmac);
		} catch (SignatureException e) {
			fail("No exception should be thrown: " + e);
		}
		// bad parameters
		try {
			Signature.calculateRFC2104HMAC(null, MOCK_KEY);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalArgumentException should be thrown");
		}
		try {
			Signature.calculateRFC2104HMAC(MOCK_DATA, null);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalArgumentException should be thrown");
		}
	}

	/**
	 * Test method for
	 * {@link org.sakaiproject.util.Signature#calculateRFC2104HMACWithEncoding(java.lang.String, java.lang.String, boolean)}
	 * .
	 */
	@Test
	public void testCalculateRFC2104HMACWithEncoding() {
		// good parameters
		try {
			final String hmac = Signature.calculateRFC2104HMACWithEncoding(
					MOCK_DATA, MOCK_KEY, true);
			assertNotNull(hmac);
			assertEquals(TEST_HMAC_URLSAFE, hmac);
		} catch (SignatureException e) {
			fail("No exception should be thrown: " + e);
		}
		// bad parameters
		try {
			Signature.calculateRFC2104HMACWithEncoding(null, MOCK_KEY, true);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalArgumentException should be thrown");
		}
		try {
			Signature.calculateRFC2104HMACWithEncoding(MOCK_DATA, null, true);
			fail("IllegalArgumentException should be thrown");
		} catch (IllegalArgumentException e) {
			assertNotNull("IllegalArgumentException should be thrown", e);
		} catch (Throwable e) {
			fail("IllegalArgumentException should be thrown");
		}
	}

}
