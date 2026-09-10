package dev.vality.orgmanager.service;

import dev.vality.orgmanagement.ListUsersRequest;
import dev.vality.orgmanagement.ListUsersResult;
import dev.vality.orgmanagement.UnknownUser;
import dev.vality.orgmanagement.User;
import dev.vality.orgmanager.converter.AdminManagementConverter;
import dev.vality.orgmanager.repository.MemberRepository;
import dev.vality.orgmanager.service.dto.AdminPage;
import dev.vality.orgmanager.service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static dev.vality.orgmanager.service.AdminCommonService.pageLimit;
import static java.util.Objects.requireNonNullElseGet;

/** Пользователи в административном контракте: страница пользователей и чтение по идентификатору. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final MemberRepository memberRepository;
    private final AdminManagementConverter converter;

    @Transactional(readOnly = true)
    public ListUsersResult list(ListUsersRequest request) {
        log.info("List users: request={}", request);
        ListUsersRequest safeRequest = requireNonNullElseGet(request, ListUsersRequest::new);
        int limit = pageLimit(safeRequest.getLimit());
        AdminPage<UserDto> page = AdminPage.of(
                memberRepository.getUserPage(
                        safeRequest.getContinuationToken(),
                        safeRequest.getEmail(),
                        PageRequest.ofSize(limit + 1)),
                limit,
                UserDto::getId);
        ListUsersResult result = new ListUsersResult(page.items().stream()
                .map(converter::toUser)
                .toList());
        page.continuationToken().ifPresent(result::setContinuationToken);
        return result;
    }

    @Transactional(readOnly = true)
    public User get(String userId) throws UnknownUser {
        log.info("Get user: userId={}", userId);
        if (userId == null || userId.isBlank()) {
            throw new UnknownUser();
        }
        return memberRepository.findById(userId)
                .map(converter::toUser)
                .orElseThrow(UnknownUser::new);
    }
}
