package com.bsjhx.dayproductivityscore.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private UUID id;
    private String username;
    private String password;
    private String roles;

    @Transient
    private boolean isNew = true;

    public User() {
    }

    public User(UUID id, String username, String password, String roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
