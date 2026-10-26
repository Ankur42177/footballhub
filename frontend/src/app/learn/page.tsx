import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";
import type { ReactNode } from "react";

import { BlogFeedSkeleton } from "@/components/blog/BlogFeed";
import { OffsideDiagram, PositionsDiagram } from "@/components/learn/LearnDiagrams";
import { LEARN_AUTHOR, LEARN_TITLE_PREFIX } from "@/lib/learn";
import { getFeed } from "@/lib/server-api";

export const metadata: Metadata = {
  title: "Learn football: a beginner's guide",
  description:
    "New to football? The pitch, every position, the offside rule, fouls and cards, how a match and a league season work, and a glossary, explained simply.",
  alternates: { canonical: "/learn" },
};

const SECTIONS = [
  { id: "basics", label: "The basics" },
  { id: "pitch", label: "The pitch" },
  { id: "positions", label: "Positions" },
  { id: "offside", label: "Offside" },
  { id: "fouls", label: "Fouls and cards" },
  { id: "restarts", label: "Restarts" },
  { id: "match", label: "A match" },
  { id: "league", label: "A league season" },
  { id: "var", label: "VAR" },
  { id: "glossary", label: "Glossary" },
  { id: "guides", label: "Guides" },
];

export default function LearnPage() {
  return (
    <div>
      <div className="border-b border-line bg-pitch-deep">
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 sm:py-16">
          <p className="text-[0.95rem] text-chalk-dim">Beginner&apos;s guide</p>
          <h1 className="display mt-2 text-5xl sm:text-7xl">Learn football</h1>
          <p className="mt-4 max-w-2xl text-lg text-chalk/85">
            Never followed football, or want to understand what you&apos;re watching? Start here. Everything a new fan
            needs, from what each player does to the offside rule, in plain language.
          </p>
          <nav aria-label="On this page" className="mt-8">
            <ul className="flex flex-wrap gap-2">
              {SECTIONS.map((section) => (
                <li key={section.id}>
                  <a
                    href={`#${section.id}`}
                    className="inline-block rounded-chip border border-line-strong px-3 py-1 text-sm text-chalk no-underline hover:border-floodlight hover:text-floodlight"
                  >
                    {section.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </div>

      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
        <Section id="basics" title="The basics">
          <p>
            Two teams of <strong>eleven players</strong> try to get the ball into the other team&apos;s goal. Only the{" "}
            <strong>goalkeeper</strong> may handle the ball, and only inside their own penalty area. Everyone else uses
            their feet, head and body. A goal counts when the <strong>whole ball crosses the whole goal line</strong>{" "}
            between the posts and under the bar. The team with more goals at the end wins; level scores are a draw.
          </p>
          <FactGrid
            facts={[
              ["Players", "11 per team, including one goalkeeper"],
              ["Match length", "Two halves of 45 minutes, plus added time"],
              ["Substitutes", "5 per team in the Premier League"],
              ["Officials", "A referee, two assistants, a fourth official and VAR"],
            ]}
          />
        </Section>

        <Section id="pitch" title="The pitch">
          <p>
            Pitches vary in size, but Premier League pitches are about <strong>105 metres long and 68 metres wide</strong>.
            The halfway line splits it in two, and each team defends one goal for a half before swapping ends.
          </p>
          <ul>
            <li>
              <strong>Goal:</strong> 7.32 m wide and 2.44 m high (8 yards by 8 feet).
            </li>
            <li>
              <strong>Penalty area</strong> (the big box): extends 16.5 m from the goal line. The goalkeeper can handle the
              ball here, and a foul by the defending team inside it gives a penalty.
            </li>
            <li>
              <strong>Goal area</strong> (the six-yard box): where goal kicks are usually taken from.
            </li>
            <li>
              <strong>Penalty spot:</strong> 11 m (12 yards) from goal.
            </li>
            <li>
              <strong>Centre circle:</strong> radius 9.15 m. Opponents must stay outside it at kick-off.
            </li>
          </ul>
        </Section>

        <Section id="positions" title="Positions: who does what">
          <p>
            Teams line up in a <strong>formation</strong>, written from the back: 4-3-3 means four defenders, three
            midfielders and three forwards (the goalkeeper isn&apos;t counted).
          </p>
          <div className="my-6">
            <PositionsDiagram />
          </div>
          <dl className="grid gap-4">
            <Term name="Goalkeeper (GK)">
              Stops shots, commands the penalty area and increasingly starts attacks with passes.
            </Term>
            <Term name="Centre-backs (CB)">
              The central defenders. They mark strikers, win headers and block shots.
            </Term>
            <Term name="Full-backs (RB, LB)">
              Defenders on the sides who also run forward to support attacks. In a back three they become wing-backs.
            </Term>
            <Term name="Defensive midfielder (DM)">
              Sits in front of the defence, breaks up attacks and sets the team&apos;s passing rhythm.
            </Term>
            <Term name="Central midfielders (CM)">
              Link defence and attack: pass, press, tackle and arrive in the box. An attacking midfielder (the
              &quot;number 10&quot;) plays just behind the strikers and creates chances.
            </Term>
            <Term name="Wingers (RW, LW)">Wide attackers who dribble, cross and cut inside to shoot.</Term>
            <Term name="Striker (ST)">
              The main goalscorer, also called the centre-forward or &quot;number 9&quot;.
            </Term>
          </dl>
        </Section>

        <Section id="offside" title="The offside rule, simply">
          <p>
            Offside stops attackers from waiting next to the goal. The key moment is{" "}
            <strong>when a team-mate plays the ball</strong> to you.
          </p>
          <p>
            You are in an <strong>offside position</strong> if, at that moment, you are in the opponents&apos; half and
            any part of your head, body or feet is closer to their goal line than <strong>both the ball and the
            second-last opponent</strong> (usually the last outfield defender, because the goalkeeper is the last).
            Arms and hands don&apos;t count.
          </p>
          <div className="my-6">
            <OffsideDiagram />
          </div>
          <ul>
            <li>
              Being in an offside position isn&apos;t an offence on its own. It&apos;s only offside if you then{" "}
              <strong>get involved</strong>: playing the ball, blocking an opponent&apos;s view or challenge, or gaining an
              advantage from a rebound.
            </li>
            <li>
              <strong>Level is onside.</strong> Being level with the second-last defender is fine.
            </li>
            <li>
              You can&apos;t be offside in <strong>your own half</strong>, or directly from a{" "}
              <strong>throw-in, goal kick or corner</strong>.
            </li>
            <li>The punishment is an indirect free kick to the defending team.</li>
          </ul>
        </Section>

        <Section id="fouls" title="Fouls, free kicks, penalties and cards">
          <p>
            Tripping, kicking, pushing, holding, dangerous tackles and deliberate handball are fouls. The referee gives a
            free kick to the other team, and opponents must stand at least 9.15 m (10 yards) away.
          </p>
          <ul>
            <li>
              <strong>Direct free kick:</strong> for most fouls. You can shoot straight into the goal.
            </li>
            <li>
              <strong>Indirect free kick:</strong> for things like offside or dangerous play. A second player must touch
              the ball before a goal can be scored; the referee raises an arm to show it.
            </li>
            <li>
              <strong>Penalty:</strong> a direct-free-kick foul by the defending team inside its own penalty area. One
              shot from the penalty spot, with only the goalkeeper to beat.
            </li>
            <li>
              <strong>Advantage:</strong> if stopping play would help the team that was fouled, the referee lets it carry
              on.
            </li>
          </ul>
          <FactGrid
            facts={[
              ["Yellow card", "A caution, for reckless fouls, time-wasting or dissent"],
              ["Second yellow", "Becomes a red: the player is sent off"],
              ["Red card", "Sent off straight away for serious foul play, violent conduct or denying an obvious goal"],
              ["After a red", "The team plays on with one fewer player; the player is banned for later games"],
            ]}
          />
        </Section>

        <Section id="restarts" title="Restarts: how play starts again">
          <dl className="grid gap-4">
            <Term name="Kick-off">Starts each half and restarts play after a goal, from the centre spot.</Term>
            <Term name="Throw-in">
              When the ball goes over a sideline. Taken with both hands from behind the head, both feet on the ground.
            </Term>
            <Term name="Goal kick">
              When the attacking team puts the ball over the goal line (not in the goal). Taken from the goal area.
            </Term>
            <Term name="Corner kick">
              When the defending team puts the ball over its own goal line. Taken from the corner of the pitch.
            </Term>
            <Term name="Drop ball">When the referee stops play for something like an injury, with no foul.</Term>
          </dl>
        </Section>

        <Section id="match" title="How a match works">
          <ul>
            <li>
              Two halves of 45 minutes with a 15-minute break. The clock doesn&apos;t stop, so the referee adds{" "}
              <strong>stoppage time</strong> at the end of each half for injuries, substitutions and delays.
            </li>
            <li>
              In a <strong>league</strong> match a draw is a valid result. In a <strong>knockout</strong> match (like a
              cup final) a draw goes to <strong>extra time</strong>, two halves of 15 minutes, then a{" "}
              <strong>penalty shoot-out</strong>: five kicks each, then one at a time until someone misses.
            </li>
            <li>A team needs at least seven players to continue; below that, the match is abandoned.</li>
          </ul>
        </Section>

        <Section id="league" title="How a league season works">
          <p>
            The Premier League has <strong>20 clubs</strong>. Each plays every other club twice, home and away:{" "}
            <strong>38 matches</strong> from August to May.
          </p>
          <FactGrid
            facts={[
              ["Win", "3 points"],
              ["Draw", "1 point"],
              ["Defeat", "0 points"],
              ["Level on points", "Goal difference, then goals scored, decide the order"],
            ]}
          />
          <ul>
            <li>The club with the most points is <strong>champion</strong>.</li>
            <li>
              The top clubs qualify for European competitions, mainly the <strong>Champions League</strong>.
            </li>
            <li>
              The bottom three are <strong>relegated</strong> to the Championship, the second tier. Three clubs come up:
              the top two in the Championship and the winner of its play-offs.
            </li>
            <li>
              Clubs also play in cup competitions alongside the league, such as the FA Cup and the League Cup.
            </li>
          </ul>
        </Section>

        <Section id="var" title="VAR">
          <p>
            The <strong>video assistant referee</strong> watches replays and can advise the referee on four kinds of
            decision only: <strong>goals</strong> (including offside in the build-up), <strong>penalties</strong>,{" "}
            <strong>straight red cards</strong> and <strong>mistaken identity</strong>. It&apos;s meant to fix clear
            and obvious errors, not re-referee every tackle. The referee can watch the replay on a pitch-side monitor
            before deciding.
          </p>
        </Section>

        <Section id="glossary" title="Glossary">
          <dl className="grid gap-x-8 gap-y-4 sm:grid-cols-2">
            <Term name="Clean sheet">A match in which a team concedes no goals.</Term>
            <Term name="Brace / hat-trick">Two / three goals by one player in a match.</Term>
            <Term name="Assist">The pass that sets up a goal.</Term>
            <Term name="Derby">A match between local rivals, such as City v United.</Term>
            <Term name="Pressing">Chasing the opponents in a group to win the ball back quickly.</Term>
            <Term name="Counter-attack">A fast attack straight after winning the ball.</Term>
            <Term name="Set piece">An attack from a free kick, corner or throw-in.</Term>
            <Term name="Nutmeg">Playing the ball between an opponent&apos;s legs.</Term>
            <Term name="Transfer window">The periods (summer and January) when clubs can sign players.</Term>
            <Term name="Treble">Winning three major trophies in one season.</Term>
          </dl>
        </Section>

        <section id="guides" aria-labelledby="guides-title" className="scroll-mt-6 border-t border-line pt-10">
          <h2 id="guides-title" className="display-medium text-4xl">
            Football basics: the guides
          </h2>
          <p className="mt-2 text-chalk-dim">
            Longer explainers by Ankur Kumar, in reading order. Each one goes deeper than the summary above.
          </p>
          <div className="mt-6">
            <Suspense fallback={<BlogFeedSkeleton rows={3} />}>
              <Guides />
            </Suspense>
          </div>
        </section>
      </div>
    </div>
  );
}

async function Guides() {
  const feed = await getFeed({ author: LEARN_AUTHOR, q: LEARN_TITLE_PREFIX, sort: "oldest", size: 50 }).catch(() => null);
  const guides = (feed?.content ?? []).filter((blog) => blog.title.startsWith(LEARN_TITLE_PREFIX));
  if (guides.length === 0) {
    return <p className="text-chalk-dim">The guides are on their way. Check back soon.</p>;
  }
  return (
    <ol className="divide-y divide-line border-y border-line">
      {guides.map((blog, index) => (
        <li key={blog.id} className="grid grid-cols-[2.5rem_1fr] gap-3 py-5">
          <span className="score text-4xl text-floodlight" aria-hidden="true">
            {index + 1}
          </span>
          <div>
            <h3 className="display-medium text-2xl">
              <Link href={`/blogs/${blog.slug}`} className="no-underline hover:text-floodlight">
                {blog.title.slice(LEARN_TITLE_PREFIX.length).trim().replace(/^./, (c) => c.toUpperCase())}
              </Link>
            </h3>
            {blog.excerpt && <p className="mt-1 text-[0.95rem] text-chalk-dim">{blog.excerpt}</p>}
            <p className="mt-1 text-sm text-chalk-dim">{blog.readingMinutes} min read</p>
          </div>
        </li>
      ))}
    </ol>
  );
}

function Section({ id, title, children }: { id: string; title: string; children: ReactNode }) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className="scroll-mt-6 border-t border-line py-10 first:border-t-0 first:pt-0">
      <h2 id={`${id}-title`} className="display-medium text-4xl">
        {title}
      </h2>
      <div className="article-body mt-4 text-[1.0625rem]">{children}</div>
    </section>
  );
}

function FactGrid({ facts }: { facts: [string, string][] }) {
  return (
    <dl className="not-prose mt-5 grid gap-3 sm:grid-cols-2">
      {facts.map(([term, value]) => (
        <div key={term} className="rounded-panel border border-line bg-pitch-raised px-4 py-3">
          <dt className="font-display text-lg font-bold text-floodlight">{term}</dt>
          <dd className="text-[0.95rem] text-chalk">{value}</dd>
        </div>
      ))}
    </dl>
  );
}

function Term({ name, children }: { name: string; children: ReactNode }) {
  return (
    <div>
      <dt className="font-semibold text-chalk">{name}</dt>
      <dd className="text-[0.98rem] text-chalk-dim">{children}</dd>
    </div>
  );
}
