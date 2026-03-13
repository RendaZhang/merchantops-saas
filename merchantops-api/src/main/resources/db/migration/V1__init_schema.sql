CREATE TABLE tenant (
                        id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        tenant_code VARCHAR(64) NOT NULL,
                        tenant_name VARCHAR(128) NOT NULL,
                        status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT uk_tenant_code UNIQUE (tenant_code)
);

CREATE TABLE users (
                       id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       tenant_id BIGINT NOT NULL,
                       username VARCHAR(64) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       display_name VARCHAR(128) NOT NULL,
                       email VARCHAR(128),
                       status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       CONSTRAINT uk_users_tenant_username UNIQUE (tenant_id, username),
                       CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE role (
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      tenant_id BIGINT NOT NULL,
                      role_code VARCHAR(64) NOT NULL,
                      role_name VARCHAR(128) NOT NULL,
                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      CONSTRAINT uk_role_tenant_code UNIQUE (tenant_id, role_code),
                      CONSTRAINT fk_role_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id)
);

CREATE TABLE permission (
                            id BIGINT PRIMARY KEY AUTO_INCREMENT,
                            permission_code VARCHAR(64) NOT NULL,
                            permission_name VARCHAR(128) NOT NULL,
                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT uk_permission_code UNIQUE (permission_code)
);

CREATE TABLE user_role (
                           id BIGINT PRIMARY KEY AUTO_INCREMENT,
                           user_id BIGINT NOT NULL,
                           role_id BIGINT NOT NULL,
                           CONSTRAINT uk_user_role UNIQUE (user_id, role_id),
                           CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id),
                           CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE role_permission (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 role_id BIGINT NOT NULL,
                                 permission_id BIGINT NOT NULL,
                                 CONSTRAINT uk_role_permission UNIQUE (role_id, permission_id),
                                 CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role(id),
                                 CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id)
);
