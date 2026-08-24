package com.example.mmarecomp.data

import kotlinx.serialization.Serializable

/** Aliment courant avec ses valeurs approximatives pour 100g. */
@Serializable
data class FoodItem(
    val name: String,
    val caloriesPer100g: Int,
    val proteinPer100gG: Double,
    val carbsPer100gG: Double,
    val fatPer100gG: Double,
)

/**
 * Base locale d'aliments courants — sert uniquement d'aide au calcul manuel
 * dans Log repas quand on ne connaît pas les valeurs d'un aliment. Valeurs
 * approximatives (moyennes usuelles), aucune donnée externe ni réseau.
 */
object FoodDatabase {
    val items: List<FoodItem> = listOf(
        // Féculents
        FoodItem("Riz blanc cuit", 130, 2.7, 28.0, 0.3),
        FoodItem("Riz complet cuit", 123, 2.6, 25.0, 1.0),
        FoodItem("Pâtes cuites", 131, 5.0, 25.0, 1.1),
        FoodItem("Pain blanc", 265, 9.0, 49.0, 3.2),
        FoodItem("Pain complet", 247, 13.0, 41.0, 3.4),
        FoodItem("Pomme de terre cuite", 87, 1.9, 20.0, 0.1),
        FoodItem("Patate douce cuite", 90, 2.0, 21.0, 0.1),
        FoodItem("Quinoa cuit", 120, 4.4, 21.0, 1.9),
        FoodItem("Avoine (flocons)", 389, 17.0, 66.0, 7.0),
        FoodItem("Semoule cuite", 112, 3.8, 23.0, 0.2),

        // Viandes, poissons, œufs
        FoodItem("Poulet (blanc, cuit)", 165, 31.0, 0.0, 3.6),
        FoodItem("Bœuf haché 5%", 137, 21.0, 0.0, 5.0),
        FoodItem("Bœuf haché 15%", 215, 19.0, 0.0, 15.0),
        FoodItem("Dinde (blanc, cuite)", 135, 29.0, 0.0, 1.0),
        FoodItem("Saumon cuit", 208, 20.0, 0.0, 13.0),
        FoodItem("Thon au naturel", 116, 26.0, 0.0, 1.0),
        FoodItem("Cabillaud cuit", 105, 23.0, 0.0, 1.0),
        FoodItem("Œuf entier", 155, 13.0, 1.1, 11.0),
        FoodItem("Blanc d'œuf", 52, 11.0, 0.7, 0.2),
        FoodItem("Jambon blanc", 107, 18.0, 1.0, 3.0),
        FoodItem("Porc (filet, cuit)", 143, 27.0, 0.0, 3.5),

        // Légumineuses
        FoodItem("Lentilles cuites", 116, 9.0, 20.0, 0.4),
        FoodItem("Pois chiches cuits", 164, 8.9, 27.0, 2.6),
        FoodItem("Haricots rouges cuits", 127, 8.7, 23.0, 0.5),
        FoodItem("Tofu", 76, 8.0, 1.9, 4.8),

        // Produits laitiers
        FoodItem("Fromage blanc 0%", 45, 8.0, 4.0, 0.2),
        FoodItem("Yaourt nature", 61, 3.5, 4.7, 3.3),
        FoodItem("Skyr", 63, 11.0, 4.0, 0.2),
        FoodItem("Lait demi-écrémé", 46, 3.3, 4.8, 1.6),
        FoodItem("Emmental", 380, 28.0, 0.5, 30.0),
        FoodItem("Mozzarella", 280, 22.0, 2.0, 21.0),

        // Fruits
        FoodItem("Banane", 89, 1.1, 23.0, 0.3),
        FoodItem("Pomme", 52, 0.3, 14.0, 0.2),
        FoodItem("Orange", 47, 0.9, 12.0, 0.1),
        FoodItem("Fraises", 32, 0.7, 7.7, 0.3),
        FoodItem("Raisin", 69, 0.7, 18.0, 0.2),
        FoodItem("Avocat", 160, 2.0, 8.5, 15.0),

        // Légumes
        FoodItem("Brocoli cuit", 35, 2.4, 7.0, 0.4),
        FoodItem("Carotte crue", 41, 0.9, 10.0, 0.2),
        FoodItem("Tomate", 18, 0.9, 3.9, 0.2),
        FoodItem("Courgette cuite", 17, 1.2, 3.1, 0.3),
        FoodItem("Épinards cuits", 23, 3.0, 3.6, 0.3),
        FoodItem("Salade verte", 15, 1.4, 2.9, 0.2),

        // Matières grasses et oléagineux
        FoodItem("Huile d'olive", 884, 0.0, 0.0, 100.0),
        FoodItem("Beurre", 717, 0.9, 0.1, 81.0),
        FoodItem("Amandes", 579, 21.0, 22.0, 50.0),
        FoodItem("Beurre de cacahuète", 588, 25.0, 20.0, 50.0),
        FoodItem("Noix", 654, 15.0, 14.0, 65.0),

        // Autres
        FoodItem("Whey (poudre)", 380, 75.0, 8.0, 5.0),
        FoodItem("Miel", 304, 0.3, 82.0, 0.0),
        FoodItem("Chocolat noir 70%", 598, 7.8, 46.0, 43.0),
    )
}
