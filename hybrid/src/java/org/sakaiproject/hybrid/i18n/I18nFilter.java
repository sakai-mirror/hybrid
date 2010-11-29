/*
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
package org.sakaiproject.hybrid.i18n;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Filter to transform i18n message keys into i18n messages.
 */
// @Component(metatype = true)
// @Service
// @Properties(value = {
// @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
// @Property(name = Constants.SERVICE_DESCRIPTION, value =
// "Nakamura i18n Filter"),
// @Property(name = Constants.SERVICE_RANKING, intValue = 10, propertyPrivate =
// true),
// @Property(name = "sling.filter.scope", value = "REQUEST", propertyPrivate =
// true),
// @Property(name = I18nFilter.BUNDLES_PATH, value =
// I18nFilter.DEFAULT_BUNDLES_PATH),
// @Property(name = I18nFilter.MESSAGE_KEY_PATTERN, value =
// I18nFilter.DEFAULT_MESSAGE_KEY_PATTERN),
// @Property(name = I18nFilter.SHOW_MISSING_KEYS, boolValue =
// I18nFilter.DEFAULT_SHOW_MISSING_KEYS)
// })
public class I18nFilter implements Filter {
	public static final String PARAM_LANGUAGE = "l";
	public static final boolean DEFAULT_SHOW_MISSING_KEYS = true;

	private static final Log logger = LogFactory.getLog(I18nFilter.class);

	static final String MESSAGE_KEY_PATTERN = "sakai.filter.i18n.message_key.pattern";
	static final String SHOW_MISSING_KEYS = "sakai.filter.i18n.message_key.show_missing";

	private String keyPattern;
	private Pattern messageKeyPattern;
	private boolean showMissingKeys;

	// ---------- Filter interface ----------
	/**
	 * {@inheritDoc}
	 *
	 * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
	 */
	public void init(FilterConfig filterConfig) throws ServletException {
		keyPattern = filterConfig.getInitParameter(MESSAGE_KEY_PATTERN);
		messageKeyPattern = Pattern.compile(keyPattern);

		showMissingKeys = Boolean.parseBoolean(filterConfig
				.getInitParameter(SHOW_MISSING_KEYS));
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see javax.servlet.Filter#destroy()
	 */
	public void destroy() {
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
	 *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		if ("true".equals(request.getParameter("raw"))) {
			chain.doFilter(request, response);
			return;
		}

		// get path info
		HttpServletRequest srequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		String path = srequest.getPathInfo();

		// check that the path is something we should filter.
		boolean filter = false;
		if ((path.startsWith("/dev/") || path.startsWith("/devwidgets/"))
				&& path.endsWith(".html")) {
			httpResponse = new CapturingHttpServletResponse(httpResponse);
			filter = true;
		}

		// allow the chain to process so we can capture the response
		chain.doFilter(request, httpResponse);

		// if the path was set to be filtered, get the output and filter it
		// otherwise the response isn't wrapped and doesn't require us to
		// intervene
		if (filter) {
			String output = httpResponse.toString();
			if (!StringUtils.isBlank(output)) {
				long start = System.currentTimeMillis();

				writeFilteredResponse(srequest, response, output);

				long end = System.currentTimeMillis();
				logger.debug("Filtered " + path + " in " + (end - start) + "ms");
			}
		}
	}

	/**
	 * Filter <code>output</code> of any message keys by replacing them with the
	 * matching message from the language bundle associated to the user.
	 *
	 * @param srequest
	 * @param response
	 * @param output
	 * @throws IOException
	 */
	private void writeFilteredResponse(HttpServletRequest srequest,
			ServletResponse response, String output) throws IOException {
		StringBuilder sb = new StringBuilder(output);
		// load the language bundle
		Locale locale = getLocale(srequest);
		ResourceBundle bundle = getBundle(locale);

		// check for message keys and replace them with the appropriate message
		Matcher m = messageKeyPattern.matcher(output);
		ArrayList<String> matchedKeys = new ArrayList<String>();
		while (m.find()) {
			String msgKey = m.group(0);
			String key = m.group(1);
			if (!matchedKeys.contains(key)) {
				String message = "";

				if (bundle.containsKey(key)) {
					message = bundle.getString(key);
				} else {
					String msg = "[MESSAGE KEY NOT FOUND '" + key + "']";
					logger.warn(msg);
					if (showMissingKeys) {
						message = msg;
					}
				}

				// replace all instances of msgKey with the actual message
				int keyStart = sb.indexOf(msgKey);
				while (keyStart >= 0) {
					sb.replace(keyStart, keyStart + msgKey.length(),
							message);
					keyStart = sb.indexOf(msgKey, keyStart);
				}

				// track the group so we don't try to replace it again
				matchedKeys.add(key);
			}
		}

		response.setContentLength(sb.length());

		// send the output to the actual response
		try {
			response.getWriter().write(sb.toString());
		} catch (IllegalStateException e) {
			response.getOutputStream().write(sb.toString().getBytes("UTF-8"));
		}
	}

	private Locale getLocale(HttpServletRequest request) {
		Locale l = null;
		String lang = request.getParameter(PARAM_LANGUAGE);

		if (lang != null) {
			String[] parts = lang.split("_");
			l = new Locale(parts[0], parts[1]);
		} else {
			l = request.getLocale();
		}

		return l;
	}

	private ResourceBundle getBundle(Locale locale) {
		ResourceBundle rb = ResourceBundle.getBundle("Messages", locale);
		return rb;
	}
}
