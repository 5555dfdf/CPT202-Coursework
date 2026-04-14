package org.example.coursework3.dto.response;


import org.example.coursework3.entity.User;


public interface AuthResult {
    String getToken();
    User getUser();
}
