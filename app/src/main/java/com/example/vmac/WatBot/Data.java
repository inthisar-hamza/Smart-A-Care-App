package com.example.vmac.WatBot;


public class Data {
    private String email;
    private String password;
    private String date;

    Data(String email, String password,  String date ){
        this.email = email;
        this.password = password;
        this.date = date;

    }

    Data(String email, String password){
        this.email = email;
        this.password = password;
        this.date = null;

    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
