package org.foundation101.karatel.entity;

import java.io.Serializable;

/**
 * Created by Dima on 17.06.2016.
 */
public class UpdateEntity implements Serializable{
    public int id, complain_status_id, complain_id;
    public UpdateEntity.DocUrl[] documents;
    public String reply, name_of_authority, created_at, updated_at;

    public boolean collapsed = true;
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }
    public boolean isCollapsed() {
        return collapsed;
    }

    public boolean rated = true;
    public void setRated(boolean rated) {
        this.rated = rated;
    }
    public boolean isRated() {
        return rated;
    }

    public static class DocUrl implements Serializable {
        public String url;
    }
}
