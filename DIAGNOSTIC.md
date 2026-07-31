# DIAGNOSTIC — TasksWidget

Constat factuel établi le **2026-07-31**, avant toute modification.
Rien n'a été corrigé : ce document consigne l'état trouvé.

Ticket : **GEN-8** (epic GEN-7).

## Conditions

| | |
|---|---|
| Commit examiné | `4dfcd39` — *remove Android target and update dependencies*, 2025-08-24 (HEAD de `develop`) |
| JDK | OpenJDK 17 |
| Gradle | wrapper 7.6 |
| Commandes | `gradlew.bat :desktop:compileKotlinJvm`, `:common:desktopTest` |

À noter d'emblée : sur ce dépôt, le wrapper `./gradlew` invoqué depuis Git Bash
reçoit un argument vide et échoue (`Task '""' not found`). Il faut passer par
`gradlew.bat`. Sans incidence sur le code, mais bloquant si on ne le sait pas.

Comme pour ScreenBrightness, ce rapport distingue ce qui a été **prouvé à
l'exécution** de ce qui a été **lu sans être exécuté**. L'application, une
fenêtre de bureau, n'a pas été lancée graphiquement.

---

## 1. Sécurité — le point le plus grave, et il est dans l'arbre, pas dans l'historique

**Le certificat de signature de code est commité, avec son mot de passe.**

```
desktop/certificate.pfx   2852 octets   SUIVI par git
desktop/key-password        14 octets   SUIVI par git
```

`certificate.pfx` est le certificat qui signe le `.msi` distribué — il contient
une **clé privée**. `key-password` en est le mot de passe. Les deux sont dans
l'arbre courant, pas seulement dans l'historique : `git ls-files` les liste. La
tâche `signMsi` de `desktop/build.gradle.kts` les lit directement
(`file("certificate.pfx")`, `file("key-password").readText()`).

Cette identité de signature doit être **considérée comme compromise** : elle est
publique depuis que le dépôt l'est. À traiter comme les autres secrets de
GEN-4 — révoquer/remplacer le certificat, puis le retirer du dépôt et de
l'historique.

**Le compte de service Firebase, lui, est déjà neutralisé** : le fichier
`taskwidget-b17c3-6fa8ea1f5dbe.json` (le compte de service Admin supprimé en
GEN-4) figure au `.gitignore` et n'est plus dans l'arbre. C'est cohérent avec la
purge déjà faite.

**La synchronisation Google est cassée par la purge.** `OAuthApi.kt` porte
maintenant `CLIENT_ID = ""` et `CLIENT_SECRET = ""` (lignes 148-149), vidés lors
du nettoyage. Le flux OAuth ne peut donc plus aboutir : la connexion Google et
la « synchronisation entre appareils » annoncée sont inertes tant que ces
valeurs ne sont pas réinjectées par une voie sûre (variable d'environnement,
fichier non suivi). Aucun repli n'est prévu dans le code.

---

## 2. État du build — il compile, malgré une configuration incohérente

`gradlew.bat :desktop:compileKotlinJvm` : ✅ **passe** (2 min 26).

C'est en soi une bonne nouvelle, mais la configuration est un champ de mines qui
tient par chance :

**Les versions se contredisent d'un fichier à l'autre.**

| | `build.gradle.kts` (racine) | `gradle.properties` (lu par `settings.gradle`) |
|---|---|---|
| Kotlin | `1.9.0` | `1.8.0` |
| Compose | `1.5.0` | `1.6.2` |

Le `build.gradle.kts` racine déclare `kotlin("multiplatform") version "1.9.0"` et
`compose version "1.5.0"` (en `apply false`), tandis que les modules appliquent
ces plugins **sans version**, donc résolus par le `pluginManagement` de
`settings.gradle` qui lit `extra["kotlin.version"]` = 1.8.0 et
`extra["compose.version"]` = 1.6.2. Deux sources de vérité qui divergent ; la
compilation retient les secondes, mais rien ne garantit que ça continue.

**Les toolchains JVM divergent aussi** : `common` cible `jvmToolchain(17)`,
`desktop` cible `jvmToolchain(11)` — deux niveaux de bytecode dans le même
binaire.

**`gradle.properties` traîne des restes Android** (`agp.version=7.3.0`,
`android.useAndroidX=true`) alors que la cible Android a été retirée.

---

## 3. Les tests existent, et un tiers échoue

Le brief de l'epic affirme « aucun test ». **C'est faux** : il y a 7 fichiers de
test (`ConfigsTest`, `GithubTest`, `MarkdownTest`, `TaskTest`, `TaskUtilsTest`,
`TasksTest`, `TimerTest`). `:common:desktopTest` exécute **10 tests, dont 4
échouent** — de vrais bugs, pas des tests mal écrits :

```
MarkdownTest > testOverlappingBoldAndStrikethrough   FAILED
MarkdownTest > testNestedBoldAndItalic               FAILED
MarkdownTest > testBoldItalicCode                    FAILED
TaskUtilsTest > test get date in correct format      FAILED (Expected value to be true)
```

Le rendu Markdown se casse sur les styles **imbriqués et chevauchants**
(gras dans italique, gras chevauchant barré, gras-italique-code) — or le
Markdown des descriptions de tâches est une fonctionnalité mise en avant dans le
README. Et un test de **formatage de date** échoue, ce qui rejoint les crashs de
date mentionnés dans l'historique (« Deadline crash », commit `3d3e038`).

---

## 4. Hygiène de dépôt — 371 fichiers suivis, dont la plupart n'ont rien à y faire

Le dépôt pèse **29,6 Mo** pour ~200 Ko de Kotlin. La cause n'est pas le code :

* **`wix311/` — 220 fichiers suivis.** C'est le **WiX Toolset 3.11 entier**
  committé dans le dépôt : binaires, bibliothèques `.lib` x86/x64, documentation
  `.chm`, SDK Visual Studio 2010. Un outil tiers de fabrication d'installeur, qui
  n'a pas sa place dans les sources. C'est aussi lui, et non l'application, qui
  explique le « C 224 Ko, C++ 28 Ko » relevé au scan : **le brief a pris le
  toolset WiX pour du code natif de l'app.** Il est désormais au `.gitignore`
  (`wix311/`) mais **reste suivi** — l'ignorer ne l'enlève pas.
* **`medias/` — 21 fichiers**, dont une **capture vidéo d'écran**
  (`vokoscreenNG-2024-09-28_20-13-05.mp4`) et sept captures PNG. La vidéo n'a
  aucune raison d'être versionnée.
* **Le module Android est orphelin mais toujours là.** La cible Android a été
  retirée du build (commit `4dfcd39`), mais les sources restent suivies :
  `android/build.gradle.kts`, `android/src/main/AndroidManifest.xml`,
  `android/.../MainActivity.kt`, `common/src/androidMain/`. Du code mort qui ne
  compile plus, et qui entretient la confusion sur le caractère « multiplateforme ».

---

## 5. « Multiplatform » — l'affirmation ne tient pas

Le README titre « TasksWidget is a multiplatform application » puis se dédit deux
lignes plus bas : « Works on Windows only for now ». Le `settings.gradle`
confirme : seuls `:desktop` et `:common` sont inclus, `:android` est commenté.
`nativeDistributions` déclare bien Dmg / Msi / Deb, mais rien n'atteste que le
`.dmg` (macOS) ou le `.deb` (Linux) aient jamais été produits ou testés. En
l'état, c'est une application **Compose Desktop pour Windows**, et le socle
Kotlin Multiplatform ne sert qu'un seul os.

---

## 6. Ce que fait l'application, aujourd'hui

Point d'entrée : `desktop/.../Main.kt` → `start()` (dans `common/.../ui/view/main.kt`),
qui lance une `application { TasksWidget() }` Compose Desktop.

C'est un **widget de tâches de bureau** : une fenêtre fine, sans bordure,
posée sur le côté de l'écran. Modules de code (`common/src/commonMain`) :

| Domaine | Fichiers | Rôle |
|---|---|---|
| Vues | `ui/view/` (App, main, taskCard, tasksList, toolbar, configs, slider, webview…) | l'interface du widget |
| Tâches | `controller/Tasks.kt`, `TasksUtils.kt`, `model/Task.kt` | création, tri, filtrage, historique |
| Markdown | `controller/markdown.kt` | rendu des descriptions (partiellement cassé, §3) |
| Auth Google | `controller/firebase/authentication/` (OAuthApi, AuthenticationApi, JWToken…) | connexion OAuth (inerte, §1) |
| Firestore | `controller/firebase/firestore/FirestoreApi.kt` | synchro cloud des tâches |
| Mise à jour | `controller/updater.kt`, `model/github/` | auto-update depuis les releases GitHub |
| Divers | `mailer.kt` (envoi d'e-mail), `EmojiApi.kt`, `Timer.kt`, `Translation.kt`, `network.kt` | fonctions annexes |

**La liste des dépendances est un inventaire à la Prévert** (`common/build.gradle.kts`) :
google-cloud-firestore, google-cloud-logging, google-auth, jjwt, ktor **client
et serveur** (netty), simple-java-mail, jsch (SSH), Apache POI (Excel), JNA,
compose-webview-multiplatform, markdown, gson, guava… Pour un widget de tâches,
c'est démesuré, et plusieurs entrées interrogent :

* **`com.guardsquare:proguard-gradle:7.2.2` est déclaré en `implementation`** —
  c'est un plugin Gradle, pas une bibliothèque d'exécution. Il n'a rien à faire
  dans le classpath de l'application.
* **ktor-server-netty** dans une application de bureau : vraisemblablement le
  serveur local qui reçoit la redirection OAuth (`http://localhost:8080/`), mais
  un serveur Netty complet pour ça est lourd.

---

## 7. Ce qui n'a pas été vérifié

* L'application **n'a pas été lancée graphiquement** — c'est une fenêtre de
  bureau, et l'exécution ouvre une UI interactive. Le comportement réel du
  widget (positionnement, réduction, barre système) vient donc du README et du
  code, pas de l'observation.
* `packageMsi` / `signMsi` n'ont pas été tentés (ils exigent WiX et le
  certificat).
* Les cibles `.dmg` et `.deb` n'ont pas été produites.
* Seul `:desktop:compileKotlinJvm` et `:common:desktopTest` ont été exécutés.

---

## Ordre de traitement suggéré

1. **Sortir `certificate.pfx` et `key-password` du dépôt** (§1) et faire
   remplacer le certificat — c'est le seul point de sécurité, et il est vivant.
2. **Trancher la synchro Google** (§1) : réinjecter les identifiants OAuth par
   une voie sûre, ou assumer que la fonctionnalité est retirée et nettoyer le
   code correspondant.
3. **Unifier les versions** (§2) : une seule source de vérité pour Kotlin,
   Compose et la toolchain.
4. **Alléger le dépôt** (§4) : désuivre `wix311/` et la vidéo, retirer le module
   Android orphelin.
5. **Réparer les tests rouges** (§3), en les gardant comme critère.
6. Ajouter une CI (aucune aujourd'hui — ce point du brief, lui, est exact).

---

## Corrections à apporter aux tickets de l'epic

Deux affirmations du brief GEN-7 sont démenties par le code :

* **« Aucun test »** : il y a 10 tests, dont 4 échouent. Le ticket « tests sur le
  cœur métier » (GEN-12) doit partir de l'existant et réparer, pas créer de zéro.
* **« C 224 Ko, C++ 28 Ko … code natif en plus du Compose »** : ce n'est pas du
  code de l'app, c'est le toolset WiX committé. Il n'y a **pas** de code natif à
  documenter ; il y a un répertoire tiers à sortir du dépôt.

En revanche, le caractère « Windows-only malgré la promesse multiplateforme » et
l'absence de CI sont exacts.

---

*Rapport produit dans le cadre du chantier de remise au propre des dépôts GitHub (Jira GEN).*
