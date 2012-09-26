/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.cloudfoundry.identity.uaa.login;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.social.SocialClientUserDetails;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;

/**
 * @author Dave Syer
 * 
 */
public class DefaultAutologinCodeStore implements AutologinCodeStore {

	private static final Log logger = LogFactory.getLog(DefaultAutologinCodeStore.class);

	private static final long DEFAULT_VALIDITY_PERIOD = 5 * 60 * 1000; // 5 minute

	private Map<String, TokenExpiry> codes = new ConcurrentHashMap<String, TokenExpiry>();

	private final DelayQueue<TokenExpiry> expiryQueue = new DelayQueue<TokenExpiry>();

	private RandomValueStringGenerator generator = new RandomValueStringGenerator(6);

	private long validityPeriod = DEFAULT_VALIDITY_PERIOD;

	private long flushInterval = DEFAULT_VALIDITY_PERIOD * 2;

	private AtomicLong flushTimestamp = new AtomicLong(System.currentTimeMillis());

	private boolean expireCodeWhenUsed = true;

	/**
	 * Flag to determine if the authentication codes can be re-used until they expire, or (if true) if they can only be
	 * used once.
	 * 
	 * @param expireCodeWhenUsed the flag to set, default true
	 */
	public void setExpireCodeWhenUsed(boolean expireCodeWhenUsed) {
		this.expireCodeWhenUsed = expireCodeWhenUsed;
	}

	@Override
	public SocialClientUserDetails getUser(String code) {
		flush();
		TokenExpiry value = codes.remove(code);
		if (value == null) {
			logger.info("Could not redeem code: " + code);
			return null;
		}
		if (value.getDelay(TimeUnit.MILLISECONDS) <= 0) {
			logger.info("Code expired: " + code);
			expiryQueue.remove(value);
			return null;
		}
		if (expireCodeWhenUsed) {
			expiryQueue.remove(value);
		}
		else {
			codes.put(code, value);
		}
		logger.info("Redeemed code: " + code);
		return value.getUser();
	}

	@Override
	public String storeUser(SocialClientUserDetails user) {
		flush();
		String code = generator.generate();
		TokenExpiry expiry = new TokenExpiry(code, user, new Date(System.currentTimeMillis() + validityPeriod));
		this.expiryQueue.put(expiry);
		codes.put(code, expiry);
		logger.info("Stored user [" + user.getUsername() + "] with code: " + code);
		return code;
	}

	private void flush() {
		if (this.flushTimestamp.get() >= System.currentTimeMillis() - this.flushInterval) {
			this.flushTimestamp.set(System.currentTimeMillis());
			TokenExpiry expiry = expiryQueue.poll();
			while (expiry != null) {
				codes.remove(expiry.getValue());
				expiry = expiryQueue.poll();
			}
		}
	}

	private static class TokenExpiry implements Delayed {

		private final long expiry;

		private final String value;

		private final SocialClientUserDetails user;

		public TokenExpiry(String value, SocialClientUserDetails user, Date date) {
			this.value = value;
			this.user = user;
			this.expiry = date.getTime();
		}

		public int compareTo(Delayed other) {
			if (this == other) {
				return 0;
			}
			long diff = getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS);
			return (diff == 0 ? 0 : ((diff < 0) ? -1 : 1));
		}

		public long getDelay(TimeUnit unit) {
			return expiry - System.currentTimeMillis();
		}

		public String getValue() {
			return value;
		}

		public SocialClientUserDetails getUser() {
			return user;
		}

	}
}
