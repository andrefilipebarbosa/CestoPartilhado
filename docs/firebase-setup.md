# Configuração do Firebase

> **Estado atual:** o projeto `cesto-partilhado` já está criado e ligado às
> duas apps (Android e iOS), com Authentication (Google) e Firestore
> ativos e as regras/índices publicados — feito via Firebase CLI numa sessão
> de trabalho anterior. `android/app/google-services.json` e
> `ios/CestoPartilhado/GoogleService-Info.plist` já existem localmente (não
> estão no Git, por serem específicos de cada máquina/checkout — ver nota no
> fim). As Cloud Functions abaixo **ainda não foram publicadas** (exigem o
> plano Blaze, que não foi ativado). O resto deste documento fica como
> referência caso precises de recriar isto de raiz (projeto novo, máquina
> nova, etc.).

## 1. Criar o projeto

1. Vai a [console.firebase.google.com](https://console.firebase.google.com) e cria
   um novo projeto (ex.: "Cesto Partilhado").
2. Faz upgrade ao plano **Blaze** (pago ao consumo) — Settings → Usage and
   billing. É obrigatório para as Cloud Functions (limpeza automática ao fim de
   30 dias e convites). O nível gratuito do Firestore/Functions mantém-se; só
   pagas se ultrapassares esse nível.

## 2. Authentication

Authentication → Sign-in method → ativa o fornecedor **Google**. Define o
"support email" pedido.

## 3. Firestore

Firestore Database → Create database → modo produção → região
**europe-west1** (Bélgica — combina com a região usada nas Cloud Functions e
nas regras deste projeto).

Depois de criada, publica as regras e os índices já preparados:

```bash
cd firebase
firebase login
firebase use --add   # escolhe o teu projeto
firebase deploy --only firestore:rules,firestore:indexes
```

## 4. Cloud Functions

```bash
cd firebase/functions
npm install
cd ..
firebase deploy --only functions
```

Isto publica: `purgeClosedLists` (corre uma vez por dia e elimina listas
fechadas há mais de 30 dias), `inviteMemberByEmail` (chamada pela app quando o
dono convida alguém) e `resolvePendingInvitesOnSignUp` (liga convites
pendentes a quem cria conta pela primeira vez).

## 5. App Android

1. Firebase console → Adicionar app → Android.
2. Nome do pacote: `pt.cestopartilhado.app` (tem de corresponder ao
   `applicationId` em `android/app/build.gradle.kts`).
3. Obtém o SHA-1 de debug (necessário para o Google Sign-In funcionar):
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   Cola o valor "SHA1: ..." no campo pedido pela Firebase. Repete este passo
   mais tarde com o keystore de **release** antes de publicares na Play Store.
4. Descarrega `google-services.json` e coloca-o em `android/app/google-services.json`.
5. Abre a pasta `android/` no Android Studio — deve sincronizar sem mais
   configuração.

## 6. App iOS

1. Firebase console → Adicionar app → iOS.
2. Bundle ID: `com.cestopartilhado` (tem de corresponder ao
   `PRODUCT_BUNDLE_IDENTIFIER` em `ios/project.yml`).
3. Descarrega `GoogleService-Info.plist` e coloca-o em
   `ios/CestoPartilhado/GoogleService-Info.plist`.
4. Abre esse ficheiro e copia o valor de `REVERSED_CLIENT_ID`. Adiciona-o como
   um segundo esquema de URL em `ios/project.yml`, na secção `CFBundleURLTypes`
   (o esquema `cestopartilhado` já lá está, para o link de convite — o
   `REVERSED_CLIENT_ID` é o que falta, e é exigido pelo Google Sign-In).
5. Volta a gerar o projeto Xcode:
   ```bash
   cd ios
   xcodegen generate
   open CestoPartilhado.xcodeproj
   ```
   Na primeira abertura o Xcode vai resolver os pacotes Swift (Firebase,
   GoogleSignIn) — precisa de ligação à internet nesse momento.

## 7. Login com Apple (só iOS)

O código das duas apps suporta login com Google; em iOS há também "Sign in
with Apple" nativo. Não precisas de nenhuma app externa nem de Services
ID/chave — basta:

1. [developer.apple.com](https://developer.apple.com/account) → Certificates,
   Identifiers & Profiles → Identifiers → escolhe o App ID
   `com.cestopartilhado` → ativa a capability **"Sign In with Apple"** →
   guarda. (A entitlement já está declarada em `ios/project.yml`; só falta
   isto do lado da conta Apple Developer.)
2. Firebase console → Authentication → Sign-in method → Apple → ativa. Não
   pede mais nada para o fluxo nativo (Services ID/Team ID/chave só são
   necessários para "Sign in with Apple" via web, que esta app não usa).

## 8. Cloud Messaging (push notifications)

O código das duas apps e das Cloud Functions (`onListWritten`,
`onSplitListWritten` em `firebase/functions/src/notifications.ts`) já está
pronto, mas faltam dois passos manuais antes de as notificações chegarem a
sério a um telemóvel:

1. **Publicar as Cloud Functions** — ver secção 4 acima. As notificações só
   são *enviadas* quando estas funções estiverem publicadas (o que exige o
   plano Blaze); até lá o código do cliente (registo de token, ecrã de
   preferências) funciona na mesma, só não chega nada porque não há ninguém
   do outro lado a disparar o envio.
2. **Chave APNs para o iOS** — a Google Cloud Messaging não consegue entregar
   pushes a dispositivos iOS sem isto; é o único passo que só o dono da conta
   Apple Developer consegue fazer:
   1. [developer.apple.com](https://developer.apple.com/account) → Certificates,
      Identifiers & Profiles → Keys → "+" → ativa "Apple Push Notifications
      service (APNs)" → cria a chave e descarrega o ficheiro `AuthKey_XXXXXXXXXX.p8`
      (só é possível descarregar uma vez — guarda-o bem).
   2. Anota o **Key ID** (aparece ao lado do nome da chave) e o **Team ID**
      (canto superior direito da página, ou em Membership).
   3. Firebase console → Project settings → Cloud Messaging → Apple app
      configuration (aparece depois de a app iOS estar registada, secção 6) →
      "Upload" em **APNs Authentication Key** → carrega o `.p8` e preenche Key
      ID + Team ID.
   4. Não é preciso mexer no Xcode/`project.yml` para isto — o
      `UIBackgroundModes: remote-notification` e o registo do token já estão
      configurados no código.

O Android não precisa de nenhum passo manual equivalente — o
`google-services.json` já existente é suficiente para o FCM.

Cada utilizador escolhe em Definições → Notificações quais dos 3 tipos quer
receber (por omissão: "adicionado a uma lista" e "lista concluída" ligados,
"lista atualizada" desligado); as Cloud Functions nunca notificam quem fez a
própria alteração, nem quem está com o ecrã dessa lista aberto nesse momento.

## Notas

- Os valores dentro de `google-services.json`/`GoogleService-Info.plist`
  (incluindo o "API key") **não são segredos** — identificam o projeto
  Firebase, não dão acesso a nada por si só; a segurança real está nas regras
  do Firestore (`firebase/firestore.rules`) e na Authentication. É seguro
  incluir estes ficheiros no binário da app; só não os publiques num
  repositório Git público sem necessidade.
- `google-services.json` e `GoogleService-Info.plist` estão no `.gitignore`
  de propósito — se clonares este repositório noutra máquina, repete os
  passos 5/6 (ou copia os ficheiros de uma máquina onde já existam) antes de
  compilar.
- Antes de publicares nas lojas, ainda faltam: SHA-1 de **release** no
  Android (passo 5.3), plano Blaze + `firebase deploy --only functions` para
  ativar os convites por email e a limpeza automática aos 30 dias, e rever
  os documentos em `legal/`.
