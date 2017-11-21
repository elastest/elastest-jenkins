package jenkins.plugins.elastest.pipeline;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.EnvVars;
import hudson.Extension;
import jenkins.YesNoMaybe;
import jenkins.plugins.elastest.Messages;

/**
 * Pipeline plug-in step for sending log traces to ElasTest Platform.
 *  
 * @author Francisco R. Díaz
 */
public class ElasTestStep extends AbstractStepImpl {

    private static final Logger logger = LoggerFactory
            .getLogger(ElasTestStep.class);

    @StepContextParameter
    public EnvVars envVars;

    @Nonnull
    private String sut = "";

    @Nonnull
    private List<String> tss = new ArrayList<String>();

    @Nonnull
    private Long tJobId = -1L;

    /**
     * Constructor.
     */
    @DataBoundConstructor
    public ElasTestStep() {
    }

    public List<String> getTss() {
        return tss;
    }

    @DataBoundSetter
    public void setTss(List<String> tss) {
        this.tss = tss;
    }

    public String getSut() {
        return sut;
    }

    public void setSut(String sut) {
        this.sut = sut;
    }

    public Long getTJobId() {
        return tJobId;
    }

    @DataBoundSetter
    public void setTJobId(Long tJobId) {
        this.tJobId = tJobId;
    }

    /**
     * Descriptor for {@link ElasTestStep}.
     */
    @Extension(dynamicLoadable = YesNoMaybe.YES, optional = true)
    public static class DescriptorImpl extends AbstractStepDescriptorImpl {

        /**
         * Constructor.
         */
        public DescriptorImpl() {
            super(ExecutionImpl.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.DisplayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getFunctionName() {
            return Messages.ElasTestPipelineStep();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getHelpFile() {
            return getDescriptorFullUrl() + "/help";
        }

        /**
         * Serve the help file.
         * 
         * @param request
         * @param response
         * @throws IOException
         */
        public void doHelp(StaplerRequest request, StaplerResponse response)
                throws IOException {
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter writer = response.getWriter();
            writer.println(Messages.DisplayName());
            writer.flush();
        }
    }
}
