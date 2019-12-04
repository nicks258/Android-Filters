package com.pntstudio.buzz.filterapp;

import android.arch.lifecycle.ViewModel;

import java.io.Serializable;

public class GetterClass extends ViewModel implements Serializable {
    String name,email,phone;
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }



}
