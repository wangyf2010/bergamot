package com.intrbiz.bergamot.ui.login;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.intrbiz.Util;
import com.intrbiz.balsa.engine.route.Router;
import com.intrbiz.balsa.engine.security.GenericAuthenticationToken;
import com.intrbiz.balsa.error.BalsaConversionError;
import com.intrbiz.balsa.error.BalsaSecurityException;
import com.intrbiz.balsa.error.BalsaValidationError;
import com.intrbiz.balsa.metadata.WithDataAdapter;
import com.intrbiz.bergamot.data.BergamotDB;
import com.intrbiz.bergamot.model.APIToken;
import com.intrbiz.bergamot.model.Contact;
import com.intrbiz.bergamot.ui.BergamotApp;
import com.intrbiz.metadata.Any;
import com.intrbiz.metadata.AsBoolean;
import com.intrbiz.metadata.Catch;
import com.intrbiz.metadata.CheckStringLength;
import com.intrbiz.metadata.CoalesceMode;
import com.intrbiz.metadata.Get;
import com.intrbiz.metadata.Order;
import com.intrbiz.metadata.Param;
import com.intrbiz.metadata.Post;
import com.intrbiz.metadata.Prefix;
import com.intrbiz.metadata.RequirePrincipal;
import com.intrbiz.metadata.RequireValidAccessTokenForURL;
import com.intrbiz.metadata.RequireValidPrincipal;

@Prefix("/")
public class LoginRouter extends Router<BergamotApp>
{   
    private Logger logger = Logger.getLogger(LoginRouter.class);
    
    @Get("/login")
    public void login(@Param("redirect") String redirect) throws IOException
    {
        // check for an auto auth cookie
        String autoAuthToken = cookie("bergamot.auto.login");
        if (! Util.isEmpty(autoAuthToken))
        {
            // try the given auth token and assert the contact has ui.access permission
            Contact contact = tryAuthenticate(new GenericAuthenticationToken(autoAuthToken));
            if (contact != null && permission("ui.access"))
            {
                logger.info("Successfully auto authenticated user: " + contact.getName() + " => " + contact.getSiteId() + "::" + contact.getId());
                // setup the session
                sessionVar("contact", currentPrincipal());
                sessionVar("site", contact.getSite());
                // record the token in the session for removal on logout
                sessionVar("bergamot.auto.login", autoAuthToken);
                // now we can redirect
                if (contact.isForcePasswordChange())
                {
                    var("redirect", redirect);
                    var("forced", true);
                    encodeOnly("login/force_change_password");
                }
                else
                {
                    // redirect
                    redirect(Util.isEmpty(redirect) ? "/" : path(redirect));
                }
                return;
            }
        }
        // show the login page
        var("redirect", redirect);
        var("username", cookie("bergamot.username"));
        encodeOnly("login/login");
    }

    @Post("/login")
    @RequireValidAccessTokenForURL()
    @WithDataAdapter(BergamotDB.class)
    public void doLogin(BergamotDB db, @Param("username") String username, @Param("password") String password, @Param("redirect") String redirect, @Param("remember_me") @AsBoolean(defaultValue = false, coalesce = CoalesceMode.ALWAYS) Boolean rememberMe) throws IOException
    {
        logger.info("Login: " + username);
        authenticate(username, password);
        // assert that the contact is permitted UI access
        require(permission("ui.access"));
        // store the current site and contact
        Contact contact = sessionVar("contact", currentPrincipal());
        sessionVar("site", contact.getSite());
        // set a cookie of the username, to remember the user
        cookie().name("bergamot.username").value(username).path(path("/login")).expiresAfter(90, TimeUnit.DAYS).httpOnly().set();
        // force a password change
        if (contact.isForcePasswordChange())
        {
            var("redirect", redirect);
            var("forced", true);
            encodeOnly("login/force_change_password");
        }
        else
        {
            // if remember me is selected then push a long term auth cookie
            if (rememberMe)
            {
                // generate the token
                String autoAuthToken = app().getSecurityEngine().generatePerpetualAuthenticationTokenForPrincipal(contact);
                // store the token
                db.setAPIToken(new APIToken(autoAuthToken, contact, "Auto login for " + request().getRemoteAddress()));
                // set the cookie
                cookie()
                .name("bergamot.auto.login")
                .value(autoAuthToken)
                .path(path("/login"))
                .expiresAfter(90, TimeUnit.DAYS)
                .httpOnly()
                .secure(request().isSecure())
                .set();
                // record the token in the session for removal on logout
                sessionVar("bergamot.auto.login", autoAuthToken);
            }
            // redirect
            redirect(Util.isEmpty(redirect) ? "/" : path(redirect));
        }
    }
    
    @Post("/force-change-password")
    @RequirePrincipal()
    @RequireValidAccessTokenForURL()
    public void changePassword(@Param("password") @CheckStringLength(mandatory = true, min = 8) String password, @Param("confirm_password") @CheckStringLength(mandatory = true, min = 8) String confirmPassword, @Param("redirect") String redirect) throws IOException
    {
        if (password.equals(confirmPassword))
        {
            // update the password
            Contact contact = currentPrincipal();
            logger.info("Processing password change for " + contact.getEmail() + " => " + contact.getSiteId() + "::" + contact.getId());
            try (BergamotDB db = BergamotDB.connect())
            {
                contact.hashPassword(password);
                db.setContact(contact);
            }
            // since we have updated the principal, we need to 
            // update it in the session
            balsa().session().currentPrincipal(contact);
            sessionVar("contact", contact);
            logger.info("Password change complete for " + contact.getEmail() + " => " + contact.getSiteId() + "::" + contact.getId());
            // redirect
            redirect(Util.isEmpty(redirect) ? "/" : path(redirect));
        }
        else
        {
            var("redirect", redirect);
            var("forced", true);
            var("error", "mismatch");
            encodeOnly("login/force_change_password");
        }
    }
    
    @Catch(BalsaValidationError.class)
    @Catch(BalsaConversionError.class)
    @Order()
    @Post("/force-change-password")
    @RequirePrincipal()
    @RequireValidAccessTokenForURL()
    public void changePasswordError(@Param("redirect") String redirect) throws IOException
    {
        var("redirect", redirect);
        var("forced", true);
        var("error", "validation");
        encodeOnly("login/force_change_password");
    }

    @Get("/logout")
    @RequireValidPrincipal()
    @WithDataAdapter(BergamotDB.class)
    public void logout(BergamotDB db) throws IOException
    {
        // deauth the current session
        deauthenticate();
        // clean up any auto auth
        String autoAuthToken = sessionVar("bergamot.auto.login");
        if (! Util.isEmpty(autoAuthToken))
        {
            db.removeAPIToken(autoAuthToken);
            // nullify any auto auth cookie
            cookie()
            .name("bergamot.auto.login")
            .value("")
            .path(path("/login"))
            .expiresAfter(90, TimeUnit.DAYS)
            .httpOnly()
            .secure(request().isSecure())
            .set();
        }
        // redirect
        redirect("/login");
    }
    
    @Catch(BalsaSecurityException.class)
    @Order()
    @Post("/login")
    public void loginError(@Param("username") String username, @Param("redirect") String redirect)
    {
        // error during login
        var("error", "invalid");
        var("redirect", redirect);
        var("username", cookie("bergamot.username"));
        // encode login page
        encodeOnly("login/login");
    }
    
    @Catch(BalsaSecurityException.class)
    @Order(Order.LAST)
    @Any("/**")
    public void forceLogin(@Param("redirect") String redirect) throws IOException
    {
        String to = Util.isEmpty(redirect) ? request().getPathInfo() : redirect;
        redirect("/login?redirect=" + Util.urlEncode(to, Util.UTF8));
    }
}
