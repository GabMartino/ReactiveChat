package reactivechat.ejb;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

public  class ConfigReader {

    Properties prop = new Properties();
    String propFileName;
    InputStream inputStream;
    public ConfigReader(String filename){
        propFileName = filename;
        try {
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            prop.load(inputStream);

        }catch (Exception e){

        }

    }

    public String getProperty(String key){
        return prop.getProperty(key);
    }

    public Set<String> getAllKeys(){
        return prop.stringPropertyNames();
    }
    public ArrayList<String> getAllProperties(){
        ArrayList<String> props = new ArrayList<>();
        for(String key : prop.stringPropertyNames()){
                props.add(prop.getProperty(key));
        }
        return props;

    }

}
