package org.foundation101.karatel;

import org.foundation101.karatel.entity.Request;

import java.io.Serializable;

/**
 * Created by Dima on 01.09.2016.
 */
public class CreationResponse implements Serializable {
    public Request data;
    public String status, error;
}
