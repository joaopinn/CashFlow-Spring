package com.cashflow.demo.config;

import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import java.util.*;

@Configuration
public class PaginationConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!(handler instanceof HandlerMethod method) || Arrays.stream(method.getMethodParameters())
                        .noneMatch(parameter -> parameter.getParameterType() == Pageable.class)) return true;
                checkNumber(request, "page", 0, Integer.MAX_VALUE);
                checkNumber(request, "size", 1, 100);
                Set<String> fields = switch (method.getBeanType().getSimpleName()) {
                    case "UserController" -> Set.of("id", "name", "email", "segment", "role");
                    case "CategoryController" -> Set.of("id", "title");
                    case "TransactionTypeController" -> Set.of("id", "title");
                    default -> Set.of("id", "date", "amount", "description", "status");
                };
                String[] sorts = request.getParameterValues("sort");
                if (sorts != null) for (String sort : sorts) {
                    String[] tokens = sort.split(",", -1);
                    int length = tokens.length;
                    if (length > 1 && Set.of("asc", "desc").contains(tokens[length - 1].toLowerCase(Locale.ROOT))) length--;
                    for (int i = 0; i < length; i++) if (!fields.contains(tokens[i])) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo de ordenação inválido");
                    }
                }
                return true;
            }
        });
    }

    private static void checkNumber(HttpServletRequest request, String name, int minimum, int maximum) {
        String raw = request.getParameter(name);
        if (raw == null) return;
        try {
            long value = Long.parseLong(raw);
            if (value >= minimum && value <= maximum) return;
        } catch (NumberFormatException ignored) { }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                name + " deve estar entre " + minimum + " e " + maximum);
    }
}
