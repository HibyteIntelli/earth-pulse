package com.earthpulse.www;

import com.earthpulse.www.security.RateLimitFilterTest;
import com.earthpulse.www.service.JwtServiceTest;
import com.earthpulse.www.service.UserServiceTest;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
        JwtServiceTest.class,
        UserServiceTest.class,
        RateLimitFilterTest.class,
        AuthFlowIT.class

})
class AuthAndSubscriptionApplicationTests {
}
