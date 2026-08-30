# Correction complète de l’application — conception

## Objectif

Fiabiliser l’application Android de recomposition et de préparation MMA sur
trois axes successifs : exactitude métier, qualité de l’expérience française,
puis robustesse de production. Les corrections partent du commit
`7de2f911`, qui contient déjà les correctifs d’intégrité de la PR 102.

Le périmètre couvre les défauts prouvés par lecture du code et tests de
régression. Une différence avec une application concurrente n’est pas, à elle
seule, un bug.

## Références produit

Le benchmark mondial consulté le 30 août 2026 retient dix références :
Strong, Hevy, Fitbod, RP Hypertrophy, MacroFactor, MyFitnessPal, Cronometer,
WHOOP, Garmin Connect et UFC GYM+.

L’application conserve sa proposition propre — force relative, nutrition et
MMA dans un même outil — et reprend seulement les pratiques vérifiables :

- saisie de séries rapide et explicite de Strong/Hevy ;
- recommandation éditable et expliquée de Fitbod/RP Hypertrophy ;
- recalibrage hebdomadaire non culpabilisant de MacroFactor ;
- provenance et niveau de confiance des données de Cronometer ;
- synthèse quotidienne contextualisée de WHOOP/Garmin ;
- primauté du coach humain pour les décisions de combat de UFC GYM+.

## Principes non négociables

1. Une recommandation ne doit jamais reposer sur une donnée inversée,
   périmée, incomplète ou hors fenêtre sans le signaler.
2. Les métriques de santé sont des repères contextuels, jamais des diagnostics
   ni des prédictions individuelles de blessure.
3. Aucune recommandation autonome de déshydratation, coupe rapide, reprise
   après commotion, entraînement malgré douleur aiguë ou remplacement d’un
   professionnel.
4. Le poids affiché comme tendance reste une moyenne mobile sur sept jours.
5. Les erreurs ne sont jamais absorbées : elles sont journalisées ou
   transformées en état opérateur explicite, puis propagées de façon utile.
6. Toute correction comportementale commence par un test qui échoue pour la
   raison attendue.
7. Les secrets ne sont pas suivis. La clé publique Supabase peut être intégrée
   à l’APK, mais la sécurité des données repose sur des politiques RLS testées.
8. Le dépôt doit fournir un bootstrap reproductible. `make lint test` doit
   échouer réellement si le lint ou les tests échouent.

## Lot 1 — Intégrité métier et sécurité scientifique

### Charge MMA

La saisie historique reste `1 = très difficile` et `5 = facile`. Une fonction
unique convertit ce ressenti en intensité croissante avant tout calcul :
`intensite = (6 - ressenti) * 2`, soit 10, 8, 6, 4, 2. Elle rejette les valeurs
hors de 1 à 5.

`TrainingLoad` utilise cette intensité pour la charge session-RPE.
`InterferenceChecker` utilise la même conversion ; aucune constante impossible
à atteindre ne subsiste.

### Calories et macronutriments

`NutritionTargetCalculator.targetFor` respecte d’abord `baseCalories`.
Protéines et lipides sont calculés avec leurs planchers ; les glucides
absorbent le solde calorique, puis les planchers peuvent relever explicitement
la cible. Le mode Prise de masse/Recomposition/Coupe ne doit pas être annulé
par une seconde formule.

Le recalibrage adaptatif :

- borne repas et pesées à la même fenêtre ;
- mesure la complétude sur cette fenêtre ;
- refuse le calcul sous le seuil existant de 67 % ;
- moyenne les apports sur les jours couverts et affiche leur couverture ;
- ne présente jamais l’estimation comme une mesure métabolique clinique.

Les entrées poids, masse grasse, calories et protéines sont validées dans le
domaine, pas seulement dans Compose.

### Charge, récupération et progression

- Les repères de volume hebdomadaire ne reçoivent que sept jours.
- L’ACWR est renommé en indicateur de variation de charge et ne déclenche
  aucune prescription avant un historique chronique minimal explicite.
- Une HRV historique ne remplace jamais silencieusement la HRV du jour.
- Une série avec RIR 0 est signalée même si la moyenne est dans la cible.
- Le signe de calibration RIR est aligné sur son utilisation.
- APRE n’impose jamais une hausse après une sous-performance et valide
  l’incrément.
- La détection de plateau exige une fenêtre temporelle suffisante et une
  tendance de performance réellement calculée.

### Phase, import et export

- La phase du profil est chargée avant de résoudre le programme courant.
- Une virgule décimale dans un exercice importé n’est pas traitée comme un
  séparateur d’exercices.
- Un jour importé avec exercices ne devient pas un jour de repos.
- L’aperçu signale les lignes ignorées au lieu de les perdre silencieusement.
- L’export CSV respecte RFC 4180, neutralise les formules tableur et inclut les
  séries. Les autres domaines disposent au minimum d’un export documenté ou
  d’une mention claire de leur absence.

### Messages de sécurité

Les textes sur ACWR, disponibilité énergétique, protéines, force de préhension
et calcul calorique décrivent limites et incertitudes. Une douleur aiguë,
maladie, commotion suspectée ou instruction médicale provoque une recommandation
d’arrêt et d’orientation vers le professionnel compétent.

## Lot 2 — Français, UX et accessibilité

Toutes les chaînes visibles sont déplacées vers `res/values/strings.xml` ou
`plurals`. Les termes techniques utiles restent disponibles : MMA, RPE, RIR,
WOD, AMRAP, HIIT, HRV, ACWR, APRE et 1RM. Les anglicismes non nécessaires
deviennent « enregistrer », « séance », « tableau de bord », « prise de
masse », « répétitions » et « programme ».

Les unités utilisent la locale française et une espace insécable : `82,5 kg`,
`30 g`, `8 à 10 %`, `90 s`. Les pluriels ne contiennent plus de formes
« exercice(s) ».

Les actions TalkBack ont un libellé unique et contextualisé. Les icônes
décoratives ne sont pas annoncées. Les curseurs, séries, graphiques, dates et
compte à rebours exposent une sémantique complète. Chaque cible tactile mesure
au moins 48 dp et le texte agrandi ne tronque pas les actions essentielles.

Une bannière d’erreur sait si l’échec vient d’un chargement, d’une sauvegarde
ou d’une suppression. « Réessayer » répète exactement l’opération échouée ;
sinon le bouton n’est pas affiché. Le texte précise si les saisies sont
conservées.

## Lot 3 — Production, Supabase et hors-ligne

### Build et qualité

Le dépôt inclut un Gradle Wrapper compatible avec la version AGP retenue, un
`SupabaseConfig.kt` généré depuis propriétés d’environnement au build, un
`.env.example` sans secret et un Makefile strict. Aucun `|| true` ne masque un
échec. Une CI exécute compilation, lint, tests JVM, couverture et tests
Compose disponibles.

### Données et sécurité

Le schéma complet nécessaire à l’application est versionné localement. Toutes
les tables exposées ont RLS, contraintes, index justifiés et politiques
SELECT/INSERT/UPDATE/DELETE testées avec deux utilisateurs. Les scripts sont
transactionnels et rejouables selon une procédure documentée.

Les mutations vérifient qu’une ligne a réellement été affectée. Les opérations
multi-écritures qui doivent être atomiques utilisent une fonction RPC privée
et testée, ou un mécanisme de compensation explicite. Les suppressions avec
annulation ne doivent ni écraser une nouvelle valeur ni dupliquer une ligne.

La sauvegarde Android exclut les sessions et données sensibles. La
déconnexion efface l’état d’authentification. Le mot de passe est vidé après
chaque tentative terminée.

### Réseau et cycle de vie

Les clients HTTP définissent des délais de connexion, lecture et requête.
Seules les erreurs transitoires sont retentées, avec backoff exponentiel et
limite. `CancellationException` est toujours relancée.

Les ViewModels sont créés par le mécanisme Android de cycle de vie avec
injection de dépendances minimale. Les requêtes dépendant d’une date, phase ou
fenêtre annulent la précédente ; une réponse obsolète ne peut pas remplacer la
plus récente.

### Hors-ligne

Le plan du jour, l’historique récent et les brouillons de saisie sont lisibles
localement. Les écritures critiques utilisent une outbox idempotente,
persistante et visible, drainée par WorkManager sous contrainte réseau avec
backoff exponentiel. L’interface distingue « local », « synchronisation en
cours » et « échec à corriger ». Aucun conflit n’est résolu par écrasement
silencieux.

## Architecture de test

- Tests JVM purs pour chaque calcul et parseur.
- Fakes de repositories pour erreurs, latence, annulation et concurrence.
- Tests Compose pour navigation, reprise d’erreur, pluriels, texte agrandi et
  sémantique TalkBack.
- Tests Supabase avec deux identités pour chaque politique RLS et chaque
  conflit logique.
- Tests WorkManager pour outbox, idempotence et backoff.
- Tests CSV adverses : virgules, guillemets, CR/LF et préfixes de formule.

La couverture cible est de 85 % ou plus sur la logique métier et les
ViewModels. Les éléments graphiques non déterministes sont validés par
sémantique et comportement plutôt que par captures fragiles.

## Séquencement et critères de sortie

Chaque lot produit un commit Conventional Commits autonome et met à jour la
PR. Le lot suivant ne commence qu’après tests du lot courant.

La livraison est terminée lorsque :

- les anomalies critiques de l’audit ont un test de régression ;
- `make lint test` réussit sans tolérance d’erreur ;
- aucune donnée factice ni opération silencieuse ne subsiste ;
- les parcours principaux fonctionnent avec TalkBack et texte agrandi ;
- le schéma et les politiques RLS sont reproductibles et testés ;
- le mode hors-ligne ne perd aucune saisie ;
- README, `.env.example`, commandes d’installation, lancement et tests sont à
  jour ;
- la PR contient les checklists tests, lint, documentation et sécurité.

## Hors périmètre

Le projet ne devient pas un dispositif médical, ne prescrit pas de coupe de
poids de compétition et n’ajoute pas de coaching vidéo par IA. Les réseaux
sociaux, abonnements, wearables propriétaires et messagerie coach ne sont pas
nécessaires pour corriger l’application existante.
