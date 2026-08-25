-- =============================================================
-- foods — bibliothèque d'aliments préchargée avec macros / 100g
-- Table de référence partagée (pas de user_id) : lecture publique,
-- écriture réservée au dashboard / service role.
-- À exécuter dans Supabase > SQL Editor, après schema.sql.
-- =============================================================

create extension if not exists pg_trgm;

create table if not exists public.foods (
  id uuid primary key default gen_random_uuid(),
  nom text not null unique,
  categorie text not null default 'autre' check (categorie in (
    'proteine', 'glucide', 'lipide', 'fruit_legume', 'produit_laitier', 'autre'
  )),
  kcal_100g numeric(6, 1) not null,
  proteines_100g numeric(5, 1) not null default 0,
  glucides_100g numeric(5, 1) not null default 0,
  lipides_100g numeric(5, 1) not null default 0,
  created_at timestamptz not null default now()
);

alter table public.foods enable row level security;

-- Lecture ouverte à tout utilisateur authentifié : c'est une bibliothèque
-- de référence partagée, pas une donnée personnelle.
create policy "foods_select_all" on public.foods
  for select to authenticated using (true);

create index if not exists foods_nom_idx on public.foods using gin (nom gin_trgm_ops);

-- =============================================================
-- Seed — une trentaine d'aliments courants (valeurs / 100g, moyennes
-- usuelles). À ajuster/compléter librement depuis le dashboard.
-- =============================================================
insert into public.foods (nom, categorie, kcal_100g, proteines_100g, glucides_100g, lipides_100g) values
  ('Blanc de poulet cru', 'proteine', 165, 31.0, 0.0, 3.6),
  ('Steak haché 5% MG', 'proteine', 143, 21.0, 0.0, 5.0),
  ('Œuf entier', 'proteine', 155, 13.0, 1.1, 11.0),
  ('Blanc d''œuf', 'proteine', 52, 11.0, 0.7, 0.2),
  ('Saumon cru', 'proteine', 208, 20.0, 0.0, 13.0),
  ('Thon au naturel (boîte)', 'proteine', 116, 26.0, 0.0, 1.0),
  ('Whey protéine (poudre)', 'proteine', 380, 80.0, 8.0, 6.0),
  ('Fromage blanc 0%', 'produit_laitier', 45, 8.0, 4.0, 0.2),
  ('Skyr nature', 'produit_laitier', 63, 11.0, 4.0, 0.2),
  ('Yaourt grec nature', 'produit_laitier', 97, 9.0, 4.0, 5.0),
  ('Lait demi-écrémé', 'produit_laitier', 46, 3.3, 4.8, 1.6),
  ('Riz blanc cru', 'glucide', 349, 7.0, 78.0, 0.6),
  ('Riz basmati cuit', 'glucide', 130, 2.7, 28.0, 0.3),
  ('Pâtes crues', 'glucide', 353, 12.0, 71.0, 1.5),
  ('Pâtes cuites', 'glucide', 158, 5.8, 31.0, 0.9),
  ('Flocons d''avoine', 'glucide', 372, 13.5, 60.0, 7.0),
  ('Pain complet', 'glucide', 247, 9.0, 41.0, 3.4),
  ('Pomme de terre cuite', 'glucide', 87, 2.0, 20.0, 0.1),
  ('Patate douce cuite', 'glucide', 90, 2.0, 21.0, 0.2),
  ('Banane', 'fruit_legume', 89, 1.1, 23.0, 0.3),
  ('Pomme', 'fruit_legume', 52, 0.3, 14.0, 0.2),
  ('Brocoli cuit', 'fruit_legume', 35, 2.4, 7.0, 0.4),
  ('Haricots verts cuits', 'fruit_legume', 31, 1.8, 7.0, 0.1),
  ('Épinards crus', 'fruit_legume', 23, 2.9, 3.6, 0.4),
  ('Avocat', 'lipide', 160, 2.0, 8.5, 15.0),
  ('Huile d''olive', 'lipide', 884, 0.0, 0.0, 100.0),
  ('Amandes', 'lipide', 579, 21.0, 22.0, 50.0),
  ('Beurre de cacahuète', 'lipide', 588, 25.0, 20.0, 50.0),
  ('Noix', 'lipide', 654, 15.0, 14.0, 65.0),
  ('Chocolat noir 70%', 'autre', 546, 7.8, 46.0, 31.0)
on conflict (nom) do nothing;
