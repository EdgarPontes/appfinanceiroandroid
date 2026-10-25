# Relatório de Roteiro e Execução de Testes Manuais FCM

Este documento detalha o protocolo e os passos de validação manual para os 5 cenários críticos de integração do Firebase Cloud Messaging (FCM) no aplicativo Android **AppFinanceiro**.

---

## Resumo da Validação Automatizada

Os contratos de dados, regras de expiração, validação de `bindingId` e persistência do `deviceId` foram validados via testes unitários automatizados:

```text
Task :app:testDebugUnitTest
11 passed, 0 skipped, 0 failed
```

- [`PushRegistrationStoreTest`](file:///mnt/dados/AndroidStudioProjects/appfinanceiro/app/src/test/java/br/com/tecpontes/appfinanceiro/notifications/PushRegistrationStoreTest.java): Garante idempotência de `deviceId` por instalação, persistência de fuso horário e manipulação de `bindingId`/`registrationId`.
- [`NotificationContractsTest`](file:///mnt/dados/AndroidStudioProjects/appfinanceiro/app/src/test/java/br/com/tecpontes/appfinanceiro/notifications/NotificationContractsTest.java): Valida rejeição de payloads com `bindingId` legado, `logId` inválido, links não autorizados e tipos/prioridades não suportados.

---

## Matriz de Execução de Testes Manuais

| Cenário | Componente Alvo | Objetivo Principal | Ferramenta / Comando | Resultado Esperado |
| :--- | :--- | :--- | :--- | :--- |
| **1. Token Rotacionado** | `AppFirebaseMessagingService#onNewToken`<br>`PushRegistrationManager` | Verificar se ao rotacionar o token FCM o app re-registra a instalação mantendo o mesmo `deviceId`. | ADB / Logcat (`tag:PushRegistrationWorker`) | Novo token é enviado ao servidor (`POST /api/device-push-tokens`) mantendo o mesmo `deviceId`. |
| **2. Troca de Conta / Logout Offline** | `PushRegistrationStore`<br>`AppFirebaseMessagingService` | Garantir que mensagens enviadas para a conta antiga sejam descartadas pela verificação de `bindingId`. | ADB (`cmd statusbar expand`) + Troca de usuário | Notificações com `bindingId` da Conta A são ignoradas se a Conta B estiver ativa. |
| **3. Mudança de Fuso / DST** | `PushRegistrationManager#synchronizeReminderTimeZone` | Sincronizar fusos e recalcular horários de lembrete no servidor ao mudar o fuso do sistema. | ADB (`service call alarm`) | Chamada `PATCH /api/message-configs/:id` enviando o novo `timeZone` IANA. |
| **4. Permissão Negada** | `LocalNotificationPublisher#canPost`<br>`POST_NOTIFICATIONS` | Garantir que negado o acesso às notificações, o token é registrado sem crashes e o histórico REST continua funcionando. | Configurações Android (Permissões) | App não exibe pop-up/banner, mas o item fica visível no histórico REST da aplicação. |
| **5. App Force-Stopped** | Sistema Operacional Android / Broadcast | Confirmar que mensagens FCM não despertam o app forçado a parar até que o usuário abra o app. | ADB (`am force-stop`) | Mensagens chegam somente após o aplicativo ser reaberto manualmente pelo usuário. |

---

## Passo a Passo para Execução dos Testes

### Cenário 1: Token Rotacionado (`onNewToken`)

#### Pré-requisitos
- App instalado no dispositivo e usuário autenticado.

#### Passos de Teste
1. Abra o **Logcat** e aplique o filtro `tag:PushRegistrationWorker` ou `tag:NetworkModule`.
2. Para simular a revogação/rotação do token FCM pelo SDK do Firebase, execute o comando ADB ou limpe os dados do serviço Google Play Services:
   ```bash
   adb shell am broadcast -a com.google.android.gms.iid.InstanceID --es "CMD" "RST"
   ```
3. Alternativamente, force uma chamada a `FirebaseMessaging.getInstance().deleteToken()` via botão/debug.
4. O SDK do Firebase acionará `onNewToken(token)`.

#### Resultado Esperado
- O `AppFirebaseMessagingService` aciona `registrationManager.requestSync()`.
- O `PushRegistrationWorker` realiza a requisição HTTP:
  `POST /api/device-push-tokens` com o novo token, mantendo o `deviceId` UUID original da instalação.
- As notificações continuam sendo entregues normalmente para o dispositivo.

---

### Cenário 2: Troca de Conta e Logout Offline

#### Pré-requisitos
- Duas contas ativas no sistema (Conta A e Conta B).

#### Passos de Teste
1. Faça login na **Conta A**. Anote o `bindingId` retornado no registro.
2. Desative o Wi-Fi e os Dados Móveis do dispositivo (ficar offline).
3. Realize o **Logout** no aplicativo.
4. Ative a conexão de rede e faça login com a **Conta B**.
5. O aplicativo registrará a instalação para a Conta B e receberá um novo `bindingId` (ex: `binding-B`).
6. Envie uma mensagem de teste FCM via Backend contendo o `bindingId` da Conta A (`binding-A`).

#### Resultado Esperado
- O método `onMessageReceived` verifica que `data.get("bindingId")` (`binding-A`) é diferente do `currentBindingId` local (`binding-B`).
- A notificação é descartada silenciosamente (`return`), sem emitir som ou banner.
- O app não vaza notificações nem dados da Conta A para a Conta B.

---

### Cenário 3: Mudança de Fuso Horário e Horário de Verão (DST)

#### Pré-requisitos
- Usuário autenticado com ao menos um lembrete configurado no fuso `America/Sao_Paulo`.

#### Passos de Teste
1. Abra o app para garantir que o fuso atual esteja registrado na store local.
2. Altere o fuso horário do sistema Android via ADB ou pelas configurações do dispositivo:
   ```bash
   # Alterar para fuso UTC
   adb shell service call alarm 3 s16 "UTC"
   ```
3. Reabra o aplicativo ou traga-o do background para o foreground.
4. Verifique o tráfego HTTP no Logcat ou na ferramenta de inspeção.

#### Resultado Esperado
- O `PushRegistrationManager` detecta que `ZoneId.systemDefault().getId()` ("UTC") difere do fuso salvo ("America/Sao_Paulo").
- O app consulta as configurações via `GET /api/message-configs` e executa:
  `PATCH /api/message-configs/:id` atualizando `timeZone` para `"UTC"`.
- O fuso salvo localmente atualiza para `"UTC"`.

---

### Cenário 4: Permissão de Notificação Negada (`POST_NOTIFICATIONS`)

#### Pré-requisitos
- Dispositivo rodando Android 13+ (API level 33+).

#### Passos de Teste
1. Acesse **Configurações -> Aplicativos -> AppFinanceiro -> Permissões -> Notificações** e selecione **Negar**.
2. Faça login no aplicativo.
3. Dispare uma notificação de vencimento financeira pelo Backend.
4. Abra o app e acesse a tela de **Histórico de Notificações**.

#### Resultado Esperado
- O registro do token FCM em `POST /api/device-push-tokens` ocorre com sucesso (status HTTP 200/201).
- Ao receber o mensagem FCM, `LocalNotificationPublisher.canPost(context)` retorna `false`.
- Nenhuma exceção `SecurityException` é lançada e o app não sofre crash.
- Ao abrir o **Histórico de Notificações** no aplicativo, a notificação aparece listada via API REST.

---

### Cenário 5: Aplicativo Forçado a Parar (Force-Stopped)

#### Pré-requisitos
- Dispositivo físico ou emulador com Google Play Services ativo.

#### Passos de Teste
1. Abra o app e garanta que ele esteja registrado com a conta atual.
2. Force a parada do aplicativo via ADB ou pelas configurações do Android:
   ```bash
   adb shell am force-stop br.com.tecpontes.appfinanceiro
   ```
3. Envie uma mensagem FCM pelo Backend para o token registrado.
4. Observe o dispositivo enquanto ele estiver no estado `STOPPED`.
5. Abra o aplicativo manualmente através do ícone na launcher.

#### Resultado Esperado
- Enquanto o app estiver no estado `STOPPED`, o sistema operacional Android bloqueia broadcasts para a aplicação. A notificação não é exibida imediatamente.
- Assim que o usuário toca no ícone e abre o app, a aplicação é retirada do estado `STOPPED`.
- Ao sincronizar com o backend, o histórico de notificações reflete o item registrado no servidor.

---

## Comandos Úteis para Logcat e Diagnóstico ADB

```bash
# Filtrar logs de sincronização de registro FCM
adb logcat -v time | grep -E "PushRegistrationWorker|AppFirebaseMessagingService|NetworkModule"

# Simular alteração de fuso horário via ADB
adb shell service call alarm 3 s16 "America/New_York"

# Forçar parada do app
adb shell am force-stop br.com.tecpontes.appfinanceiro

# Conceder/Revogar permissão de notificação no Android 13+
adb shell pm grant br.com.tecpontes.appfinanceiro android.permission.POST_NOTIFICATIONS
adb shell pm revoke br.com.tecpontes.appfinanceiro android.permission.POST_NOTIFICATIONS
```
