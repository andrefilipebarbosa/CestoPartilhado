# Cesto Partilhado

App de listas de compras partilhadas, por loja, com Firebase como backend.
Duas apps nativas separadas (Android/Kotlin e iOS/Swift), sem código
partilhado entre elas.

## Estrutura

```
firebase/          Regras do Firestore, índices e Cloud Functions (Node/TypeScript)
android/           App Android nativa — Kotlin + Jetpack Compose
ios/               App iOS nativa — Swift + SwiftUI (projeto gerado com xcodegen)
legal/             Termos de Serviço e Política de Privacidade (PT + EN, rascunhos)
docs/firebase-setup.md   Passo a passo de configuração do Firebase
```

## Antes de compilar

Nenhuma das apps compila sem primeiro configurares o teu próprio projeto
Firebase — segue **[docs/firebase-setup.md](docs/firebase-setup.md)**. Em
resumo, faltam sempre `android/app/google-services.json` e
`ios/CestoPartilhado/GoogleService-Info.plist`, que só tu consegues gerar
(dependem da tua conta Firebase).

## O que está implementado

- Login com Google (Firebase Auth) nas duas apps.
- Listas de compras organizadas por loja, com autocompletar/criação de lojas
  a partir de um catálogo global partilhado.
- Qualquer pessoa convidada para uma lista pode adicionar/remover artigos e
  marcá-los como comprados; **só quem criou a lista a pode eliminar**
  (reforçado nas regras do Firestore, não só na interface).
- Fechar/reabrir listas; listas fechadas há mais de 30 dias são eliminadas
  automaticamente por uma Cloud Function agendada.
- Convite por email (Cloud Function) com resolução automática de convites
  pendentes quando essa pessoa cria conta.
- "Exportar os meus dados": gera um relatório em texto com todas as listas a
  que o utilizador tem acesso, e abre o menu de partilha nativo (share sheet)
  do Android/iOS.
- Português/Inglês nas duas apps, com deteção automática do idioma do
  telefone e escolha manual nas Definições.
- Ícone da app e paleta de cores, reaproveitados do pacote de design.

## Verificação feita nesta sessão

- **iOS**: o projeto foi gerado com `xcodegen` e **compilou com sucesso**
  (`xcodebuild build`, destino "iOS Simulator") — os pacotes Swift do Firebase
  e do GoogleSignIn já estavam em cache local nesta máquina. Um erro real de
  sintaxe (`onChange` só disponível a partir do iOS 17) foi encontrado e
  corrigido desta forma.
- **Android**: não foi possível compilar nesta sessão — não há Gradle nem
  Kotlin instalados aqui, e o ambiente não tem acesso à rede para descarregar
  o Gradle Wrapper. O código foi escrito e revisto com cuidado (incluindo uma
  correção a um bug real de listeners de itens não seguidos corretamente),
  mas só terá confirmação de compilação real quando abrires o projeto no
  Android Studio.

## O que falta para publicar nas lojas

- A tua conta Firebase configurada (ver acima) — sem isto nada corre.
- Conta de programador Apple (99 USD/ano) e Google Play Console (25 USD,
  pagamento único), criadas e geridas por ti.
- Assinatura dos builds e submissão manual em cada consola — não é algo que
  eu consiga fazer por ti.
- Rever os documentos em `legal/` com um advogado antes de publicar — são
  rascunhos, não aconselhamento jurídico, e têm campos por preencher
  ([EMAIL DE CONTACTO], jurisdição, etc.).
- Screenshots reais da app a correr para as fichas das lojas (os que já
  tinhas do canvas de design são mockups, não capturas da app real).
- O link de convite de listas (`cestopartilhado://join?listId=...`) hoje só
  abre a app quando já está instalada; não configurámos Universal
  Links/App Links (isso exige teres um domínio próprio a apontar para a app).
