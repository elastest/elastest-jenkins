package jenkins.plugins.elastest.utils;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.DatatypeConverter;

public class Authenticator implements ClientRequestFilter, Serializable {
    private static final long serialVersionUID = 1L;
    private String user;
    private String password;
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUsingSecurity() {
        return usingSecurity;
    }

    public void setUsingSecurity(boolean usingSecurity) {
        this.usingSecurity = usingSecurity;
    }

    private boolean usingSecurity;
    
    public Authenticator () {}

    public Authenticator(String user, String password) {
        this.setCredentials(user, password);
    }

    public void setCredentials(String newUser, String newPassword) {
        user = newUser;
        password = newPassword;
        usingSecurity = user != null && password != null ? true : false;
    }

    @Override
    public void filter(ClientRequestContext requestContext)
            throws IOException {
        MultivaluedMap<String, Object> headers = requestContext
                .getHeaders();
        headers.add("ContentType", MediaType.APPLICATION_JSON);
        if (usingSecurity) {
            final String basicAuthentication = getBasicAuthentication();
            headers.add("Authorization", basicAuthentication);
        }
    }

    private String getBasicAuthentication() {
        String token = user + ":" + password;
        try {
            return "BASIC " + DatatypeConverter
                    .printBase64Binary(token.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Cannot encode with UTF-8", ex);
        }
    }
}