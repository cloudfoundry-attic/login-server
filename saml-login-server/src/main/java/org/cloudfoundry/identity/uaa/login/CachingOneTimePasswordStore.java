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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 
 * @author jdsa
 * 
 */
public class CachingOneTimePasswordStore implements OneTimePasswordStore, InitializingBean {

	private SecureRandom rand = null;

	private CacheManager cacheManager = null;

	private Cache cache = null;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public CachingOneTimePasswordStore() {
		try {
			rand = SecureRandom.getInstance("SHA1PRNG");
		}
		catch (NoSuchAlgorithmException e) {
			// Ignore
		}
	}

	@Override
	public String getOneTimePassword(String userId) {
		String oneTimePassword = String.valueOf(rand.nextInt(1 << 30));
		cache.put(new Element(userId, generatePassword(userId, oneTimePassword)));
		return oneTimePassword;
	}

	@Override
	public boolean validateOneTimePassword(String userId, String oneTimePassword) {
		Element element = cache.get(userId);
		if (element != null && element.getObjectValue() != null
				&& passwordEncoder.matches(userId + oneTimePassword, (String) element.getObjectValue())) {
			cache.remove(userId);
		}
		else {
			return false;
		}

		return true;
	}

	private String generatePassword(String userId, String oneTimePassword) {
		return passwordEncoder.encode(userId + oneTimePassword);
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		cache = cacheManager.getCache("oneTimePasswordCache");
	}
}
