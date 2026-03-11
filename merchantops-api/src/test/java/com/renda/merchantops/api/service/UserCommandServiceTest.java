package com.renda.merchantops.api.service;

import com.renda.merchantops.api.dto.user.command.UserCreateCommand;
import com.renda.merchantops.api.dto.user.command.UserCreateResponse;
import com.renda.merchantops.api.dto.user.command.UserPasswordUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentCommand;
import com.renda.merchantops.api.dto.user.command.UserRoleAssignmentResponse;
import com.renda.merchantops.api.dto.user.command.UserStatusUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserUpdateCommand;
import com.renda.merchantops.api.dto.user.command.UserWriteResponse;
import com.renda.merchantops.common.exception.BizException;
import com.renda.merchantops.common.exception.ErrorCode;
import com.renda.merchantops.infra.persistence.entity.PermissionEntity;
import com.renda.merchantops.infra.persistence.entity.RoleEntity;
import com.renda.merchantops.infra.persistence.entity.UserEntity;
import com.renda.merchantops.infra.repository.PermissionRepository;
import com.renda.merchantops.infra.repository.RoleRepository;
import com.renda.merchantops.infra.repository.UserRepository;
import com.renda.merchantops.infra.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserCommandService userCommandService;

    @Test
    void createUserShouldThrowBadRequestWhenUsernameAlreadyExists() {
        UserCreateCommand command = new UserCreateCommand(
                "admin",
                "123456",
                "Admin",
                "admin@demo-shop.local",
                List.of("TENANT_ADMIN")
        );
        when(userRepository.existsByTenantIdAndUsername(1L, "admin")).thenReturn(true);

        assertBizException(
                () -> userCommandService.createUser(1L, 101L, command),
                ErrorCode.BAD_REQUEST,
                "username already exists in tenant"
        );

        verify(userRepository).existsByTenantIdAndUsername(1L, "admin");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createUserShouldRejectRoleCodesOutsideCurrentTenant() {
        UserCreateCommand command = new UserCreateCommand(
                "new-user",
                "123456",
                "New User",
                "new-user@demo-shop.local",
                List.of("TENANT_ADMIN", "OTHER_ONLY")
        );
        RoleEntity role = roleEntity(11L, 1L, "TENANT_ADMIN");
        when(userRepository.existsByTenantIdAndUsername(1L, "new-user")).thenReturn(false);
        when(roleRepository.findAllByTenantIdAndRoleCodeInOrderByIdAsc(1L, List.of("TENANT_ADMIN", "OTHER_ONLY")))
                .thenReturn(List.of(role));

        assertBizException(
                () -> userCommandService.createUser(1L, 101L, command),
                ErrorCode.BAD_REQUEST,
                "roleCodes must exist in current tenant"
        );
    }

    @Test
    void createUserShouldPersistActiveUserWithBcryptPasswordAndRoleBindings() {
        UserCreateCommand command = new UserCreateCommand(
                "cashier",
                "123456",
                "Cashier User",
                "cashier@demo-shop.local",
                List.of("READ_ONLY", "OPS_USER", "READ_ONLY")
        );
        RoleEntity readOnly = roleEntity(13L, 1L, "READ_ONLY");
        RoleEntity opsUser = roleEntity(12L, 1L, "OPS_USER");
        when(userRepository.existsByTenantIdAndUsername(1L, "cashier")).thenReturn(false);
        when(roleRepository.findAllByTenantIdAndRoleCodeInOrderByIdAsc(1L, List.of("READ_ONLY", "OPS_USER")))
                .thenReturn(List.of(opsUser, readOnly));
        when(passwordEncoder.encode("123456")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(205L);
            return user;
        });

        UserCreateResponse response = userCommandService.createUser(1L, 101L, command);

        assertThat(response.getId()).isEqualTo(205L);
        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("cashier");
        assertThat(response.getDisplayName()).isEqualTo("Cashier User");
        assertThat(response.getEmail()).isEqualTo("cashier@demo-shop.local");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getRoleCodes()).containsExactly("READ_ONLY", "OPS_USER");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(userCaptor.getValue().getUsername()).isEqualTo("cashier");
        assertThat(userCaptor.getValue().getDisplayName()).isEqualTo("Cashier User");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("cashier@demo-shop.local");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(userCaptor.getValue().getCreatedBy()).isEqualTo(101L);
        assertThat(userCaptor.getValue().getUpdatedBy()).isEqualTo(101L);

        verify(passwordEncoder).encode("123456");
        verify(userRoleRepository).saveAll(argThat(userRoles -> {
            assertThat(StreamSupport.stream(userRoles.spliterator(), false).toList())
                    .extracting("userId", "roleId")
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(205L, 12L),
                            org.assertj.core.groups.Tuple.tuple(205L, 13L)
                    );
            return true;
        }));
    }

    @Test
    void createUserShouldRejectPasswordWithLeadingOrTrailingWhitespace() {
        UserCreateCommand command = new UserCreateCommand(
                "cashier",
                " 123456 ",
                "Cashier User",
                "cashier@demo-shop.local",
                List.of("READ_ONLY")
        );

        assertBizException(
                () -> userCommandService.createUser(1L, 101L, command),
                ErrorCode.BAD_REQUEST,
                "password must not start or end with whitespace"
        );
    }

    @Test
    void validateUsernameUniqueWithExcludeUserIdShouldRejectDuplicate() {
        when(userRepository.existsByTenantIdAndUsernameAndIdNot(1L, "admin", 3L)).thenReturn(true);

        assertBizException(
                () -> userCommandService.validateUsernameUnique(1L, "admin", 3L),
                ErrorCode.BAD_REQUEST,
                "username already exists in tenant"
        );
    }

    @Test
    void updateUserShouldThrowNotFoundWhenTenantUserMissing() {
        when(userRepository.findByIdAndTenantId(8L, 1L)).thenReturn(Optional.empty());

        assertBizException(
                () -> userCommandService.updateUser(1L, 101L, 8L, new UserUpdateCommand("Ops", "ops@demo.local")),
                ErrorCode.NOT_FOUND,
                "user not found"
        );
    }

    @Test
    void updateUserShouldPersistMutableProfileFieldsOnly() {
        UserEntity user = userEntity(1L, 1L, "cashier", "ACTIVE");
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserWriteResponse response = userCommandService.updateUser(
                1L,
                101L,
                1L,
                new UserUpdateCommand("Updated Cashier", "updated@demo.local")
        );

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("cashier");
        assertThat(response.getDisplayName()).isEqualTo("Updated Cashier");
        assertThat(response.getEmail()).isEqualTo("updated@demo.local");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getUpdatedAt()).isNotNull();

        verify(userRepository).save(any(UserEntity.class));
        assertThat(user.getUsername()).isEqualTo("cashier");
        assertThat(user.getTenantId()).isEqualTo(1L);
        assertThat(user.getDisplayName()).isEqualTo("Updated Cashier");
        assertThat(user.getEmail()).isEqualTo("updated@demo.local");
        assertThat(user.getUpdatedBy()).isEqualTo(101L);
    }

    @Test
    void updateStatusShouldRejectInvalidStatus() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(userEntity(1L, 1L, "cashier", "ACTIVE")));

        assertBizException(
                () -> userCommandService.updateStatus(1L, 101L, 1L, new UserStatusUpdateCommand("ARCHIVED")),
                ErrorCode.BAD_REQUEST,
                "status must be one of ACTIVE, DISABLED"
        );
    }

    @Test
    void updateStatusShouldPersistDisabledStatus() {
        UserEntity user = userEntity(1L, 1L, "cashier", "ACTIVE");
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserWriteResponse response = userCommandService.updateStatus(
                1L,
                101L,
                1L,
                new UserStatusUpdateCommand("DISABLED")
        );

        assertThat(response.getStatus()).isEqualTo("DISABLED");
        assertThat(user.getStatus()).isEqualTo("DISABLED");
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(user.getUpdatedBy()).isEqualTo(101L);
    }

    @Test
    void assignRolesShouldRejectRoleCodesOutsideCurrentTenant() {
        UserEntity user = userEntity(1L, 1L, "viewer", "ACTIVE");
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
        when(roleRepository.findAllByTenantIdAndRoleCodeInOrderByIdAsc(1L, List.of("TENANT_ADMIN", "OTHER_ONLY")))
                .thenReturn(List.of(roleEntity(11L, 1L, "TENANT_ADMIN")));

        assertBizException(
                () -> userCommandService.assignRoles(1L, 101L, 1L, new UserRoleAssignmentCommand(List.of("TENANT_ADMIN", "OTHER_ONLY"))),
                ErrorCode.BAD_REQUEST,
                "roleCodes must exist in current tenant"
        );
    }

    @Test
    void assignRolesShouldReplaceExistingRolesAndReturnCurrentPermissions() {
        UserEntity user = userEntity(1L, 1L, "viewer", "ACTIVE");
        RoleEntity tenantAdmin = roleEntity(11L, 1L, "TENANT_ADMIN");
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(user));
        when(roleRepository.findAllByTenantIdAndRoleCodeInOrderByIdAsc(1L, List.of("TENANT_ADMIN"))).thenReturn(List.of(tenantAdmin));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findPermissionsByUserIdAndTenantId(1L, 1L))
                .thenReturn(List.of(
                        permissionEntity(1L, "USER_READ"),
                        permissionEntity(2L, "USER_WRITE"),
                        permissionEntity(3L, "FEATURE_FLAG_MANAGE"),
                        permissionEntity(4L, "TICKET_READ"),
                        permissionEntity(5L, "TICKET_WRITE")
                ));

        UserRoleAssignmentResponse response = userCommandService.assignRoles(
                1L,
                101L,
                1L,
                new UserRoleAssignmentCommand(List.of("TENANT_ADMIN"))
        );

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTenantId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("viewer");
        assertThat(response.getRoleCodes()).containsExactly("TENANT_ADMIN");
        assertThat(response.getPermissionCodes()).containsExactly("USER_READ", "USER_WRITE", "FEATURE_FLAG_MANAGE", "TICKET_READ", "TICKET_WRITE");
        assertThat(response.getUpdatedAt()).isNotNull();

        inOrder(userRoleRepository, userRepository).verify(userRoleRepository).deleteByUserId(1L);
        verify(userRoleRepository).saveAll(argThat(userRoles -> {
            assertThat(StreamSupport.stream(userRoles.spliterator(), false).toList())
                    .extracting("userId", "roleId")
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(1L, 11L));
            return true;
        }));
        verify(userRepository).save(user);
        assertThat(user.getUpdatedBy()).isEqualTo(101L);
    }

    @Test
    void updatePasswordShouldThrowBizErrorWhenFeatureIsNotReady() {
        when(userRepository.findByIdAndTenantId(1L, 1L)).thenReturn(Optional.of(new UserEntity()));

        assertBizException(
                () -> userCommandService.updatePassword(1L, 1L, new UserPasswordUpdateCommand("new-hash")),
                ErrorCode.BIZ_ERROR,
                "update password flow is not implemented yet"
        );
    }

    private void assertBizException(ThrowingCall call, ErrorCode errorCode, String message) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException bizException = (BizException) ex;
                    assertThat(bizException.getErrorCode()).isEqualTo(errorCode);
                    assertThat(bizException.getMessage()).isEqualTo(message);
                });
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run();
    }

    private RoleEntity roleEntity(Long id, Long tenantId, String roleCode) {
        RoleEntity role = new RoleEntity();
        role.setId(id);
        role.setTenantId(tenantId);
        role.setRoleCode(roleCode);
        role.setRoleName(roleCode);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }

    private PermissionEntity permissionEntity(Long id, String permissionCode) {
        PermissionEntity permission = new PermissionEntity();
        permission.setId(id);
        permission.setPermissionCode(permissionCode);
        permission.setPermissionName(permissionCode);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        return permission;
    }

    private UserEntity userEntity(Long id, Long tenantId, String username, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setPasswordHash("bcrypt-hash");
        user.setDisplayName("Cashier");
        user.setEmail("cashier@demo.local");
        user.setStatus(status);
        user.setCreatedAt(LocalDateTime.now().minusDays(1));
        user.setUpdatedAt(LocalDateTime.now().minusHours(1));
        user.setCreatedBy(88L);
        user.setUpdatedBy(89L);
        return user;
    }
}
