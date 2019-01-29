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
package jenkins.plugins.elastest.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import jenkins.plugins.elastest.ElasTestService;
import jenkins.plugins.elastest.Messages;

/**
 * This class creates the icon that allows access to the Job execution in
 * ElasTest
 * 
 * @author Francisco R. Díaz
 *
 */
public class ElasTestItemMenuAction extends Actionable implements Action {

    private final static Logger LOG = LoggerFactory
            .getLogger(ElasTestItemMenuAction.class);
    private static final String ICON_IMAGE = "/plugin/elastest/images/icon.png";

    private String elasTestLogAnalyzerUrl = "http://localhost:4200/#/logmanager";
    private String elasTestTJobExecutionUrl = "";

    public ElasTestItemMenuAction(@SuppressWarnings("rawtypes") Run<?, ?> build,
            String elasTestLogAnalyzerUrl, String elasTestTJobExecutionUrl) {
        super();
        LOG.debug("[elastest-plugin]: ElasTest Log Analayser URL: {}",
                elasTestLogAnalyzerUrl);
        LOG.debug("[elastest-plugin]: ElasTest TJob execution URL: {}",
                elasTestTJobExecutionUrl);
        this.elasTestLogAnalyzerUrl = elasTestLogAnalyzerUrl;
        this.elasTestTJobExecutionUrl = elasTestTJobExecutionUrl;
    }

    public static void addActionToMenu(Run<?, ?> build) {
        ElasTestService elasTestService = ElasTestService.getInstance();
        ElasTestItemMenuAction action = new ElasTestItemMenuAction(build,
                elasTestService.getExternalJobByBuildFullName(
                        build.getFullDisplayName()).getLogAnalyzerUrl(),
                elasTestService.getExternalJobByBuildFullName(
                        build.getFullDisplayName()).getExecutionUrl());
        build.addAction(action);
    }

    @Override
    public String getDisplayName() {
        return Messages.MenuItemLabel();
    }

    @Override
    public String getSearchUrl() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return ICON_IMAGE;
    }

    @Override
    public String getUrlName() {
        return elasTestTJobExecutionUrl;
    }

    public String getElasTestLogAnalyzerUrl() {
        return elasTestLogAnalyzerUrl;
    }

    public void setElasTestLogAnalyzerUrl(String elasTestLogAnalyzerUrl) {
        this.elasTestLogAnalyzerUrl = elasTestLogAnalyzerUrl;
    }

    public String getElasTestTJobExecutionUrl() {
        return elasTestTJobExecutionUrl;
    }

    public void setElasTestTJobExecutionUrl(String elasTestTJobExecutionUrl) {
        this.elasTestTJobExecutionUrl = elasTestTJobExecutionUrl;
    }

}
