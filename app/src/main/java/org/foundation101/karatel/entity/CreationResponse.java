package org.foundation101.karatel.entity;

import java.io.Serializable;

/**
 * Created by Dima on 01.09.2016.
 */
public class CreationResponse implements Serializable {
    public CreationResponse() {} //required to deserialize with Jackson

    public CreationResponse(Request data, String status, String error) {
        this.data = data;
        this.status = status;
        this.error = error;
    }

    public Request data;
    public String status, error;
}
