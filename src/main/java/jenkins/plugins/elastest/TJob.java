package jenkins.plugins.elastest;

public class TJob {
	
	private Long tJobId;
	private Long tJobExecId;
	
	public TJob(){}
	
	public TJob(Long tJobId, Long tJobExecId){
		this.tJobId = tJobId;
		this.tJobExecId = tJobExecId;
	}
	
	public Long gettJobId() {
		return tJobId;
	}
	public void settJobId(Long tJobId) {
		this.tJobId = tJobId;
	}
	public Long gettJobExecId() {
		return tJobExecId;
	}
	public void settJobExecId(Long tJobExecId) {
		this.tJobExecId = tJobExecId;
	}
	
	

}
