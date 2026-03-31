package org.example.courework3.result;


import org.example.courework3.entity.User;


public interface AuthResult {
    String getToken();
    User getUser();
}
