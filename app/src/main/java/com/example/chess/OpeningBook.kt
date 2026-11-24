package com.example.chess

import kotlin.random.Random

/**
 * Built-in offline ECO Opening Classifier & Grandmaster Opening Book.
 * Identifies openings dynamically from the move sequence and provides
 * style-aligned grandmaster book lines for the offline engine.
 */
object OpeningBook {

    private data class OpeningEntry(
        val movesUci: List<String>,
        val eco: String,
        val name: String,
        val description: String,
        val preferredStyles: Set<EngineStyle> = emptySet()
    )

    private val entries: List<OpeningEntry> = listOf(
        // Starting / First moves
        OpeningEntry(listOf("e2e4"), "B00", "King's Pawn Opening", "Classic open center fight for initiative."),
        OpeningEntry(listOf("d2d4"), "A40", "Queen's Pawn Opening", "Solid central control and strategic tension."),
        OpeningEntry(listOf("c2c4"), "A10", "English Opening", "Flank control targeting the d5 square.", setOf(EngineStyle.POSITIONAL, EngineStyle.ENDGAME)),
        OpeningEntry(listOf("g1f3"), "A04", "Réti Opening", "Hypermodern flexible knight development.", setOf(EngineStyle.POSITIONAL, EngineStyle.ENDGAME)),
        OpeningEntry(listOf("f2f4"), "A02", "Bird's Opening", "Unbalanced flank attack on e5.", setOf(EngineStyle.SHARP)),
        OpeningEntry(listOf("b2b3"), "A01", "Nimzo-Larsen Attack", "Fianchetto pressure along the long diagonal."),

        // King's Pawn Responses
        OpeningEntry(listOf("e2e4", "e7e5"), "C20", "Open Game", "Symmetrical battle for central space."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3"), "C40", "King's Knight Opening", "Immediate pressure on Black's e5 pawn."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6"), "C44", "King's Knight Game", "Classical development defending e5."),

        // Italian Game & Giuoco Piano / Evans Gambit / Two Knights
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4"), "C50", "Italian Game", "Direct pressure on the f7 weak point.", setOf(EngineStyle.UNIVERSAL, EngineStyle.AGGRESSIVE)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5"), "C50", "Giuoco Piano", "Classical Italian battle for d4 and rapid castling."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "c2c3"), "C53", "Giuoco Piano: Main Line", "Preparing a strong d4 central expansion."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "d2d3"), "C50", "Giuoco Pianissimo", "Quiet, deep maneuvering Italian structure.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "f8c5", "b2b4"), "C51", "Evans Gambit", "Sacrificing a flank pawn for explosive central speed.", setOf(EngineStyle.SHARP, EngineStyle.AGGRESSIVE)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6"), "C55", "Two Knights Defense", "Counterattacking e4 immediately.", setOf(EngineStyle.AGGRESSIVE, EngineStyle.SHARP)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "f3g5"), "C57", "Two Knights: Fried Liver Attack", "Fierce tactical assault on f7.", setOf(EngineStyle.AGGRESSIVE, EngineStyle.SHARP)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6", "d2d3"), "C55", "Italian Game: Modern Line", "Solid grandmaster buildup preserving tension."),

        // Ruy Lopez (Spanish Game)
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5"), "C60", "Ruy Lopez (Spanish Game)", "The premier classical test of Black's center.", setOf(EngineStyle.UNIVERSAL, EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6"), "C70", "Ruy Lopez: Morphy Defense", "Questioning the Spanish bishop immediately."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6"), "C78", "Ruy Lopez: Closed / Open", "Deep grandmaster strategic struggle."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5a4", "g8f6", "e1g1", "f8e7"), "C84", "Ruy Lopez: Closed Defense", "Rich maneuvering with long-term kingside plans."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "a7a6", "b5c6"), "C68", "Ruy Lopez: Exchange Variation", "Structural endgame edge via doubled pawns.", setOf(EngineStyle.ENDGAME)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "g8f6"), "C65", "Ruy Lopez: Berlin Defense", "Ultra-solid championship weapon.", setOf(EngineStyle.ENDGAME, EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "f1b5", "g8f6", "e1g1", "f6e4"), "C67", "Berlin Defense: Rio Gambit", "Leading toward the famous Berlin endgame."),

        // Scotch, Four Knights, Petrov, Philidor, King's Gambit, Vienna
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4"), "C45", "Scotch Game", "Immediate central rupture for active piece play.", setOf(EngineStyle.AGGRESSIVE, EngineStyle.UNIVERSAL)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "d2d4", "e5d4", "f3d4"), "C45", "Scotch Game: Classical", "Open center with lively tactical prospects."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "b8c6", "b1c3", "g8f6"), "C47", "Four Knights Game", "Harmonic classical piece development."),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "g8f6"), "C42", "Petrov's Defense", "Symmetrical counter-strike at e4.", setOf(EngineStyle.POSITIONAL, EngineStyle.ENDGAME)),
        OpeningEntry(listOf("e2e4", "e7e5", "g1f3", "d7d6"), "C41", "Philidor Defense", "Compact, solid central pawn chain."),
        OpeningEntry(listOf("e2e4", "e7e5", "f2f4"), "C30", "King's Gambit", "Romantic sacrificial assault on the f-file.", setOf(EngineStyle.SHARP, EngineStyle.AGGRESSIVE)),
        OpeningEntry(listOf("e2e4", "e7e5", "f2f4", "e5f4"), "C33", "King's Gambit Accepted", "High-stakes tactical battle for the initiative."),
        OpeningEntry(listOf("e2e4", "e7e5", "b1c3"), "C25", "Vienna Game", "Flexible knight development preparing f4."),
        OpeningEntry(listOf("e2e4", "e7e5", "f1c4"), "C23", "Bishop's Opening", "Targeting f7 while keeping the f-pawn free."),

        // Sicilian Defense Family
        OpeningEntry(listOf("e2e4", "c7c5"), "B20", "Sicilian Defense", "Asymmetrical counterattack fighting for d4.", setOf(EngineStyle.AGGRESSIVE, EngineStyle.SHARP, EngineStyle.UNIVERSAL)),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3"), "B27", "Sicilian Defense: Open Setup", "Preparing d4 to seize central space."),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3"), "B56", "Open Sicilian", "Dynamic sharp middlegame battle."),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "a7a6"), "B90", "Sicilian Defense: Najdorf", "The sharpest grandmaster weapon in chess.", setOf(EngineStyle.SHARP, EngineStyle.AGGRESSIVE)),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3", "d7d6", "d2d4", "c5d4", "f3d4", "g8f6", "b1c3", "g7g6"), "B70", "Sicilian Defense: Dragon", "Opposite-side castling razor-sharp attack.", setOf(EngineStyle.SHARP)),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3", "b8c6", "d2d4", "c5d4", "f3d4"), "B32", "Open Sicilian: Classical", "Active piece play in the center."),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3", "b8c6", "f1b5"), "B30", "Sicilian: Rossolimo Attack", "Positional pressure avoiding main-line chaos.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "c7c5", "g1f3", "e7e6", "d2d4", "c5d4", "f3d4"), "B40", "Sicilian Defense: Paulsen / Kan", "Flexible, resilient structure with counterplay."),
        OpeningEntry(listOf("e2e4", "c7c5", "c2c3"), "B22", "Sicilian Defense: Alapin", "Direct bid for a full pawn center with d4.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "c7c5", "b1c3"), "B23", "Closed Sicilian", "Strategic kingside expansion.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "c7c5", "d2d4", "c5d4", "c2c3"), "B21", "Smith-Morra Gambit", "Rapid open-file development for a pawn.", setOf(EngineStyle.SHARP)),

        // French Defense
        OpeningEntry(listOf("e2e4", "e7e6"), "C00", "French Defense", "Resilient counterattack on the White center.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "e7e6", "d2d4", "d7d5"), "C00", "French Defense: Main Line", "Classic tension between e4 and d5."),
        OpeningEntry(listOf("e2e4", "e7e6", "d2d4", "d7d5", "b1c3"), "C10", "French: Classical / Winawer", "Defending e4 while maintaining maximum tension."),
        OpeningEntry(listOf("e2e4", "e7e6", "d2d4", "d7d5", "b1c3", "f8b4"), "C15", "French Defense: Winawer", "Sharp double-edged battle for the dark squares.", setOf(EngineStyle.SHARP)),
        OpeningEntry(listOf("e2e4", "e7e6", "d2d4", "d7d5", "b1d2"), "C03", "French Defense: Tarrasch", "Solid structural control without pins.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("e2e4", "e7e6", "d2d4", "d7d5", "e4e5"), "C02", "French Defense: Advance", "Space advantage vs queenside counterplay."),
        OpeningEntry(listOf("e2e4", "e7e6", "d2d4", "d7d5", "e4d5"), "C01", "French Defense: Exchange", "Symmetrical open e-file battle."),

        // Caro-Kann, Scandinavian, Pirc, Modern, Alekhine
        OpeningEntry(listOf("e2e4", "c7c6"), "B10", "Caro-Kann Defense", "Rock-solid structure with strong endgame prospects.", setOf(EngineStyle.POSITIONAL, EngineStyle.ENDGAME)),
        OpeningEntry(listOf("e2e4", "c7c6", "d2d4", "d7d5"), "B12", "Caro-Kann: Main Line", "Challenging e4 with c6 support."),
        OpeningEntry(listOf("e2e4", "c7c6", "d2d4", "d7d5", "b1c3", "d5e4", "c3e4"), "B15", "Caro-Kann: Classical", "Clean piece play and healthy pawn structure."),
        OpeningEntry(listOf("e2e4", "c7c6", "d2d4", "d7d5", "e4e5"), "B12", "Caro-Kann: Advance Variation", "Space grab with lively tactical motifs."),
        OpeningEntry(listOf("e2e4", "d7d5"), "B01", "Scandinavian Defense", "Immediate central confrontation on move 1."),
        OpeningEntry(listOf("e2e4", "d7d6", "d2d4", "g8f6", "b1c3", "g7g6"), "B07", "Pirc Defense", "Hypermodern counterattack against a broad center."),
        OpeningEntry(listOf("e2e4", "g7g6"), "B06", "Modern Defense", "Flexible fianchetto aiming to undermine the center."),
        OpeningEntry(listOf("e2e4", "g8f6"), "B02", "Alekhine's Defense", "Provoking White's pawns forward to attack them later.", setOf(EngineStyle.SHARP)),

        // Queen's Pawn Openings: Queen's Gambit, Slav, London, Colle
        OpeningEntry(listOf("d2d4", "d7d5"), "D00", "Closed Game", "Classical fight for d4 and d5."),
        OpeningEntry(listOf("d2d4", "d7d5", "c2c4"), "D06", "Queen's Gambit", "Offering the c-pawn to dominate the center.", setOf(EngineStyle.UNIVERSAL, EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("d2d4", "d7d5", "c2c4", "e7e6"), "D30", "Queen's Gambit Declined", "Time-tested classical defense of d5.", setOf(EngineStyle.POSITIONAL, EngineStyle.ENDGAME)),
        OpeningEntry(listOf("d2d4", "d7d5", "c2c4", "e7e6", "b1c3", "g8f6"), "D35", "QGD: Orthodox / Exchange", "Deep central strategy and minority attack plans."),
        OpeningEntry(listOf("d2d4", "d7d5", "c2c4", "d5c4"), "D20", "Queen's Gambit Accepted", "Active piece play in exchange for central space."),
        OpeningEntry(listOf("d2d4", "d7d5", "c2c4", "c7c6"), "D10", "Slav Defense", "Fortifying d5 while keeping the light-squared bishop free.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("d2d4", "d7d5", "c2c4", "c7c6", "g1f3", "g8f6", "b1c3", "e7e6"), "D43", "Semi-Slav Defense", "Combines steel structure with razor-sharp counterplay.", setOf(EngineStyle.SHARP)),
        OpeningEntry(listOf("d2d4", "d7d5", "c1f4"), "D00", "London System", "Ultra-reliable pyramid structure and smooth development.", setOf(EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("d2d4", "g8f6", "c1f4"), "A45", "London System", "Solid dark-square control and flexible kingside plans."),
        OpeningEntry(listOf("d2d4", "g8f6", "c1g5"), "A45", "Trompowsky Attack", "Immediate asymmetrical disruption on f6.", setOf(EngineStyle.AGGRESSIVE)),

        // Indian Defenses (Nimzo, King's Indian, Grünfeld, Queen's Indian, Benoni, Dutch)
        OpeningEntry(listOf("d2d4", "g8f6"), "A45", "Indian Defense", "Hypermodern control of e4 before committing pawns."),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4"), "A50", "Indian Game", "Seizing central space against Black's knight."),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4", "e7e6", "b1c3", "f8b4"), "E20", "Nimzo-Indian Defense", "Elite championship defense targeting e4 and structure.", setOf(EngineStyle.UNIVERSAL, EngineStyle.POSITIONAL)),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4", "e7e6", "g1f3", "b7b6"), "E12", "Queen's Indian Defense", "Fianchetto control of the light-square diagonal."),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4", "e7e6", "g2g3"), "E00", "Catalan Opening", "Long-term queenside pressure with a lethal g2 bishop.", setOf(EngineStyle.POSITIONAL, EngineStyle.ENDGAME)),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4", "g7g6", "b1c3", "f8g7"), "E60", "King's Indian Defense", "Fierce kingside counterattack vs White's queenside push.", setOf(EngineStyle.AGGRESSIVE, EngineStyle.SHARP)),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4", "g7g6", "b1c3", "d7d5"), "D80", "Grünfeld Defense", "Dynamic hypermodern strike at White's central pawns.", setOf(EngineStyle.SHARP, EngineStyle.UNIVERSAL)),
        OpeningEntry(listOf("d2d4", "g8f6", "c2c4", "c7c5", "d4d5"), "A60", "Benoni Defense", "Imbalanced pawn majority race.", setOf(EngineStyle.SHARP)),
        OpeningEntry(listOf("d2d4", "f7f5"), "A80", "Dutch Defense", "Ambitious kingside fight for the e4 square.", setOf(EngineStyle.AGGRESSIVE)),

        // English & Réti Continuations
        OpeningEntry(listOf("c2c4", "e7e5"), "A20", "English: Reversed Sicilian", "Sicilian structure with an extra tempo."),
        OpeningEntry(listOf("c2c4", "c7c5"), "A30", "English: Symmetrical", "Deep positional battle for central outposts."),
        OpeningEntry(listOf("c2c4", "g8f6", "b1c3", "e7e6"), "A15", "Anglo-Indian Defense", "Flexible transpositional grandmaster play."),
        OpeningEntry(listOf("g1f3", "d7d5", "c2c4"), "A09", "Réti Opening: Main Line", "Undermining Black's center from the flank.")
    )

    fun identifyOpening(movesUci: List<String>): OpeningInfo {
        if (movesUci.isEmpty()) {
            return OpeningInfo(
                eco = "START",
                name = "Starting Position",
                description = "Make a move or press Guess the Move to train."
            )
        }
        var bestMatch: OpeningEntry? = null
        for (entry in entries) {
            if (entry.movesUci.size <= movesUci.size) {
                var matches = true
                for (i in entry.movesUci.indices) {
                    if (entry.movesUci[i] != movesUci[i]) {
                        matches = false
                        break
                    }
                }
                if (matches && (bestMatch == null || entry.movesUci.size > bestMatch.movesUci.size)) {
                    bestMatch = entry
                }
            }
        }
        return if (bestMatch != null) {
            OpeningInfo(bestMatch.eco, bestMatch.name, bestMatch.description)
        } else {
            OpeningInfo(
                eco = "A00",
                name = "Irregular Opening",
                description = "Unconventional opening structure."
            )
        }
    }

    /**
     * Picks a strong grandmaster book move matching the current sequence and engine style,
     * keeping opening play varied, authentic, and tournament-strength.
     */
    fun pickBookMoveUci(movesUci: List<String>, style: EngineStyle): String? {
        if (movesUci.size >= 8) return null
        val candidates = mutableListOf<Pair<String, Int>>()
        for (entry in entries) {
            if (entry.movesUci.size > movesUci.size) {
                var prefixMatch = true
                for (i in movesUci.indices) {
                    if (entry.movesUci[i] != movesUci[i]) {
                        prefixMatch = false
                        break
                    }
                }
                if (prefixMatch) {
                    val nextUci = entry.movesUci[movesUci.size]
                    val weight = if (style in entry.preferredStyles) 4 else 1
                    candidates.add(nextUci to weight)
                }
            }
        }
        if (candidates.isEmpty()) return null

        // Deduplicate & sum weights
        val grouped = candidates.groupBy { it.first }
            .map { (move, list) -> move to list.sumOf { it.second } }
        val total = grouped.sumOf { it.second }
        var roll = Random.nextInt(total)
        for ((move, w) in grouped) {
            roll -= w
            if (roll < 0) return move
        }
        return grouped.first().first
    }
}
