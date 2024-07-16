package el.ps.mymapboxgpxapp;

import java.io.InputStream;

public class GlobalData {
    private static GlobalData instance;
    private String globalName;
    private String globalPassword;
    private InputStream xmlObject;

    private GlobalData() {
        // Initialize your global variable here if needed
        globalName = "";
        globalPassword = "";
        xmlObject = null;
    }

    public static synchronized GlobalData getInstance() {
        if (instance == null) {
            instance = new GlobalData();
        }
        return instance;
    }

    public String getGlobalName() {
        return globalName;
    }
    public void setGlobalName(String globalVariable) {
        this.globalName = globalVariable;
    }

    public String getGlobalPassword() {
        return globalPassword;
    }
    public void setGlobalPassword(String globalVariable) {
        this.globalPassword = globalVariable;
    }

    public InputStream getGlobalXMLObject() {
        return xmlObject;
    }
    public void setGlobalXMLObject(InputStream xml) {
        this.xmlObject = xml;
    }
}
