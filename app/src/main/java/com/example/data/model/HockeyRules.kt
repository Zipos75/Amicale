package com.example.data.model

data class RuleItem(
    val label: String,
    val description: String
)

data class CategoryRules(
    val category: String,
    val durationText: String,
    val subtitle: String,
    val items: List<RuleItem>
)

object HockeyRulesRepository {

    val commonRules = listOf(
        "Les coaches ne sont plus admis sur le terrain, pour laisser les enfants décider eux-mêmes.",
        "Un seul temps mort par coach, 30 secondes maximum, demandé uniquement quand le jeu est arrêté, et jamais dans les 5 dernières minutes."
    )

    val disclaimerText = "Ces documents sont republiés chaque saison, la version en ligne fait foi, et le règlement sportif prime sur toute fiche résumée."

    val categories: List<CategoryRules> = listOf(
        CategoryRules(
            category = "U7/U8",
            durationText = "2 × 20 minutes",
            subtitle = "Initiation — 3 contre 3",
            items = listOf(
                RuleItem("Joueurs", "3 contre 3 (minimum 2)"),
                RuleItem("Terrain", "⅛ de terrain"),
                RuleItem("Buts", "4 buts de 2 m, planches ou cônes"),
                RuleItem("Gardien", "Pas de gardien"),
                RuleItem("Engagement", "Au centre : début de match, mi-temps et après chaque but"),
                RuleItem("Sortie ligne de côté", "Remise en jeu à l'endroit de la sortie, pour l'équipe qui n'a pas touché la balle en dernier, adversaires à 3 m. Si la balle sort à hauteur de la zone, remise à 3 m derrière la ligne des 5 m"),
                RuleItem("Ligne de fond, défenseur en dernier", "La balle revient à l'attaque, à 3 m derrière la ligne des 5 m. Pas encore de long corner. Si c'était volontaire : coup franc pour l'attaque au même endroit"),
                RuleItem("Ligne de fond, attaquant en dernier", "Remise en jeu dans la zone pour la défense"),
                RuleItem("Coup franc", "Balle immobile, adversaires à 3 m"),
                RuleItem("Self-pass", "Autorisé"),
                RuleItem("Pied et revers", "« Gentleman's agreement » : les coaches peuvent convenir d'ignorer les touches involontaires"),
                RuleItem("Penalty corner", "Non"),
                RuleItem("Stroke", "Non"),
                RuleItem("Remplacements", "À n'importe quel moment")
            )
        ),
        CategoryRules(
            category = "U9",
            durationText = "2 × 25 minutes, 5 minutes de pause",
            subtitle = "Apprentissage — 6 contre 6",
            items = listOf(
                RuleItem("Joueurs", "6 contre 6 (minimum 4), maximum 12 sur la feuille de match"),
                RuleItem("Terrain", "¼ de terrain, avec une zone de 25 m qui remplace le cercle"),
                RuleItem("Buts", "Vrais buts, ou planches des deux côtés"),
                RuleItem("Gardien", "Obligatoire, équipement complet (sabots, guêtres, gants, casque) mais sans crosse. Il ne joue du corps que dans sa zone"),
                RuleItem("Engagement", "Au centre : début de match, mi-temps et après chaque but. Self-pass autorisé"),
                RuleItem("Sortie ligne de côté", "Remise en jeu à l'endroit de la sortie, pour l'équipe qui n'a pas touché la balle en dernier, adversaires à 3 m. Si la balle sort à hauteur de la zone, la remise se joue en dehors de celle-ci, à au moins 3 m"),
                RuleItem("Ligne de fond, défenseur en dernier", "Long corner pour l'attaque : remise en jeu sur la ligne du milieu, perpendiculairement au point de sortie, tout le monde à 3 m. Si le défenseur l'a envoyée dehors volontairement : coup franc pour l'attaque à 3 m de la zone"),
                RuleItem("Ligne de fond, attaquant en dernier", "Dégagement pour la défense, depuis sa zone, à la perpendiculaire du point de sortie, attaquants à 3 m"),
                RuleItem("Coup franc", "Balle immobile, adversaires à 3 m. Faute hors zone : à l'endroit de la faute, au moins à 3 m de la zone. Faute dans la zone : l'attaque se place à 3 m de la zone"),
                RuleItem("Self-pass", "Autorisé"),
                RuleItem("Back stick", "Interdit"),
                RuleItem("Balle haute", "Interdite, même en direction du but"),
                RuleItem("Jeu dangereux", "Toujours sanctionné : balle au-dessus du genou, mouvement dangereux, joueur accroupi ou à terre près de la balle"),
                RuleItem("But validé", "La balle doit franchir entièrement la ligne, après avoir été touchée dans la zone par un attaquant avec sa crosse, et passer à hauteur de la planche. Une déviation d'un défenseur au-dessus de la planche donne un long corner à l'attaque"),
                RuleItem("Pied et revers", "Sanctionnés lorsqu'ils sont volontaires"),
                RuleItem("Penalty corner", "Non"),
                RuleItem("Stroke", "Non"),
                RuleItem("Remplacements", "À n'importe quel moment"),
                RuleItem("Arbitrage", "En régionale : deux arbitres du club qui reçoit")
            )
        ),
        CategoryRules(
            category = "U10",
            durationText = "2 × 25 minutes, 5 minutes de pause",
            subtitle = "Transition grand terrain — 8 contre 8",
            items = listOf(
                RuleItem("Joueurs", "8 contre 8 (minimum 6)"),
                RuleItem("Terrain", "½ terrain"),
                RuleItem("Gardien", "Obligatoire, équipement complet"),
                RuleItem("Engagement", "Balle immobile au centre, adversaires à 5 m"),
                RuleItem("Sortie ligne de côté", "Coup franc à l'endroit de la sortie, en dehors de la zone, adversaires à 5 m"),
                RuleItem("Ligne de fond, défenseur en dernier", "Long corner pour l'attaque, sur la ligne du milieu, perpendiculairement au point de sortie, tout le monde à 5 m. Si c'était volontaire : penalty corner"),
                RuleItem("Ligne de fond, attaquant en dernier", "Dégagement pour la défense, 15 m maximum"),
                RuleItem("Coup franc", "Adversaires à 5 m. Dans le quart adverse, la balle doit parcourir 5 m ou être touchée par un autre joueur avant d'entrer dans le cercle"),
                RuleItem("Self-pass", "Autorisé, sauf sur penalty corner"),
                RuleItem("Back stick", "Interdit"),
                RuleItem("Balle haute", "Au-dessus du genou : jeu dangereux"),
                RuleItem("Jeu dangereux", "Mouvement dangereux, coup de crosse, pousser ou tenir un joueur"),
                RuleItem("Penalty corner", "Oui — 3 défenseurs + le gardien dans le but. Il remplace le shoot-out"),
                RuleItem("Stroke", "Non"),
                RuleItem("Remplacements", "À n'importe quel moment, sauf sur penalty corner")
            )
        ),
        CategoryRules(
            category = "U11/U12",
            durationText = "2 × 25 minutes, 5 minutes de pause",
            subtitle = "Perfectionnement — 8 contre 8",
            items = listOf(
                RuleItem("Joueurs", "8 contre 8 (minimum 6)"),
                RuleItem("Terrain", "½ terrain"),
                RuleItem("Gardien", "Obligatoire, équipement complet"),
                RuleItem("Engagement", "Balle immobile au centre, adversaires à 5 m"),
                RuleItem("Sortie ligne de côté", "Coup franc à l'endroit de la sortie, en dehors de la zone, adversaires à 5 m"),
                RuleItem("Ligne de fond, défenseur en dernier", "Long corner pour l'attaque, sur la ligne du milieu, perpendiculairement au point de sortie, tout le monde à 5 m. Si c'était volontaire : penalty corner"),
                RuleItem("Ligne de fond, attaquant en dernier", "Dégagement pour la défense, 15 m maximum"),
                RuleItem("Coup franc", "Adversaires à 5 m. Dans le quart adverse, la balle doit parcourir 5 m ou être touchée par un autre joueur avant d'entrer dans le cercle"),
                RuleItem("Self-pass", "Autorisé, sauf sur penalty corner"),
                RuleItem("Back stick", "Interdit"),
                RuleItem("Balle haute", "Au-dessus du genou : jeu dangereux"),
                RuleItem("Jeu dangereux", "Mouvement dangereux, coup de crosse, pousser ou tenir un joueur"),
                RuleItem("Penalty corner", "Oui — 4 défenseurs + le gardien dans le but"),
                RuleItem("Stroke", "Oui, pour une faute volontaire dans le cercle"),
                RuleItem("Remplacements", "À n'importe quel moment, sauf sur penalty corner")
            )
        )
    )
}
