package com.example.chess

enum class PuzzleCategory(val displayName: String, val subtitle: String) {
    TACTICS("Tactics", "Calculating forcing checks, sacrifices & combinations"),
    STRATEGY("Strategy", "Positional mastery, outposts, pawn breaks & endgames")
}

enum class PuzzleGroup(val category: PuzzleCategory, val displayName: String, val description: String) {
    CHECKMATE_BRILLIANCIES(
        PuzzleCategory.TACTICS,
        "Checkmate Brilliancies",
        "Immortal queen sacrifices and forcing mating nets from classic games."
    ),
    SACRIFICES_AND_DEFLECTIONS(
        PuzzleCategory.TACTICS,
        "Sacrifices, Pins & Deflections",
        "Decisive tactical blows exploiting overloaded defenders and weak back ranks."
    ),
    POSITIONAL_OUTPOSTS(
        PuzzleCategory.STRATEGY,
        "Dominant Outposts & Space",
        "Legendary grandmaster maneuvers establishing eternal piece domination."
    ),
    PAWN_STRUCTURE_PLANS(
        PuzzleCategory.STRATEGY,
        "Pawn Breaks & Blockades",
        "Classic minority attacks, structural binds, and good-vs-bad piece imbalances."
    ),
    ENDGAME_TECHNIQUE(
        PuzzleCategory.STRATEGY,
        "Classic Endgame Strategy",
        "Essential theoretical masterpieces every tournament player must master."
    )
}

data class HistoricalPuzzle(
    val id: String,
    val title: String,
    val matchInfo: String, // e.g., "Morphy vs Duke Karl & Count Isouard · Paris, 1858"
    val group: PuzzleGroup,
    val difficulty: String, // Club, Master, Grandmaster
    val fen: String,
    /**
     * Alternating player solution moves and forced opponent replies in UCI format:
     * index 0 = player's 1st move, index 1 = opponent reply (if any), index 2 = player's 2nd move, etc.
     */
    val solutionLineUci: List<String>,
    val objectivePrompt: String,
    val hintText: String,
    val explanation: String
) {
    val sideToMove: PieceColor
        get() = ChessBoardState.fromFen(fen).sideToMove
}

object HistoricalPuzzleLibrary {

    val allPuzzles: List<HistoricalPuzzle> = listOf(
        // ==================== TACTICS: CHECKMATE BRILLIANCIES ====================
        HistoricalPuzzle(
            id = "tac_opera_game",
            title = "The Opera Game Finale",
            matchInfo = "Paul Morphy vs Duke Karl & Count Isouard · Paris, 1858",
            group = PuzzleGroup.CHECKMATE_BRILLIANCIES,
            difficulty = "Club",
            fen = "4kb1r/p2n1ppp/4q3/4p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 1 16",
            solutionLineUci = listOf("b3b8", "d7b8", "d1d8"),
            objectivePrompt = "White to move · Deliver Morphy's immortal 2-move checkmate.",
            hintText = "Deflect the d7 knight away from defending the d8 square.",
            explanation = "16. Qb8+!! forces 16...Nxb8, clearing the d-file for 17. Rd8# — the most famous Queen sacrifice in chess history, showing the power of rapid development and pins."
        ),
        HistoricalPuzzle(
            id = "tac_immortal_game",
            title = "The Immortal Game Mate",
            matchInfo = "Adolf Anderssen vs Lionel Kieseritzky · London, 1851",
            group = PuzzleGroup.CHECKMATE_BRILLIANCIES,
            difficulty = "Master",
            fen = "r1bk2nr/p2p1pNp/n2B4/1p1NP2P/6P1/3P1Q2/P1P1K3/q5b1 w - - 1 22",
            solutionLineUci = listOf("f3f6", "g8f6", "d6e7"),
            objectivePrompt = "White to move · Finish the Immortal Game with a Queen sacrifice.",
            hintText = "Lure Black's g8 knight away from defending e7.",
            explanation = "Having already sacrificed both rooks and a bishop, Anderssen crowned the Immortal Game with 22. Qf6+!! Nxf6 23. Be7#, mating with just three minor pieces!"
        ),
        HistoricalPuzzle(
            id = "tac_philidor_smothered",
            title = "Philidor's Legacy",
            matchInfo = "François-André Danican Philidor · Classical Pattern, 1777",
            group = PuzzleGroup.CHECKMATE_BRILLIANCIES,
            difficulty = "Club",
            fen = "5r1k/6pp/7N/8/8/1Q6/6PP/6K1 w - - 0 1",
            solutionLineUci = listOf("b3g8", "f8g8", "h6f7"),
            objectivePrompt = "White to move · Execute the classic smothered checkmate.",
            hintText = "Force Black's own rook onto the g8 escape square.",
            explanation = "1. Qg8+!! Rxg8 2. Nf7# smothers the cornered Black king behind its own pieces. This exact motif still decides master tournament games centuries later."
        ),
        HistoricalPuzzle(
            id = "tac_anastasia_mate",
            title = "Anastasia's Mating Net",
            matchInfo = "Classical Romantic Motif · Wilhelm Heinse, 1803",
            group = PuzzleGroup.CHECKMATE_BRILLIANCIES,
            difficulty = "Master",
            fen = "5rk1/1p3ppp/8/3N4/8/4R2Q/5PPP/6K1 w - - 0 1",
            solutionLineUci = listOf("d5e7", "g8h8", "h3h7", "h8h7", "e3h3"),
            objectivePrompt = "White to move · Execute the classic 3-move Anastasia's Mate.",
            hintText = "First drive the Black king into the corner with a knight check on e7.",
            explanation = "1. Ne7+! Kh8 2. Qxh7+!! Kxh7 3. Rh3# — the iconic Anastasia's Mate! Black's own g7 pawn blocks the king while White's e7 knight seals g8 and g6."
        ),

        // ==================== TACTICS: SACRIFICES, PINS & DEFLECTIONS ====================
        HistoricalPuzzle(
            id = "tac_game_of_century",
            title = "Game of the Century",
            matchInfo = "Donald Byrne vs Bobby Fischer (age 13) · New York, 1956",
            group = PuzzleGroup.SACRIFICES_AND_DEFLECTIONS,
            difficulty = "Grandmaster",
            fen = "r3r1k1/pp3pbp/1qp3p1/2B5/2BP2b1/Q1n2N2/P4PPP/3R1K1R b - - 0 17",
            solutionLineUci = listOf("g4e6"),
            objectivePrompt = "Black to move · Find 13-year-old Bobby Fischer's immortal Queen sacrifice.",
            hintText = "Ignore the attack on your Queen and strike at White's c4 bishop with tempo.",
            explanation = "17...Be6!! leaves the Black queen en prise on b6! If White captures 18. Bxb6, Black unleashes 18...Bxc4+ 19. Kg1 Ne2+ 20. Kf1 Nxd4+ and wins overwhelming material via a devastating windmill."
        ),
        HistoricalPuzzle(
            id = "tac_legals_mate",
            title = "Légal's Trap Sacrifice",
            matchInfo = "Sire de Légal vs Saint Brie · Café de la Régence, Paris, 1750",
            group = PuzzleGroup.SACRIFICES_AND_DEFLECTIONS,
            difficulty = "Club",
            fen = "r2qkbnr/ppp2ppp/2np4/4p3/2B1P1b1/2N2N2/PPPP1PPP/R1BQK2R w KQkq - 0 5",
            solutionLineUci = listOf("f3e5", "g4d1", "c4f7", "e8e7", "c3d5"),
            objectivePrompt = "White to move · Break the pin on f3 and punish Black's greed.",
            hintText = "Take the undefended e5 pawn — if Black takes your Queen, mate follows in two!",
            explanation = "5. Nxe5!! ignores the pin on the White queen. After 5...Bxd1??, White mates with 6. Bxf7+ Ke7 7. Nd5#! Even if Black declines with 5...Nxe5, White wins a clean pawn."
        ),
        HistoricalPuzzle(
            id = "tac_capablanca_deflection",
            title = "Moscow Back-Rank Deflection",
            matchInfo = "Ossip Bernstein vs José Raúl Capablanca · Moscow, 1914",
            group = PuzzleGroup.SACRIFICES_AND_DEFLECTIONS,
            difficulty = "Master",
            fen = "3r2k1/5ppp/8/p7/P7/2R1Q3/2q2PPP/6K1 b - - 0 29",
            solutionLineUci = listOf("d8d1"),
            objectivePrompt = "Black to move · Exploit White's unguarded back rank decisively.",
            hintText = "Force White's Queen back to e1 before picking off the Rook.",
            explanation = "29...Rd1+! forces 30. Qe1, after which 30...Rxe1# wins cleanly — a textbook illustration of back-rank deflection when the king has no luft."
        ),

        // ==================== STRATEGY: DOMINANT OUTPOSTS & SPACE ====================
        HistoricalPuzzle(
            id = "str_kasparov_octopus",
            title = "The Octopus Knight",
            matchInfo = "Anatoly Karpov vs Garry Kasparov · World Championship Game 16, 1985",
            group = PuzzleGroup.POSITIONAL_OUTPOSTS,
            difficulty = "Grandmaster",
            fen = "r2q1rk1/3nbppp/p2pbn2/1p2p3/4P3/1NN1BP2/PPP1Q1PP/2KR1B1R b - - 0 16",
            solutionLineUci = listOf("d7b6"),
            objectivePrompt = "Black to move · Begin Kasparov's famous queenside outpost maneuver toward c4 and d3.",
            hintText = "Reroute the d7 knight to control c4 and open the d-file for your pieces.",
            explanation = "Kasparov's queenside knight maneuver (...Nb6-c4 and ...Nd3 in the Game 16 Sicilian) paralyzed White's army by planting an unassailable 'Octopus Knight' deep inside enemy territory."
        ),
        HistoricalPuzzle(
            id = "str_karpov_afile_lock",
            title = "Karpov's Eternal a-File Bind",
            matchInfo = "Anatoly Karpov vs Wolfgang Unzicker · Nice Olympiad, 1974",
            group = PuzzleGroup.POSITIONAL_OUTPOSTS,
            difficulty = "Grandmaster",
            fen = "2r2rk1/1b1nbppp/p2p1n2/1ppPp3/4P3/2P2NP1/PPB1NPBP/R2Q1RK1 w - - 0 24",
            solutionLineUci = listOf("a2a4"),
            objectivePrompt = "White to move · Seize permanent queenside space and challenge Black's flank.",
            hintText = "Strike immediately on the a-file to fracture Black's b5-a6 structure.",
            explanation = "Just as Karpov strangled Unzicker in Nice 1974 (creating an unbreakable bind along the a-file), opening the queenside with a4 undermines Black's cramped pieces and gives White lasting structural pressure."
        ),
        HistoricalPuzzle(
            id = "str_botvinnik_outpost",
            title = "The Classical d5 Outpost",
            matchInfo = "Mikhail Botvinnik · Central Domination Strategy",
            group = PuzzleGroup.POSITIONAL_OUTPOSTS,
            difficulty = "Club",
            fen = "r2q1rk1/pp2bppp/2np1n2/4p3/2P1P3/2N2N2/PP2BPPP/R1BQ1RK1 w - - 0 11",
            solutionLineUci = listOf("c3d5"),
            objectivePrompt = "White to move · Occupy the permanent hole in Black's central pawn structure.",
            hintText = "Black's d6 and e5 pawns can never attack the d5 square again.",
            explanation = "11. Nd5! plants a monster knight on the eternal d5 outpost. Since Black has already pushed both the c-pawn (or lacks c6) and e-pawn, no enemy pawn can ever drive this piece away."
        ),

        // ==================== STRATEGY: PAWN BREAKS & BLOCKADES ====================
        HistoricalPuzzle(
            id = "str_carlsbad_minority",
            title = "The Carlsbad Minority Attack",
            matchInfo = "Queen's Gambit Declined · Classic Championship Plan",
            group = PuzzleGroup.PAWN_STRUCTURE_PLANS,
            difficulty = "Master",
            fen = "r1q2rk1/pp1nbppp/2p1pn2/3p4/1P1P4/2N1PN2/P3BPPP/1R1Q1RK1 w - - 0 14",
            solutionLineUci = listOf("b4b5"),
            objectivePrompt = "White to move · Execute the signature Carlsbad strategic pawn break.",
            hintText = "Push your queenside minority to create a permanent backward or isolated pawn for Black.",
            explanation = "14. b5! launches the classic Minority Attack: White uses two queenside pawns against Black's three to force weaknesses on c6 or d5, creating long-term targets on the half-open b- and c-files."
        ),
        HistoricalPuzzle(
            id = "str_good_knight_bad_bishop",
            title = "Good Knight vs Bad Bishop",
            matchInfo = "Aron Nimzowitsch · My System Positional Blueprint",
            group = PuzzleGroup.PAWN_STRUCTURE_PLANS,
            difficulty = "Club",
            fen = "6k1/pp3pbp/2p1p1p1/3pP3/3P4/2P2N2/PP3PPP/6K1 w - - 0 1",
            solutionLineUci = listOf("f3e1"),
            objectivePrompt = "White to move · Begin rerouting the knight toward the weak dark squares on d3 and c5.",
            hintText = "Black's g7 bishop is entombed behind its own pawns. Reposition your knight via e1-d3-c5!",
            explanation = "1. Ne1! starts the classic Nimzowitsch knight journey Ne1-d3-c5 (or f4). Because Black's pawns are fixed on dark/light squares blocking the bishop, White's agile knight dominates the entire board."
        ),

        // ==================== STRATEGY: CLASSIC ENDGAME TECHNIQUE ====================
        HistoricalPuzzle(
            id = "str_lucena_bridge",
            title = "The Lucena Position (Building the Bridge)",
            matchInfo = "Luis Ramírez de Lucena · Classic Endgame Theory, 1497",
            group = PuzzleGroup.ENDGAME_TECHNIQUE,
            difficulty = "Master",
            fen = "1K1k4/1P6/8/8/8/8/r7/2R5 w - - 0 1",
            solutionLineUci = listOf("c1c4"),
            objectivePrompt = "White to move · Set up the famous 'Building the Bridge' winning technique.",
            hintText = "Lift your rook to the 4th rank so it can shield your King from vertical checks after stepping out.",
            explanation = "1. Rc4! (or Rd1+ followed by Rc4) prepares to shelter the White king on the 4th rank! Once the king marches out (Ka7-a6-b5) and Black checks from behind, White blocks with Rb4, winning effortlessly."
        ),
        HistoricalPuzzle(
            id = "str_philidor_defense",
            title = "Philidor's 3rd-Rank Defense",
            matchInfo = "François-André Danican Philidor · Endgame Treatise, 1777",
            group = PuzzleGroup.ENDGAME_TECHNIQUE,
            difficulty = "Club",
            fen = "4k3/8/8/4PK2/8/8/R7/4r3 b - - 0 1",
            solutionLineUci = listOf("e1e3"),
            objectivePrompt = "Black to move · Establish the unbreakable Philidor defensive fortress to force a draw.",
            hintText = "Place your rook on the 3rd rank to prevent White's King from advancing in front of its pawn.",
            explanation = "1...Re3! (occupying the 3rd rank — Black's 6th) stops White's king from ever reaching f6 or e6. The moment White pushes e6, Black swings behind the pawn with ...Re1 and delivers endless checks from behind!"
        )
    )

    fun groupedByCategory(category: PuzzleCategory): List<Pair<PuzzleGroup, List<HistoricalPuzzle>>> {
        return PuzzleGroup.entries
            .filter { it.category == category }
            .map { group -> group to allPuzzles.filter { it.group == group } }
            .filter { it.second.isNotEmpty() }
    }
}
