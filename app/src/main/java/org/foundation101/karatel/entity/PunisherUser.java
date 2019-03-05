package org.foundation101.karatel.entity;

/**
 * Created by Dima on 15.05.2016.
 */
public class PunisherUser {
    public PunisherUser(String email, String password, String surname, String name, String secondName, String phone){
        this.email = email;
        this.password = password;
        this.surname = surname;
        this.name = name;
        this.secondName = secondName;
        this.phone = phone;
    }

    public PunisherUser withId(int id) {
        this.id = id;
        return this;
    }

    public PunisherUser withAvatar(String avatar) {
        this.avatarFileName = avatar;
        return this;
    }

    public String email, password, surname, name, secondName, phone, avatarFileName;
    public Integer id;
}
