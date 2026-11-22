package com.cashflow.demo.integration;

import com.cashflow.demo.entity.*;
import com.cashflow.demo.repository.*;
import com.cashflow.demo.security.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.config.import=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.context.annotation.Import(ApiIntegrationTest.ErrorProbe.class)
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired TransactionRepository transactions;
    @Autowired TransactionTypeRepository types;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired JwtProperties jwtProperties;
    UserModel owner;
    UserModel other;
    UserModel admin;
    TransactionTypeModel expense;
    TransactionTypeModel income;
    CategoryModel food;
    CategoryModel salary;
    CategoryModel otherCategory;
    String bearer;
    static final String PASSWORD = "senha-segura-123";

    @BeforeEach
    void seedDatabase() throws Exception {
        transactions.deleteAll();
        categories.deleteAll();
        types.deleteAll();
        users.deleteAll();
        owner = user("owner@example.com", "USER");
        other = user("other@example.com", "USER");
        admin = user("admin@example.com", "ADMIN");
        expense = type("Despesa");
        income = type("Receita");
        food = category(owner, expense, "Alimentação");
        salary = category(owner, income, "Salário");
        otherCategory = category(other, expense, "Alimentação de outro usuário");
        bearer = "Bearer " + login(owner);
    }

    @Test
    void registerHashesPasswordAndForcesUserRole() throws Exception {
        var result = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registration("NEW@example.com"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist()).andReturn();
        var saved = users.findByEmailIgnoreCase("new@example.com").orElseThrow();
        assertEquals("new@example.com", saved.getEmail());
        assertTrue(passwords.matches(PASSWORD, saved.getPassword()));
        mvc.perform(get(result.getResponse().getHeader("Location")).header("Authorization", "Bearer " + login(saved)))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateEmailIgnoringCaseReturnsConflict() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(registration("OWNER@example.com"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"));
    }

    @Test
    void publicRegistrationCannotGrantAdminRole() throws Exception {
        var body = registration("intruder@example.com");
        body.put("role", "ADMIN");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        assertFalse(users.existsByEmailIgnoreCase("intruder@example.com"));
    }

    @Test
    void beanValidationReportsFieldsWithoutRejectedSecrets() throws Exception {
        var body = registration("invalid-email");
        body.put("password", "short");
        body.put("cnpj", "00000000000000");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/auth/register"))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItems("password", "email", "cnpj")))
                .andExpect(content().string(not(containsString("short"))));
    }

    @Test
    void loginReturnsSignedJwtWithNoStore() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", owner.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900)).andExpect(jsonPath("$.accessToken").isString());
        mvc.perform(get("/users/" + owner.getId()).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void wrongPasswordAndUnknownUserHaveSameUnauthorizedResponse() throws Exception {
        for (String email : List.of(owner.getEmail(), "unknown@example.com")) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Credenciais inválidas"));
        }
    }

    @Test
    void absentAndMalformedTokensReturnStandardUnauthorized() throws Exception {
        mvc.perform(get(base() + "/search")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
        mvc.perform(get(base() + "/search").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void expiredWrongIssuerWrongAudienceMissingExpiryAndWrongSignatureAreRejected() throws Exception {
        Instant now = Instant.now();
        var wrongEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new byte[32]));
        List<String> tokens = List.of(
                signed(encoder, jwtProperties.issuer(), jwtProperties.audience(), now.minusSeconds(300)),
                signed(encoder, "wrong-issuer", jwtProperties.audience(), now.plusSeconds(300)),
                signed(encoder, jwtProperties.issuer(), "wrong-audience", now.plusSeconds(300)),
                signed(encoder, jwtProperties.issuer(), jwtProperties.audience(), null),
                signed(wrongEncoder, jwtProperties.issuer(), jwtProperties.audience(), now.plusSeconds(300)));
        for (String token : tokens) {
            mvc.perform(get(base() + "/search").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
    }

    @Test
    void deletedUserTokenAndRemovedAdminPrivilegesAreNotAccepted() throws Exception {
        String token = login(admin);
        admin.setRole("USER");
        users.saveAndFlush(admin);
        mvc.perform(get("/users").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        users.delete(admin);
        mvc.perform(get("/users").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotReadOrWriteOtherUsersResources() throws Exception {
        String foreignBase = "/users/" + other.getId();
        mvc.perform(get(foreignBase).header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(get(foreignBase + "/transactions/search").header("Authorization", bearer))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc.perform(post(foreignBase + "/categories").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("title", "X", "typeId", expense.getId()))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/users").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(post("/transaction-types").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Tipo\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotPromoteSelf() throws Exception {
        var body = registration(owner.getEmail());
        body.put("role", "ADMIN");
        mvc.perform(put("/users/" + owner.getId()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isForbidden());
        assertEquals("USER", users.findById(owner.getId()).orElseThrow().getRole());
    }

    @Test
    void adminCanManageCatalogAndSearchUsers() throws Exception {
        String auth = "Bearer " + login(admin);
        var created = mvc.perform(post("/transaction-types").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Novo tipo\"}"))
                .andExpect(status().isCreated()).andReturn();
        String location = created.getResponse().getHeader("Location");
        mvc.perform(get(location).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(put(location).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alterado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Alterado"));
        mvc.perform(delete(location).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get("/users/search").header("Authorization", auth).param("email", "OWNER").param("role", "user"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].email").value(owner.getEmail()));
    }

    @Test
    void combinedPeriodTypeAndNameFiltersIncludeBoundariesAndExcludeOtherOwners() throws Exception {
        transaction(owner, food, "Mercado início", "2026-11-01");
        transaction(owner, food, "Mercado fim", "2026-11-30");
        transaction(owner, food, "Mercado fora", "2026-12-01");
        transaction(owner, food, "Farmácia", "2026-11-10");
        transaction(owner, salary, "Mercado receita", "2026-11-10");
        transaction(other, otherCategory, "Mercado de outro", "2026-11-10");
        mvc.perform(get(base() + "/search").header("Authorization", bearer)
                        .param("name", "MERCADO").param("typeId", expense.getId().toString())
                        .param("startDate", "2026-11-01").param("endDate", "2026-11-30").param("sort", "date,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].description").value("Mercado fim"))
                .andExpect(jsonPath("$.content[1].description").value("Mercado início"));
    }

    @Test
    void searchWithoutFiltersAndOpenEndedPeriodRemainScopedAndPaginated() throws Exception {
        transaction(owner, food, "Antiga", "2026-10-01");
        transaction(owner, food, "Nova", "2026-11-30");
        transaction(other, otherCategory, "Outra", "2026-11-10");
        mvc.perform(get(base() + "/search").header("Authorization", bearer).param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(1)));
        mvc.perform(get(base() + "/search").header("Authorization", bearer).param("startDate", "2026-11-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(base() + "/search").header("Authorization", bearer).param("endDate", "2026-10-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].description").value("Antiga"));
    }

    @Test
    void literalWildcardSearchDoesNotMatchAllRecords() throws Exception {
        transaction(owner, food, "100% desconto", "2026-11-01");
        transaction(owner, food, "Comum", "2026-11-02");
        mvc.perform(get(base() + "/search").header("Authorization", bearer).param("name", "%"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void categoryFiltersCombineTypeNameAndOwner() throws Exception {
        mvc.perform(get("/users/" + owner.getId() + "/categories/search").header("Authorization", bearer)
                        .param("name", "ALIMENTAÇÃO").param("typeId", expense.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(food.getId()));
    }

    @Test
    void invalidFiltersDatesPaginationAndSortReturnBadRequest() throws Exception {
        for (Map<String, String> params : List.of(Map.of("startDate", "2026-12-01", "endDate", "2026-11-01"),
                Map.of("typeId", "0"), Map.of("startDate", "not-a-date"), Map.of("typeId", "abc"),
                Map.of("size", "101"), Map.of("size", "0"), Map.of("page", "-1"), Map.of("page", "abc"),
                Map.of("sort", "user.password,desc"))) {
            var request = get(base() + "/search").header("Authorization", bearer);
            params.forEach(request::param);
            mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        }
    }

    @Test
    void invalidBodyAndNegativeIdsReturnStandardValidationErrors() throws Exception {
        mvc.perform(post(base()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[*].field", hasItems("description", "amount", "date")));
        mvc.perform(get(base() + "/-1").header("Authorization", bearer))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isNotEmpty());
        mvc.perform(post(base()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
    }

    @Test
    void transactionCrudPreservesIdOwnerAndCountAndLocationResolves() throws Exception {
        var created = mvc.perform(post(base()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(transactionBody(food, "Original", "2026-11-01"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        String location = created.getResponse().getHeader("Location");
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get(location).header("Authorization", bearer)).andExpect(status().isOk());
        mvc.perform(put(location).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(transactionBody(food, "Atualizada", "2026-11-02"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Atualizada"));
        assertEquals(1, transactions.count());
        assertEquals(owner.getId(), transactions.findById(id).orElseThrow().getUser().getId());
        mvc.perform(delete(location).header("Authorization", bearer)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get(location).header("Authorization", bearer)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void transactionCannotUseForeignCategoryOrMismatchedTypeOrUpdateForeignRecord() throws Exception {
        var body = transactionBody(otherCategory, "Proibida", "2026-11-01");
        mvc.perform(post(base()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isNotFound());
        body = transactionBody(food, "Tipo errado", "2026-11-01");
        body.put("typeId", income.getId());
        mvc.perform(post(base()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        var foreign = transaction(other, otherCategory, "Estrangeira", "2026-11-01");
        mvc.perform(put(base() + "/" + foreign.getId()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(transactionBody(food, "Tentativa", "2026-11-01"))))
                .andExpect(status().isNotFound());
        assertEquals("Estrangeira", transactions.findById(foreign.getId()).orElseThrow().getDescription());
    }

    @Test
    void categoryTypeCannotInvalidateExistingTransactions() throws Exception {
        transaction(owner, food, "Vínculo", "2026-11-01");
        mvc.perform(put("/users/" + owner.getId() + "/categories/" + food.getId()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Alterada", "typeId", income.getId()))))
                .andExpect(status().isConflict());
        assertEquals(expense.getId(), categories.findById(food.getId()).orElseThrow().getType().getId());
    }

    @Test
    void deletingReferencedCategoryReturnsSafeConflict() throws Exception {
        transaction(owner, food, "Vínculo", "2026-11-01");
        mvc.perform(delete("/users/" + owner.getId() + "/categories/" + food.getId()).header("Authorization", bearer))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("constraint ["))));
    }

    @Test
    void swaggerDocumentsSecurityFiltersAndErrorSchemas() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.ApiError").exists())
                .andExpect(jsonPath("$.paths['/auth/login'].post.security").isEmpty())
                .andExpect(jsonPath("$.paths['/auth/register'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/users/{userId}/transactions/search'].get.parameters[*].name",
                        hasItems("typeId", "startDate", "endDate", "page", "size", "sort")))
                .andExpect(jsonPath("$.paths['/users/{userId}/transactions/search'].get.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ApiError"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void passwordsBeyondBcryptByteLimitAreRejectedBeforeEncoding() throws Exception {
        var body = registration("unicode@example.com");
        body.put("password", "é".repeat(40));
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[*].field", hasItem("password")));
    }

    @Test
    void futureNotBeforeAndUnexpectedAlgorithmAreRejected() throws Exception {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(jwtProperties.issuer()).audience(List.of(jwtProperties.audience()))
                .subject(owner.getId().toString()).issuedAt(now).notBefore(now.plusSeconds(600)).expiresAt(now.plusSeconds(900)).build();
        String future = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        var hs512 = new NimbusJwtEncoder(new ImmutableSecret<>(new byte[64]));
        var otherClaims = JwtClaimsSet.builder().issuer(jwtProperties.issuer()).audience(List.of(jwtProperties.audience()))
                .subject(owner.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        String unexpectedAlgorithm = hs512.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS512).build(), otherClaims)).getTokenValue();
        for (String token : List.of(future, unexpectedAlgorithm)) {
            mvc.perform(get(base()).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void categoryCrudAndUserUpdatePreserveIdentityAndProtectSecrets() throws Exception {
        String categoryBase = "/users/" + owner.getId() + "/categories";
        var created = mvc.perform(post(categoryBase).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Nova", "typeId", expense.getId()))))
                .andExpect(status().isCreated()).andReturn();
        String location = created.getResponse().getHeader("Location");
        mvc.perform(put(location).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Editada", "typeId", expense.getId()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Editada"));
        mvc.perform(delete(location).header("Authorization", bearer)).andExpect(status().isNoContent());
        mvc.perform(get(location).header("Authorization", bearer)).andExpect(status().isNotFound());

        var profile = registration(owner.getEmail());
        profile.put("role", "USER");
        profile.put("name", "Nome atualizado");
        mvc.perform(put("/users/" + owner.getId()).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(profile)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(owner.getId()))
                .andExpect(jsonPath("$.name").value("Nome atualizado")).andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void adminCreatesAndDeletesUsersButDuplicateEmailUpdateIsRejected() throws Exception {
        String auth = "Bearer " + login(admin);
        var body = registration("managed@example.com"); body.put("role", "USER");
        var created = mvc.perform(post("/users").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body))).andExpect(status().isCreated()).andReturn();
        String location = created.getResponse().getHeader("Location");
        body.put("email", owner.getEmail().toUpperCase(Locale.ROOT));
        mvc.perform(put(location).header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body))).andExpect(status().isConflict());
        mvc.perform(delete(location).header("Authorization", auth)).andExpect(status().isNoContent());
        mvc.perform(get(location).header("Authorization", auth)).andExpect(status().isNotFound());
    }

    @Test
    void unexpectedFailureHasSafeStandardResponse() throws Exception {
        mvc.perform(get("/test/failure").header("Authorization", bearer))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"))
                .andExpect(content().string(not(containsString("private-diagnostic"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    void unsupportedMethodAndMediaTypeHaveStandardErrors() throws Exception {
        mvc.perform(patch(base()).header("Authorization", bearer)).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405)).andExpect(header().exists("Allow"));
        mvc.perform(post(base()).header("Authorization", bearer).contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }

    @org.springframework.web.bind.annotation.RestController
    @io.swagger.v3.oas.annotations.Hidden
    static class ErrorProbe {
        @org.springframework.web.bind.annotation.GetMapping("/test/failure")
        String fail() { throw new IllegalStateException("private-diagnostic"); }
    }

    private UserModel user(String email, String role) {
        var user = new UserModel();
        user.setName(email); user.setCnpj("11222333000181"); user.setEmail(email);
        user.setSegment("Varejo"); user.setPhone("71999999999"); user.setRole(role);
        user.setPassword(passwords.encode(PASSWORD));
        return users.saveAndFlush(user);
    }
    private TransactionTypeModel type(String title) {
        var type = new TransactionTypeModel(); type.setTitle(title); return types.saveAndFlush(type);
    }
    private CategoryModel category(UserModel user, TransactionTypeModel type, String title) {
        var category = new CategoryModel(); category.setUser(user); category.setType(type); category.setTitle(title);
        return categories.saveAndFlush(category);
    }
    private TransactionModel transaction(UserModel user, CategoryModel category, String description, String date) {
        var transaction = new TransactionModel(); transaction.setUser(user); transaction.setCategory(category);
        transaction.setType(category.getType()); transaction.setDescription(description); transaction.setAmount(new BigDecimal("20.50"));
        transaction.setDate(LocalDate.parse(date)); transaction.setStatus("PAGO");
        return transactions.saveAndFlush(transaction);
    }
    private String login(UserModel user) throws Exception {
        var result = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", user.getEmail(), "password", PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("accessToken").asString();
    }
    private Map<String, Object> registration(String email) {
        return new HashMap<>(Map.of("name", "Maria", "cnpj", "11222333000181", "segment", "Varejo",
                "email", email, "password", PASSWORD, "phone", "71999999999"));
    }
    private Map<String, Object> transactionBody(CategoryModel category, String description, String date) {
        return new HashMap<>(Map.of("description", description, "amount", "25.50", "typeId", category.getType().getId(),
                "categoryId", category.getId(), "date", date, "status", "PAGO"));
    }
    private String signed(JwtEncoder signer, String issuer, String audience, Instant expiry) {
        var claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(owner.getId().toString())
                .issuedAt(Instant.now().minusSeconds(600)).notBefore(Instant.now().minusSeconds(600));
        if (expiry != null) claims.expiresAt(expiry);
        return signer.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
    }
    private String base() { return "/users/" + owner.getId() + "/transactions"; }
}
