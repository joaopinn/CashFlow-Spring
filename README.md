# CashFlow

## Banco de dados

Execute os comandos a partir da pasta `demo` (também configure essa pasta como working directory na IDE).

A configuração local fica em `config/database-local.properties`, ignorado pelo Git.
Para configurar outra máquina, copie `config/database-local.properties.example`
para `config/database-local.properties` e preencha as credenciais.

Em ambientes de implantação, configure `DATABASE_URL` (URL JDBC),
`DATABASE_USERNAME` e `DATABASE_PASSWORD` como variáveis de ambiente.
O driver JDBC usa `channelBinding=require`, com B maiúsculo.

```sh
./mvnw spring-boot:run
```

O Hibernate apenas valida o esquema, sem criar ou alterar tabelas.
O Flyway está desabilitado até a criação da primeira migration; depois,
habilite com `FLYWAY_ENABLED=true`.
