# Integração FCM no app Android

Este guia descreve a preparação do aplicativo Android para receber lembretes e outras notificações por Firebase Cloud Messaging (FCM). O envio e o agendamento ficam no backend; o app obtém e registra o token FCM, exibe a notificação e abre o histórico.

> **Estado:** documentação de implementação. FCM ainda não está integrado neste app. Alinhe o contrato de token e o formato do payload com `docs/FCM_INTEGRATION.md` do backend antes de implementar.

## Situação atual

- O app não possui dependências Firebase nem um `FirebaseMessagingService`.
- A publicação local atual é alimentada pelo `NotificationPollingWorker`, que consulta logs do servidor periodicamente.
- `NotificationChannels` e `LocalNotificationPublisher` já fornecem infraestrutura para exibir notificações locais e navegar ao histórico.
- `POST_NOTIFICATIONS` já está declarado no `AndroidManifest.xml` e a tela de notificações já trata a permissão em Android 13+.
- O Web Push/VAPID do servidor é para o PWA; o Android deve registrar um token FCM próprio.

## 1. Preparar o Firebase para Android

1. Use o mesmo projeto Firebase configurado para o backend.
2. Registre um app Android com application ID `br.com.tecpontes.appfinanceiro`.
3. Baixe o `google-services.json` desse app e coloque-o em `app/google-services.json`.
4. Confira que o arquivo corresponde aos ambientes/application IDs usados no build. Se houver variantes, configure um arquivo por variante.
5. O arquivo de configuração cliente não é uma service account. Nunca adicione credenciais Admin SDK ou chaves privadas ao app.

## 2. Adicionar Firebase Messaging ao Gradle

O projeto usa Gradle Kotlin DSL e version catalog (`gradle/libs.versions.toml`). Siga as versões compatíveis vigentes na documentação oficial Firebase:

1. Declare o plugin `com.google.gms.google-services` no version catalog e no `build.gradle.kts` raiz com `apply false`.
2. Aplique o plugin no `app/build.gradle.kts`.
3. Adicione o Firebase BoM e a dependência `com.google.firebase:firebase-messaging` usando o version catalog, sem fixar uma versão independente para Messaging.
4. Sincronize o Gradle e compile uma variante de debug com o arquivo de configuração no local correto.

Não adicione Firebase Analytics se o requisito for somente mensagens FCM.

## 3. Implementar registro e renovação do token

Crie um serviço/repositório do app para registrar o token junto à API autenticada. Contrato proposto pelo guia do backend:

- `POST /api/device-push-tokens`
- corpo JSON: `{"token":"<token-fcm>","deviceId":"<id-da-instalacao>","platform":"android"}`
- `DELETE /api/device-push-tokens/:id` para remover o registro associado a esta instalação.

Implemente o fluxo:

1. Solicite o token com `FirebaseMessaging.getInstance().token` depois que a sessão estiver autenticada.
2. Envie-o à API com o JWT atual. Não coloque o token FCM em logs de produção.
3. Persista localmente o ID de registro retornado pela API, se aplicável.
4. Implemente uma subclasse de `FirebaseMessagingService` e, em `onNewToken`, atualize o registro no backend quando o usuário estiver autenticado. Se não houver sessão/rede, guarde a atualização para retry após login/conexão.
5. Registre novamente após login e ao reabrir o app para cobrir reinstalação, troca de conta e falhas de sincronização.
6. No logout, remova do backend o token/registro da conta atual antes de limpar a sessão. Não remova o token Firebase global do aparelho se o app permitir login subsequente; reassocie-o à nova conta após autenticação.
7. Trate HTTP 401/rede indisponível sem bloquear login ou uso normal do app.

O token FCM identifica uma instalação, não um usuário autenticado. O backend deve associá-lo à conta a partir da sessão e nunca confiar num `userId` enviado pelo app.

## 4. Declarar o serviço FCM

Registre a implementação de `FirebaseMessagingService` no `AndroidManifest.xml` com `android:exported="false"` e o intent filter `com.google.firebase.MESSAGING_EVENT`. O serviço deve:

- tratar `onNewToken` para atualizar o backend;
- tratar `onMessageReceived` para validar e exibir o payload;
- ignorar payload inválido sem encerrar o processo;
- evitar registrar tokens ou dados privados nos logs.

## 5. Processar e mostrar notificações

Combine o payload com o backend antes da implementação. O formato recomendado é uma mensagem de dados com valores string, incluindo `logId`, `type`, `title`, `body` e `deepLink`.

1. Em `onMessageReceived`, valide o `logId` e o conteúdo e crie uma notificação local usando o canal já existente em `NotificationChannels`.
2. Reutilize ou adapte `LocalNotificationPublisher` para aceitar o payload FCM. Preserve o canal/prioridade coerentes com o nível da mensagem e verifique a permissão antes de publicar.
3. Inclua o ID do `message_log` no `PendingIntent`, para abrir `NotificationHistoryActivity` e marcar a notificação correspondente como lida.
4. Se o payload não possuir `logId` (por exemplo, mensagem de teste), publique com fallback seguro e sem quebrar o histórico.
5. Teste app em foreground, background, processo encerrado, aparelho sem rede e permissão negada. FCM pode ser adiado por rede, economia de bateria e políticas do Android; não prometa entrega exata ao segundo.

O backend deve criar o log antes do envio, para incluir o ID no FCM. Assim, o app abre o mesmo item que aparece no histórico sincronizado.

## 6. Evitar duplicação com o polling existente

Durante a migração, um mesmo `message_log` pode chegar por FCM e por `NotificationPollingWorker`. Não deixe os dois caminhos publicar a mesma notificação.

1. Use o `logId` como chave de deduplicação.
2. Ao publicar via FCM, persista esse ID usando `NotificationPreferences` (ou armazenamento compartilhado equivalente), que o worker também consulta antes de publicar.
3. Faça o worker ignorar IDs já entregues ao app; garanta que o registro local sobreviva ao encerramento do processo e reinício do aparelho, conforme o comportamento atual das preferências.
4. Para cobrir a corrida em que o polling ocorre antes do FCM, alinhe com o backend a indicação de entrega Android por log/destino. O cliente deve ignorar logs marcados pelo servidor como enviados ao FCM, ou o polling deve ser desativado para esses registros após validar a migração.
5. Não desative o polling de fallback de forma global antes de validar recebimento, permissão e renovação de token em dispositivos reais. Escolha uma regra explícita para logs antigos e dispositivos que não têm token registrado.

## 7. Permissões, canal e UX

- Preserve a solicitação da permissão `POST_NOTIFICATIONS` em Android 13+; uma permissão negada impede exibição mesmo que o FCM chegue.
- Crie os canais antes da primeira notificação (a inicialização atual já chama `NotificationChannels.ensureCreated`). Em versões Android que deixam o usuário controlar canais, respeite a configuração do sistema.
- A interface deve informar quando notificações do aparelho estão desativadas, sem impedir salvar lembretes no servidor.
- Se o usuário desativar notificações pelo app, envie a preferência ao backend ou remova o token de entrega conforme a regra de produto; apenas parar de publicar localmente ainda deixa o servidor enviando FCM.

## 8. Testes e validação

Valide com dispositivo/emulador Google Play Services e token obtido em runtime:

- registro pós-login, atualização em `onNewToken`, retry offline e remoção no logout;
- isolamento entre usuários ao trocar de conta no mesmo aparelho;
- payload válido/inválido, toque e deep link ao histórico;
- foreground, background, processo encerrado e permissão negada;
- um único alerta quando o mesmo log chegar por FCM e polling;
- logs não duplicados, modo “Não Perturbe” e lembretes recorrentes;
- build de debug e release com as configurações Firebase correspondentes.

Use Firebase Console ou Firebase Admin no backend para enviar uma mensagem de teste ao token de um dispositivo. Não use a chave privada Admin SDK no app para testar.

## Referências deste repositório

- `app/build.gradle.kts` — dependências Android.
- `gradle/libs.versions.toml` — catálogo de versões Gradle.
- `app/src/main/AndroidManifest.xml` — permissões e componentes declarados.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/NotificationChannels.java` — canais Android.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/LocalNotificationPublisher.java` — publicação e navegação.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/NotificationPollingWorker.java` — fallback/polling e deduplicação atual.
- `app/src/main/java/br/com/tecpontes/appfinanceiro/notifications/NotificationPreferences.java` — estado local de notificações já apresentadas.
- `docs/FCM_INTEGRATION.md` no repositório do backend — armazenamento de tokens, envio, agenda e API.
