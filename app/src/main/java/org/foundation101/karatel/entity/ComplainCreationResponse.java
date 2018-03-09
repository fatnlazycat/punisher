package org.foundation101.karatel.entity;

import java.io.Serializable;

/**
 * Created by Dima on 01.09.2016.
 */
public class ComplainCreationResponse implements Serializable {
    public ComplainRequest data;
    public String status;
    public Object error; //because if it contains nested JSON then Jackson can't deserialize it to ordinary String
}
