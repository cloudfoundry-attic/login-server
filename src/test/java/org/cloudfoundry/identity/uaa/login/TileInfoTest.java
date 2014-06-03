package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.identity.uaa.login.test.DefaultTestConfig;
import org.cloudfoundry.identity.uaa.login.test.DefaultTestConfigContextLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = DefaultTestConfig.class, loader = DefaultTestConfigContextLoader.class)
public class TileInfoTest {

    @Autowired
    private TileInfo tileInfo;

    @Test
    public void testLoginTiles() throws Exception {
        ArrayList<LinkedHashMap<String,String>> loginTiles = tileInfo.getLoginTiles();

        assertEquals(3, loginTiles.size());
        for (LinkedHashMap<String,String> loginTile : loginTiles) {
            assertFalse(StringUtils.isEmpty(loginTile.get("login-link")));
        }
    }

    @Test
    public void testSignupTiles() throws Exception {
        ArrayList<LinkedHashMap<String,String>> signupTiles = tileInfo.getSignupTiles();

        assertEquals(2, signupTiles.size());
        for (LinkedHashMap<String,String> loginTile : signupTiles) {
            assertFalse(StringUtils.isEmpty(loginTile.get("signup-link")));
        }
    }
}