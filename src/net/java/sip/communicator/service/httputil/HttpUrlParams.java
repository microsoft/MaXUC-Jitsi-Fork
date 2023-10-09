package net.java.sip.communicator.service.httputil;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.message.BasicNameValuePair;

import net.java.sip.communicator.util.Logger;

/**
 * Despite being in an httputil package, this class is specific to processing of the URL parameter
 * names/values we expect on requests for SIP-PS config, and is only used for this.
 */
public class HttpUrlParams
{
    private static final Logger logger = Logger.getLogger(HttpUrlParams.class);

    private List<String> paramNames = null;
    private List<String> paramValues = null;
    private int usernameIx = -1;
    private int passwordIx = -1;

    public static HttpUrlParams getHttpUrlParamsFromArgs(String[] args)
    {
        HttpUrlParams params = new HttpUrlParams();

        if (args != null && args.length > 0)
        {
            params.paramNames = new ArrayList<>(args.length);
            params.paramValues = new ArrayList<>(args.length);

            for(int i = 0; i < args.length; i++)
            {
                String s = args[i];

                String usernameParam = "${username}";
                String dnParam = "${directorynumber}";
                String passwordParam = "${password}";

                // If we find the username or password parameter at this
                // stage we replace it with an empty string.
                if(s.contains(usernameParam))
                {
                    s = s.replace(usernameParam, "");
                    params.usernameIx = params.paramNames.size();
                }
                else if(s.contains(dnParam))
                {
                    s = s.replace(dnParam, "");
                    params.usernameIx = params.paramNames.size();
                }
                else if(s.contains(passwordParam))
                {
                    s = s.replace(passwordParam, "");
                    params.passwordIx = params.paramNames.size();
                }

                int equalsIndex = s.indexOf("=");
                if (equalsIndex > -1)
                {
                    params.paramNames.add(s.substring(0, equalsIndex));
                    params.paramValues.add(s.substring(equalsIndex + 1));
                }
                else
                {
                    logger.info("Invalid URL parameter: \"" + s + "\", is replaced by \"" + s + "=\"");
                    params.paramNames.add(s);
                    params.paramValues.add("");
                }
            }
        }

        return params;
    }

    /**
     * @param credentials User credentials, either Username+Password or SSO.
     * @return Return a list of NameValuePairs for the params to use on an HTTP request.
     * When username is not null, we use that value in for the paramName at index usernameIx.
     * When password is not null, we use that value in for the paramName at index passwordIx.
     */
    public List<NameValuePair> getParams(Credentials credentials)
    {
        List<NameValuePair> parameters = new ArrayList<>();
        String username = null;
        String password = null;
        if (credentials != null)
        {
            password = credentials.getPassword();
            if (!(credentials instanceof SSOCredentials))
            {
                username = credentials.getUserPrincipal().getName();
            }
        }

        if (paramNames != null)
        {
            for (int paramIdx = 0; paramIdx < paramNames.size(); paramIdx++)
            {
                if (paramIdx == usernameIx && credentials instanceof SSOCredentials)
                {
                    logger.info("Logging in via SSO, avoid passing 'Username' param");
                }
                else if (paramIdx == usernameIx && username != null)
                {   // we are on the username index, insert retrieved username value
                    parameters.add(new BasicNameValuePair(paramNames.get(paramIdx), username));
                }
                else if (paramIdx == passwordIx && password != null)
                { // we are on the password index, insert retrieved password val
                    if (credentials instanceof SSOCredentials)
                    {
                        logger.info("Logging in via SSO, replacing 'Password' param with 'AccessToken'");
                        parameters.add(new BasicNameValuePair("AccessToken", password));
                    }
                    else
                    {
                        parameters.add(new BasicNameValuePair(paramNames.get(paramIdx), password));
                    }
                }
                else // common name value pair, all info is present
                {
                    parameters.add(new BasicNameValuePair(paramNames.get(paramIdx), paramValues.get(paramIdx)));
                }
            }
        }

        return parameters;
    }

    /**
     * @return true iff we have a 'username' and a 'password' parameter defined in paramNames (by
     * virtue of the usernameIx and passwordIx values).
     */
    public boolean haveUsernameAndPassword()
    {
        return usernameIx != -1 &&
               usernameIx < paramNames.size() &&
               passwordIx != -1 &&
               passwordIx < paramNames.size();
    }
}
