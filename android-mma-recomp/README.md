# Recomp & MMA — app Android

Portage Android (Kotlin + Jetpack Compose) de l'app de suivi entraînement /
nutrition / poids en recomposition corporelle et préparation MMA. Même
backend, même schéma Supabase, même logique métier que la version iOS
(`ios-mma-recomp/`, conservée pour référence) — seule la couche UI change.

## Pourquoi Android change la donne (par rapport à iOS)

- **Android Studio tourne nativement sur Windows/Linux** — pas besoin de Mac,
  contrairement à Xcode.
- **Aucun compte payant récurrent requis** pour installer l'app sur ton
  propre téléphone : gratuit, illimité dans le temps.
- Pour des mises à jour "propres" façon store (optionnel, voir §4), le compte
  Google Play Console coûte **25$, une seule fois** (à vie), contre 99$/an
  pour Apple.

## 1. Setup Supabase

Identique à la version iOS : exécute `../ios-mma-recomp/supabase/schema.sql`
dans le SQL Editor de ton projet Supabase (schéma et RLS déjà écrits, rien à
changer côté base). Récupère ensuite l'URL du projet et la clé `anon` dans
**Project Settings > API**.

## 2. Setup du projet Android

1. Installe [Android Studio](https://developer.android.com/studio) (Windows
   ou Linux, gratuit).
2. Ouvre le dossier `android-mma-recomp/` dans Android Studio — il détecte
   `settings.gradle.kts` et propose de synchroniser Gradle automatiquement.
3. Configure tes identifiants Supabase :
   ```bash
   cp app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt.example \
      app/src/main/java/com/example/mmarecomp/SupabaseConfig.kt
   ```
   Édite `SupabaseConfig.kt` avec ton URL de projet et ta clé `anon`. Ce
   fichier est gitignored — ne commit jamais de vraies clés.
4. Avant le premier build, remplace `com.example.mmarecomp` par un
   identifiant que tu possèdes dans `app/build.gradle.kts`
   (`applicationId`) — nécessaire seulement si tu comptes un jour publier
   sur le Play Store, sinon la valeur par défaut suffit pour un usage perso.

> Le SDK `supabase-kt` évoluant vite, si un appel (`postgrest.from().select()...`)
> ne compile pas tel quel avec la version résolue, ajuste la signature : la
> logique métier (repositories/viewmodels/écrans) n'a pas besoin de changer.

## 3. Installer l'app sur ton téléphone (gratuit, sans Mac)

1. Sur ton Android : **Réglages > À propos du téléphone**, tape 7 fois sur
   "Numéro de build" pour activer le mode développeur, puis **Réglages >
   Options pour développeurs > Débogage USB** (activer).
2. Branche le téléphone en USB (ou configure le débogage Wi-Fi si tu
   préfères sans câble).
3. Dans Android Studio, sélectionne ton appareil dans la liste en haut, puis
   clique sur ▶️ **Run**. L'app se compile et s'installe directement — pas de
   limite de 7 jours, pas de compte à créer.

## 4. Mises à jour — deux façons de faire

**Option simple (gratuite, recommandée pour démarrer) :** à chaque fois que
le code change, rebranche le téléphone et reclique sur Run dans Android
Studio. Ça écrase l'ancienne version par la nouvelle en quelques secondes.

**Option "store" (25$ une fois, plus confortable au quotidien) :** si tu veux
recevoir tes mises à jour comme une vraie app (notification "Mise à jour
disponible" dans le Play Store, sans rebrancher de câble) :
1. Crée un compte [Google Play Console](https://play.google.com/console)
   (25$, paiement unique, à vie).
2. Crée l'app et une piste de **test interne** (gratuite, illimitée,
   réservée aux emails que tu invites — toi-même).
3. Génère un app bundle signé (`./gradlew bundleRelease` avec un keystore, ou
   via Android Studio : **Build > Generate Signed App Bundle**) et
   uploade-le sur la piste de test interne.
4. Installe l'app **Google Play Store** sur ton téléphone, accepte
   l'invitation de testeur, et l'app apparaît comme installable/mettable à
   jour normalement.

Si tu veux automatiser cette étape 4 avec une pipeline CI (comme la
TestFlight qu'on avait commencée côté iOS), c'est possible et beaucoup plus
simple qu'avec Apple — dis-le-moi et je la mets en place.

## 5. Structure du projet

```
android-mma-recomp/
├── app/src/main/java/com/example/mmarecomp/
│   ├── MainActivity.kt, MMARecompApp.kt
│   ├── model/        # data classes @Serializable ↔ tables Postgres
│   ├── data/          # repositories Supabase (1 par table)
│   ├── util/           # moyenne mobile, détection plateau, calcul cible, parsing WOD
│   ├── viewmodel/       # ViewModels (state Compose), 1 par écran
│   └── ui/
│       ├── theme/        # couleurs/typo (même identité que la maquette)
│       ├── nav/            # navigation par onglets
│       ├── auth/, dashboard/, workout/, nutrition/, weighin/, progress/, settings/
│       └── components/    # graphique de tendance, barres cible/réel, bandeau doux
└── app/src/main/res/       # icône adaptative, thème, strings
```

Mêmes règles UX que la version iOS : `WeightTrendChart` n'affiche que la
moyenne mobile 7 jours (jamais le poids brut), `PlateauDetector` transforme
un plateau + performances en hausse en message positif, et
`TargetVsActualBar` ne culpabilise jamais sur un déficit calorique.

## 6. Limitations connues de ce scaffold

- Les ViewModels d'écran sont instanciés directement (pas via
  `ViewModelProvider.Factory`) pour rester simple : une rotation d'écran
  réinitialise le formulaire en cours. À corriger si ça gêne à l'usage.
- Pas de compilation possible dans cet environnement (pas d'Android SDK/Gradle
  ici) — le code est écrit avec soin mais pas vérifié par le compilateur.
