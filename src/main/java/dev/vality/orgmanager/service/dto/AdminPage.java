package dev.vality.orgmanager.service.dto;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Страница выдачи, собранная приёмом «выбрать limit + 1»: лишняя строка означает, что за
 * страницей есть продолжение, а последняя оставленная строка даёт continuation token.
 * Механика одинакова для всех постраничных методов административного контракта, поэтому
 * живёт здесь, а не в каждом сервисе.
 */
public record AdminPage<T>(List<T> items, Optional<String> continuationToken) {

    public static <T> AdminPage<T> of(List<T> fetched, int limit, Function<T, String> tokenOf) {
        if (fetched.size() <= limit) {
            return new AdminPage<>(List.copyOf(fetched), Optional.empty());
        }
        List<T> items = List.copyOf(fetched.subList(0, limit));
        return new AdminPage<>(items, Optional.of(tokenOf.apply(items.get(items.size() - 1))));
    }
}
