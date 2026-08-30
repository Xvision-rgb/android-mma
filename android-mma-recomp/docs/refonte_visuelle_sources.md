# Sources — refonte visuelle

Références derrière chaque principe de `brief_refonte_visuelle.md`. Recherche
effectuée en août 2026.

> **Réserve méthodologique** : le proxy réseau a bloqué l'accès au texte
> intégral de plusieurs articles de design (notamment le breakdown WHOOP de
> 925studios). Les synthèses ci-dessous proviennent des résumés de résultats
> de recherche, pas des articles complets. Les chiffres marqués ⚠️ sont à
> revérifier sur la source primaire avant d'être traités comme définitifs.
>
> Seule exception vérifiée en source primaire : la page de release
> `compose-material3` (§4), récupérée intégralement — les numéros de version
> et dates de cette section sont fiables.

## 1. Hiérarchie visuelle — la métrique unique

- ⚠️ [WHOOP Design Breakdown: Data-Dense UI That Feels Simple (925studios)](https://www.925studios.co/blog/whoop-design-breakdown)
  — score de récupération rendu à ~72pt équivalent, « lisible à bout de bras » ;
  la taille porte à elle seule la hiérarchie. Système couleur volontairement
  étroit : vert = prêt, rouge = charge/récupération basse, jaune = entre-deux,
  répété à l'identique sur tous les écrans — « l'utilisateur apprend le langage
  visuel une fois ». Divulgation progressive en 3 niveaux : score en un coup
  d'œil → vue tendance → graphes biométriques détaillés.
  **Texte intégral inaccessible (egress bloqué) — chiffres non revérifiés.**
- [Introducing the New Oura App Design (Oura)](https://ouraring.com/blog/new-oura-app-experience/)
  — l'onglet Today est « la source unique de vérité », conçu pour couper le
  bruit et concentrer sur **« one big thing »** : le seul score ou insight qui
  compte à cet instant. Scores Sleep/Readiness/Activity ancrés en haut.

## 2. Surcharge de dashboard

- [Dashboard Design Principles: The Definitive Guide (UXPin, 2026)](https://www.uxpin.com/studio/blog/dashboard-design-principles/)
  — l'erreur la plus commune est d'afficher toutes les métriques disponibles
  sur un seul écran ; l'utilisateur passe alors plus de temps à chercher
  l'insight qu'à agir. Remède : hiérarchie explicite par taille, couleur et
  espacement + drill-down pour le détail.
- [5 UI/UX Mistakes in Fitness Apps to Avoid](https://www.sportfitnessapps.com/blog/5-uiux-mistakes-in-fitness-apps-to-avoid)
  — un dashboard encombré est particulièrement pénalisant quand l'utilisateur
  jette un œil à l'écran **pendant** une séance ; cité parmi les causes de
  churn.
- [Fitness App Design: Onboarding, Tracking, and Retention](https://www.designyourway.net/blog/fitness-app-design/)
  — une hiérarchie forte pousse les données secondaires (totaux hebdo, badges,
  feed social) sous la ligne de flottaison ou dans un onglet dédié.

## 3. Ergonomie en salle — zone du pouce, cibles, mains moites

- [Mastering the Thumb Zone: Mobile UX & UI Design Guide (Parachute Design)](https://parachutedesign.ca/blog/thumb-zone-ux/)
  — carte en 3 zones : verte (bas-centre, atteinte sans effort → actions
  primaires), jaune (milieu/côtés, étirement), rouge (coins hauts, changement
  de prise nécessaire). ⚠️ Recherche Hoober citée : ~49 % des utilisateurs
  tiennent le téléphone d'une main, pouce comme pointeur principal.
- Synthèse des sources de logging (⚠️ résumés de recherche) : cible tactile
  minimale 48×48dp (Material) / 44×44pt (Apple HIG) — « des doigts moites après
  une série ont besoin de cibles plus grandes qu'une app bancaire n'en a
  jamais besoin ». Retour haptique à chaque interaction = confirmation même
  sans lecture de l'écran. Démarrage automatique du timer de repos après
  validation d'une série pour supprimer une action entre deux séries.

## 4. Socle technique Material 3 — **source primaire vérifiée**

- [Compose Material3 — releases (developer.android.com)](https://developer.android.com/jetpack/androidx/releases/compose-material3)
  — **1.4.0 stable le 26 août 2026**. Les composants Expressive sont sortis de
  l'expérimental plus tard, en 1.5.0-alpha :
  FAB + FAB Menu en 1.5.0-alpha19 (6 mai 2026), SplitButton en 1.5.0-alpha20
  (19 mai 2026), FloatingToolbar et ButtonGroup en 1.5.0-alpha22 (17 juin 2026).
  L'exigence `compileSdk 37` a été retirée en 1.5.0-alpha23 (1er juillet 2026).
- [Material 3 Expressive: New Components, Motion, Shapes, and More (Supercharge)](https://supercharge.design/blog/material-3-expressive)
  et [annonce Google](https://blog.google/products-and-platforms/platforms/android/material-3-expressive-android-wearos-launch/)
  — 15 composants nouveaux ou mis à jour ; bibliothèque de 35 formes ;
  système de mouvement à ressorts (spring-based) ; titres et actions clés
  agrandis et alourdis pour une hiérarchie plus lisible.
- [FAB menu — Material Design 3](https://m3.material.io/components/fab-menu)
  — spécification du composant qui remplace le pattern « FAB + colonne
  d'ExtendedFAB » actuellement bricolé dans `RootNav.kt`.

## 5. Vitesse de logging — la métrique produit

- [MacroFactor vs MyFitnessPal (MacroFactor, 2025)](https://macrofactor.com/macrofactor-vs-myfitnesspal-2025/)
  — ⚠️ comptage d'actions : 10 actions pour logger un aliment par recherche
  chez MacroFactor contre 15 chez MyFitnessPal ; **24 actions contre 36 sur
  4 parcours cumulés, soit 50 % d'écart**. Thèse défendue : « la vitesse de
  logging est un des meilleurs prédicteurs de la persévérance ».
  Chiffres publiés par MacroFactor sur sa propre app — biais commercial
  évident, à traiter comme un ordre de grandeur, pas comme une mesure neutre.
  La **méthode** (compter les actions par parcours) reste valable et c'est
  elle qu'on reprend comme critère d'acceptation.
- [Hevy vs Strong App comparison (Setgraph, 2026)](https://setgraph.app/ai-blog/hevy-vs-strong-app-comparison-2026)
  — Hevy est construit autour d'un workflow de session unique « pour passer
  moins de temps à taper et plus à soulever » ; Strong guide exercice par
  exercice depuis un template. Deux réponses différentes à la même contrainte :
  minimiser la friction pendant la séance.
