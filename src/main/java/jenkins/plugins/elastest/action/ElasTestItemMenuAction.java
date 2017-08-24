/*
 * The MIT License
 *
 * Copyright 2017 ElasTest
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

import java.util.logging.Logger;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.Actionable;
import hudson.model.Run;
import jenkins.plugins.elastest.Messages;

/**
 * 
 * @author Francisco R. Díaz
 *
 */
public class ElasTestItemMenuAction extends Actionable implements Action {
	
	private final static Logger LOG = Logger.getLogger(ElasTestItemMenuAction.class.getName());
	private static final String ICON_IMAGE ="/plugin/elastest/images/icon.png";
	
	private String elasTestLogAnalyzerUrl = "http://localhost:4200/#/logmanager";
			
	public ElasTestItemMenuAction(@SuppressWarnings("rawtypes") Run<?,?> build, String elasTestLogAnalyzerUrl) {
		super();
		this.elasTestLogAnalyzerUrl = elasTestLogAnalyzerUrl;
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
		return elasTestLogAnalyzerUrl;
	}

	public String getElasTestLogAnalyzerUrl() {
		return elasTestLogAnalyzerUrl;
	}

	public void setElasTestLogAnalyzerUrl(String elasTestLogAnalyzerUrl) {
		this.elasTestLogAnalyzerUrl = elasTestLogAnalyzerUrl;
	}

}
