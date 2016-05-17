package com.example.dnk.punisher;

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

    public String email, password, surname, name, secondName, phone;
}
