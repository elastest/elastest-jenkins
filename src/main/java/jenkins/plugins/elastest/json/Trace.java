package jenkins.plugins.elastest.json;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Trace implements Serializable {
    private static final long serialVersionUID = 1L;

    private String component;
    private String exec;
    private String stream;
    private String message;
    @JsonProperty("@timestamp")
    private String timestampStr;

    public Trace() {
        super();
    }

    public Trace(String component, String exec, String stream, String message,
            Date timestamp) {
        super();
        this.component = component;
        this.exec = exec;
        this.stream = stream;
        this.message = message;
        this.timestampStr = timestamp != null ? timestamp.toString() : null;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getExec() {
        return exec;
    }

    public void setExec(String exec) {
        this.exec = exec;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestampStr() {
        return timestampStr;
    }

    public void setTimestampStr(String timestampStr) {
        this.timestampStr = timestampStr;
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString;
        try {
            jsonInString = mapper.writeValueAsString(this);
            return jsonInString;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String toString() {
        return "Trace [component=" + component + ", exec=" + exec + ", stream="
                + stream + ", message=" + message 
                + ", timestampStr=" + timestampStr + "]";
    }
}
