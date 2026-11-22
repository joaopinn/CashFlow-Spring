package com.cashflow.demo.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.Locale;

public final class TextFilters {
    private TextFilters() {}

    public static Predicate containsIgnoreCase(CriteriaBuilder cb, Path<String> field, String value) {
        String term = value.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return cb.like(cb.lower(field), "%" + term + "%", '!');
    }
}
