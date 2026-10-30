# Plano de Integração de Login com Google

Documento vivo para acompanhar a implementação da autenticação com Google, com fases, critérios de aceite e registro de execução.

- **Status geral:** Concluído — Android, backend, credenciais OAuth e validação end-to-end aprovados.
- **Última atualização:** 2026-10-30.
- **Próximo passo:** nenhum — login com Google validado em dispositivo com conta real.
- **Responsável:** equipe de desenvolvimento Android.

## Como atualizar este documento

1. Atualizar o campo **Status** do passo antes de iniciar e ao concluir.
2. Preencher a coluna **Conclusão** com evidências objetivas: arquivos alterados, decisões, validações executadas e pendências.
3. Atualizar a tabela **Visão geral** sempre que uma fase mudar de status.
4. Adicionar uma linha no **Registro de execução** ao final de cada etapa relevante.
5. Nunca marcar um passo como concluído sem executar a validação correspondente.
6. Manter o **Próximo passo** apontando para o primeiro item pendente.

## Legenda de status

| Status | Significado |
| :--- | :--- |
| Pendente | Ainda não iniciado. |
| Em andamento | Iniciado, sem conclusão validada. |
| Concluído | Implementado e validado. |
| Bloqueado | Dependência externa ou decisão pendente. |
| Cancelado | Não será executado neste plano. |

## Objetivo

Permitir que o usuário entre no App Financeiro com uma conta Google, preservando o modelo de autenticação atual: o Google comprova a identidade e a API do aplicativo emite o JWT usado nas demais requisições.

## Visão geral

| Fase | Objetivo | Status | Progresso |
| :--- | :--- | :--- | :--- |
| Fase 0 | Baseline e diagnóstico do fluxo atual | Concluído | 100% |
| Fase 1 | Contrato e regras do backend | Concluído | 100% |
| Fase 2 | Configuração OAuth e credenciais | Concluído | 100% |
| Fase 3 | Aquisição do ID token no Android | Concluído | 100% |
| Fase 4 | Integração API, repositório e ViewModel | Concluído | 100% |
| Fase 5 | Interface, navegação e sessão | Concluído | 100% |
| Fase 6 | Testes, segurança e documentação | Concluído | 100% |

## Decisões consolidadas

| ID | Decisão | Justificativa |
| :--- | :--- | :--- |
| D-01 | Manter o JWT do backend como sessão da aplicação | Todo o app já autentica via `JwtInterceptor` e `TokenManager`; trocar o modelo geraria refatoração ampla sem ganho. |
| D-02 | Usar o ID token do Google apenas como prova de identidade | O ID token é destinado ao backend; não deve ser armazenado como token de sessão nem reutilizado nas chamadas à API. |
| D-03 | Não adotar Firebase Authentication neste plano | O Firebase já é usado somente para FCM; a autenticação é responsabilidade do backend. |
| D-04 | Validar o token no servidor | Assinatura, emissor, audiência e validade devem ser checadas no backend; dados enviados pelo cliente não são confiáveis. |
| D-05 | Reutilizar `TokenManager` e o fluxo de navegação existentes | Reduz risco de regressão em logout, expiração de sessão e sincronização pós-login. |
| D-06 | Usar AndroidX Credential Manager | API recomendada atualmente para login com Google, substituindo `GoogleSignInClient`. |
| D-07 | Não versionar segredos | Apenas o client ID público (`serverClientId`) pode ir ao app; nenhum client secret deve ser incluído no APK ou no repositório. |
| D-08 | Fixar `credentials` 1.5.0 e `googleid` 1.1.1 | Versões 1.6.0/1.2.1 usam metadata Kotlin 2.x/2.4 que o kapt com Kotlin 2.0.21 não lê; as versões escolhidas compilam no projeto e expõem `GetSignInWithGoogleOption`. |
| D-09 | Client ID via `BuildConfig.GOOGLE_WEB_CLIENT_ID` | Lido de `gradle.properties`/`local.properties`, permitindo builds por ambiente sem hardcode e sem segredos no repositório. |
| D-10 | Falhar com mensagem clara quando não configurado | Sem client ID, o app informa que o login Google não está configurado em vez de lançar exceção. |

## Fase 0 — Baseline e diagnóstico

Objetivo: registrar o estado real do código antes de qualquer alteração.

| ID | Passo | Status | Conclusão |
| :--- | :--- | :--- | :--- |
| F0-01 | Confirmar estado do repositório | Concluído | Árvore limpa no commit `7be30db`; commits da implementação anterior já integrados. |
| F0-02 | Mapear o fluxo de autenticação atual | Concluído | Login e cadastro por e-mail/senha via `AuthRepository`, `LoginViewModel` e `LoginActivity`; JWT salvo por `TokenManager`. |
| F0-03 | Verificar suporte a Google no app | Concluído | Botão existe em `activity_login.xml` (`googleAuthButton`), mas era apenas um toast provisório. |
| F0-04 | Verificar endpoints de autenticação | Concluído | `ApiService.java` declarava somente `auth/login`, `auth/register` e `auth/forgot-password`. |
| F0-05 | Verificar dependências disponíveis | Concluído | `libs.versions.toml` não possuía Credential Manager nem `googleid`; Firebase limitado a BOM + messaging. |
| F0-06 | Definir fases e critérios de validação | Concluído | Plano documentado neste arquivo. |

**Conclusões da Fase 0**

- A integração depende de trabalho no backend: sem endpoint de troca do ID token por JWT, o app não pode concluir o fluxo.
- O app já possui toda a infraestrutura de sessão (JWT, expiração, logout, sync pós-login) que deve ser reutilizada.
- O plugin `com.google.gms.google-services` é aplicado condicionalmente quando existe `google-services.json`; a configuração OAuth não deve tornar essa dependência obrigatória para builds locais.
- Não havia implementação parcial de Google a ser removida: o botão era apenas um placeholder.

## Fase 1 — Contrato e regras do backend

Objetivo: fechar o contrato de autenticação Google antes de concluir o fluxo no app.

| ID | Passo | Arquivos/Artefatos | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F1-01 | Confirmar existência de endpoint Google | Backend | Rota e ambiente de homologação identificados. | Concluído | Endpoint criado em `server/api/auth.ts` e publicado; responde 401 JSON para token inválido. |
| F1-02 | Definir rota e payload | Backend | Ex.: `POST /api/auth/google` com `{ "idToken": "...", "remember": true }`. | Concluído | `POST /api/auth/google`, Zod exige `idToken` e aceita `remember`. |
| F1-03 | Definir resposta | Backend | Compatível com `AuthResponse` (`token`, `user`). | Concluído | Retorna `{ message, token, user: { id, email, name } }` e cookies de sessão. |
| F1-04 | Definir regra de criação de conta | Backend/produto | Primeiro acesso cria usuário; campos obrigatórios e consentimento definidos. | Concluído | Cria conta com e-mail/nome/foto do Google, senha aleatória e `cellphone` nulo; consentimento segue o fluxo atual do app. |
| F1-05 | Definir vínculo com conta existente | Backend/produto | Comportamento para e-mail já cadastrado por senha e para conta desativada. | Concluído | Vincula automaticamente apenas com `email_verified=true`; se já houver outro `google_id`, responde 409. |
| F1-06 | Definir validação do token | Backend | Assinatura, emissor, audiência (`serverClientId`) e expiração validadas. | Concluído | `google-auth-library` valida assinatura/emissor/audiência/expiração; `GOOGLE_CLIENT_ID` é a audiência. |
| F1-07 | Definir erros e códigos | Backend | Token inválido, conta desativada, e-mail conflitante e indisponibilidade mapeados. | Concluído | 400 payload inválido, 401 token/e-mail inválido, 409 conflito de identidade, 503 sem configuração, 500 inesperado. |
| F1-08 | Validar endpoint manualmente | Backend | Requisição de teste retorna JWT utilizável nas rotas autenticadas. | Concluído | Deploy concluído em 2026-10-30; login real gerou JWT e acesso autenticado ao app. |

**Critérios de saída da Fase 1**

- Endpoint disponível em desenvolvimento/homologação.
- Contrato documentado (rota, payload, resposta e erros).
- Regras de conta e consentimento definidas pelo produto.

**Situação atual:** o backend implementa e publica `POST /api/auth/google` com `GoogleAuthRequestDto` (`idToken`, `remember`) retornando `AuthResponse`/JWT, incluindo criação e vínculo de contas. Validado em produção com token real em 2026-10-30.

## Fase 2 — Configuração OAuth e credenciais

Objetivo: preparar as credenciais e a configuração necessária para emissão e validação do ID token.

| ID | Passo | Arquivos/Artefatos | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F2-01 | Criar/confirmar projeto OAuth | Google Cloud Console | Projeto e tela de consentimento configurados. | Concluído | Projeto OAuth e tela de consentimento configurados; usuários de teste adicionados. |
| F2-02 | Criar cliente Android | Google Cloud Console | Pacote `br.com.tecpontes.appfinanceiro` + SHA-1 de debug registrados. | Concluído | Cliente Android criado com pacote e SHA-1 do build de teste. |
| F2-03 | Registrar fingerprints de release | Google Cloud Console | SHA-1/SHA-256 do certificado de release registrados. | Concluído | Fingerprints dos certificados usados nos ambientes de teste/release cadastrados. |
| F2-04 | Criar cliente Web | Google Cloud Console | `serverClientId` definido e alinhado ao backend. | Concluído | Web Client ID configurado no app (`GOOGLE_WEB_CLIENT_ID`) e no backend (`GOOGLE_CLIENT_ID`). |
| F2-05 | Definir forma de injeção do client ID | `app/build.gradle.kts` | Client ID público acessível via `BuildConfig`, sem segredo no código. | Concluído | `GOOGLE_WEB_CLIENT_ID` lido de `gradle.properties`/`local.properties` e exposto em `BuildConfig`. |
| F2-06 | Verificar interação com `google-services.json` | `app/build.gradle.kts` | Build local continua funcionando sem o arquivo; FCM não é afetado. | Concluído | `assembleDebug` executado sem `google-services.json`; plugin continua condicional. |
| F2-07 | Documentar credenciais por ambiente | Documentação | Debug, homologação e release descritos sem expor segredos. | Concluído | README com passo a passo OAuth, `local.properties` (não versionado) e documentação do backend. |

**Critérios de saída da Fase 2**

- Clientes OAuth criados para debug e release.
- `serverClientId` disponível ao app e ao backend.
- Nenhum segredo versionado.

## Fase 3 — Aquisição do ID token no Android

Objetivo: obter o ID token do Google com Credential Manager, isolando a lógica da Activity.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F3-01 | Declarar dependências | `gradle/libs.versions.toml`, `app/build.gradle.kts` | Credential Manager e credencial Google compilando com as versões atuais do projeto. | Concluído | `credentials` 1.5.0, `credentials-play-services-auth` 1.5.0 e `googleid` 1.1.1 (ver D-08). Compilação validada. |
| F3-02 | Criar componente de autenticação Google | `ui/auth/GoogleSignInClient.java` | Responsável único por montar a requisição e extrair o ID token. | Concluído | Componente `@Singleton` com callback tipado (`onSuccess`/`onCancelled`/`onError`). |
| F3-03 | Configurar `serverClientId` | `GoogleSignInClient`, `BuildConfig` | Requisição usa o client Web definido na Fase 2. | Concluído | `GetSignInWithGoogleOption` recebe `BuildConfig.GOOGLE_WEB_CLIENT_ID`. |
| F3-04 | Tratar cancelamento | Componente | Cancelamento não gera erro nem sessão. | Concluído | Validado em dispositivo: fechar o seletor não cria sessão nem exibe erro. |
| F3-05 | Tratar falhas do Google | Componente | Sem conta, serviços indisponíveis e erro de rede têm mensagens próprias. | Concluído | Validado em dispositivo, incluindo mensagem de app não registrado no OAuth. |
| F3-06 | Garantir ausência de logs sensíveis | Componente | Nenhum ID token em `Log`, toast ou relatório de erro. | Concluído | Logs registram apenas tipo de erro; o token nunca é logado nem persistido. |
| F3-07 | Expor resultado tipado | Componente | Sucesso (ID token) ou erro classificável para a camada superior. | Concluído | Fluxo real retornou o ID token e prosseguiu para a API. |

**Critérios de saída da Fase 3**

- Fluxo abre o seletor de contas e retorna ID token válido.
- Cancelamento e falhas tratados sem travar a UI.
- Nenhum token exposto em logs.

## Fase 4 — Integração API, repositório e ViewModel

Objetivo: trocar o ID token pelo JWT da aplicação e integrar os estados à arquitetura existente.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F4-01 | Criar DTO de requisição Google | `model/GoogleAuthRequestDto.java` | Campos alinhados ao contrato; `toString` não expõe token. | Concluído | `idToken` + `remember`; `toString` usa `[PROTECTED]`. Teste unitário criado. |
| F4-02 | Declarar endpoint | `network/ApiService.java` | Chamada `POST` disponível e compilando. | Concluído | `@POST("auth/google") Call<AuthResponse> loginWithGoogle(...)`. |
| F4-03 | Implementar troca no repositório | `data/repository/AuthRepository.java` | ID token enviado; JWT validado e salvo via `TokenManager`. | Concluído | Validado com o endpoint real: JWT salvo e requisições autenticadas funcionando. |
| F4-04 | Reutilizar tratamento de erros | `AuthRepository`, `ApiErrorUtils`, `NetworkErrors` | Erros HTTP e de rede seguem o padrão atual. | Concluído | Mesmas mensagens/utilitários do login por senha. |
| F4-05 | Expor estados no ViewModel | `viewmodel/LoginViewModel.java` | Loading, sucesso e erro do Google observáveis pela Activity. | Concluído | `loginWithGoogle` reutiliza `_loading`, `_error` e `_loginSuccess`. |
| F4-06 | Respeitar "manter conectado" | `AuthRepository`, `TokenManager` | Preferência aplicada ao JWT salvo, como no login por senha. | Concluído | Flag repassada ao backend e ao salvamento do JWT. |
| F4-07 | Garantir que ID token não seja persistido | `TokenManager` | Somente o JWT da API é armazenado. | Concluído | ID token apenas trafega para a API; não é salvo nem logado. |
| F4-08 | Testes da camada | `app/src/test/` | DTO, respostas e fluxo real cobertos. | Concluído | Teste unitário do DTO aprovado; integração validada end-to-end com token real e backend publicado. |

**Critérios de saída da Fase 4**

- Resposta válida do backend produz sessão idêntica à do login por senha.
- Estados de erro e carregamento consistentes com o fluxo atual.
- Nenhum ID token persistido.

## Fase 5 — Interface, navegação e sessão

Objetivo: conectar o botão Google ao fluxo real sem afetar login por senha e cadastro.

| ID | Passo | Arquivos principais | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F5-01 | Substituir toast provisório | `ui/auth/LoginActivity.java` | Botão inicia o fluxo real. | Concluído | Clique chama `startGoogleSignIn()`; toast removido. |
| F5-02 | Reutilizar estados de loading | `LoginActivity`, `LoginViewModel` | Botões desabilitados durante a autenticação e reabilitados ao final. | Concluído | Flag `googleSignInInProgress` evita duplo disparo; observer de loading mantém os botões. |
| F5-03 | Manter feedback de sucesso | `LoginActivity` | Toast e navegação equivalentes ao login por senha. | Concluído | `_loginSuccess` dispara o mesmo observer e navega para o dashboard. |
| F5-04 | Definir comportamento na aba de cadastro | `LoginActivity` | Mesma autenticação Google; criação/vínculo decididos pelo backend. | Concluído | O botão usa o mesmo fluxo nas duas abas; backend decide criação/vínculo (F1-04/F1-05). |
| F5-05 | Preservar pós-login | `LoginActivity` | Registro push e sincronização continuam sendo disparados. | Concluído | `navigateToDashboard()` inalterado. |
| F5-06 | Revisar textos e acessibilidade | `activity_login.xml`, `strings` | Rótulos claros e contraste adequado. | Concluído | Rótulos existentes ("Continuar com o Google"/"Inscrever-se no Google") mantidos. |
| F5-07 | Validar regressões | App em claro/escuro | Login por senha, cadastro, recuperação e logout inalterados. | Concluído | Fluxos existentes validados em dispositivo após a integração. |

**Critérios de saída da Fase 5**

- Botão Google funcional nas abas de login e cadastro.
- Navegação, loading e erros consistentes.
- Nenhuma regressão nos fluxos existentes.

## Fase 6 — Testes, segurança e documentação

Objetivo: validar o fluxo completo e registrar as configurações necessárias.

| ID | Passo | Arquivos/Artefatos | Critério de aceite | Status | Conclusão |
| :--- | :--- | :--- | :--- | :--- | :--- |
| F6-01 | Teste manual de primeira autenticação | Emulador/dispositivo | Conta criada e dashboard acessível. | Concluído | Primeiro acesso com conta Google criou usuário e abriu o dashboard. |
| F6-02 | Teste manual de usuário existente | Emulador/dispositivo | Sessão iniciada sem duplicar conta. | Concluído | Login com conta já cadastrada autenticou sem duplicar usuário. |
| F6-03 | Teste de cancelamento e falhas | Emulador/dispositivo | Sem sessão criada; mensagens corretas; botão reabilitado. | Concluído | Validado em dispositivo; botão é reabilitado e nenhuma sessão é criada. |
| F6-04 | Teste de token rejeitado | Backend + app | Erro claro, sem persistência de token. | Concluído | `POST /api/auth/google` com token inválido retornou 401 JSON; app exibiu erro sem salvar sessão. |
| F6-05 | Teste de sessão persistente e logout | Emulador/dispositivo | JWT respeita "manter conectado"; logout limpa sessão. | Concluído | Validado com a opção marcada/desmarcada e logout. |
| F6-06 | Validar expiração de sessão | App | `notifySessionExpired` continua redirecionando ao login. | Concluído | Fluxo de expiração mantido e validado. |
| F6-07 | Revisão de segurança | Código e configuração | Sem segredos/tokens em logs, repositório ou backups. | Concluído | ID token não é logado nem persistido; `.env`/`local.properties` fora do versionamento. |
| F6-08 | Atualizar README | `README.md` | Pré-requisitos OAuth, configuração local e teste documentados. | Concluído | Seção de login com Google com `GOOGLE_WEB_CLIENT_ID`, endpoint esperado e link deste plano. |
| F6-09 | Executar validação final | Gradle | `./gradlew testDebugUnitTest assembleDebug` sem erros. | Concluído | `BUILD SUCCESSFUL` em 2026-10-29; fluxo real validado em 2026-10-30. |

**Critérios de saída da Fase 6**

- Roteiro manual executado e registrado.
- Testes automatizados aprovados.
- Documentação de configuração disponível para a equipe.

## Implementação Android entregue

| Arquivo | Alteração |
| :--- | :--- |
| `gradle/libs.versions.toml` | `credentials` 1.5.0, `credentials-play-services-auth` 1.5.0, `googleid` 1.1.1. |
| `app/build.gradle.kts` | Leitura de `GOOGLE_WEB_CLIENT_ID` e `BuildConfig`; dependências do Credential Manager. |
| `gradle.properties` | Documentação da chave `GOOGLE_WEB_CLIENT_ID`. |
| `model/GoogleAuthRequestDto.java` | DTO `idToken` + `remember`, com token protegido em `toString`. |
| `network/ApiService.java` | `POST auth/google` retornando `AuthResponse`. |
| `data/repository/AuthRepository.java` | `loginWithGoogle` e tratamento de resposta compartilhado. |
| `viewmodel/LoginViewModel.java` | Estados de loading/sucesso/erro para o fluxo Google. |
| `ui/auth/GoogleSignInClient.java` | Credential Manager, extração do ID token e erros amigáveis. |
| `ui/auth/LoginActivity.java` | Botão Google conectado ao fluxo real, com proteção contra duplo clique. |
| `app/src/test/.../GoogleAuthRequestDtoTest.java` | Serialização e proteção do token em log. |
| `README.md` | Configuração local e contrato esperado. |

**Validação executada:** `./gradlew :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`.

**Limitações atuais:** nenhuma conhecida. O fluxo foi validado end-to-end em 2026-10-30 com backend publicado, credenciais OAuth reais e conta Google verdadeira.

## Bloqueios e decisões pendentes

Nenhum bloqueio pendente. As decisões B-01 a B-05 foram resolvidas:

| ID | Resolução |
| :--- | :--- |
| B-01 | Endpoint publicado e validado com token real; JWT aceito nas rotas autenticadas. |
| B-02 | Vínculo automático apenas para e-mail verificado pelo Google; conflito de identidade retorna 409. |
| B-03 | A opção "manter conectado" é aplicada também ao login Google. |
| B-04 | Web Client ID e fingerprints configurados; documentação de debug/release no README. |
| B-05 | Consentimento segue o fluxo atual de termos exibido na tela de cadastro. |

## Fora de escopo

- Substituir o JWT do backend pelo ID token do Google.
- Migrar a autenticação para Firebase Authentication.
- Refatorar a tela de login além do necessário para o fluxo Google.
- Alterar o fluxo de recuperação de senha.

## Registro de execução

| Data | Fase/Passo | Evidência |
| :--- | :--- | :--- |
| 2026-10-29 | Fase 0 | Baseline revisada no commit `7be30db`, árvore limpa. Botão Google era placeholder; `ApiService` sem endpoint; dependências ausentes. Plano gravado em `docs/`. |
| 2026-10-29 | Fases 2–5 (Android) | Dependências, DTO, endpoint, repositório, ViewModel, `GoogleSignInClient` e integração na `LoginActivity` implementados. `credentials` 1.6.0/`googleid` 1.2.1 falharam no kapt por metadata Kotlin; fixadas 1.5.0/1.1.1 (D-08). |
| 2026-10-29 | F6-09 | `./gradlew :app:testDebugUnitTest :app:assembleDebug` → `BUILD SUCCESSFUL`; teste do DTO aprovado. |
| 2026-10-29 | B-01 (verificação) | `POST /api/auth/google` em produção retorna HTML da SPA (rota inexistente); `POST /api/auth/login` retorna JSON 400. Endpoint Google ainda precisa ser criado no backend. |
| 2026-10-29 | Fases 1 e 4 (backend) | Backend em `/mnt/dados/workspace/appfinanceiro`: endpoint `POST /api/auth/google`, validação com `google-auth-library`, vínculo/criação de conta, `users.google_id`, `cellphone` opcional e migration `0022_add_google_login.sql`. `npm run check` OK; 8 testes novos e 6 testes de auth existentes passando. |
| 2026-10-30 | F1-08 / F2 / F6 | Backend publicado; `GOOGLE_CLIENT_ID` e `GOOGLE_WEB_CLIENT_ID` configurados; schema aplicado (`google_id` + `cellphone` nullable). Testes reais no dispositivo aprovados: primeiro acesso, conta existente, cancelamento, token rejeitado, "manter conectado", logout e expiração. |
| 2026-10-30 | Conclusão | Plano concluído; todas as fases em 100% e sem bloqueios pendentes. |
