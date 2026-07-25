package com.earthpulse.www;

import com.earthpulse.www.security.JwtAuthenticationFilter;
import com.earthpulse.www.security.RateLimitFilterTest;
import com.earthpulse.www.service.JwtServiceTest;
import com.earthpulse.www.service.UserServiceAccountTest;
import com.earthpulse.www.service.UserServiceTest;
import com.earthpulse.www.service.WatchService;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        JwtServiceTest.class,
        JwtAuthenticationFilter.class,
        RateLimitFilterTest.class,
        //------------
        UserServiceTest.class,
        UserServiceAccountTest.class,
        //WatchService.class,
        UserServiceAccountTest.class,
        //------------
        AuthFlowIT.class,
        //AccountAndWatchIT.class
})
class AuthAndSubscriptionApplicationTests {
}
