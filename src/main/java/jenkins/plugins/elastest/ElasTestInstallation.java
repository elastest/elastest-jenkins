/*
 * The MIT License
 *
 * (C) Copyright 2017-2019 ElasTest (http://elastest.io/)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.elastest;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.plugins.elastest.utils.Utils;
import net.sf.json.JSONObject;

/**
 * Stores global configuration of the ElasTest plugin that is shared between
 * components.
 * 
 * @author Francisco R Díaz
 * @since 0.0.1
 */
public class ElasTestInstallation extends ToolInstallation {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory
            .getLogger(ElasTestInstallation.class);

    @DataBoundConstructor
    public ElasTestInstallation(String name, String home,
            List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    public static Descriptor getLogstashDescriptor() {
        return (Descriptor) Jenkins.getInstance()
                .getDescriptor(ElasTestInstallation.class);
    }

    @Extension
    public static final class Descriptor
            extends ToolDescriptor<ElasTestInstallation> {
        public String elasTestUrl;
        public String username;
        public String password;
        public String elasTestVersion;

        public Descriptor() {
            super();
            load();
            loadElasTestVersion();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData)
                throws FormException {
            if (req != null) {
                req.bindJSON(this, formData.getJSONObject("elastest"));
                save();
            }
            return super.configure(req, formData);
        }

        @Override
        public ToolInstallation newInstance(StaplerRequest req,
                JSONObject formData) throws FormException {
            if (req != null) {
                req.bindJSON(this, formData.getJSONObject("elastest"));
                save();
                return super.newInstance(req, formData);
            } else {
                throw new FormException("Stapler request values null.",
                        "No field.");
            }
        }

        public FormValidation doTestElasTestConnection(
                @QueryParameter("elasTestUrl") final String elasTestUrl,
                @QueryParameter("username") final String username,
                @QueryParameter("password") final String password) {
            this.elasTestUrl = elasTestUrl;
            this.username = username;
            this.password = password;

            if ((!username.equals("") && !password.equals(""))
                    || (username.equals("") && password.equals(""))) {
                loadElasTestVersion();
                try {
                    ElasTestService elasTestService = ElasTestService
                            .getInstance();
                    if (Utils.isCompatibleVersions(elasTestVersion,
                            elasTestService.getElasTestVersion())) {
                        return FormValidation.ok("Success");
                    } else {
                        return FormValidation.error(
                                "Your installed ElasTest version is not compatible"
                                        + " with this plugin version. You need to get "
                                        + "installed ElasTest v."
                                        + elasTestVersion + "or later.");
                    }
                } catch (Exception e) {
                    return FormValidation.error(
                            "Connection error. Check the ElasTest health and the plugin configuration.");
                }
            } else {
                return FormValidation.error(
                        "To use credentials to access ElasTest, it is necessary to complete both fields, username and password.");
            }
        }

        private void loadElasTestVersion() {
            if (elasTestVersion == null || elasTestVersion.equals("")) {
                try {
                    elasTestVersion = PropertiesService
                            .getCompatibleElasTestVersion();
                    LOG.info(
                            "[elastest-plugin]: ElasTest version compatible with this plugin version: "
                                    + elasTestVersion);
                } catch (IOException e) {
                    elasTestVersion = "Error reading the compatible ElasTest version.";
                }
            }
        }

        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        /*
         * Form validation methods
         */
        public FormValidation doCheckInteger(
                @QueryParameter("value") String value) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error(Messages.ValueIsInt());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckHost(
                @QueryParameter("value") String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.warning(Messages.PleaseProvideHost());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckString(
                @QueryParameter("value") String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.ValueIsRequired());
            }

            return FormValidation.ok();
        }

        public String getElasTestUrl() {
            return elasTestUrl;
        }

        public void setElasTestUrl(String elasTestUrl) {
            this.elasTestUrl = elasTestUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getElasTestVersion() {
            return elasTestVersion;
        }

        public void setElasTestVersion(String elasTestVersion) {
            this.elasTestVersion = elasTestVersion;
        }
    }
}
