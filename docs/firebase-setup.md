# Configuração do Firebase

Passos a fazer na tua conta antes de conseguires compilar e correr as apps.

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
2. Bundle ID: `pt.cestopartilhado.app` (tem de corresponder ao
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

## Notas

- Os valores dentro de `google-services.json`/`GoogleService-Info.plist`
  (incluindo o "API key") **não são segredos** — identificam o projeto
  Firebase, não dão acesso a nada por si só; a segurança real está nas regras
  do Firestore (`firebase/firestore.rules`) e na Authentication. É seguro
  incluir estes ficheiros no binário da app; só não os publiques num
  repositório Git público sem necessidade.
- Este projeto nunca foi ligado a um projeto Firebase real nem compilado com
  as tuas credenciais — os passos acima não foram executados por mim.
