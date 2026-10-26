/**
 * Diagrams for the beginner's guide, drawn as chalk on the navy pitch. Coordinates are in
 * metres on a 105 x 68 pitch (x = 0 is the team's own goal line), scaled by 10.
 */

function PitchMarkings() {
  return (
    <g fill="none" stroke="currentColor" strokeWidth="3" className="text-chalk/40">
      <rect x="10" y="10" width="1050" height="680" />
      <line x1="535" y1="10" x2="535" y2="690" />
      <circle cx="535" cy="350" r="91.5" />
      <rect x="10" y="148.5" width="165" height="403" />
      <rect x="10" y="258.5" width="55" height="183" />
      <rect x="895" y="148.5" width="165" height="403" />
      <rect x="1005" y="258.5" width="55" height="183" />
      <path d="M 175 276.9 A 91.5 91.5 0 0 1 175 423.1" />
      <path d="M 895 276.9 A 91.5 91.5 0 0 0 895 423.1" />
      <rect x="0" y="313.4" width="10" height="73.2" />
      <rect x="1060" y="313.4" width="10" height="73.2" />
    </g>
  );
}

const POSITIONS: { x: number; y: number; label: string; group: "gk" | "def" | "mid" | "att" }[] = [
  { x: 60, y: 350, label: "GK", group: "gk" },
  { x: 250, y: 110, label: "RB", group: "def" },
  { x: 220, y: 270, label: "CB", group: "def" },
  { x: 220, y: 430, label: "CB", group: "def" },
  { x: 250, y: 590, label: "LB", group: "def" },
  { x: 370, y: 350, label: "DM", group: "mid" },
  { x: 500, y: 210, label: "CM", group: "mid" },
  { x: 500, y: 490, label: "CM", group: "mid" },
  { x: 760, y: 120, label: "RW", group: "att" },
  { x: 820, y: 350, label: "ST", group: "att" },
  { x: 760, y: 580, label: "LW", group: "att" },
];

const GROUP_COLOUR = { gk: "#7FD8A6", def: "#6CABDD", mid: "#FFD166", att: "#FF8A80" } as const;

/** A 4-3-3 line-up on a full pitch, attacking left to right. */
export function PositionsDiagram() {
  return (
    <figure className="m-0">
      <svg viewBox="0 0 1070 700" className="h-auto w-full" role="img" aria-labelledby="positions-title positions-desc">
        <title id="positions-title">The positions in a 4-3-3 formation</title>
        <desc id="positions-desc">
          A goalkeeper, four defenders (right back, two centre-backs, left back), three midfielders (one defensive
          midfielder and two central midfielders) and three forwards (right winger, striker, left winger), attacking
          from left to right.
        </desc>
        <rect x="0" y="0" width="1070" height="700" fill="#0A1422" />
        <PitchMarkings />
        <g fontFamily="var(--font-display)" fontWeight="800" textAnchor="middle">
          <text x="535" y="40" fontSize="26" fill="#9FB0C4">Attacking this way →</text>
          {POSITIONS.map((spot, index) => (
            <g key={index}>
              <circle cx={spot.x} cy={spot.y} r="34" fill={GROUP_COLOUR[spot.group]} />
              <text x={spot.x} y={spot.y + 10} fontSize="28" fill="#0E1A2B">
                {spot.label}
              </text>
            </g>
          ))}
        </g>
      </svg>
      <figcaption className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-sm text-chalk-dim">
        <Key colour={GROUP_COLOUR.gk} label="Goalkeeper" />
        <Key colour={GROUP_COLOUR.def} label="Defenders" />
        <Key colour={GROUP_COLOUR.mid} label="Midfielders" />
        <Key colour={GROUP_COLOUR.att} label="Forwards" />
      </figcaption>
    </figure>
  );
}

function Key({ colour, label }: { colour: string; label: string }) {
  return (
    <span className="flex items-center gap-2">
      <span aria-hidden="true" className="h-3 w-3 rounded-full" style={{ background: colour }} />
      {label}
    </span>
  );
}

/**
 * Two pictures of the same pass: the attacker level with the second-last defender (onside),
 * and the attacker beyond him when the ball is played (offside).
 */
export function OffsideDiagram() {
  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <OffsideScene offside={false} />
      <OffsideScene offside />
    </div>
  );
}

function OffsideScene({ offside }: { offside: boolean }) {
  const lineX = 300;
  const attackerX = offside ? 345 : 300;
  const title = offside ? "Offside" : "Onside";
  return (
    <figure className="m-0 rounded-panel border border-line bg-pitch-deep p-3">
      <svg viewBox="0 0 420 300" className="h-auto w-full" role="img" aria-label={`${title}: ${offside ? "the attacker is beyond the second-last defender when the pass is played" : "the attacker is level with the second-last defender when the pass is played"}`}>
        {/* Goal on the right, attacking left to right */}
        <rect x="408" y="110" width="10" height="80" fill="none" stroke="#ECEFF3" strokeOpacity="0.5" strokeWidth="2" />
        <line x1="408" y1="10" x2="408" y2="290" stroke="#ECEFF3" strokeOpacity="0.35" strokeWidth="2" />
        {/* Line of the second-last defender */}
        <line x1={lineX} y1="20" x2={lineX} y2="280" stroke="#FFD166" strokeWidth="2.5" strokeDasharray="8 6" />
        <text x={lineX} y="16" textAnchor="middle" fontSize="13" fill="#FFD166">second-last defender</text>
        {/* Goalkeeper (last defender) and the second-last defender */}
        <circle cx="395" cy="150" r="12" fill="#6CABDD" />
        <circle cx={lineX} cy="95" r="12" fill="#6CABDD" />
        {/* Passer with the ball, and the attacker */}
        <circle cx="120" cy="220" r="12" fill="#FF8A80" />
        <circle cx="136" cy="214" r="5" fill="#ECEFF3" />
        <circle cx={attackerX} cy="190" r="12" fill="#FF8A80" />
        <line x1="142" y1="212" x2={attackerX - 18} y2="192" stroke="#ECEFF3" strokeWidth="2" strokeDasharray="4 5" markerEnd="url(#arrow)" />
        <defs>
          <marker id="arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" fill="#ECEFF3" />
          </marker>
        </defs>
        <text x="120" y="255" textAnchor="middle" fontSize="13" fill="#9FB0C4">passer</text>
        <text x={attackerX} y="225" textAnchor="middle" fontSize="13" fill="#9FB0C4">attacker</text>
      </svg>
      <figcaption className="mt-2 text-[0.95rem]">
        <strong className={offside ? "text-danger" : "text-success"}>{title}.</strong>{" "}
        {offside
          ? "When the pass is played, the attacker is closer to the goal line than the second-last defender."
          : "Level with the second-last defender counts as onside."}
      </figcaption>
    </figure>
  );
}
