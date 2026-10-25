package com.footballhub.demo;

import java.util.List;

/** Original, evergreen sample articles used to populate a fresh demo deployment. */
final class DemoArticles {

    record Article(String clubSlug, String title, String excerpt, String content, int daysAgo) {
    }

    private DemoArticles() {
    }

    static final List<Article> ALL = List.of(
            new Article("manchester-city",
                    "Inverted full-backs: why the wide defender keeps drifting inside",
                    "The full-back who steps into midfield isn't a gimmick. It is a fix for the oldest problem "
                            + "in possession football: how to stay compact after you lose the ball.",
                    """
                    Watch a possession-heavy side build from the back and you will often see a full-back do something odd. Instead of hugging the touchline, he walks infield and stands next to the holding midfielder.

                    ## The problem it solves

                    A team that keeps the ball high up the pitch is always one misplaced pass away from a counter-attack. Traditional full-backs bomb forward, so when possession is lost the centre of the pitch is wide open.

                    Moving a full-back inside creates a **2-3 or 3-2 shape** behind the ball:

                    - two or three defenders who never leave their zone,
                    - two midfielders screening the most dangerous passing lanes,
                    - wingers and forwards free to stay high and wide.

                    ## What it costs

                    Width has to come from somewhere else. The winger must stay glued to the line, which suits some players and wastes others. The inverted full-back also needs a midfielder's skills: receiving on the half-turn, scanning before the ball arrives, and passing under pressure.

                    ## What to watch for

                    Next time you see the tactic, track the full-back in the seconds *after* possession is lost. If he is already in the central lane, the counter-press starts with five players around the ball rather than three. That is the real point of the move.
                    """, 2),
            new Article("arsenal",
                    "Set pieces are the most honest edge in football",
                    "Corners and free kicks can be rehearsed in a way open play never can. That is why more "
                            + "clubs now treat them as a specialist discipline.",
                    """
                    Open play is chaos. Set pieces are the only moments in a match when a coach can script almost everything: the starting positions, the decoy runs, the blocks and the delivery.

                    ## Why the edge is real

                    A typical team gets plenty of corners and wide free kicks in a match. If rehearsed routines turn even a small share of those into goals, that adds up over a 38-game season, and it can be the gap between mid-table and Europe.

                    ## The building blocks

                    - **Delivery zones.** In-swinging balls aimed at the six-yard box force the goalkeeper to decide. Out-swingers give attackers a run at the ball.
                    - **Screens.** Legal blocks that stop a marker following his man are the modern equivalent of a basketball pick.
                    - **The second ball.** Many set-piece goals come from the clearance, not the first header. Good teams station players to recycle it.

                    ## Defending them

                    Zonal, man-to-man or a hybrid: none is universally best. What matters is that every player knows his job, and that the team practises the specific threats of its next opponent.

                    The beauty of set pieces is that they reward preparation over talent. For a club without the budget to buy elite forwards, that is the most honest edge in the game.
                    """, 4),
            new Article("liverpool",
                    "Reading a high press: triggers, traps and cover shadows",
                    "A good press isn't eleven players running at the ball. It's a shared set of cues that "
                            + "tells everyone when to jump and where to herd the opponent.",
                    """
                    Pressing looks like energy. Underneath, it is information. Every player needs to know the same answer to one question: *when do we go?*

                    ## Common pressing triggers

                    1. A pass back to the goalkeeper.
                    2. A centre-back receiving on his weaker foot.
                    3. A sideways pass into a full-back near the touchline.
                    4. A poor first touch.

                    Each trigger signals that the receiver's options just shrank, and that is the moment to close him down.

                    ## Cover shadows

                    The first presser rarely aims straight at the ball. He curves his run so his body blocks a passing lane behind him. That "shadow" removes an option without anyone needing to mark it.

                    ## Traps

                    The best presses deliberately leave one pass open, usually out to the touchline, and then collapse on it. The touchline acts as an extra defender.

                    ## When it fails

                    If one player jumps late, a gap opens between the lines, and a single pass breaks the whole structure. That is why pressing teams train the press as a unit, and why fatigue late in games is so costly for them.
                    """, 6),
            new Article("aston-villa",
                    "The quiet return of the back three",
                    "Three centre-backs used to signal caution. Used well, the shape now gives attacking "
                            + "teams more width and more security at the same time.",
                    """
                    For a while the back four seemed to have won. Now three-centre-back systems are back across the league, often in disguise.

                    ## Why three at the back again

                    - **Width without risk.** Wing-backs can push high because a third centre-back covers the space behind them.
                    - **Build-up numbers.** Three players across the back make it hard for two strikers to press effectively.
                    - **Flexibility.** Many teams build with three and defend with four, or the other way round, depending on the phase of play.

                    ## The 3-4-2-1 in particular

                    Two narrow attacking midfielders behind a striker occupy the spaces between the opponent's full-backs and centre-backs. Defenders are forced to choose who steps out, and the choice is usually wrong for someone.

                    ## The weaknesses

                    The space outside the wide centre-backs is the classic target. A quick winger isolated there, with the wing-back caught upfield, is the scenario every three-at-the-back coach fears.

                    Formations are only starting positions, but this one has earned its comeback.
                    """, 8),
            new Article("newcastle-united",
                    "Expected goals without the jargon",
                    "xG is just a way of asking how good a chance was. Here is how to use it without "
                            + "pretending it predicts the future.",
                    """
                    Expected goals (xG) puts a number on a simple idea: some chances are better than others.

                    ## How it works

                    Models look at thousands of past shots with similar features: distance, angle, type of assist, header or foot, and whether the shooter was under pressure. If shots like this one went in 12% of the time, it is worth **0.12 xG**.

                    ## What it is good for

                    - Judging whether a team creates good chances or just lots of shots.
                    - Spotting finishing streaks that are unlikely to last.
                    - Comparing defences by the quality of chances they allow, not just goals conceded.

                    ## What it is not

                    xG does not know about the striker's form, the goalkeeper's positioning in most models, or a deflection that changed everything. A single match's xG is noisy. Over a season it becomes far more meaningful.

                    ## A healthy way to use it

                    Treat xG as a question, not an answer. When a team keeps beating its xG, ask why: elite finishers, or luck that will even out? Either answer is interesting.
                    """, 10),
            new Article("chelsea",
                    "Why the No. 6 is the hardest position to scout",
                    "The holding midfielder's best work happens off the ball and away from the camera. "
                            + "Highlight reels barely show it.",
                    """
                    Scouts can measure a winger's dribbles and a striker's shots. The defensive midfielder's most important contributions are often things that *didn't* happen.

                    ## The invisible work

                    - Standing in the passing lane so the opponent never tries the pass.
                    - Moving five metres to cover a full-back who has pushed on.
                    - Receiving under pressure and playing the simple ball that relieves it.

                    None of that shows up in a goals-and-assists column.

                    ## What to look for instead

                    1. **Scanning.** Count how often the player checks over his shoulder before receiving.
                    2. **Body shape.** Does he receive side-on, ready to play forward?
                    3. **Positional discipline.** Where is he when his team loses the ball?

                    ## Why it matters

                    Many title-winning sides have been built around a No. 6 who rarely made headlines. When that player is missing, the whole structure feels looser, even if nobody can quite say why.
                    """, 12),
            new Article("hull-city",
                    "What newly promoted sides can learn from the first ten games",
                    "The jump to the top flight is mostly about margins. The first ten matches decide how "
                            + "much pressure the rest of the season brings.",
                    """
                    Promotion brings a step up in speed, precision and punishment for mistakes. The clubs that settle quickest tend to share a few habits.

                    ## Pick an identity and keep it

                    Abandoning the style that won promotion at the first defeat rarely works. Small adjustments to defensive depth and pressing height usually beat a full rebuild.

                    ## Defend the box first

                    Top-flight forwards punish loose marking far more often. Compact defending around the penalty area keeps games close, and close games give a promoted side a chance.

                    ## Win the transitions

                    Many goals for promoted teams come from fast breaks after winning the ball. Having two quick outlets ready changes how opponents commit players forward.

                    ## Manage the calendar

                    Fixture lists often look harder than they are. Treat the first ten games as a block and judge progress on performances, not single results.

                    None of this guarantees survival, but it gives a newly promoted squad the best chance to stay competitive.
                    """, 14));
}
