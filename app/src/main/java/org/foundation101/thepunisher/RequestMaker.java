package org.foundation101.thepunisher;

/**
 * Created by Dima on 17.05.2016.
 */
public class RequestMaker{
    final String fixed;

    public RequestMaker(String prefix){
        this.fixed = prefix;
    }

    public String makeRequest(String[] args){
        String result = "";
        int i = 0;
        while (i < args.length){
            if (args[i].contains("@")) args[i] = args[i].replace("@", "%40");
            if (args[i+1].contains("@")) args[i+1] = args[i+1].replace("@", "%40");
            result = result.concat(fixed).concat("["+args[i++]+"]=").concat(args[i++]+"&");
        }
        return result.substring(0, result.length()-1); //remove trailing "&"
    }
}
